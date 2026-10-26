package com.carwash.controller.dto;

import com.carwash.constant.PaymentMethod;
import com.carwash.constant.ReportPeriod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 營收報表。營收一律以「已付款（PAID）付款單的付款時間」認列；
 * 已退款的付款單不計入營收，另外列在 refundAmount。
 *
 * @param endDate 區間最後一天（含）
 */
public record RevenueReportResponse(
        ReportPeriod period,
        LocalDate startDate,
        LocalDate endDate,
        Summary summary,
        ReservationStats reservations,
        List<TrendPoint> trend,
        List<MethodItem> byMethod,
        List<ServiceItemRow> byService) {

    /**
     * @param growthRate 與上一個同長度區間比較的成長百分比；上一期為 0 時為 null
     */
    public record Summary(
            BigDecimal revenue,
            long paymentCount,
            BigDecimal averageOrderValue,
            BigDecimal refundAmount,
            long refundCount,
            BigDecimal previousRevenue,
            BigDecimal growthRate) {
    }

    /** 以預約日期落在區間內的預約統計 */
    public record ReservationStats(long total, long completed, long cancelled) {
    }

    public record TrendPoint(String label, BigDecimal revenue, long count) {
    }

    public record MethodItem(PaymentMethod method, BigDecimal revenue, long count) {
    }

    public record ServiceItemRow(String name, BigDecimal revenue, long count) {
    }
}
