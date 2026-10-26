package com.carwash.controller;

import com.carwash.constant.ReservationStatus;
import com.carwash.controller.dto.CreateReservationRequest;
import com.carwash.controller.dto.ReservationResponse;
import com.carwash.entity.Reservation;
import com.carwash.exception.BusinessException;
import com.carwash.response.ApiResponse;
import com.carwash.security.AccessGuard;
import com.carwash.security.UserPrincipal;
import com.carwash.service.ReservationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "預約", description = "核心預約流程：選服務、選時段、選車輛、送出預約、確認/取消/完成")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ApiResponse<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest req,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        // 會員一律用登入者本人的 id，不信任 body 的 memberId；後台代客預約才採用 body 的值
        Long memberId = principal.getId();
        if (AccessGuard.isStaff(principal)) {
            memberId = req.getMemberId();
            if (memberId == null) {
                throw new BusinessException("請指定會員");
            }
        }
        Reservation reservation = reservationService.createReservation(
                memberId, req.getVehicleId(), req.getServiceId(), req.getEmployeeId(), req.getZoneId(),
                req.getReservationDate(), req.getTimeSlotStart(), req.getCouponId(), req.getRemark());
        return ApiResponse.success("預約建立成功", ReservationResponse.from(reservation));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReservationResponse> getById(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        Reservation reservation = reservationService.getById(id);
        AccessGuard.requireSelfOrStaff(reservation.getMember().getId(), principal);
        return ApiResponse.success(ReservationResponse.from(reservation));
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<ReservationResponse> confirm(@PathVariable Long id,
                                                      @RequestParam(required = false) Long employeeId) {
        return ApiResponse.success("預約已確認", ReservationResponse.from(reservationService.confirm(id, employeeId)));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<ReservationResponse> cancel(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        AccessGuard.requireSelfOrStaff(reservationService.getById(id).getMember().getId(), principal);
        return ApiResponse.success("預約已取消", ReservationResponse.from(reservationService.cancel(id)));
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<ReservationResponse> complete(@PathVariable Long id) {
        return ApiResponse.success("預約已完成", ReservationResponse.from(reservationService.complete(id)));
    }

    @GetMapping("/member/{memberId}")
    public ApiResponse<Page<ReservationResponse>> listByMember(@PathVariable Long memberId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size,
                                                                @RequestParam(required = false) List<ReservationStatus> statuses,
                                                                @AuthenticationPrincipal UserPrincipal principal) {
        AccessGuard.requireSelfOrStaff(memberId, principal);
        Page<ReservationResponse> result = reservationService
                .listByMember(memberId, statuses, PageRequest.of(page, size))
                .map(ReservationResponse::from);
        return ApiResponse.success(result);
    }

    @GetMapping("/by-date-range")
    public ApiResponse<List<ReservationResponse>> listByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(reservationService.listByDateRange(startDate, endDate).stream()
                .map(ReservationResponse::from).collect(Collectors.toList()));
    }
}
