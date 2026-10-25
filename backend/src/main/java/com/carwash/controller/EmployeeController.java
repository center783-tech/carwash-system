package com.carwash.controller;

import com.carwash.controller.dto.AssignServicesRequest;
import com.carwash.controller.dto.EmployeeRequest;
import com.carwash.controller.dto.EmployeeResponse;
import com.carwash.controller.dto.EmployeeStatusRequest;
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

    @PutMapping("/{id}")
    public ApiResponse<EmployeeResponse> update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest req) {
        return ApiResponse.success("員工資料已更新",
                EmployeeResponse.from(employeeService.update(id, req.getName(), req.getPhone())));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<EmployeeResponse> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody EmployeeStatusRequest req) {
        return ApiResponse.success(req.getStatus() == 1 ? "員工已啟用" : "員工已停用",
                EmployeeResponse.from(employeeService.updateStatus(id, req.getStatus())));
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
