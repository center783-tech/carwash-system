-- =========================================================
-- V007: 建立預約資料表 (reservations) -- 核心表
-- =========================================================
CREATE TABLE reservations (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id            BIGINT   NOT NULL,
    vehicle_id           BIGINT   NOT NULL,
    service_id           BIGINT   NOT NULL,
    employee_id          BIGINT,                         -- 可為 NULL，後台再指派
    reservation_date     DATE     NOT NULL,
    time_slot_start      TIME     NOT NULL,
    time_slot_end        TIME     NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
        -- PENDING / CONFIRMED / IN_PROGRESS / COMPLETED / CANCELLED
    total_amount         DECIMAL(10,2) NOT NULL,
    coupon_id            BIGINT,                        -- 使用的優惠券 (可為 NULL)
    remark                VARCHAR(255),
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_res_member   FOREIGN KEY (member_id)  REFERENCES members(id),
    CONSTRAINT fk_res_vehicle  FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_res_service  FOREIGN KEY (service_id) REFERENCES services(id),
    CONSTRAINT fk_res_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    INDEX idx_res_date_time (reservation_date, time_slot_start)
);
