# 2026-04-12 服务评价表结构兼容修复记录

## 问题背景

- 运行问诊列表、医生工作台或服务评价相关接口时，数据库报错：
  - `Unknown column 'doctor_handle_status' in 'field list'`
- 原因是代码已经接入了 2026-04-11 新增的服务评价处理字段，但现有 MySQL 库没有执行对应升级脚本。

## 本次修复

- 后端新增启动期数据库结构自检与补齐逻辑。
- 应用启动时会检查表 `db_consultation_service_feedback` 是否缺少以下字段：
  - `doctor_handle_status`
  - `doctor_handle_remark`
  - `doctor_handle_account_id`
  - `doctor_handle_doctor_id`
  - `doctor_handle_doctor_name`
  - `doctor_handle_time`
- 若字段缺失，会自动执行 `ALTER TABLE` 补齐。
- 若相关索引 `idx_consultation_service_feedback_doctor_handle` 缺失，也会自动补齐。

## 涉及文件

- `template-backend/src/main/java/cn/gugufish/config/ConsultationSchemaUpgradeRunner.java`
- `sql/mysql57-upgrade-2026-04-11-doctor-service-feedback-handle.sql`

## 兼容策略

- 优先自动修复老库，避免应用启动后再在接口运行阶段暴露 SQL 语法错误。
- 如果数据库账号没有 `ALTER` 权限，启动时会直接抛出更明确的异常，提示手动执行升级脚本，而不是等到接口访问时才出现难定位错误。

## 手动升级兜底

- 如需手动执行，可运行：
  - `sql/mysql57-upgrade-2026-04-11-doctor-service-feedback-handle.sql`

## 后续建议

1. 后续新增问诊表字段时，继续保留独立升级 SQL，并为关键表增加启动期兼容检查。
2. 如果后面数据库升级点继续变多，可以把这类升级器沉淀成统一的 schema migration 模块。
