# 2026-04-10 医生端字段级 AI 重写治理统计开发记录

## 1. 本轮目标

上一轮已经完成：

- 医生处理 / 随访草稿字段级 `AI重写`
- 后端按目标字段生成更聚焦的草稿
- 前端在字段工具条中直接触发局部重写

但当时还缺一层治理能力：

- 管理端无法区分“整份草稿生成”和“字段级重写”
- 表单 AI 日志里也没有记录本次到底重写了哪个字段

因此本轮重点是把字段级 `AI重写` 正式纳入留痕和管理端统计。

## 2. 本轮完成

### 2.1 表单 AI 日志新增字段重写标识

更新：

- `template-backend/src/main/java/cn/gugufish/entity/dto/DoctorFormAiLog.java`
- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-04-10-doctor-form-ai-regenerate-field.sql`

在 `db_doctor_form_ai_log` 中新增：

- `regenerate_field`

当医生触发整份草稿生成时：

- `regenerate_field = null`

当医生触发字段级 `AI重写` 时：

- 处理草稿会记录为：
  - `doctor_summary`
  - `medical_advice`
  - `follow_up_plan`
  - `patient_instruction`
- 随访草稿会记录为：
  - `followup_summary`
  - `followup_advice`
  - `followup_next_step`

这样后续就能直接在日志层区分：

- 整份草稿生成
- 字段级重写

### 2.2 后端日志写入和统计口径补齐

更新：

- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageOverviewVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageSceneVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageItemVO.java`

本轮在表单 AI 使用概览中新增统计：

- `fullGeneratedCount`
- `fieldRegenerateCount`

并同步补到场景维度：

- 医生处理
- 随访记录

最近记录项中也会直接带出：

- `regenerateField`

便于前端渲染“整份草稿”或“字段重写：处理建议”这类标识。

### 2.3 管理端新增字段重写可视化

更新：

- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

在“医生端 AI 处理 / 随访草稿使用”卡片中，现在新增了：

- 整份生成数
- 字段重写数

场景分布也从原来的：

- 生成 / 纯AI / 拼装 / 保存

扩展为：

- 生成 / 整份 / 重写 / 纯AI / 拼装 / 保存

最近记录中则会显示：

- `整份草稿`
- 或 `字段重写：判断摘要 / 处理建议 / 随访建议 ...`

这样管理员在看医生侧 AI 助理使用情况时，已经可以分清：

- 是在频繁重开整份草稿
- 还是更多在局部精修某些字段

## 3. 设计取舍

本轮没有单独新建“字段重写日志表”，而是继续复用：

- `db_doctor_form_ai_log`

这样做的原因是：

- 现有医生处理 / 随访草稿已经有完整生成、带入、保存链路
- 字段级重写本质上仍属于同一份表单草稿生成行为
- 复用原表可以继续沿用现有管理端统计和保存采纳分析

因此本轮是通过新增 `regenerate_field` 来扩展“生成语义”，而不是拆新模型。

## 4. 涉及文件

- `template-backend/src/main/java/cn/gugufish/entity/dto/DoctorFormAiLog.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageOverviewVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageSceneVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageItemVO.java`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`
- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-04-10-doctor-form-ai-regenerate-field.sql`

## 5. 验证

本轮已完成验证：

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 6. 下一步建议

建议下一轮继续优先推进：

1. 把字段级 `AI重写` 也细分到模板拼装采纳率分析里，识别“哪些字段最常被重写后再拼模板”
2. 在管理端补一个字段维度排行，直接看到最常被重写的是处理建议、患者指导还是下一步安排
3. 给字段级 `AI重写` 增加“保留当前草稿风格继续改写”的上下文，进一步降低非目标字段漂移
