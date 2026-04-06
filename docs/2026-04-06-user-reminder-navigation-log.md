# 2026-04-06 患者提醒导航增强记录

## 1. 本轮目标

在已经具备提醒中心页面的基础上，继续补齐两个体验上的关键缺口：

- 患者侧左侧导航需要直接感知“当前有多少提醒待处理”
- 提醒中心需要支持按筛选条件深链打开，方便后续从首页、通知和其他页面直接带条件跳转

## 2. 本轮完成

### 2.1 工作区侧边栏增加提醒徽标

更新文件：

- `template-front/src/components/WorkspaceShell.vue`

本轮为患者工作区增加了提醒汇总能力：

- 工作区壳层会在患者侧路由下自动拉取问诊记录列表
- 统一计算提醒总数、医生新回复、待医生处理、待随访、今日到期和逾期数量
- 左侧菜单中的“消息与提醒”项新增数量徽标
- 当存在逾期随访时，徽标会按更高优先级展示

同时将这份工作区汇总能力通过 `account-context` 向子页面透出，方便后续页面在完成问诊操作后主动刷新侧边栏状态。

### 2.2 提醒中心支持按路由参数恢复筛选状态

更新文件：

- `template-front/src/views/index/ConsultationReminderPage.vue`

提醒中心新增了 `feed` 查询参数同步能力，支持：

- `all`
- `unread`
- `waiting`
- `followup`
- `overdue`

这样后续无论是从患者工作台、消息通知，还是从其他业务入口跳转进来，都可以直接打开指定筛选视图，而不需要用户二次手动切换。

### 2.3 抽出提醒记录标准化工具

更新文件：

- `template-front/src/triage/reminder.js`
- `template-front/src/views/index/OverviewPage.vue`
- `template-front/src/views/index/ConsultationReminderPage.vue`

新增提醒工具模块后，患者首页、提醒中心和工作区壳层可以共用问诊提醒记录的标准化逻辑，后续继续扩展提醒流转时不需要在每个页面重复处理 `smartDispatch` 和 `messageSummary` 的归一化。

## 3. 涉及文件

- `template-front/src/components/WorkspaceShell.vue`
- `template-front/src/triage/reminder.js`
- `template-front/src/views/index/ConsultationReminderPage.vue`
- `template-front/src/views/index/OverviewPage.vue`
- `docs/2026-04-06-user-reminder-navigation-log.md`

## 4. 价值

- 患者进入工作区后，左侧就能立刻感知是否有待处理提醒
- 提醒中心具备了可复用的深链能力，适合作为后续通知中心的承接页
- 工作区提醒统计与页面提醒数据开始向统一工具收敛，为后续继续抽公共逻辑打基础

## 5. 验证

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过
