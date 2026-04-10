SET NAMES utf8mb4;

USE `consultation`;

CREATE TABLE IF NOT EXISTS `db_doctor_message_ai_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `consultation_id` int NOT NULL,
  `consultation_no` varchar(50) DEFAULT NULL,
  `patient_name` varchar(50) DEFAULT NULL,
  `category_name` varchar(100) DEFAULT NULL,
  `doctor_id` int NOT NULL,
  `doctor_name` varchar(50) DEFAULT NULL,
  `department_id` int DEFAULT NULL,
  `department_name` varchar(100) DEFAULT NULL,
  `scene_type` varchar(40) NOT NULL,
  `source` varchar(20) DEFAULT NULL,
  `prompt_version` varchar(100) DEFAULT NULL,
  `fallback` tinyint(1) NOT NULL DEFAULT 0,
  `risk_flags_json` varchar(500) DEFAULT NULL,
  `draft_summary` varchar(255) DEFAULT NULL,
  `draft_content` varchar(1000) DEFAULT NULL,
  `apply_count` int NOT NULL DEFAULT 0,
  `last_apply_mode` varchar(20) DEFAULT NULL,
  `template_scene_type` varchar(50) DEFAULT NULL,
  `template_id` int DEFAULT NULL,
  `template_title` varchar(100) DEFAULT NULL,
  `template_used` tinyint(1) NOT NULL DEFAULT 0,
  `sent_status` tinyint(1) NOT NULL DEFAULT 0,
  `sent_message_id` int DEFAULT NULL,
  `sent_content_preview` varchar(500) DEFAULT NULL,
  `generated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_apply_time` datetime DEFAULT NULL,
  `sent_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_doctor_message_ai_log_consultation_time` (`consultation_id`, `generated_time`),
  KEY `idx_doctor_message_ai_log_doctor_time` (`doctor_id`, `generated_time`),
  KEY `idx_doctor_message_ai_log_scene_time` (`scene_type`, `generated_time`),
  KEY `idx_doctor_message_ai_log_sent_status` (`sent_status`, `generated_time`),
  CONSTRAINT `fk_doctor_message_ai_log_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_doctor_message_ai_log_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notes:
-- 1. This script is for existing MySQL 5.7 databases.
-- 2. Execute it directly in a SQL client.
-- 3. If you see content like @@ -0,0 +1,xx @@ while importing,
--    you are running a patch text instead of this SQL file.
