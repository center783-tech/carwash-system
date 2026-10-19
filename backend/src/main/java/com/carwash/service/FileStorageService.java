package com.carwash.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /** 上傳服務項目封面圖，回傳可公開存取的 URL */
    String uploadServiceImage(MultipartFile file);

    /** 上傳評價附圖 */
    String uploadReviewImage(MultipartFile file);

    /** 上傳會員大頭照 */
    String uploadMemberAvatar(MultipartFile file);

    /** 刪除檔案，objectUrl 是 uploadXxx 系列方法回傳的完整 URL */
    void delete(String objectUrl);
}
