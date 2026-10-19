-- =========================================================
-- V004: 建立員工資料表 (employees)
-- =========================================================
CREATE TABLE employees (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(50)  NOT NULL,
    phone           VARCHAR(20),
    status          TINYINT      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
