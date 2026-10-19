package com.carwash.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String tokenType;
    private Long userId;
    private String username;
    private String role;

    public static LoginResponse of(String token, Long userId, String username, String role) {
        return new LoginResponse(token, "Bearer", userId, username, role);
    }
}
