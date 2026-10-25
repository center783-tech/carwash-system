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

/** 金流 NotifyURL 的通知紀錄（含原始內容），方便對帳與除錯 */
@Entity
@Table(name = "payment_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 驗證失敗或找不到付款單時為 NULL */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "merchant_order_no", length = 30)
    private String merchantOrderNo;

    @Column(name = "raw_body", columnDefinition = "TEXT")
    private String rawBody;

    /** 見 NotifyStatus */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 255)
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
