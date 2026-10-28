package com.carwash.service;

import com.carwash.entity.ServiceItem;

import java.util.List;

public interface ServiceItemService {

    ServiceItem create(ServiceItem serviceItem);

    ServiceItem update(Long id, ServiceItem updates);

    void delete(Long id);

    ServiceItem getById(Long id);

    List<ServiceItem> listActive();

    List<ServiceItem> listByCategory(String category);

    /** 後台用：含已下架項目 */
    List<ServiceItem> listAll();

    /** 組合包含的單項服務 id；非組合回傳空清單 */
    List<Long> bundleItemIds(ServiceItem item);

    /** 組合內各單項目前價格加總；非組合回傳 null */
    java.math.BigDecimal bundleOriginalPrice(ServiceItem item);
}
