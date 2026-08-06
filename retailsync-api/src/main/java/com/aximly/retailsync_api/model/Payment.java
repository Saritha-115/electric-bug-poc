package com.aximly.retailsync_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Payments")
public class Payment {

    @Id
    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "docket_id")
    private Integer docketId;

    @Column(name = "docket_date")
    private LocalDateTime docketDate;

    @Column(name = "paymenttype")
    private String paymentType;

    @Column(name = "amount")
    private Double amount;
}