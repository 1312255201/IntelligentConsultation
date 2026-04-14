SET @consultation_category_has_price_amount := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'db_consultation_category'
    AND COLUMN_NAME = 'price_amount'
);
SET @consultation_category_price_sql := IF(
  @consultation_category_has_price_amount = 0,
  'ALTER TABLE `db_consultation_category` ADD COLUMN `price_amount` decimal(10,2) NOT NULL DEFAULT 0.00 AFTER `description`',
  'SELECT 1'
);
PREPARE consultation_category_price_stmt FROM @consultation_category_price_sql;
EXECUTE consultation_category_price_stmt;
DEALLOCATE PREPARE consultation_category_price_stmt;

CREATE TABLE IF NOT EXISTS `db_consultation_payment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `consultation_id` int NOT NULL,
  `account_id` int NOT NULL,
  `patient_id` int NOT NULL,
  `patient_name` varchar(50) NOT NULL,
  `category_id` int NOT NULL,
  `category_name` varchar(50) NOT NULL,
  `amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `payment_no` varchar(40) NOT NULL,
  `payment_channel` varchar(20) DEFAULT NULL,
  `paid_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consultation_payment_consultation` (`consultation_id`),
  UNIQUE KEY `uk_consultation_payment_no` (`payment_no`),
  KEY `idx_consultation_payment_account_status` (`account_id`, `status`),
  KEY `idx_consultation_payment_paid_time` (`paid_time`),
  CONSTRAINT `fk_consultation_payment_consultation`
    FOREIGN KEY (`consultation_id`) REFERENCES `db_consultation_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_payment_account`
    FOREIGN KEY (`account_id`) REFERENCES `db_account` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_payment_patient`
    FOREIGN KEY (`patient_id`) REFERENCES `db_patient_profile` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consultation_payment_category`
    FOREIGN KEY (`category_id`) REFERENCES `db_consultation_category` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE `db_consultation_category`
SET `price_amount` = CASE `code`
  WHEN 'TEXT_CONSULT' THEN 29.90
  WHEN 'CHRONIC_FOLLOWUP' THEN 39.90
  WHEN 'REPORT_INTERPRET' THEN 49.90
  WHEN 'PEDI_FEVER' THEN 35.90
  WHEN 'WOMEN_HEALTH' THEN 45.90
  WHEN 'SKIN_ISSUE' THEN 32.90
  ELSE `price_amount`
END
WHERE `code` IN (
  'TEXT_CONSULT',
  'CHRONIC_FOLLOWUP',
  'REPORT_INTERPRET',
  'PEDI_FEVER',
  'WOMEN_HEALTH',
  'SKIN_ISSUE'
);
