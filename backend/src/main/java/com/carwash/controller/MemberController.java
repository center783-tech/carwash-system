package com.carwash.controller;

import com.carwash.controller.dto.*;
import com.carwash.entity.Member;
import com.carwash.response.ApiResponse;
import com.carwash.security.JwtTokenProvider;
import com.carwash.security.UserPrincipal;
import com.carwash.service.MemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "會員", description = "會員註冊、登入、個資維護")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ApiResponse<MemberResponse> register(@Valid @RequestBody RegisterRequest req) {
        Member member = Member.builder()
                .username(req.getUsername())
                .password(req.getPassword())
                .email(req.getEmail())
                .phone(req.getPhone())
                .realName(req.getRealName())
                .build();
        return ApiResponse.success("註冊成功", MemberResponse.from(memberService.register(member)));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        Member member = memberService.login(req.getUsername(), req.getPassword());
        String token = jwtTokenProvider.generateToken(UserPrincipal.fromMember(member));
        return ApiResponse.success(LoginResponse.of(token, member.getId(), member.getUsername(), "MEMBER"));
    }

    @GetMapping("/{id}")
    public ApiResponse<MemberResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(MemberResponse.from(memberService.getById(id)));
    }

    @PutMapping("/{id}")
    public ApiResponse<MemberResponse> updateProfile(@PathVariable Long id, @RequestBody UpdateProfileRequest req) {
        Member updates = Member.builder()
                .realName(req.getRealName())
                .phone(req.getPhone())
                .avatarUrl(req.getAvatarUrl())
                .build();
        return ApiResponse.success("更新成功", MemberResponse.from(memberService.updateProfile(id, updates)));
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest req) {
        memberService.changePassword(id, req.getOldPassword(), req.getNewPassword());
        return ApiResponse.success("密碼修改成功", null);
    }
}
