package com.carwash.controller;

import com.carwash.controller.dto.AccountResponse;
import com.carwash.controller.dto.CreateAccountRequest;
import com.carwash.controller.dto.EmployeeStatusRequest;
import com.carwash.response.ApiResponse;
import com.carwash.security.UserPrincipal;
import com.carwash.service.AdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "後台帳號管理", description = "建立 / 停用後台帳號（僅限 ADMIN，見 SecurityConfig）")
@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<List<AccountResponse>> list() {
        return ApiResponse.success(adminService.listAccounts().stream().map(AccountResponse::from).toList());
    }

    @PostMapping
    public ApiResponse<AccountResponse> create(@Valid @RequestBody CreateAccountRequest req) {
        return ApiResponse.success("帳號建立成功",
                AccountResponse.from(adminService.createAccount(req.getUsername(), req.getPassword(), req.getRole())));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<AccountResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody EmployeeStatusRequest req,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(AccountResponse.from(adminService.updateStatus(id, req.getStatus(), principal.getId())));
    }
}
