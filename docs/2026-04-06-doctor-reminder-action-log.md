# 2026-04-06 医生端待办中心快捷处理开发记录

## 1. 本轮目标

继续把医生端“消息与待办中心”从汇总页推进成可直接处理的工作页，重点补齐：

- 待认领问诊可在待办中心直接认领
- 已认领问诊可在待办中心直接释放
- 不同待办类型能直接跳到最合适的处理动作
- 医生问诊列表与待办中心共用一套消息/随访判断逻辑

## 2. 本轮完成

### 2.1 待办中心补齐快捷认领与释放

更新文件：

- `template-front/src/views/doctor/DoctorReminderPage.vue`

本轮在待办流列表中新增了可直接处理的动作：

- 对未认领且可接手的记录显示“认领并处理”
- 对本人已认领且仍可释放的记录显示“释放”
- 认领或释放成功后自动刷新待办列表
- 同步刷新医生工作台汇总数据

这样医生不需要先跳回问诊列表页，便可以在待办中心完成最常见的接单动作。

### 2.2 不同待办类型补齐更直接的入口

更新文件：

- `template-front/src/views/doctor/DoctorReminderPage.vue`

待办流列表的主按钮不再固定为“查看详情”，而是根据当前任务状态动态切换为：

- `认领并处理`
- `去回复`
- `去随访`
- `查看详情`

同时会带上对应的列表上下文查询参数，例如：

- 消息待回复记录带上 `messageFilter`
- 随访记录带上 `followUpFilter + sortMode=follow_up_due`
- 待认领记录继续保留待认领上下文

这样医生从待办中心进入问诊页后，列表状态和详情处理动作会更连贯。

### 2.3 医生问诊页开始复用待办共享工具

更新文件：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/doctor/reminder.js`

本轮把医生问诊页中已稳定的公共判断逻辑切换为复用 `doctor/reminder.js`：

- 消息摘要归一化
- 未读消息判断
- 待医生回复判断
- 随访到期计算
- 待随访状态判断
- 风险问诊识别
- 消息进度标签
- 随访标签与日期格式化
- 归属判断

这样医生工作台、待办中心、问诊列表后续会共享同一套提醒标准，减少重复维护和排序口径偏差。

## 3. 涉及文件

- `template-front/src/views/doctor/DoctorReminderPage.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `docs/2026-04-06-doctor-reminder-action-log.md`

## 4. 价值

- 医生端待办中心开始具备“直接处理”能力，而不是仅做提醒展示
- 认领、回复、随访三类高频动作形成更顺手的处理链路
- 待办中心与问诊列表的规则判断开始统一，为后续继续扩展通知、协作和患者随访闭环打下基础

## 5. 验证

- 前端执行 `npm run build`
- 后端执行 `mvn -q -DskipTests compile`
