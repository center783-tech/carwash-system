-- =========================================================
-- V010: 建立會員優惠券資料表 (member_coupons)
--       並補上 reservations.coupon_id 的外鍵
-- =========================================================
CREATE TABLE member_coupons (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id       BIGINT   NOT NULL,
    coupon_id       BIGINT   NOT NULL,
    is_used         TINYINT  NOT NULL DEFAULT 0,
    used_at         DATETIME,
    received_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mc_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_mc_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id)
);

ALTER TABLE reservations
    ADD CONSTRAINT fk_res_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id);
