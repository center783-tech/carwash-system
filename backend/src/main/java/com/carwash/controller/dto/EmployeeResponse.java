package com.carwash.controller.dto;

import com.carwash.entity.Employee;
import com.carwash.entity.ServiceItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class EmployeeResponse {

    private Long id;
    private String name;
    private String phone;
    private String title;
    private Integer status;
    private List<String> serviceItemNames;
    private List<Long> serviceItemIds;

    public static EmployeeResponse from(Employee employee) {
        // 只對 Hibernate 懶載入集合觸發一次初始化（轉成一般 ArrayList），
        // 避免像之前那樣對同一個懶載入集合連續呼叫兩次 .stream() 導致 ConcurrentModificationException
        List<ServiceItem> items = new ArrayList<>(employee.getServiceItems());
        List<String> names = items.stream()
                .map(ServiceItem::getName)
                .collect(Collectors.toList());
        List<Long> ids = items.stream()
                .map(ServiceItem::getId)
                .collect(Collectors.toList());
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getPhone(),
                employee.getTitle(),
                employee.getStatus(),
                names,
                ids
        );
    }
}
