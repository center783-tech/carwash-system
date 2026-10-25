package com.carwash.controller.dto;

import com.carwash.constant.DiscountType;
import com.carwash.entity.MemberCoupon;
import com.carwash.service.CouponService;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Duration;
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

    /** 現場出示中時，距離自動變成已使用還剩幾秒；沒有在出示則為 null（前端用它倒數，避免裝置與伺服器時間不一致） */
    private Long redeemRemainingSeconds;

    /** 僅發放時使用：是否已成功寄出通知信，其他查詢回傳 null */
    private Boolean emailSent;

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
                mc.getReceivedAt(),
                remainingSeconds(mc),
                null
        );
    }

    private static Long remainingSeconds(MemberCoupon mc) {
        if (mc.getIsUsed() == 1 || mc.getRedeemStartedAt() == null) return null;
        LocalDateTime expiresAt = mc.getRedeemStartedAt().plusMinutes(CouponService.REDEEM_MINUTES);
        return Math.max(Duration.between(LocalDateTime.now(), expiresAt).getSeconds(), 0);
    }
}
