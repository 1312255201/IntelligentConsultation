SET NAMES utf8mb4;

USE `consultation`;

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

-- Notes:
-- 1. This script is for existing MySQL 5.7 databases.
-- 2. Execute it directly in a SQL client.
-- 3. If you see content like @@ -0,0 +1,xx @@ while importing,
--    you are running a patch text instead of this SQL file.
