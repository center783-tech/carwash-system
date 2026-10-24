package com.carwash.controller;

import com.carwash.controller.dto.ZoneAvailabilityResponse;
import com.carwash.controller.dto.ZoneRequest;
import com.carwash.controller.dto.ZoneResponse;
import com.carwash.response.ApiResponse;
import com.carwash.service.ParkingZoneService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Tag(name = "車位區", description = "後台管理車位區與車位數；會員預約時查詢各區剩餘車位")
@RestController
@RequiredArgsConstructor
public class ParkingZoneController {

    private final ParkingZoneService parkingZoneService;

    /** 會員預約：指定日期時段下，各啟用車位區的剩餘車位（登入即可） */
    @GetMapping("/api/zones/availability")
    public ApiResponse<List<ZoneAvailabilityResponse>> availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start,
            @RequestParam Long serviceId) {
        return ApiResponse.success(parkingZoneService.availability(date, start, serviceId));
    }

    // 以下僅限後台（SecurityConfig 的 /api/admin/** 規則）

    @GetMapping("/api/admin/zones")
    public ApiResponse<List<ZoneResponse>> listAll() {
        return ApiResponse.success(parkingZoneService.listAll().stream().map(ZoneResponse::from).toList());
    }

    @PostMapping("/api/admin/zones")
    public ApiResponse<ZoneResponse> create(@Valid @RequestBody ZoneRequest req) {
        return ApiResponse.success("車位區新增成功",
                ZoneResponse.from(parkingZoneService.create(req.getName(), req.getCapacity(), req.getStatus())));
    }

    @PutMapping("/api/admin/zones/{id}")
    public ApiResponse<ZoneResponse> update(@PathVariable Long id, @Valid @RequestBody ZoneRequest req) {
        return ApiResponse.success("車位區更新成功",
                ZoneResponse.from(parkingZoneService.update(id, req.getName(), req.getCapacity(), req.getStatus())));
    }
}
