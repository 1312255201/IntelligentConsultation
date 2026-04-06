SET NAMES utf8mb4;

USE `consultation`;

CREATE TABLE IF NOT EXISTS `db_consultation_dispatch_config` (
  `id` int NOT NULL,
  `visit_type_weight` int NOT NULL DEFAULT 100,
  `schedule_weight` int NOT NULL DEFAULT 100,
  `capacity_weight` int NOT NULL DEFAULT 100,
  `workload_weight` int NOT NULL DEFAULT 100,
  `tag_match_weight` int NOT NULL DEFAULT 100,
  `tag_match_score_per_hit` int NOT NULL DEFAULT 4,
  `max_matched_tags` int NOT NULL DEFAULT 3,
  `recommend_doctor_limit` int NOT NULL DEFAULT 3,
  `min_recommendation_score` int NOT NULL DEFAULT 24,
  `max_recommendation_score` int NOT NULL DEFAULT 99,
  `waiting_overdue_hours` int NOT NULL DEFAULT 24,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notes:
-- 1. This script is for existing MySQL 5.7 databases.
-- 2. Execute it directly in a SQL client.
-- 3. The default config row will be auto-created on first backend read.
