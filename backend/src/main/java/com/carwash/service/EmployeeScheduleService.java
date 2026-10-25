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

    /** 取消請假，恢復為正常上班 */
    void cancelLeave(Long scheduleId);

    /** 一次排多天（同一個上下班時間）；該員工當天已有同開始時間的班會略過，回傳實際新增的班表 */
    List<EmployeeSchedule> createBatch(Long employeeId, List<LocalDate> workDates,
                                       java.time.LocalTime startTime, java.time.LocalTime endTime);

    void delete(Long scheduleId);
}
