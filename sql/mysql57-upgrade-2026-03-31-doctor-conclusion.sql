SET NAMES utf8mb4;

USE `consultation`;

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

-- Notes:
-- 1. This script is for existing MySQL 5.7 databases.
-- 2. Execute it directly in a SQL client.
-- 3. If you see content like @@ -0,0 +1,xx @@ while importing,
--    you are running a patch text instead of this SQL file.
