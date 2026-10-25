package com.carwash.service.impl;

import com.carwash.config.NewebPayProperties;
import com.carwash.constant.PaymentMethod;
import com.carwash.constant.PaymentStatus;
import com.carwash.constant.ReservationStatus;
import com.carwash.entity.Payment;
import com.carwash.entity.Reservation;
import com.carwash.exception.BusinessException;
import com.carwash.repository.PaymentNotificationRepository;
import com.carwash.repository.PaymentRepository;
import com.carwash.repository.ReservationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 用跟藍新相同的加密/簽章方式組一筆通知，驗證 handleNotify 的處理流程 */
class NewebPayServiceImplTest {

    private static final String KEY = "12345678901234567890123456789012"; // 32
    private static final String IV = "1234567890123456"; // 16

    private PaymentRepository paymentRepository;
    private PaymentNotificationRepository notificationRepository;
    private ReservationRepository reservationRepository;
    private NewebPayServiceImpl service;

    @BeforeEach
    void setUp() {
        NewebPayProperties props = new NewebPayProperties();
        props.setMerchantId("MS123");
        props.setHashKey(KEY);
        props.setHashIv(IV);
        paymentRepository = mock(PaymentRepository.class);
        notificationRepository = mock(PaymentNotificationRepository.class);
        reservationRepository = mock(ReservationRepository.class);
        TransactionTemplate tx = mock(TransactionTemplate.class);
        when(tx.execute(any())).thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        service = new NewebPayServiceImpl(props, paymentRepository, notificationRepository,
                reservationRepository, tx, new ObjectMapper());
    }

    private String encrypt(String plain) throws Exception {
        // 藍新用 32 bytes 區塊的 PKCS7 補位
        byte[] src = plain.getBytes(StandardCharsets.UTF_8);
        int pad = 32 - src.length % 32;
        byte[] padded = java.util.Arrays.copyOf(src, src.length + pad);
        java.util.Arrays.fill(padded, src.length, padded.length, (byte) pad);
        Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY.getBytes(), "AES"), new IvParameterSpec(IV.getBytes()));
        return HexFormat.of().formatHex(c.doFinal(padded));
    }

    private String sha(String tradeInfo) throws Exception {
        String s = "HashKey=" + KEY + "&" + tradeInfo + "&HashIV=" + IV;
        return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes()));
    }

    private String notifyBody(String json, String shaOverride) throws Exception {
        String info = encrypt(json);
        return "Status=SUCCESS&MerchantID=MS123&Version=2.0&TradeInfo=" + URLEncoder.encode(info, StandardCharsets.UTF_8)
                + "&TradeSha=" + (shaOverride != null ? shaOverride : sha(info));
    }

    private Payment pendingPayment() {
        Reservation reservation = Reservation.builder().id(1L).status(ReservationStatus.PENDING).build();
        Payment payment = Payment.builder().id(9L).reservation(reservation).amount(new BigDecimal("2500.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD).paymentStatus(PaymentStatus.UNPAID)
                .merchantOrderNo("CW1").build();
        when(paymentRepository.findByMerchantOrderNo("CW1")).thenReturn(Optional.of(payment));
        when(paymentRepository.findById(9L)).thenReturn(Optional.of(payment));
        return payment;
    }

    private static final String SUCCESS_JSON = "{\"Status\":\"SUCCESS\",\"Message\":\"授權成功\",\"Result\":"
            + "{\"MerchantID\":\"MS123\",\"Amt\":2500,\"TradeNo\":\"T123\",\"MerchantOrderNo\":\"CW1\","
            + "\"PaymentType\":\"CREDIT\"}}";

    @Test
    void notify_success_marksPaidAndConfirmsReservation() throws Exception {
        Payment payment = pendingPayment();

        service.handleNotify(notifyBody(SUCCESS_JSON, null));

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getProviderTradeNo()).isEqualTo("T123");
        assertThat(payment.getReservation().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void notify_stringFormat_marksPaid() throws Exception {
        Payment payment = pendingPayment();
        String plain = "Status=SUCCESS&Message=%E6%8E%88%E6%AC%8A%E6%88%90%E5%8A%9F&MerchantID=MS123&Amt=2500"
                + "&TradeNo=T123&MerchantOrderNo=CW1&RespondType=String&PaymentType=CREDIT&PayTime=2026-10-26+02%3A17%3A17";

        service.handleNotify(notifyBody(plain, null));

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getReservation().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void notify_duplicate_isIgnored() throws Exception {
        Payment payment = pendingPayment();
        payment.setPaymentStatus(PaymentStatus.PAID);

        service.handleNotify(notifyBody(SUCCESS_JSON, null));

        assertThat(payment.getReservation().getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void notify_badSignature_rejected() throws Exception {
        pendingPayment();

        assertThatThrownBy(() -> service.handleNotify(notifyBody(SUCCESS_JSON, "BAD")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("TradeSha");
    }

    @Test
    void notify_amountMismatch_notPaid() throws Exception {
        Payment payment = pendingPayment();

        service.handleNotify(notifyBody(SUCCESS_JSON.replace("2500", "1"), null));

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }
}
