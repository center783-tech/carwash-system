package com.carwash.service;

import com.carwash.entity.Employee;

import java.util.List;

public interface EmployeeService {

    Employee create(Employee employee);

    Employee assignServices(Long employeeId, List<Long> serviceIds);

    Employee getById(Long id);

    List<Employee> findAvailableByService(Long serviceId);

    List<Employee> findAll();
}
