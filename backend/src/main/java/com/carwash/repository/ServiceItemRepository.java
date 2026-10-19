package com.carwash.repository;

import com.carwash.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    List<ServiceItem> findByStatus(Integer status);

    List<ServiceItem> findByCategory(String category);

    List<ServiceItem> findByCategoryAndStatus(String category, Integer status);
}
