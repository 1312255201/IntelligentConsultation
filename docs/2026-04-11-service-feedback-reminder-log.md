# 2026-04-11 问诊服务评价提醒闭环记录
## 本轮目标

在已经支持患者提交问诊服务评价的基础上，继续补齐“已完成问诊 -> 待患者评价”的提醒闭环，让患者能够更自然地完成最后一步反馈。

本轮仍然优先完善在线问诊主流程，不继续扩展新的 AI 能力。

## 本轮完成

- 问诊记录列表接口补充返回 `serviceFeedback`
- 患者问诊页新增“待服务评价”提醒卡片
- 患者提醒中心新增“待服务评价”提醒分组与筛选
- 患者工作台概览新增“待服务评价”统计与待办识别
- 患者可从提醒中心、工作台、问诊页直接定位到评价表单区域
- 左侧患者提醒角标统计会自动包含待服务评价记录

## 后端改动

更新文件：

- `template-backend/src/main/java/cn/gugufish/service/ConsultationServiceFeedbackQueryService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceFeedbackQueryServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceImpl.java`

实现说明：

- 新增按问诊记录批量查询服务评价的能力
- 在患者问诊记录列表接口中补齐 `serviceFeedback` 字段
- 前端提醒、概览、问诊记录页都可以基于同一份列表数据判断是否“待评价”

## 前端改动

更新文件：

- `template-front/src/triage/reminder.js`
- `template-front/src/views/index/ConsultationPage.vue`
- `template-front/src/views/index/ConsultationReminderPage.vue`
- `template-front/src/views/index/OverviewPage.vue`

实现说明：

- 公共提醒判断中新增“已完成但未提交服务评价”的识别逻辑
- 问诊记录页支持按 `pending_feedback` 快速筛选
- 页面路由动作新增 `action=feedback`，可直接聚焦到服务评价表单
- 提醒中心支持单独筛选“待评价”记录
- 工作台待办和最近问诊中会展示“待服务评价”标签

## 用户价值

- 患者在医生完成处理后，不会错过最后一步服务反馈
- 问诊闭环从“处理完成”进一步延伸到“评价留存”
- 平台后续可以在此基础上继续做催评、评价统计和服务质量分析

## 下一步建议

- 增加“完成后 X 天未评价”的催评提醒策略
- 增加医生端与管理端的服务评价统计看板
- 增加低分评价的重点巡检与回访处理能力
