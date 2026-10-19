package com.carwash.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignServicesRequest {

    @NotEmpty(message = "請至少選擇一個服務項目")
    private List<Long> serviceIds;
}
