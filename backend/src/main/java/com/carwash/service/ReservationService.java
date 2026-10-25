package com.carwash.service;

import com.carwash.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ReservationService {

    /**
     * 建立預約：檢查時段是否衝突、計算金額（套用優惠券後的總額）。
     *
     * @param memberId   會員 id
     * @param vehicleId  車輛 id
     * @param serviceId  服務項目 id
     * @param employeeId 指定員工 id，可為 null（後續由後台指派）
     * @param zoneId     車位區 id，必填；該區在預約時段內必須還有空車位
     * @param couponId   欲使用的優惠券 id（member_coupons.id），可為 null
     */
    Reservation createReservation(Long memberId, Long vehicleId, Long serviceId, Long employeeId, Long zoneId,
                                   LocalDate reservationDate, java.time.LocalTime timeSlotStart,
                                   Long couponId, String remark);

    Reservation confirm(Long reservationId, Long employeeId);

    Reservation cancel(Long reservationId);

    Reservation complete(Long reservationId);

    Reservation getById(Long id);

    Page<Reservation> listByMember(Long memberId, Pageable pageable);

    /** 只列出指定狀態的預約；statuses 為空代表不篩選 */
    Page<Reservation> listByMember(Long memberId, java.util.List<com.carwash.constant.ReservationStatus> statuses, Pageable pageable);

    List<Reservation> listByDate(LocalDate date);

    /** 依日期範圍查詢（含頭尾兩天） */
    List<Reservation> listByDateRange(LocalDate startDate, LocalDate endDate);
}
