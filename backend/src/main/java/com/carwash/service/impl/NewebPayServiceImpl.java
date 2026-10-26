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
    /** 藍新 AES 補位的區塊大小（bytes） */
    private static final int PAD_BLOCK = 32;
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
        params.put("RespondType", "String");
        params.put("TimeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("Version", MPG_VERSION);
        params.put("MerchantOrderNo", payment.getMerchantOrderNo());
        params.put("Amt", String.valueOf(amount));
        params.put("ItemDesc", reservation.getServiceItem().getName());
        params.put("ReturnURL", props.getReturnUrl());
        params.put("NotifyURL", props.getNotifyUrl());
        // 藍新付款／取號結果頁的「返回商店」按鈕；ATM 取號後使用者要留在藍新頁抄帳號，所以不用 CustomerURL 自動導回
        params.put("ClientBackURL", props.getReturnUrl());
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
            // 通常是 form-urlencoded；保險起見也接受 JSON 本文
            MultiValueMap<String, String> form = parseNotifyBody(rawBody);
            String tradeInfo = form.getFirst("TradeInfo");
            String tradeSha = form.getFirst("TradeSha");
            if (tradeInfo == null || tradeSha == null) {
                log.warn("藍新通知缺少 TradeInfo/TradeSha，欄位={}", form.keySet());
                saveNotification(null, null, rawBody, NotifyStatus.INVALID,
                        "缺少 TradeInfo 或 TradeSha，收到的欄位：" + form.keySet());
                throw new BusinessException("通知格式不正確");
            }
            if (!MessageDigest.isEqual(tradeSha(tradeInfo).getBytes(StandardCharsets.UTF_8),
                    tradeSha.toUpperCase().getBytes(StandardCharsets.UTF_8))) {
                log.warn("藍新通知 TradeSha 驗證失敗，請確認 HashKey/HashIV 與藍新後台一致");
                saveNotification(null, null, rawBody, NotifyStatus.INVALID, "TradeSha 驗證失敗");
                throw new BusinessException("TradeSha 驗證失敗");
            }

            JsonNode root = parseTradeInfo(decrypt(tradeInfo));
            JsonNode result = root.path("Result");
            merchantOrderNo = result.path("MerchantOrderNo").asText(null);
            boolean success = "SUCCESS".equals(root.path("Status").asText());
            String message = root.path("Message").asText();

            payment = merchantOrderNo == null ? null
                    : paymentRepository.findByMerchantOrderNo(merchantOrderNo).orElse(null);
            if (payment == null) {
                log.warn("藍新通知找不到付款單 merchantOrderNo={}", merchantOrderNo);
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

    private MultiValueMap<String, String> parseNotifyBody(String rawBody) {
        String body = rawBody == null ? "" : rawBody.trim();
        if (body.startsWith("{")) {
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            try {
                objectMapper.readTree(body).fields().forEachRemaining(e -> map.add(e.getKey(), e.getValue().asText()));
            } catch (Exception e) {
                // 解析失敗就回空，由呼叫端當作格式錯誤
            }
            return map;
        }
        return parseForm(body);
    }

    /** TradeInfo 解密後通常是 JSON（{Status, Message, Result:{...}}）；若是 querystring 就包成同樣結構 */
    private JsonNode parseTradeInfo(String plain) throws Exception {
        String text = plain.trim();
        if (text.startsWith("{")) {
            return objectMapper.readTree(text);
        }
        MultiValueMap<String, String> flat = parseForm(text);
        var root = objectMapper.createObjectNode();
        var result = root.putObject("Result");
        flat.forEach((k, v) -> result.put(k, v.get(0)));
        root.put("Status", flat.getFirst("Status") == null ? "" : flat.getFirst("Status"));
        root.put("Message", flat.getFirst("Message") == null ? "" : flat.getFirst("Message"));
        return root;
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
        throw new BusinessException("信用卡退款失敗，請聯絡店家處理（取消授權：" + cancelError
                + "；請款後退款：" + closeError + "）");
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
            String status = root.path("Status").asText();
            // TRA20001：取消授權已受理，由金融機構批次處理（手冊 4.4.2），不是失敗
            if ("SUCCESS".equals(status) || (closeType == null && "TRA20001".equals(status))) {
                return null;
            }
            return status + " " + root.path("Message").asText("藍新回應異常");
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
            // 藍新的補位區塊是 32 bytes（不是 JCE PKCS5Padding 的 16），所以自己補位
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), iv());
            return HexFormat.of().formatHex(cipher.doFinal(pkcs7Pad(plain.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            throw new IllegalStateException("藍新 TradeInfo 加密失敗（HashKey 長度=" + len(props.getHashKey())
                    + "，HashIV 長度=" + len(props.getHashIv()) + "，應為 32 / 16）：" + e.getMessage(), e);
        }
    }

    private String decrypt(String hex) {
        try {
            // 跟藍新官方範例一致：不讓 JCE 處理 padding，解完再自己去掉尾端補位，
            // 避免藍新回傳的補位不符合標準 PKCS7 時整個解密失敗
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), iv());
            byte[] plain = cipher.doFinal(HexFormat.of().parseHex(hex.trim()));
            return new String(stripPadding(plain), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("藍新 TradeInfo 解密失敗：" + e.getClass().getSimpleName()
                    + " " + e.getMessage() + "（密文長度=" + (hex == null ? 0 : hex.length()) + "）", e);
        }
    }

    /** 以 32 bytes 為區塊做 PKCS7 補位（藍新規格） */
    private byte[] pkcs7Pad(byte[] source) {
        int pad = PAD_BLOCK - source.length % PAD_BLOCK;
        byte[] padded = java.util.Arrays.copyOf(source, source.length + pad);
        java.util.Arrays.fill(padded, source.length, padded.length, (byte) pad);
        return padded;
    }

    /** 去掉 32 bytes 區塊的 PKCS7 補位；補位不合法時，改去掉尾端的控制字元（0x00~0x20） */
    private byte[] stripPadding(byte[] data) {
        int end = data.length;
        if (end == 0) {
            return data;
        }
        int pad = data[end - 1] & 0xFF;
        if (pad >= 1 && pad <= PAD_BLOCK && pad <= end) {
            boolean valid = true;
            for (int i = end - pad; i < end; i++) {
                if ((data[i] & 0xFF) != pad) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                return java.util.Arrays.copyOf(data, end - pad);
            }
        }
        while (end > 0 && (data[end - 1] & 0xFF) <= PAD_BLOCK) {
            end--;
        }
        return java.util.Arrays.copyOf(data, end);
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
