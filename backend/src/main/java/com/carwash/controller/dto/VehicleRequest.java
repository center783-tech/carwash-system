package com.carwash.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VehicleRequest {

    @NotBlank(message = "車牌不可為空")
    private String plateNumber;

    private String brand;
    private String model;
    private String color;
    private String carType;
}
