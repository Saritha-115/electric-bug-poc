package com.aximly.retailsync_api.repository;

import com.aximly.retailsync_api.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByDocketId(Integer docketId);
}