package com.carwash.controller;

import com.carwash.controller.dto.CouponRequest;
import com.carwash.controller.dto.CouponResponse;
import com.carwash.controller.dto.MemberCouponResponse;
import com.carwash.entity.Coupon;
import com.carwash.response.ApiResponse;
import com.carwash.service.CouponService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "優惠券", description = "優惠券建立、發放給會員、會員優惠券查詢")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponRequest req) {
        Coupon coupon = Coupon.builder()
                .code(req.getCode())
                .discountType(req.getDiscountType())
                .discountValue(req.getDiscountValue())
                .minAmount(req.getMinAmount())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .usageLimit(req.getUsageLimit())
                .build();
        return ApiResponse.success("優惠券建立成功", CouponResponse.from(couponService.create(coupon)));
    }

    @GetMapping
    public ApiResponse<List<CouponResponse>> listActive() {
        return ApiResponse.success(couponService.listActive().stream()
                .map(CouponResponse::from).collect(Collectors.toList()));
    }

    @PostMapping("/{couponId}/issue")
    public ApiResponse<MemberCouponResponse> issueToMember(@PathVariable Long couponId, @RequestParam Long memberId) {
        return ApiResponse.success("發放成功",
                MemberCouponResponse.from(couponService.issueToMember(memberId, couponId)));
    }

    @GetMapping("/member/{memberId}")
    public ApiResponse<List<MemberCouponResponse>> listMemberCoupons(@PathVariable Long memberId) {
        return ApiResponse.success(couponService.listMemberCoupons(memberId).stream()
                .map(MemberCouponResponse::from).collect(Collectors.toList()));
    }
}
