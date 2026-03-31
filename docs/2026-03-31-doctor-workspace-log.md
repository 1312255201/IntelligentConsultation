# 2026-03-31 Doctor Workspace Log

## 本轮目标

在 AI 导诊基础信息已经具备可用性的前提下，继续完成第一版医生端基础工作台，让 `doctor` 角色登录后可以进入独立界面查看：

- 医生账号与医生档案绑定状态
- 当前医生所属科室的问诊记录
- AI 分诊结果、规则命中和用户反馈
- 医生自己的近期排班

同时补齐管理员侧绑定入口，避免后续每次都要手改数据库。

## 当前判断

到本轮为止，AI 导诊基础建设已经可以支撑医生端第一版开发，原因如下：

- 已有用户问诊提交链路
- 已有前置模板和结构化问诊答案
- 已有规则分诊结果
- 已有 triage session / triage message 留痕
- 已有 triage result 存档
- 已有 triage feedback 反馈闭环

所以现在开始做医生端是合适的，不会出现“医生端页面出来了但没有数据可看”的问题。

## 本轮完成内容

### 1. 新增 doctor 角色路由与登录分流

- 前端新增 `/doctor` 路由树
- 新增 `DoctorView.vue`
- 登录后会根据角色自动跳转：
  - `user` -> `/index/profile`
  - `doctor` -> `/doctor/workbench`
  - `admin` -> `/admin/homepage`
- 路由守卫已改为支持三种角色

### 2. 新增医生工作台

已新增页面：

- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/doctor/DoctorSchedulePage.vue`

医生工作台第一版能力：

- 展示医生基本资料、科室、专长、启停状态
- 展示服务标签
- 展示近期排班
- 展示当前科室最近问诊
- 查看问诊详情
- 查看 AI 分诊结果
- 查看规则命中
- 查看用户反馈
- 查看分诊消息轨迹

### 3. 新增后端医生端接口

新增接口：

- `GET /api/doctor/workbench/summary`
- `GET /api/doctor/consultation/list`
- `GET /api/doctor/consultation/detail?id=xxx`
- `GET /api/doctor/schedule/list`

说明：

- 当前医生端问诊列表按“医生所属科室”展示
- 这是第一版基础工作台设计
- 后续如果要进一步做“指派给具体医生”“接诊状态流转”“医生回复”和“诊后归档”，可以在此基础上继续演进

### 4. 新增医生账号绑定能力

为了解决“doctor 角色能登录，但找不到对应医生档案”的问题，本轮补了账号绑定基础设施：

- `db_doctor` 新增 `account_id`
- 一个医生档案最多绑定一个账号
- 一个 doctor 账号最多绑定一个医生档案
- 管理员端医生维护页面新增“绑定医生账号”字段
- 管理员可直接选择 `role = doctor` 的账号进行绑定

### 5. SQL 与演示数据

已更新：

- `sql/mysql57-init.sql`
- `sql/mysql57-demo-data.sql`

新增：

- `sql/mysql57-upgrade-2026-03-31-doctor-workspace.sql`

用途：

- 新项目初始化：直接用新的 `mysql57-init.sql`
- 已在跑的旧库：先执行 `mysql57-upgrade-2026-03-31-doctor-workspace.sql`
- 需要快速体验医生端：再执行 `mysql57-demo-data.sql`

### 6. 升级 SQL 导入问题修复

针对旧库升级时出现的 SQL 导入报错，本轮额外做了兼容性修复：

- 将升级脚本改为更直接的 MySQL 5.7 一次性执行版本
- 去掉了动态 SQL 检查写法，减少不同导入工具的兼容风险
- 明确标注该脚本是旧库一次性升级脚本
- 在脚本末尾补充说明：如果导入时看到 `@@ -0,0 +1,57 @@` 之类内容，说明执行的不是干净 SQL 文件，而是补丁文本

## 演示医生账号

如果你导入了最新的 `mysql57-demo-data.sql`，会自动生成下面这些医生账号：

- `doctor_linyuanhang`
- `doctor_zhouyaning`
- `doctor_chenzhixia`
- `doctor_xuqinglan`
- `doctor_songlinchuan`

默认密码：

- `Doctor@123`

这些账号会自动绑定到演示医生档案。

## 涉及的主要文件

后端：

- `template-backend/src/main/java/cn/gugufish/utils/Const.java`
- `template-backend/src/main/java/cn/gugufish/config/SecurityConfiguration.java`
- `template-backend/src/main/java/cn/gugufish/entity/dto/Doctor.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorCreateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorUpdateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorAccountOptionVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorWorkbenchVO.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorService.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorWorkspaceService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminDoctorController.java`
- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java`

前端：

- `template-front/src/net/index.js`
- `template-front/src/router/index.js`
- `template-front/src/components/WorkspaceShell.vue`
- `template-front/src/views/AdminView.vue`
- `template-front/src/views/IndexView.vue`
- `template-front/src/views/DoctorView.vue`
- `template-front/src/views/admin/DoctorPage.vue`
- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/doctor/DoctorSchedulePage.vue`
- `template-front/src/views/HomeView.vue`

SQL：

- `sql/mysql57-init.sql`
- `sql/mysql57-demo-data.sql`
- `sql/mysql57-upgrade-2026-03-31-doctor-workspace.sql`

## 验证结果

本轮已完成构建验证：

- 后端：`mvn -q -DskipTests compile`
- 前端：`npm run build`

均已通过。

## 下一步建议

医生端第一版现在已经能看数据，下一阶段建议按这个顺序继续：

1. 增加“医生接诊处理动作”
2. 增加“问诊状态流转”
3. 增加“医生回复/诊疗建议记录”
4. 增加“医生处理后的结构化结论沉淀”
5. 在此基础上再接 AI 辅助问诊与智能导诊协同
