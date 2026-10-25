package com.carwash.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ClosedDateBatchRequest {

    @NotEmpty(message = "請至少選擇一天")
    private List<LocalDate> dates;

    @Size(max = 100, message = "備註最多 100 字")
    private String reason;
}
