# 2026-04-12 医生入口页患者后续动作增强记录
## 本次完成

- 扩展问诊消息摘要结构，新增双向未读计数：
  - `unreadByDoctorCount`：医生尚未查看的患者消息数
  - `unreadByPatientCount`：患者尚未查看的医生回复数
- 在医生端公共辅助文件中补充“患者后续动作”推导逻辑，统一输出：
  - 患者是否待查看医生回复
  - 是否补充恢复更新
  - 是否补充检查结果
  - 是否提交或处理服务评价
  - 是否仍处于随访等待反馈阶段
- 在医生提醒中心补充患者后续动作标签与说明文案，医生无需先点进详情页，就能看到当前患者最近的后续行为。
- 在医生工作台的“患者新消息 / 待回复 / 待随访”卡片中补充患者后续动作标签。
- 在医生工作台“最近问诊”表格新增“患者后续动作”列，集中展示状态与摘要。

## 实现说明

- 本次没有新增页面接口，仍复用现有问诊列表与工作台汇总接口。
- 后端只增强了消息摘要字段，兼容现有 `unreadCount` 语义，不影响已有页面逻辑。
- 前端将患者后续动作的判定抽到了 `template-front/src/doctor/reminder.js`，后续医生列表页和更多入口都可以直接复用。

## 涉及文件

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationMessageSummaryVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationMessageServiceImpl.java`
- `template-front/src/doctor/reminder.js`
- `template-front/src/views/doctor/DoctorReminderPage.vue`
- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`

## 验证

- 前端构建通过：
  - `cmd /c npm run build`
- 后端本轮未能在当前终端完成编译验证：
  - 原因是环境中没有 `mvn` 命令，项目内也没有 `mvnw` / `mvnw.cmd`

## 后续建议

1. 下一步可把同一套患者后续动作状态继续下沉到医生问诊列表页，做到列表、提醒中心、详情页三处一致。
2. 如果后续接入 WebSocket，可把“患者补充恢复更新 / 检查结果 / 服务评价 / 已读医生回复”直接推送到医生工作台。
3. 后端后续可以继续补充“患者已查看医生结论 / 已查看随访建议”等更细粒度阅读状态，进一步完善在线问诊闭环。
