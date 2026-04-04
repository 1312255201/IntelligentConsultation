# 2026-04-04 AI 导诊多轮会话开发记录

## 1. 本轮目标

在已经完成 `Spring AI + DeepSeek` 基础接入的前提下，继续把患者侧导诊能力从“提交后补充一条 AI 建议”推进为“可继续与 AI 导诊交互”的最小可用版本。

本轮重点不是重做整套问诊页面，而是在现有问诊详情弹窗中，直接把导诊留痕区扩展成可继续补充症状和接收 AI 回复的会话区域。

## 2. 后端能力

### 2.1 新增患者侧 AI 导诊会话接口

新增接口：

- `POST /api/user/consultation/triage/message/send`

用途：

- 患者在已提交问诊后，可继续发送补充说明给 AI 导诊
- 系统会基于已有问诊答案、导诊留痕、候选医生和本次补充内容继续生成回复

### 2.2 新增请求对象

新增：

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/ConsultationTriageMessageSendVO.java`

字段：

- `recordId`
- `content`

约束：

- 记录 ID 必须为正整数
- 内容不能为空
- 内容长度不超过 1000

### 2.3 新增导诊会话服务

新增：

- `template-backend/src/main/java/cn/gugufish/service/ConsultationTriageChatService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationTriageChatServiceImpl.java`

本轮实现逻辑：

1. 校验问诊记录归属当前用户
2. 校验当前问诊已经生成导诊会话
3. 读取已有导诊消息、问诊答案和候选医生
4. 基于当前补充内容继续调用 DeepSeek
5. 将患者补充消息写入 `db_triage_message`
6. 将 AI 回复和 AI 建议补充问题继续写入 `db_triage_message`
7. 更新 `db_triage_session.message_count`

### 2.4 扩展 AI 服务抽象

更新：

- `template-backend/src/main/java/cn/gugufish/ai/AiTriageAdvice.java`
- `template-backend/src/main/java/cn/gugufish/ai/AiTriageContext.java`
- `template-backend/src/main/java/cn/gugufish/service/AiTriageService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/AiTriageServiceImpl.java`

本轮新增能力：

- 支持继续导诊会话的上下文输入
- 支持输出 `reply`
- 支持基于导诊历史消息继续生成 AI 回复

### 2.5 导诊消息类型扩展

本轮新增消息类型：

- `ai_user_followup`
- `ai_chat_reply`
- `ai_followup_questions`

这样后续患者侧、医生侧、管理端都可以继续复用统一的导诊留痕结构，不需要额外新建独立聊天表。

## 3. 前端能力

更新页面：

- `template-front/src/views/index/ConsultationPage.vue`

本轮新增效果：

- 在“导诊留痕”区域下方增加“继续 AI 导诊”输入框
- 患者可以继续输入症状变化、持续时间、体温、检查结果等内容
- 发送成功后自动刷新当前问诊详情
- 新的患者补充消息和 AI 回复会直接出现在导诊留痕列表中

### 3.1 本轮没有改动的部分

- 医患沟通区仍然保留原有独立消息流
- 医生侧暂未增加单独的 AI 导诊会话操作区
- 本轮先支持文本补充，不支持在 AI 导诊区上传附件

## 4. 本轮涉及文件

### 文档

- `docs/2026-04-04-ai-consultation-roadmap.md`
- `docs/2026-04-04-ai-triage-chat-log.md`

### 后端

- `template-backend/pom.xml`
- `template-backend/src/main/resources/application.yml`
- `template-backend/src/main/java/cn/gugufish/controller/ConsultationController.java`
- `template-backend/src/main/java/cn/gugufish/ai/AiTriageAdvice.java`
- `template-backend/src/main/java/cn/gugufish/ai/AiTriageContext.java`
- `template-backend/src/main/java/cn/gugufish/ai/AiTriageProperties.java`
- `template-backend/src/main/java/cn/gugufish/service/AiTriageService.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationAiEnrichmentService.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationTriageChatService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/AiTriageServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiEnrichmentServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationTriageChatServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/ConsultationTriageMessageSendVO.java`

### 前端

- `template-front/src/views/index/ConsultationPage.vue`

## 5. 验证结果

本轮已完成验证：

- 后端执行 `mvn -q -DskipTests compile` 通过
- 前端执行 `npm run build` 通过

## 6. 当前阶段结论

到这里，患者侧的 AI 导诊已经从“单次生成”进入“可继续补充并追问”的阶段，虽然还不是独立的完整 AI 对话页面，但已经具备了真实多轮导诊的核心链路：

1. 患者提交问诊
2. 规则分诊生成初步结果
3. AI 给出首轮导诊建议
4. 患者继续补充情况
5. AI 继续追问和更新建议
6. 整个过程完整留痕，后续医生可复用

## 7. 下一步建议

建议下一步优先继续推进以下方向之一：

1. 把患者侧 AI 导诊从“详情弹窗内继续交互”升级为独立的多轮导诊页面
2. 在医生侧问诊详情中展示更完整的 AI 导诊会话和 AI 摘要解释
3. 开始把 AI 对候选医生的排序解释接入医生推荐链路

如果继续沿当前主线推进，最合理的下一步是：

- 做患者侧独立 AI 导诊页
- 把“问诊前表单 + AI 多轮对话 + 导诊结果”串成一条更完整的主链路
