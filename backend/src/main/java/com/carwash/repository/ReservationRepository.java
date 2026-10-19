package com.carwash.repository;

import com.carwash.constant.ReservationStatus;
import com.carwash.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Page<Reservation> findByMemberId(Long memberId, Pageable pageable);

    List<Reservation> findByReservationDate(LocalDate reservationDate);

    List<Reservation> findByEmployeeIdAndReservationDate(Long employeeId, LocalDate reservationDate);

    List<Reservation> findByStatus(ReservationStatus status);

    /** 檢查同一時段是否已被預約，用來做時段管理/避免重複預約 */
    boolean existsByEmployeeIdAndReservationDateAndTimeSlotStartAndStatusNot(
            Long employeeId,
            LocalDate reservationDate,
            LocalTime timeSlotStart,
            ReservationStatus excludedStatus
    );
}
