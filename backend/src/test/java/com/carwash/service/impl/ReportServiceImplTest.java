package com.carwash.service.impl;

import com.carwash.constant.PaymentMethod;
import com.carwash.constant.PaymentStatus;
import com.carwash.constant.ReportPeriod;
import com.carwash.constant.ReservationStatus;
import com.carwash.controller.dto.RevenueReportResponse;
import com.carwash.entity.Payment;
import com.carwash.entity.Reservation;
import com.carwash.entity.ServiceItem;
import com.carwash.repository.PaymentRepository;
import com.carwash.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReportServiceImpl service;

    private static Payment payment(String service, String amount, PaymentMethod method, PaymentStatus status,
                                   LocalDateTime serviceAt) {
        // 營收以預約服務日期認列：付款可能更早，所以付款時間故意設成服務日的 5 天前
        return Payment.builder()
                .reservation(Reservation.builder().serviceItem(ServiceItem.builder().name(service).build())
                        .reservationDate(serviceAt.toLocalDate()).timeSlotStart(serviceAt.toLocalTime()).build())
                .amount(new BigDecimal(amount))
                .paymentMethod(method)
                .paymentStatus(status)
                .paidAt(serviceAt.minusDays(5))
                .build();
    }

    // ---------------------------------------------------------------- 區間計算

    @Test
    void weekStartsOnMonday() {
        // 2026-10-28 是週三
        assertThat(ReportServiceImpl.startOf(ReportPeriod.WEEK, LocalDate.of(2026, 10, 28)))
                .isEqualTo(LocalDate.of(2026, 10, 26));
        // 週日屬於同一週的最後一天
        assertThat(ReportServiceImpl.startOf(ReportPeriod.WEEK, LocalDate.of(2026, 11, 1)))
                .isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void quarterStartsOnFirstMonthOfQuarter() {
        assertThat(ReportServiceImpl.startOf(ReportPeriod.QUARTER, LocalDate.of(2026, 2, 14))).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(ReportServiceImpl.startOf(ReportPeriod.QUARTER, LocalDate.of(2026, 6, 30))).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(ReportServiceImpl.startOf(ReportPeriod.QUARTER, LocalDate.of(2026, 10, 26))).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(ReportServiceImpl.startOf(ReportPeriod.QUARTER, LocalDate.of(2026, 12, 31))).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void plusMovesByWholePeriods() {
        LocalDate q4 = LocalDate.of(2026, 10, 1);
        assertThat(ReportServiceImpl.plus(ReportPeriod.QUARTER, q4, 1)).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(ReportServiceImpl.plus(ReportPeriod.QUARTER, q4, -1)).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(ReportServiceImpl.plus(ReportPeriod.MONTH, LocalDate.of(2026, 1, 1), -1)).isEqualTo(LocalDate.of(2025, 12, 1));
    }

    // ---------------------------------------------------------------- 報表內容

    @Test
    void monthlyReport_sumsPaidOnly_andCountsRefundsSeparately() {
        LocalDate day = LocalDate.of(2026, 10, 10);
        List<Payment> current = List.of(
                payment("精緻洗車", "300", PaymentMethod.CASH, PaymentStatus.PAID, day.atTime(10, 5)),
                payment("精緻洗車", "300", PaymentMethod.CREDIT_CARD, PaymentStatus.PAID, day.atTime(15, 0)),
                payment("鍍膜", "2500", PaymentMethod.CREDIT_CARD, PaymentStatus.PAID, day.plusDays(3).atTime(9, 0)),
                payment("打蠟", "500", PaymentMethod.CREDIT_CARD, PaymentStatus.REFUNDED, day.atTime(11, 0)));
        // 兩次查詢（本期、上期）用 statuses 區分：本期含 REFUNDED，上期只有 PAID
        when(paymentRepository.findSettledBetween(eq(List.of(PaymentStatus.PAID, PaymentStatus.REFUNDED)), any(), any()))
                .thenReturn(current);
        when(paymentRepository.findSettledBetween(eq(List.of(PaymentStatus.PAID)), any(), any()))
                .thenReturn(List.of(payment("精緻洗車", "1550", PaymentMethod.CASH, PaymentStatus.PAID,
                        LocalDate.of(2026, 9, 5).atTime(12, 0))));
        lenient().when(reservationRepository.countByReservationDateBetween(any(), any())).thenReturn(7L);
        when(reservationRepository.countByReservationDateBetweenAndStatus(any(), any(), eq(ReservationStatus.COMPLETED)))
                .thenReturn(5L);
        when(reservationRepository.countByReservationDateBetweenAndStatus(any(), any(), eq(ReservationStatus.CANCELLED)))
                .thenReturn(1L);

        RevenueReportResponse report = service.revenueReport(ReportPeriod.MONTH, day);

        assertThat(report.startDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(report.endDate()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(report.summary().revenue()).isEqualByComparingTo("3100");
        assertThat(report.summary().paymentCount()).isEqualTo(3);
        assertThat(report.summary().averageOrderValue()).isEqualByComparingTo("1033");
        assertThat(report.summary().refundAmount()).isEqualByComparingTo("500");
        assertThat(report.summary().refundCount()).isEqualTo(1);
        assertThat(report.summary().previousRevenue()).isEqualByComparingTo("1550");
        assertThat(report.summary().growthRate()).isEqualByComparingTo("100.0");
        assertThat(report.reservations().completed()).isEqualTo(5);
        assertThat(report.reservations().cancelled()).isEqualTo(1);

        // 月報表：每天一個點（10 月 31 天），沒營收的日子補 0
        assertThat(report.trend()).hasSize(31);
        assertThat(report.trend().get(9).label()).isEqualTo("10/10");
        assertThat(report.trend().get(9).revenue()).isEqualByComparingTo("600");
        assertThat(report.trend().get(9).count()).isEqualTo(2);
        assertThat(report.trend().get(0).revenue()).isEqualByComparingTo("0");

        // 依營收由高到低
        assertThat(report.byService()).extracting(RevenueReportResponse.ServiceItemRow::name)
                .containsExactly("鍍膜", "精緻洗車");
        assertThat(report.byMethod().get(0).method()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(report.byMethod().get(0).revenue()).isEqualByComparingTo("2800");
    }

    @Test
    void growthRateIsNullWhenPreviousPeriodHadNoRevenue() {
        when(paymentRepository.findSettledBetween(any(), any(), any())).thenReturn(List.of());

        RevenueReportResponse report = service.revenueReport(ReportPeriod.DAY, LocalDate.of(2026, 10, 26));

        assertThat(report.summary().revenue()).isEqualByComparingTo("0");
        assertThat(report.summary().averageOrderValue()).isEqualByComparingTo("0");
        assertThat(report.summary().growthRate()).isNull();
        assertThat(report.trend()).hasSize(24);   // 當日按小時
    }

    @Test
    void quarterTrendHasOnePointPerMonth() {
        when(paymentRepository.findSettledBetween(any(), any(), any())).thenReturn(List.of());

        RevenueReportResponse report = service.revenueReport(ReportPeriod.QUARTER, LocalDate.of(2026, 11, 15));

        assertThat(report.startDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(report.endDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(report.trend()).extracting(RevenueReportResponse.TrendPoint::label)
                .containsExactly("2026-10", "2026-11", "2026-12");
    }
}
