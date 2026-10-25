package com.carwash.repository;

import com.carwash.constant.PaymentStatus;
import com.carwash.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByReservationId(Long reservationId);

    Optional<Payment> findByTransactionNo(String transactionNo);

    Optional<Payment> findByMerchantOrderNo(String merchantOrderNo);

    /** 某會員在指定時間範圍內、仍未付款的金流付款單（供補查交易狀態） */
    @Query("select p from Payment p where p.reservation.member.id = :memberId "
            + "and p.provider = :provider and p.paymentStatus = :status "
            + "and p.createdAt between :from and :to order by p.createdAt desc")
    List<Payment> findPendingByMember(@Param("memberId") Long memberId,
                                      @Param("provider") String provider,
                                      @Param("status") PaymentStatus status,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);
}
