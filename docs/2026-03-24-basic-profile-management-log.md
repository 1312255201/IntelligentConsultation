# 2026-03-24 开发记录

## 项目定位

当前项目是一个前后端分离模板，后续将逐步演进为“智能问诊系统”。

本阶段主要围绕以下几类能力持续补齐：

- 登录后的用户工作台
- 用户基础信息管理
- 管理员端基础数据维护
- 管理员端主页内容配置
- 公共网站首页展示
- MySQL 5.7 初始化脚本
- MinIO 图片上传与展示

## 已完成功能总览

### 1. 用户端基础信息管理

已完成：

- 登录后工作台布局
- 左侧菜单、右侧路由内容区
- 右上角头像、用户信息、退出登录
- 当前登录用户资料读取
- 修改邮箱
- 修改密码
- 上传头像

后端接口：

- `GET /api/user/me`
- `POST /api/user/change-email`
- `POST /api/user/change-password`
- `POST /api/image/avatar`

相关后端文件：

- `template-backend/src/main/java/cn/gugufish/controller/AccountController.java`
- `template-backend/src/main/java/cn/gugufish/service/AccountService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/AccountServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/ChangeEmailVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/ChangePasswordVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AccountInfoVO.java`

相关前端文件：

- `template-front/src/views/IndexView.vue`
- `template-front/src/views/index/ProfilePage.vue`
- `template-front/src/views/index/OverviewPage.vue`
- `template-front/src/components/WorkspaceShell.vue`
- `template-front/src/net/index.js`
- `template-front/src/router/index.js`

### 1.1 用户端就诊人管理

已完成：

- 当前账号维护多个就诊人档案
- 支持维护本人、子女、父母、配偶、其他关系类型
- 支持设置默认就诊人
- 支持就诊人启停状态维护
- 支持就诊人新增、编辑、删除
- 登录后工作台新增 `就诊人管理` 页面入口

后端接口：

- `GET /api/user/patient/list`
- `POST /api/user/patient/create`
- `POST /api/user/patient/update`
- `GET /api/user/patient/delete?patientId=xxx`

相关后端文件：

- `template-backend/src/main/java/cn/gugufish/controller/PatientProfileController.java`
- `template-backend/src/main/java/cn/gugufish/entity/dto/PatientProfile.java`
- `template-backend/src/main/java/cn/gugufish/mapper/PatientProfileMapper.java`
- `template-backend/src/main/java/cn/gugufish/service/PatientProfileService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/PatientProfileServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/PatientProfileCreateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/PatientProfileUpdateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/PatientProfileVO.java`

相关前端文件：

- `template-front/src/views/index/PatientPage.vue`
- `template-front/src/views/IndexView.vue`
- `template-front/src/router/index.js`

实现说明：

- 首个就诊人会自动设置为默认就诊人
- 一个账号仅允许维护一个“本人”就诊人档案
- 删除默认就诊人后，系统会自动补位下一个默认就诊人
- 当前阶段先完成就诊人基础资料维护，健康档案将在后续单独扩展

验证结果：

- 后端执行 `mvn -q -DskipTests compile` 已通过
- 前端执行 `npm run build` 已通过

### 1.2 用户端健康档案管理

已完成：

- 按就诊人维护一对一健康档案
- 支持维护过敏史、既往病史、慢性病史、手术史、家族史
- 支持维护长期用药、孕期状态、哺乳状态、传染病史
- 登录后工作台新增 `健康档案` 页面入口
- 就诊人管理页支持直接跳转到对应就诊人的健康档案

后端接口：

- `GET /api/user/medical-history/list`
- `GET /api/user/medical-history/detail?patientId=xxx`
- `POST /api/user/medical-history/save`
- `GET /api/user/medical-history/delete?patientId=xxx`

相关后端文件：

- `template-backend/src/main/java/cn/gugufish/controller/PatientMedicalHistoryController.java`
- `template-backend/src/main/java/cn/gugufish/entity/dto/PatientMedicalHistory.java`
- `template-backend/src/main/java/cn/gugufish/mapper/PatientMedicalHistoryMapper.java`
- `template-backend/src/main/java/cn/gugufish/service/PatientMedicalHistoryService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/PatientMedicalHistoryServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/PatientMedicalHistorySaveVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/PatientMedicalHistoryVO.java`

相关前端文件：

- `template-front/src/views/index/HealthPage.vue`
- `template-front/src/views/index/PatientPage.vue`
- `template-front/src/views/IndexView.vue`
- `template-front/src/router/index.js`

