package com.carwash.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ZoneRequest {

    @NotBlank(message = "區域名稱不可為空")
    @Size(max = 50, message = "區域名稱最多 50 字")
    private String name;

    @NotNull(message = "車位數不可為空")
    @Min(value = 1, message = "車位數至少 1 個")
    @Max(value = 200, message = "車位數最多 200 個")
    private Integer capacity;

    /** 1 = 啟用、0 = 停用；新增時不填視為啟用 */
    private Integer status;
}
