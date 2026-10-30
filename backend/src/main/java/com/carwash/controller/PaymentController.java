package com.carwash.controller;

import com.carwash.constant.PaymentMethod;
import com.carwash.controller.dto.NewebPayPaymentFormResponse;
import com.carwash.controller.dto.NewebPayTradeQueryResponse;
import com.carwash.controller.dto.PaymentResponse;
import com.carwash.response.ApiResponse;
import com.carwash.security.UserPrincipal;
import com.carwash.service.NewebPayService;
import com.carwash.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "付款", description = "建立付款單、金流回呼、付款紀錄查詢")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final NewebPayService newebPayService;

    /** 付款完成後把使用者導回前端的預約列表 */
    @Value("${app.base-url:http://localhost:5300/carwash}")
    private String appBaseUrl;

    @PostMapping
    public ApiResponse<PaymentResponse> create(@RequestParam Long reservationId, @RequestParam PaymentMethod method) {
        return ApiResponse.success("付款單建立成功",
                PaymentResponse.from(paymentService.createPayment(reservationId, method)));
    }

    /** 後台手動標記付款（例如現金收款），僅限 ADMIN / STAFF（見 SecurityConfig） */
    @PostMapping("/callback/success")
    public ApiResponse<PaymentResponse> callbackSuccess(@RequestParam String transactionNo) {
        return ApiResponse.success("付款成功", PaymentResponse.from(paymentService.markPaid(transactionNo)));
    }

    @PostMapping("/callback/fail")
    public ApiResponse<PaymentResponse> callbackFail(@RequestParam String transactionNo) {
        return ApiResponse.success("付款失敗", PaymentResponse.from(paymentService.markFailed(transactionNo)));
    }

    @GetMapping("/reservation/{reservationId}")
    public ApiResponse<List<PaymentResponse>> listByReservation(@PathVariable Long reservationId) {
        return ApiResponse.success(paymentService.listByReservation(reservationId).stream()
                .map(PaymentResponse::from).collect(Collectors.toList()));
    }

    /**
     * 建立藍新付款單（method：目前只支援 CREDIT_CARD 信用卡），回傳 MPG 表單欄位。
     * 前端拿到後，建立隱藏表單 POST（MerchantID、TradeInfo、TradeSha、Version）到 gatewayUrl 即可跳轉刷卡頁。
     */
    @PostMapping("/reservations/{reservationId}/newebpay/form")
    public ApiResponse<NewebPayPaymentFormResponse> createNewebPayForm(
            @PathVariable Long reservationId,
            @RequestParam(defaultValue = "CREDIT_CARD") PaymentMethod method,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(newebPayService.createPaymentForm(reservationId, principal.getId(), method));
    }

    /** 會員選擇「現場付現」：建立現金付款單（未收款），到場後由店家收款 */
    @PostMapping("/reservations/{reservationId}/cash")
    public ApiResponse<PaymentResponse> chooseCash(@PathVariable Long reservationId,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success("已選擇現場付現",
                PaymentResponse.from(paymentService.createCashPayment(reservationId, principal.getId())));
    }

    /** 後台現場收到現金：標記已付款並確認預約，僅限 ADMIN / STAFF（見 SecurityConfig） */
    @PostMapping("/reservations/{reservationId}/cash/collect")
    public ApiResponse<PaymentResponse> collectCash(@PathVariable Long reservationId) {
        return ApiResponse.success("已收款",
                PaymentResponse.from(paymentService.collectCash(reservationId)));
    }

    /** 主動向藍新查詢單筆交易（NotifyURL 沒收到時的備援） */
    @GetMapping("/newebpay/query/{paymentId}")
    public ApiResponse<NewebPayTradeQueryResponse> queryNewebPayTrade(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(newebPayService.queryPaymentStatus(paymentId, principal.getId()));
    }

    /** 補查目前登入會員逾時仍未付款的藍新付款單 */
    @GetMapping("/newebpay/query-pending")
    public ApiResponse<List<NewebPayTradeQueryResponse>> queryStalePendingTrades(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(newebPayService.queryStalePendingPayments(principal.getId()));
    }

    /**
     * 藍新 NotifyURL：付款結果以此為準（公開端點，靠 TradeSha 驗證來源）。
     * 處理成功回 200 "OK"；驗證或處理失敗會回 400，藍新會稍後重送。
     */
    @PostMapping(value = "/newebpay/notify", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<String> newebPayNotify(@RequestBody String rawBody) {
        newebPayService.handleNotify(rawBody);
        return ResponseEntity.ok("OK");
    }

    /**
     * 藍新 ReturnURL：使用者付款完成後瀏覽器被導回的入口（藍新用 POST）。
     * 這裡不更新付款狀態，只負責導回前端預約列表；付款結果以 NotifyURL 或交易查詢為準。
     */
    @RequestMapping(value = "/newebpay/return", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> returnFromNewebPay() {
        // 用 303 讓瀏覽器把 POST 改成 GET 再導向
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.LOCATION, URI.create(appBaseUrl + "/reservations?payment=newebpay").toString())
                .build();
    }
}
