package com.carwash.entity;

import com.carwash.constant.PaymentMethod;
import com.carwash.constant.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    /** 金流商，見 PaymentProviders；現金付款為 NULL */
    @Column(length = 20)
    private String provider;

    /** 送給金流商的商店訂單編號（藍新 MerchantOrderNo，最長 30 字元） */
    @Column(name = "merchant_order_no", length = 30, unique = true)
    private String merchantOrderNo;

    /** 金流商回傳的交易序號（藍新 TradeNo） */
    @Column(name = "provider_trade_no", length = 50)
    private String providerTradeNo;

    /** 第三方金流交易序號 */
    @Column(name = "transaction_no", length = 100)
    private String transactionNo;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
