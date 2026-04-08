# 2026-04-08 管理端 AI 导诊输出审计开发记录

## 1. 本轮目标

继续完善智能问诊系统后台的 AI 治理入口，在已完成“运行状态概览”的基础上，再补一层管理员可直接查看的最近 AI 导诊输出审计能力，解决以下问题：

- AI 是否真的产出了可复盘的导诊消息
- 当前 Prompt 版本和模型来源是否已经跟随消息一起留痕
- 管理员想快速抽查最近 AI 输出时，还需要手动翻数据库和问诊详情

## 2. 本轮完成

### 2.1 新增管理端最近 AI 输出审计接口

更新文件：

- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationAiController.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationAiAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationAiAuditItemVO.java`

本轮新增后台接口：

- `GET /api/admin/consultation-ai/audit-list`

接口支持：

- `messageType=all`
- `messageType=ai_triage_summary`
- `messageType=ai_followup_questions`
- `messageType=ai_chat_reply`

接口返回内容包括：

- 消息 ID、会话 ID、会话单号
- 问诊记录 ID、问诊单号
- 患者姓名、分类、科室
- 问诊状态、分诊等级、建议去向
- 消息类型、标题、正文、结构化内容
- 创建时间

实现方式上，直接复用了现有：

- `db_triage_message`
- `db_triage_session`
- `db_consultation_record`

这让管理端不需要额外新增复杂审计表，也能先具备“最近 AI 输出可视化抽查”的能力。

### 2.2 补齐 AI 继续追问消息的 Prompt 留痕

更新文件：

- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationTriageChatServiceImpl.java`

本轮把 AI 继续追问链路里生成的结构化消息也补齐了以下字段：

- `promptVersion`
- `source=deepseek`

补齐范围包括：

- `ai_chat_reply`
- `ai_followup_questions`

这样首轮 AI 导诊增强和后续患者补充追问两条链路，现在都能在消息留痕里看到当前 Prompt 版本和模型来源，后续做审计与版本对比会更顺。

### 2.3 管理端 AI 配置页新增最近 AI 输出审计面板

更新文件：

- `template-front/src/views/admin/ConsultationAiConfigPage.vue`
- `template-front/src/triage/insight.js`

本轮在 `/admin/consultation-ai` 页面新增了“最近 AI 导诊输出审计”区块，支持：

- 按消息类型筛选最近 AI 输出
- 展示患者、问诊单号、分类、时间等基础上下文
- 展示 AI 总结、AI 回复、追问建议
- 展示推荐科室、建议方式、风险标签、推荐医生、推荐依据
- 展示 `Prompt` 版本与 `DeepSeek` 来源标记
- 展示“建议医生接管”“建议尽快线下”等高风险提示

同时为了不影响患者端和医生端现有消息展示，本轮没有直接改写原有洞察逻辑，而是在 `triage/insight.js` 里新增了更适合后台审计使用的解析能力。

### 2.4 补齐审计面板到问诊详情的联动

更新文件：

- `template-front/src/views/admin/ConsultationAiConfigPage.vue`
- `template-front/src/views/admin/ConsultationRecordPage.vue`

本轮继续把 AI 审计页面从“能看”推进到“能直接复盘”：

- AI 审计面板新增“仅看高风险”筛选
- 高风险判定会结合以下信号：
  - 风险标签
  - 建议医生接管
  - 建议尽快线下
  - 建议方式为急诊或线下
- 每条审计消息新增“查看问诊详情”和“导诊记录中心”操作

同时把管理员问诊记录页补成了支持路由参数直接拉起详情弹窗：

- `/admin/consultation-record?detailId=123`

现在从 AI 审计页点击进入后，页面会自动打开对应问诊单详情；关闭弹窗后也会同步清理路由参数，避免后台导航状态和弹窗状态脱节。

### 2.5 在 AI 审计页直接展示医生复盘结果

更新文件：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationAiAuditItemVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

本轮继续把 AI 审计面板往“质控复盘”方向推进：

- 后端审计项现在会一并返回：
  - 医生处理留痕
  - 医生最终结构化结论
- 前端会在每条 AI 审计样本下直接展示“医生复盘”卡片

当前卡片里可以直接看到：

- 当前是否已有医生接手
- 医生是否已经形成最终结构化结论
- 医生标记为“与 AI 一致”还是“不一致”
- 医生最终病情等级、处理去向、随访安排
- 基于当前 AI 审计样本的字段级对比结果
- 医生填写的 AI 差异原因和差异说明

这样管理员在 AI 配置页上就能直接回答两个关键问题：

- 这条 AI 输出后来有没有被医生真正接手和复核
- 医生最终判断与当前 AI 建议是一致、部分可比，还是存在差异

### 2.6 新增高风险 AI 样本待复核队列

更新文件：

- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationAiController.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationAiAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

本轮继续把管理页从“样本抽查”推进到“待办治理”：

- 后端新增接口：
  - `GET /api/admin/consultation-ai/high-risk-review-queue`
- 队列会从最近 AI 导诊输出里筛出：
  - 命中高风险判定的样本
  - 且当前还没有医生最终结论，或医生已明确判定与 AI 不一致的样本

当前高风险判定会结合以下信号：

- 风险标签
- `shouldEscalateToHuman=1`
- `suggestOfflineImmediately=1`
- 建议方式为 `emergency / offline`

前端页面新增“高风险待复核队列”区块，能够直接看到：

- 当前待复核总量
- 待医生接手数量
- 待形成最终结论数量
- 医生已判定与 AI 存在差异的数量

并且每条队列项都可以直接看到：

- 当前高风险信号
- 当前 AI 判断摘要
- 医生接手 / 结论沉淀进度
- 已存在的对比结果
- 一键进入问诊详情继续复盘

这样管理员现在打开 AI 配置页，就能先盯“最需要人工看”的高风险样本，而不是先从全部 AI 输出里手动翻。

## 3. 涉及文件

### 后端

- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationAiController.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationAiAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationAiAuditItemVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationTriageChatServiceImpl.java`

### 前端

- `template-front/src/views/admin/ConsultationAiConfigPage.vue`
- `template-front/src/views/admin/ConsultationRecordPage.vue`
- `template-front/src/triage/insight.js`

### 文档

- `docs/2026-04-08-admin-ai-audit-log.md`

## 4. 价值

- 管理员现在不仅能看 AI 是否可用，还能直接看到最近 AI 实际输出了什么
- Prompt 版本与模型来源已经进入导诊消息留痕，后续更容易做版本回溯和效果复盘
- AI 配置页开始从“运行状态页”往“轻量审计中心”演进，为后续扩展 AI 偏差样本、Prompt 对比和人工复核入口打下基础
- 审计面板已经和问诊详情打通，后台复盘路径从“看到异常”缩短到了“直接点开对应问诊”
- 审计样本已经能直接看到医生处理结果，管理员不必频繁在多个页面之间来回切换
- 高风险样本已经被聚合成独立待复核队列，AI 治理入口开始具备轻量待办中心能力

## 5. 验证

本轮已完成验证：

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 6. 下一步建议

建议下一轮继续沿着这个治理入口推进以下方向之一：

1. 增加 Prompt 版本切换后的样本对照区
2. 支持按指定问诊单号、患者或时间范围筛选 AI 审计样本
3. 增加 AI 审计样本导出与人工复核备注能力
4. 为高风险待复核队列增加运营状态流转和处理人归档
