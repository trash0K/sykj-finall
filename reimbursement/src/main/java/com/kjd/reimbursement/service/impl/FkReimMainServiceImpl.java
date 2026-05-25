package com.kjd.reimbursement.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kjd.reimbursement.mapper.FkReimMainMapper;
import com.kjd.reimbursement.pojo.entity.FkReimItinerary;
import com.kjd.reimbursement.pojo.entity.FkReimMain;
import com.kjd.reimbursement.pojo.entity.FkReimSubsidy;
import com.kjd.reimbursement.pojo.entity.FkSubsidyCalendar;
import com.kjd.reimbursement.pojo.vo.PageResult;
import com.kjd.reimbursement.service.FkReimItineraryService;
import com.kjd.reimbursement.service.FkReimMainService;
import com.kjd.reimbursement.service.FkReimSubsidyService;
import com.kjd.reimbursement.service.FkSubsidyCalendarService;
import com.kjd.reimbursement.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class FkReimMainServiceImpl extends ServiceImpl<FkReimMainMapper, FkReimMain> implements FkReimMainService {

    @Autowired
    private FkReimItineraryService fkReimItineraryService;
    @Autowired
    private FkReimSubsidyService fkReimSubsidyService;
    @Autowired
    private FkSubsidyCalendarService fkSubsidyCalendarService;
    @Autowired
    private IdGenerator idGenerator;

    // 城市餐补标准映射: Key为城市类型(1=一线, 2=二线, 3=三线), Value为每日餐补金额(元)
    private static final Map<String, String> CITY_MEAL_STANDARD = Map.of("1", "100", "2", "80", "3", "50");
    // 交通补助标准: 每人每日固定金额(元)
    private static final String TRAFFIC_STANDARD = "40";
    // 通讯补助标准: 每人每日固定金额(元)
    private static final String COMMUNICATION_STANDARD = "40";

    /**
     * 获取报销单列表（分页）
     */
    @Override
    public PageResult getReimbursementList(Integer page, Integer size, String id, String reimbursementTitle,
                                           String reimburserName, String reimDepartmentName, String reimCompanyName,
                                           String businessTripReason, String businessTypeName) {
        // 1. 构建分页对象
        Page<FkReimMain> pageParam = new Page<>(page, size);

        // 2. 构建动态查询条件，根据传入参数进行模糊匹配
        Page<FkReimMain> result = this.page(pageParam,
                lambdaQuery()
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
        return new PageResult(result.getTotal(), result.getRecords());
    }

    /**
     * 获取报销单详情（含行程、补助明细及日历）
     */
    @Override
    public Map<String, Object> getReimbursementDetail(String id) {
        // 1. 查询报销单主表信息
        FkReimMain fkReimMain = this.getById(id);
        if (fkReimMain == null) {
            throw new RuntimeException("报销单不存在");
        }

        // 2. 查询关联的行程列表
        List<FkReimItinerary> itineraries = fkReimItineraryService.lambdaQuery()
                .eq(FkReimItinerary::getMainId, id)
                .list();

        // 3. 查询关联的补助列表，并进一步查询每个补助对应的日历详情
        List<FkReimSubsidy> subsidies = fkReimSubsidyService.lambdaQuery()
                .eq(FkReimSubsidy::getMainId, id)
                .list();

        List<Map<String, Object>> subsidyDetails = new ArrayList<>();
        for (FkReimSubsidy subsidy : subsidies) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("subsidy", subsidy);
            detail.put("calendars", fkSubsidyCalendarService.lambdaQuery()
                    .eq(FkSubsidyCalendar::getMainId, subsidy.getId())
                    .list());
            subsidyDetails.add(detail);
        }

        // 4. 组装最终数据并返回
        Map<String, Object> data = new HashMap<>();
        data.put("main", fkReimMain);
        data.put("itineraries", itineraries);
        data.put("subsidyDetails", subsidyDetails);
        return data;
    }

    /**
     * 保存报销单（包含主单、行程、补助计算及日历生成）
     */
    @Override
    @Transactional
    public String saveReimbursement(Map<String, Object> params) {
        // 1. 解析前端传来的主单数据并设置创建时间和ID
        Map<String, Object> mainData = (Map<String, Object>) params.get("main");
        FkReimMain fkReimMain = parseMain(mainData);
        fkReimMain.setId(idGenerator.nextId(this));
        fkReimMain.setCreationTime(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        // 2. 解析行程数据并进行重复性校验
        List<Map<String, Object>> itineraryData = (List<Map<String, Object>>) params.get("itineraries");
        List<FkReimItinerary> itineraries = new ArrayList<>();
        if (itineraryData != null) {
            for (Map<String, Object> item : itineraryData) {
                FkReimItinerary it = parseItinerary(item);
                it.setId(idGenerator.nextId(fkReimItineraryService));
                itineraries.add(it);
            }
        }
        validateItineraryDuplicate(itineraries);

        // 3. 遍历行程，自动计算补助金额并生成补助日历
        List<FkReimSubsidy> subsidies = new ArrayList<>();
        List<List<FkSubsidyCalendar>> allCalendars = new ArrayList<>();
        for (FkReimItinerary it : itineraries) {
            Map<String, Object> subsidyResult = calculateSubsidy(it, fkReimMain.getBusinessTypeId(), fkReimMain.getBusinessTypeNo(), fkReimMain.getBusinessTypeName());
            FkReimSubsidy subsidy = (FkReimSubsidy) subsidyResult.get("subsidy");
            subsidy.setId(idGenerator.nextId(fkReimSubsidyService));
            List<FkSubsidyCalendar> calendars = (List<FkSubsidyCalendar>) subsidyResult.get("calendars");
            for (FkSubsidyCalendar cal : calendars) {
                cal.setId(idGenerator.nextId(fkSubsidyCalendarService));
            }
            subsidies.add(subsidy);
            allCalendars.add(calendars);
        }

        // 4. 汇总所有行程的补助金额（餐补、交通补、通讯补），更新到主单
        BigDecimal totalSubsidy = BigDecimal.ZERO;
        BigDecimal totalMeal = BigDecimal.ZERO;
        BigDecimal totalTraffic = BigDecimal.ZERO;
        BigDecimal totalComm = BigDecimal.ZERO;
        for (FkReimSubsidy s : subsidies) {
            totalSubsidy = totalSubsidy.add(new BigDecimal(s.getSubsidyAmount()));
            totalMeal = totalMeal.add(new BigDecimal(s.getMealAllowance()));
            totalTraffic = totalTraffic.add(new BigDecimal(s.getTransportationAllowance()));
            totalComm = totalComm.add(new BigDecimal(s.getPhoneAllowance()));
        }
        fkReimMain.setSubsidyTotal(totalSubsidy.toPlainString());
        fkReimMain.setMealAllowance(totalMeal.toPlainString());
        fkReimMain.setTransportationAllowance(totalTraffic.toPlainString());
        fkReimMain.setPhoneAllowance(totalComm.toPlainString());

        // 5. 持久化保存主单数据
        this.save(fkReimMain);

        // 6. 关联主单ID，批量保存行程数据
        for (FkReimItinerary it : itineraries) {
            it.setMainId(fkReimMain.getId());
        }
        fkReimItineraryService.saveBatch(itineraries);

        // 步骤7: 关联主单ID，保存补助数据及其对应的日历数据
        for (int i = 0; i < subsidies.size(); i++) {
            FkReimSubsidy sub = subsidies.get(i);
            sub.setMainId(fkReimMain.getId());
            fkReimSubsidyService.save(sub);
            List<FkSubsidyCalendar> calendars = allCalendars.get(i);
            for (FkSubsidyCalendar cal : calendars) {
                cal.setMainId(sub.getId());
            }
            fkSubsidyCalendarService.saveBatch(calendars);
        }

        return fkReimMain.getId();
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
            throw new RuntimeException("报销单不存在");
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

        // 4. 删除报销单主表记录
        this.removeById(id);
    }

    /**
     * 更新报销单（先删后增策略）
     */
    @Override
    @Transactional
    public String updateReimbursement(Map<String, Object> params) {
        // 1. 校验待更新的报销单是否存在
        Map<String, Object> mainData = (Map<String, Object>) params.get("main");
        String mainId = (String) mainData.get("id");
        if (!StringUtils.hasText(mainId)) {
            throw new RuntimeException("报销单ID不能为空");
        }
        FkReimMain existing = this.getById(mainId);
        if (existing == null) {
            throw new RuntimeException("报销单不存在");
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

        // 3. 调用保存逻辑重新插入最新数据
        return saveReimbursement(params);
    }

    // ===== 私有辅助方法 =====

    /**
     * 解析主单数据
     */
    private FkReimMain parseMain(Map<String, Object> data) {
        FkReimMain fkReimMain = new FkReimMain();
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
        return fkReimMain;
    }

    /**
     * 解析行程数据
     */
    private FkReimItinerary parseItinerary(Map<String, Object> data) {
        FkReimItinerary it = new FkReimItinerary();
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
        return it;
    }

    /**
     * 校验行程是否重复（同一出行人+同一天数区间）
     */
    private void validateItineraryDuplicate(List<FkReimItinerary> itineraries) {
        Set<String> keySet = new HashSet<>();
        for (FkReimItinerary it : itineraries) {
            String key = it.getTravelerId() + "_" + it.getDepartureDate() + "_" + it.getArrivalDate();
            if (!keySet.add(key)) {
                throw new RuntimeException("出行人[" + it.getTravelerName() + "]在日期[" + it.getDepartureDate() + "~" + it.getArrivalDate() + "]存在重复行程");
            }
        }
    }

    /**
     * 计算单个行程的补助金额及每日日历
     */
    private Map<String, Object> calculateSubsidy(FkReimItinerary it, String businessTypeId, String businessTypeNo, String businessTypeName) {
        // 步骤1: 计算出差天数
        LocalDate start = LocalDate.parse(it.getDepartureDate());
        LocalDate end = LocalDate.parse(it.getArrivalDate());
        if (end.isBefore(start)) {
            throw new RuntimeException("到达日期不能早于出发日期");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;

        // 步骤2: 根据城市类型确定补助标准
        String cityType = getCityTypeByNo(it.getArrivingCityNo());
        String mealStandard = CITY_MEAL_STANDARD.getOrDefault(cityType, "50");

        // 步骤3: 计算各项补助总额
        BigDecimal mealTotal = new BigDecimal(mealStandard).multiply(BigDecimal.valueOf(days));
        BigDecimal trafficTotal = new BigDecimal(TRAFFIC_STANDARD).multiply(BigDecimal.valueOf(days));
        BigDecimal commTotal = new BigDecimal(COMMUNICATION_STANDARD).multiply(BigDecimal.valueOf(days));
        BigDecimal totalAmount = mealTotal.add(trafficTotal).add(commTotal);

        // 步骤4: 封装补助实体对象
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

        // 步骤5: 生成每日补助日历（按天拆分）
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

        Map<String, Object> result = new HashMap<>();
        result.put("subsidy", subsidy);
        result.put("calendars", calendars);
        return result;
    }

    /**
     * 根据城市编号获取城市类型（一线/二线/三线）
     */
    private String getCityTypeByNo(String cityNo) {
        Map<String, String> cityTypeMap = Map.of(
                "10119", "1", "10621", "1", "10458", "2", "10216", "2", "10455", "3"
        );
        return cityTypeMap.getOrDefault(cityNo, "3");
    }
}
