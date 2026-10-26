package com.carwash.service.impl;

import com.carwash.constant.PaymentMethod;
import com.carwash.constant.PaymentStatus;
import com.carwash.constant.ReportPeriod;
import com.carwash.constant.ReservationStatus;
import com.carwash.controller.dto.RevenueReportResponse;
import com.carwash.controller.dto.RevenueReportResponse.MethodItem;
import com.carwash.controller.dto.RevenueReportResponse.ReservationStats;
import com.carwash.controller.dto.RevenueReportResponse.ServiceItemRow;
import com.carwash.controller.dto.RevenueReportResponse.Summary;
import com.carwash.controller.dto.RevenueReportResponse.TrendPoint;
import com.carwash.entity.Payment;
import com.carwash.repository.PaymentRepository;
import com.carwash.repository.ReservationRepository;
import com.carwash.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM/dd");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("yyyy-MM");

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    @Override
    public RevenueReportResponse revenueReport(ReportPeriod period, LocalDate date) {
        LocalDate start = startOf(period, date);
        LocalDate endExclusive = plus(period, start, 1);
        LocalDate endInclusive = endExclusive.minusDays(1);
        LocalDate prevStart = plus(period, start, -1);

        List<Payment> settled = paymentRepository.findSettledBetween(
                List.of(PaymentStatus.PAID, PaymentStatus.REFUNDED), start.atStartOfDay(), endExclusive.atStartOfDay());
        List<Payment> paid = settled.stream().filter(p -> p.getPaymentStatus() == PaymentStatus.PAID).toList();
        List<Payment> refunded = settled.stream().filter(p -> p.getPaymentStatus() == PaymentStatus.REFUNDED).toList();

        BigDecimal revenue = sum(paid);
        BigDecimal previousRevenue = sum(paymentRepository.findSettledBetween(
                List.of(PaymentStatus.PAID), prevStart.atStartOfDay(), start.atStartOfDay()));

        Summary summary = new Summary(
                revenue,
                paid.size(),
                paid.isEmpty() ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(paid.size()), 0, RoundingMode.HALF_UP),
                sum(refunded),
                refunded.size(),
                previousRevenue,
                growthRate(revenue, previousRevenue));

        ReservationStats reservations = new ReservationStats(
                reservationRepository.countByReservationDateBetween(start, endInclusive),
                reservationRepository.countByReservationDateBetweenAndStatus(start, endInclusive, ReservationStatus.COMPLETED),
                reservationRepository.countByReservationDateBetweenAndStatus(start, endInclusive, ReservationStatus.CANCELLED));

        return new RevenueReportResponse(period, start, endInclusive, summary, reservations,
                trend(period, start, endInclusive, paid), byMethod(paid), byService(paid));
    }

    // ---------------------------------------------------------------- 區間計算

    static LocalDate startOf(ReportPeriod period, LocalDate date) {
        return switch (period) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
            case QUARTER -> LocalDate.of(date.getYear(), ((date.getMonthValue() - 1) / 3) * 3 + 1, 1);
        };
    }

    /** 從區間起點往後（或往前）移動 n 個區間 */
    static LocalDate plus(ReportPeriod period, LocalDate start, int n) {
        return switch (period) {
            case DAY -> start.plusDays(n);
            case WEEK -> start.plusWeeks(n);
            case MONTH -> start.plusMonths(n);
            case QUARTER -> start.plusMonths(3L * n);
        };
    }

    // ---------------------------------------------------------------- 彙總

    private static BigDecimal sum(List<Payment> payments) {
        return payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal growthRate(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    /** 當日按小時；週、月按天；季按月。沒有營收的時段也要有一筆 0，圖表才不會斷掉 */
    private static List<TrendPoint> trend(ReportPeriod period, LocalDate start, LocalDate endInclusive, List<Payment> paid) {
        Map<String, BigDecimal> revenue = new LinkedHashMap<>();
        Function<LocalDateTime, String> bucket;

        if (period == ReportPeriod.DAY) {
            for (int h = 0; h < 24; h++) {
                revenue.put(String.format("%02d:00", h), BigDecimal.ZERO);
            }
            bucket = t -> String.format("%02d:00", t.getHour());
        } else if (period == ReportPeriod.QUARTER) {
            for (LocalDate m = start; !m.isAfter(endInclusive); m = m.plusMonths(1)) {
                revenue.put(m.format(MONTH_LABEL), BigDecimal.ZERO);
            }
            bucket = t -> t.format(MONTH_LABEL);
        } else {
            for (LocalDate d = start; !d.isAfter(endInclusive); d = d.plusDays(1)) {
                revenue.put(d.format(DAY_LABEL), BigDecimal.ZERO);
            }
            bucket = t -> t.format(DAY_LABEL);
        }

        Map<String, Long> count = new LinkedHashMap<>();
        revenue.keySet().forEach(k -> count.put(k, 0L));
        for (Payment p : paid) {
            String key = bucket.apply(p.getPaidAt());
            revenue.merge(key, p.getAmount(), BigDecimal::add);
            count.merge(key, 1L, Long::sum);
        }
        return revenue.entrySet().stream()
                .map(e -> new TrendPoint(e.getKey(), e.getValue(), count.get(e.getKey())))
                .toList();
    }

    private static List<MethodItem> byMethod(List<Payment> paid) {
        Map<PaymentMethod, BigDecimal> revenue = new EnumMap<>(PaymentMethod.class);
        Map<PaymentMethod, Long> count = new EnumMap<>(PaymentMethod.class);
        for (Payment p : paid) {
            revenue.merge(p.getPaymentMethod(), p.getAmount(), BigDecimal::add);
            count.merge(p.getPaymentMethod(), 1L, Long::sum);
        }
        return revenue.entrySet().stream()
                .map(e -> new MethodItem(e.getKey(), e.getValue(), count.get(e.getKey())))
                .sorted(Comparator.comparing(MethodItem::revenue).reversed())
                .toList();
    }

    private static List<ServiceItemRow> byService(List<Payment> paid) {
        Map<String, BigDecimal> revenue = new LinkedHashMap<>();
        Map<String, Long> count = new LinkedHashMap<>();
        for (Payment p : paid) {
            String name = p.getReservation().getServiceItem().getName();
            revenue.merge(name, p.getAmount(), BigDecimal::add);
            count.merge(name, 1L, Long::sum);
        }
        return revenue.entrySet().stream()
                .map(e -> new ServiceItemRow(e.getKey(), e.getValue(), count.get(e.getKey())))
                .sorted(Comparator.comparing(ServiceItemRow::revenue).reversed())
                .toList();
    }
}
