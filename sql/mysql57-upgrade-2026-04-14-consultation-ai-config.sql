SET NAMES utf8mb4;

USE `consultation`;

CREATE TABLE IF NOT EXISTS `db_consultation_ai_config` (
  `id` int NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `prompt_version` varchar(100) NOT NULL DEFAULT 'deepseek-triage-v1',
  `doctor_candidate_limit` int NOT NULL DEFAULT 3,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notes:
-- 1. This script is for existing MySQL 5.7 databases.
-- 2. Execute it directly in a SQL client.
-- 3. The default config row will be auto-created on first backend read.
