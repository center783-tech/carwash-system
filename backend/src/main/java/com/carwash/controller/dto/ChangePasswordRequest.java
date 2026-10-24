package com.carwash.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "請輸入原密碼")
    private String oldPassword;

    @NotBlank(message = "請輸入新密碼")
    @Size(min = 8, message = "新密碼長度至少 8 字元")
    private String newPassword;

    @NotBlank(message = "請輸入 Email 驗證碼")
    @Pattern(regexp = "\\d{6}", message = "驗證碼為 6 位數字")
    private String verificationCode;
}
