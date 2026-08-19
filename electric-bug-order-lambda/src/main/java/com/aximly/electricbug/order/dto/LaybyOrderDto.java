package com.aximly.electricbug.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LaybyOrderDto {
    private Integer laybyId;
    private LocalDateTime laybyDate;
    private Integer customerId;
    private Double totalInc;
    private Boolean closed;
    private String comments;
}