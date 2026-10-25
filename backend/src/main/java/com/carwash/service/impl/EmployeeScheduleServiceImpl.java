package com.carwash.service.impl;

import com.carwash.entity.Employee;
import com.carwash.entity.EmployeeSchedule;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.ClosedDateRepository;
import com.carwash.repository.EmployeeRepository;
import com.carwash.repository.EmployeeScheduleRepository;
import com.carwash.service.EmployeeScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeScheduleServiceImpl implements EmployeeScheduleService {

    private final EmployeeScheduleRepository scheduleRepository;
    private final EmployeeRepository employeeRepository;
    private final ClosedDateRepository closedDateRepository;

    @Override
    public EmployeeSchedule create(Long employeeId, EmployeeSchedule schedule) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", employeeId));
        assertNotClosed(schedule.getWorkDate());
        schedule.setEmployee(employee);
        schedule.setStatus(1);
        return scheduleRepository.save(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSchedule> listByEmployee(Long employeeId) {
        return scheduleRepository
                .findByEmployeeIdAndWorkDateGreaterThanEqualOrderByWorkDateAscStartTimeAsc(employeeId, LocalDate.now());
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
    public void cancelLeave(Long scheduleId) {
        EmployeeSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> ResourceNotFoundException.of("EmployeeSchedule", scheduleId));
        schedule.setStatus(1);
        scheduleRepository.save(schedule);
    }

    @Override
    public List<EmployeeSchedule> createBatch(Long employeeId, List<LocalDate> workDates,
                                              LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("下班時間必須晚於上班時間");
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", employeeId));
        List<LocalDate> closed = workDates.stream().distinct().sorted()
                .filter(closedDateRepository::existsByClosedDate).toList();
        if (!closed.isEmpty()) {
            throw new BusinessException("以下日期是休息日，不能排班：" + closed.stream().map(LocalDate::toString)
                    .collect(java.util.stream.Collectors.joining("、")));
        }
        List<EmployeeSchedule> created = new ArrayList<>();
        for (LocalDate date : workDates.stream().distinct().toList()) {
            if (scheduleRepository.existsByEmployeeIdAndWorkDateAndStartTime(employeeId, date, startTime)) continue;
            created.add(scheduleRepository.save(EmployeeSchedule.builder()
                    .employee(employee).workDate(date).startTime(startTime).endTime(endTime).status(1).build()));
        }
        return created;
    }

    private void assertNotClosed(LocalDate date) {
        if (closedDateRepository.existsByClosedDate(date)) {
            throw new BusinessException(date + " 是休息日，不能排班");
        }
    }

    @Override
    public void delete(Long scheduleId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw ResourceNotFoundException.of("EmployeeSchedule", scheduleId);
        }
        scheduleRepository.deleteById(scheduleId);
    }

    @Override
    public void markLeave(Long scheduleId) {
        EmployeeSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> ResourceNotFoundException.of("EmployeeSchedule", scheduleId));
        schedule.setStatus(0);
        scheduleRepository.save(schedule);
    }
}
