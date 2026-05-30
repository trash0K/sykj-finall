package com.kjd.reimbursement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjd.reimbursement.exception.BusinessException;
import com.kjd.reimbursement.exception.ErrorCode;
import com.kjd.reimbursement.mapper.FkReimMainMapper;
import com.kjd.reimbursement.pojo.entity.FkReimAllocation;
import com.kjd.reimbursement.pojo.entity.FkReimItinerary;
import com.kjd.reimbursement.pojo.entity.FkReimMain;
import com.kjd.reimbursement.pojo.entity.FkReimSubsidy;
import com.kjd.reimbursement.pojo.entity.FkSubsidyCalendar;
import com.kjd.reimbursement.pojo.vo.PageResult;
import com.kjd.reimbursement.service.FkReimAllocationService;
import com.kjd.reimbursement.service.FkReimItineraryService;
import com.kjd.reimbursement.service.FkReimMainService;
import com.kjd.reimbursement.service.FkReimSubsidyService;
import com.kjd.reimbursement.service.FkSubsidyCalendarService;
import com.kjd.reimbursement.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class FkReimMainServiceImpl extends ServiceImpl<FkReimMainMapper, FkReimMain> implements FkReimMainService {

    @Autowired
    private FkReimItineraryService fkReimItineraryService;
    @Autowired
    private FkReimSubsidyService fkReimSubsidyService;
    @Autowired
    private FkReimAllocationService fkReimAllocationService;
    @Autowired
    private FkSubsidyCalendarService fkSubsidyCalendarService;
    @Autowired
    private IdGenerator idGenerator;
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    // 城市餐补标准映射: Key为城市类型(1=一线, 2=二线, 3=三线), Value为每日餐补金额(元)
    private static final Map<String, String> CITY_MEAL_STANDARD = Map.of("1", "100", "2", "80", "3", "50");
    // 交通补助标准: 每人每日固定金额(元)
    private static final String TRAFFIC_STANDARD = "40";
    // 通讯补助标准: 每人每日固定金额(元)
    private static final String COMMUNICATION_STANDARD = "40";
    private static final String LIST_CACHE_PREFIX = "reim:list:";
    private static final String LIST_CACHE_INDEX_KEY = "reim:list:keys";
    private static final String DETAIL_CACHE_PREFIX = "reim:main:";
    private static final String CITY_CACHE_PREFIX = "reim:city:";
    private static final String NULL_CACHE_VALUE = "__NULL__";

    /**
     * 获取报销单列表（分页）
     */
    @Override
    public PageResult getReimbursementList(Integer page, Integer size, String id, String reimbursementTitle,
                                           String reimburserName, String reimDepartmentName, String reimCompanyName,
                                           String businessTripReason, String businessTypeName) {
        String cacheKey = buildListCacheKey(page, size, id, reimbursementTitle, reimburserName,
                reimDepartmentName, reimCompanyName, businessTripReason, businessTypeName);
        PageResult cached = getJsonCache(cacheKey, PageResult.class);
        if (cached != null) {
            return cached;
        }

        // 1. 构建分页对象
        Page<FkReimMain> pageParam = new Page<>(page, size);

        // 2. 构建动态查询条件，根据传入参数进行模糊匹配
        Page<FkReimMain> result = this.page(pageParam,
                new LambdaQueryWrapper<FkReimMain>()
                        .like(StringUtils.hasText(id), FkReimMain::getId, id)
                        .like(StringUtils.hasText(reimbursementTitle), FkReimMain::getReimbursementTitle, reimbursementTitle)
                        .like(StringUtils.hasText(reimburserName), FkReimMain::getReimburserName, reimburserName)
                        .like(StringUtils.hasText(reimDepartmentName), FkReimMain::getReimDepartmentName, reimDepartmentName)
                        .like(StringUtils.hasText(reimCompanyName), FkReimMain::getReimCompanyName, reimCompanyName)
                        .like(StringUtils.hasText(businessTripReason), FkReimMain::getBusinessTripReason, businessTripReason)
                        .like(StringUtils.hasText(businessTypeName), FkReimMain::getBusinessTypeName, businessTypeName)
                        .orderByDesc(FkReimMain::getCreationTime)
        );

        // 3.返回包含总记录数和当前页数据的PageResult对象
        PageResult pageResult = new PageResult(result.getTotal(), result.getRecords());
        putJsonCache(cacheKey, pageResult, 10, TimeUnit.MINUTES);
        registerListCacheKey(cacheKey);
        return pageResult;
    }

    /**
     * 获取报销单详情（含行程、补助明细及日历）
     */
    @Override
    public Map<String, Object> getReimbursementDetail(String id) {
        String cacheKey = DETAIL_CACHE_PREFIX + id;
        String cachedJson = getStringCache(cacheKey);
        if (StringUtils.hasText(cachedJson)) {
            if (NULL_CACHE_VALUE.equals(cachedJson)) {
                throw new BusinessException(ErrorCode.REIMBURSEMENT_NOT_FOUND);
            }
            Map<String, Object> cached = readJson(cachedJson, Map.class);
            if (cached != null) {
                return cached;
            }
        }

        // 1. 查询报销单主表信息
        FkReimMain fkReimMain = this.getById(id);
        if (fkReimMain == null) {
            putStringCache(cacheKey, NULL_CACHE_VALUE, 2, TimeUnit.MINUTES);
            throw new BusinessException(ErrorCode.REIMBURSEMENT_NOT_FOUND);
        }

        // 2. 查询关联的行程列表
        List<FkReimItinerary> itineraries = fkReimItineraryService.lambdaQuery()
                .eq(FkReimItinerary::getMainId, id)
                .list();

        // 3. 查询关联的补助列表
        List<FkReimSubsidy> subsidies = fkReimSubsidyService.lambdaQuery()
                .eq(FkReimSubsidy::getMainId, id)
                .list();

        // 查询所有关联的日历数据，平铺为前端需要的列表
        List<FkSubsidyCalendar> calendars = new ArrayList<>();
        for (FkReimSubsidy subsidy : subsidies) {
            calendars.addAll(fkSubsidyCalendarService.lambdaQuery()
                    .eq(FkSubsidyCalendar::getMainId, subsidy.getId())
                    .list());
        }

        // 4. 查询费用归属及分摊数据
        List<FkReimAllocation> allocations = fkReimAllocationService.lambdaQuery()
                .eq(FkReimAllocation::getMainId, id)
                .list();

        // 5. 组装最终数据并返回
        Map<String, Object> data = new HashMap<>();
        data.put("main", fkReimMain);
        data.put("itineraries", itineraries);
        data.put("subsidies", subsidies);
        data.put("calendars", calendars);
        data.put("allocations", allocations);
        putJsonCache(cacheKey, data, 10, TimeUnit.MINUTES);
        return data;
    }

    /**
     * 保存报销单（包含主单、行程、补助计算及日历生成）
     */
    @Override
    @Transactional
    public String saveReimbursement(Map<String, Object> params) {
        // 1. 调用内部保存方法处理核心逻辑（新增模式）
        String id = saveReimbursementInternal(params, null, null);
        // 2. 清除相关缓存，保证数据一致性
        evictReimbursementCaches(id);
        // 3. 返回新生成的报销单ID
        return id;
    }

    /**
     * 删除报销单（级联删除关联的行程、补助及日历）
     */
    @Override
    @Transactional
    public void deleteReimbursement(String id) {
        // 1. 校验主单是否存在
        FkReimMain fkReimMain = this.getById(id);
        if (fkReimMain == null) {
            throw new BusinessException(ErrorCode.REIMBURSEMENT_NOT_FOUND);
        }

        // 2. 删除关联的行程数据
        fkReimItineraryService.lambdaUpdate()
                .eq(FkReimItinerary::getMainId, id)
                .remove();

        // 3. 删除关联的补助日历及补助主数据
        List<FkReimSubsidy> subsidies = fkReimSubsidyService.lambdaQuery()
                .eq(FkReimSubsidy::getMainId, id)
                .list();
        for (FkReimSubsidy sub : subsidies) {
            fkSubsidyCalendarService.lambdaUpdate()
                    .eq(FkSubsidyCalendar::getMainId, sub.getId())
                    .remove();
        }
        fkReimSubsidyService.lambdaUpdate()
                .eq(FkReimSubsidy::getMainId, id)
                .remove();

        // 4. 删除关联的费用归属及分摊数据
        fkReimAllocationService.lambdaUpdate()
                .eq(FkReimAllocation::getMainId, id)
                .remove();

        // 5. 删除报销单主表记录
        this.removeById(id);
        // 5. 清除相关缓存
        evictReimbursementCaches(id);
    }

    /**
     * 更新报销单（先删后增策略）
     */
    @Override
    @Transactional
    public String updateReimbursement(Map<String, Object> params) {
        // 1. 校验待更新的报销单是否存在
        Map<String, Object> mainData = (Map<String, Object>) params.get("main");
        if (mainData == null) {
            throw new BusinessException(ErrorCode.MAIN_DATA_EMPTY);
        }
        String mainId = (String) mainData.get("id");
        if (!StringUtils.hasText(mainId)) {
            throw new BusinessException(ErrorCode.REIMBURSEMENT_ID_EMPTY);
        }
        FkReimMain existing = this.getById(mainId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.REIMBURSEMENT_NOT_FOUND);
        }

        // 2. 清除旧的关联数据（行程、补助日历、补助）
        fkReimItineraryService.lambdaUpdate()
                .eq(FkReimItinerary::getMainId, mainId)
                .remove();

        List<FkReimSubsidy> oldSubsidies = fkReimSubsidyService.lambdaQuery()
                .eq(FkReimSubsidy::getMainId, mainId)
                .list();
        for (FkReimSubsidy sub : oldSubsidies) {
            fkSubsidyCalendarService.lambdaUpdate()
                    .eq(FkSubsidyCalendar::getMainId, sub.getId())
                    .remove();
        }
        fkReimSubsidyService.lambdaUpdate()
                .eq(FkReimSubsidy::getMainId, mainId)
                .remove();

        // 清除旧的费用归属及分摊数据
        fkReimAllocationService.lambdaUpdate()
                .eq(FkReimAllocation::getMainId, mainId)
                .remove();

        // 3. 保留原报销单ID和创建时间，重新保存主单和明细
        String id = saveReimbursementInternal(params, mainId, existing.getCreationTime());
        // 4. 清除相关缓存
        evictReimbursementCaches(id);
        // 5. 返回报销单ID
        return id;
    }

    /**
     * 复制报销单
     */
    @Override
    @Transactional
    public String copyReimbursement(String id) {
        // 1. 查询源报销单是否存在
        FkReimMain source = this.getById(id);
        if (source == null) {
            throw new BusinessException(ErrorCode.REIMBURSEMENT_NOT_FOUND);
        }

        // 2. 查询源报销单的所有行程
        List<FkReimItinerary> sourceItineraries = fkReimItineraryService.lambdaQuery()
                .eq(FkReimItinerary::getMainId, id)
                .list();

        // 3. 构建复制参数，将源数据转换为Map结构
        Map<String, Object> params = new HashMap<>();
        params.put("main", buildMainMap(source));

        List<Map<String, Object>> itineraries = new ArrayList<>();
        for (FkReimItinerary sourceItinerary : sourceItineraries) {
            itineraries.add(buildItineraryMap(sourceItinerary));
        }
        params.put("itineraries", itineraries);

        // 4. 调用内部保存方法创建新报销单（生成新ID）
        String newId = saveReimbursementInternal(params, null, null);
        // 5. 清除相关缓存
        evictReimbursementCaches(newId);
        // 6. 返回新报销单ID
        return newId;
    }

    // ===== 私有辅助方法 =====

    /**
     * 内部保存方法：处理报销单的核心保存逻辑（支持新增和更新）
     */
    private String saveReimbursementInternal(Map<String, Object> params, String fixedMainId, String fixedCreationTime) {
        // 1. 解析并校验主单数据
        Map<String, Object> mainData = (Map<String, Object>) params.get("main");
        if (mainData == null) {
            throw new BusinessException(ErrorCode.MAIN_DATA_EMPTY);
        }

        // 2. 构建主单对象，判断是更新模式还是新增模式
        FkReimMain fkReimMain = parseMain(mainData);
        boolean updateMode = StringUtils.hasText(fixedMainId);
        fkReimMain.setId(updateMode ? fixedMainId : idGenerator.nextId(this));
        fkReimMain.setCreationTime(StringUtils.hasText(fixedCreationTime)
                ? fixedCreationTime
                : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        // 3. 解析行程数据，使用缓冲ID生成机制避免冲突
        List<Map<String, Object>> itineraryData = (List<Map<String, Object>>) params.get("itineraries");
        List<FkReimItinerary> itineraries = new ArrayList<>();
        Set<String> itineraryIds = new HashSet<>();
        if (itineraryData != null) {
            for (Map<String, Object> item : itineraryData) {
                FkReimItinerary it = parseItinerary(item);
                it.setId(nextBufferedId(fkReimItineraryService, itineraryIds));
                itineraries.add(it);
            }
        }
        // 4. 校验行程是否重复
        validateItineraryDuplicate(itineraries);

        // 5. 解析补助及日历数据：优先使用前端传来的数据，复制场景下走服务端计算
        List<Map<String, Object>> subsidyData = (List<Map<String, Object>>) params.get("subsidies");
        List<Map<String, Object>> calendarData = (List<Map<String, Object>>) params.get("calendars");
        boolean useFrontendData = subsidyData != null && !subsidyData.isEmpty();

        List<FkReimSubsidy> subsidies = new ArrayList<>();
        List<List<FkSubsidyCalendar>> allCalendars = new ArrayList<>();
        Set<String> subsidyIds = new HashSet<>();
        Set<String> calendarIds = new HashSet<>();

        if (useFrontendData) {
            // 从前端数据解析补助，建立前端key到新ID的映射
            Map<String, String> subsidyKeyToId = new HashMap<>();
            for (Map<String, Object> item : subsidyData) {
                FkReimSubsidy subsidy = parseSubsidy(item);
                subsidy.setId(nextBufferedId(fkReimSubsidyService, subsidyIds));
                subsidies.add(subsidy);
                // 前端用 subsidy.id 或 travelerId+departureDate+arrivalDate 作为临时key
                String frontendKey = (String) item.get("id");
                if (!StringUtils.hasText(frontendKey)) {
                    frontendKey = subsidy.getTravelerId() + subsidy.getDepartureDate() + subsidy.getArrivalDate();
                }
                subsidyKeyToId.put(frontendKey, subsidy.getId());
            }
            // 解析日历并按键分组
            Map<String, List<FkSubsidyCalendar>> calendarGroups = new LinkedHashMap<>();
            if (calendarData != null) {
                for (Map<String, Object> item : calendarData) {
                    FkSubsidyCalendar cal = parseCalendar(item);
                    cal.setId(nextBufferedId(fkSubsidyCalendarService, calendarIds));
                    // calendar.mainId 是前端 subsidy key
                    String calMainId = (String) item.get("mainId");
                    calendarGroups.computeIfAbsent(calMainId, k -> new ArrayList<>()).add(cal);
                }
            }
            // 将日历按 subsidy 顺序对齐
            for (FkReimSubsidy subsidy : subsidies) {
                // 用 subsidy 的原始 key 查找日历
                String lookupKey = null;
                for (Map.Entry<String, String> entry : subsidyKeyToId.entrySet()) {
                    if (entry.getValue().equals(subsidy.getId())) {
                        lookupKey = entry.getKey();
                        break;
                    }
                }
                List<FkSubsidyCalendar> grouped = calendarGroups.getOrDefault(lookupKey, new ArrayList<>());
                allCalendars.add(grouped);
            }
        } else {
            // 复制场景：服务端根据行程计算补助和日历
            for (FkReimItinerary it : itineraries) {
                Map<String, Object> subsidyResult = calculateSubsidy(it, fkReimMain.getBusinessTypeId(), fkReimMain.getBusinessTypeNo(), fkReimMain.getBusinessTypeName());
                FkReimSubsidy subsidy = (FkReimSubsidy) subsidyResult.get("subsidy");
                subsidy.setId(nextBufferedId(fkReimSubsidyService, subsidyIds));
                List<FkSubsidyCalendar> calendars = (List<FkSubsidyCalendar>) subsidyResult.get("calendars");
                for (FkSubsidyCalendar cal : calendars) {
                    cal.setId(nextBufferedId(fkSubsidyCalendarService, calendarIds));
                }
                subsidies.add(subsidy);
                allCalendars.add(calendars);
            }
        }

        // 6. 汇总所有补助金额
        BigDecimal totalSubsidy = BigDecimal.ZERO;
        BigDecimal totalMeal = BigDecimal.ZERO;
        BigDecimal totalTraffic = BigDecimal.ZERO;
        BigDecimal totalComm = BigDecimal.ZERO;
        for (FkReimSubsidy subsidy : subsidies) {
            totalSubsidy = totalSubsidy.add(new BigDecimal(subsidy.getSubsidyAmount()));
            totalMeal = totalMeal.add(new BigDecimal(subsidy.getMealAllowance()));
            totalTraffic = totalTraffic.add(new BigDecimal(subsidy.getTransportationAllowance()));
            totalComm = totalComm.add(new BigDecimal(subsidy.getPhoneAllowance()));
        }

        // 7. 校验前端传入金额与后端计算金额是否一致（仅复制场景需要校验，前端数据场景金额已确定）
        if (!useFrontendData) {
            validateAmount(mainData, "subsidyTotal", "补助总金额", totalSubsidy);
            validateAmount(mainData, "mealAllowance", "餐费补助", totalMeal);
            validateAmount(mainData, "transportationAllowance", "交通补助", totalTraffic);
            validateAmount(mainData, "phoneAllowance", "通讯补助", totalComm);
        }

        // 8. 将汇总金额设置到主单对象
        fkReimMain.setSubsidyTotal(totalSubsidy.toPlainString());
        fkReimMain.setMealAllowance(totalMeal.toPlainString());
        fkReimMain.setTransportationAllowance(totalTraffic.toPlainString());
        fkReimMain.setPhoneAllowance(totalComm.toPlainString());

        // 9. 根据模式执行插入或更新操作
        if (updateMode) {
            this.updateById(fkReimMain);
        } else {
            this.save(fkReimMain);
        }

        // 10. 关联主单ID，批量保存行程数据
        for (FkReimItinerary it : itineraries) {
            it.setMainId(fkReimMain.getId());
        }
        if (!itineraries.isEmpty()) {
            fkReimItineraryService.saveBatch(itineraries);
        }

        // 11. 关联主单ID，保存补助数据及其对应的日历数据
        for (int i = 0; i < subsidies.size(); i++) {
            FkReimSubsidy sub = subsidies.get(i);
            sub.setMainId(fkReimMain.getId());
            fkReimSubsidyService.save(sub);
            List<FkSubsidyCalendar> calendars = allCalendars.get(i);
            for (FkSubsidyCalendar cal : calendars) {
                cal.setMainId(sub.getId());
            }
            if (!calendars.isEmpty()) {
                fkSubsidyCalendarService.saveBatch(calendars);
            }
        }

        // 12. 解析并保存费用归属及分摊数据
        List<Map<String, Object>> allocData = (List<Map<String, Object>>) params.get("allocations");
        if (allocData != null && !allocData.isEmpty()) {
            Set<String> allocIds = new HashSet<>();
            List<FkReimAllocation> allocations = new ArrayList<>();
            for (Map<String, Object> item : allocData) {
                FkReimAllocation alloc = parseAllocation(item);
                alloc.setId(nextBufferedId(fkReimAllocationService, allocIds));
                alloc.setMainId(fkReimMain.getId());
                allocations.add(alloc);
            }
            fkReimAllocationService.saveBatch(allocations);
        }

        // 12. 返回报销单ID
        return fkReimMain.getId();
    }

    /**
     * 解析主单数据
     */
    private FkReimMain parseMain(Map<String, Object> data) {
        // 1. 创建主单对象
        FkReimMain fkReimMain = new FkReimMain();
        // 2. 从Map中提取字段并设置到对象
        fkReimMain.setReimbursementTitle((String) data.get("reimbursementTitle"));
        fkReimMain.setReimburserId((String) data.get("reimburserId"));
        fkReimMain.setReimburserNo((String) data.get("reimburserNo"));
        fkReimMain.setReimburserName((String) data.get("reimburserName"));
        fkReimMain.setReimDepartmentId((String) data.get("reimDepartmentId"));
        fkReimMain.setReimDepartmentNo((String) data.get("reimDepartmentNo"));
        fkReimMain.setReimDepartmentName((String) data.get("reimDepartmentName"));
        fkReimMain.setReimCompanyId((String) data.get("reimCompanyId"));
        fkReimMain.setReimCompanyNo((String) data.get("reimCompanyNo"));
        fkReimMain.setReimCompanyName((String) data.get("reimCompanyName"));
        fkReimMain.setBusinessTypeId((String) data.get("businessTypeId"));
        fkReimMain.setBusinessTypeNo((String) data.get("businessTypeNo"));
        fkReimMain.setBusinessTypeName((String) data.get("businessTypeName"));
        fkReimMain.setBusinessTripReason((String) data.get("businessTripReason"));
        fkReimMain.setRemarks((String) data.get("remarks"));
        // 3. 返回主单对象
        return fkReimMain;
    }

    /**
     * 解析行程数据
     */
    private FkReimItinerary parseItinerary(Map<String, Object> data) {
        // 1. 创建行程对象
        FkReimItinerary it = new FkReimItinerary();
        // 2. 从Map中提取字段并设置到对象
        it.setTravelerId((String) data.get("travelerId"));
        it.setTravelerNo((String) data.get("travelerNo"));
        it.setTravelerName((String) data.get("travelerName"));
        it.setDepartureDate((String) data.get("departureDate"));
        it.setArrivalDate((String) data.get("arrivalDate"));
        it.setDepartureCity((String) data.get("departureCity"));
        it.setDepartureCityNo((String) data.get("departureCityNo"));
        it.setArrivingCity((String) data.get("arrivingCity"));
        it.setArrivingCityNo((String) data.get("arrivingCityNo"));
        it.setItineraryInstructions((String) data.get("itineraryInstructions"));
        // 3. 返回行程对象
        return it;
    }

    /**
     * 解析前端传来的补助数据
     */
    private FkReimSubsidy parseSubsidy(Map<String, Object> data) {
        FkReimSubsidy subsidy = new FkReimSubsidy();
        subsidy.setTravelerId((String) data.get("travelerId"));
        subsidy.setTravelerNo((String) data.get("travelerNo"));
        subsidy.setTravelerName((String) data.get("travelerName"));
        subsidy.setDepartureDate((String) data.get("departureDate"));
        subsidy.setArrivalDate((String) data.get("arrivalDate"));
        subsidy.setSubsidyDays((String) data.get("subsidyDays"));
        subsidy.setDepartureCity((String) data.get("departureCity"));
        subsidy.setDepartureCityNo((String) data.get("departureCityNo"));
        subsidy.setArrivingCity((String) data.get("arrivingCity"));
        subsidy.setArrivingCityNo((String) data.get("arrivingCityNo"));
        subsidy.setApplicationAmount(Objects.toString(data.get("applicationAmount"), "0"));
        subsidy.setSubsidyAmount(Objects.toString(data.get("subsidyAmount"), "0"));
        subsidy.setMealAllowance(Objects.toString(data.get("mealAllowance"), "0"));
        subsidy.setTransportationAllowance(Objects.toString(data.get("transportationAllowance"), "0"));
        subsidy.setPhoneAllowance(Objects.toString(data.get("phoneAllowance"), "0"));
        subsidy.setBusinessTypeId((String) data.get("businessTypeId"));
        subsidy.setBusinessTypeNo((String) data.get("businessTypeNo"));
        subsidy.setBusinessTypeName((String) data.get("businessTypeName"));
        return subsidy;
    }

    /**
     * 解析前端传来的费用归属分摊数据
     */
    private FkReimAllocation parseAllocation(Map<String, Object> data) {
        FkReimAllocation alloc = new FkReimAllocation();
        alloc.setAttributionId((String) data.get("attributionId"));
        alloc.setAttributionName((String) data.get("attributionName"));
        alloc.setProjectId((String) data.get("projectId"));
        alloc.setProjectNo((String) data.get("projectNo"));
        alloc.setProjectName((String) data.get("projectName"));
        alloc.setAllocationRatio(Objects.toString(data.get("allocationRatio"), "0"));
        alloc.setAllocationAmount(Objects.toString(data.get("allocationAmount"), "0"));
        return alloc;
    }

    /**
     * 将分摊对象转换为Map
     */
    private Map<String, Object> buildAllocationMap(FkReimAllocation alloc) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", alloc.getId());
        data.put("mainId", alloc.getMainId());
        data.put("attributionId", alloc.getAttributionId());
        data.put("attributionName", alloc.getAttributionName());
        data.put("projectId", alloc.getProjectId());
        data.put("projectNo", alloc.getProjectNo());
        data.put("projectName", alloc.getProjectName());
        data.put("allocationRatio", alloc.getAllocationRatio());
        data.put("allocationAmount", alloc.getAllocationAmount());
        return data;
    }

    /**
     * 解析前端传来的补助日历数据
     */
    private FkSubsidyCalendar parseCalendar(Map<String, Object> data) {
        FkSubsidyCalendar cal = new FkSubsidyCalendar();
        cal.setTravelDate((String) data.get("travelDate"));
        cal.setTravelDateWeek((String) data.get("travelDateWeek"));
        cal.setSubsidizedCities((String) data.get("subsidizedCities"));
        cal.setSubsidizedCityNumber((String) data.get("subsidizedCityNumber"));
        cal.setStandardMealExpensesAmount(Objects.toString(data.get("standardMealExpensesAmount"), "0"));
        cal.setStandardTrafficAmount(Objects.toString(data.get("standardTrafficAmount"), "0"));
        cal.setStandardCommunicationAmount(Objects.toString(data.get("standardCommunicationAmount"), "0"));
        cal.setMealExpensesAmount(Objects.toString(data.get("mealExpensesAmount"), "0"));
        cal.setTrafficAmount(Objects.toString(data.get("trafficAmount"), "0"));
        cal.setCommunicationAmount(Objects.toString(data.get("communicationAmount"), "0"));
        cal.setIsReimbursed(Objects.toString(data.get("isReimbursed"), "1"));
        return cal;
    }

    /**
     * 校验行程是否重复（同一出行人+同一天数区间）
     */
    private void validateItineraryDuplicate(List<FkReimItinerary> itineraries) {
        // 1. 使用Set存储行程唯一标识
        Set<String> keySet = new HashSet<>();
        for (FkReimItinerary it : itineraries) {
            // 2. 构建唯一键：出行人ID_出发日期_到达日期
            String key = it.getTravelerId() + "_" + it.getDepartureDate() + "_" + it.getArrivalDate();
            // 3. 如果添加失败，说明存在重复行程
            if (!keySet.add(key)) {
                throw new BusinessException(ErrorCode.ITINERARY_DUPLICATE, "出行人[" + it.getTravelerName() + "]在日期[" + it.getDepartureDate() + "~" + it.getArrivalDate() + "]存在重复行程");
            }
        }
    }

    /**
     * 计算单个行程的补助金额及每日日历
     */
    private Map<String, Object> calculateSubsidy(FkReimItinerary it, String businessTypeId, String businessTypeNo, String businessTypeName) {
        // 1. 计算出差天数
        LocalDate start = LocalDate.parse(it.getDepartureDate());
        LocalDate end = LocalDate.parse(it.getArrivalDate());
        if (end.isBefore(start)) {
            throw new BusinessException(ErrorCode.DATE_INVALID, "到达日期不能早于出发日期");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;

        // 2. 根据城市类型确定补助标准
        String cityType = getCityTypeByNo(it.getArrivingCityNo());
        String mealStandard = CITY_MEAL_STANDARD.getOrDefault(cityType, "50");

        // 3. 计算各项补助总额
        BigDecimal mealTotal = new BigDecimal(mealStandard).multiply(BigDecimal.valueOf(days));
        BigDecimal trafficTotal = new BigDecimal(TRAFFIC_STANDARD).multiply(BigDecimal.valueOf(days));
        BigDecimal commTotal = new BigDecimal(COMMUNICATION_STANDARD).multiply(BigDecimal.valueOf(days));
        BigDecimal totalAmount = mealTotal.add(trafficTotal).add(commTotal);

        // 4. 封装补助实体对象
        FkReimSubsidy subsidy = new FkReimSubsidy();
        subsidy.setTravelerId(it.getTravelerId());
        subsidy.setTravelerNo(it.getTravelerNo());
        subsidy.setTravelerName(it.getTravelerName());
        subsidy.setDepartureDate(it.getDepartureDate());
        subsidy.setArrivalDate(it.getArrivalDate());
        subsidy.setSubsidyDays(String.valueOf(days));
        subsidy.setDepartureCity(it.getDepartureCity());
        subsidy.setDepartureCityNo(it.getDepartureCityNo());
        subsidy.setArrivingCity(it.getArrivingCity());
        subsidy.setArrivingCityNo(it.getArrivingCityNo());
        subsidy.setApplicationAmount(totalAmount.toPlainString());
        subsidy.setSubsidyAmount(totalAmount.toPlainString());
        subsidy.setMealAllowance(mealTotal.toPlainString());
        subsidy.setTransportationAllowance(trafficTotal.toPlainString());
        subsidy.setPhoneAllowance(commTotal.toPlainString());
        subsidy.setBusinessTypeId(businessTypeId);
        subsidy.setBusinessTypeNo(businessTypeNo);
        subsidy.setBusinessTypeName(businessTypeName);

        // 5. 生成每日补助日历（按天拆分）
        List<FkSubsidyCalendar> calendars = new ArrayList<>();
        DateTimeFormatter weekFmt = DateTimeFormatter.ofPattern("E", Locale.CHINA); //星期
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            FkSubsidyCalendar cal = new FkSubsidyCalendar();
            cal.setTravelDate(d.toString());
            cal.setTravelDateWeek(d.format(weekFmt));
            cal.setSubsidizedCities(it.getArrivingCity());
            cal.setSubsidizedCityNumber(it.getArrivingCityNo());
            cal.setStandardMealExpensesAmount(mealStandard);
            cal.setStandardTrafficAmount(TRAFFIC_STANDARD);
            cal.setStandardCommunicationAmount(COMMUNICATION_STANDARD);
            cal.setMealExpensesAmount(mealStandard);
            cal.setTrafficAmount(TRAFFIC_STANDARD);
            cal.setCommunicationAmount(COMMUNICATION_STANDARD);
            cal.setIsReimbursed("1");
            calendars.add(cal);
        }

        // 6. 封装结果并返回
        Map<String, Object> result = new HashMap<>();
        result.put("subsidy", subsidy);
        result.put("calendars", calendars);
        return result;
    }

    /**
     * 根据城市编号获取城市类型（一线/二线/三线）
     */
    private String getCityTypeByNo(String cityNo) {
        // 1. 尝试从缓存中获取城市类型
        String cacheKey = CITY_CACHE_PREFIX + Objects.toString(cityNo, "");
        String cached = getStringCache(cacheKey);
        if (StringUtils.hasText(cached)) {
            return cached;
        }

        // 2. 从配置Map中获取城市类型
        Map<String, String> cityTypeMap = Map.of(
                "10119", "1", "10621", "1", "10458", "2", "10216", "2", "10455", "3"
        );
        String cityType = cityTypeMap.getOrDefault(cityNo, "3");
        // 3. 存入缓存，有效期1小时
        putStringCache(cacheKey, cityType, 1, TimeUnit.HOURS);
        // 4. 返回城市类型
        return cityType;
    }

    /**
     * 校验金额：比较前端传入值与后端计算值
     */
    private void validateAmount(Map<String, Object> mainData, String fieldName, String fieldLabel, BigDecimal calculated) {
        // 1. 获取前端传入的金额值
        Object value = mainData.get(fieldName);
        if (value == null || !StringUtils.hasText(value.toString())) {
            return;
        }

        // 2. 解析前端金额为BigDecimal
        BigDecimal frontendAmount;
        try {
            frontendAmount = new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.AMOUNT_CHECK_FAILED, fieldLabel + "格式不正确");
        }

        // 3. 比较前端金额与后端计算金额是否一致
        if (frontendAmount.compareTo(calculated) != 0) {
            BigDecimal diff = frontendAmount.subtract(calculated);
            throw new BusinessException(ErrorCode.AMOUNT_CHECK_FAILED, fieldLabel + "与后端计算金额不一致，前端传入"
                    + frontendAmount.toPlainString() + "，后端计算"
                    + calculated.toPlainString() + "，差额" + diff.toPlainString());
        }
    }

    /**
     * 生成缓冲ID，避免在同一批次中产生重复ID
     */
    private <T> String nextBufferedId(IService<T> service, Set<String> usedIds) {
        // 1. 从ID生成器获取新ID
        String id = idGenerator.nextId(service);
        // 2. 如果ID已使用，递增直到找到未使用的ID
        while (usedIds.contains(id)) {
            id = increaseId(id);
        }
        // 3. 标记该ID已被使用
        usedIds.add(id);
        // 4. 返回唯一ID
        return id;
    }

    /**
     * ID递增：将最后3位序号加1
     */
    private String increaseId(String id) {
        // 1. 校验ID格式
        if (id == null || id.length() < 3) {
            throw new BusinessException(ErrorCode.ID_GENERATE_FAILED, "主键生成失败");
        }
        // 2. 提取前缀和序号
        String prefix = id.substring(0, id.length() - 3);
        int seq = Integer.parseInt(id.substring(id.length() - 3)) + 1;
        // 3. 检查序号是否超出上限
        if (seq > 999) {
            throw new BusinessException(ErrorCode.ID_GENERATE_FAILED, "当日ID序号已超上限(999)，请稍后重试");
        }
        // 4. 返回递增后的ID
        return prefix + String.format("%03d", seq);
    }

    /**
     * 将主单对象转换为Map
     */
    private Map<String, Object> buildMainMap(FkReimMain main) {
        // 1. 创建Map对象
        Map<String, Object> data = new HashMap<>();
        // 2. 将主单字段逐个放入Map
        data.put("reimbursementTitle", main.getReimbursementTitle());
        data.put("reimburserId", main.getReimburserId());
        data.put("reimburserNo", main.getReimburserNo());
        data.put("reimburserName", main.getReimburserName());
        data.put("reimDepartmentId", main.getReimDepartmentId());
        data.put("reimDepartmentNo", main.getReimDepartmentNo());
        data.put("reimDepartmentName", main.getReimDepartmentName());
        data.put("reimCompanyId", main.getReimCompanyId());
        data.put("reimCompanyNo", main.getReimCompanyNo());
        data.put("reimCompanyName", main.getReimCompanyName());
        data.put("businessTypeId", main.getBusinessTypeId());
        data.put("businessTypeNo", main.getBusinessTypeNo());
        data.put("businessTypeName", main.getBusinessTypeName());
        data.put("businessTripReason", main.getBusinessTripReason());
        data.put("subsidyTotal", main.getSubsidyTotal());
        data.put("mealAllowance", main.getMealAllowance());
        data.put("transportationAllowance", main.getTransportationAllowance());
        data.put("phoneAllowance", main.getPhoneAllowance());
        data.put("remarks", main.getRemarks());
        // 3. 返回Map
        return data;
    }

    /**
     * 将行程对象转换为Map
     */
    private Map<String, Object> buildItineraryMap(FkReimItinerary itinerary) {
        // 1. 创建Map对象
        Map<String, Object> data = new HashMap<>();
        // 2. 将行程字段逐个放入Map
        data.put("travelerId", itinerary.getTravelerId());
        data.put("travelerNo", itinerary.getTravelerNo());
        data.put("travelerName", itinerary.getTravelerName());
        data.put("departureDate", itinerary.getDepartureDate());
        data.put("arrivalDate", itinerary.getArrivalDate());
        data.put("departureCity", itinerary.getDepartureCity());
        data.put("departureCityNo", itinerary.getDepartureCityNo());
        data.put("arrivingCity", itinerary.getArrivingCity());
        data.put("arrivingCityNo", itinerary.getArrivingCityNo());
        data.put("itineraryInstructions", itinerary.getItineraryInstructions());
        // 3. 返回Map
        return data;
    }

    /**
     * 构建列表查询的缓存Key
     */
    private String buildListCacheKey(Integer page, Integer size, String id, String reimbursementTitle,
                                     String reimburserName, String reimDepartmentName, String reimCompanyName,
                                     String businessTripReason, String businessTypeName) {
        // 1. 将所有查询参数拼接成字符串
        String rawKey = String.join("|",
                Objects.toString(page, "1"),
                Objects.toString(size, "10"),
                Objects.toString(id, ""),
                Objects.toString(reimbursementTitle, ""),
                Objects.toString(reimburserName, ""),
                Objects.toString(reimDepartmentName, ""),
                Objects.toString(reimCompanyName, ""),
                Objects.toString(businessTripReason, ""),
                Objects.toString(businessTypeName, ""));
        // 2. 对原始字符串进行MD5加密，生成唯一缓存Key
        return LIST_CACHE_PREFIX + DigestUtils.md5DigestAsHex(rawKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 从缓存中获取JSON数据并反序列化
     */
    private <T> T getJsonCache(String key, Class<T> type) {
        // 1. 获取缓存中的JSON字符串
        String json = getStringCache(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        // 2. 将JSON字符串反序列化为指定类型
        return readJson(json, type);
    }

    /**
     * 将JSON字符串反序列化为对象
     */
    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 从缓存中获取Map对象
     */
    private Map<String, Object> getMapCache(String key) {
        Map cached = getJsonCache(key, Map.class);
        if (cached == null) {
            return null;
        }
        return cached;
    }

    /**
     * 从Redis获取字符串缓存
     */
    private String getStringCache(String key) {
        // 1. 检查Redis模板是否可用
        if (redisTemplate == null) {
            return null;
        }
        // 2. 从Redis获取值
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 将对象序列化后存入Redis缓存
     */
    private void putJsonCache(String key, Object value, long timeout, TimeUnit unit) {
        try {
            // 1. 将对象序列化为JSON字符串
            putStringCache(key, objectMapper.writeValueAsString(value), timeout, unit);
        } catch (JsonProcessingException e) {
            // 缓存失败时保留主流程
        }
    }

    /**
     * 将字符串存入Redis缓存
     */
    private void putStringCache(String key, String value, long timeout, TimeUnit unit) {
        // 1. 检查Redis模板是否可用
        if (redisTemplate == null) {
            return;
        }
        // 2. 存入Redis并设置过期时间
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (RuntimeException e) {
            // 缓存失败时保留主流程
        }
    }

    /**
     * 注册列表缓存Key，方便后续批量清理
     */
    private void registerListCacheKey(String cacheKey) {
        if (redisTemplate == null) {
            return;
        }
        try {
            // 1. 将缓存Key加入索引集合
            redisTemplate.opsForSet().add(LIST_CACHE_INDEX_KEY, cacheKey);
            // 2. 设置索引集合的过期时间
            redisTemplate.expire(LIST_CACHE_INDEX_KEY, 30, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            // 缓存失败时保留主流程
        }
    }

    /**
     * 清除报销单相关的所有缓存
     */
    private void evictReimbursementCaches(String id) {
        if (redisTemplate == null) {
            return;
        }
        try {
            // 1. 获取所有列表缓存Key
            Set<String> keys = redisTemplate.opsForSet().members(LIST_CACHE_INDEX_KEY);
            if (keys != null && !keys.isEmpty()) {
                // 2. 批量删除列表缓存
                redisTemplate.delete(keys);
            }
            // 3. 删除列表索引集合
            redisTemplate.delete(LIST_CACHE_INDEX_KEY);
            // 4. 删除详情缓存
            if (StringUtils.hasText(id)) {
                redisTemplate.delete(DETAIL_CACHE_PREFIX + id);
            }
        } catch (RuntimeException e) {
            // 缓存失败时保留主流程
        }
    }
}
