package com.aximly.retailsync_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "layby")
public class Layby {

    @Id
    @Column(name = "layby_id")
    private Integer laybyId;

    @Column(name = "layby_date")
    private LocalDateTime laybyDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "total_inc")
    private Double totalInc;

    @Column(name = "subtotal")
    private Double subtotal;

    @Column(name = "closed")
    private Boolean closed;

    @Column(name = "comments")
    private String comments;
}