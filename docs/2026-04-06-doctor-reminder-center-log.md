# 2026-04-06 医生端消息与待办中心开发记录
## 1. 本轮目标

在已经完成医生工作台待办看板、问诊列表筛选和侧边栏徽标的基础上，继续补齐一个更稳定的承接页：

- 让医生侧边栏中的待办徽标有独立页面承接
- 让“待认领 / 系统推荐 / 新消息 / 待回复 / 待随访”不只停留在首页卡片，而是可以集中处理
- 为后续继续扩展医生通知中心、协作提醒和诊后运营页面打下结构基础

## 2. 本轮完成

### 2.1 新增医生端独立消息与待办中心

更新文件：
- `template-front/src/views/doctor/DoctorReminderPage.vue`
- `template-front/src/router/index.js`

本轮新增了医生端独立页面：

- 路由：`/doctor/reminder`
- 页面职责：
  - 汇总当前医生侧所有关键待办
  - 展示总待办、待认领、系统推荐、患者新消息、待回复、待随访、今日到期、逾期数量
  - 支持按 `全部 / 待认领 / 系统推荐 / 患者新消息 / 待回复 / 待随访 / 逾期` 切换查看
  - 支持带上下文直接跳转到问诊详情抽屉
  - 支持一键跳转到问诊列表对应筛选状态

这样医生不再只能从工作台首页或问诊列表拆分处理待办，而是有了一个统一的集中处理页。

### 2.2 医生侧导航补齐待办中心入口，并把徽标切到新入口

更新文件：
- `template-front/src/views/DoctorView.vue`
- `template-front/src/components/WorkspaceShell.vue`
- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`

本轮医生侧菜单新增了“消息与待办”入口：

- 菜单项新增 `badgeKey: doctor-reminder`
- 侧边栏徽标改为挂在“消息与待办”菜单上
- 工作台顶部待办看板新增“进入待办中心”入口

这样医生在首页、菜单和独立处理页之间形成了更顺的导航闭环：

- 工作台看总览
- 待办中心看当前优先事项
- 问诊列表和详情页继续精细处理

### 2.3 抽取医生端待办辅助工具，避免新页面重复复制逻辑

更新文件：
- `template-front/src/doctor/reminder.js`

为了避免新页面继续在组件内部重复实现一套：

- 消息摘要归一化
- 未读消息判断
- 待医生回复判断
- 随访到期时间计算
- 随访状态判断
- 风险问诊识别
- 医生侧待办排序

本轮新增了医生端共享工具模块，供医生待办中心复用，也为后续继续收敛医生工作台和问诊列表中的重复逻辑做了准备。

## 3. 涉及文件

- `template-front/src/doctor/reminder.js`
- `template-front/src/views/doctor/DoctorReminderPage.vue`
- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`
- `template-front/src/views/DoctorView.vue`
- `template-front/src/components/WorkspaceShell.vue`
- `template-front/src/router/index.js`
- `docs/2026-04-06-doctor-reminder-center-log.md`

## 4. 价值

- 医生端待办开始具备独立承接页，不再全部挤压到问诊列表
- 侧边栏徽标、工作台卡片和集中处理页开始对齐为同一条业务链路
- 为后续继续做医生通知中心、站内协作提醒、团队分诊协同和诊后运营页面留出了自然入口

## 5. 验证

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过
