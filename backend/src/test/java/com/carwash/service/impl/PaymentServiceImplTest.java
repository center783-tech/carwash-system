package com.carwash.service.impl;

import com.carwash.constant.PaymentMethod;
import com.carwash.constant.PaymentStatus;
import com.carwash.constant.ReservationStatus;
import com.carwash.entity.Payment;
import com.carwash.entity.Reservation;
import com.carwash.exception.BusinessException;
import com.carwash.repository.PaymentRepository;
import com.carwash.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private ReservationRepository reservationRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void createPayment_success() {
        Reservation reservation = Reservation.builder().id(1L).totalAmount(new BigDecimal("300")).build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.createPayment(1L, PaymentMethod.CREDIT_CARD);

        assertThat(result.getAmount()).isEqualByComparingTo("300");
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(result.getTransactionNo()).isNotBlank();
    }

    @Test
    void markPaid_updatesPaymentAndReservationStatus() {
        Reservation reservation = Reservation.builder().id(1L).status(ReservationStatus.PENDING).build();
        Payment payment = Payment.builder()
                .id(1L).reservation(reservation).paymentStatus(PaymentStatus.UNPAID)
                .transactionNo("tx-123").build();

        when(paymentRepository.findByTransactionNo("tx-123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.markPaid("tx-123");

        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(result.getPaidAt()).isNotNull();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void markPaid_transactionNotFound_throwsBusinessException() {
        when(paymentRepository.findByTransactionNo("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.markPaid("unknown"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("找不到對應的交易紀錄");
    }

    @Test
    void markFailed_updatesStatusToFailed() {
        Payment payment = Payment.builder().id(1L).paymentStatus(PaymentStatus.UNPAID)
                .transactionNo("tx-456").build();
        when(paymentRepository.findByTransactionNo("tx-456")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.markFailed("tx-456");

        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }
}
