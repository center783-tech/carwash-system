package com.carwash.repository;

import com.carwash.entity.EmployeeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeScheduleRepository extends JpaRepository<EmployeeSchedule, Long> {

    List<EmployeeSchedule> findByEmployeeId(Long employeeId);

    List<EmployeeSchedule> findByWorkDate(LocalDate workDate);

    /** 總班表：指定日期範圍內「在職員工」的排班（含請假），員工姓名一併載入 */
    @org.springframework.data.jpa.repository.Query("select s from EmployeeSchedule s join fetch s.employee e "
            + "where s.workDate between :from and :to and e.status = 1 "
            + "order by s.workDate, s.startTime, e.name")
    List<EmployeeSchedule> findActiveByWorkDateBetween(
            @org.springframework.data.repository.query.Param("from") LocalDate from,
            @org.springframework.data.repository.query.Param("to") LocalDate to);

    /** 停用員工時，把他所有的排班一併刪除 */
    void deleteByEmployeeId(Long employeeId);

    boolean existsByEmployeeIdAndWorkDateAndStartTime(Long employeeId, LocalDate workDate, java.time.LocalTime startTime);

    List<EmployeeSchedule> findByEmployeeIdAndWorkDateGreaterThanEqualOrderByWorkDateAscStartTimeAsc(Long employeeId, LocalDate from);

    List<EmployeeSchedule> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);
}
