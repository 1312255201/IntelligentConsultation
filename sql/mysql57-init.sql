SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `consultation`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `consultation`;

CREATE TABLE IF NOT EXISTS `db_account` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(20) NOT NULL,
  `password` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'user',
  `avatar` varchar(191) DEFAULT NULL,
  `register_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_username` (`username`),
  UNIQUE KEY `uk_account_email` (`email`),
  KEY `idx_account_register_time` (`register_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_image_store` (
  `uid` int NOT NULL,
  `name` varchar(191) NOT NULL,
  `time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`name`),
  KEY `idx_image_store_uid_time` (`uid`, `time`),
  CONSTRAINT `fk_image_store_uid`
    FOREIGN KEY (`uid`) REFERENCES `db_account` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_department` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `code` varchar(30) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `location` varchar(100) DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_department_name` (`name`),
  UNIQUE KEY `uk_department_code` (`code`),
  KEY `idx_department_sort_status` (`sort`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_doctor` (
  `id` int NOT NULL AUTO_INCREMENT,
  `department_id` int NOT NULL,
  `account_id` int DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `title` varchar(50) DEFAULT NULL,
  `photo` varchar(191) DEFAULT NULL,
  `introduction` text,
  `expertise` varchar(500) DEFAULT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_account` (`account_id`),
  KEY `idx_doctor_department_status` (`department_id`, `status`),
  KEY `idx_doctor_sort` (`sort`),
  CONSTRAINT `fk_doctor_account`
    FOREIGN KEY (`account_id`) REFERENCES `db_account` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `fk_doctor_department`
    FOREIGN KEY (`department_id`) REFERENCES `db_department` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_doctor_service_tag` (
  `id` int NOT NULL AUTO_INCREMENT,
  `doctor_id` int NOT NULL,
  `tag_code` varchar(50) NOT NULL,
  `tag_name` varchar(100) NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_service_tag` (`doctor_id`, `tag_code`),
  KEY `idx_doctor_service_tag_status_sort` (`status`, `sort`),
  CONSTRAINT `fk_doctor_service_tag_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_doctor_schedule` (
  `id` int NOT NULL AUTO_INCREMENT,
  `doctor_id` int NOT NULL,
  `schedule_date` date NOT NULL,
  `time_period` varchar(20) NOT NULL,
  `visit_type` varchar(20) NOT NULL DEFAULT 'both',
  `max_capacity` int NOT NULL DEFAULT 0,
  `used_capacity` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_schedule_slot` (`doctor_id`, `schedule_date`, `time_period`, `visit_type`),
  KEY `idx_doctor_schedule_date_status` (`schedule_date`, `status`),
  KEY `idx_doctor_schedule_doctor_date` (`doctor_id`, `schedule_date`),
  CONSTRAINT `fk_doctor_schedule_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_doctor_reply_template` (
  `id` int NOT NULL AUTO_INCREMENT,
  `doctor_id` int NOT NULL,
  `scene_type` varchar(30) NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` varchar(2000) NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_reply_template_scene_title` (`doctor_id`, `scene_type`, `title`),
  KEY `idx_doctor_reply_template_scene_status` (`doctor_id`, `scene_type`, `status`),
  KEY `idx_doctor_reply_template_sort` (`doctor_id`, `sort`),
  CONSTRAINT `fk_doctor_reply_template_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_patient_profile` (
  `id` int NOT NULL AUTO_INCREMENT,
  `account_id` int NOT NULL,
  `name` varchar(50) NOT NULL,
  `gender` varchar(10) NOT NULL DEFAULT 'unknown',
  `birth_date` date DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `id_card` varchar(30) DEFAULT NULL,
  `relation_type` varchar(20) NOT NULL DEFAULT 'self',
  `is_self` tinyint(1) NOT NULL DEFAULT 0,
  `is_default` tinyint(1) NOT NULL DEFAULT 0,
  `remark` varchar(255) DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_patient_profile_account_status` (`account_id`, `status`),
  KEY `idx_patient_profile_account_default` (`account_id`, `is_default`),
  CONSTRAINT `fk_patient_profile_account`
    FOREIGN KEY (`account_id`) REFERENCES `db_account` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_patient_medical_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `patient_id` int NOT NULL,
  `allergy_history` text,
  `past_history` text,
  `chronic_history` text,
  `surgery_history` text,
  `family_history` text,
  `medication_history` text,
  `pregnancy_status` varchar(20) NOT NULL DEFAULT 'unknown',
  `lactation_status` varchar(20) NOT NULL DEFAULT 'unknown',
  `infectious_history` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_patient_medical_history_patient` (`patient_id`),
  CONSTRAINT `fk_patient_medical_history_patient`
    FOREIGN KEY (`patient_id`) REFERENCES `db_patient_profile` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_consultation_category` (
  `id` int NOT NULL AUTO_INCREMENT,
  `department_id` int NOT NULL,
  `name` varchar(50) NOT NULL,
  `code` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consultation_category_name` (`name`),
  UNIQUE KEY `uk_consultation_category_code` (`code`),
  KEY `idx_consultation_category_department_status` (`department_id`, `status`),
  KEY `idx_consultation_category_sort` (`sort`),
  CONSTRAINT `fk_consultation_category_department`
    FOREIGN KEY (`department_id`) REFERENCES `db_department` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_consultation_intake_template` (
  `id` int NOT NULL AUTO_INCREMENT,
  `category_id` int NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `version` int NOT NULL DEFAULT 1,
  `is_default` tinyint(1) NOT NULL DEFAULT 1,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consultation_template_name_version` (`category_id`, `name`, `version`),
  KEY `idx_consultation_template_category_default` (`category_id`, `is_default`, `status`),
  CONSTRAINT `fk_consultation_template_category`
    FOREIGN KEY (`category_id`) REFERENCES `db_consultation_category` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_consultation_intake_field` (
  `id` int NOT NULL AUTO_INCREMENT,
  `template_id` int NOT NULL,
  `field_code` varchar(50) NOT NULL,
  `field_label` varchar(100) NOT NULL,
  `field_type` varchar(30) NOT NULL,
  `is_required` tinyint(1) NOT NULL DEFAULT 0,
  `options_json` text,
  `default_value` varchar(255) DEFAULT NULL,
  `placeholder` varchar(255) DEFAULT NULL,
  `help_text` varchar(255) DEFAULT NULL,
  `condition_rule` varchar(255) DEFAULT NULL,
  `validation_rule` varchar(255) DEFAULT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consultation_field_code` (`template_id`, `field_code`),
  KEY `idx_consultation_field_template_sort` (`template_id`, `sort`, `status`),
  CONSTRAINT `fk_consultation_field_template`
    FOREIGN KEY (`template_id`) REFERENCES `db_consultation_intake_template` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_consultation_record` (
  `id` int NOT NULL AUTO_INCREMENT,
  `consultation_no` varchar(32) NOT NULL,
  `account_id` int NOT NULL,
  `patient_id` int NOT NULL,
  `patient_name` varchar(50) NOT NULL,
  `category_id` int NOT NULL,
  `category_name` varchar(50) NOT NULL,
  `department_id` int DEFAULT NULL,
  `department_name` varchar(50) DEFAULT NULL,
  `template_id` int NOT NULL,
  `template_name` varchar(100) NOT NULL,
  `title` varchar(100) NOT NULL,
  `chief_complaint` varchar(500) DEFAULT NULL,
  `health_summary` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'submitted',
  `answer_count` int NOT NULL DEFAULT 0,
  `triage_level_id` int DEFAULT NULL,
  `triage_level_code` varchar(50) DEFAULT NULL,
  `triage_level_name` varchar(50) DEFAULT NULL,
  `triage_level_color` varchar(20) DEFAULT NULL,
  `triage_action_type` varchar(30) DEFAULT NULL,
  `triage_suggestion` varchar(255) DEFAULT NULL,
  `triage_rule_summary` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consultation_record_no` (`consultation_no`),
  KEY `idx_consultation_record_account_status` (`account_id`, `status`),
  KEY `idx_consultation_record_patient` (`patient_id`),
  KEY `idx_consultation_record_category` (`category_id`),
  KEY `idx_consultation_record_triage_level` (`triage_level_id`),
  KEY `idx_consultation_record_create_time` (`create_time`),
  CONSTRAINT `fk_consultation_record_account`
    FOREIGN KEY (`account_id`) REFERENCES `db_account` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_record_patient`
    FOREIGN KEY (`patient_id`) REFERENCES `db_patient_profile` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_record_category`
    FOREIGN KEY (`category_id`) REFERENCES `db_consultation_category` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_record_department`
    FOREIGN KEY (`department_id`) REFERENCES `db_department` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_record_template`
    FOREIGN KEY (`template_id`) REFERENCES `db_consultation_intake_template` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_consultation_record_answer` (
  `id` int NOT NULL AUTO_INCREMENT,
  `consultation_id` int NOT NULL,
  `field_code` varchar(50) NOT NULL,
  `field_label` varchar(100) NOT NULL,
  `field_type` varchar(30) NOT NULL,
  `field_value` text,
  `sort` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_consultation_record_answer_consultation_sort` (`consultation_id`, `sort`),
  CONSTRAINT `fk_consultation_record_answer_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_consultation_doctor_assignment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `consultation_id` int NOT NULL,
  `doctor_id` int NOT NULL,
  `doctor_name` varchar(50) NOT NULL,
  `department_id` int NOT NULL,
  `department_name` varchar(50) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'claimed',
  `claim_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `release_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consultation_doctor_assignment_consultation` (`consultation_id`),
  KEY `idx_consultation_doctor_assignment_doctor_status` (`doctor_id`, `status`),
  KEY `idx_consultation_doctor_assignment_department_status` (`department_id`, `status`),
  KEY `idx_consultation_doctor_assignment_claim_time` (`claim_time`),
  CONSTRAINT `fk_consultation_doctor_assignment_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_doctor_assignment_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_doctor_assignment_department`
    FOREIGN KEY (`department_id`) REFERENCES `db_department` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_consultation_doctor_handle` (
  `id` int NOT NULL AUTO_INCREMENT,
  `consultation_id` int NOT NULL,
  `doctor_id` int NOT NULL,
  `doctor_name` varchar(50) NOT NULL,
  `department_id` int NOT NULL,
  `department_name` varchar(50) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'processing',
  `summary` varchar(500) DEFAULT NULL,
  `medical_advice` text,
  `follow_up_plan` varchar(500) DEFAULT NULL,
  `internal_remark` varchar(500) DEFAULT NULL,
  `receive_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `complete_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consultation_doctor_handle_consultation` (`consultation_id`),
  KEY `idx_consultation_doctor_handle_doctor_status` (`doctor_id`, `status`),
  KEY `idx_consultation_doctor_handle_department_status` (`department_id`, `status`),
  CONSTRAINT `fk_consultation_doctor_handle_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_doctor_handle_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_doctor_handle_department`
    FOREIGN KEY (`department_id`) REFERENCES `db_department` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_consultation_doctor_conclusion` (
  `id` int NOT NULL AUTO_INCREMENT,
  `consultation_id` int NOT NULL,
  `doctor_id` int NOT NULL,
  `doctor_name` varchar(50) NOT NULL,
  `department_id` int NOT NULL,
  `department_name` varchar(50) DEFAULT NULL,
  `condition_level` varchar(20) DEFAULT NULL,
  `disposition` varchar(30) DEFAULT NULL,
  `diagnosis_direction` varchar(100) DEFAULT NULL,
  `conclusion_tags_json` text,
  `need_follow_up` tinyint(1) NOT NULL DEFAULT 0,
  `follow_up_within_days` int DEFAULT NULL,
  `is_consistent_with_ai` tinyint(1) DEFAULT NULL,
  `ai_mismatch_reasons_json` varchar(500) DEFAULT NULL,
  `ai_mismatch_remark` varchar(500) DEFAULT NULL,
  `patient_instruction` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consultation_doctor_conclusion_consultation` (`consultation_id`),
  KEY `idx_consultation_doctor_conclusion_doctor` (`doctor_id`),
  KEY `idx_consultation_doctor_conclusion_department` (`department_id`),
  KEY `idx_consultation_doctor_conclusion_condition_disposition` (`condition_level`, `disposition`),
  CONSTRAINT `fk_consultation_doctor_conclusion_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_doctor_conclusion_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_doctor_conclusion_department`
    FOREIGN KEY (`department_id`) REFERENCES `db_department` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_consultation_doctor_follow_up` (
  `id` int NOT NULL AUTO_INCREMENT,
  `consultation_id` int NOT NULL,
  `doctor_id` int NOT NULL,
  `doctor_name` varchar(50) NOT NULL,
  `department_id` int NOT NULL,
  `department_name` varchar(50) DEFAULT NULL,
  `follow_up_type` varchar(20) NOT NULL DEFAULT 'platform',
  `patient_status` varchar(20) NOT NULL DEFAULT 'stable',
  `summary` varchar(500) NOT NULL,
  `advice` varchar(1000) DEFAULT NULL,
  `next_step` varchar(500) DEFAULT NULL,
  `need_revisit` tinyint(1) NOT NULL DEFAULT 0,
  `next_follow_up_date` date DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_consultation_doctor_follow_up_consultation_time` (`consultation_id`, `create_time`),
  KEY `idx_consultation_doctor_follow_up_doctor_time` (`doctor_id`, `create_time`),
  KEY `idx_consultation_doctor_follow_up_department_time` (`department_id`, `create_time`),
  KEY `idx_consultation_doctor_follow_up_next_date` (`next_follow_up_date`),
  CONSTRAINT `fk_consultation_doctor_follow_up_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_doctor_follow_up_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_doctor_follow_up_department`
    FOREIGN KEY (`department_id`) REFERENCES `db_department` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
  `read_status` tinyint(1) NOT NULL DEFAULT 0,
  `read_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_consultation_message_consultation_time` (`consultation_id`, `create_time`),
  KEY `idx_consultation_message_read_status` (`consultation_id`, `read_status`, `create_time`),
  KEY `idx_consultation_message_sender_time` (`sender_type`, `sender_id`, `create_time`),
  KEY `idx_consultation_message_status` (`status`, `create_time`),
  CONSTRAINT `fk_consultation_message_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_body_part_dict` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `code` varchar(50) NOT NULL,
  `parent_id` int DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_body_part_name` (`name`),
  UNIQUE KEY `uk_body_part_code` (`code`),
  KEY `idx_body_part_parent_status` (`parent_id`, `status`),
  KEY `idx_body_part_sort` (`sort`),
  CONSTRAINT `fk_body_part_parent`
    FOREIGN KEY (`parent_id`) REFERENCES `db_body_part_dict` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_symptom_dict` (
  `id` int NOT NULL AUTO_INCREMENT,
  `body_part_id` int NOT NULL,
  `name` varchar(50) NOT NULL,
  `code` varchar(50) NOT NULL,
  `keywords` varchar(255) DEFAULT NULL,
  `alias_keywords` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_symptom_code` (`code`),
  UNIQUE KEY `uk_symptom_body_part_name` (`body_part_id`, `name`),
  KEY `idx_symptom_body_part_status` (`body_part_id`, `status`),
  KEY `idx_symptom_sort` (`sort`),
  CONSTRAINT `fk_symptom_body_part`
    FOREIGN KEY (`body_part_id`) REFERENCES `db_body_part_dict` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_triage_level_dict` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `code` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `suggestion` varchar(255) DEFAULT NULL,
  `color` varchar(20) DEFAULT NULL,
  `priority` int NOT NULL DEFAULT 0,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_triage_level_name` (`name`),
  UNIQUE KEY `uk_triage_level_code` (`code`),
  KEY `idx_triage_level_priority_status` (`priority`, `status`),
  KEY `idx_triage_level_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_red_flag_rule` (
  `id` int NOT NULL AUTO_INCREMENT,
  `rule_name` varchar(100) NOT NULL,
  `rule_code` varchar(50) NOT NULL,
  `trigger_type` varchar(30) NOT NULL,
  `body_part_id` int DEFAULT NULL,
  `keyword_pattern` varchar(255) DEFAULT NULL,
  `condition_description` varchar(255) DEFAULT NULL,
  `triage_level_id` int NOT NULL,
  `suggestion` varchar(255) NOT NULL,
  `action_type` varchar(30) NOT NULL DEFAULT 'offline',
  `priority` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_red_flag_rule_code` (`rule_code`),
  KEY `idx_red_flag_rule_priority_status` (`priority`, `status`),
  KEY `idx_red_flag_rule_triage_level` (`triage_level_id`),
  KEY `idx_red_flag_rule_body_part` (`body_part_id`),
  CONSTRAINT `fk_red_flag_rule_triage_level`
    FOREIGN KEY (`triage_level_id`) REFERENCES `db_triage_level_dict` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_red_flag_rule_body_part`
    FOREIGN KEY (`body_part_id`) REFERENCES `db_body_part_dict` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_red_flag_rule_symptom` (
  `rule_id` int NOT NULL,
  `symptom_id` int NOT NULL,
  PRIMARY KEY (`rule_id`, `symptom_id`),
  KEY `idx_red_flag_rule_symptom_symptom` (`symptom_id`),
  CONSTRAINT `fk_red_flag_rule_symptom_rule`
    FOREIGN KEY (`rule_id`) REFERENCES `db_red_flag_rule` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_red_flag_rule_symptom_symptom`
    FOREIGN KEY (`symptom_id`) REFERENCES `db_symptom_dict` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_triage_rule_hit_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `consultation_id` int NOT NULL,
  `rule_id` int DEFAULT NULL,
  `rule_name` varchar(100) NOT NULL,
  `rule_code` varchar(50) DEFAULT NULL,
  `trigger_type` varchar(30) DEFAULT NULL,
  `triage_level_id` int DEFAULT NULL,
  `triage_level_code` varchar(50) DEFAULT NULL,
  `triage_level_name` varchar(50) DEFAULT NULL,
  `action_type` varchar(30) DEFAULT NULL,
  `suggestion` varchar(255) DEFAULT NULL,
  `matched_summary` varchar(255) DEFAULT NULL,
  `priority` int NOT NULL DEFAULT 0,
  `is_primary` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_triage_hit_consultation_primary` (`consultation_id`, `is_primary`),
  KEY `idx_triage_hit_rule` (`rule_id`),
  KEY `idx_triage_hit_triage_level` (`triage_level_id`),
  CONSTRAINT `fk_triage_hit_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_hit_rule`
    FOREIGN KEY (`rule_id`) REFERENCES `db_red_flag_rule` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_hit_triage_level`
    FOREIGN KEY (`triage_level_id`) REFERENCES `db_triage_level_dict` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_triage_knowledge` (
  `id` int NOT NULL AUTO_INCREMENT,
  `knowledge_type` varchar(30) NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` text NOT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `department_id` int DEFAULT NULL,
  `doctor_id` int DEFAULT NULL,
  `source_type` varchar(30) NOT NULL DEFAULT 'manual',
  `version` int NOT NULL DEFAULT 1,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_triage_knowledge_type_title` (`knowledge_type`, `title`),
  KEY `idx_triage_knowledge_department_status` (`department_id`, `status`),
  KEY `idx_triage_knowledge_doctor_status` (`doctor_id`, `status`),
  KEY `idx_triage_knowledge_type_status` (`knowledge_type`, `status`),
  KEY `idx_triage_knowledge_sort` (`sort`, `id`),
  CONSTRAINT `fk_triage_knowledge_department`
    FOREIGN KEY (`department_id`) REFERENCES `db_department` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_knowledge_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_triage_case_reference` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `chief_complaint` varchar(500) NOT NULL,
  `symptom_summary` text,
  `triage_result` varchar(500) NOT NULL,
  `department_id` int NOT NULL,
  `doctor_id` int DEFAULT NULL,
  `risk_level` varchar(30) NOT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `source_type` varchar(30) NOT NULL DEFAULT 'manual',
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_triage_case_title` (`title`),
  KEY `idx_triage_case_department_status` (`department_id`, `status`),
  KEY `idx_triage_case_doctor_status` (`doctor_id`, `status`),
  KEY `idx_triage_case_risk_level_status` (`risk_level`, `status`),
  KEY `idx_triage_case_sort` (`sort`, `id`),
  CONSTRAINT `fk_triage_case_department`
    FOREIGN KEY (`department_id`) REFERENCES `db_department` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_case_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_triage_session` (
  `id` int NOT NULL AUTO_INCREMENT,
  `session_no` varchar(32) NOT NULL,
  `consultation_id` int NOT NULL,
  `account_id` int NOT NULL,
  `patient_id` int NOT NULL,
  `patient_name` varchar(50) NOT NULL,
  `category_id` int NOT NULL,
  `category_name` varchar(50) NOT NULL,
  `department_id` int DEFAULT NULL,
  `department_name` varchar(50) DEFAULT NULL,
  `source_type` varchar(30) NOT NULL DEFAULT 'consultation_submit',
  `status` varchar(30) NOT NULL DEFAULT 'completed',
  `triage_level_id` int DEFAULT NULL,
  `triage_level_code` varchar(50) DEFAULT NULL,
  `triage_level_name` varchar(50) DEFAULT NULL,
  `triage_action_type` varchar(30) DEFAULT NULL,
  `triage_summary` varchar(500) DEFAULT NULL,
  `message_count` int NOT NULL DEFAULT 0,
  `started_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ended_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_triage_session_no` (`session_no`),
  UNIQUE KEY `uk_triage_session_consultation` (`consultation_id`),
  KEY `idx_triage_session_account_create` (`account_id`, `create_time`),
  KEY `idx_triage_session_patient_create` (`patient_id`, `create_time`),
  KEY `idx_triage_session_triage_status` (`triage_level_id`, `status`),
  CONSTRAINT `fk_triage_session_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_session_account`
    FOREIGN KEY (`account_id`) REFERENCES `db_account` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_session_patient`
    FOREIGN KEY (`patient_id`) REFERENCES `db_patient_profile` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_session_category`
    FOREIGN KEY (`category_id`) REFERENCES `db_consultation_category` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_session_department`
    FOREIGN KEY (`department_id`) REFERENCES `db_department` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_session_triage_level`
    FOREIGN KEY (`triage_level_id`) REFERENCES `db_triage_level_dict` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_triage_message` (
  `id` int NOT NULL AUTO_INCREMENT,
  `session_id` int NOT NULL,
  `role_type` varchar(30) NOT NULL,
  `message_type` varchar(30) NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` text NOT NULL,
  `structured_content` text,
  `sort` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_triage_message_session_sort` (`session_id`, `sort`),
  KEY `idx_triage_message_role_type` (`role_type`, `message_type`),
  CONSTRAINT `fk_triage_message_session`
    FOREIGN KEY (`session_id`) REFERENCES `db_triage_session` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_triage_result` (
  `id` int NOT NULL AUTO_INCREMENT,
  `session_id` int NOT NULL,
  `consultation_id` int NOT NULL,
  `result_type` varchar(30) NOT NULL DEFAULT 'initial',
  `triage_level_id` int DEFAULT NULL,
  `triage_level_code` varchar(50) DEFAULT NULL,
  `triage_level_name` varchar(50) DEFAULT NULL,
  `department_id` int DEFAULT NULL,
  `department_name` varchar(50) DEFAULT NULL,
  `doctor_id` int DEFAULT NULL,
  `doctor_name` varchar(50) DEFAULT NULL,
  `doctor_candidates_json` text,
  `reason_text` varchar(500) DEFAULT NULL,
  `risk_flags_json` text,
  `symptom_extract_json` text,
  `confidence_score` decimal(5,2) DEFAULT NULL,
  `is_final` tinyint(1) NOT NULL DEFAULT 1,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_triage_result_session_final` (`session_id`, `is_final`),
  KEY `idx_triage_result_consultation` (`consultation_id`),
  KEY `idx_triage_result_department_status` (`department_id`, `status`),
  KEY `idx_triage_result_doctor_status` (`doctor_id`, `status`),
  CONSTRAINT `fk_triage_result_session`
    FOREIGN KEY (`session_id`) REFERENCES `db_triage_session` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_result_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_result_triage_level`
    FOREIGN KEY (`triage_level_id`) REFERENCES `db_triage_level_dict` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_result_department`
    FOREIGN KEY (`department_id`) REFERENCES `db_department` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_result_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_triage_feedback` (
  `id` int NOT NULL AUTO_INCREMENT,
  `session_id` int NOT NULL,
  `consultation_id` int NOT NULL,
  `account_id` int NOT NULL,
  `patient_id` int NOT NULL,
  `user_score` tinyint NOT NULL,
  `is_adopted` tinyint(1) NOT NULL DEFAULT 1,
  `feedback_text` varchar(500) DEFAULT NULL,
  `manual_correct_department_id` int DEFAULT NULL,
  `manual_correct_department_name` varchar(50) DEFAULT NULL,
  `manual_correct_doctor_id` int DEFAULT NULL,
  `manual_correct_doctor_name` varchar(50) DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_triage_feedback_consultation` (`consultation_id`),
  KEY `idx_triage_feedback_session_status` (`session_id`, `status`),
  KEY `idx_triage_feedback_account_status` (`account_id`, `status`),
  KEY `idx_triage_feedback_patient_status` (`patient_id`, `status`),
  KEY `idx_triage_feedback_department` (`manual_correct_department_id`),
  KEY `idx_triage_feedback_doctor` (`manual_correct_doctor_id`),
  CONSTRAINT `fk_triage_feedback_session`
    FOREIGN KEY (`session_id`) REFERENCES `db_triage_session` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_feedback_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_feedback_account`
    FOREIGN KEY (`account_id`) REFERENCES `db_account` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_feedback_patient`
    FOREIGN KEY (`patient_id`) REFERENCES `db_patient_profile` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_feedback_department`
    FOREIGN KEY (`manual_correct_department_id`) REFERENCES `db_department` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `fk_triage_feedback_doctor`
    FOREIGN KEY (`manual_correct_doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_homepage_config` (
  `id` int NOT NULL,
  `hero_title` varchar(100) NOT NULL,
  `hero_subtitle` varchar(255) DEFAULT NULL,
  `notice_text` varchar(255) DEFAULT NULL,
  `intro_title` varchar(100) DEFAULT NULL,
  `intro_content` text,
  `service_phone` varchar(30) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_homepage_recommend_doctor` (
  `id` int NOT NULL AUTO_INCREMENT,
  `doctor_id` int NOT NULL,
  `display_title` varchar(100) DEFAULT NULL,
  `recommend_reason` varchar(255) DEFAULT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_homepage_recommend_doctor` (`doctor_id`),
  KEY `idx_homepage_recommend_sort_status` (`sort`, `status`),
  CONSTRAINT `fk_homepage_recommend_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_homepage_case` (
  `id` int NOT NULL AUTO_INCREMENT,
  `department_id` int NOT NULL,
  `doctor_id` int DEFAULT NULL,
  `title` varchar(100) NOT NULL,
  `cover` varchar(191) NOT NULL,
  `summary` varchar(500) NOT NULL,
  `detail` text,
  `tags` varchar(255) DEFAULT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_homepage_case_department_status` (`department_id`, `status`),
  KEY `idx_homepage_case_doctor` (`doctor_id`),
  KEY `idx_homepage_case_sort` (`sort`),
  CONSTRAINT `fk_homepage_case_department`
    FOREIGN KEY (`department_id`) REFERENCES `db_department` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_homepage_case_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Note:
-- 1. Password values are stored as BCrypt hashes, not plain text.
-- 2. Accounts can be created through the existing register page or /api/auth/register.
-- 3. Avatar paths are stored as relative MinIO object paths such as /avatar/xxxxx.
-- 4. To grant an existing account administrator permissions:
--    UPDATE db_account SET role = 'admin' WHERE username = 'your_admin_username';
-- 5. One account can maintain multiple patient profiles for self or family members.
-- 6. Each patient profile can maintain one medical history archive for AI triage and consultation intake.
-- 7. Doctor service tags and doctor schedules are used for AI triage recommendation and will cascade on doctor deletion.
-- 8. Department rows referenced by doctors cannot be deleted until those doctors are removed or reassigned.
-- 9. Homepage case and recommended doctor data reference department/doctor base data and should be cleared first before deleting those records.
-- 10. Consultation categories provide the entry configuration for AI triage scenes and map to base departments.
-- 11. Intake templates store pre-consultation field configuration, and each category should keep at least one default template for later user-side intake.
-- 12. Body part, symptom, triage level and red-flag rule dictionaries are the core structured data for rule-based triage and AI-assisted recommendation.
-- 13. Triage knowledge stores reusable structured triage guidance, service boundaries and case summaries for later AI retrieval and recommendation explanation.
-- 14. Triage case references store structured internal examples for later AI retrieval, recommendation explanation and operational review.
-- 15. Doctor-side login requires an account with role = 'doctor' and a doctor row bound through db_doctor.account_id.
