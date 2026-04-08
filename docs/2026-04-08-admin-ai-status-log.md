# 2026-04-08 管理端 AI 导诊配置与状态页开发记录

## 1. 本轮目标

继续完善智能问诊系统的 AI 治理入口，先补齐一个管理员可直接查看的 AI 导诊运行概览页，解决以下问题：

- 当前环境是否真的启用了 `Spring AI + DeepSeek`
- AI 导诊不可用时，后台缺少一个能快速定位问题的入口
- Prompt 版本、模型参数和导诊产出数量分散在代码与数据库中，不利于持续运营

## 2. 本轮完成

### 2.1 新增管理端 AI 导诊运行概览接口

更新文件：

- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationAiController.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationAiAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationAiOverviewVO.java`

本轮新增了后台接口：

- `GET /api/admin/consultation-ai/overview`

接口返回内容包括：

- AI 导诊总开关状态
- DeepSeek Chat 接入开关
- API Key 是否已配置
- `DeepSeekChatModel` Bean 是否成功装配
- 当前模型参数与 Prompt 版本
- 当前问诊、导诊会话、导诊结果数量
- AI 导诊总结、AI 追问建议、AI 对话回复、患者补充追问数量
- 最近一次 AI 导诊输出时间
- 当前运行状态说明与告警列表

这样管理员不用翻环境变量、启动日志和数据库，也能快速判断 AI 链路现在处于“已运行”还是“规则兜底”。

### 2.2 新增管理端“AI 导诊配置”页面

更新文件：

- `template-front/src/views/admin/ConsultationAiConfigPage.vue`
- `template-front/src/router/index.js`
- `template-front/src/views/AdminView.vue`

本轮新增后台页面：

- 路由：`/admin/consultation-ai`

页面内容包括：

- AI 导诊运行状态总览
- DeepSeek 接入状态与核心检查项
- 当前 Prompt 版本、模型、温度、Token、候选医生上限
- AI 导诊产出统计看板
- 运行告警列表
- 环境变量检查提示与下一步治理建议

同时已把该页面挂入管理员左侧导航，作为当前 AI 治理入口的第一版承接页。

## 3. 涉及文件

- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationAiController.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationAiAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationAiOverviewVO.java`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`
- `template-front/src/views/AdminView.vue`
- `template-front/src/router/index.js`
- `docs/2026-04-08-admin-ai-status-log.md`

## 4. 价值

- 补上了 AI 导诊“是否真正可用”的后台可见性
- 为后续扩展 Prompt 版本管理、模型参数治理和 AI 审计中心留出了自然入口
- 让当前 `Spring AI + DeepSeek` 接入不再只是代码层能力，而是开始具备运维和运营视角

## 5. 验证

- 前端执行 `npm run build`
- 后端执行 `mvn -q -DskipTests compile`
