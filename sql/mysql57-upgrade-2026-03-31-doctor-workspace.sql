SET NAMES utf8mb4;

USE `consultation`;

ALTER TABLE `db_doctor`
  ADD COLUMN `account_id` int DEFAULT NULL AFTER `department_id`;

ALTER TABLE `db_doctor`
  ADD UNIQUE KEY `uk_doctor_account` (`account_id`);

ALTER TABLE `db_doctor`
  ADD CONSTRAINT `fk_doctor_account`
    FOREIGN KEY (`account_id`) REFERENCES `db_account` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

-- Notes:
-- One-time upgrade script for existing MySQL 5.7 databases.
-- If account_id / uk_doctor_account / fk_doctor_account already exists,
-- the repeated execution will fail on the existing object and can be skipped.
-- If you still see @@ -0,0 +1,57 @@ while importing, you are executing a patch text,
-- not this clean SQL file. Re-open this file and run it again.
