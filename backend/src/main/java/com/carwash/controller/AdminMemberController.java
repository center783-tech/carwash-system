package com.carwash.controller;

import com.carwash.controller.dto.MemberResponse;
import com.carwash.repository.MemberRepository;
import com.carwash.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "後台會員清單", description = "後台分頁查詢會員（姓名、電話、Email），供發放優惠券勾選")
@RestController
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberRepository memberRepository;

    // 僅限後台（SecurityConfig 的 /api/admin/** 規則）
    @GetMapping("/api/admin/members")
    public ApiResponse<Page<MemberResponse>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("id").descending());
        return ApiResponse.success(memberRepository.search(keyword.trim(), pageable).map(MemberResponse::from));
    }
}
