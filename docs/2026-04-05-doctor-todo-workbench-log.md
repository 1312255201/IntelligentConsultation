# 2026-04-05 医生待办看板与患者沟通状态增强

## 1. 本轮目标

在已经具备“医患沟通消息流 + 未读状态 + 消息摘要”的基础上，继续把这些数据真正转成可行动的工作流：

- 医生工作台不再只展示“最近问诊”，而是直接展示待办看板
- 医生可以快速定位“患者新消息 / 待回复 / 待随访”问诊
- 患者在自己的问诊记录列表中，也能一眼看出医生是否有新回复

这一轮的重点不是新增新的流程节点，而是把前一轮沉淀下来的消息摘要真正用起来。

## 2. 后端增强

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorWorkbenchVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

新增工作台摘要字段：

- `unreadConsultationCount`
- `waitingReplyConsultationCount`
- `pendingFollowUpCount`
- `unreadConsultations`
- `waitingReplyConsultations`
- `pendingFollowUpConsultations`

聚合规则：

- `患者新消息`
  - 当前医生可处理的问诊
  - 问诊消息摘要中 `unreadCount > 0`
- `待医生回复`
  - 当前医生可处理的问诊
  - 最新一条沟通消息来自患者
- `待随访`
  - 问诊已完成
  - 当前问诊由该医生处理完成
  - 结构化结论里标记 `needFollowUp = 1`
  - 且当前还没有结束随访闭环

说明：

- 待办聚合继续复用已有 `messageSummary`
- 待随访逻辑复用了医生处理、结构化结论、随访记录三段已有数据
- 没有新增数据库表，属于已有能力的工作台聚合增强

## 3. 医生端页面增强

更新：

- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`

增强效果：

- 医生工作台顶部统计新增：
  - 患者新消息
  - 待医生回复
  - 待随访
- 工作台新增“待办看板”区块，分成三栏：
  - 患者新消息
  - 待医生回复
  - 待随访
- 每个待办卡片都可直接点击跳转到对应问诊详情
- 从工作台跳转到问诊列表时，可直接带上消息筛选参数

这样医生登录后，已经不需要先进入长表格再自己找记录，工作台首页就能直接看到当前最值得优先处理的问诊。

## 4. 患者端页面增强

更新：

- `template-front/src/views/index/ConsultationPage.vue`

增强效果：

- 问诊记录页顶部统计新增“医生新回复”
- 问诊记录列表新增“沟通状态”列
- 问诊详情中的“医患沟通”区显示：
  - 消息总数
  - 医生新回复数量
  - 最近更新时间

当前患者侧可区分的状态包括：

- 暂无沟通
- 医生新回复
- 已发送，待医生处理
- 医生已回复

## 5. 本轮涉及文件

### 后端

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorWorkbenchVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

### 前端

- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/index/ConsultationPage.vue`

## 6. 验证结果

本轮已完成构建验证：

- 后端：`mvn -q -DskipTests compile`
- 前端：`npm run build`

均已通过。

## 7. 下一步建议

建议下一优先级继续按下面顺序推进：

1. 医生待办看板进一步支持“即将逾期随访 / 高优先级待处理”排序
2. 患者侧增加更明确的未读提醒入口与问诊进度时间线
3. 把 AI 追问消息与医患沟通消息进一步融合，减少两套消息入口的割裂感
