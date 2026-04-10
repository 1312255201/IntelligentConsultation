# 2026-04-09 医生处理 / 随访 AI 草稿留痕开发记录

## 1. 本轮目标

在上一轮已经补齐：

- 医生处理 AI 草稿
- 医生随访 AI 草稿

本轮继续把这两类能力从“能生成”推进到“可治理、可统计、可回看”。

核心目标是补齐下面这条链路：

- 生成
- 带入
- 保存

这样后续才能回答：

- 医生处理草稿到底生成了多少次
- 生成后有多少真的被带入表单
- 最终有多少真的保存成医生处理或随访记录
- 哪类场景更容易被医生采纳

## 2. SQL 变更

新增表：

- `db_doctor_form_ai_log`

同步更新：

- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-04-09-doctor-form-ai-log.sql`

核心字段包括：

- `consultation_id / consultation_no / patient_name / category_name`
- `doctor_id / doctor_name / department_id / department_name`
- `scene_type`
  - `handle`
  - `follow_up`
- `source / prompt_version / fallback`
- `risk_flags_json`
- `draft_summary / draft_preview / draft_payload_json`
- `apply_count / last_apply_target`
- `saved_status / saved_target / saved_preview / saved_payload_json`
- `generated_time / last_apply_time / saved_time`

这张表承接医生端表单草稿从生成到保存的完整留痕。

## 3. 后端能力

### 3.1 处理 / 随访 AI 草稿生成时自动落日志

更新：

- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

本轮在两个已有生成接口上继续补齐日志写入：

- `POST /api/doctor/consultation/handle/ai-draft`
- `POST /api/doctor/consultation/follow-up/ai-draft`

现在生成后会自动落一条日志，并在返回值里带上：

- `logId`

对应返回对象也新增了：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorConsultationHandleDraftVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorConsultationFollowUpDraftVO.java`

### 3.2 新增表单 AI 草稿带入留痕接口

新增：

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationFormDraftApplyVO.java`

更新：

- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorWorkspaceService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

新增接口：

- `POST /api/doctor/consultation/form/ai-draft/apply`

支持记录的带入目标：

- `handle_form`
- `conclusion_form`
- `follow_up_form`

### 3.3 医生提交处理 / 随访时自动回写“已保存采纳”

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationHandleSubmitVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationFollowUpSubmitVO.java`

两个提交对象都新增可选字段：

- `aiLogId`

当医生最终保存：

- 医生处理结果
- 随访记录

后端会自动回写：

- 已保存状态
- 保存目标
- 保存内容摘要
- 保存时间

## 4. 管理端能力

新增返回对象：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageOverviewVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageSceneVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageItemVO.java`

更新：

- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationAiController.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationAiAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`

新增接口：

- `GET /api/admin/consultation-ai/doctor-form-usage`

当前输出内容包括：

- 累计生成
- 已带入
- 已保存
- DeepSeek 生成数
- 规则兜底数
- 带入率
- 保存采纳率
- 场景维度统计
- 最近草稿使用记录

## 5. 前端能力

更新：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

### 5.1 医生问诊页新增表单草稿使用状态绑定

本轮在医生页里新增了两套使用状态：

- `handleAiUsage`
- `followUpAiUsage`

现在会在这些动作发生时自动留痕：

- 生成处理草稿 / 随访草稿
  - 后端返回 `logId`
- 带入处理表单
  - 记录 `handle_form`
- 带入结构化结论
  - 记录 `conclusion_form`
- 带入随访表单
  - 记录 `follow_up_form`
- 提交医生处理 / 提交随访记录
  - 若当前表单来自已带入 AI 草稿，则回写为“已保存采纳”

### 5.2 管理端新增医生表单草稿使用概览

现在管理端 AI 治理页除了“医生端 AI 沟通草稿使用”之外，又新增了：

- 医生端 AI 处理 / 随访草稿使用

管理员可以直接看到：

- 处理和随访草稿累计生成数
- 带入数
- 保存数
- DeepSeek / 规则兜底分布
- 医生最近的草稿采纳记录

## 6. 本轮涉及文件

### 后端

- `template-backend/src/main/java/cn/gugufish/entity/dto/DoctorFormAiLog.java`
- `template-backend/src/main/java/cn/gugufish/mapper/DoctorFormAiLogMapper.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationFormDraftApplyVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationHandleSubmitVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationFollowUpSubmitVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageOverviewVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageSceneVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageItemVO.java`
- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java`
- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationAiController.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorWorkspaceService.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationAiAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`

### 前端

- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

### SQL

- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-04-09-doctor-form-ai-log.sql`

## 7. 验证

本轮已完成验证：

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 8. 下一步建议

建议下一轮优先继续往以下方向之一推进：

1. 给医生处理 / 随访 AI 草稿补“按医生、按科室、按场景”的排行统计
2. 把医生模板系统继续接到处理草稿和随访草稿，支持 `AI + 模板` 组合带入
3. 在医生工作台首页新增“高采纳 AI 草稿场景”或“待优先处理 AI 推荐”视图
4. 对已保存采纳的草稿做回流分析，沉淀更高质量的 Prompt 和模板策略
