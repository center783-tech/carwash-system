package com.carwash.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "帳號不可為空")
    @Size(min = 4, max = 50, message = "帳號長度須介於 4~50 字元")
    private String username;

    @NotBlank(message = "密碼不可為空")
    @Size(min = 8, message = "密碼長度至少 8 字元")
    private String password;

    @NotBlank(message = "Email 不可為空")
    @Email(message = "Email 格式不正確")
    private String email;

    @NotBlank(message = "手機不可為空")
    private String phone;

    private String realName;
}
