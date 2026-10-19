package com.carwash.controller;

import com.carwash.controller.dto.LoginRequest;
import com.carwash.controller.dto.LoginResponse;
import com.carwash.entity.Admin;
import com.carwash.response.ApiResponse;
import com.carwash.security.JwtTokenProvider;
import com.carwash.security.UserPrincipal;
import com.carwash.service.AdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "後台管理員", description = "後台管理員登入")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        Admin admin = adminService.login(req.getUsername(), req.getPassword());
        String token = jwtTokenProvider.generateToken(UserPrincipal.fromAdmin(admin));
        return ApiResponse.success(LoginResponse.of(token, admin.getId(), admin.getUsername(), admin.getRole().name()));
    }
}
