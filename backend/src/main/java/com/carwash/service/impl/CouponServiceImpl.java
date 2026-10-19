package com.carwash.service.impl;

import com.carwash.entity.Coupon;
import com.carwash.entity.Member;
import com.carwash.entity.MemberCoupon;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.CouponRepository;
import com.carwash.repository.MemberCouponRepository;
import com.carwash.repository.MemberRepository;
import com.carwash.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    @Transactional(readOnly = true)
    public List<MemberCoupon> listMemberCoupons(Long memberId) {
        return memberCouponRepository.findByMemberId(memberId);
    }
}
