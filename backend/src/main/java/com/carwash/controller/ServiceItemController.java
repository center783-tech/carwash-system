package com.carwash.controller;

import com.carwash.controller.dto.ServiceItemRequest;
import com.carwash.controller.dto.ServiceItemResponse;
import com.carwash.entity.ServiceItem;
import com.carwash.response.ApiResponse;
import com.carwash.service.ServiceItemService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "服務項目", description = "洗車/美容/保養服務項目的瀏覽與後台維護")
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    @PostMapping
    public ApiResponse<ServiceItemResponse> create(@Valid @RequestBody ServiceItemRequest req) {
        ServiceItem item = toEntity(req);
        return ApiResponse.success("新增服務項目成功", toResponse(serviceItemService.create(item)));
    }

    @GetMapping
    public ApiResponse<List<ServiceItemResponse>> list(@RequestParam(required = false) String category) {
        List<ServiceItem> items = (category != null)
                ? serviceItemService.listByCategory(category)
                : serviceItemService.listActive();
        return ApiResponse.success(items.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/admin/all")
    public ApiResponse<List<ServiceItemResponse>> listAllForAdmin() {
        return ApiResponse.success(serviceItemService.listAll().stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceItemResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(toResponse(serviceItemService.getById(id)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ServiceItemResponse> update(@PathVariable Long id, @Valid @RequestBody ServiceItemRequest req) {
        ServiceItem updates = toEntity(req);
        return ApiResponse.success("更新成功", toResponse(serviceItemService.update(id, updates)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        serviceItemService.delete(id);
        return ApiResponse.success("下架成功", null);
    }

    private ServiceItem toEntity(ServiceItemRequest req) {
        return ServiceItem.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .durationMinutes(req.getDurationMinutes())
                .category(req.getCategory())
                .imageUrl(req.getImageUrl())
                .status(req.getStatus())
                .bundle(Boolean.TRUE.equals(req.getBundle()) ? 1 : 0)
                .bundleItemIds(req.getBundleItemIds() == null ? null
                        : req.getBundleItemIds().stream().map(String::valueOf).collect(Collectors.joining(",")))
                .build();
    }

    private ServiceItemResponse toResponse(ServiceItem item) {
        return ServiceItemResponse.from(item, serviceItemService.bundleItemIds(item),
                serviceItemService.bundleOriginalPrice(item));
    }
}
