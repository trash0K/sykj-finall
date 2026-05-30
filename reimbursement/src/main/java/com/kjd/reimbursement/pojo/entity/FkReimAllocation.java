package com.kjd.reimbursement.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("fk_reim_allocation")
public class FkReimAllocation implements Serializable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String mainId;
    private String attributionId;
    private String attributionName;
    private String projectId;
    private String projectNo;
    private String projectName;
    private String allocationRatio;
    private String allocationAmount;
}
