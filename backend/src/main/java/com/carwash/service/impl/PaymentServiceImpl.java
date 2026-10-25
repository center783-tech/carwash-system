package com.carwash.service.impl;

import com.carwash.constant.PaymentMethod;
import com.carwash.constant.PaymentStatus;
import com.carwash.constant.ReservationStatus;
import com.carwash.entity.Payment;
import com.carwash.entity.Reservation;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.PaymentRepository;
import com.carwash.repository.ReservationRepository;
import com.carwash.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    @Override
    public Payment createPayment(Long reservationId, PaymentMethod method) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Reservation", reservationId));

        Payment payment = Payment.builder()
                .reservation(reservation)
                .amount(reservation.getTotalAmount())
                .paymentMethod(method)
                .paymentStatus(PaymentStatus.UNPAID)
                .transactionNo(UUID.randomUUID().toString())
                .build();
        return paymentRepository.save(payment);
    }

    @Override
    public Payment createCashPayment(Long reservationId, Long memberId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Reservation", reservationId));
        if (!reservation.getMember().getId().equals(memberId)) {
            throw new BusinessException("無權限操作此預約");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("此預約目前狀態無法選擇現場付現");
        }
        return findOrCreateUnpaidCash(reservation);
    }

    @Override
    public Payment collectCash(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Reservation", reservationId));
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("已取消的預約無法收款");
        }
        if (reservation.getPayments().stream().anyMatch(p -> p.getPaymentStatus() == PaymentStatus.PAID)) {
            throw new BusinessException("此預約已完成付款");
        }
        Payment payment = findOrCreateUnpaidCash(reservation);
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);
        }
        return paymentRepository.save(payment);
    }

    private Payment findOrCreateUnpaidCash(Reservation reservation) {
        return reservation.getPayments().stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.CASH && p.getPaymentStatus() == PaymentStatus.UNPAID)
                .findFirst()
                .orElseGet(() -> paymentRepository.save(Payment.builder()
                        .reservation(reservation)
                        .amount(reservation.getTotalAmount())
                        .paymentMethod(PaymentMethod.CASH)
                        .paymentStatus(PaymentStatus.UNPAID)
                        .build()));
    }

    @Override
    public Payment markPaid(String transactionNo) {
        Payment payment = paymentRepository.findByTransactionNo(transactionNo)
                .orElseThrow(() -> new BusinessException("找不到對應的交易紀錄"));
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());

        Reservation reservation = payment.getReservation();
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        return paymentRepository.save(payment);
    }

    @Override
    public Payment markFailed(String transactionNo) {
        Payment payment = paymentRepository.findByTransactionNo(transactionNo)
                .orElseThrow(() -> new BusinessException("找不到對應的交易紀錄"));
        payment.setPaymentStatus(PaymentStatus.FAILED);
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> listByReservation(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId);
    }
}
