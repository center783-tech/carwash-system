package com.carwash.service.impl;

import com.carwash.constant.DiscountType;
import com.carwash.constant.ReservationStatus;
import com.carwash.entity.*;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.*;
import com.carwash.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final EmployeeRepository employeeRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final ParkingZoneRepository parkingZoneRepository;

    @Override
    public Reservation createReservation(Long memberId, Long vehicleId, Long serviceId, Long employeeId, Long zoneId,
                                          LocalDate reservationDate, LocalTime timeSlotStart,
                                          Long couponId, String remark) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> ResourceNotFoundException.of("Member", memberId));
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Vehicle", vehicleId));
        ServiceItem serviceItem = serviceItemRepository.findById(serviceId)
                .orElseThrow(() -> ResourceNotFoundException.of("ServiceItem", serviceId));

        // 預約時段必須在「現在的 12 小時後」(或之後)，這條規則本身就涵蓋了「不能預約現在以前的時段」
        LocalDateTime reservationDateTime = LocalDateTime.of(reservationDate, timeSlotStart);
        if (reservationDateTime.isBefore(LocalDateTime.now().plusHours(12))) {
            throw new BusinessException("預約時段須在 12 小時後，請選擇更晚的日期或時段");
        }

        Employee employee = null;
        LocalTime timeSlotEnd = timeSlotStart.plusMinutes(serviceItem.getDurationMinutes());

        if (employeeId != null) {
            employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Employee", employeeId));

            boolean conflict = reservationRepository
                    .existsByEmployeeIdAndReservationDateAndTimeSlotStartAndStatusNot(
                            employeeId, reservationDate, timeSlotStart, ReservationStatus.CANCELLED);
            if (conflict) {
                throw new BusinessException("該員工在此時段已有預約");
            }
        }

        // 車位檢查：先鎖住該區（避免同時搶最後一個車位），再確認啟用中且此時段還有空位
        ParkingZone zone = parkingZoneRepository.findByIdForUpdate(zoneId)
                .orElseThrow(() -> ResourceNotFoundException.of("ParkingZone", zoneId));
        if (zone.getStatus() == null || zone.getStatus() != 1) {
            throw new BusinessException("此車位區目前未開放預約");
        }
        long used = reservationRepository.countOverlapping(zoneId, reservationDate, timeSlotStart, timeSlotEnd);
        if (used >= zone.getCapacity()) {
            throw new BusinessException("該區在此時段已無空車位，請改選其他區或時段");
        }

        BigDecimal totalAmount = serviceItem.getPrice();

        MemberCoupon memberCoupon = null;
        if (couponId != null) {
            memberCoupon = memberCouponRepository.findById(couponId)
                    .orElseThrow(() -> ResourceNotFoundException.of("MemberCoupon", couponId));
            if (memberCoupon.getIsUsed() == 1) {
                throw new BusinessException("此優惠券已被使用");
            }
            Coupon coupon = memberCoupon.getCoupon();
            if (coupon.getStatus() == 0 || coupon.getEndDate().isBefore(LocalDate.now())) {
                throw new BusinessException("優惠券已失效");
            }
            if (totalAmount.compareTo(coupon.getMinAmount()) < 0) {
                throw new BusinessException("未達優惠券最低使用金額");
            }
            totalAmount = applyDiscount(totalAmount, coupon);
        }

        Reservation reservation = Reservation.builder()
                .member(member)
                .vehicle(vehicle)
                .serviceItem(serviceItem)
                .employee(employee)
                .zone(zone)
                .reservationDate(reservationDate)
                .timeSlotStart(timeSlotStart)
                .timeSlotEnd(timeSlotEnd)
                .status(ReservationStatus.PENDING)
                .totalAmount(totalAmount)
                .coupon(memberCoupon != null ? memberCoupon.getCoupon() : null)
                .remark(remark)
                .build();

        Reservation saved = reservationRepository.save(reservation);

        if (memberCoupon != null) {
            memberCoupon.setIsUsed(1);
            memberCoupon.setUsedAt(LocalDateTime.now());
            memberCouponRepository.save(memberCoupon);
        }

        return saved;
    }

    private BigDecimal applyDiscount(BigDecimal amount, Coupon coupon) {
        if (coupon.getDiscountType() == DiscountType.FIXED) {
            BigDecimal result = amount.subtract(coupon.getDiscountValue());
            return result.max(BigDecimal.ZERO);
        } else { // PERCENTAGE，discountValue 以「折扣百分比」儲存，例如 90 代表 9 折
            BigDecimal rate = coupon.getDiscountValue().divide(BigDecimal.valueOf(100));
            return amount.multiply(rate);
        }
    }

    @Override
    public Reservation confirm(Long reservationId, Long employeeId) {
        Reservation reservation = getById(reservationId);
        if (employeeId != null) {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Employee", employeeId));
            reservation.setEmployee(employee);
        }
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    @Override
    public Reservation cancel(Long reservationId) {
        Reservation reservation = getById(reservationId);
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new BusinessException("已完成的預約無法取消");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }

    @Override
    public Reservation complete(Long reservationId) {
        Reservation reservation = getById(reservationId);
        reservation.setStatus(ReservationStatus.COMPLETED);
        return reservationRepository.save(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public Reservation getById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Reservation", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Reservation> listByMember(Long memberId, Pageable pageable) {
        return reservationRepository.findByMemberId(memberId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> listByDate(LocalDate date) {
        return reservationRepository.findByReservationDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> listByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("開始日期不能晚於結束日期");
        }
        return reservationRepository.findByReservationDateBetween(startDate, endDate);
    }
}
