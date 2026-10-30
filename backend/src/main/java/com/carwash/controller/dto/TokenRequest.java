package com.carwash.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRequest {

    @NotBlank(message = "連結無效")
    private String token;
}
