package com.aximly.retailsync_api.repository;

import com.aximly.retailsync_api.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepository extends JpaRepository<Stock, Double> {
}