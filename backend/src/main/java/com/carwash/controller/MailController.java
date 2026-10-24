package com.carwash.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.carwash.controller.dto.MailSendRequest;
import com.carwash.service.MailService;
import com.carwash.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
public class MailController {
    private MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping("/send/gmail")
    public ApiResponse<Void> sendEmail(@Valid @RequestBody MailSendRequest request) {
        mailService.sendEmail(request.getToMailAddress(), request.getSubject(), request.getContent());
        return ApiResponse.success("寄送Gmail成功", null);
    }
}
