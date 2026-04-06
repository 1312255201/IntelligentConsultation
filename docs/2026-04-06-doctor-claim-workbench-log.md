# 2026-04-06 医生端认领看板增强记录

## 1. 本轮目标

继续完善医生端“先认领，再处理”的工作流，让医生进入工作台后，不只是看到推荐问诊和患者新消息，还能直接看到：

- 当前还有多少问诊尚未认领
- 哪些待认领问诊属于高优先级
- 从工作台跳到问诊列表后，能否直接保留“待认领 + 高优先级”的筛选上下文

## 2. 本轮完成

### 2.1 医生工作台补齐待认领入口

更新文件：

- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorWorkbenchVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

本轮在医生工作台新增了认领相关汇总能力：

- 汇总统计中新增
  - `待认领问诊`
  - `我认领的`
- 待办看板新增“待认领问诊”卡片
  - 展示待认领问诊预览
  - 对高优先级记录做更显眼的提示
  - 支持一键跳到全部待认领列表
  - 支持一键跳到“待认领 + 高优先级”上下文

后端工作台汇总同步补充了：

- `unclaimedConsultationCount`
- `myClaimedConsultationCount`
- `highPriorityUnclaimedCount`
- `unclaimedConsultations`

并在服务层增加了未认领问诊的排序逻辑，优先把高优先级和最近有动态的记录放在前面。

### 2.2 医生问诊列表新增高优先级筛选

更新文件：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

医生问诊列表本轮新增：

- `riskFilter`
  - 全部优先级
  - 高优先级
  - 普通优先级

同时补齐了与路由查询参数的同步：

- 工作台跳转到列表时，可以直接带上 `riskFilter=high_priority`
- 医生在列表打开详情、关闭详情后，当前“待认领 / 高优先级”等筛选上下文不会丢失

## 3. 涉及文件

- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorWorkbenchVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `docs/2026-04-06-doctor-claim-workbench-log.md`

## 4. 价值

- 医生端首页开始真正承接“认领”动作，而不是只能在列表页里被动筛选
- 高优先级未认领问诊更容易被尽快发现，减少遗漏
- 工作台与问诊列表之间的处理链路更顺，适合批量接单和连续处理

## 5. 验证

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过
