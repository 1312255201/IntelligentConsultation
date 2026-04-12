# 2026-04-12 提醒状态一致性与恢复更新语义补齐记录

## 本次目标

围绕在线问诊主流程，继续补齐“患者随访提醒中心 / 患者总览 / 医生待办中心 / 医生工作台”之间的状态一致性问题，重点解决以下两类偏差：

- 已完成随访的问诊，部分页面仍被当作“待随访”
- 患者发送恢复更新后，医生侧部分列表仍只显示为普通新消息

## 已完成内容

### 1. 患者端随访状态统一

- 更新 [triage/reminder.js](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-front/src/triage/reminder.js)
- 当最近一条随访记录 `needRevisit === 0` 时，统一返回 `done`
- 补齐 `done` 状态下的标签、提示文案、时间线文案
- 服务评价待办判断改为同时兼容 `none` 与 `done`

### 2. 患者提醒页与总览页同步旧逻辑

- 更新 [OverviewPage.vue](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-front/src/views/index/OverviewPage.vue)
- 更新 [ConsultationReminderPage.vue](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-front/src/views/index/ConsultationReminderPage.vue)
- 这两个页面原先保留了一份旧版 `followUpState` 逻辑，本次已同步为：
  - 已完成随访不再计入待随访数量
  - 若已完成随访但仍待服务评价，页面会正确落到“待服务评价”分支

### 3. 医生端恢复更新语义增强

- 更新 [doctor/reminder.js](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-front/src/doctor/reminder.js)
- 新增 `isFollowUpUpdateMessageType`
- 医生侧消息进度标签支持区分：
  - `患者恢复更新`
  - `待跟进恢复更新`
  - 普通 `患者新消息`
- 新增 `doctorMessagePreview`，用于在列表场景中为恢复更新自动补 `[恢复更新]` 前缀

### 4. 医生提醒页与工作台联动增强

- 更新 [DoctorReminderPage.vue](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-front/src/views/doctor/DoctorReminderPage.vue)
- 更新 [DoctorWorkbenchPage.vue](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-front/src/views/doctor/DoctorWorkbenchPage.vue)
- 医生提醒流、工作台卡片、最近沟通摘要现在都能更明确识别恢复更新消息
- 工作台本地随访状态也补齐 `done`，避免完成后仍展示成待随访

### 5. 后端消息预览补前缀

- 更新 [ConsultationMessageServiceImpl.java](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-backend/src/main/java/cn/gugufish/service/impl/ConsultationMessageServiceImpl.java)
- 当消息类型为 `followup_update` 时，统一在摘要预览前补 `[恢复更新]`
- 这样医生端列表、工作台、提醒流使用同一份后端摘要时可以直接受益

### 6. 恢复更新文案回归中文风格

- 更新 [ConsultationPage.vue](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-front/src/views/index/ConsultationPage.vue)
- 更新 [DoctorConsultationPage.vue](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-front/src/views/doctor/DoctorConsultationPage.vue)
- 将恢复更新面板、标签、按钮与摘要前缀从临时英文统一回中文

## 当前效果

- 已完成随访的问诊不会再被患者提醒中心、患者总览误判为待随访
- 完成随访后若仍待服务评价，会正确进入服务评价提醒流
- 患者补充恢复更新后，医生端能在更多入口直接看出这是“恢复更新”而不是普通聊天
- 前后端对恢复更新摘要的展示语义更统一

## 验证

- 前端：`cmd /c npm run build` 通过
- 后端：`"C:\Users\13122\AppData\Local\Programs\IntelliJ IDEA Ultimate\plugins\maven\lib\maven3\bin\mvn.cmd" -q -DskipTests compile` 通过

## 后续建议

1. 继续清理患者端与医生端页面中剩余的历史英文占位文案，统一项目文案风格
2. 继续收敛前端重复的提醒判断函数，逐步改为复用 `triage/reminder.js` 与 `doctor/reminder.js`
3. 在在线问诊主流程继续补“处方 / 检查建议 / 问诊结果归档 / 患者评价回看”的闭环能力
