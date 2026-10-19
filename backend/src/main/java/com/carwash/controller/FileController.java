package com.carwash.controller;

import com.carwash.response.ApiResponse;
import com.carwash.service.FileStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "檔案上傳", description = "服務項目封面圖、評價附圖、會員大頭照上傳（MinIO）")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /** 僅限後台使用，權限規則見 SecurityConfig */
    @PostMapping("/service-images")
    public ApiResponse<String> uploadServiceImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success("上傳成功", fileStorageService.uploadServiceImage(file));
    }

    @PostMapping("/review-images")
    public ApiResponse<String> uploadReviewImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success("上傳成功", fileStorageService.uploadReviewImage(file));
    }

    @PostMapping("/avatars")
    public ApiResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success("上傳成功", fileStorageService.uploadMemberAvatar(file));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@RequestParam String url) {
        fileStorageService.delete(url);
        return ApiResponse.success("刪除成功", null);
    }
}
