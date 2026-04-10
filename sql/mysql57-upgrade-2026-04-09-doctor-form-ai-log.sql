CREATE TABLE IF NOT EXISTS `db_doctor_form_ai_log` (
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
  `draft_preview` varchar(1000) DEFAULT NULL,
  `draft_payload_json` text,
  `apply_count` int NOT NULL DEFAULT 0,
  `last_apply_mode` varchar(20) DEFAULT NULL,
  `last_apply_target` varchar(40) DEFAULT NULL,
  `template_scene_type` varchar(50) DEFAULT NULL,
  `template_id` int DEFAULT NULL,
  `template_title` varchar(100) DEFAULT NULL,
  `template_used` tinyint(1) NOT NULL DEFAULT 0,
  `saved_status` tinyint(1) NOT NULL DEFAULT 0,
  `saved_target` varchar(40) DEFAULT NULL,
  `saved_preview` varchar(1000) DEFAULT NULL,
  `saved_payload_json` text,
  `generated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_apply_time` datetime DEFAULT NULL,
  `saved_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_doctor_form_ai_log_consultation_time` (`consultation_id`, `generated_time`),
  KEY `idx_doctor_form_ai_log_doctor_time` (`doctor_id`, `generated_time`),
  KEY `idx_doctor_form_ai_log_scene_time` (`scene_type`, `generated_time`),
  KEY `idx_doctor_form_ai_log_saved_status` (`saved_status`, `generated_time`),
  CONSTRAINT `fk_doctor_form_ai_log_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_doctor_form_ai_log_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @doctor_form_ai_log_has_last_apply_mode := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'db_doctor_form_ai_log'
    AND COLUMN_NAME = 'last_apply_mode'
);
SET @doctor_form_ai_log_sql := IF(
  @doctor_form_ai_log_has_last_apply_mode = 0,
  'ALTER TABLE `db_doctor_form_ai_log` ADD COLUMN `last_apply_mode` varchar(20) DEFAULT NULL AFTER `apply_count`',
  'SELECT 1'
);
PREPARE doctor_form_ai_log_stmt FROM @doctor_form_ai_log_sql;
EXECUTE doctor_form_ai_log_stmt;
DEALLOCATE PREPARE doctor_form_ai_log_stmt;

SET @doctor_form_ai_log_has_template_scene_type := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'db_doctor_form_ai_log'
    AND COLUMN_NAME = 'template_scene_type'
);
SET @doctor_form_ai_log_sql := IF(
  @doctor_form_ai_log_has_template_scene_type = 0,
  'ALTER TABLE `db_doctor_form_ai_log` ADD COLUMN `template_scene_type` varchar(50) DEFAULT NULL AFTER `last_apply_target`',
  'SELECT 1'
);
PREPARE doctor_form_ai_log_stmt FROM @doctor_form_ai_log_sql;
EXECUTE doctor_form_ai_log_stmt;
DEALLOCATE PREPARE doctor_form_ai_log_stmt;

SET @doctor_form_ai_log_has_template_id := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'db_doctor_form_ai_log'
    AND COLUMN_NAME = 'template_id'
);
SET @doctor_form_ai_log_sql := IF(
  @doctor_form_ai_log_has_template_id = 0,
  'ALTER TABLE `db_doctor_form_ai_log` ADD COLUMN `template_id` int DEFAULT NULL AFTER `template_scene_type`',
  'SELECT 1'
);
PREPARE doctor_form_ai_log_stmt FROM @doctor_form_ai_log_sql;
EXECUTE doctor_form_ai_log_stmt;
DEALLOCATE PREPARE doctor_form_ai_log_stmt;

SET @doctor_form_ai_log_has_template_title := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'db_doctor_form_ai_log'
    AND COLUMN_NAME = 'template_title'
);
SET @doctor_form_ai_log_sql := IF(
  @doctor_form_ai_log_has_template_title = 0,
  'ALTER TABLE `db_doctor_form_ai_log` ADD COLUMN `template_title` varchar(100) DEFAULT NULL AFTER `template_id`',
  'SELECT 1'
);
PREPARE doctor_form_ai_log_stmt FROM @doctor_form_ai_log_sql;
EXECUTE doctor_form_ai_log_stmt;
DEALLOCATE PREPARE doctor_form_ai_log_stmt;

SET @doctor_form_ai_log_has_template_used := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'db_doctor_form_ai_log'
    AND COLUMN_NAME = 'template_used'
);
SET @doctor_form_ai_log_sql := IF(
  @doctor_form_ai_log_has_template_used = 0,
  'ALTER TABLE `db_doctor_form_ai_log` ADD COLUMN `template_used` tinyint(1) NOT NULL DEFAULT 0 AFTER `template_title`',
  'SELECT 1'
);
PREPARE doctor_form_ai_log_stmt FROM @doctor_form_ai_log_sql;
EXECUTE doctor_form_ai_log_stmt;
DEALLOCATE PREPARE doctor_form_ai_log_stmt;
