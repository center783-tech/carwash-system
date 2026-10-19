-- =========================================================
-- V003: 建立服務項目資料表 (services)
-- =========================================================
CREATE TABLE services (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    name             VARCHAR(50)    NOT NULL,         -- 精緻洗車/打蠟/鍍膜...
    description      VARCHAR(255),
    price            DECIMAL(10,2)  NOT NULL,
    duration_minutes INT            NOT NULL,          -- 預估服務時長
    category         VARCHAR(30),                     -- 洗車/美容/保養
    status           TINYINT        NOT NULL DEFAULT 1,
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
);
