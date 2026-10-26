package com.carwash.controller;

import com.carwash.controller.dto.*;
import com.carwash.entity.Member;
import com.carwash.response.ApiResponse;
import com.carwash.security.AccessGuard;
import com.carwash.security.JwtTokenProvider;
import com.carwash.security.UserPrincipal;
import com.carwash.exception.BusinessException;
import com.carwash.service.MemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ApiResponse<MemberResponse> getById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        AccessGuard.requireSelfOrStaff(id, principal);
        return ApiResponse.success(MemberResponse.from(memberService.getById(id)));
    }

    @PutMapping("/{id}")
    public ApiResponse<MemberResponse> updateProfile(@PathVariable Long id, @RequestBody UpdateProfileRequest req,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        AccessGuard.requireSelf(id, principal);
        Member updates = Member.builder()
                .realName(req.getRealName())
                .phone(req.getPhone())
                .avatarUrl(req.getAvatarUrl())
                .build();
        return ApiResponse.success("更新成功", MemberResponse.from(memberService.updateProfile(id, updates)));
    }

    @PostMapping("/{id}/password/code")
    public ApiResponse<Void> sendPasswordChangeCode(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        requireSelf(id, principal);
        memberService.sendPasswordChangeCode(id);
        return ApiResponse.success("驗證碼已寄送至您的 Email", null);
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest req,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        requireSelf(id, principal);
        memberService.changePassword(id, req.getOldPassword(), req.getNewPassword(), req.getVerificationCode());
        return ApiResponse.success("密碼修改成功", null);
    }

    /** 密碼相關操作只允許會員本人，避免登入後帶別人的 id 去改他人密碼 */
    private void requireSelf(Long id, UserPrincipal principal) {
        if (principal == null || !"MEMBER".equals(principal.getRole()) || !id.equals(principal.getId())) {
            throw new BusinessException("只能修改自己的密碼");
        }
    }
}
