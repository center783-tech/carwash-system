package com.carwash.repository;

import com.carwash.entity.PaymentNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentNotificationRepository extends JpaRepository<PaymentNotification, Long> {
}
