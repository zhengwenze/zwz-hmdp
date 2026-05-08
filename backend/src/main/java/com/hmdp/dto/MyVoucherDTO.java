package com.hmdp.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MyVoucherDTO {
    private Long orderId;
    private Long voucherId;
    private Long shopId;
    private String shopName;
    private String title;
    private String subTitle;
    private String rules;
    private Long payValue;
    private Long actualValue;
    private Integer type;
    private Integer stock;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private Integer orderStatus;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime useTime;
    private LocalDateTime updateTime;
}
