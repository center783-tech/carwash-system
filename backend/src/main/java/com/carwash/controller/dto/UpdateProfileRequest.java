package com.carwash.controller.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String realName;
    private String phone;

    /** 選填，先呼叫 POST /api/files/avatars 上傳拿到 URL，再把 URL 填進這裡 */
    private String avatarUrl;
}
