SET @doctor_form_ai_log_has_regenerate_field := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'db_doctor_form_ai_log'
    AND COLUMN_NAME = 'regenerate_field'
);

SET @doctor_form_ai_log_sql := IF(
  @doctor_form_ai_log_has_regenerate_field = 0,
  'ALTER TABLE `db_doctor_form_ai_log` ADD COLUMN `regenerate_field` varchar(50) DEFAULT NULL AFTER `scene_type`',
  'SELECT 1'
);

PREPARE doctor_form_ai_log_stmt FROM @doctor_form_ai_log_sql;
EXECUTE doctor_form_ai_log_stmt;
DEALLOCATE PREPARE doctor_form_ai_log_stmt;
