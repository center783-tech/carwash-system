package com.carwash.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ClosedDateRequest {

    @NotNull(message = "休息日不可為空")
    private LocalDate closedDate;

    @Size(max = 100, message = "原因最多 100 字")
    private String reason;
}
