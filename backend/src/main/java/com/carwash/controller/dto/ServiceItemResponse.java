package com.carwash.controller.dto;

import com.carwash.entity.ServiceItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

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
    private Boolean bundle;
    private List<Long> bundleItemIds;
    /** 組合內各單項加總的原價；非組合為 null */
    private BigDecimal originalPrice;

    public static ServiceItemResponse from(ServiceItem item, List<Long> bundleItemIds, BigDecimal originalPrice) {
        return new ServiceItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getDurationMinutes(),
                item.getCategory(),
                item.getImageUrl(),
                item.getStatus(),
                Integer.valueOf(1).equals(item.getBundle()),
                bundleItemIds,
                originalPrice
        );
    }
}
