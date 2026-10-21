package com.carwash.service;

import com.carwash.entity.EmployeeSchedule;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeScheduleService {

    EmployeeSchedule create(Long employeeId, EmployeeSchedule schedule);

    List<EmployeeSchedule> listByEmployee(Long employeeId);

    /** 查某一天「所有員工」的排班，給後台總覽當天人力用 */
    List<EmployeeSchedule> listByDate(LocalDate date);

    /** 查「單一員工」在某一天的排班 */
    List<EmployeeSchedule> listByEmployeeAndDate(Long employeeId, LocalDate date);

    void markLeave(Long scheduleId);
}
