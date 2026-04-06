# 2026-04-06 医生端导航联动与待办徽标增强记录
## 1. 本轮目标

继续完善医生端“工作台 -> 问诊列表 -> 详情处理”的连续操作体验，补齐两个关键缺口：

- 医生侧边栏需要直接感知当前还有多少待处理问诊
- 医生问诊列表的筛选状态需要和路由保持同步，方便从工作台深链进入后继续保留上下文

## 2. 本轮完成

### 2.1 工作区壳层补齐医生摘要与菜单徽标

更新文件：
- `template-front/src/components/WorkspaceShell.vue`
- `template-front/src/views/DoctorView.vue`
- `template-front/src/views/IndexView.vue`

本轮将工作区壳层的摘要能力拆分为患者侧和医生侧两套：

- 患者侧继续使用原有提醒汇总逻辑
- 医生侧新增 `/api/doctor/workbench/summary` 摘要加载
- 通过 `account-context` 向医生页面暴露：
  - `doctorWorkspaceSummary`
  - `patchDoctorWorkspaceSummary`
  - `refreshDoctorWorkspaceSummary`

同时，医生侧菜单中的“科室问诊列表”新增待办徽标：

- 徽标数量使用聚合待办口径：
  - 待认领问诊
  - 患者新消息
  - 待医生回复
  - 逾期随访
- 当存在高优先级未认领或逾期随访时，徽标显示为更高优先级颜色

这样医生即使不进入工作台首页，也能在侧边栏快速感知当前待办压力。

### 2.2 医生问诊列表补齐筛选与路由双向同步

更新文件：
- `template-front/src/views/doctor/DoctorConsultationPage.vue`

医生问诊列表本轮补齐了筛选和路由之间的双向联动：

- 页面初始化时会根据路由参数恢复筛选项
- 当路由中缺失某个筛选参数时，会回落到默认值，而不是保留旧状态
- 当医生手动修改筛选条件时，会自动同步回 URL
- 当抽屉详情打开时，会在当前筛选参数基础上追加 `id`
- 当详情关闭时，会自动移除 `id`，但保留当前列表上下文

目前已支持持续同步的筛选项包括：

- `ownerFilter`
- `status`
- `messageFilter`
- `dispatchFilter`
- `followUpFilter`
- `riskFilter`
- `sortMode`

这样从工作台卡片、待办看板或其他深链入口进入问诊列表后，医生可以继续切换和分享当前视图，而不会丢失上下文。

### 2.3 医生动作完成后自动刷新外层待办摘要

更新文件：
- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`

本轮把医生问诊动作和外层壳组件的待办摘要接通了：

- 认领问诊后刷新侧边栏摘要
- 释放问诊后刷新侧边栏摘要
- 发送沟通消息后刷新侧边栏摘要
- 保存处理结果后刷新侧边栏摘要
- 追加随访记录后刷新侧边栏摘要
- 医生工作台加载自己的摘要数据后，同步更新壳组件中的医生摘要

这样列表页、详情页和侧边栏的数据变化开始保持一致，医生在一个页面完成动作后，不需要手动回到首页才能看到待办数量变化。

## 3. 涉及文件

- `template-front/src/components/WorkspaceShell.vue`
- `template-front/src/views/DoctorView.vue`
- `template-front/src/views/IndexView.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`
- `docs/2026-04-06-doctor-navigation-badge-log.md`

## 4. 价值

- 医生端导航开始具备“实时待办入口”的工作台属性，而不只是页面跳转菜单
- 工作台、问诊列表、详情抽屉之间的上下文链路更完整
- 后续继续扩展医生通知中心、诊后随访看板和消息提醒时，可以直接复用当前壳层摘要能力

## 5. 验证

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过
