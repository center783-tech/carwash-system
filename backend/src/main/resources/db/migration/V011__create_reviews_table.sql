-- =========================================================
-- V011: 建立評價資料表 (reviews)
-- =========================================================
CREATE TABLE reviews (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id  BIGINT   NOT NULL,
    member_id       BIGINT   NOT NULL,
    rating          TINYINT  NOT NULL,         -- 1~5
    comment         VARCHAR(500),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id),
    CONSTRAINT fk_review_member FOREIGN KEY (member_id) REFERENCES members(id)
);
