package com.carwash.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmployeeRequest {

    @NotBlank(message = "員工姓名不可為空")
    private String name;

    private String phone;
}
