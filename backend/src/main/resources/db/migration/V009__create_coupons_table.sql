-- =========================================================
-- V009: 建立優惠券資料表 (coupons)
-- =========================================================
CREATE TABLE coupons (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(30)   NOT NULL UNIQUE,
    discount_type   VARCHAR(20)   NOT NULL,     -- FIXED / PERCENTAGE
    discount_value  DECIMAL(10,2) NOT NULL,
    min_amount      DECIMAL(10,2) DEFAULT 0,
    start_date      DATE          NOT NULL,
    end_date        DATE          NOT NULL,
    usage_limit     INT           DEFAULT NULL, -- NULL 表示不限
    status          TINYINT       NOT NULL DEFAULT 1,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
