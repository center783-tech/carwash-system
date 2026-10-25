package com.carwash.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Integer isUsed = 0;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** 會員按下「現場使用」的時間；超過 10 分鐘會自動轉成已使用 */
    @Column(name = "redeem_started_at")
    private LocalDateTime redeemStartedAt;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false)
    private LocalDateTime receivedAt;
}
