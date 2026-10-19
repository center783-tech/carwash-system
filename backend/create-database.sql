-- =========================================================
-- 建立資料庫本身，這一步要在 Flyway migration (V001~V014) 之前手動執行一次。
-- 原因：Flyway 是連進 .env 裡 DB_URL 指定的那個資料庫（carwash_db）去跑 migration，
-- 但如果 carwash_db 這個 schema 根本不存在，Flyway 會直接連線失敗。
-- =========================================================
CREATE DATABASE IF NOT EXISTS carwash_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- project26_v1 原本是另一個資料庫配發的帳號，預設不會有 carwash_db 的存取權，
-- 這裡手動把權限開給它（跟它原本登入用的密碼設定一致：無密碼）。
-- 用 '%' 而不是 'localhost'：避免 JDBC 用 TCP 連線時，MySQL 對 host 的比對跟 'localhost' 對不起來。
GRANT ALL PRIVILEGES ON carwash_db.* TO 'project26_v1'@'%';
FLUSH PRIVILEGES;
