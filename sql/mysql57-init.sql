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
  KEY `idx_doctor_department_status` (`department_id`, `status`),
  KEY `idx_doctor_sort` (`sort`),
  CONSTRAINT `fk_doctor_department`
    FOREIGN KEY (`department_id`) REFERENCES `db_department` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Note:
-- 1. Password values are stored as BCrypt hashes, not plain text.
-- 2. Accounts can be created through the existing register page or /api/auth/register.
-- 3. Avatar paths are stored as relative MinIO object paths such as /avatar/xxxxx.
-- 4. To grant an existing account administrator permissions:
--    UPDATE db_account SET role = 'admin' WHERE username = 'your_admin_username';
-- 5. Department rows referenced by doctors cannot be deleted until those doctors are removed or reassigned.
