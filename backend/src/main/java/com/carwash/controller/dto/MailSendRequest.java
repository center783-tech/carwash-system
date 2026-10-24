package com.carwash.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 寄送 email 時的 request body；validation message 放錯誤碼，再由 GlobalExceptionHandler 轉成語系訊息。
public class MailSendRequest {

    @NotBlank(message = "收件人郵件地址不能為空")
    @Email(message = "收件人郵件地址格式不正確")
    private String toMailAddress;

    @NotBlank(message = "主旨不能為空")
    @Size(max = 100, message = "主旨長度不能超過100個字元")
    private String subject;

    @NotBlank(message = "內容不能為空")
    @Size(max = 500, message = "內容長度不能超過500個字元")
    private String content;

    public String getToMailAddress() {
        return toMailAddress;
    }

    public void setToMailAddress(String toMailAddress) {
        this.toMailAddress = toMailAddress;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
