package com.carwash.repository;

import com.carwash.entity.MemberCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    List<MemberCoupon> findByMemberId(Long memberId);

    List<MemberCoupon> findByMemberIdAndIsUsed(Long memberId, Integer isUsed);

    /** 現場使用已超過時限、還沒被轉成已使用的優惠券 */
    List<MemberCoupon> findByIsUsedAndRedeemStartedAtBefore(Integer isUsed, java.time.LocalDateTime cutoff);
}
