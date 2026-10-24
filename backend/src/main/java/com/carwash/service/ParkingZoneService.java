package com.carwash.service;

import com.carwash.controller.dto.ZoneAvailabilityResponse;
import com.carwash.entity.ParkingZone;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ParkingZoneService {

    List<ParkingZone> listAll();

    ParkingZone create(String name, int capacity, Integer status);

    ParkingZone update(Long id, String name, int capacity, Integer status);

    /** 列出啟用中的車位區，以及在指定日期時段（依服務時長計算結束時間）內各區的剩餘車位 */
    List<ZoneAvailabilityResponse> availability(LocalDate date, LocalTime start, Long serviceId);
}
