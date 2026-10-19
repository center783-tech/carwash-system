package com.carwash.service.impl;

import com.carwash.entity.Member;
import com.carwash.entity.Vehicle;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.MemberRepository;
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

    private final VehicleRepository vehicleRepository;
    private final MemberRepository memberRepository;

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
        return vehicleRepository.findByMemberId(memberId);
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
        vehicleRepository.deleteById(vehicleId);
    }
}
