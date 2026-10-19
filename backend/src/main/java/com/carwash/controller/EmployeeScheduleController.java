package com.carwash.controller;

import com.carwash.controller.dto.EmployeeScheduleRequest;
import com.carwash.controller.dto.EmployeeScheduleResponse;
import com.carwash.entity.EmployeeSchedule;
import com.carwash.response.ApiResponse;
import com.carwash.service.EmployeeScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "員工排班", description = "員工排班建立、查詢、請假")
@RestController
@RequestMapping("/api/employees/{employeeId}/schedules")
@RequiredArgsConstructor
public class EmployeeScheduleController {

    private final EmployeeScheduleService scheduleService;

    @PostMapping
    public ApiResponse<EmployeeScheduleResponse> create(@PathVariable Long employeeId,
                                                          @Valid @RequestBody EmployeeScheduleRequest req) {
        EmployeeSchedule schedule = EmployeeSchedule.builder()
                .workDate(req.getWorkDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .build();
        EmployeeSchedule saved = scheduleService.create(employeeId, schedule);
        return ApiResponse.success("新增排班成功", EmployeeScheduleResponse.from(saved));
    }

    @GetMapping
    public ApiResponse<List<EmployeeScheduleResponse>> list(@PathVariable Long employeeId) {
        return ApiResponse.success(scheduleService.listByEmployee(employeeId).stream()
                .map(EmployeeScheduleResponse::from).collect(Collectors.toList()));
    }

    @GetMapping("/by-date")
    public ApiResponse<List<EmployeeScheduleResponse>> listByDate(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(scheduleService.listByDate(date).stream()
                .map(EmployeeScheduleResponse::from).collect(Collectors.toList()));
    }

    @PutMapping("/{scheduleId}/leave")
    public ApiResponse<Void> markLeave(@PathVariable Long employeeId, @PathVariable Long scheduleId) {
        scheduleService.markLeave(scheduleId);
        return ApiResponse.success("已標記請假", null);
    }
}