实现说明：

- 当前健康档案与就诊人是 `一对一` 关系
- 健康档案保存时要求至少填写一项有效信息，避免产生空档案
- 删除就诊人时，其健康档案会通过外键级联一起删除
- 当前阶段先完成基础病史信息维护，结构化健康标签将在后续单独扩展

验证结果：

- 后端执行 `mvn -q -DskipTests compile` 已通过
- 前端执行 `npm run build` 已通过

### 2. 管理员端基础框架

已完成：

- `admin` 角色登录后自动进入管理员工作台
- 新增管理员路由前缀：`/admin`
- 管理员与普通用户路由隔离
- 管理员左侧菜单与右侧业务页面布局

相关文件：

- `template-front/src/views/AdminView.vue`
- `template-front/src/router/index.js`
- `template-front/src/net/index.js`
- `template-front/src/components/WorkspaceShell.vue`

### 3. 科室信息维护

已完成：

- 科室列表查询
- 新增科室
- 编辑科室
- 删除科室
- 科室启停状态维护

后端接口：

- `GET /api/admin/department/list`
- `POST /api/admin/department/create`
- `POST /api/admin/department/update`
- `GET /api/admin/department/delete?id=xxx`

主要文件：

- `template-backend/src/main/java/cn/gugufish/entity/dto/Department.java`
- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminDepartmentController.java`
- `template-backend/src/main/java/cn/gugufish/service/DepartmentService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DepartmentServiceImpl.java`
- `template-front/src/views/admin/DepartmentPage.vue`

说明：

- 已去掉管理员科室页面中的说明型大卡片，只保留实际管理内容

### 4. 医生信息维护

已完成：

- 医生绑定科室
- 医生照片上传
- 医生介绍维护
- 医生专长维护
- 医生启停状态维护
- 医生新增、编辑、删除

后端接口：

- `GET /api/admin/doctor/list`
- `POST /api/admin/doctor/create`
- `POST /api/admin/doctor/update`
- `GET /api/admin/doctor/delete?id=xxx`
- `POST /api/admin/doctor/upload-photo`

主要文件：

- `template-backend/src/main/java/cn/gugufish/entity/dto/Doctor.java`
- `template-backend/src/main/java/cn/gugufish/mapper/DoctorMapper.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminDoctorController.java`
- `template-front/src/views/admin/DoctorPage.vue`

补充说明：

- 删除科室前会先校验该科室下是否仍有关联医生

## 管理员主页设置

本轮已完成管理员端“主页设置”能力，用于维护系统首页的核心展示信息。

### 1. 当前可配置内容

- 首页主标题
- 首页副标题
- 首页公告
- 平台简介标题
- 平台简介内容
- 服务热线
- 推荐医生展示
- 经典案例展示

其中经典案例支持上传封面图。

### 2. 后端实现

新增实体：

- `template-backend/src/main/java/cn/gugufish/entity/dto/HomepageConfig.java`
- `template-backend/src/main/java/cn/gugufish/entity/dto/HomepageRecommendDoctor.java`
- `template-backend/src/main/java/cn/gugufish/entity/dto/HomepageCase.java`

新增 Mapper：

- `template-backend/src/main/java/cn/gugufish/mapper/HomepageConfigMapper.java`
- `template-backend/src/main/java/cn/gugufish/mapper/HomepageRecommendDoctorMapper.java`
- `template-backend/src/main/java/cn/gugufish/mapper/HomepageCaseMapper.java`

新增请求 VO：

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/HomepageConfigSaveVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/HomepageRecommendDoctorCreateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/HomepageRecommendDoctorUpdateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/HomepageCaseCreateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/HomepageCaseUpdateVO.java`

