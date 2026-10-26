package com.carwash.controller;

import com.carwash.constant.ReportPeriod;
import com.carwash.controller.dto.RevenueReportResponse;
import com.carwash.response.ApiResponse;
import com.carwash.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "後台報表", description = "營收報表：當日、週、月、季（僅限 ADMIN，見 SecurityConfig）")
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    /** date 不帶就用今天；回傳 date 所在的當日／週／月／季 */
    @GetMapping("/revenue")
    public ApiResponse<RevenueReportResponse> revenue(
            @RequestParam ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(reportService.revenueReport(period, date != null ? date : LocalDate.now()));
    }
}
