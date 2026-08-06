package com.aximly.retailsync_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Stock")
public class Stock {

    @Id
    @Column(name = "stock_id")
    private Double stockId;

    @Column(name = "Barcode")
    private String barcode;

    @Column(name = "description")
    private String description;

    @Column(name = "sell")
    private Double sellPrice;

    @Column(name = "cost")
    private Double costPrice;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "dept_id")
    private Integer deptId;
}