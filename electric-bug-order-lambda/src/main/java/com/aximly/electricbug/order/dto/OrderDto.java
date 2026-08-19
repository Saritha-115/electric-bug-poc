package com.aximly.electricbug.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Integer orderId;
    private Integer revision;
    private LocalDateTime orderDate;
    private LocalDateTime dueDate;
    private Integer staffId;
    private Integer supplierId;
    private String orderSuffix;
    private String comments;
    private Boolean archive;
}