package com.carwash.service.impl;

import com.carwash.entity.Employee;
import com.carwash.repository.EmployeeRepository;
import com.carwash.repository.EmployeeScheduleRepository;
import com.carwash.repository.ServiceItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private ServiceItemRepository serviceItemRepository;
    @Mock private EmployeeScheduleRepository scheduleRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = Employee.builder().id(1L).name("小明").status(1).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        lenient().when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void updateStatus_disable_deletesAllSchedules() {
        Employee result = employeeService.updateStatus(1L, 0);

        assertThat(result.getStatus()).isEqualTo(0);
        verify(scheduleRepository).deleteByEmployeeId(1L);
    }

    @Test
    void updateStatus_enable_keepsSchedules() {
        employee.setStatus(0);

        Employee result = employeeService.updateStatus(1L, 1);

        assertThat(result.getStatus()).isEqualTo(1);
        verify(scheduleRepository, never()).deleteByEmployeeId(any());
    }
}
