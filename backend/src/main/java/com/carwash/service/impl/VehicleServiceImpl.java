package com.carwash.service.impl;

import com.carwash.entity.Member;
import com.carwash.entity.Vehicle;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.MemberRepository;
import com.carwash.repository.ReservationRepository;
import com.carwash.repository.VehicleRepository;
import com.carwash.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private static final java.util.List<com.carwash.constant.ReservationStatus> ACTIVE_STATUSES = java.util.List.of(
            com.carwash.constant.ReservationStatus.PENDING,
            com.carwash.constant.ReservationStatus.CONFIRMED,
            com.carwash.constant.ReservationStatus.IN_PROGRESS);

    private final VehicleRepository vehicleRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;

    @Override
    public Vehicle addVehicle(Long memberId, Vehicle vehicle) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> ResourceNotFoundException.of("Member", memberId));
        vehicle.setMember(member);
        return vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> listByMember(Long memberId) {
        return vehicleRepository.findByMemberIdAndDeleted(memberId, 0);
    }

    @Override
    public Vehicle update(Long vehicleId, Vehicle updates) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Vehicle", vehicleId));
        vehicle.setPlateNumber(updates.getPlateNumber());
        vehicle.setBrand(updates.getBrand());
        vehicle.setModel(updates.getModel());
        vehicle.setColor(updates.getColor());
        vehicle.setCarType(updates.getCarType());
        return vehicleRepository.save(vehicle);
    }

    @Override
    public void delete(Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw ResourceNotFoundException.of("Vehicle", vehicleId);
        }
        // 進行中的預約還需要這輛車，先擋下來；已取消或已完成的預約只是歷史紀錄
        if (reservationRepository.existsByVehicleIdAndStatusIn(vehicleId, ACTIVE_STATUSES)) {
            throw new BusinessException("此車輛有進行中的預約，請先取消預約後再刪除");
        }
        // 預約紀錄以外鍵參照車輛，直接刪除會失敗，所以用軟刪除：標記後從清單隱藏
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow();
        vehicle.setDeleted(1);
        vehicleRepository.save(vehicle);
    }
}
