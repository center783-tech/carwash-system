package com.carwash.repository;

import com.carwash.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByMemberId(Long memberId);

    List<Vehicle> findByMemberIdAndDeleted(Long memberId, Integer deleted);

    Optional<Vehicle> findByPlateNumber(String plateNumber);
}
