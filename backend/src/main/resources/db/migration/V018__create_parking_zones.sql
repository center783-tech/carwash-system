-- =========================================================
-- V018: 車位區 (parking_zones)，預約時需選擇車位區，且該區在預約時段內要有空位
-- capacity = 該區的車位數；同一時段內未取消的預約數不得超過 capacity
-- =========================================================
CREATE TABLE parking_zones (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL UNIQUE,
    capacity    INT         NOT NULL DEFAULT 1,
    status      INT         NOT NULL DEFAULT 1,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 既有預約沒有車位區，所以允許 NULL；新建立的預約由程式強制必填
ALTER TABLE reservations
    ADD COLUMN zone_id BIGINT NULL,
    ADD CONSTRAINT fk_res_zone FOREIGN KEY (zone_id) REFERENCES parking_zones(id);

-- 預設建一個區，避免上線後沒有任何車位區導致無法預約；後台可再修改車位數或新增其他區
INSERT INTO parking_zones (name, capacity, status) VALUES ('A 區', 3, 1);
