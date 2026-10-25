package com.carwash.service.impl;

import com.carwash.entity.Coupon;
import com.carwash.entity.Member;
import com.carwash.entity.MemberCoupon;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.CouponRepository;
import com.carwash.repository.MemberCouponRepository;
import com.carwash.repository.MemberRepository;
import com.carwash.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;
    private final MemberCouponRepository memberCouponRepository;

    @Override
    public Coupon create(Coupon coupon) {
        coupon.setStatus(1);
        return couponRepository.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Coupon> listActive() {
        return couponRepository.findByStatusAndEndDateGreaterThanEqual(1, LocalDate.now());
    }

    @Override
    public MemberCoupon issueToMember(Long memberId, Long couponId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> ResourceNotFoundException.of("Member", memberId));
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> ResourceNotFoundException.of("Coupon", couponId));

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .member(member)
                .coupon(coupon)
                .isUsed(0)
                .build();
        return memberCouponRepository.save(memberCoupon);
    }

    @Override
    public List<MemberCoupon> listMemberCoupons(Long memberId) {
        // 排程之外，查詢時也順手結算，避免排程間隔內畫面還顯示可出示
        expireOverdueRedemptions();
        return memberCouponRepository.findByMemberId(memberId);
    }

    @Override
    public MemberCoupon startRedeem(Long memberCouponId, Long memberId) {
        expireOverdueRedemptions();

        MemberCoupon mc = memberCouponRepository.findById(memberCouponId)
                .orElseThrow(() -> ResourceNotFoundException.of("MemberCoupon", memberCouponId));
        if (!mc.getMember().getId().equals(memberId)) {
            throw new BusinessException("這不是您的優惠券");
        }
        if (mc.getIsUsed() == 1) {
            throw new BusinessException("此優惠券已被使用");
        }
        if (mc.getRedeemStartedAt() != null) {
            return mc; // 出示中，沿用原本的倒數
        }
        Coupon coupon = mc.getCoupon();
        LocalDate today = LocalDate.now();
        if (coupon.getStatus() == 0 || coupon.getEndDate().isBefore(today)) {
            throw new BusinessException("優惠券已失效");
        }
        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(today)) {
            throw new BusinessException("優惠券尚未生效");
        }
        mc.setRedeemStartedAt(LocalDateTime.now());
        return memberCouponRepository.save(mc);
    }

    @Override
    public int expireOverdueRedemptions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(REDEEM_MINUTES);
        List<MemberCoupon> overdue = memberCouponRepository.findByIsUsedAndRedeemStartedAtBefore(0, cutoff);
        for (MemberCoupon mc : overdue) {
            mc.setIsUsed(1);
            mc.setUsedAt(mc.getRedeemStartedAt().plusMinutes(REDEEM_MINUTES));
        }
        memberCouponRepository.saveAll(overdue);
        return overdue.size();
    }
}
