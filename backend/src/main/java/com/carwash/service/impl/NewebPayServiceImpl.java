package com.carwash.service.impl;

import com.carwash.config.NewebPayProperties;
import com.carwash.constant.NotifyStatus;
import com.carwash.constant.PaymentMethod;
import com.carwash.constant.PaymentProviders;
import com.carwash.constant.PaymentStatus;
import com.carwash.constant.ReservationStatus;
import com.carwash.controller.dto.NewebPayPaymentFormResponse;
import com.carwash.controller.dto.NewebPayTradeQueryResponse;
import com.carwash.entity.Payment;
import com.carwash.entity.PaymentNotification;
import com.carwash.entity.Reservation;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.PaymentNotificationRepository;
import com.carwash.repository.PaymentRepository;
import com.carwash.repository.ReservationRepository;
import com.carwash.service.NewebPayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 藍新金流 MPG（信用卡一次付清）。
 * 流程：建立付款單 → 前端 POST 表單到藍新 → 使用者刷卡 → 藍新 NotifyURL 通知（為準）、ReturnURL 把瀏覽器導回；
 * NotifyURL 沒收到時，可用交易查詢 API 補查。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewebPayServiceImpl implements NewebPayService {

    private static final String MPG_VERSION = "2.0";
    private static final String QUERY_VERSION = "1.3";
    private static final String ORDER_NO_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    /** 補查只看最近幾天內建立的付款單 */
    private static final int STALE_LOOKBACK_DAYS = 5;
    /** ATM 虛擬帳號的繳費期限（天） */
    private static final int ATM_EXPIRE_DAYS = 3;

    private final NewebPayProperties props;
    private final PaymentRepository paymentRepository;
    private final PaymentNotificationRepository notificationRepository;
    private final ReservationRepository reservationRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    private final RestClient restClient = RestClient.create();

    // ------------------------------------------------------------------ 建立付款表單

    @Override
    @Transactional
    public NewebPayPaymentFormResponse createPaymentForm(Long reservationId, Long memberId, PaymentMethod method) {
        if (method != PaymentMethod.CREDIT_CARD && method != PaymentMethod.ATM) {
            throw new BusinessException("不支援的付款方式");
        }
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Reservation", reservationId));
        if (!reservation.getMember().getId().equals(memberId)) {
            throw new BusinessException("無權限為此預約付款");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("此預約目前狀態無法付款");
        }
        boolean alreadyPaid = paymentRepository.findByReservationId(reservationId).stream()
                .anyMatch(p -> p.getPaymentStatus() == PaymentStatus.PAID);
        if (alreadyPaid) {
            throw new BusinessException("此預約已完成付款");
        }
        int amount = toIntAmount(reservation.getTotalAmount());
        if (amount < 1) {
            throw new BusinessException("付款金額必須大於 0");
        }

        // 藍新的 MerchantOrderNo 不能重複，所以每次重新付款都開新的付款單
        Payment payment = paymentRepository.save(Payment.builder()
                .reservation(reservation)
                .amount(reservation.getTotalAmount())
                .paymentMethod(method)
                .paymentStatus(PaymentStatus.UNPAID)
                .provider(PaymentProviders.NEWEBPAY)
                .merchantOrderNo(generateMerchantOrderNo())
                .build());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("MerchantID", props.getMerchantId());
        params.put("RespondType", "JSON");
        params.put("TimeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("Version", MPG_VERSION);
        params.put("MerchantOrderNo", payment.getMerchantOrderNo());
        params.put("Amt", String.valueOf(amount));
        params.put("ItemDesc", reservation.getServiceItem().getName());
        params.put("ReturnURL", props.getReturnUrl());
        params.put("NotifyURL", props.getNotifyUrl());
        params.put("LoginType", "0");
        if (method == PaymentMethod.ATM) {
            params.put("VACC", "1");
            // ATM 虛擬帳號繳費期限；超過就無法繳費（藍新預設 7 天，這裡縮短）
            params.put("ExpireDate", LocalDate.now().plusDays(ATM_EXPIRE_DAYS).format(DateTimeFormatter.BASIC_ISO_DATE));
        } else {
            params.put("CREDIT", "1");
        }
        String email = reservation.getMember().getEmail();
        if (email != null && !email.isBlank()) {
            params.put("Email", email);
        }

        String tradeInfo = encrypt(toQueryString(params));
        return new NewebPayPaymentFormResponse(props.getGatewayUrl(), props.getMerchantId(),
                MPG_VERSION, tradeInfo, tradeSha(tradeInfo));
    }

    // ------------------------------------------------------------------ NotifyURL

    @Override
    public void handleNotify(String rawBody) {
        String merchantOrderNo = null;
        Payment payment = null;
        try {
            MultiValueMap<String, String> form = parseForm(rawBody);
            String tradeInfo = form.getFirst("TradeInfo");
            String tradeSha = form.getFirst("TradeSha");
            if (tradeInfo == null || tradeSha == null) {
                saveNotification(null, null, rawBody, NotifyStatus.INVALID, "缺少 TradeInfo 或 TradeSha");
                throw new BusinessException("通知格式不正確");
            }
            if (!MessageDigest.isEqual(tradeSha(tradeInfo).getBytes(StandardCharsets.UTF_8),
                    tradeSha.toUpperCase().getBytes(StandardCharsets.UTF_8))) {
                saveNotification(null, null, rawBody, NotifyStatus.INVALID, "TradeSha 驗證失敗");
                throw new BusinessException("TradeSha 驗證失敗");
            }

            JsonNode root = objectMapper.readTree(decrypt(tradeInfo));
            JsonNode result = root.path("Result");
            merchantOrderNo = result.path("MerchantOrderNo").asText(null);
            boolean success = "SUCCESS".equals(root.path("Status").asText());
            String message = root.path("Message").asText();

            payment = merchantOrderNo == null ? null
                    : paymentRepository.findByMerchantOrderNo(merchantOrderNo).orElse(null);
            if (payment == null) {
                saveNotification(null, merchantOrderNo, rawBody, NotifyStatus.FAILED, "找不到對應的付款單");
                throw new BusinessException("找不到對應的付款單");
            }

            String outcome = applyNotifyResult(payment.getId(), success, result);
            saveNotification(payment, merchantOrderNo, rawBody, outcome, message);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("處理藍新通知失敗 merchantOrderNo={}", merchantOrderNo, e);
            saveNotification(payment, merchantOrderNo, rawBody, NotifyStatus.FAILED, e.getMessage());
            // 讓藍新收到非 200，稍後重送
            throw new BusinessException("處理藍新通知失敗");
        }
    }

    /** 在單一交易內更新付款單與預約狀態，回傳要記錄的 NotifyStatus */
    private String applyNotifyResult(Long paymentId, boolean success, JsonNode result) {
        return transactionTemplate.execute(tx -> {
            Payment payment = paymentRepository.findById(paymentId).orElseThrow();
            if (payment.getPaymentStatus() == PaymentStatus.PAID) {
                return NotifyStatus.IGNORED; // 重複通知
            }
            if (!success) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                return NotifyStatus.PROCESSED;
            }
            // 金額必須與我們自己建立的付款單一致
            if (result.path("Amt").asInt(-1) != toIntAmount(payment.getAmount())) {
                log.warn("藍新通知金額不符 paymentId={} amt={}", paymentId, result.path("Amt").asText());
                return NotifyStatus.INVALID;
            }
            markPaid(payment, result.path("TradeNo").asText(null));
            return NotifyStatus.PROCESSED;
        });
    }

    // ------------------------------------------------------------------ 交易查詢

    @Override
    public NewebPayTradeQueryResponse queryPaymentStatus(Long paymentId, Long memberId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", paymentId));
        Long ownerId = transactionTemplate.execute(tx ->
                paymentRepository.findById(paymentId).orElseThrow().getReservation().getMember().getId());
        if (!ownerId.equals(memberId) || payment.getMerchantOrderNo() == null) {
            throw new BusinessException("無權限查詢此付款單");
        }
        return queryAndSync(payment);
    }

    @Override
    public List<NewebPayTradeQueryResponse> queryStalePendingPayments(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        List<Payment> pending = paymentRepository.findPendingByMember(memberId, PaymentProviders.NEWEBPAY,
                PaymentStatus.UNPAID, now.minusDays(STALE_LOOKBACK_DAYS), now);
        List<NewebPayTradeQueryResponse> responses = new ArrayList<>();
        for (Payment payment : pending) {
            try {
                responses.add(queryAndSync(payment));
            } catch (Exception e) {
                // 單筆查詢失敗（例如藍新暫時連不上）不影響其他筆
                log.warn("補查藍新交易失敗 paymentId={}", payment.getId(), e);
            }
        }
        return responses;
    }

    private NewebPayTradeQueryResponse queryAndSync(Payment payment) {
        int amount = toIntAmount(payment.getAmount());
        // CheckValue 的參數順序固定為 IV、Amt、MerchantID、MerchantOrderNo、Key
        String checkValue = sha256Upper("IV=" + props.getHashIv() + "&Amt=" + amount
                + "&MerchantID=" + props.getMerchantId()
                + "&MerchantOrderNo=" + payment.getMerchantOrderNo()
                + "&Key=" + props.getHashKey());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("MerchantID", props.getMerchantId());
        body.add("Version", QUERY_VERSION);
        body.add("RespondType", "JSON");
        body.add("CheckValue", checkValue);
        body.add("TimeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        body.add("MerchantOrderNo", payment.getMerchantOrderNo());
        body.add("Amt", String.valueOf(amount));

        String json = restClient.post()
                .uri(props.getQueryUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BusinessException("解析藍新查詢結果失敗");
        }
        JsonNode result = root.path("Result");
        String message = root.path("Message").asText();
        String tradeStatus = result.path("TradeStatus").asText(null);

        if ("SUCCESS".equals(root.path("Status").asText())) {
            if (!verifyQueryCheckCode(result)) {
                throw new BusinessException("藍新查詢結果驗證失敗");
            }
            syncFromTradeStatus(payment.getId(), tradeStatus, result);
        }

        // 同一筆交易取最新狀態回傳
        return transactionTemplate.execute(tx -> {
            Payment latest = paymentRepository.findById(payment.getId()).orElseThrow();
            return new NewebPayTradeQueryResponse(latest.getId(), latest.getMerchantOrderNo(),
                    latest.getPaymentStatus().name(), latest.getReservation().getStatus().name(), tradeStatus,
                    result.path("TradeNo").asText(null), result.path("PaymentType").asText(null), message);
        });
    }

    /** TradeStatus：0 未付款、1 付款成功、2 付款失敗、3 付款取消、6 已退款 */
    private void syncFromTradeStatus(Long paymentId, String tradeStatus, JsonNode result) {
        transactionTemplate.executeWithoutResult(tx -> {
            Payment payment = paymentRepository.findById(paymentId).orElseThrow();
            if (payment.getPaymentStatus() != PaymentStatus.UNPAID) {
                return;
            }
            if ("1".equals(tradeStatus)) {
                if (result.path("Amt").asInt(-1) == toIntAmount(payment.getAmount())) {
                    markPaid(payment, result.path("TradeNo").asText(null));
                }
            } else if ("2".equals(tradeStatus) || "3".equals(tradeStatus)) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
        });
    }

    /** CheckCode = SHA256("HashIV=..&Amt=..&MerchantID=..&MerchantOrderNo=..&TradeNo=..&HashKey=..") 大寫 */
    private boolean verifyQueryCheckCode(JsonNode result) {
        String checkCode = result.path("CheckCode").asText(null);
        if (checkCode == null) {
            return false;
        }
        String expected = sha256Upper("HashIV=" + props.getHashIv()
                + "&Amt=" + result.path("Amt").asText()
                + "&MerchantID=" + props.getMerchantId()
                + "&MerchantOrderNo=" + result.path("MerchantOrderNo").asText()
                + "&TradeNo=" + result.path("TradeNo").asText()
                + "&HashKey=" + props.getHashKey());
        return expected.equalsIgnoreCase(checkCode);
    }

    // ------------------------------------------------------------------ 退款

    @Override
    public void refundCreditCard(Payment payment) {
        int amount = toIntAmount(payment.getAmount());
        String orderNo = payment.getMerchantOrderNo();
        // 尚未請款：取消授權；已請款：請款後退款（CloseType=2）。哪一種要看當時的請款狀態，所以依序嘗試
        String cancelError = callCardApi(props.getCancelUrl(), "1.0", amount, orderNo, null);
        if (cancelError == null) {
            return;
        }
        String closeError = callCardApi(props.getCloseUrl(), "1.1", amount, orderNo, "2");
        if (closeError == null) {
            return;
        }
        log.warn("藍新退款失敗 orderNo={} cancel={} close={}", orderNo, cancelError, closeError);
        throw new BusinessException("信用卡退款失敗，請聯絡店家處理：" + closeError);
    }

    /** 呼叫藍新信用卡取消/退款 API；成功回 null，失敗回藍新的錯誤訊息 */
    private String callCardApi(String url, String version, int amount, String orderNo, String closeType) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("RespondType", "JSON");
        params.put("Version", version);
        params.put("Amt", String.valueOf(amount));
        params.put("MerchantOrderNo", orderNo);
        params.put("TimeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("IndexType", "1"); // 1：以 MerchantOrderNo 指定交易
        if (closeType != null) {
            params.put("CloseType", closeType);
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("MerchantID_", props.getMerchantId());
        body.add("PostData_", encrypt(toQueryString(params)));
        try {
            String json = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(json);
            if ("SUCCESS".equals(root.path("Status").asText())) {
                return null;
            }
            return root.path("Message").asText("藍新回應異常");
        } catch (Exception e) {
            return "無法連線藍新：" + e.getMessage();
        }
    }

    // ------------------------------------------------------------------ 共用

    /** 必須在交易內呼叫：付款單標記已付款，並把待付款的預約改為已確認 */
    private void markPaid(Payment payment, String tradeNo) {
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setProviderTradeNo(tradeNo);
        payment.setTransactionNo(tradeNo);
        paymentRepository.save(payment);

        Reservation reservation = payment.getReservation();
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);
        }
    }

    private void saveNotification(Payment payment, String merchantOrderNo, String rawBody,
                                  String status, String message) {
        if (message != null && message.length() > 255) {
            message = message.substring(0, 255);
        }
        notificationRepository.save(PaymentNotification.builder()
                .payment(payment)
                .provider(PaymentProviders.NEWEBPAY)
                .merchantOrderNo(merchantOrderNo)
                .rawBody(rawBody)
                .status(status)
                .message(message)
                .build());
    }

    /** 藍新 Amt 只收整數元；有小數就無條件進位避免少收 */
    private int toIntAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.CEILING).intValueExact();
    }

    /** 最長 30 字元、只含英數：CW + yyMMddHHmmss + 8 碼亂數 */
    private String generateMerchantOrderNo() {
        StringBuilder sb = new StringBuilder("CW")
                .append(DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now()));
        for (int i = 0; i < 8; i++) {
            sb.append(ORDER_NO_CHARS.charAt(ThreadLocalRandom.current().nextInt(ORDER_NO_CHARS.length())));
        }
        return sb.toString();
    }

    private String toQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(k).append('=').append(UriUtils.encode(v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private MultiValueMap<String, String> parseForm(String rawBody) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        if (rawBody == null || rawBody.isBlank()) {
            return form;
        }
        for (String pair : rawBody.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                form.add(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
            }
        }
        return form;
    }

    // ------------------------------------------------------------------ 加解密

    /** AES-256-CBC / PKCS7，輸出小寫 16 進位字串 */
    private String encrypt(String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), iv());
            return HexFormat.of().formatHex(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("藍新 TradeInfo 加密失敗（HashKey 長度=" + len(props.getHashKey())
                    + "，HashIV 長度=" + len(props.getHashIv()) + "，應為 32 / 16）：" + e.getMessage(), e);
        }
    }

    private String decrypt(String hex) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key(), iv());
            return new String(cipher.doFinal(HexFormat.of().parseHex(hex)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("藍新 TradeInfo 解密失敗", e);
        }
    }

    private int len(String s) {
        return s == null ? 0 : s.getBytes(StandardCharsets.UTF_8).length;
    }

    private SecretKeySpec key() {
        return new SecretKeySpec(props.getHashKey().getBytes(StandardCharsets.UTF_8), "AES");
    }

    private IvParameterSpec iv() {
        return new IvParameterSpec(props.getHashIv().getBytes(StandardCharsets.UTF_8));
    }

    /** TradeSha = SHA256("HashKey=..&{TradeInfo}&HashIV=..") 大寫 */
    private String tradeSha(String tradeInfo) {
        return sha256Upper("HashKey=" + props.getHashKey() + "&" + tradeInfo + "&HashIV=" + props.getHashIv());
    }

    private String sha256Upper(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 失敗", e);
        }
    }
}
