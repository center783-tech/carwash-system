package com.carwash.controller.dto;

import com.carwash.entity.Admin;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AccountResponse {
    private Long id;
    private String username;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;

    public static AccountResponse from(Admin admin) {
        return new AccountResponse(admin.getId(), admin.getUsername(), admin.getRole().name(),
                admin.getStatus(), admin.getCreatedAt());
    }
}
