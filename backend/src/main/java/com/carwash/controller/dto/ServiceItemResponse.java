package com.carwash.controller.dto;

import com.carwash.entity.ServiceItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ServiceItemResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
    private String category;
    private String imageUrl;
    private Integer status;

    public static ServiceItemResponse from(ServiceItem item) {
        return new ServiceItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getDurationMinutes(),
                item.getCategory(),
                item.getImageUrl(),
                item.getStatus()
        );
    }
}
