SET NAMES utf8mb4;

USE `consultation`;

-- =========================================================
-- 智能问诊系统演示数据
-- 用途：
-- 1. 帮助理解管理员新页面应该填写什么
-- 2. 让后台页面和首页能看到更真实的示例内容
-- 3. 可重复执行，不会重复插入相同演示记录
-- =========================================================

-- -------------------------
-- 1. 科室基础数据
-- -------------------------
INSERT INTO `db_department`
(`name`, `code`, `description`, `location`, `phone`, `sort`, `status`, `create_time`, `update_time`)
SELECT '全科门诊', 'GENERAL_MEDICINE', '适合作为首次线上问诊入口，承接常见轻症、初步分诊与就医建议。', '门诊楼 2 层 A 区', '0755-82001001', 10, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_department` WHERE `code` = 'GENERAL_MEDICINE');

INSERT INTO `db_department`
(`name`, `code`, `description`, `location`, `phone`, `sort`, `status`, `create_time`, `update_time`)
SELECT '内科', 'INTERNAL_MEDICINE', '适用于慢病复诊、用药评估、报告解读和常见内科症状咨询。', '门诊楼 3 层 B 区', '0755-82001002', 20, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_department` WHERE `code` = 'INTERNAL_MEDICINE');

INSERT INTO `db_department`
(`name`, `code`, `description`, `location`, `phone`, `sort`, `status`, `create_time`, `update_time`)
SELECT '儿科', 'PEDIATRICS', '适用于儿童发热、咳嗽、腹泻、喂养与生长发育咨询。', '门诊楼 1 层 C 区', '0755-82001003', 30, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_department` WHERE `code` = 'PEDIATRICS');

INSERT INTO `db_department`
(`name`, `code`, `description`, `location`, `phone`, `sort`, `status`, `create_time`, `update_time`)
SELECT '妇科', 'GYNECOLOGY', '适用于月经异常、备孕咨询、白带异常及女性常见健康问题。', '门诊楼 4 层 A 区', '0755-82001004', 40, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_department` WHERE `code` = 'GYNECOLOGY');

INSERT INTO `db_department`
(`name`, `code`, `description`, `location`, `phone`, `sort`, `status`, `create_time`, `update_time`)
SELECT '皮肤科', 'DERMATOLOGY', '适用于皮疹、湿疹、痤疮、过敏反应等皮肤问题咨询。', '门诊楼 2 层 D 区', '0755-82001005', 50, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_department` WHERE `code` = 'DERMATOLOGY');

-- -------------------------
-- 2. 医生基础数据
-- -------------------------
INSERT INTO `db_doctor`
(`department_id`, `name`, `title`, `photo`, `introduction`, `expertise`, `sort`, `status`, `create_time`, `update_time`)
SELECT d.id, '林远航', '主治医师', NULL,
       '长期参与线上首诊分流与常见病咨询，擅长把复杂主诉整理成清晰的就医路径。',
       '常见病初诊、轻症分诊建议、检查结果初步说明',
       10, 1, NOW(), NOW()
FROM `db_department` d
WHERE d.`code` = 'GENERAL_MEDICINE'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor` x
    WHERE x.`department_id` = d.id AND x.`name` = '林远航'
  );

INSERT INTO `db_doctor`
(`department_id`, `name`, `title`, `photo`, `introduction`, `expertise`, `sort`, `status`, `create_time`, `update_time`)
SELECT d.id, '周雅宁', '副主任医师', NULL,
       '擅长慢病长期管理，能结合用药和检查指标给出复诊阶段建议。',
       '高血压、糖尿病、慢病复诊、用药评估、报告解读',
       20, 1, NOW(), NOW()
FROM `db_department` d
WHERE d.`code` = 'INTERNAL_MEDICINE'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor` x
    WHERE x.`department_id` = d.id AND x.`name` = '周雅宁'
  );

INSERT INTO `db_doctor`
(`department_id`, `name`, `title`, `photo`, `introduction`, `expertise`, `sort`, `status`, `create_time`, `update_time`)
SELECT d.id, '陈知夏', '主治医师', NULL,
       '专注儿童常见急性症状咨询，尤其适合发热、咳嗽与日常护理相关场景。',
       '儿童发热、咳嗽、腹泻、喂养与居家护理咨询',
       30, 1, NOW(), NOW()
FROM `db_department` d
WHERE d.`code` = 'PEDIATRICS'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor` x
    WHERE x.`department_id` = d.id AND x.`name` = '陈知夏'
  );

INSERT INTO `db_doctor`
(`department_id`, `name`, `title`, `photo`, `introduction`, `expertise`, `sort`, `status`, `create_time`, `update_time`)
SELECT d.id, '许清岚', '主治医师', NULL,
       '擅长线上收集女性健康相关资料，帮助用户更高效进入后续问诊流程。',
       '月经异常、备孕咨询、妇科炎症初步评估、隐私健康问题咨询',
       40, 1, NOW(), NOW()
FROM `db_department` d
WHERE d.`code` = 'GYNECOLOGY'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor` x
    WHERE x.`department_id` = d.id AND x.`name` = '许清岚'
  );

INSERT INTO `db_doctor`
(`department_id`, `name`, `title`, `photo`, `introduction`, `expertise`, `sort`, `status`, `create_time`, `update_time`)
SELECT d.id, '宋临川', '主治医师', NULL,
       '适合线上初步识别皮肤问题类型，并结合病程与用药情况做分层建议。',
       '皮疹初筛、湿疹护理、痤疮复诊、常见皮肤问题用药指导',
       50, 1, NOW(), NOW()
FROM `db_department` d
WHERE d.`code` = 'DERMATOLOGY'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor` x
    WHERE x.`department_id` = d.id AND x.`name` = '宋临川'
  );

-- -------------------------
-- 3. 医生登录账号（演示）
-- 默认密码：Doctor@123
-- -------------------------
INSERT INTO `db_account`
(`username`, `password`, `email`, `role`, `avatar`, `register_time`)
SELECT 'doctor_linyuanhang', '$2a$10$CWy..OLIocnoOwSwz7O2uuAGkzjqTjxgsAPjTLOL/svyJ3SK4NxG2', 'doctor_linyuanhang@example.com', 'doctor', NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_account` WHERE `username` = 'doctor_linyuanhang');

INSERT INTO `db_account`
(`username`, `password`, `email`, `role`, `avatar`, `register_time`)
SELECT 'doctor_zhouyaning', '$2a$10$CWy..OLIocnoOwSwz7O2uuAGkzjqTjxgsAPjTLOL/svyJ3SK4NxG2', 'doctor_zhouyaning@example.com', 'doctor', NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_account` WHERE `username` = 'doctor_zhouyaning');

INSERT INTO `db_account`
(`username`, `password`, `email`, `role`, `avatar`, `register_time`)
SELECT 'doctor_chenzhixia', '$2a$10$CWy..OLIocnoOwSwz7O2uuAGkzjqTjxgsAPjTLOL/svyJ3SK4NxG2', 'doctor_chenzhixia@example.com', 'doctor', NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_account` WHERE `username` = 'doctor_chenzhixia');

INSERT INTO `db_account`
(`username`, `password`, `email`, `role`, `avatar`, `register_time`)
SELECT 'doctor_xuqinglan', '$2a$10$CWy..OLIocnoOwSwz7O2uuAGkzjqTjxgsAPjTLOL/svyJ3SK4NxG2', 'doctor_xuqinglan@example.com', 'doctor', NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_account` WHERE `username` = 'doctor_xuqinglan');

