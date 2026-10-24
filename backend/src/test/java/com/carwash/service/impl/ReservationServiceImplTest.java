package com.carwash.service.impl;

import com.carwash.constant.DiscountType;
import com.carwash.constant.ReservationStatus;
import com.carwash.entity.*;
import com.carwash.exception.BusinessException;
import com.carwash.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private ServiceItemRepository serviceItemRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private MemberCouponRepository memberCouponRepository;
    @Mock private ParkingZoneRepository parkingZoneRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Member member;
    private Vehicle vehicle;
    private ServiceItem serviceItem;
    private Employee employee;
    private ParkingZone zone;
    // 用相對「現在」的未來日期，確保不管測試什麼時候跑，都遠遠超過 12 小時的預約門檻
    private final LocalDate date = LocalDate.now().plusDays(2);
    private final LocalTime startTime = LocalTime.of(10, 0);

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).username("member1").build();
        vehicle = Vehicle.builder().id(1L).member(member).plateNumber("ABC-1234").build();
        serviceItem = ServiceItem.builder().id(1L).name("精緻洗車").price(new BigDecimal("300")).durationMinutes(30).build();
        employee = Employee.builder().id(1L).name("小明").status(1).build();
        zone = ParkingZone.builder().id(1L).name("A 區").capacity(2).status(1).build();

        lenient().when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        lenient().when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        lenient().when(serviceItemRepository.findById(1L)).thenReturn(Optional.of(serviceItem));
        lenient().when(parkingZoneRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(zone));
    }

    @Test
    void createReservation_withoutEmployeeOrCoupon_success() {
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.createReservation(
                1L, 1L, 1L, null, 1L, date, startTime, null, "備註");

        assertThat(result.getTotalAmount()).isEqualByComparingTo("300");
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(result.getEmployee()).isNull();
        assertThat(result.getTimeSlotEnd()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    void createReservation_zoneFull_throwsBusinessException() {
        when(reservationRepository.countOverlapping(eq(1L), eq(date), eq(startTime), any(LocalTime.class)))
                .thenReturn(2L); // capacity = 2，已滿

        assertThatThrownBy(() -> reservationService.createReservation(
                1L, 1L, 1L, null, 1L, date, startTime, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無空車位");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_zoneInactive_throwsBusinessException() {
        zone.setStatus(0);

        assertThatThrownBy(() -> reservationService.createReservation(
                1L, 1L, 1L, null, 1L, date, startTime, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未開放");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_lastBayFree_success() {
        when(reservationRepository.countOverlapping(eq(1L), eq(date), eq(startTime), any(LocalTime.class)))
                .thenReturn(1L); // capacity = 2，還剩 1 個
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.createReservation(
                1L, 1L, 1L, null, 1L, date, startTime, null, null);

        assertThat(result.getZone()).isEqualTo(zone);
    }

    @Test
    void createReservation_lessThan12HoursAhead_throwsBusinessException() {
        LocalDateTime tooSoon = LocalDateTime.now().plusHours(1);

        assertThatThrownBy(() -> reservationService.createReservation(
                1L, 1L, 1L, null, 1L, tooSoon.toLocalDate(), tooSoon.toLocalTime(), null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("12 小時");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_employeeTimeSlotConflict_throwsBusinessException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reservationRepository.existsByEmployeeIdAndReservationDateAndTimeSlotStartAndStatusNot(
                1L, date, startTime, ReservationStatus.CANCELLED)).thenReturn(true);

        assertThatThrownBy(() -> reservationService.createReservation(
                1L, 1L, 1L, 1L, 1L, date, startTime, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已有預約");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_withFixedCoupon_appliesDiscount() {
        Coupon coupon = Coupon.builder()
                .id(1L).code("SAVE50").discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("50")).minAmount(BigDecimal.ZERO)
                .endDate(LocalDate.of(2099, 1, 1)).status(1).build();
        MemberCoupon memberCoupon = MemberCoupon.builder().id(10L).coupon(coupon).isUsed(0).build();

        when(memberCouponRepository.findById(10L)).thenReturn(Optional.of(memberCoupon));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.createReservation(
                1L, 1L, 1L, null, 1L, date, startTime, 10L, null);

        assertThat(result.getTotalAmount()).isEqualByComparingTo("250"); // 300 - 50
        verify(memberCouponRepository).save(argThat(mc -> mc.getIsUsed() == 1));
    }

    @Test
    void createReservation_withPercentageCoupon_appliesDiscount() {
        // discountValue = 90 代表 9 折
        Coupon coupon = Coupon.builder()
                .id(2L).code("90OFF").discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("90")).minAmount(BigDecimal.ZERO)
                .endDate(LocalDate.of(2099, 1, 1)).status(1).build();
        MemberCoupon memberCoupon = MemberCoupon.builder().id(11L).coupon(coupon).isUsed(0).build();

        when(memberCouponRepository.findById(11L)).thenReturn(Optional.of(memberCoupon));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.createReservation(
                1L, 1L, 1L, null, 1L, date, startTime, 11L, null);

        assertThat(result.getTotalAmount()).isEqualByComparingTo("270.0"); // 300 * 0.9
    }

    @Test
    void createReservation_usedCoupon_throwsBusinessException() {
        Coupon coupon = Coupon.builder().id(3L).discountType(DiscountType.FIXED)
                .discountValue(BigDecimal.TEN).build();
        MemberCoupon memberCoupon = MemberCoupon.builder().id(12L).coupon(coupon).isUsed(1).build();

        when(memberCouponRepository.findById(12L)).thenReturn(Optional.of(memberCoupon));

        assertThatThrownBy(() -> reservationService.createReservation(
                1L, 1L, 1L, null, 1L, date, startTime, 12L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被使用");
    }

    @Test
    void cancel_completedReservation_throwsBusinessException() {
        Reservation reservation = Reservation.builder()
                .id(1L).status(ReservationStatus.COMPLETED).build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無法取消");
    }

    @Test
    void cancel_pendingReservation_success() {
        Reservation reservation = Reservation.builder()
                .id(1L).status(ReservationStatus.PENDING).build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.cancel(1L);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }
}
