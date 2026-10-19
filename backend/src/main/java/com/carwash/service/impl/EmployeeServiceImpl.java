package com.carwash.service.impl;

import com.carwash.entity.Employee;
import com.carwash.entity.ServiceItem;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.EmployeeRepository;
import com.carwash.repository.ServiceItemRepository;
import com.carwash.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ServiceItemRepository serviceItemRepository;

    @Override
    public Employee create(Employee employee) {
        employee.setStatus(1);
        return employeeRepository.save(employee);
    }

    @Override
    public Employee assignServices(Long employeeId, List<Long> serviceIds) {
        Employee employee = getById(employeeId);
        Set<ServiceItem> items = new HashSet<>(serviceItemRepository.findAllById(serviceIds));
        employee.setServiceItems(items);
        return employeeRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAvailableByService(Long serviceId) {
        return employeeRepository.findAvailableByServiceId(serviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }
}
