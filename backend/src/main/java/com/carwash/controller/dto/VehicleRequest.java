package com.carwash.controller.dto;

import com.carwash.constant.RegexConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VehicleRequest {

    @NotBlank(message = "車牌不可為空")
    @Pattern(regexp = RegexConstants.PLATE_NUMBER_REGEX, message = "車牌格式不正確，例如 ABC-1234、AB-1234、1234-AB")
    private String plateNumber;

    private String brand;
    private String model;
    private String color;
    private String carType;
}
