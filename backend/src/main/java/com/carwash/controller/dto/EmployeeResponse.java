package com.carwash.controller.dto;

import com.carwash.entity.Employee;
import com.carwash.entity.ServiceItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class EmployeeResponse {

    private Long id;
    private String name;
    private String phone;
    private Integer status;
    private List<String> serviceItemNames;

    public static EmployeeResponse from(Employee employee) {
        List<String> names = employee.getServiceItems().stream()
                .map(ServiceItem::getName)
                .collect(Collectors.toList());
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getPhone(),
                employee.getStatus(),
                names
        );
    }
}
