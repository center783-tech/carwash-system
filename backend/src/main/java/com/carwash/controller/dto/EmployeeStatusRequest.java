package com.carwash.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmployeeStatusRequest {

    /** 1 = 在職（可被指派）、0 = 停用（離職或暫停，不能再被指派） */
    @NotNull(message = "狀態不可為空")
    @Min(0)
    @Max(1)
    private Integer status;
}
