package com.carwash.service.impl;

import com.carwash.entity.Coupon;
import com.carwash.entity.Member;
import com.carwash.entity.MemberCoupon;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.CouponRepository;
import com.carwash.repository.MemberCouponRepository;
import com.carwash.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock private CouponRepository couponRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberCouponRepository memberCouponRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    @Test
    void issueToMember_success() {
        Member member = Member.builder().id(1L).username("member1").build();
        Coupon coupon = Coupon.builder().id(1L).code("WELCOME100").build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.save(any(MemberCoupon.class))).thenAnswer(inv -> inv.getArgument(0));

        MemberCoupon result = couponService.issueToMember(1L, 1L);

        assertThat(result.getMember()).isEqualTo(member);
        assertThat(result.getCoupon()).isEqualTo(coupon);
        assertThat(result.getIsUsed()).isEqualTo(0);
    }

    @Test
    void issueToMember_memberNotFound_throwsResourceNotFoundException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issueToMember(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void issueToMember_couponNotFound_throwsResourceNotFoundException() {
        Member member = Member.builder().id(1L).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issueToMember(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_setsStatusActive() {
        Coupon coupon = Coupon.builder().code("NEW").build();
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        Coupon result = couponService.create(coupon);

        assertThat(result.getStatus()).isEqualTo(1);
    }
}
