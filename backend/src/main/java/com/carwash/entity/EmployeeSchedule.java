package com.carwash.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "employee_schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_employee_date_time",
                columnNames = {"employee_id", "work_date", "start_time"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** 1:正常 0:請假 */
    @Column(nullable = false)
    @Builder.Default
    private Integer status = 1;
}
