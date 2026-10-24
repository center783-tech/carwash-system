package com.carwash.controller.dto;

import com.carwash.entity.ParkingZone;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ZoneResponse {

    private Long id;
    private String name;
    private Integer capacity;
    private Integer status;

    public static ZoneResponse from(ParkingZone zone) {
        return new ZoneResponse(zone.getId(), zone.getName(), zone.getCapacity(), zone.getStatus());
    }
}
