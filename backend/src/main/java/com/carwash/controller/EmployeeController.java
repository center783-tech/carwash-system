package com.carwash.controller;

import com.carwash.controller.dto.AssignServicesRequest;
import com.carwash.controller.dto.EmployeeRequest;
import com.carwash.controller.dto.EmployeeResponse;
import com.carwash.entity.Employee;
import com.carwash.response.ApiResponse;
import com.carwash.service.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "員工", description = "後台員工資料與可服務項目指派")
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ApiResponse<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest req) {
        Employee employee = Employee.builder().name(req.getName()).phone(req.getPhone()).build();
        return ApiResponse.success("新增員工成功", EmployeeResponse.from(employeeService.create(employee)));
    }

    @GetMapping
    public ApiResponse<List<EmployeeResponse>> list() {
        return ApiResponse.success(employeeService.findAll().stream()
                .map(EmployeeResponse::from).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ApiResponse<EmployeeResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(EmployeeResponse.from(employeeService.getById(id)));
    }

    @PutMapping("/{id}/services")
    public ApiResponse<EmployeeResponse> assignServices(@PathVariable Long id,
                                                          @Valid @RequestBody AssignServicesRequest req) {
        Employee employee = employeeService.assignServices(id, req.getServiceIds());
        return ApiResponse.success("指派服務項目成功", EmployeeResponse.from(employee));
    }

    @GetMapping("/available")
    public ApiResponse<List<EmployeeResponse>> findAvailable(@RequestParam Long serviceId) {
        return ApiResponse.success(employeeService.findAvailableByService(serviceId).stream()
                .map(EmployeeResponse::from).collect(Collectors.toList()));
    }
}
