package com.carwash.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "預約 id 不可為空")
    private Long reservationId;

    @NotNull(message = "會員 id 不可為空")
    private Long memberId;

    @NotNull(message = "評分不可為空")
    @Min(value = 1, message = "評分最低為 1")
    @Max(value = 5, message = "評分最高為 5")
    private Integer rating;

    private String comment;

    /** 選填，先呼叫 POST /api/files/review-images 上傳拿到 URL，再把 URL 填進這裡 */
    private String imageUrl;
}
