package com.carwash.controller.dto;

import com.carwash.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MemberResponse {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String realName;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime createdAt;

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getUsername(),
                member.getEmail(),
                member.getPhone(),
                member.getRealName(),
                member.getAvatarUrl(),
                member.getStatus(),
                member.getCreatedAt()
        );
    }
}
