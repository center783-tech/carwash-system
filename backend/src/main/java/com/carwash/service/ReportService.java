package com.carwash.service;

import com.carwash.constant.ReportPeriod;
import com.carwash.controller.dto.RevenueReportResponse;

import java.time.LocalDate;

public interface ReportService {

    /** 產生 date 所在的當日／週／月／季的營收報表 */
    RevenueReportResponse revenueReport(ReportPeriod period, LocalDate date);
}