新增响应 VO：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/HomepageConfigVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/HomepageRecommendDoctorVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/HomepageCaseVO.java`

新增服务与控制器：

- `template-backend/src/main/java/cn/gugufish/service/HomepageService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/HomepageServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminHomepageController.java`

新增管理员接口：

- `GET /api/admin/homepage/config`
- `POST /api/admin/homepage/config`
- `GET /api/admin/homepage/recommend-doctor/list`
- `POST /api/admin/homepage/recommend-doctor/create`
- `POST /api/admin/homepage/recommend-doctor/update`
- `GET /api/admin/homepage/recommend-doctor/delete?id=xxx`
- `GET /api/admin/homepage/case/list`
- `POST /api/admin/homepage/case/create`
- `POST /api/admin/homepage/case/update`
- `GET /api/admin/homepage/case/delete?id=xxx`
- `POST /api/admin/homepage/upload-image?type=case`

### 3. 图片上传

在原有 MinIO 能力上，新增了首页图片上传实现：

- `ImageService.uploadHomepageImage(String type, MultipartFile file)`

对象路径规则：

- `/homepage/{type}/{yyyyMMdd}/{uuid}`

当前已使用：

- `type=case`，用于经典案例封面图

已预留：

- `type=banner`
- `type=section`

### 4. 删除保护

为了避免管理员删除基础数据后导致首页配置异常，本轮新增了引用校验：

- 医生如果已被“推荐医生”引用，则不能直接删除
- 医生如果已被“经典案例”引用，则不能直接删除
- 科室如果已被“经典案例”引用，则不能直接删除

相关实现文件：

- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DepartmentServiceImpl.java`

### 5. 管理员页面

新增页面：

- `template-front/src/views/admin/HomepageSettingPage.vue`

页面结构：

- 顶部统计卡片
- `基础信息` 标签页
- `推荐医生` 标签页
- `经典案例` 标签页

页面能力：

- 首页基础信息可编辑并保存
- 推荐医生支持新增、编辑、删除、启停
- 推荐医生从已有医生数据中选择
- 经典案例支持上传封面图
- 经典案例支持绑定科室
- 经典案例支持可选绑定医生
- 经典案例支持维护摘要、详情、标签、排序、状态
- 经典案例医生下拉会随科室自动过滤

### 6. 管理员入口调整

本轮已调整管理员默认首页为主页设置页：

- 新增菜单：`/admin/homepage`
- 管理员默认入口改为：`/admin/homepage`

涉及文件：

- `template-front/src/views/AdminView.vue`
- `template-front/src/router/index.js`
- `template-front/src/net/index.js`

## 本轮新增：公共网站首页

本轮进一步完成了面向普通访客的公共首页，让用户访问网站时先看到主页，而不是直接落到登录页。

### 1. 路由调整

当前入口结构已调整为：

- `/`：公共网站首页
- `/login`：登录页
- `/register`：注册页
- `/forget`：找回密码页

说明：

- 用户现在直接访问网站时优先进入公共首页
- 认证页面改为独立入口，不再占用根路由
- 未登录用户若访问 `/index` 或 `/admin`，会自动跳转到 `/login`

涉及文件：

- `template-front/src/router/index.js`
- `template-front/src/net/index.js`

### 2. 公共首页展示数据接口

为公共首页新增公开访问接口：

- `GET /api/homepage/landing`

对应控制器：

- `template-backend/src/main/java/cn/gugufish/controller/HomepageController.java`

对应服务扩展：

- `template-backend/src/main/java/cn/gugufish/service/HomepageService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/HomepageServiceImpl.java`

