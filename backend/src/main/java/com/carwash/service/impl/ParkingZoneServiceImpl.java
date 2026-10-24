package com.carwash.service.impl;

import com.carwash.controller.dto.ZoneAvailabilityResponse;
import com.carwash.entity.ParkingZone;
import com.carwash.entity.ServiceItem;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.ParkingZoneRepository;
import com.carwash.repository.ReservationRepository;
import com.carwash.repository.ServiceItemRepository;
import com.carwash.service.ParkingZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ParkingZoneServiceImpl implements ParkingZoneService {

    private final ParkingZoneRepository parkingZoneRepository;
    private final ReservationRepository reservationRepository;
    private final ServiceItemRepository serviceItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ParkingZone> listAll() {
        return parkingZoneRepository.findAll().stream()
                .sorted(Comparator.comparing(ParkingZone::getId))
                .toList();
    }

    @Override
    public ParkingZone create(String name, int capacity, Integer status) {
        String trimmed = name.trim();
        if (parkingZoneRepository.existsByName(trimmed)) {
            throw new BusinessException("已有同名的車位區");
        }
        return parkingZoneRepository.save(ParkingZone.builder()
                .name(trimmed)
                .capacity(capacity)
                .status(status == null ? 1 : status)
                .build());
    }

    @Override
    public ParkingZone update(Long id, String name, int capacity, Integer status) {
        ParkingZone zone = parkingZoneRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ParkingZone", id));
        String trimmed = name.trim();
        if (parkingZoneRepository.existsByNameAndIdNot(trimmed, id)) {
            throw new BusinessException("已有同名的車位區");
        }
        zone.setName(trimmed);
        zone.setCapacity(capacity);
        if (status != null) {
            zone.setStatus(status);
        }
        return parkingZoneRepository.save(zone);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ZoneAvailabilityResponse> availability(LocalDate date, LocalTime start, Long serviceId) {
        ServiceItem serviceItem = serviceItemRepository.findById(serviceId)
                .orElseThrow(() -> ResourceNotFoundException.of("ServiceItem", serviceId));
        LocalTime end = start.plusMinutes(serviceItem.getDurationMinutes());

        return parkingZoneRepository.findByStatusOrderByIdAsc(1).stream()
                .map(zone -> {
                    long used = reservationRepository.countOverlapping(zone.getId(), date, start, end);
                    int available = (int) Math.max(zone.getCapacity() - used, 0);
                    return new ZoneAvailabilityResponse(zone.getId(), zone.getName(), zone.getCapacity(), available);
                })
                .toList();
    }
}
