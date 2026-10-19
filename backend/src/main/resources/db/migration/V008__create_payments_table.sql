CREATE TABLE payments (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id  BIGINT        NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    payment_method  VARCHAR(30)   NOT NULL,
    payment_status  VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    transaction_no  VARCHAR(100),
    paid_at         DATETIME,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id)
);
