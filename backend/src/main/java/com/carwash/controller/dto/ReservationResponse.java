package com.carwash.controller.dto;

import com.carwash.constant.ReservationStatus;
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
                r.getCreatedAt()
        );
    }
}
