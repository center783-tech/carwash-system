package com.carwash.service;

import com.carwash.entity.Reservation;
import com.carwash.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 定時取消「信用卡預約建立超過 2 小時仍未完成付款」的預約 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnpaidReservationScheduler {

    private static final long PAYMENT_TIMEOUT_HOURS = 2;

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    @Scheduled(fixedDelay = 60_000)
    public void cancelUnpaidCreditCardReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(PAYMENT_TIMEOUT_HOURS);
        for (Reservation r : reservationRepository.findUnpaidCreditCardBefore(cutoff)) {
            try {
                reservationService.cancel(r.getId());
                log.info("預約 {} 信用卡逾時未付款，已自動取消", r.getId());
            } catch (Exception e) {
                log.warn("預約 {} 自動取消失敗：{}", r.getId(), e.getMessage());
            }
        }
    }
}
