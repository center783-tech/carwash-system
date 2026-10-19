package com.carwash.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "請輸入原密碼")
    private String oldPassword;

    @NotBlank(message = "請輸入新密碼")
    @Size(min = 8, message = "新密碼長度至少 8 字元")
    private String newPassword;
}
