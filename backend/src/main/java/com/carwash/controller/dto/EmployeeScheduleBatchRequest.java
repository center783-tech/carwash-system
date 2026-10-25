package com.carwash.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class EmployeeScheduleBatchRequest {

    @NotEmpty(message = "請至少選擇一天")
    @Size(max = 62, message = "一次最多排 62 天")
    private List<LocalDate> workDates;

    @NotNull(message = "開始時間不可為空")
    private LocalTime startTime;

    @NotNull(message = "結束時間不可為空")
    private LocalTime endTime;
}
