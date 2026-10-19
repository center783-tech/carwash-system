package com.carwash.repository;

import com.carwash.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByReservationId(Long reservationId);

    Optional<Payment> findByTransactionNo(String transactionNo);
}
