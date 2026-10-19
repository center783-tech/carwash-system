package com.carwash.repository;

import com.carwash.entity.EmployeeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeScheduleRepository extends JpaRepository<EmployeeSchedule, Long> {

    List<EmployeeSchedule> findByEmployeeId(Long employeeId);

    List<EmployeeSchedule> findByWorkDate(LocalDate workDate);

    List<EmployeeSchedule> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);
}
