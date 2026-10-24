-- 車輛改為軟刪除：有預約紀錄（含已取消）的車輛不能真的刪掉，改成標記為已刪除並從清單隱藏
ALTER TABLE vehicles
    ADD COLUMN deleted INT NOT NULL DEFAULT 0;
