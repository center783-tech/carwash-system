package com.carwash.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceItemRequest {

    @NotBlank(message = "服務名稱不可為空")
    private String name;

    private String description;

    @NotNull(message = "價格不可為空")
    @DecimalMin(value = "0.0", inclusive = false, message = "價格須大於 0")
    private BigDecimal price;

    @NotNull(message = "服務時長不可為空")
    @Positive(message = "服務時長須大於 0")
    private Integer durationMinutes;

    private String category;

    /** 先呼叫 POST /api/files/service-images 上傳拿到 URL，再把 URL 填進這裡 */
    private String imageUrl;
}
