-- =========================================================
-- V012: 建立後台管理人員資料表 (admins)
-- =========================================================
CREATE TABLE admins (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',  -- ADMIN / STAFF
    status          TINYINT      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
