# 2026-03-24 基本信息管理开发记录

## 本次目标

为当前前后端分离模板补齐登录后的基础工作台和用户基本信息管理能力，覆盖以下范围：

- 登录后主界面布局
- 左侧功能菜单与右侧路由内容区
- 右上角头像与退出登录操作
- 当前登录用户资料读取
- 邮箱修改
- 密码修改
- 头像上传接入现有 MinIO 接口

## 已完成内容

### 1. 后端接口补充

新增当前登录用户资料管理接口：

- `GET /api/user/me`
  - 读取当前登录用户的基础资料
  - 返回字段：`id`、`username`、`email`、`role`、`avatar`、`registerTime`
- `POST /api/user/change-email`
  - 修改当前登录用户邮箱
  - 入参：`email`、`password`
  - 校验：邮箱格式、非空、当前密码匹配、邮箱唯一
- `POST /api/user/change-password`
  - 修改当前登录用户密码
  - 入参：`oldPassword`、`newPassword`
  - 校验：非空、长度 6-20、当前密码匹配、新旧密码不能一致

补充的数据对象：

- `ChangeEmailVO`
- `ChangePasswordVO`
- `AccountInfoVO`

新增控制器：

- `template-backend/src/main/java/cn/gugufish/controller/AccountController.java`

扩展服务：

- `AccountService`
- `AccountServiceImpl`

### 2. 后端安全配置调整

开放图片访问路径：

- `GET /images/**`

原因：

- 前端头像展示使用 `<img>` / `el-avatar` 拉取图片，浏览器不会自动附带 `Authorization` 头
- 如果图片路径不放行，已上传头像将无法正常显示

涉及文件：

- `template-backend/src/main/java/cn/gugufish/config/SecurityConfiguration.java`

### 3. 前端主布局重构

登录后 `IndexView` 已重构为工作台布局：

- 左上角品牌 Logo
- 左侧功能菜单
- 顶部页面标题区
- 右上角用户头像、邮箱、下拉操作
- 中间路由页面内容区

当前菜单项：

- `基本信息管理`
- `系统概览`

### 4. 前端路由调整

新增登录后子路由：

- `/index/profile`
- `/index/overview`

并将 `/index` 默认重定向到：

- `/index/profile`

### 5. 基本信息管理页面

新增页面：

- `template-front/src/views/index/ProfilePage.vue`

功能：

- 展示用户名、邮箱、角色、注册时间
- 上传头像
- 修改邮箱
- 修改密码

头像上传说明：

- 继续复用现有接口：`POST /api/image/avatar`
- 前端上传成功后会刷新当前资料

### 6. 系统概览页面

新增页面：

- `template-front/src/views/index/OverviewPage.vue`

作用：

- 作为登录后工作台的第二个路由页面
- 展示当前系统基础能力建设进度
- 为后续扩展智能问诊模块预留主入口

### 7. 网络层整理

整理了前端请求工具：

- 登录 token 存取
- 登录过期处理
- 通用 `get` / `post`
- 头像展示路径转换
- 统一 `Authorization` 请求头获取

涉及文件：

- `template-front/src/net/index.js`

## 本次修改的关键文件

### 后端

- `template-backend/src/main/java/cn/gugufish/controller/AccountController.java`
- `template-backend/src/main/java/cn/gugufish/service/AccountService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/AccountServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/ChangeEmailVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/ChangePasswordVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AccountInfoVO.java`
- `template-backend/src/main/java/cn/gugufish/config/SecurityConfiguration.java`

### 前端

- `template-front/src/App.vue`
- `template-front/src/main.js`
- `template-front/src/router/index.js`
- `template-front/src/net/index.js`
- `template-front/src/views/IndexView.vue`
- `template-front/src/views/index/ProfilePage.vue`
- `template-front/src/views/index/OverviewPage.vue`

## 编译验证

已完成验证：

- 前端：`npm run build`
- 后端：`mvn -q -DskipTests compile`

结果：

- 两端均已通过编译

## SQL 初始化文件

已补充 MySQL 5.7 初始化脚本：

- `sql/mysql57-init.sql`

脚本内容包含：

- `consultation` 数据库创建
- `db_account` 用户表创建
- `db_image_store` 图片记录表创建
- `db_department` 科室表创建

说明：

- 当前脚本采用 `CREATE ... IF NOT EXISTS`，默认不会主动删除已有数据
- 密码字段按 BCrypt 哈希存储，建议通过现有注册接口或注册页面创建首个账号
- 如果需要管理员账号，可先注册普通账号，再执行：
  `UPDATE db_account SET role = 'admin' WHERE username = '你的账号';`

## 管理员界面与科室维护

已补充管理员工作台能力：

- `admin` 身份登录后自动进入管理员界面
- 新增管理员路由前缀：`/admin`
- 默认首页：`/admin/department`
- 普通用户访问管理员页面时会自动跳回普通用户工作台

管理员当前可用功能：

- 科室列表查询
- 科室新增
- 科室编辑
- 科室删除
- 科室状态维护
- 管理员个人信息维护

后端新增接口：

- `GET /api/admin/department/list`
- `POST /api/admin/department/create`
- `POST /api/admin/department/update`
- `GET /api/admin/department/delete?id=xxx`

新增主要文件：

- `template-backend/src/main/java/cn/gugufish/entity/dto/Department.java`
- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminDepartmentController.java`
- `template-backend/src/main/java/cn/gugufish/service/DepartmentService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DepartmentServiceImpl.java`
- `template-front/src/views/AdminView.vue`
- `template-front/src/views/admin/DepartmentPage.vue`
- `template-front/src/components/WorkspaceShell.vue`

## 管理员医生管理

本轮新增了管理员医生管理能力，覆盖以下内容：

- 医生与科室绑定
- 医生照片上传
- 医生介绍维护
- 医生专长维护
- 医生启停状态维护
- 医生新增、编辑、删除

新增后端接口：

- `GET /api/admin/doctor/list`
- `POST /api/admin/doctor/create`
- `POST /api/admin/doctor/update`
- `GET /api/admin/doctor/delete?id=xxx`
- `POST /api/admin/doctor/upload-photo`

新增主要文件：

- `template-backend/src/main/java/cn/gugufish/entity/dto/Doctor.java`
- `template-backend/src/main/java/cn/gugufish/mapper/DoctorMapper.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminDoctorController.java`
- `template-front/src/views/admin/DoctorPage.vue`

相关调整：

- `sql/mysql57-init.sql` 新增 `db_doctor` 表
- 删除科室前会先校验该科室下是否仍有绑定医生
- 管理员前端菜单新增“医生信息维护”
- 管理员页面移除了原来的大段说明展示区块，只保留实际管理内容

## 目前的实现约定

- 邮箱修改当前采用“输入新邮箱 + 输入当前密码确认”的方案
- 头像展示路径通过 `/images/**` 访问后端对象代理
- 密码修改成功后不强制退出登录，当前 token 继续有效

## 下一步建议

建议你接下来优先继续做以下模块：

1. 用户资料页增加昵称、性别、年龄、手机号、基础病史等字段，为智能问诊做用户档案基础
2. 增加首页仪表盘数据卡片，例如问诊次数、最近问诊记录、待处理消息
3. 增加问诊会话模块的菜单和占位路由
4. 规划患者档案表、问诊记录表、AI 对话记录表
5. 为头像和个人资料更新补充接口测试

## 备注

本次开发过程中发现：

- 工作区里原本已有 `Account.java` 与 `AccountServiceImpl.java` 的未提交修改
- 本次实现已基于这些现有改动继续开发，没有回退你已有的字段调整
