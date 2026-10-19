package com.carwash.service;

import com.carwash.entity.EmployeeSchedule;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeScheduleService {

    EmployeeSchedule create(Long employeeId, EmployeeSchedule schedule);

    List<EmployeeSchedule> listByEmployee(Long employeeId);

    List<EmployeeSchedule> listByDate(LocalDate date);

    void markLeave(Long scheduleId);
}
