package com.kjd.reimbursement.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("fk_subsidy_calendar")
public class FkSubsidyCalendar implements Serializable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String mainId;
    private String travelDate;
    private String travelDateWeek;
    private String subsidizedCities;
    private String subsidizedCityNumber;
    private String remark;
    private String standardMealExpensesAmount;
    private String standardTrafficAmount;
    private String standardCommunicationAmount;
    private String mealExpensesAmount;
    private String trafficAmount;
    private String communicationAmount;
    private String isReimbursed;
}