-- =========================================================
-- V006: 建立員工排班資料表 (employee_schedules)
-- =========================================================
CREATE TABLE employee_schedules (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id     BIGINT   NOT NULL,
    work_date       DATE     NOT NULL,
    start_time      TIME     NOT NULL,
    end_time        TIME     NOT NULL,
    status          TINYINT  NOT NULL DEFAULT 1,       -- 1:正常 0:請假
    CONSTRAINT fk_schedule_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    UNIQUE KEY uk_employee_date_time (employee_id, work_date, start_time)
);
