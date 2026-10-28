package com.carwash.service.impl;

import com.carwash.constant.RedisKeyConstants;
import com.carwash.entity.ServiceItem;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.ServiceItemRepository;
import com.carwash.service.ServiceItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceItemRepository serviceItemRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ServiceItem create(ServiceItem serviceItem) {
        if (serviceItem.getStatus() == null) {
            serviceItem.setStatus(1);
        }
        applyBundleRules(serviceItem, null);
        ServiceItem saved = serviceItemRepository.save(serviceItem);
        evictListCaches(saved.getCategory());
        return saved;
    }

    @Override
    public ServiceItem update(Long id, ServiceItem updates) {
        ServiceItem item = getById(id);
        String oldCategory = item.getCategory();

        item.setName(updates.getName());
        item.setDescription(updates.getDescription());
        item.setPrice(updates.getPrice());
        item.setDurationMinutes(updates.getDurationMinutes());
        item.setCategory(updates.getCategory());
        item.setImageUrl(updates.getImageUrl());
        item.setBundle(updates.getBundle());
        item.setBundleItemIds(updates.getBundleItemIds());
        if (updates.getStatus() != null) {
            item.setStatus(updates.getStatus());
        }
        applyBundleRules(item, id);
        ServiceItem saved = serviceItemRepository.save(item);

        // 分類可能被改掉，新舊分類的快取都要清，不然舊分類頁面還會看到已經不屬於這個分類的項目
        evictListCaches(oldCategory);
        evictListCaches(saved.getCategory());
        return saved;
    }

    @Override
    public void delete(Long id) {
        ServiceItem item = getById(id);
        item.setStatus(0); // 軟刪除，避免已被預約引用的資料被實際刪除
        serviceItemRepository.save(item);
        evictListCaches(item.getCategory());
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceItem getById(Long id) {
        // 單筆查詢讀取頻率低、不是瀏覽頁的效能瓶頸，不快取，資料一定是最新的
        return serviceItemRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ServiceItem", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceItem> listActive() {
        return getCachedList(
                RedisKeyConstants.SERVICE_ACTIVE_LIST,
                () -> serviceItemRepository.findByStatus(1)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceItem> listByCategory(String category) {
        return getCachedList(
                RedisKeyConstants.serviceCategoryListKey(category),
                () -> serviceItemRepository.findByCategoryAndStatus(category, 1)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceItem> listAll() {
        return serviceItemRepository.findAll();
    }

    @Override
    public List<Long> bundleItemIds(ServiceItem item) {
        if (!Boolean.TRUE.equals(item.getBundle()) || item.getBundleItemIds() == null
                || item.getBundleItemIds().isBlank()) {
            return List.of();
        }
        return Arrays.stream(item.getBundleItemIds().split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal bundleOriginalPrice(ServiceItem item) {
        List<Long> ids = bundleItemIds(item);
        if (ids.isEmpty()) {
            return null;
        }
        return serviceItemRepository.findAllById(ids).stream()
                .map(ServiceItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 組合的規則檢查與整理：至少 2 個單項、不可包含組合或自己、單項需存在；
     * 服務時長由各單項加總（預約時段長度依此計算），非組合則清掉組合欄位。
     */
    private void applyBundleRules(ServiceItem item, Long selfId) {
        if (!Boolean.TRUE.equals(item.getBundle())) {
            item.setBundle(false);
            item.setBundleItemIds(null);
            return;
        }
        List<Long> ids = bundleItemIds(item).stream().distinct().collect(Collectors.toList());
        if (ids.size() < 2) {
            throw new BusinessException("組合優惠至少要選 2 個服務項目");
        }
        if (selfId != null && ids.contains(selfId)) {
            throw new BusinessException("組合不能包含自己");
        }
        List<ServiceItem> children = serviceItemRepository.findAllById(ids);
        if (children.size() != ids.size()) {
            throw new BusinessException("組合內有不存在的服務項目");
        }
        if (children.stream().anyMatch(c -> Boolean.TRUE.equals(c.getBundle()))) {
            throw new BusinessException("組合內不能再包含其他組合");
        }
        item.setBundleItemIds(ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
        item.setDurationMinutes(children.stream().mapToInt(ServiceItem::getDurationMinutes).sum());
    }

    /**
     * 快取讀取共用邏輯：Redis 有資料就直接回傳，沒有的話查資料庫再寫回快取。
     * Redis 連線異常時只記 warning、改查資料庫，不讓快取層的問題影響主要功能（服務項目瀏覽）。
     */
    @SuppressWarnings("unchecked")
    private List<ServiceItem> getCachedList(String key, Supplier<List<ServiceItem>> loader) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return (List<ServiceItem>) cached;
            }
        } catch (Exception e) {
            log.warn("讀取 Redis 快取失敗，key={}，改查資料庫: {}", key, e.getMessage());
        }

        List<ServiceItem> result = loader.get();

        try {
            redisTemplate.opsForValue().set(key, result, RedisKeyConstants.SERVICE_LIST_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("寫入 Redis 快取失敗，key={}: {}", key, e.getMessage());
        }

        return result;
    }

    private void evictListCaches(String category) {
        try {
            redisTemplate.delete(RedisKeyConstants.SERVICE_ACTIVE_LIST);
            if (category != null) {
                redisTemplate.delete(RedisKeyConstants.serviceCategoryListKey(category));
            }
        } catch (Exception e) {
            log.warn("清除 Redis 快取失敗: {}", e.getMessage());
        }
    }
}
