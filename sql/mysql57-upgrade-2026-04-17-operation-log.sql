SET NAMES utf8mb4;

USE `consultation`;

CREATE TABLE IF NOT EXISTS `db_operation_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `request_id` bigint NOT NULL,
  `request_url` varchar(255) NOT NULL,
  `request_method` varchar(20) NOT NULL,
  `remote_ip` varchar(64) DEFAULT NULL,
  `account_id` int DEFAULT NULL,
  `username` varchar(100) DEFAULT NULL,
  `role` varchar(50) DEFAULT NULL,
  `request_params` text,
  `response_code` int DEFAULT NULL,
  `response_summary` text,
  `duration_ms` int DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_request_time` (`request_url`, `create_time`),
  KEY `idx_operation_log_account_time` (`account_id`, `create_time`),
  KEY `idx_operation_log_request_id` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
