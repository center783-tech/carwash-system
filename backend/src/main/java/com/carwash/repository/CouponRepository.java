package com.carwash.repository;

import com.carwash.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    List<Coupon> findByStatusAndEndDateGreaterThanEqual(Integer status, LocalDate today);
}
