-- =========================================================
-- V019: 休息日 (closed_dates)，後台設定的日期不開放預約
-- =========================================================
CREATE TABLE closed_dates (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    closed_date  DATE         NOT NULL UNIQUE,
    reason       VARCHAR(100) NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
