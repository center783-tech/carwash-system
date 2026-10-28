package com.carwash.controller.dto;

import com.carwash.entity.EmployeeSchedule;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class EmployeeScheduleResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeTitle;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer status;

    public static EmployeeScheduleResponse from(EmployeeSchedule schedule) {
        return new EmployeeScheduleResponse(
                schedule.getId(),
                schedule.getEmployee().getId(),
                schedule.getEmployee().getName(),
                schedule.getEmployee().getTitle(),
                schedule.getWorkDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getStatus()
        );
    }
}
