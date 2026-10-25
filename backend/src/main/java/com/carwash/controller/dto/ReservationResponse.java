package com.carwash.controller.dto;

import com.carwash.constant.PaymentMethod;
import com.carwash.constant.PaymentStatus;
import com.carwash.constant.ReservationStatus;
import com.carwash.entity.Payment;
import com.carwash.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private Long memberId;
    private String memberName;
    private Long vehicleId;
    private String plateNumber;
    private Long serviceId;
    private String serviceName;
    private Long employeeId;
    private String employeeName;
    private String zoneName;
    private LocalDate reservationDate;
    private LocalTime timeSlotStart;
    private LocalTime timeSlotEnd;
    private ReservationStatus status;
    private BigDecimal totalAmount;
    private String couponCode;
    private String remark;
    private LocalDateTime createdAt;
    /** 付款狀態：PAID 已付款、REFUNDED 已退款、其餘（含尚未建立付款單）為 UNPAID */
    private PaymentStatus paymentStatus;
    /** 已付款時的付款方式；未付款但選了現場付現為 CASH，其餘為 NULL */
    private PaymentMethod paymentMethod;

    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getMember().getId(),
                r.getMember().getRealName() != null ? r.getMember().getRealName() : r.getMember().getUsername(),
                r.getVehicle().getId(),
                r.getVehicle().getPlateNumber(),
                r.getServiceItem().getId(),
                r.getServiceItem().getName(),
                r.getEmployee() != null ? r.getEmployee().getId() : null,
                r.getEmployee() != null ? r.getEmployee().getName() : null,
                r.getZone() != null ? r.getZone().getName() : null,
                r.getReservationDate(),
                r.getTimeSlotStart(),
                r.getTimeSlotEnd(),
                r.getStatus(),
                r.getTotalAmount(),
                r.getCoupon() != null ? r.getCoupon().getCode() : null,
                r.getRemark(),
                r.getCreatedAt(),
                summarizePaymentStatus(r),
                paidMethod(r)
        );
    }

    private static PaymentStatus summarizePaymentStatus(Reservation r) {
        boolean refunded = false;
        for (Payment p : r.getPayments()) {
            if (p.getPaymentStatus() == PaymentStatus.PAID) {
                return PaymentStatus.PAID;
            }
            refunded |= p.getPaymentStatus() == PaymentStatus.REFUNDED;
        }
        return refunded ? PaymentStatus.REFUNDED : PaymentStatus.UNPAID;
    }

    /** 已付款 → 實際付款方式；未付款但選了現場付現 → CASH；其餘 NULL */
    private static PaymentMethod paidMethod(Reservation r) {
        return r.getPayments().stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID)
                .map(Payment::getPaymentMethod)
                .findFirst()
                .orElseGet(() -> r.getPayments().stream()
                        .filter(p -> p.getPaymentStatus() == PaymentStatus.UNPAID && p.getPaymentMethod() == PaymentMethod.CASH)
                        .map(Payment::getPaymentMethod)
                        .findFirst().orElse(null));
    }
}
