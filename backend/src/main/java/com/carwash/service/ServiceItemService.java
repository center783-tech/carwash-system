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
}
