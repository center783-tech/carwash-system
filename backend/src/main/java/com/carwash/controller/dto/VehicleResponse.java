package com.carwash.controller.dto;

import com.carwash.entity.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class VehicleResponse {

    private Long id;
    private Long memberId;
    private String plateNumber;
    private String brand;
    private String model;
    private String color;
    private String carType;
    private LocalDateTime createdAt;

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getMember().getId(),
                vehicle.getPlateNumber(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getColor(),
                vehicle.getCarType(),
                vehicle.getCreatedAt()
        );
    }
}
