-- 藍新金流：付款單加上金流商、商店訂單編號、金流商交易序號
ALTER TABLE payments
    ADD COLUMN provider          VARCHAR(20) NULL AFTER payment_status,
    ADD COLUMN merchant_order_no VARCHAR(30) NULL AFTER provider,
    ADD COLUMN provider_trade_no VARCHAR(50) NULL AFTER merchant_order_no,
    ADD CONSTRAINT uk_payment_merchant_order_no UNIQUE (merchant_order_no);

-- 金流通知（NotifyURL）紀錄，保留原始內容方便對帳與除錯
CREATE TABLE payment_notifications (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_id        BIGINT       NULL,
    provider          VARCHAR(20)  NOT NULL,
    merchant_order_no VARCHAR(30)  NULL,
    raw_body          TEXT         NULL,
    status            VARCHAR(20)  NOT NULL,
    message           VARCHAR(255) NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    INDEX idx_notification_order_no (merchant_order_no)
);
