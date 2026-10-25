package com.carwash.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmployeeRequest {

    @NotBlank(message = "員工姓名不可為空")
    @Size(max = 50, message = "員工姓名最多 50 字")
    private String name;

    /** 選填；有填的話要是 09 開頭共 10 碼的手機號碼 */
    @Pattern(regexp = "^(09\\d{8})?$", message = "電話格式錯誤，請輸入 09 開頭共 10 碼的手機號碼")
    private String phone;
}
