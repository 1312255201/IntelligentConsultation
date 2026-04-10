SET @doctor_form_ai_log_has_draft_context_used := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'db_doctor_form_ai_log'
    AND COLUMN_NAME = 'draft_context_used'
);

SET @doctor_form_ai_log_draft_context_used_sql := IF(
  @doctor_form_ai_log_has_draft_context_used = 0,
  'ALTER TABLE `db_doctor_form_ai_log` ADD COLUMN `draft_context_used` tinyint(1) NOT NULL DEFAULT 0 AFTER `draft_summary`',
  'SELECT 1'
);

PREPARE doctor_form_ai_log_draft_context_used_stmt FROM @doctor_form_ai_log_draft_context_used_sql;
EXECUTE doctor_form_ai_log_draft_context_used_stmt;
DEALLOCATE PREPARE doctor_form_ai_log_draft_context_used_stmt;

SET @doctor_form_ai_log_has_rewrite_requirement := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'db_doctor_form_ai_log'
    AND COLUMN_NAME = 'rewrite_requirement'
);

SET @doctor_form_ai_log_rewrite_requirement_sql := IF(
  @doctor_form_ai_log_has_rewrite_requirement = 0,
  'ALTER TABLE `db_doctor_form_ai_log` ADD COLUMN `rewrite_requirement` varchar(500) DEFAULT NULL AFTER `draft_context_used`',
  'SELECT 1'
);

PREPARE doctor_form_ai_log_rewrite_requirement_stmt FROM @doctor_form_ai_log_rewrite_requirement_sql;
EXECUTE doctor_form_ai_log_rewrite_requirement_stmt;
DEALLOCATE PREPARE doctor_form_ai_log_rewrite_requirement_stmt;
