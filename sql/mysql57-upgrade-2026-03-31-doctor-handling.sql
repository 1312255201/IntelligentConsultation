SET NAMES utf8mb4;

USE `consultation`;

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

-- Notes:
-- 1. This is a clean MySQL 5.7 upgrade script for existing databases.
-- 2. It can be executed directly in Navicat / DataGrip / mysql client.
-- 3. If you still see content like @@ -0,0 +1,57 @@ while importing,
--    you are executing a patch text instead of this SQL file.
