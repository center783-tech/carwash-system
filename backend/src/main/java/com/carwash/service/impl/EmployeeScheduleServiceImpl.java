package com.carwash.service.impl;

import com.carwash.entity.Employee;
import com.carwash.entity.EmployeeSchedule;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.EmployeeRepository;
import com.carwash.repository.EmployeeScheduleRepository;
import com.carwash.service.EmployeeScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeScheduleServiceImpl implements EmployeeScheduleService {

    private final EmployeeScheduleRepository scheduleRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public EmployeeSchedule create(Long employeeId, EmployeeSchedule schedule) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", employeeId));
        schedule.setEmployee(employee);
        schedule.setStatus(1);
        return scheduleRepository.save(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSchedule> listByEmployee(Long employeeId) {
        return scheduleRepository.findByEmployeeId(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSchedule> listByDate(LocalDate date) {
        return scheduleRepository.findByWorkDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSchedule> listByEmployeeAndDate(Long employeeId, LocalDate date) {
        return scheduleRepository.findByEmployeeIdAndWorkDate(employeeId, date);
    }

    @Override
    public void markLeave(Long scheduleId) {
        EmployeeSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> ResourceNotFoundException.of("EmployeeSchedule", scheduleId));
        schedule.setStatus(0);
        scheduleRepository.save(schedule);
    }
}
