package com.carwash.controller;

import com.carwash.controller.dto.EmployeeScheduleBatchRequest;
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
        return ApiResponse.success(scheduleService.listByEmployeeAndDate(employeeId, date).stream()
                .map(EmployeeScheduleResponse::from).collect(Collectors.toList()));
    }

    /** 一次排多天 */
    @PostMapping("/batch")
    public ApiResponse<List<EmployeeScheduleResponse>> createBatch(@PathVariable Long employeeId,
                                                                    @Valid @RequestBody EmployeeScheduleBatchRequest req) {
        List<EmployeeScheduleResponse> created = scheduleService
                .createBatch(employeeId, req.getWorkDates(), req.getStartTime(), req.getEndTime()).stream()
                .map(EmployeeScheduleResponse::from).collect(Collectors.toList());
        return ApiResponse.success("已新增 " + created.size() + " 筆排班", created);
    }

    @PutMapping("/{scheduleId}/leave/cancel")
    public ApiResponse<Void> cancelLeave(@PathVariable Long employeeId, @PathVariable Long scheduleId) {
        scheduleService.cancelLeave(scheduleId);
        return ApiResponse.success("已取消請假", null);
    }

    @DeleteMapping("/{scheduleId}")
    public ApiResponse<Void> delete(@PathVariable Long employeeId, @PathVariable Long scheduleId) {
        scheduleService.delete(scheduleId);
        return ApiResponse.success("排班已刪除", null);
    }

    @PutMapping("/{scheduleId}/leave")
    public ApiResponse<Void> markLeave(@PathVariable Long employeeId, @PathVariable Long scheduleId) {
        scheduleService.markLeave(scheduleId);
        return ApiResponse.success("已標記請假", null);
    }
}
