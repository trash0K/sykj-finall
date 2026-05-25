package com.kjd.reimbursement.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("fk_reim_itinerary")
public class FkReimItinerary implements Serializable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String mainId;
    private String travelerId;
    private String travelerNo;
    private String travelerName;
    private String departureDate;
    private String arrivalDate;
    private String departureCity;
    private String departureCityNo;
    private String arrivingCity;
    private String arrivingCityNo;
    private String itineraryInstructions;
}