package com.carwash.controller;

import com.carwash.controller.dto.EmployeeScheduleResponse;
import com.carwash.exception.BusinessException;
import com.carwash.repository.EmployeeScheduleRepository;
import com.carwash.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Tag(name = "總班表", description = "後台查看某段期間所有在職員工的排班，確認每天都有人上班")
@RestController
@RequiredArgsConstructor
public class ScheduleOverviewController {

    private static final long MAX_DAYS = 62;

    private final EmployeeScheduleRepository scheduleRepository;

    // 僅限後台（SecurityConfig 的 /api/admin/** 規則）
    @GetMapping("/api/admin/schedules")
    @Transactional(readOnly = true)
    public ApiResponse<List<EmployeeScheduleResponse>> overview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to.isBefore(from)) {
            throw new BusinessException("結束日期不能早於開始日期");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
            throw new BusinessException("一次最多查詢 " + MAX_DAYS + " 天");
        }
        return ApiResponse.success(scheduleRepository.findActiveByWorkDateBetween(from, to).stream()
                .map(EmployeeScheduleResponse::from).toList());
    }
}
