package com.carwash.controller.dto;

import com.carwash.constant.DiscountType;
import com.carwash.entity.MemberCoupon;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MemberCouponResponse {

    private Long id;
    private String couponCode;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minAmount;
    private LocalDate endDate;
    private Integer isUsed;
    private LocalDateTime usedAt;
    private LocalDateTime receivedAt;

    public static MemberCouponResponse from(MemberCoupon mc) {
        return new MemberCouponResponse(
                mc.getId(),
                mc.getCoupon().getCode(),
                mc.getCoupon().getDiscountType(),
                mc.getCoupon().getDiscountValue(),
                mc.getCoupon().getMinAmount(),
                mc.getCoupon().getEndDate(),
                mc.getIsUsed(),
                mc.getUsedAt(),
                mc.getReceivedAt()
        );
    }
}