INSERT INTO `db_account`
(`username`, `password`, `email`, `role`, `avatar`, `register_time`)
SELECT 'doctor_songlinchuan', '$2a$10$CWy..OLIocnoOwSwz7O2uuAGkzjqTjxgsAPjTLOL/svyJ3SK4NxG2', 'doctor_songlinchuan@example.com', 'doctor', NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_account` WHERE `username` = 'doctor_songlinchuan');

UPDATE `db_account` SET `role` = 'doctor' WHERE `username` IN (
  'doctor_linyuanhang',
  'doctor_zhouyaning',
  'doctor_chenzhixia',
  'doctor_xuqinglan',
  'doctor_songlinchuan'
);

UPDATE `db_doctor` doc
JOIN `db_account` acc ON acc.`username` = 'doctor_linyuanhang'
SET doc.`account_id` = acc.`id`
WHERE doc.`name` = '林远航' AND doc.`account_id` IS NULL;

UPDATE `db_doctor` doc
JOIN `db_account` acc ON acc.`username` = 'doctor_zhouyaning'
SET doc.`account_id` = acc.`id`
WHERE doc.`name` = '周雅宁' AND doc.`account_id` IS NULL;

UPDATE `db_doctor` doc
JOIN `db_account` acc ON acc.`username` = 'doctor_chenzhixia'
SET doc.`account_id` = acc.`id`
WHERE doc.`name` = '陈知夏' AND doc.`account_id` IS NULL;

UPDATE `db_doctor` doc
JOIN `db_account` acc ON acc.`username` = 'doctor_xuqinglan'
SET doc.`account_id` = acc.`id`
WHERE doc.`name` = '许清岚' AND doc.`account_id` IS NULL;

UPDATE `db_doctor` doc
JOIN `db_account` acc ON acc.`username` = 'doctor_songlinchuan'
SET doc.`account_id` = acc.`id`
WHERE doc.`name` = '宋临川' AND doc.`account_id` IS NULL;

-- -------------------------
-- 4. 首页推荐医生（可选）
-- -------------------------
INSERT INTO `db_homepage_recommend_doctor`
(`doctor_id`, `display_title`, `recommend_reason`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, '首诊分诊顾问', '适合首次在线描述症状，快速获得就医方向建议。', 10, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'GENERAL_MEDICINE' AND doc.name = '林远航'
  AND NOT EXISTS (SELECT 1 FROM `db_homepage_recommend_doctor` r WHERE r.doctor_id = doc.id);

INSERT INTO `db_homepage_recommend_doctor`
(`doctor_id`, `display_title`, `recommend_reason`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, '慢病管理顾问', '适合高血压、糖尿病等慢病复诊和用药评估场景。', 20, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'INTERNAL_MEDICINE' AND doc.name = '周雅宁'
  AND NOT EXISTS (SELECT 1 FROM `db_homepage_recommend_doctor` r WHERE r.doctor_id = doc.id);

INSERT INTO `db_homepage_recommend_doctor`
(`doctor_id`, `display_title`, `recommend_reason`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, '儿科在线咨询', '适合儿童发热、咳嗽和家长居家观察咨询。', 30, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'PEDIATRICS' AND doc.name = '陈知夏'
  AND NOT EXISTS (SELECT 1 FROM `db_homepage_recommend_doctor` r WHERE r.doctor_id = doc.id);

INSERT INTO `db_homepage_recommend_doctor`
(`doctor_id`, `display_title`, `recommend_reason`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, '女性健康咨询', '适合月经异常、备孕与隐私健康问题线上沟通。', 40, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'GYNECOLOGY' AND doc.name = '许清岚'
  AND NOT EXISTS (SELECT 1 FROM `db_homepage_recommend_doctor` r WHERE r.doctor_id = doc.id);

-- -------------------------
-- 4. 医生服务标签
-- -------------------------
INSERT INTO `db_doctor_service_tag`
(`doctor_id`, `tag_code`, `tag_name`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'common_consultation', '常见病初诊', 10, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'GENERAL_MEDICINE' AND doc.name = '林远航'
  AND NOT EXISTS (SELECT 1 FROM `db_doctor_service_tag` t WHERE t.doctor_id = doc.id AND t.tag_code = 'common_consultation');

INSERT INTO `db_doctor_service_tag`
(`doctor_id`, `tag_code`, `tag_name`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'report_first_review', '报告初步解读', 20, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'GENERAL_MEDICINE' AND doc.name = '林远航'
  AND NOT EXISTS (SELECT 1 FROM `db_doctor_service_tag` t WHERE t.doctor_id = doc.id AND t.tag_code = 'report_first_review');

INSERT INTO `db_doctor_service_tag`
(`doctor_id`, `tag_code`, `tag_name`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'chronic_followup', '慢病复诊', 10, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'INTERNAL_MEDICINE' AND doc.name = '周雅宁'
  AND NOT EXISTS (SELECT 1 FROM `db_doctor_service_tag` t WHERE t.doctor_id = doc.id AND t.tag_code = 'chronic_followup');

INSERT INTO `db_doctor_service_tag`
(`doctor_id`, `tag_code`, `tag_name`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'medication_review', '用药评估', 20, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'INTERNAL_MEDICINE' AND doc.name = '周雅宁'
  AND NOT EXISTS (SELECT 1 FROM `db_doctor_service_tag` t WHERE t.doctor_id = doc.id AND t.tag_code = 'medication_review');

INSERT INTO `db_doctor_service_tag`
(`doctor_id`, `tag_code`, `tag_name`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'pediatric_fever', '儿童发热咨询', 10, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'PEDIATRICS' AND doc.name = '陈知夏'
  AND NOT EXISTS (SELECT 1 FROM `db_doctor_service_tag` t WHERE t.doctor_id = doc.id AND t.tag_code = 'pediatric_fever');

INSERT INTO `db_doctor_service_tag`
(`doctor_id`, `tag_code`, `tag_name`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'child_cough', '儿童咳嗽与呼吸道症状', 20, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'PEDIATRICS' AND doc.name = '陈知夏'
  AND NOT EXISTS (SELECT 1 FROM `db_doctor_service_tag` t WHERE t.doctor_id = doc.id AND t.tag_code = 'child_cough');

INSERT INTO `db_doctor_service_tag`
(`doctor_id`, `tag_code`, `tag_name`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'women_health', '女性健康咨询', 10, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'GYNECOLOGY' AND doc.name = '许清岚'
  AND NOT EXISTS (SELECT 1 FROM `db_doctor_service_tag` t WHERE t.doctor_id = doc.id AND t.tag_code = 'women_health');

INSERT INTO `db_doctor_service_tag`
(`doctor_id`, `tag_code`, `tag_name`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'pregnancy_precheck', '备孕与孕前咨询', 20, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'GYNECOLOGY' AND doc.name = '许清岚'
  AND NOT EXISTS (SELECT 1 FROM `db_doctor_service_tag` t WHERE t.doctor_id = doc.id AND t.tag_code = 'pregnancy_precheck');

INSERT INTO `db_doctor_service_tag`
(`doctor_id`, `tag_code`, `tag_name`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'rash_screening', '皮疹初筛', 10, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'DERMATOLOGY' AND doc.name = '宋临川'
  AND NOT EXISTS (SELECT 1 FROM `db_doctor_service_tag` t WHERE t.doctor_id = doc.id AND t.tag_code = 'rash_screening');

INSERT INTO `db_doctor_service_tag`
(`doctor_id`, `tag_code`, `tag_name`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'acne_followup', '痤疮复诊', 20, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'DERMATOLOGY' AND doc.name = '宋临川'
  AND NOT EXISTS (SELECT 1 FROM `db_doctor_service_tag` t WHERE t.doctor_id = doc.id AND t.tag_code = 'acne_followup');

-- -------------------------
-- 5. 医生排班
-- -------------------------
INSERT INTO `db_doctor_schedule`
(`doctor_id`, `schedule_date`, `time_period`, `visit_type`, `max_capacity`, `used_capacity`, `status`, `remark`, `create_time`, `update_time`)
SELECT doc.id, '2026-04-01', 'morning', 'both', 20, 3, 1, '适合首诊分诊与图文咨询', NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'GENERAL_MEDICINE' AND doc.name = '林远航'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_schedule` s
    WHERE s.doctor_id = doc.id AND s.schedule_date = '2026-04-01' AND s.time_period = 'morning' AND s.visit_type = 'both'
  );

INSERT INTO `db_doctor_schedule`
(`doctor_id`, `schedule_date`, `time_period`, `visit_type`, `max_capacity`, `used_capacity`, `status`, `remark`, `create_time`, `update_time`)
SELECT doc.id, '2026-04-02', 'afternoon', 'online', 18, 5, 1, '线上图文问诊时段', NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'GENERAL_MEDICINE' AND doc.name = '林远航'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_schedule` s
    WHERE s.doctor_id = doc.id AND s.schedule_date = '2026-04-02' AND s.time_period = 'afternoon' AND s.visit_type = 'online'
  );

INSERT INTO `db_doctor_schedule`
(`doctor_id`, `schedule_date`, `time_period`, `visit_type`, `max_capacity`, `used_capacity`, `status`, `remark`, `create_time`, `update_time`)
SELECT doc.id, '2026-04-01', 'afternoon', 'followup', 15, 6, 1, '慢病复诊专场', NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'INTERNAL_MEDICINE' AND doc.name = '周雅宁'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_schedule` s
    WHERE s.doctor_id = doc.id AND s.schedule_date = '2026-04-01' AND s.time_period = 'afternoon' AND s.visit_type = 'followup'
  );

