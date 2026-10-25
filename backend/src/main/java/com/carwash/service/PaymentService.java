package com.carwash.service;

import com.carwash.constant.PaymentMethod;
import com.carwash.entity.Payment;

import java.util.List;

public interface PaymentService {

    /** 建立付款單（狀態為 UNPAID），之後由金流回呼更新為 PAID/FAILED */
    Payment createPayment(Long reservationId, PaymentMethod method);

    /** 金流回呼：依交易序號更新付款狀態，並同步把預約狀態改為 CONFIRMED */
    Payment markPaid(String transactionNo);

    Payment markFailed(String transactionNo);

    /** 會員選擇「現場付現」：建立 CASH 付款單（UNPAID），到場後由店家收款 */
    Payment createCashPayment(Long reservationId, Long memberId);

    /** 店家現場收到現金：沒有現金付款單就補建，標記已付款，並把待確認的預約改為已確認 */
    Payment collectCash(Long reservationId);

    List<Payment> listByReservation(Long reservationId);
}
