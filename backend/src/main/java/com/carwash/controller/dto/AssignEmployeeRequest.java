package com.carwash.controller.dto;

import lombok.Data;

@Data
public class AssignEmployeeRequest {

    /** 要指派的員工 id；null 代表取消指派 */
    private Long employeeId;
}
