package com.carwash.controller;

import com.carwash.controller.dto.ClosedDateBatchRequest;
import com.carwash.controller.dto.ClosedDateReasonRequest;
import com.carwash.controller.dto.ClosedDateRequest;
import com.carwash.entity.ClosedDate;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.ClosedDateRepository;
import com.carwash.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "休息日", description = "後台設定休息日；會員預約時休息日不可選")
@RestController
@RequiredArgsConstructor
public class ClosedDateController {

    private final ClosedDateRepository closedDateRepository;

    /** 會員預約：今天起的休息日清單（登入即可） */
    @GetMapping("/api/closed-dates")
    public ApiResponse<List<ClosedDate>> upcoming() {
        return ApiResponse.success(
                closedDateRepository.findByClosedDateGreaterThanEqualOrderByClosedDateAsc(LocalDate.now()));
    }

    // 以下僅限後台（SecurityConfig 的 /api/admin/** 規則）

    @GetMapping("/api/admin/closed-dates")
    public ApiResponse<List<ClosedDate>> listUpcoming() {
        return upcoming();
    }

    @PostMapping("/api/admin/closed-dates")
    public ApiResponse<ClosedDate> create(@Valid @RequestBody ClosedDateRequest req) {
        if (closedDateRepository.existsByClosedDate(req.getClosedDate())) {
            throw new BusinessException("該日期已經是休息日");
        }
        ClosedDate saved = closedDateRepository.save(ClosedDate.builder()
                .closedDate(req.getClosedDate())
                .reason(req.getReason())
                .build());
        return ApiResponse.success("休息日新增成功", saved);
    }

    /** 一次設定多天為休息日（已是休息日的會略過），備註可之後再逐筆補 */
    @PostMapping("/api/admin/closed-dates/batch")
    public ApiResponse<List<ClosedDate>> createBatch(@Valid @RequestBody ClosedDateBatchRequest req) {
        List<ClosedDate> saved = req.getDates().stream()
                .distinct()
                .filter(d -> !closedDateRepository.existsByClosedDate(d))
                .map(d -> closedDateRepository.save(ClosedDate.builder().closedDate(d).reason(req.getReason()).build()))
                .toList();
        return ApiResponse.success("已新增 " + saved.size() + " 個休息日", saved);
    }

    /** 修改某個休息日的備註 */
    @PutMapping("/api/admin/closed-dates/{id}")
    public ApiResponse<ClosedDate> updateReason(@PathVariable Long id, @Valid @RequestBody ClosedDateReasonRequest req) {
        ClosedDate cd = closedDateRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ClosedDate", id));
        cd.setReason(req.getReason() == null || req.getReason().isBlank() ? null : req.getReason().trim());
        return ApiResponse.success("備註已更新", closedDateRepository.save(cd));
    }

    @DeleteMapping("/api/admin/closed-dates/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        if (!closedDateRepository.existsById(id)) {
            throw ResourceNotFoundException.of("ClosedDate", id);
        }
        closedDateRepository.deleteById(id);
        return ApiResponse.success("休息日已取消", null);
    }
}
