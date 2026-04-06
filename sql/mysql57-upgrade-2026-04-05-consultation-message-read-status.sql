SET NAMES utf8mb4;

USE `consultation`;

ALTER TABLE `db_consultation_message`
  ADD COLUMN `read_status` tinyint(1) NOT NULL DEFAULT 0 AFTER `status`,
  ADD COLUMN `read_time` datetime DEFAULT NULL AFTER `read_status`;

ALTER TABLE `db_consultation_message`
  ADD KEY `idx_consultation_message_read_status` (`consultation_id`, `read_status`, `create_time`);

UPDATE `db_consultation_message`
SET `read_status` = IFNULL(`read_status`, 0)
WHERE `read_status` IS NULL;

-- Notes:
-- 1. This script is for existing MySQL 5.7 databases that already have db_consultation_message.
-- 2. If your database already added these columns/indexes manually, skip the duplicated statements.
-- 3. Execute it directly in a SQL client; do not import patch diff text into MySQL.
