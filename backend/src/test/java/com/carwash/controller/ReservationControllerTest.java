package com.carwash.controller;

import com.carwash.controller.dto.CreateReservationRequest;
import com.carwash.entity.Member;
import com.carwash.entity.Reservation;
import com.carwash.security.UserPrincipal;
import com.carwash.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** 驗證預約 API 的擁有者檢查（IDOR）：會員不能碰別人的預約，也不能替別人下預約 */
@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationController controller;

    private UserPrincipal alice;   // 會員 id=1
    private UserPrincipal bob;     // 會員 id=2
    private UserPrincipal staff;   // 後台 id=1（id 故意和 alice 相同，驗證會連 role 一起比對）

    @BeforeEach
    void setUp() {
        alice = UserPrincipal.fromMember(Member.builder().id(1L).username("alice").password("x").status(1).build());
        bob = UserPrincipal.fromMember(Member.builder().id(2L).username("bob").password("x").status(1).build());
        staff = UserPrincipal.fromAdmin(com.carwash.entity.Admin.builder()
                .id(1L).username("boss").password("x").status(1)
                .role(com.carwash.constant.AdminRole.STAFF).build());
    }

    private Reservation reservationOf(long memberId) {
        return Reservation.builder().id(10L)
                .member(Member.builder().id(memberId).username("u" + memberId).build())
                .vehicle(com.carwash.entity.Vehicle.builder().id(1L).plateNumber("ABC-1234").build())
                .serviceItem(com.carwash.entity.ServiceItem.builder().id(1L).name("洗車").build())
                .build();
    }

    @Test
    void getById_otherMember_denied() {
        when(reservationService.getById(10L)).thenReturn(reservationOf(1L));

        assertThatThrownBy(() -> controller.getById(10L, bob)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancel_otherMember_deniedAndNotCancelled() {
        when(reservationService.getById(10L)).thenReturn(reservationOf(1L));

        assertThatThrownBy(() -> controller.cancel(10L, bob)).isInstanceOf(AccessDeniedException.class);
        verify(reservationService, never()).cancel(any());
    }

    @Test
    void cancel_owner_allowed() {
        Reservation reservation = reservationOf(1L);
        when(reservationService.getById(10L)).thenReturn(reservation);
        when(reservationService.cancel(10L)).thenReturn(reservation);

        controller.cancel(10L, alice);

        verify(reservationService).cancel(10L);
    }

    @Test
    void listByMember_otherMember_denied() {
        assertThatThrownBy(() -> controller.listByMember(1L, 0, 10, null, bob))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(reservationService);
    }

    @Test
    void listByMember_staff_allowed() {
        when(reservationService.listByMember(eq(1L), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        controller.listByMember(1L, 0, 10, null, staff);

        verify(reservationService).listByMember(eq(1L), any(), any());
    }

    @Test
    void create_memberIdInBodyIsIgnored() {
        CreateReservationRequest req = requestFor(2L);   // alice 想用 bob 的 id 下預約
        when(reservationService.createReservation(eq(1L), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(reservationOf(1L));

        controller.create(req, alice);

        verify(reservationService).createReservation(eq(1L), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void create_staffMustSpecifyMember() {
        assertThatThrownBy(() -> controller.create(requestFor(null), staff))
                .isInstanceOf(com.carwash.exception.BusinessException.class);
        verifyNoInteractions(reservationService);
    }

    private CreateReservationRequest requestFor(Long memberId) {
        CreateReservationRequest req = new CreateReservationRequest();
        req.setMemberId(memberId);
        req.setVehicleId(1L);
        req.setServiceId(1L);
        req.setZoneId(1L);
        req.setReservationDate(LocalDate.now().plusDays(3));
        req.setTimeSlotStart(LocalTime.of(10, 0));
        return req;
    }
}
