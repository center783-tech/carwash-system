package com.carwash.controller;

import com.carwash.constant.PaymentMethod;
import com.carwash.controller.dto.PaymentResponse;
import com.carwash.response.ApiResponse;
import com.carwash.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "付款", description = "建立付款單、金流回呼、付款紀錄查詢")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ApiResponse<PaymentResponse> create(@RequestParam Long reservationId, @RequestParam PaymentMethod method) {
        return ApiResponse.success("付款單建立成功",
                PaymentResponse.from(paymentService.createPayment(reservationId, method)));
    }

    /** 供金流（如藍新）回呼使用 */
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
}
