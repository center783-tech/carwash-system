package com.carwash.controller.dto;

import com.carwash.constant.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CouponRequest {

    @NotBlank(message = "優惠券代碼不可為空")
    private String code;

    @NotNull(message = "折扣類型不可為空")
    private DiscountType discountType;

    @NotNull(message = "折扣數值不可為空")
    @DecimalMin(value = "0.0", inclusive = false, message = "折扣數值須大於 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", message = "最低使用金額不可為負數")
    private BigDecimal minAmount = BigDecimal.ZERO;

    @NotNull(message = "生效日期不可為空")
    private LocalDate startDate;

    @NotNull(message = "到期日期不可為空")
    private LocalDate endDate;

    /** 不限使用次數時可留空 */
    private Integer usageLimit;
}
