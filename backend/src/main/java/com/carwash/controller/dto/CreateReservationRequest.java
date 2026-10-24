package com.carwash.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateReservationRequest {

    @NotNull
    private Long memberId;

    @NotNull
    private Long vehicleId;

    @NotNull
    private Long serviceId;

    /** 可為 null，後台再指派員工 */
    private Long employeeId;

    @NotNull(message = "請選擇車位區")
    private Long zoneId;

    @NotNull
    private LocalDate reservationDate;

    @NotNull
    private LocalTime timeSlotStart;

    /** member_coupons.id，可為 null */
    private Long couponId;

    private String remark;
}
