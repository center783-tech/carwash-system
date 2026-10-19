-- =========================================================
-- V002: 建立車輛資料表 (vehicles)
-- =========================================================
CREATE TABLE vehicles (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id       BIGINT       NOT NULL,
    plate_number    VARCHAR(20)  NOT NULL,
    brand           VARCHAR(50),
    model           VARCHAR(50),
    color           VARCHAR(20),
    car_type        VARCHAR(20),                     -- 小型車/休旅車/貨車...
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_member FOREIGN KEY (member_id) REFERENCES members(id)
);
