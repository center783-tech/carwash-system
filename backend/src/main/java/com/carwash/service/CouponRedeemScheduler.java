package com.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定時把「現場使用」超過 10 分鐘的優惠券轉成已使用 */
@Component
@RequiredArgsConstructor
public class CouponRedeemScheduler {

    private final CouponService couponService;

    @Scheduled(fixedDelay = 30_000)
    public void expireOverdueRedemptions() {
        couponService.expireOverdueRedemptions();
    }
}
