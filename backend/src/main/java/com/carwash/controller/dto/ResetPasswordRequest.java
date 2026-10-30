package com.carwash.controller.dto;

import com.carwash.constant.RegexConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "連結無效")
    private String token;

    @NotBlank(message = "請輸入新密碼")
    @Pattern(regexp = RegexConstants.PASSWORD_REGEX, message = "密碼至少 8 碼，需同時包含英文字母與數字")
    private String newPassword;
}
