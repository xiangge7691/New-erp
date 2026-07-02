-- ===================================
-- V6: personnel_file 表 employee_no 加唯一约束
-- ===================================
ALTER TABLE personnel_file ADD UNIQUE INDEX uk_employee_no (employee_no);
