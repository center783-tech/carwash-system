package com.carwash.repository;

import com.carwash.entity.ClosedDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ClosedDateRepository extends JpaRepository<ClosedDate, Long> {

    boolean existsByClosedDate(LocalDate closedDate);

    List<ClosedDate> findByClosedDateGreaterThanEqualOrderByClosedDateAsc(LocalDate from);
}
