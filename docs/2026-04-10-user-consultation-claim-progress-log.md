# 2026-04-10 患者侧问诊接诊进度补齐记录

## 本轮目标

围绕“先把主要在线问诊主流程跑顺”继续补齐患者侧体验，重点解决下面这类主流程问题：

- 医生已经认领问诊单，患者侧仍可能显示“待医生处理”
- 医生发送过消息且患者已读后，问诊进度又退回到等待状态
- 患者详情里能看到智能分配，但缺少清晰的“谁已接诊 / 何时认领”信息

这一轮不继续扩展 AI，而是优先把非 AI 的在线问诊闭环做扎实。

## 本轮完成

- 后端患者问诊记录聚合补充 `doctorAssignment`
- 患者问诊列表的进度判断补充“医生已认领 / 已有医生消息”识别
- 患者问诊详情的留言提示补充当前跟进医生
- 患者问诊详情时间线新增“医生已认领”节点
- 患者提醒中心与首页总览同步修正同一套进度判断

## 后端改动

更新文件：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationRecordVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceImpl.java`

改动说明：

- `ConsultationRecordVO` 新增 `doctorAssignment`
- `listRecords(...)` 现在会把最新认领记录一并返回给患者侧列表
- `recordDetail(...)` 现在会把最新认领记录一并返回给患者侧详情

这样前端不需要再仅靠 `smartDispatch` 或 `doctorHandle` 间接推断“是否已接诊”，而是可以直接读取认领快照。

## 前端改动

更新文件：

- `template-front/src/triage/reminder.js`
- `template-front/src/views/index/ConsultationPage.vue`
- `template-front/src/views/index/ConsultationReminderPage.vue`
- `template-front/src/views/index/OverviewPage.vue`

改动说明：

- 新增“医生已认领 / 已接手 / 当前跟进医生”的统一判断辅助逻辑
- 当问诊已被医生认领、正在处理中、或最近一条消息来自医生时，患者侧会归入“医生处理中”
- 医生回复被患者读过后，不会再错误回退成“待医生处理”
- 问诊详情的智能分配区域补充“当前接诊医生”和“认领时间”
- 问诊时间线补充“医生已认领”节点，帮助患者看清提交、分诊、认领、处理、随访的全过程

## 用户价值

- 患者能更直观地判断问诊是否已经有人接手
- 留言区提示会更明确，不再只有“等医生处理”的模糊表述
- 提醒中心、首页总览、问诊详情三处的进度语义保持一致
- 在线问诊主链路从“提交 -> 接诊 -> 沟通 -> 处理 -> 随访”的状态展示更完整

## 验证

已完成验证：

- 前端：`cmd /c npm run build`
- 后端：`mvn -q -DskipTests compile`
