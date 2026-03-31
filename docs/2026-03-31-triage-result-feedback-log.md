# 2026-03-31 导诊结果归档与反馈纠错开发记录

## 本次目标

在已经完成的“导诊会话 / 消息留痕”基础上，继续补齐两个关键能力：

- 导诊结果归档
- 用户反馈与纠错

这样系统就不仅能记录“过程”，还能记录“最终结果”和“用户是否认可结果”，为后续 AI 导诊优化提供真实反馈数据。

## 本次完成内容

### 1. 数据库结构

在 `sql/mysql57-init.sql` 中新增：

- `db_triage_result`
  - 保存一次导诊的归档结果
  - 包含分诊等级、推荐科室、推荐医生、候选医生列表、结果说明、风险摘要、症状提取摘要、置信度等
- `db_triage_feedback`
  - 保存用户对导诊结果的反馈
  - 包含评分、是否采纳、反馈文本、修正科室、修正医生等

### 2. 后端能力

新增后端对象：

- `TriageResult`
- `TriageFeedback`
- `TriageResultMapper`
- `TriageFeedbackMapper`
- `TriageResultVO`
- `TriageFeedbackVO`
- `ConsultationTriageFeedbackSubmitVO`
- `ConsultationFeedbackOptionsVO`
- `ConsultationFeedbackDepartmentOptionVO`
- `ConsultationFeedbackDoctorOptionVO`

新增服务：

- `TriageResultQueryService`
- `TriageFeedbackQueryService`
- `TriageFeedbackService`
- 对应实现类

并完成以下接入：

- 用户提交问诊后，在原有导诊会话生成成功后，自动生成一条 `db_triage_result`
- 用户端问诊详情接口新增：
  - `triageResult`
  - `triageFeedback`
- 管理端问诊详情接口新增：
  - `triageResult`
  - `triageFeedback`
- 新增用户接口：
  - `GET /api/user/consultation/feedback/options`
  - `POST /api/user/consultation/feedback/submit`

### 3. 用户端页面

在 `ConsultationPage.vue` 的问诊详情弹窗中新增：

- 导诊结果归档展示
  - 分诊等级
  - 推荐科室
  - 推荐医生
  - 置信度
  - 结果说明
  - 风险标签
  - 候选医生列表
- 导诊反馈表单
  - 用户评分
  - 是否采纳
  - 反馈说明
  - 修正科室
  - 修正医生

支持首次提交和再次修改反馈。

### 4. 管理端页面

在 `ConsultationRecordPage.vue` 的详情弹窗中新增：

- 导诊结果归档查看区
- 用户反馈查看区

管理员现在可以在一个详情弹窗里同时查看：

- 原始问诊资料
- 规则命中日志
- 导诊会话留痕
- 导诊结果归档
- 用户反馈与纠错

## 设计说明

### 为什么这一步很重要

如果没有结果归档和用户反馈，系统只能做到：

- 知道系统当时怎么想的
- 不知道系统最后给了什么结果
- 更不知道用户是否认可这个结果

而后续 AI 导诊要做效果优化，最有价值的数据恰恰是：

- 哪些推荐被用户采纳了
- 哪些推荐被纠正了
- 哪些科室 / 医生推荐经常被人工改掉

所以这一层是后续“AI 纠偏训练数据”的基础。

### 当前版本定位

当前这一版仍然属于“AI 导诊前置基础设施”阶段：

- 已有问诊前置采集
- 已有规则分诊
- 已有会话留痕
- 已有结果归档
- 已有用户反馈

但还没有真正做：

- AI 多轮追问
- AI 动态重算结果
- 管理端反馈统计分析页

## 本次验证

已完成验证：

- 后端：`mvn -q -DskipTests compile`
- 前端：`npm run build`

均通过。

## 下一步建议

建议下一轮继续做：

1. 管理端导诊效果分析页
   - 统计采纳率、低分反馈、人工纠错分布
2. 导诊结果多版本
   - 支持规则结果、AI 结果、人工修订结果并存
3. AI 多轮追问
   - 在已有 `session + message + result + feedback` 基础上继续扩展

## 相关文件

- `sql/mysql57-init.sql`
- `template-backend/src/main/java/cn/gugufish/controller/ConsultationController.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/TriageResultQueryServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/TriageFeedbackQueryServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/TriageFeedbackServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/entity/dto/TriageResult.java`
- `template-backend/src/main/java/cn/gugufish/entity/dto/TriageFeedback.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/ConsultationTriageFeedbackSubmitVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/TriageResultVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/TriageFeedbackVO.java`
- `template-front/src/views/index/ConsultationPage.vue`
- `template-front/src/views/admin/ConsultationRecordPage.vue`
