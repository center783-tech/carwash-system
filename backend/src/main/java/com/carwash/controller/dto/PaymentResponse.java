package com.carwash.controller.dto;

import com.carwash.constant.PaymentMethod;
import com.carwash.constant.PaymentStatus;
import com.carwash.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long reservationId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String transactionNo;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getReservation().getId(),
                p.getAmount(),
                p.getPaymentMethod(),
                p.getPaymentStatus(),
                p.getTransactionNo(),
                p.getPaidAt(),
                p.getCreatedAt()
        );
    }
}
