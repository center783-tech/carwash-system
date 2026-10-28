-- =========================================================
-- V022: 服務項目支援「組合優惠」
-- is_bundle = 1 表示這筆是組合；bundle_item_ids 存組成它的單項服務 id（逗號分隔）
-- 組合的 price 即優惠價，原價由後端依各單項價格加總算出
-- =========================================================
ALTER TABLE services
    ADD COLUMN is_bundle       TINYINT NOT NULL DEFAULT 0,
    ADD COLUMN bundle_item_ids VARCHAR(255) NULL;
