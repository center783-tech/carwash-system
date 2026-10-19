package com.carwash.service.impl;

import com.carwash.constant.FileConstants;
import com.carwash.exception.BusinessException;
import com.carwash.service.FileStorageService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Override
    public String uploadServiceImage(MultipartFile file) {
        return upload(FileConstants.BUCKET_SERVICE_IMAGES, file);
    }

    @Override
    public String uploadReviewImage(MultipartFile file) {
        return upload(FileConstants.BUCKET_REVIEW_IMAGES, file);
    }

    @Override
    public String uploadMemberAvatar(MultipartFile file) {
        return upload(FileConstants.BUCKET_MEMBER_AVATARS, file);
    }

    private String upload(String bucket, MultipartFile file) {
        validate(file);

        try {
            ensureBucketExists(bucket);

            String objectName = buildObjectName(file);
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            }

            return buildPublicUrl(bucket, objectName);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上傳檔案到 MinIO 失敗，bucket={}", bucket, e);
            throw new BusinessException("檔案上傳失敗，請稍後再試");
        }
    }

    @Override
    public void delete(String objectUrl) {
        try {
            String base = endpoint.endsWith("/") ? endpoint : endpoint + "/";
            if (!objectUrl.startsWith(base)) {
                throw new BusinessException("無效的檔案網址");
            }

            String path = objectUrl.substring(base.length()); // {bucket}/{objectName}
            int slashIndex = path.indexOf('/');
            if (slashIndex < 0) {
                throw new BusinessException("無效的檔案網址");
            }
            String bucket = path.substring(0, slashIndex);
            String objectName = path.substring(slashIndex + 1);

            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("刪除 MinIO 檔案失敗，url={}", objectUrl, e);
            throw new BusinessException("檔案刪除失敗，請稍後再試");
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("請選擇要上傳的檔案");
        }
        if (file.getSize() > FileConstants.MAX_IMAGE_SIZE_BYTES) {
            throw new BusinessException("檔案大小不可超過 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !FileConstants.ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("僅支援 JPG / PNG / WEBP 格式的圖片");
        }
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (exists) {
            return;
        }

        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());

        // 設成公開可讀，前端才能直接用 uploadXxx 回傳的 URL 顯示圖片。
        // 如果不想公開（例如會員大頭照不希望被任意存取），改成用 presigned URL 取代直接回傳網址。
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [{
                    "Effect": "Allow",
                    "Principal": "*",
                    "Action": ["s3:GetObject"],
                    "Resource": ["arn:aws:s3:::%s/*"]
                  }]
                }
                """.formatted(bucket);
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
    }

    private String buildObjectName(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        // 用 UUID 當檔名，避免檔名衝突、避免使用者上傳的原始檔名洩漏資訊或造成路徑穿越風險
        return UUID.randomUUID() + ext;
    }

    private String buildPublicUrl(String bucket, String objectName) {
        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return base + "/" + bucket + "/" + objectName;
    }
}
