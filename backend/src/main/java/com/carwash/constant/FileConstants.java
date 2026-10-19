package com.carwash.constant;

import java.util.Set;

/**
 * MinIO（或 AWS S3）物件儲存相關常數，給之後實作「評價附照片」「服務項目封面圖」
 * 「員工大頭照」等上傳功能時統一使用，避免 bucket 名稱、副檔名限制散落各處。
 */
public final class FileConstants {

    private FileConstants() {
    }

    // ------- Bucket 名稱：依用途分開，方便之後個別設定存取權限/生命週期規則 -------
    public static final String BUCKET_SERVICE_IMAGES = "carwash-service-images";
    public static final String BUCKET_REVIEW_IMAGES = "carwash-review-images";
    public static final String BUCKET_MEMBER_AVATARS = "carwash-member-avatars";

    // ------- 上傳限制 -------
    public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    public static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    public static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp"
    );

    /** 單一服務項目或單一評價最多允許上傳的圖片數 */
    public static final int MAX_IMAGES_PER_UPLOAD = 5;
}
