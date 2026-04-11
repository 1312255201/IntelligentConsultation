SET NAMES utf8mb4;

USE `consultation`;

ALTER TABLE `db_consultation_service_feedback`
  ADD COLUMN `doctor_handle_status` tinyint(1) NOT NULL DEFAULT 0 AFTER `feedback_text`,
  ADD COLUMN `doctor_handle_remark` varchar(1000) DEFAULT NULL AFTER `doctor_handle_status`,
  ADD COLUMN `doctor_handle_account_id` int DEFAULT NULL AFTER `doctor_handle_remark`,
  ADD COLUMN `doctor_handle_doctor_id` int DEFAULT NULL AFTER `doctor_handle_account_id`,
  ADD COLUMN `doctor_handle_doctor_name` varchar(50) DEFAULT NULL AFTER `doctor_handle_doctor_id`,
  ADD COLUMN `doctor_handle_time` datetime DEFAULT NULL AFTER `doctor_handle_doctor_name`;

ALTER TABLE `db_consultation_service_feedback`
  ADD KEY `idx_consultation_service_feedback_doctor_handle` (`doctor_id`, `doctor_handle_status`, `update_time`);

-- Notes:
-- 1. This script is for existing MySQL 5.7 databases.
-- 2. Execute it directly in a SQL client.
-- 3. If your database already contains any of these columns or indexes,
--    remove the duplicated clauses before running.
