package com.carwash.controller.dto;

import com.carwash.constant.AdminRole;
import com.carwash.constant.RegexConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAccountRequest {

    @NotBlank(message = "帳號不可為空")
    @Size(min = 3, max = 50, message = "帳號長度需為 3~50 字")
    private String username;

    @NotBlank(message = "密碼不可為空")
    @Pattern(regexp = RegexConstants.PASSWORD_REGEX, message = "密碼至少 8 碼，需同時包含英文字母與數字")
    private String password;

    @NotNull(message = "請選擇角色")
    private AdminRole role;
}
