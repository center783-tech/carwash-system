package com.carwash.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CouponBatchIssueRequest {

    @NotEmpty(message = "請至少勾選一位會員")
    @Size(max = 200, message = "一次最多發放給 200 位會員")
    private List<Long> memberIds;
}
