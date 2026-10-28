package com.carwash.service.impl;

import com.carwash.entity.Employee;
import com.carwash.entity.ServiceItem;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.EmployeeRepository;
import com.carwash.repository.EmployeeScheduleRepository;
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
    private final EmployeeScheduleRepository scheduleRepository;

    @Override
    public Employee create(Employee employee) {
        employee.setStatus(1);
        return employeeRepository.save(employee);
    }

    @Override
    public Employee update(Long id, String name, String phone) {
        Employee employee = getById(id);
        employee.setName(name.trim());
        employee.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        return employeeRepository.save(employee);
    }

    @Override
    public Employee updateStatus(Long id, Integer status) {
        Employee employee = getById(id);
        employee.setStatus(status);
        // 停用 = 離職或暫停，已排的班全部刪除（重新啟用不會恢復，需重新排班）
        if (status == 0) {
            scheduleRepository.deleteByEmployeeId(id);
        }
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
        ServiceItem item = serviceItemRepository.findById(serviceId).orElse(null);
        if (item != null && Boolean.TRUE.equals(item.getBundle()) && item.getBundleItemIds() != null) {
            // 組合：員工必須會做組合內的每一項服務
            List<Employee> result = null;
            for (String id : item.getBundleItemIds().split(",")) {
                List<Employee> capable = employeeRepository.findAvailableByServiceId(Long.valueOf(id.trim()));
                if (result == null) {
                    result = new java.util.ArrayList<>(capable);
                } else {
                    result.retainAll(capable);
                }
            }
            return result == null ? List.of() : result;
        }
        return employeeRepository.findAvailableByServiceId(serviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }
}
