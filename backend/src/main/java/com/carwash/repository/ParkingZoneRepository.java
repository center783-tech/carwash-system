package com.carwash.repository;

import com.carwash.entity.ParkingZone;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParkingZoneRepository extends JpaRepository<ParkingZone, Long> {

    List<ParkingZone> findByStatusOrderByIdAsc(Integer status);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    /** 建立預約時先鎖住該區，避免兩個人同時搶最後一個車位 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select z from ParkingZone z where z.id = :id")
    Optional<ParkingZone> findByIdForUpdate(@Param("id") Long id);
}
