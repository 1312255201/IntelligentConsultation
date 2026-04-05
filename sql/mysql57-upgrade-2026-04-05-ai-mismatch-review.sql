SET NAMES utf8mb4;

USE `consultation`;

ALTER TABLE `db_consultation_doctor_conclusion`
  ADD COLUMN `ai_mismatch_reasons_json` varchar(500) DEFAULT NULL AFTER `is_consistent_with_ai`,
  ADD COLUMN `ai_mismatch_remark` varchar(500) DEFAULT NULL AFTER `ai_mismatch_reasons_json`;

-- Notes:
-- 1. This script is for existing MySQL 5.7 databases.
-- 2. Execute it directly in a SQL client.
-- 3. If the two columns already exist, skip this script.
