-- =========================================================
-- V005: 建立員工可服務項目對應表 (employee_services, 多對多)
-- =========================================================
CREATE TABLE employee_services (
    employee_id     BIGINT NOT NULL,
    service_id      BIGINT NOT NULL,
    PRIMARY KEY (employee_id, service_id),
    CONSTRAINT fk_es_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_es_service  FOREIGN KEY (service_id)  REFERENCES services(id)
);
