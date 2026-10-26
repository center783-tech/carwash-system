package com.carwash.controller;

import com.carwash.controller.dto.ReviewRequest;
import com.carwash.controller.dto.ReviewResponse;
import com.carwash.response.ApiResponse;
import com.carwash.security.AccessGuard;
import com.carwash.security.UserPrincipal;
import com.carwash.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "評價", description = "已完成預約的評分與留言")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ApiResponse<ReviewResponse> create(@Valid @RequestBody ReviewRequest req,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        AccessGuard.requireSelf(req.getMemberId(), principal);
        ReviewResponse response = ReviewResponse.from(reviewService.create(
                req.getReservationId(), req.getMemberId(), req.getRating(), req.getComment(), req.getImageUrl()));
        return ApiResponse.success("評價成功", response);
    }

    @GetMapping("/member/{memberId}")
    public ApiResponse<Page<ReviewResponse>> listByMember(@PathVariable Long memberId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        AccessGuard.requireSelfOrStaff(memberId, principal);
        Page<ReviewResponse> result = reviewService
                .listByMember(memberId, PageRequest.of(page, size))
                .map(ReviewResponse::from);
        return ApiResponse.success(result);
    }
}
