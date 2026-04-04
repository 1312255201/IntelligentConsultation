SET NAMES utf8mb4;

USE `consultation`;

CREATE TABLE IF NOT EXISTS `db_consultation_message` (
  `id` int NOT NULL AUTO_INCREMENT,
  `consultation_id` int NOT NULL,
  `sender_type` varchar(20) NOT NULL,
  `sender_id` int NOT NULL,
  `sender_name` varchar(50) DEFAULT NULL,
  `sender_role_name` varchar(50) DEFAULT NULL,
  `message_type` varchar(20) NOT NULL DEFAULT 'text',
  `content` varchar(2000) DEFAULT NULL,
  `attachments_json` text,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_consultation_message_consultation_time` (`consultation_id`, `create_time`),
  KEY `idx_consultation_message_sender_time` (`sender_type`, `sender_id`, `create_time`),
  KEY `idx_consultation_message_status` (`status`, `create_time`),
  CONSTRAINT `fk_consultation_message_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notes:
-- 1. This script is for existing MySQL 5.7 databases.
-- 2. Execute it directly in a SQL client.
-- 3. If you see content like @@ -0,0 +1,xx @@ while importing,
--    you are running a patch text instead of this SQL file.
