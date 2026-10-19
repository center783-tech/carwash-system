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
}
