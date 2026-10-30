package com.carwash.service.impl;

import com.carwash.constant.DiscountType;
import com.carwash.constant.PaymentMethod;
import com.carwash.constant.PaymentProviders;
import com.carwash.constant.PaymentStatus;
import com.carwash.constant.ReservationStatus;
import com.carwash.entity.*;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.*;
import com.carwash.service.NewebPayService;
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
    private final EmployeeScheduleRepository employeeScheduleRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final ParkingZoneRepository parkingZoneRepository;
    private final ClosedDateRepository closedDateRepository;
    private final NewebPayService newebPayService;

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

        if (closedDateRepository.existsByClosedDate(reservationDate)) {
            throw new BusinessException("該日為休息日，不開放預約，請改選其他日期");
        }

        Employee employee = null;
        LocalTime timeSlotEnd = timeSlotStart.plusMinutes(serviceItem.getDurationMinutes());

        // 同一台車不能在重疊的時段重複預約（已取消的不算）
        if (reservationRepository.countVehicleOverlapping(vehicleId, reservationDate, timeSlotStart, timeSlotEnd) > 0) {
            throw new BusinessException("這台車在此時段已有預約，請改選其他時段或車輛");
        }

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
            if (memberCoupon.getRedeemStartedAt() != null) {
                throw new BusinessException("此優惠券正在現場使用中，無法用於預約");
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
    public Reservation assignEmployee(Long reservationId, Long employeeId) {
        Reservation reservation = getById(reservationId);
        requireAssignable(reservation);
        if (employeeId == null) {
            reservation.setEmployee(null);
        } else {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Employee", employeeId));
            String reason = unassignableReason(reservation, employee);
            if (reason != null) {
                throw new BusinessException(reason);
            }
            reservation.setEmployee(employee);
        }
        return reservationRepository.save(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> listAssignableEmployees(Long reservationId) {
        Reservation reservation = getById(reservationId);
        requireAssignable(reservation);
        return employeeRepository.findByStatus(1).stream()
                .filter(e -> unassignableReason(reservation, e) == null)
                .toList();
    }

    private void requireAssignable(Reservation reservation) {
        ReservationStatus status = reservation.getStatus();
        if (status == ReservationStatus.CANCELLED || status == ReservationStatus.COMPLETED) {
            throw new BusinessException("已取消或已完成的預約無法指派員工");
        }
    }

    /** 不能指派的原因；可以指派時回傳 null。目前已指派的員工不算跟自己衝突 */
    private String unassignableReason(Reservation r, Employee employee) {
        if (employee.getStatus() == null || employee.getStatus() != 1) {
            return "該員工已停用，無法指派";
        }
        if (employee.getServiceItems().stream().noneMatch(s -> s.getId().equals(r.getServiceItem().getId()))) {
            return "該員工無法提供此服務項目";
        }
        boolean working = employeeScheduleRepository
                .findByEmployeeIdAndWorkDate(employee.getId(), r.getReservationDate()).stream()
                .anyMatch(s -> s.getStatus() == 1
                        && !r.getTimeSlotStart().isBefore(s.getStartTime())
                        && r.getTimeSlotStart().isBefore(s.getEndTime()));
        if (!working) {
            return "該員工在此時段沒有排班或正在請假";
        }
        if (reservationRepository.countEmployeeOverlapping(employee.getId(), r.getReservationDate(),
                r.getTimeSlotStart(), r.getTimeSlotEnd(), r.getId()) > 0) {
            return "該員工在此時段已有其他預約";
        }
        return null;
    }

    @Override
    public Reservation cancel(Long reservationId) {
        Reservation reservation = getById(reservationId);
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new BusinessException("已完成的預約無法取消");
        }
        refundOrVoidPayments(reservation);
        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }

    /**
     * 取消預約時處理付款單：
     * 信用卡已付款 → 線上退款（失敗會丟例外，預約不會被取消）；
     * 未付款的線上付款單 → 作廢；現場現金／ATM 已付款 → 無法線上退款，保留 PAID 由店家手動處理。
     */
    private void refundOrVoidPayments(Reservation reservation) {
        for (Payment payment : reservation.getPayments()) {
            if (payment.getPaymentStatus() == PaymentStatus.PAID
                    && payment.getPaymentMethod() == PaymentMethod.CREDIT_CARD
                    && PaymentProviders.NEWEBPAY.equals(payment.getProvider())) {
                newebPayService.refundCreditCard(payment);
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
            } else if (payment.getPaymentStatus() == PaymentStatus.UNPAID) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
            }
        }
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
    public Page<Reservation> listByMember(Long memberId, List<ReservationStatus> statuses, Pageable pageable) {
        if (statuses == null || statuses.isEmpty()) {
            return reservationRepository.findByMemberId(memberId, pageable);
        }
        return reservationRepository.findByMemberIdAndStatusIn(memberId, statuses, pageable);
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
