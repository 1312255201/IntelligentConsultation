SET NAMES utf8mb4;

USE `consultation`;

CREATE TABLE IF NOT EXISTS `db_consultation_ai_config_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `config_id` int NOT NULL DEFAULT 1,
  `enabled_before` tinyint(1) DEFAULT NULL,
  `enabled_after` tinyint(1) NOT NULL DEFAULT 1,
  `prompt_version_before` varchar(100) DEFAULT NULL,
  `prompt_version_after` varchar(100) NOT NULL DEFAULT 'deepseek-triage-v1',
  `doctor_candidate_limit_before` int DEFAULT NULL,
  `doctor_candidate_limit_after` int NOT NULL DEFAULT 3,
  `operator_account_id` int DEFAULT NULL,
  `operator_username` varchar(100) DEFAULT NULL,
  `change_summary` varchar(1000) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_config_history_config_time` (`config_id`, `id`),
  KEY `idx_ai_config_history_operator` (`operator_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notes:
-- 1. This script is for existing MySQL 5.7 databases.
-- 2. Execute it directly in a SQL client.
-- 3. New rows are inserted automatically when admin saves AI config.