新增公开响应对象：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/HomepageLandingVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/HomepageDepartmentPublicVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/HomepageRecommendDoctorPublicVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/HomepageCasePublicVO.java`

公开接口返回内容包括：

- 首页基础信息
- 启用中的科室信息
- 启用中的推荐医生
- 启用中的经典案例

同时已开放匿名访问权限：

- `SecurityConfiguration` 中已放行 `/api/homepage/**`

### 3. 公共首页前端展示

新增页面：

- `template-front/src/views/HomeView.vue`

首页当前包含以下展示区域：

- 顶部导航
- 首屏主视觉
- 平台简介
- 服务亮点
- 科室服务入口
- 推荐医生
- 经典案例
- 使用流程
- 底部行动横幅
- 右下角悬浮“立刻咨询”按钮

### 4. 多入口跳转登录

首页上已新增多种进入登录页或系统入口的方式，不再仅依赖右上角登录按钮。

当前可进入登录页的入口包括：

- 顶部“登录系统”按钮
- 顶部“立刻咨询”按钮
- 首屏“立即咨询”按钮
- 首屏公告条点击
- 各科室卡片中的咨询按钮
- 推荐医生卡片中的“预约咨询”按钮
- 经典案例卡片中的“获取方案”按钮
- 使用流程卡片中的行动按钮
- 底部横幅中的咨询按钮
- 右下角悬浮咨询按钮

行为说明：

- 未登录用户点击这些入口时，会进入 `/login`
- 已登录用户点击这些入口时，会直接进入自己的系统首页

### 5. 登录、注册、找回密码页重做

为了配合新的公共首页入口，本轮同步重做了认证页风格：

- `template-front/src/views/WelcomeView.vue`
- `template-front/src/views/welcome/LoginPage.vue`
- `template-front/src/views/welcome/RegisterPage.vue`
- `template-front/src/views/welcome/ForgetPage.vue`

调整内容：

- 认证页改为更贴近医疗系统的视觉风格
- 支持从认证页返回公共首页
- 注册成功后跳转到 `/login`
- 重置密码成功后跳转到 `/login`

### 6. 首页视觉优化补充：三维轮播首屏

针对公共首页“首屏太平、视觉吸引力不足”的问题，本轮继续对首页进行了视觉强化，重点改为带动效的三维轮播展示。

本次优化内容：

- Hero 右侧升级为 3D 轮播舞台
- 推荐医生与经典案例混合编排到轮播内容中
- 支持自动轮播、左右切换、悬停暂停、圆点定位切换
- 新增舞台光晕、旋转环、地台网格、卡片呼吸浮动等层次动效
- 新增轮播进度状态条，增强当前展示位置感知
- 当前聚焦卡片可直接进入咨询入口或打开案例详情弹窗
- 保留首页多入口跳转登录/系统逻辑，不再依赖单一登录按钮

涉及文件：

- `template-front/src/views/HomeView.vue`

验证结果：

- 前端执行 `npm run build` 已通过

### 7. 首页文案生产化收口

为避免首页出现偏开发、偏后台或偏实现过程的表达，本轮继续对公共首页文案做了用户视角收口。

本次调整方向：

- 删除“管理员配置”“登录页”“工作台”“后台维护”等内部视角描述
- 删除“通过案例展示平台价值，再自然引导到登录入口”这类实现导向表述
- 将首页文案统一改为面向访客的服务介绍、医生展示、案例参考与咨询引导
- 保留多入口跳转行为，但按钮与说明文案改为更自然的用户侧表达
- 将空状态文案改为“信息整理中”“案例持续更新中”等更接近生产环境的提示

涉及文件：

- `template-front/src/views/HomeView.vue`

验证结果：

- 前端执行 `npm run build` 已通过

## SQL 初始化脚本

当前 `sql/mysql57-init.sql` 已包含以下表：

- `db_account`
- `db_image_store`
- `db_department`
- `db_doctor`
- `db_patient_profile`
- `db_patient_medical_history`
- `db_homepage_config`
- `db_homepage_recommend_doctor`
- `db_homepage_case`

说明：

- 数据库名：`consultation`
- 脚本兼容 MySQL 5.7
- 使用 `utf8mb4`
- 推荐医生表通过唯一索引限制同一医生重复推荐
- 经典案例表支持科室、医生、封面、摘要、详情、标签、排序与启停状态

## 编译验证

已完成验证：

- 前端：`npm run build`
- 后端：`mvn -q -DskipTests compile`

结果：

- 前后端均已通过编译

## 当前实现约束

- 当前项目接口风格继续沿用 `GET` 和 `POST`
- 头像、医生照片、案例封面均通过 MinIO 对象路径保存
- 图片展示继续通过后端 `/images/**` 代理访问
- 当前公共首页已可展示主页基础数据，但轮播图管理尚未开始
- 当前公共首页重点解决“先看网站首页、再跳登录”的访问路径问题

## 建议下一步

建议下一步继续按以下顺序推进：

1. 新增首页轮播图管理
2. 新增轮播图对应的公开展示接口
3. 继续完善公共首页展示细节与动效
4. 新增患者档案、预约挂号、问诊记录等业务模块
5. 接入智能问诊对话与分诊流程

## 备注

开发过程中遵循了以下原则：

- 不回退工作区中已有的非当前任务改动
- 尽量复用当前布局、接口风格与 MinIO 方案
- 将关键开发过程持续记录到 Markdown 中，便于后续续做

## 2026-03-25 补充规划

为后续接入 AI 智能导诊，已新增专题规划文档：

- `docs/2026-03-25-ai-triage-foundation-guide.md`

文档内容覆盖：

- AI 导诊定位与能力边界
- 就诊人、健康档案、医生服务能力、医生排班等基础数据建设
- 问诊分类、前置资料模板、症状字典、红旗规则建设
- 导诊知识库、导诊会话、导诊消息、导诊结果留痕设计
- 规则引擎与 AI 结合策略
- 风控、留痕、审计、后台运营配置建议
- 建议开发阶段顺序与近期实施路线
