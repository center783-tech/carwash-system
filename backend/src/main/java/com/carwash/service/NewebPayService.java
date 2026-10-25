package com.carwash.service;

import com.carwash.constant.PaymentMethod;
import com.carwash.controller.dto.NewebPayPaymentFormResponse;
import com.carwash.controller.dto.NewebPayTradeQueryResponse;
import com.carwash.entity.Payment;

import java.util.List;

public interface NewebPayService {

    /** 為預約建立一筆付款單（信用卡或 ATM 轉帳），並回傳要 POST 到藍新 MPG 的表單欄位 */
    NewebPayPaymentFormResponse createPaymentForm(Long reservationId, Long memberId, PaymentMethod method);

    /** 主動向藍新查詢交易結果；若已付款則補更新付款/預約狀態（NotifyURL 沒收到時的備援） */
    NewebPayTradeQueryResponse queryPaymentStatus(Long paymentId, Long memberId);

    /** 補查該會員逾時仍未付款的藍新付款單 */
    List<NewebPayTradeQueryResponse> queryStalePendingPayments(Long memberId);

    /** 信用卡線上退款（取消預約時用）：先試取消授權，失敗再試請款後退款；兩者都失敗會丟 BusinessException */
    void refundCreditCard(Payment payment);

    /** 處理藍新 NotifyURL 的 form-urlencoded 原始內容；驗證失敗或處理失敗會丟例外，讓藍新稍後重送 */
    void handleNotify(String rawBody);
}
