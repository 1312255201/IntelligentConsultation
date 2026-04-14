SET NAMES utf8mb4;

USE `consultation`;

CREATE TABLE IF NOT EXISTS `db_medicine_catalog` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `generic_name` varchar(100) DEFAULT NULL,
  `category_name` varchar(50) DEFAULT NULL,
  `specification` varchar(100) DEFAULT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_medicine_catalog_name_specification` (`name`, `specification`),
  KEY `idx_medicine_catalog_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_medicine_warning` (
  `id` int NOT NULL AUTO_INCREMENT,
  `medicine_id` int NOT NULL,
  `warning_text` varchar(255) NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_medicine_warning_medicine_status` (`medicine_id`, `status`, `sort`),
  CONSTRAINT `fk_medicine_warning_medicine`
    FOREIGN KEY (`medicine_id`) REFERENCES `db_medicine_catalog` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_medicine_interaction` (
  `id` int NOT NULL AUTO_INCREMENT,
  `medicine_id` int NOT NULL,
  `conflict_medicine_id` int NOT NULL,
  `interaction_text` varchar(255) DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_medicine_interaction_pair` (`medicine_id`, `conflict_medicine_id`),
  KEY `idx_medicine_interaction_conflict` (`conflict_medicine_id`, `status`),
  CONSTRAINT `fk_medicine_interaction_medicine`
    FOREIGN KEY (`medicine_id`) REFERENCES `db_medicine_catalog` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_medicine_interaction_conflict_medicine`
    FOREIGN KEY (`conflict_medicine_id`) REFERENCES `db_medicine_catalog` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `db_consultation_prescription` (
  `id` int NOT NULL AUTO_INCREMENT,
  `consultation_id` int NOT NULL,
  `doctor_id` int NOT NULL,
  `medicine_id` int DEFAULT NULL,
  `medicine_name` varchar(100) NOT NULL,
  `generic_name` varchar(100) DEFAULT NULL,
  `category_name` varchar(50) DEFAULT NULL,
  `specification` varchar(100) DEFAULT NULL,
  `dosage` varchar(100) NOT NULL,
  `frequency` varchar(100) NOT NULL,
  `duration_days` int NOT NULL,
  `medication_instruction` varchar(255) DEFAULT NULL,
  `warning_summary` varchar(1000) DEFAULT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_consultation_prescription_consultation_sort` (`consultation_id`, `sort`, `id`),
  KEY `idx_consultation_prescription_doctor_time` (`doctor_id`, `update_time`),
  KEY `idx_consultation_prescription_medicine` (`medicine_id`),
  CONSTRAINT `fk_consultation_prescription_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_prescription_doctor`
    FOREIGN KEY (`doctor_id`) REFERENCES `db_doctor` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_prescription_medicine`
    FOREIGN KEY (`medicine_id`) REFERENCES `db_medicine_catalog` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
