package com.carwash.controller.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClosedDateReasonRequest {

    @Size(max = 100, message = "備註最多 100 字")
    private String reason;
}
