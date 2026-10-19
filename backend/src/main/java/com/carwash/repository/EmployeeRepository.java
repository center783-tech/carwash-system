package com.carwash.repository;

import com.carwash.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByStatus(Integer status);

    @Query("SELECT e FROM Employee e JOIN e.serviceItems s WHERE s.id = :serviceId AND e.status = 1")
    List<Employee> findAvailableByServiceId(@Param("serviceId") Long serviceId);
}
