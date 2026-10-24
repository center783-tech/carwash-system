package com.carwash.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 指定日期時段下，某個車位區的剩餘車位 */
@Data
@AllArgsConstructor
public class ZoneAvailabilityResponse {

    private Long id;
    private String name;
    private Integer capacity;
    private Integer available;
}