INSERT INTO `db_doctor_schedule`
(`doctor_id`, `schedule_date`, `time_period`, `visit_type`, `max_capacity`, `used_capacity`, `status`, `remark`, `create_time`, `update_time`)
SELECT doc.id, '2026-04-03', 'morning', 'both', 16, 4, 1, '支持慢病复诊和报告解读', NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'INTERNAL_MEDICINE' AND doc.name = '周雅宁'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_schedule` s
    WHERE s.doctor_id = doc.id AND s.schedule_date = '2026-04-03' AND s.time_period = 'morning' AND s.visit_type = 'both'
  );

INSERT INTO `db_doctor_schedule`
(`doctor_id`, `schedule_date`, `time_period`, `visit_type`, `max_capacity`, `used_capacity`, `status`, `remark`, `create_time`, `update_time`)
SELECT doc.id, '2026-04-02', 'morning', 'online', 20, 8, 1, '儿童发热线上咨询', NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'PEDIATRICS' AND doc.name = '陈知夏'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_schedule` s
    WHERE s.doctor_id = doc.id AND s.schedule_date = '2026-04-02' AND s.time_period = 'morning' AND s.visit_type = 'online'
  );

INSERT INTO `db_doctor_schedule`
(`doctor_id`, `schedule_date`, `time_period`, `visit_type`, `max_capacity`, `used_capacity`, `status`, `remark`, `create_time`, `update_time`)
SELECT doc.id, '2026-04-04', 'morning', 'both', 18, 7, 1, '支持儿童常见症状咨询', NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'PEDIATRICS' AND doc.name = '陈知夏'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_schedule` s
    WHERE s.doctor_id = doc.id AND s.schedule_date = '2026-04-04' AND s.time_period = 'morning' AND s.visit_type = 'both'
  );

INSERT INTO `db_doctor_schedule`
(`doctor_id`, `schedule_date`, `time_period`, `visit_type`, `max_capacity`, `used_capacity`, `status`, `remark`, `create_time`, `update_time`)
SELECT doc.id, '2026-04-03', 'evening', 'online', 12, 2, 1, '女性隐私健康线上咨询', NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'GYNECOLOGY' AND doc.name = '许清岚'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_schedule` s
    WHERE s.doctor_id = doc.id AND s.schedule_date = '2026-04-03' AND s.time_period = 'evening' AND s.visit_type = 'online'
  );

INSERT INTO `db_doctor_schedule`
(`doctor_id`, `schedule_date`, `time_period`, `visit_type`, `max_capacity`, `used_capacity`, `status`, `remark`, `create_time`, `update_time`)
SELECT doc.id, '2026-04-05', 'morning', 'offline', 14, 3, 1, '线下妇科复查时段', NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'GYNECOLOGY' AND doc.name = '许清岚'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_schedule` s
    WHERE s.doctor_id = doc.id AND s.schedule_date = '2026-04-05' AND s.time_period = 'morning' AND s.visit_type = 'offline'
  );

INSERT INTO `db_doctor_schedule`
(`doctor_id`, `schedule_date`, `time_period`, `visit_type`, `max_capacity`, `used_capacity`, `status`, `remark`, `create_time`, `update_time`)
SELECT doc.id, '2026-04-04', 'afternoon', 'online', 16, 5, 1, '皮疹和痤疮在线复诊', NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'DERMATOLOGY' AND doc.name = '宋临川'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_schedule` s
    WHERE s.doctor_id = doc.id AND s.schedule_date = '2026-04-04' AND s.time_period = 'afternoon' AND s.visit_type = 'online'
  );

INSERT INTO `db_doctor_schedule`
(`doctor_id`, `schedule_date`, `time_period`, `visit_type`, `max_capacity`, `used_capacity`, `status`, `remark`, `create_time`, `update_time`)
SELECT doc.id, '2026-04-06', 'evening', 'both', 12, 1, 1, '适合上传患处照片后的进一步沟通', NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.code = 'DERMATOLOGY' AND doc.name = '宋临川'
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_schedule` s
    WHERE s.doctor_id = doc.id AND s.schedule_date = '2026-04-06' AND s.time_period = 'evening' AND s.visit_type = 'both'
  );

-- -------------------------
-- 6. 问诊分类
-- -------------------------
INSERT INTO `db_consultation_category`
(`department_id`, `name`, `code`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT d.id, '图文问诊', 'TEXT_CONSULT', '适合首次线上描述症状，让系统先进行基础信息采集和分诊。', 10, 1, NOW(), NOW()
FROM `db_department` d
WHERE d.code = 'GENERAL_MEDICINE'
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_category` c WHERE c.code = 'TEXT_CONSULT');

INSERT INTO `db_consultation_category`
(`department_id`, `name`, `code`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT d.id, '慢病复诊', 'CHRONIC_FOLLOWUP', '适用于高血压、糖尿病等长期管理场景，强调近期指标和用药情况。', 20, 1, NOW(), NOW()
FROM `db_department` d
WHERE d.code = 'INTERNAL_MEDICINE'
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_category` c WHERE c.code = 'CHRONIC_FOLLOWUP');

INSERT INTO `db_consultation_category`
(`department_id`, `name`, `code`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT d.id, '检查报告解读', 'REPORT_INTERPRET', '适用于用户上传检查报告后，请医生协助理解结果含义。', 30, 1, NOW(), NOW()
FROM `db_department` d
WHERE d.code = 'INTERNAL_MEDICINE'
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_category` c WHERE c.code = 'REPORT_INTERPRET');

INSERT INTO `db_consultation_category`
(`department_id`, `name`, `code`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT d.id, '儿科发热咨询', 'PEDI_FEVER', '适用于家长咨询孩子发热、咳嗽、精神状态和居家观察问题。', 40, 1, NOW(), NOW()
FROM `db_department` d
WHERE d.code = 'PEDIATRICS'
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_category` c WHERE c.code = 'PEDI_FEVER');

INSERT INTO `db_consultation_category`
(`department_id`, `name`, `code`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT d.id, '女性健康咨询', 'WOMEN_HEALTH', '适用于月经、备孕、白带异常和其他女性健康问题。', 50, 1, NOW(), NOW()
FROM `db_department` d
WHERE d.code = 'GYNECOLOGY'
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_category` c WHERE c.code = 'WOMEN_HEALTH');

