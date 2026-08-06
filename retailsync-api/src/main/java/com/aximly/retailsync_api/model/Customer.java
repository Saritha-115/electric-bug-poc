package com.aximly.retailsync_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Customer")
public class Customer {

    @Id
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "given_names")
    private String firstname;

    @Column(name = "surname")
    private String lastname;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    // No 'address' column on Customer itself — lives in CustomerAddress table.
    // Dropped for now; see note below if you need it later.
}