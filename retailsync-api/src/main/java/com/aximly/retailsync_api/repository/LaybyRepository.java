package com.aximly.retailsync_api.repository;

import com.aximly.retailsync_api.model.Layby;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LaybyRepository extends JpaRepository<Layby, Integer> {
    List<Layby> findByClosed(Boolean closed);
}