INSERT INTO `db_consultation_category`
(`department_id`, `name`, `code`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT d.id, '皮肤问题咨询', 'SKIN_ISSUE', '适用于皮疹、湿疹、痤疮和过敏相关皮肤问题。', 60, 1, NOW(), NOW()
FROM `db_department` d
WHERE d.code = 'DERMATOLOGY'
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_category` c WHERE c.code = 'SKIN_ISSUE');

-- -------------------------
-- 7. 问诊前置模板
-- -------------------------
INSERT INTO `db_consultation_intake_template`
(`category_id`, `name`, `description`, `version`, `is_default`, `status`, `create_time`, `update_time`)
SELECT c.id, '图文问诊标准模板', '适用于首次在线问诊时的通用信息采集。', 1, 1, 1, NOW(), NOW()
FROM `db_consultation_category` c
WHERE c.code = 'TEXT_CONSULT'
  AND NOT EXISTS (
    SELECT 1 FROM `db_consultation_intake_template` t
    WHERE t.category_id = c.id AND t.name = '图文问诊标准模板' AND t.version = 1
  );

INSERT INTO `db_consultation_intake_template`
(`category_id`, `name`, `description`, `version`, `is_default`, `status`, `create_time`, `update_time`)
SELECT c.id, '慢病复诊模板', '重点采集慢病名称、当前用药和近期指标。', 1, 1, 1, NOW(), NOW()
FROM `db_consultation_category` c
WHERE c.code = 'CHRONIC_FOLLOWUP'
  AND NOT EXISTS (
    SELECT 1 FROM `db_consultation_intake_template` t
    WHERE t.category_id = c.id AND t.name = '慢病复诊模板' AND t.version = 1
  );

INSERT INTO `db_consultation_intake_template`
(`category_id`, `name`, `description`, `version`, `is_default`, `status`, `create_time`, `update_time`)
SELECT c.id, '报告解读模板', '重点采集报告类型、报告日期和附件。', 1, 1, 1, NOW(), NOW()
FROM `db_consultation_category` c
WHERE c.code = 'REPORT_INTERPRET'
  AND NOT EXISTS (
    SELECT 1 FROM `db_consultation_intake_template` t
    WHERE t.category_id = c.id AND t.name = '报告解读模板' AND t.version = 1
  );

INSERT INTO `db_consultation_intake_template`
(`category_id`, `name`, `description`, `version`, `is_default`, `status`, `create_time`, `update_time`)
SELECT c.id, '儿科发热模板', '适用于家长提交孩子发热相关前置信息。', 1, 1, 1, NOW(), NOW()
FROM `db_consultation_category` c
WHERE c.code = 'PEDI_FEVER'
  AND NOT EXISTS (
    SELECT 1 FROM `db_consultation_intake_template` t
    WHERE t.category_id = c.id AND t.name = '儿科发热模板' AND t.version = 1
  );

INSERT INTO `db_consultation_intake_template`
(`category_id`, `name`, `description`, `version`, `is_default`, `status`, `create_time`, `update_time`)
SELECT c.id, '女性健康模板', '适用于女性健康咨询的隐私资料采集。', 1, 1, 1, NOW(), NOW()
FROM `db_consultation_category` c
WHERE c.code = 'WOMEN_HEALTH'
  AND NOT EXISTS (
    SELECT 1 FROM `db_consultation_intake_template` t
    WHERE t.category_id = c.id AND t.name = '女性健康模板' AND t.version = 1
  );

INSERT INTO `db_consultation_intake_template`
(`category_id`, `name`, `description`, `version`, `is_default`, `status`, `create_time`, `update_time`)
SELECT c.id, '皮肤问题模板', '适用于皮疹、痤疮、湿疹等皮肤问题的资料采集。', 1, 1, 1, NOW(), NOW()
FROM `db_consultation_category` c
WHERE c.code = 'SKIN_ISSUE'
  AND NOT EXISTS (
    SELECT 1 FROM `db_consultation_intake_template` t
    WHERE t.category_id = c.id AND t.name = '皮肤问题模板' AND t.version = 1
  );

-- -------------------------
-- 8. 模板字段：图文问诊
-- -------------------------
INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'chief_complaint', '主要症状', 'textarea', 1, NULL, NULL, '例如：咳嗽 3 天，夜间加重', '建议尽量描述开始时间、主要不适和变化过程', NULL, 'length<=500', 10, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'TEXT_CONSULT' AND t.name = '图文问诊标准模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'chief_complaint');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'symptom_duration', '症状持续时间', 'single_select', 1, '["1天内","3天内","1周内","1个月以上"]', NULL, NULL, '帮助系统判断病程阶段', NULL, NULL, 20, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'TEXT_CONSULT' AND t.name = '图文问诊标准模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'symptom_duration');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'severity_level', '当前严重程度', 'single_select', 1, '["轻度","中度","重度"]', NULL, NULL, '用于后续分诊优先级判断', NULL, NULL, 30, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'TEXT_CONSULT' AND t.name = '图文问诊标准模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'severity_level');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'accompanying_symptoms', '伴随症状', 'multi_select', 0, '["发热","咳嗽","头痛","恶心","腹痛","乏力"]', NULL, NULL, '可多选，帮助补充症状画像', NULL, NULL, 40, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'TEXT_CONSULT' AND t.name = '图文问诊标准模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'accompanying_symptoms');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'related_files', '相关资料', 'upload', 0, NULL, NULL, NULL, '可上传检查单、化验单、患处照片等资料', NULL, NULL, 50, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'TEXT_CONSULT' AND t.name = '图文问诊标准模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'related_files');

-- -------------------------
-- 9. 模板字段：慢病复诊
-- -------------------------
INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'disease_name', '慢病名称', 'input', 1, NULL, NULL, '例如：高血压 / 糖尿病', '可填写一个或多个已确诊的慢病名称', NULL, 'length<=100', 10, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'CHRONIC_FOLLOWUP' AND t.name = '慢病复诊模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'disease_name');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'current_medicine', '当前用药', 'textarea', 1, NULL, NULL, '例如：缬沙坦 80mg，每日一次', '建议写清药名、剂量和服用频率', NULL, 'length<=500', 20, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'CHRONIC_FOLLOWUP' AND t.name = '慢病复诊模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'current_medicine');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'recent_indicator', '近期指标', 'textarea', 0, NULL, NULL, '例如：血压 138/88 mmHg，空腹血糖 7.2 mmol/L', '便于医生结合复诊阶段判断', NULL, 'length<=500', 30, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'CHRONIC_FOLLOWUP' AND t.name = '慢病复诊模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'recent_indicator');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'refill_needed', '是否需要续方', 'switch', 1, NULL, '0', NULL, '用于区分仅复诊评估还是需要续方沟通', NULL, NULL, 40, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'CHRONIC_FOLLOWUP' AND t.name = '慢病复诊模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'refill_needed');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'recent_discomfort', '近期不适', 'textarea', 0, NULL, NULL, '例如：最近一周偶有头晕', '复诊时如有新变化建议补充说明', NULL, 'length<=500', 50, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'CHRONIC_FOLLOWUP' AND t.name = '慢病复诊模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'recent_discomfort');

-- -------------------------
-- 10. 模板字段：报告解读
-- -------------------------
INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'report_type', '报告类型', 'single_select', 1, '["血常规","尿常规","生化检查","影像报告","病理报告","其他"]', NULL, NULL, '帮助系统判断更适合的解读方向', NULL, NULL, 10, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'REPORT_INTERPRET' AND t.name = '报告解读模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'report_type');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'report_date', '报告日期', 'date', 1, NULL, NULL, NULL, '有助于判断报告时效性', NULL, NULL, 20, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'REPORT_INTERPRET' AND t.name = '报告解读模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'report_date');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'report_file', '报告附件', 'upload', 1, NULL, NULL, NULL, '请上传清晰完整的报告照片或 PDF', NULL, NULL, 30, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'REPORT_INTERPRET' AND t.name = '报告解读模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'report_file');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'abnormal_focus', '最担心的异常项', 'textarea', 0, NULL, NULL, '例如：转氨酶偏高、白细胞偏低', '方便医生针对性解释', NULL, 'length<=300', 40, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'REPORT_INTERPRET' AND t.name = '报告解读模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'abnormal_focus');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'symptom_relation', '是否伴随症状', 'switch', 1, NULL, '0', NULL, '如果同时存在不适症状，后续分诊会更准确', NULL, NULL, 50, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'REPORT_INTERPRET' AND t.name = '报告解读模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'symptom_relation');

-- -------------------------
-- 11. 模板字段：儿科发热
-- -------------------------
INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'child_age', '儿童年龄', 'number', 1, NULL, NULL, '单位：岁', '如不足 1 岁，可填写 0.5', NULL, NULL, 10, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'PEDI_FEVER' AND t.name = '儿科发热模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'child_age');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'max_temperature', '最高体温', 'input', 1, NULL, NULL, '例如：39.2℃', '建议填写测量到的最高体温', NULL, 'length<=20', 20, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'PEDI_FEVER' AND t.name = '儿科发热模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'max_temperature');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'fever_duration', '发热持续时间', 'single_select', 1, '["6小时内","24小时内","48小时内","72小时以上"]', NULL, NULL, '帮助医生判断病程进展', NULL, NULL, 30, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'PEDI_FEVER' AND t.name = '儿科发热模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'fever_duration');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'accompany_signs', '伴随表现', 'multi_select', 1, '["咳嗽","流鼻涕","腹泻","呕吐","精神差","抽搐史"]', NULL, NULL, '可多选，帮助判断是否需要进一步就医', NULL, NULL, 40, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'PEDI_FEVER' AND t.name = '儿科发热模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'accompany_signs');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'mental_state', '精神状态', 'single_select', 1, '["精神可","一般","明显萎靡"]', NULL, NULL, '儿科分诊中非常重要', NULL, NULL, 50, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'PEDI_FEVER' AND t.name = '儿科发热模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'mental_state');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'exposure_history', '是否有接触史', 'switch', 0, NULL, '0', NULL, '如近期接触流感、手足口、水痘等', NULL, NULL, 60, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'PEDI_FEVER' AND t.name = '儿科发热模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'exposure_history');

-- -------------------------
-- 12. 模板字段：女性健康
-- -------------------------
INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'main_issue', '主要问题', 'textarea', 1, NULL, NULL, '例如：月经推迟 10 天并伴轻微腹痛', '建议描述最主要的不适和开始时间', NULL, 'length<=500', 10, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'WOMEN_HEALTH' AND t.name = '女性健康模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'main_issue');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'menstrual_status', '月经情况', 'single_select', 0, '["规律","推迟","提前","经量异常","已绝经"]', NULL, NULL, '用于辅助判断问题方向', NULL, NULL, 20, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'WOMEN_HEALTH' AND t.name = '女性健康模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'menstrual_status');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'pregnancy_possible', '是否可能怀孕', 'switch', 1, NULL, '0', NULL, '用于后续分诊与风险提示', NULL, NULL, 30, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'WOMEN_HEALTH' AND t.name = '女性健康模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'pregnancy_possible');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'symptom_duration', '症状持续时间', 'single_select', 1, '["当天开始","3天内","1周内","1个月以上"]', NULL, NULL, '帮助判断急慢性程度', NULL, NULL, 40, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'WOMEN_HEALTH' AND t.name = '女性健康模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'symptom_duration');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'secretion_change', '是否伴有分泌物异常', 'switch', 0, NULL, '0', NULL, '仅作为资料补充，不代表最终诊断', NULL, NULL, 50, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'WOMEN_HEALTH' AND t.name = '女性健康模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'secretion_change');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'privacy_note', '补充说明', 'textarea', 0, NULL, NULL, '可填写不方便在主诉中直接描述的内容', '隐私场景可在此补充更多说明', NULL, 'length<=500', 60, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'WOMEN_HEALTH' AND t.name = '女性健康模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'privacy_note');

-- -------------------------
-- 13. 模板字段：皮肤问题
-- -------------------------
INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'skin_problem', '皮肤问题描述', 'textarea', 1, NULL, NULL, '例如：面部反复起红疹并伴瘙痒 1 周', '建议描述外观变化、是否扩散、是否反复', NULL, 'length<=500', 10, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'SKIN_ISSUE' AND t.name = '皮肤问题模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'skin_problem');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'affected_area', '主要部位', 'single_select', 1, '["面部","颈部","躯干","四肢","私密部位","全身"]', NULL, NULL, '帮助系统快速定位问题部位', NULL, NULL, 20, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'SKIN_ISSUE' AND t.name = '皮肤问题模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'affected_area');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'duration', '症状持续时间', 'single_select', 1, '["1天内","1周内","1个月内","3个月以上"]', NULL, NULL, '用于区分急性和慢性变化', NULL, NULL, 30, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'SKIN_ISSUE' AND t.name = '皮肤问题模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'duration');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'itch_level', '瘙痒程度', 'single_select', 0, '["无","轻度","中度","重度"]', NULL, NULL, '帮助判断不适程度', NULL, NULL, 40, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'SKIN_ISSUE' AND t.name = '皮肤问题模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'itch_level');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'lesion_photo', '病灶照片', 'upload', 0, NULL, NULL, NULL, '建议上传清晰、无遮挡、光线充足的患处照片', NULL, NULL, 50, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'SKIN_ISSUE' AND t.name = '皮肤问题模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'lesion_photo');

INSERT INTO `db_consultation_intake_field`
(`template_id`, `field_code`, `field_label`, `field_type`, `is_required`, `options_json`, `default_value`, `placeholder`, `help_text`, `condition_rule`, `validation_rule`, `sort`, `status`, `create_time`, `update_time`)
SELECT t.id, 'medicine_used', '是否已自行用药', 'switch', 0, NULL, '0', NULL, '若已用药，后续问诊时建议补充药名', NULL, NULL, 60, 1, NOW(), NOW()
FROM `db_consultation_intake_template` t
JOIN `db_consultation_category` c ON c.id = t.category_id
WHERE c.code = 'SKIN_ISSUE' AND t.name = '皮肤问题模板' AND t.version = 1
  AND NOT EXISTS (SELECT 1 FROM `db_consultation_intake_field` f WHERE f.template_id = t.id AND f.field_code = 'medicine_used');

-- -------------------------
-- 14. 身体部位字典
-- -------------------------
INSERT INTO `db_body_part_dict`
(`name`, `code`, `parent_id`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT '全身', 'WHOLE_BODY', NULL, '适合承接发热、乏力等全身性症状表达。', 10, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_body_part_dict` WHERE `code` = 'WHOLE_BODY');

