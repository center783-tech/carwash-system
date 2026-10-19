package com.carwash.controller.dto;

import com.carwash.constant.DiscountType;
import com.carwash.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class CouponResponse {

    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer usageLimit;
    private Integer status;

    public static CouponResponse from(Coupon c) {
        return new CouponResponse(
                c.getId(), c.getCode(), c.getDiscountType(), c.getDiscountValue(),
                c.getMinAmount(), c.getStartDate(), c.getEndDate(), c.getUsageLimit(), c.getStatus()
        );
    }
}
