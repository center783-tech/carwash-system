package com.carwash.service;

import com.carwash.entity.Employee;

import java.util.List;

public interface EmployeeService {

    Employee create(Employee employee);

    Employee update(Long id, String name, String phone);

    /** 啟用 / 停用員工；停用後不會出現在可指派清單、已排的班全部刪除，既有預約不受影響 */
    Employee updateStatus(Long id, Integer status);

    Employee assignServices(Long employeeId, List<Long> serviceIds);

    Employee getById(Long id);

    List<Employee> findAvailableByService(Long serviceId);

    List<Employee> findAll();
}