INSERT INTO `db_body_part_dict`
(`name`, `code`, `parent_id`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT '头颈部', 'HEAD_NECK', NULL, '适合归类头痛、头晕、咽喉不适等上部相关表达。', 20, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_body_part_dict` WHERE `code` = 'HEAD_NECK');

INSERT INTO `db_body_part_dict`
(`name`, `code`, `parent_id`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT '咽喉', 'THROAT', parent.id, '作为头颈部下级部位，适合咽痛、咽干、吞咽不适等表述。', 30, 1, NOW(), NOW()
FROM `db_body_part_dict` parent
WHERE parent.`code` = 'HEAD_NECK'
  AND NOT EXISTS (SELECT 1 FROM `db_body_part_dict` WHERE `code` = 'THROAT');

INSERT INTO `db_body_part_dict`
(`name`, `code`, `parent_id`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT '胸部', 'CHEST', NULL, '适合胸痛、胸闷、咳嗽、呼吸困难等胸部相关表达。', 40, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_body_part_dict` WHERE `code` = 'CHEST');

INSERT INTO `db_body_part_dict`
(`name`, `code`, `parent_id`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT '腹部', 'ABDOMEN', NULL, '适合腹痛、腹胀、恶心呕吐等消化道相关表达。', 50, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_body_part_dict` WHERE `code` = 'ABDOMEN');

INSERT INTO `db_body_part_dict`
(`name`, `code`, `parent_id`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT '皮肤', 'SKIN', NULL, '适合皮疹、红斑、瘙痒、破溃等皮肤表现。', 60, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_body_part_dict` WHERE `code` = 'SKIN');

