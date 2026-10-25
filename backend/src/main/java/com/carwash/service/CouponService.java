package com.carwash.service;

import com.carwash.entity.Coupon;
import com.carwash.entity.MemberCoupon;

import java.util.List;

public interface CouponService {

    Coupon create(Coupon coupon);

    List<Coupon> listActive();

    /** 後台發放優惠券給會員 */
    MemberCoupon issueToMember(Long memberId, Long couponId);

    List<MemberCoupon> listMemberCoupons(Long memberId);

    /** 現場使用的出示時限（分鐘），時間到自動變成已使用 */
    int REDEEM_MINUTES = 10;

    /**
     * 會員到店出示優惠券：開始 10 分鐘倒數。已在出示中的再呼叫一次會回傳同一筆（不重新計時），
     * 方便會員關掉畫面後重新打開。
     */
    MemberCoupon startRedeem(Long memberCouponId, Long memberId);

    /** 把現場使用超過 10 分鐘的優惠券轉成已使用，回傳處理筆數 */
    int expireOverdueRedemptions();
}
