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

    boolean existsByVehicleIdAndStatusIn(Long vehicleId, java.util.Collection<com.carwash.constant.ReservationStatus> statuses);

    /** 某車位區在指定日期內，與 [start, end) 時段重疊的未取消預約數 */
    @org.springframework.data.jpa.repository.Query("select count(r) from Reservation r "
            + "where r.zone.id = :zoneId and r.reservationDate = :date "
            + "and r.status <> com.carwash.constant.ReservationStatus.CANCELLED "
            + "and r.timeSlotStart < :end and r.timeSlotEnd > :start")
    long countOverlapping(@org.springframework.data.repository.query.Param("zoneId") Long zoneId,
                          @org.springframework.data.repository.query.Param("date") LocalDate date,
                          @org.springframework.data.repository.query.Param("start") LocalTime start,
                          @org.springframework.data.repository.query.Param("end") LocalTime end);

    /** 同一台車在指定日期內，與 [start, end) 時段重疊的未取消預約數 */
    @org.springframework.data.jpa.repository.Query("select count(r) from Reservation r "
            + "where r.vehicle.id = :vehicleId and r.reservationDate = :date "
            + "and r.status <> com.carwash.constant.ReservationStatus.CANCELLED "
            + "and r.timeSlotStart < :end and r.timeSlotEnd > :start")
    long countVehicleOverlapping(@org.springframework.data.repository.query.Param("vehicleId") Long vehicleId,
                                 @org.springframework.data.repository.query.Param("date") LocalDate date,
                                 @org.springframework.data.repository.query.Param("start") LocalTime start,
                                 @org.springframework.data.repository.query.Param("end") LocalTime end);

    /** 某員工在指定日期內，與 [start, end) 時段重疊的未取消預約數；excludeId 用來排除預約本身（重新指派時） */
    @org.springframework.data.jpa.repository.Query("select count(r) from Reservation r "
            + "where r.employee.id = :employeeId and r.reservationDate = :date and r.id <> :excludeId "
            + "and r.status <> com.carwash.constant.ReservationStatus.CANCELLED "
            + "and r.timeSlotStart < :end and r.timeSlotEnd > :start")
    long countEmployeeOverlapping(@org.springframework.data.repository.query.Param("employeeId") Long employeeId,
                                  @org.springframework.data.repository.query.Param("date") LocalDate date,
                                  @org.springframework.data.repository.query.Param("start") LocalTime start,
                                  @org.springframework.data.repository.query.Param("end") LocalTime end,
                                  @org.springframework.data.repository.query.Param("excludeId") Long excludeId);

    Page<Reservation> findByMemberId(Long memberId, Pageable pageable);

    Page<Reservation> findByMemberIdAndStatusIn(Long memberId, java.util.Collection<ReservationStatus> statuses, Pageable pageable);

    List<Reservation> findByReservationDate(LocalDate reservationDate);

    /** 依日期範圍查詢（含頭尾兩天），給後台查詢用 */
    List<Reservation> findByReservationDateBetween(LocalDate startDate, LocalDate endDate);

    /** 報表用：日期範圍（含頭尾）內的預約數 */
    long countByReservationDateBetween(LocalDate startDate, LocalDate endDate);

    long countByReservationDateBetweenAndStatus(LocalDate startDate, LocalDate endDate, ReservationStatus status);

    List<Reservation> findByEmployeeIdAndReservationDate(Long employeeId, LocalDate reservationDate);

    List<Reservation> findByStatus(ReservationStatus status);

    /** 建立時間早於 cutoff、仍待確認、有信用卡未付款單且沒有任何已付款單的預約（逾時未付款，供自動取消） */
    @org.springframework.data.jpa.repository.Query("select r from Reservation r "
            + "where r.status = com.carwash.constant.ReservationStatus.PENDING and r.createdAt < :cutoff "
            + "and exists (select 1 from Payment p where p.reservation = r "
            + "and p.paymentMethod = com.carwash.constant.PaymentMethod.CREDIT_CARD "
            + "and p.paymentStatus = com.carwash.constant.PaymentStatus.UNPAID) "
            + "and not exists (select 1 from Payment p2 where p2.reservation = r "
            + "and p2.paymentStatus = com.carwash.constant.PaymentStatus.PAID)")
    List<Reservation> findUnpaidCreditCardBefore(
            @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff);

    /** 檢查同一時段是否已被預約，用來做時段管理/避免重複預約 */
    boolean existsByEmployeeIdAndReservationDateAndTimeSlotStartAndStatusNot(
            Long employeeId,
            LocalDate reservationDate,
            LocalTime timeSlotStart,
            ReservationStatus excludedStatus
    );
}