-- -------------------------
-- 15. 症状字典
-- -------------------------
INSERT INTO `db_symptom_dict`
(`body_part_id`, `name`, `code`, `keywords`, `alias_keywords`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT bp.id, '发热', 'FEVER', '发烧,体温高,高热', '持续发热,反复发热', '适合全身性发热相关描述。', 10, 1, NOW(), NOW()
FROM `db_body_part_dict` bp
WHERE bp.`code` = 'WHOLE_BODY'
  AND NOT EXISTS (SELECT 1 FROM `db_symptom_dict` WHERE `code` = 'FEVER');

INSERT INTO `db_symptom_dict`
(`body_part_id`, `name`, `code`, `keywords`, `alias_keywords`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT bp.id, '乏力', 'FATIGUE', '没劲,没力气,无力', '容易疲劳,浑身无力', '适合全身无力、精神差等表达。', 20, 1, NOW(), NOW()
FROM `db_body_part_dict` bp
WHERE bp.`code` = 'WHOLE_BODY'
  AND NOT EXISTS (SELECT 1 FROM `db_symptom_dict` WHERE `code` = 'FATIGUE');

INSERT INTO `db_symptom_dict`
(`body_part_id`, `name`, `code`, `keywords`, `alias_keywords`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT bp.id, '剧烈头痛', 'SEVERE_HEADACHE', '头特别痛,头痛厉害,爆炸样头痛', '头痛加重,难以忍受的头痛', '适合作为头痛高风险场景识别症状。', 30, 1, NOW(), NOW()
FROM `db_body_part_dict` bp
WHERE bp.`code` = 'HEAD_NECK'
  AND NOT EXISTS (SELECT 1 FROM `db_symptom_dict` WHERE `code` = 'SEVERE_HEADACHE');

INSERT INTO `db_symptom_dict`
(`body_part_id`, `name`, `code`, `keywords`, `alias_keywords`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT bp.id, '咽痛', 'SORE_THROAT', '嗓子痛,喉咙痛,吞咽痛', '咽部疼痛,咽喉疼', '适合咽喉不适相关描述。', 40, 1, NOW(), NOW()
FROM `db_body_part_dict` bp
WHERE bp.`code` = 'THROAT'
  AND NOT EXISTS (SELECT 1 FROM `db_symptom_dict` WHERE `code` = 'SORE_THROAT');

INSERT INTO `db_symptom_dict`
(`body_part_id`, `name`, `code`, `keywords`, `alias_keywords`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT bp.id, '咳嗽', 'COUGH', '咳,咳个不停,咳得厉害', '干咳,咳痰', '适合作为呼吸道常见症状识别。', 50, 1, NOW(), NOW()
FROM `db_body_part_dict` bp
WHERE bp.`code` = 'CHEST'
  AND NOT EXISTS (SELECT 1 FROM `db_symptom_dict` WHERE `code` = 'COUGH');

INSERT INTO `db_symptom_dict`
(`body_part_id`, `name`, `code`, `keywords`, `alias_keywords`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT bp.id, '胸痛', 'CHEST_PAIN', '胸口痛,心口痛,胸前痛', '胸部刺痛,胸部压痛', '适合作为胸部高风险症状识别。', 60, 1, NOW(), NOW()
FROM `db_body_part_dict` bp
WHERE bp.`code` = 'CHEST'
  AND NOT EXISTS (SELECT 1 FROM `db_symptom_dict` WHERE `code` = 'CHEST_PAIN');

INSERT INTO `db_symptom_dict`
(`body_part_id`, `name`, `code`, `keywords`, `alias_keywords`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT bp.id, '呼吸困难', 'DYSPNEA', '喘不上气,呼吸费力,气不够', '气短,憋气', '适合作为胸部和呼吸风险症状识别。', 70, 1, NOW(), NOW()
FROM `db_body_part_dict` bp
WHERE bp.`code` = 'CHEST'
  AND NOT EXISTS (SELECT 1 FROM `db_symptom_dict` WHERE `code` = 'DYSPNEA');

INSERT INTO `db_symptom_dict`
(`body_part_id`, `name`, `code`, `keywords`, `alias_keywords`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT bp.id, '腹痛', 'ABDOMINAL_PAIN', '肚子痛,胃痛,腹部疼', '肚子绞痛,肚子难受', '适合作为腹部不适通用症状。', 80, 1, NOW(), NOW()
FROM `db_body_part_dict` bp
WHERE bp.`code` = 'ABDOMEN'
  AND NOT EXISTS (SELECT 1 FROM `db_symptom_dict` WHERE `code` = 'ABDOMINAL_PAIN');

INSERT INTO `db_symptom_dict`
(`body_part_id`, `name`, `code`, `keywords`, `alias_keywords`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT bp.id, '呕吐', 'VOMITING', '吐了,反胃呕吐,一直吐', '恶心呕吐,反复呕吐', '适合作为消化道症状补充识别。', 90, 1, NOW(), NOW()
FROM `db_body_part_dict` bp
WHERE bp.`code` = 'ABDOMEN'
  AND NOT EXISTS (SELECT 1 FROM `db_symptom_dict` WHERE `code` = 'VOMITING');

INSERT INTO `db_symptom_dict`
(`body_part_id`, `name`, `code`, `keywords`, `alias_keywords`, `description`, `sort`, `status`, `create_time`, `update_time`)
SELECT bp.id, '皮疹', 'RASH', '起疹子,红疹,皮肤起包', '皮肤发红,身上长疹', '适合作为皮肤异常表现识别。', 100, 1, NOW(), NOW()
FROM `db_body_part_dict` bp
WHERE bp.`code` = 'SKIN'
  AND NOT EXISTS (SELECT 1 FROM `db_symptom_dict` WHERE `code` = 'RASH');

-- -------------------------
-- 16. 分诊等级字典
-- -------------------------
INSERT INTO `db_triage_level_dict`
(`name`, `code`, `description`, `suggestion`, `color`, `priority`, `sort`, `status`, `create_time`, `update_time`)
SELECT '立即急诊', 'EMERGENCY', '用于强烈提示存在急危重症风险的场景。', '建议立即前往急诊或呼叫急救，不建议继续等待线上问诊。', '#D03050', 100, 10, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_triage_level_dict` WHERE `code` = 'EMERGENCY');

INSERT INTO `db_triage_level_dict`
(`name`, `code`, `description`, `suggestion`, `color`, `priority`, `sort`, `status`, `create_time`, `update_time`)
SELECT '尽快线下就医', 'URGENT', '用于建议尽快前往线下门诊进一步评估的场景。', '建议尽快线下就医，可先完成基础资料补充后安排问诊。', '#F59E0B', 80, 20, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_triage_level_dict` WHERE `code` = 'URGENT');

INSERT INTO `db_triage_level_dict`
(`name`, `code`, `description`, `suggestion`, `color`, `priority`, `sort`, `status`, `create_time`, `update_time`)
SELECT '在线咨询', 'ONLINE', '适用于风险相对可控、可先进行线上沟通的场景。', '可先进入在线咨询，由系统推荐合适科室与医生。', '#2F7D6D', 50, 30, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_triage_level_dict` WHERE `code` = 'ONLINE');

INSERT INTO `db_triage_level_dict`
(`name`, `code`, `description`, `suggestion`, `color`, `priority`, `sort`, `status`, `create_time`, `update_time`)
SELECT '居家观察', 'OBSERVE', '适用于可先观察、后续持续追踪的低风险场景。', '建议先记录症状变化，如持续加重再发起进一步咨询。', '#7C8EA0', 20, 40, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `db_triage_level_dict` WHERE `code` = 'OBSERVE');

-- -------------------------
-- 17. 红旗规则
-- -------------------------
INSERT INTO `db_red_flag_rule`
(`rule_name`, `rule_code`, `trigger_type`, `body_part_id`, `keyword_pattern`, `condition_description`, `triage_level_id`, `suggestion`, `action_type`, `priority`, `status`, `create_time`, `update_time`)
SELECT '胸痛伴呼吸困难', 'CHEST_PAIN_DYSPNEA', 'combination', bp.id, NULL,
       '胸部不适同时伴呼吸困难时，应优先评估急症风险。',
       tl.id, '建议立即前往急诊或呼叫急救，不建议继续等待线上问诊。',
       'emergency', 100, 1, NOW(), NOW()
FROM `db_body_part_dict` bp
JOIN `db_triage_level_dict` tl ON tl.`code` = 'EMERGENCY'
WHERE bp.`code` = 'CHEST'
  AND NOT EXISTS (SELECT 1 FROM `db_red_flag_rule` WHERE `rule_code` = 'CHEST_PAIN_DYSPNEA');

INSERT INTO `db_red_flag_rule`
(`rule_name`, `rule_code`, `trigger_type`, `body_part_id`, `keyword_pattern`, `condition_description`, `triage_level_id`, `suggestion`, `action_type`, `priority`, `status`, `create_time`, `update_time`)
SELECT '高热伴抽搐或精神差', 'HIGH_FEVER_ALERT', 'keyword_match', NULL, '高热|39\\.5|抽搐|精神差|意识差',
       '自由文本出现高热并伴神经系统或精神状态异常表达时，需要尽快线下评估。',
       tl.id, '建议尽快线下就医，儿童、老人或基础病人群需优先处理。',
       'offline', 90, 1, NOW(), NOW()
FROM `db_triage_level_dict` tl
WHERE tl.`code` = 'URGENT'
  AND NOT EXISTS (SELECT 1 FROM `db_red_flag_rule` WHERE `rule_code` = 'HIGH_FEVER_ALERT');

INSERT INTO `db_red_flag_rule`
(`rule_name`, `rule_code`, `trigger_type`, `body_part_id`, `keyword_pattern`, `condition_description`, `triage_level_id`, `suggestion`, `action_type`, `priority`, `status`, `create_time`, `update_time`)
SELECT '剧烈头痛线下评估', 'SEVERE_HEADACHE_ALERT', 'symptom_match', bp.id, NULL,
       '当用户明确表达为剧烈头痛时，建议优先线下评估风险。',
       tl.id, '建议尽快线下就医，必要时结合伴随症状进一步排查。',
       'offline', 80, 1, NOW(), NOW()
FROM `db_body_part_dict` bp
JOIN `db_triage_level_dict` tl ON tl.`code` = 'URGENT'
WHERE bp.`code` = 'HEAD_NECK'
  AND NOT EXISTS (SELECT 1 FROM `db_red_flag_rule` WHERE `rule_code` = 'SEVERE_HEADACHE_ALERT');

INSERT INTO `db_red_flag_rule`
(`rule_name`, `rule_code`, `trigger_type`, `body_part_id`, `keyword_pattern`, `condition_description`, `triage_level_id`, `suggestion`, `action_type`, `priority`, `status`, `create_time`, `update_time`)
SELECT '胸部不适优先线下', 'CHEST_DISCOMFORT_OFFLINE', 'body_part_match', bp.id, NULL,
       '当仅能识别到胸部不适但症状尚不明确时，可先提高线下评估优先级。',
       tl.id, '建议结合具体症状继续补充资料，必要时优先安排线下就医。',
       'offline', 60, 1, NOW(), NOW()
FROM `db_body_part_dict` bp
JOIN `db_triage_level_dict` tl ON tl.`code` = 'URGENT'
WHERE bp.`code` = 'CHEST'
  AND NOT EXISTS (SELECT 1 FROM `db_red_flag_rule` WHERE `rule_code` = 'CHEST_DISCOMFORT_OFFLINE');

INSERT INTO `db_red_flag_rule_symptom` (`rule_id`, `symptom_id`)
SELECT r.id, s.id
FROM `db_red_flag_rule` r
JOIN `db_symptom_dict` s ON s.`code` = 'CHEST_PAIN'
WHERE r.`rule_code` = 'CHEST_PAIN_DYSPNEA'
  AND NOT EXISTS (
    SELECT 1 FROM `db_red_flag_rule_symptom` x
    WHERE x.`rule_id` = r.id AND x.`symptom_id` = s.id
  );

INSERT INTO `db_red_flag_rule_symptom` (`rule_id`, `symptom_id`)
SELECT r.id, s.id
FROM `db_red_flag_rule` r
JOIN `db_symptom_dict` s ON s.`code` = 'DYSPNEA'
WHERE r.`rule_code` = 'CHEST_PAIN_DYSPNEA'
  AND NOT EXISTS (
    SELECT 1 FROM `db_red_flag_rule_symptom` x
    WHERE x.`rule_id` = r.id AND x.`symptom_id` = s.id
  );

INSERT INTO `db_red_flag_rule_symptom` (`rule_id`, `symptom_id`)
SELECT r.id, s.id
FROM `db_red_flag_rule` r
JOIN `db_symptom_dict` s ON s.`code` = 'SEVERE_HEADACHE'
WHERE r.`rule_code` = 'SEVERE_HEADACHE_ALERT'
  AND NOT EXISTS (
    SELECT 1 FROM `db_red_flag_rule_symptom` x
    WHERE x.`rule_id` = r.id AND x.`symptom_id` = s.id
  );

-- -------------------------
-- 15. Triage knowledge demo data
-- -------------------------
INSERT INTO `db_triage_knowledge`
(`knowledge_type`, `title`, `content`, `tags`, `department_id`, `doctor_id`, `source_type`, `version`, `sort`, `status`, `create_time`, `update_time`)
SELECT 'triage_strategy', 'Chest pain offline-first assessment',
       'When the chief complaint focuses on chest pain, chest tightness, shortness of breath or worsening discomfort, the system should prioritize offline evaluation. If sweating, radiation pain or near-syncope is also present, the urgency level should be raised and online-only guidance should not be the final recommendation.',
       'chest-pain,offline-first,high-risk', d.id, NULL, 'guideline', 1, 10, 1, NOW(), NOW()
FROM `db_department` d
WHERE d.`code` = 'GENERAL_MEDICINE'
  AND NOT EXISTS (
    SELECT 1 FROM `db_triage_knowledge`
    WHERE `knowledge_type` = 'triage_strategy' AND `title` = 'Chest pain offline-first assessment'
  );

INSERT INTO `db_triage_knowledge`
(`knowledge_type`, `title`, `content`, `tags`, `department_id`, `doctor_id`, `source_type`, `version`, `sort`, `status`, `create_time`, `update_time`)
SELECT 'department_guide', 'Pediatric fever online triage boundary',
       'Online pediatric fever triage should first collect temperature, duration, mental status, fluid intake and accompanying symptoms. If the child is very young, obviously lethargic, has persistent high fever or a seizure history, the recommended action should switch to offline care instead of continued online observation.',
       'pediatrics,fever,boundary', d.id, NULL, 'manual', 1, 20, 1, NOW(), NOW()
FROM `db_department` d
WHERE d.`code` = 'PEDIATRICS'
  AND NOT EXISTS (
    SELECT 1 FROM `db_triage_knowledge`
    WHERE `knowledge_type` = 'department_guide' AND `title` = 'Pediatric fever online triage boundary'
  );

INSERT INTO `db_triage_knowledge`
(`knowledge_type`, `title`, `content`, `tags`, `department_id`, `doctor_id`, `source_type`, `version`, `sort`, `status`, `create_time`, `update_time`)
SELECT 'doctor_profile', 'Zhou Yaning chronic care profile',
       'This doctor profile is suitable for hypertension, diabetes and other chronic disease follow-up scenarios. The system should pay special attention to recent indicators, current medication, adherence and newly developed discomfort, and can prioritize follow-up or report-interpretation pathways.',
       'chronic-followup,report,medication', dep.id, doc.id, 'operation', 1, 30, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.`code` = 'INTERNAL_MEDICINE' AND doc.`sort` = 20
  AND NOT EXISTS (
    SELECT 1 FROM `db_triage_knowledge`
    WHERE `knowledge_type` = 'doctor_profile' AND `title` = 'Zhou Yaning chronic care profile'
  );

INSERT INTO `db_triage_knowledge`
(`knowledge_type`, `title`, `content`, `tags`, `department_id`, `doctor_id`, `source_type`, `version`, `sort`, `status`, `create_time`, `update_time`)
SELECT 'case_reference', 'Rash with itching case summary',
       'A user reported recurrent facial rash with itching for one week and uploaded lesion photos. The triage flow should first supplement duration, affected area, self-medication history and exposure clues, then route the case into dermatology online consultation or follow-up review.',
       'dermatology,case-review,photo', dep.id, doc.id, 'case_review', 1, 40, 1, NOW(), NOW()
FROM `db_doctor` doc
JOIN `db_department` dep ON dep.id = doc.department_id
WHERE dep.`code` = 'DERMATOLOGY' AND doc.`sort` = 50
  AND NOT EXISTS (
    SELECT 1 FROM `db_triage_knowledge`
    WHERE `knowledge_type` = 'case_reference' AND `title` = 'Rash with itching case summary'
  );

INSERT INTO `db_triage_knowledge`
(`knowledge_type`, `title`, `content`, `tags`, `department_id`, `doctor_id`, `source_type`, `version`, `sort`, `status`, `create_time`, `update_time`)
SELECT 'service_notice', 'Report interpretation upload reminder',
       'Before report interpretation starts, users should be reminded to upload complete and clear report files, fill in the report date, mention the most concerning abnormal item and indicate whether symptoms are present. Better input quality directly improves later recommendation accuracy.',
       'report,upload,service-note', NULL, NULL, 'manual', 1, 50, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `db_triage_knowledge`
  WHERE `knowledge_type` = 'service_notice' AND `title` = 'Report interpretation upload reminder'
);

-- -------------------------
-- 16. Triage case reference demo data
-- -------------------------
INSERT INTO `db_triage_case_reference`
(`title`, `chief_complaint`, `symptom_summary`, `triage_result`, `department_id`, `doctor_id`, `risk_level`, `tags`, `source_type`, `sort`, `status`, `create_time`, `update_time`)
SELECT 'Chest pain with dyspnea offline triage case',
       'Chest pain for 2 hours with shortness of breath and sweating.',
       'The user reported worsening chest tightness, shortness of breath and obvious sweating. This scenario should first screen for acute cardiopulmonary risk and avoid continuing as a routine online consultation.',
       'Recommend immediate offline evaluation and prioritize emergency risk screening.',
       dep.id, doc.id, 'emergency', 'chest-pain,dyspnea,offline-first', 'case_review', 10, 1, NOW(), NOW()
FROM `db_department` dep
JOIN `db_doctor` doc ON doc.department_id = dep.id
WHERE dep.`code` = 'GENERAL_MEDICINE' AND doc.`sort` = 10
  AND NOT EXISTS (
    SELECT 1 FROM `db_triage_case_reference`
    WHERE `title` = 'Chest pain with dyspnea offline triage case'
  );

INSERT INTO `db_triage_case_reference`
(`title`, `chief_complaint`, `symptom_summary`, `triage_result`, `department_id`, `doctor_id`, `risk_level`, `tags`, `source_type`, `sort`, `status`, `create_time`, `update_time`)
SELECT 'Child high fever lethargy triage case',
       'High fever for 1 day with poor mental status.',
       'This pediatric case highlights that persistent fever combined with lethargy should trigger offline pediatric evaluation instead of simple home observation guidance.',
       'Recommend urgent offline pediatric assessment and continuous temperature observation.',
       dep.id, doc.id, 'high', 'pediatrics,fever,mental-status', 'doctor_summary', 20, 1, NOW(), NOW()
FROM `db_department` dep
JOIN `db_doctor` doc ON doc.department_id = dep.id
WHERE dep.`code` = 'PEDIATRICS' AND doc.`sort` = 30
  AND NOT EXISTS (
    SELECT 1 FROM `db_triage_case_reference`
    WHERE `title` = 'Child high fever lethargy triage case'
  );

INSERT INTO `db_triage_case_reference`
(`title`, `chief_complaint`, `symptom_summary`, `triage_result`, `department_id`, `doctor_id`, `risk_level`, `tags`, `source_type`, `sort`, `status`, `create_time`, `update_time`)
SELECT 'Chronic disease report follow-up case',
       'Blood pressure fluctuation with recent report upload.',
       'The user has a chronic disease history, uploaded a recent report and needs medication and indicator review. This kind of case is suitable for follow-up consultation rather than emergency routing.',
       'Recommend chronic follow-up review and report interpretation pathway.',
       dep.id, doc.id, 'medium', 'chronic-followup,report-review,medication', 'operation', 30, 1, NOW(), NOW()
FROM `db_department` dep
JOIN `db_doctor` doc ON doc.department_id = dep.id
WHERE dep.`code` = 'INTERNAL_MEDICINE' AND doc.`sort` = 20
  AND NOT EXISTS (
    SELECT 1 FROM `db_triage_case_reference`
    WHERE `title` = 'Chronic disease report follow-up case'
  );

INSERT INTO `db_triage_case_reference`
(`title`, `chief_complaint`, `symptom_summary`, `triage_result`, `department_id`, `doctor_id`, `risk_level`, `tags`, `source_type`, `sort`, `status`, `create_time`, `update_time`)
SELECT 'Facial rash photo upload triage case',
       'Recurrent facial rash with itching for one week.',
       'The user uploaded lesion photos and described repeated itching. The triage focus should supplement affected area, duration, self-medication and exposure history, then direct the case into dermatology consultation.',
       'Recommend dermatology online consultation and lesion-photo-assisted review.',
       dep.id, doc.id, 'medium', 'rash,photo,dermatology', 'case_review', 40, 1, NOW(), NOW()
FROM `db_department` dep
JOIN `db_doctor` doc ON doc.department_id = dep.id
WHERE dep.`code` = 'DERMATOLOGY' AND doc.`sort` = 50
  AND NOT EXISTS (
    SELECT 1 FROM `db_triage_case_reference`
    WHERE `title` = 'Facial rash photo upload triage case'
  );

-- -------------------------
-- 17. Doctor reply template demo data
-- -------------------------
INSERT INTO `db_doctor_reply_template`
(`doctor_id`, `scene_type`, `title`, `content`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'handle_summary', '通用病情判断',
       '结合当前主诉和已提交资料，暂未见明确急症指征，建议先按线上方案继续观察，并根据症状变化决定是否需要转线下就医。',
       10, 1, NOW(), NOW()
FROM `db_doctor` doc
WHERE doc.`name` IN ('林远航', '周雅宁', '陈知夏', '许清岚', '宋临川')
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_reply_template` t
    WHERE t.`doctor_id` = doc.id AND t.`scene_type` = 'handle_summary' AND t.`title` = '通用病情判断'
  );

INSERT INTO `db_doctor_reply_template`
(`doctor_id`, `scene_type`, `title`, `content`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'medical_advice', '常规观察建议',
       '建议近期保持规律作息、清淡饮食并记录症状变化；如症状持续加重、出现高热或明显不适，请尽快到线下门诊进一步评估。',
       20, 1, NOW(), NOW()
FROM `db_doctor` doc
WHERE doc.`name` IN ('林远航', '周雅宁', '陈知夏', '许清岚', '宋临川')
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_reply_template` t
    WHERE t.`doctor_id` = doc.id AND t.`scene_type` = 'medical_advice' AND t.`title` = '常规观察建议'
  );

INSERT INTO `db_doctor_reply_template`
(`doctor_id`, `scene_type`, `title`, `content`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'follow_up_plan', '三日复诊计划',
       '建议 3 天后再次复诊；若中途症状明显加重、体温持续升高或出现新的不适，请提前发起问诊或线下就医。',
       30, 1, NOW(), NOW()
FROM `db_doctor` doc
WHERE doc.`name` IN ('林远航', '周雅宁', '陈知夏', '许清岚', '宋临川')
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_reply_template` t
    WHERE t.`doctor_id` = doc.id AND t.`scene_type` = 'follow_up_plan' AND t.`title` = '三日复诊计划'
  );

INSERT INTO `db_doctor_reply_template`
(`doctor_id`, `scene_type`, `title`, `content`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'patient_instruction', '风险提示',
       '请继续观察体温、精神状态和主要不适变化；若出现持续高热、呼吸困难、明显嗜睡或疼痛加重，请立即前往线下医院就诊。',
       40, 1, NOW(), NOW()
FROM `db_doctor` doc
WHERE doc.`name` IN ('林远航', '周雅宁', '陈知夏', '许清岚', '宋临川')
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_reply_template` t
    WHERE t.`doctor_id` = doc.id AND t.`scene_type` = 'patient_instruction' AND t.`title` = '风险提示'
  );

INSERT INTO `db_doctor_reply_template`
(`doctor_id`, `scene_type`, `title`, `content`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'followup_summary', '症状缓解随访',
       '本次随访较前反馈显示主要不适已有缓解，当前整体状态趋于稳定，但仍建议继续观察近期变化。',
       50, 1, NOW(), NOW()
FROM `db_doctor` doc
WHERE doc.`name` IN ('林远航', '周雅宁', '陈知夏', '许清岚', '宋临川')
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_reply_template` t
    WHERE t.`doctor_id` = doc.id AND t.`scene_type` = 'followup_summary' AND t.`title` = '症状缓解随访'
  );

INSERT INTO `db_doctor_reply_template`
(`doctor_id`, `scene_type`, `title`, `content`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'followup_advice', '继续观察建议',
       '建议继续按既定方案观察，并做好体温、症状频次或相关指标记录；如再次明显加重，请及时复诊。',
       60, 1, NOW(), NOW()
FROM `db_doctor` doc
WHERE doc.`name` IN ('林远航', '周雅宁', '陈知夏', '许清岚', '宋临川')
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_reply_template` t
    WHERE t.`doctor_id` = doc.id AND t.`scene_type` = 'followup_advice' AND t.`title` = '继续观察建议'
  );

INSERT INTO `db_doctor_reply_template`
(`doctor_id`, `scene_type`, `title`, `content`, `sort`, `status`, `create_time`, `update_time`)
SELECT doc.id, 'followup_next_step', '再次随访安排',
       '建议 2 到 3 天后继续平台随访，如期间出现新发高风险症状，可提前联系医生或安排线下检查。',
       70, 1, NOW(), NOW()
FROM `db_doctor` doc
WHERE doc.`name` IN ('林远航', '周雅宁', '陈知夏', '许清岚', '宋临川')
  AND NOT EXISTS (
    SELECT 1 FROM `db_doctor_reply_template` t
    WHERE t.`doctor_id` = doc.id AND t.`scene_type` = 'followup_next_step' AND t.`title` = '再次随访安排'
  );

-- =========================================================
-- 导入完成后建议查看以下页面：
-- 1. 管理员 > 科室信息维护
-- 2. 管理员 > 医生信息维护
-- 3. 管理员 > 医生服务标签
-- 4. 管理员 > 医生排班管理
-- 5. 管理员 > 问诊分类管理
-- 6. 管理员 > 前置模板管理
-- 7. 管理员 > 身体部位字典
-- 8. 管理员 > 症状字典管理
-- 9. 管理员 > 分诊等级字典
-- 10. 管理员 > 红旗规则管理
-- 11. 网站首页 > 推荐医生展示
-- 说明：经典案例仍建议你手动上传真实封面图后再配置。
-- =========================================================
