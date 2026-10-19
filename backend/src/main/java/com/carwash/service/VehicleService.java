package com.carwash.service;

import com.carwash.entity.Vehicle;

import java.util.List;

public interface VehicleService {

    Vehicle addVehicle(Long memberId, Vehicle vehicle);

    List<Vehicle> listByMember(Long memberId);

    Vehicle update(Long vehicleId, Vehicle updates);

    void delete(Long vehicleId);
}
