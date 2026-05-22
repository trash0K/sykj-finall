package com.kjd.reimbursement.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("fk_reim_main")
public class FkReimMain implements Serializable {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;//报销单号

    private String creationTime;//创建时间
    private String reimbursementTitle;//报销标题
    private String reimburserId;//报销人
    private String reimburserNo;//报销人编号
    private String reimburserName;//报销人名称
    private String reimDepartmentId;//报销部门
    private String reimDepartmentNo;//报销部门编号
    private String reimDepartmentName;//报销部门名称
    private String reimCompanyId;//报销公司
    private String reimCompanyNo;//报销公司编号
    private String reimCompanyName;//报销公司名称
    private String businessTypeId;//业务类型
    private String businessTypeNo;//业务类型编号
    private String businessTypeName;//业务类型名称
    private String businessTripReason;//业务出差事由
    private String subsidyTotal;//补贴总额
    private String mealAllowance;//餐饮补贴
    private String transportationAllowance;//交通补贴
    private String phoneAllowance;//通讯补贴
    private String remarks;//备注
}