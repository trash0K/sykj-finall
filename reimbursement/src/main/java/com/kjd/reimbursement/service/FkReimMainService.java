package com.kjd.reimbursement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kjd.reimbursement.pojo.entity.FkReimMain;
import com.kjd.reimbursement.pojo.vo.PageResult;
import java.util.Map;

public interface FkReimMainService extends IService<FkReimMain> {

    /**
     * 获取报销单列表
     * @param page
     * @param size
     * @param id
     * @param reimbursementTitle
     * @param reimburserName
     * @param reimDepartmentName
     * @param reimCompanyName
     * @param businessTripReason
     * @param businessTypeName
     * @return
     */
    PageResult getReimbursementList(Integer page, Integer size, String id, String reimbursementTitle, 
                                    String reimburserName, String reimDepartmentName, String reimCompanyName,
                                    String businessTripReason, String businessTypeName);

    /**
     * 获取报销单详情
     * @param id
     * @return
     */
    Map<String, Object> getReimbursementDetail(String id);

    /**
     * 保存报销单
     * @param params
     * @return
     */
    String saveReimbursement(Map<String, Object> params);

    /**
     * 删除报销单
     * @param id
     */
    void deleteReimbursement(String id);

    /**
     * 更新报销单
     * @param params
     * @return
     */
    String updateReimbursement(Map<String, Object> params);
}
