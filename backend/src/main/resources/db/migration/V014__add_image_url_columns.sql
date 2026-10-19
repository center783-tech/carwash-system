-- =========================================================
-- V014: 新增圖片網址欄位，對應 MinIO 檔案上傳功能
--       services.image_url      -> 服務項目封面圖
--       reviews.image_url       -> 評價附圖
--       members.avatar_url      -> 會員大頭照
-- =========================================================
ALTER TABLE services ADD COLUMN image_url VARCHAR(500) NULL AFTER category;
ALTER TABLE reviews  ADD COLUMN image_url VARCHAR(500) NULL AFTER comment;
ALTER TABLE members  ADD COLUMN avatar_url VARCHAR(500) NULL AFTER real_name;
