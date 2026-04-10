# 2026-04-09 医生端 AI 沟通草稿使用留痕开发记录

## 1. 本轮目标

继续完善智能问诊系统中的医生侧 AI 助理链路，不只让医生“能生成草稿”，还要能回答下面几个治理问题：

- 医生端 AI 沟通草稿一共生成了多少次
- 生成后有多少真正被医生带入输入框
- 最终有多少真的被发送给患者
- 哪些场景更常被采纳
- AI 草稿和医生模板的拼装使用是否正在发生

本轮因此聚焦补齐“生成 -> 带入 -> 发送”的留痕链路，并把统计接入管理端 AI 治理页面。

## 2. SQL 变更

### 2.1 新增医生端 AI 沟通草稿日志表

新增：

- `db_doctor_message_ai_log`

同步更新：

- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-04-09-doctor-message-ai-log.sql`

核心字段包括：

- `consultation_id / consultation_no / patient_name / category_name`
- `doctor_id / doctor_name / department_id / department_name`
- `scene_type`
- `source / prompt_version / fallback`
- `risk_flags_json / draft_summary / draft_content`
- `apply_count / last_apply_mode`
- `template_scene_type / template_id / template_title / template_used`
- `sent_status / sent_message_id / sent_content_preview`
- `generated_time / last_apply_time / sent_time`

这张表用来承接医生侧 AI 沟通草稿的全链路行为留痕。

## 3. 后端能力

### 3.1 医生端新增 AI 草稿带入留痕接口

新增：

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationMessageDraftApplyVO.java`

更新：

- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorWorkspaceService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

新增接口：

- `POST /api/doctor/consultation/message/ai-draft/apply`

作用：

- 当医生把 AI 草稿带入输入框时记录一次“带入”
- 支持记录带入方式：
  - `replace`
  - `append`
  - `compose`
- 支持记录本次是否同时拼装了沟通模板

### 3.2 AI 草稿生成时自动落日志

医生生成沟通草稿后，后端现在会立即写入一条 AI 草稿生成日志，并在返回结果里带上：

- `logId`

前端后续带入和发送时，会基于这条 `logId` 继续补齐行为链路。

### 3.3 医生发送消息时自动补齐“已发送采纳”

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/ConsultationMessageSendVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationMessageServiceImpl.java`

新增可选字段：

- `aiLogId`

如果本次发送消息来自已带入的 AI 草稿，后端会自动回写：

- 已发送状态
- 发送消息 ID
- 实际发送内容预览
- 发送时间

这样后续就能在后台直接看到真正被医生采纳并发给患者的 AI 沟通草稿。

### 3.4 管理端新增医生侧 AI 使用概览接口

新增返回对象：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorMessageAiUsageOverviewVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorMessageAiUsageSceneVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorMessageAiUsageItemVO.java`

更新：

- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationAiController.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationAiAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`

新增接口：

- `GET /api/admin/consultation-ai/doctor-message-usage`

当前输出内容包括：

- 生成总数
- 已带入总数
- 已发送总数
- 模板拼装总数
- DeepSeek 生成数
- 规则兜底数
- 带入率
- 发送采纳率
- 场景维度统计
- 最近医生侧 AI 草稿使用记录

## 4. 前端能力

### 4.1 医生问诊页接入 AI 使用留痕

更新：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

本轮在页面内新增了一层“与输入框绑定”的 AI 使用状态，用来避免以下错误统计：

- 医生生成了新草稿但没带入，却被误算成已使用
- 医生先带入旧草稿，再生成新草稿，发送时误记到新草稿

现在页面会在这些动作发生时自动留痕：

- `AI 生成建议`
  - 后端生成日志并返回 `logId`
- `覆盖带入 / 追加带入`
  - 记录一次带入行为
- `AI+模板合成带入`
  - 记录一次 AI + 模板拼装带入
- `发送消息`
  - 若当前输入框内容来自已带入的 AI 草稿，则回写为“已发送采纳”

### 4.2 管理端 AI 治理页新增医生侧使用概览

更新：

- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

页面现在新增“医生端 AI 沟通草稿使用”区块，支持直接查看：

- 累计生成
- 已带入
- 已发送
- 模板拼装
- DeepSeek 生成
- 规则兜底
- 各场景生成/带入/发送分布
- 最近使用记录

这样管理员不需要再开新页，就能在现有 AI 治理入口里同时看到：

- 患者侧 AI 导诊产出
- 高风险待复核队列
- 医生侧 AI 沟通草稿真实使用情况

## 5. 本轮涉及文件

### 后端

- `template-backend/src/main/java/cn/gugufish/entity/dto/DoctorMessageAiLog.java`
- `template-backend/src/main/java/cn/gugufish/mapper/DoctorMessageAiLogMapper.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationMessageDraftApplyVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/ConsultationMessageSendVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorConsultationMessageDraftVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorMessageAiUsageOverviewVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorMessageAiUsageSceneVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorMessageAiUsageItemVO.java`
- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java`
- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationAiController.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorWorkspaceService.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationAiAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationMessageServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`

### 前端

- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

### SQL

- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-04-09-doctor-message-ai-log.sql`

## 6. 验证结果

本轮已完成验证：

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 7. 当前阶段价值

到这里，医生端 AI 沟通草稿已经不再只是“能生成一段文本”，而是开始具备真正可治理、可复盘、可持续优化的数据基础：

- 可以看医生到底有没有用
- 可以看哪些场景更容易被采纳
- 可以看模板拼装是否在提升发送落地率
- 可以把真实发送链路和 Prompt / 模板策略连接起来

## 8. 下一步建议

建议下一轮优先继续往以下方向之一推进：

1. 给医生处理表单和随访表单补独立 AI 草稿，并沿用同一套使用留痕
2. 在管理端增加医生侧 AI 草稿按医生、科室、场景的细分排行榜
3. 对“已发送采纳”的草稿做质量回流，沉淀高采纳模板和高采纳 Prompt 版本
4. 在医生端增加“最近高采纳草稿/模板推荐”，形成闭环优化
