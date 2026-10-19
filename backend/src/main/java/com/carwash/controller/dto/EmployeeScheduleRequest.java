package com.carwash.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EmployeeScheduleRequest {

    @NotNull(message = "排班日期不可為空")
    private LocalDate workDate;

    @NotNull(message = "開始時間不可為空")
    private LocalTime startTime;

    @NotNull(message = "結束時間不可為空")
    private LocalTime endTime;
}
