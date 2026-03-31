# 2026-04-01 医生常用回复模板开发记录

## 1. 本轮目标

继续完善医生端基础工作台能力，新增“医生个人常用回复模板”功能，并将模板直接接入问诊处理页和随访记录填写页，减少重复录入。

本轮除了完成基础 CRUD，还额外优化了上手体验：

- 医生问诊页内可直接跳转模板管理
- 模板为空时给出明确引导
- 模板管理页增加场景说明和示例内容
- 演示数据补充模板样例，便于直接测试

## 2. SQL 变更

### 2.1 初始化结构

已在 `sql/mysql57-init.sql` 中新增：

- `db_doctor_reply_template`

字段说明：

- `doctor_id`：模板所属医生
- `scene_type`：模板使用场景
- `title`：模板标题
- `content`：模板正文
- `sort`：排序
- `status`：启停状态
- `create_time` / `update_time`：创建和更新时间

约束说明：

- 同一医生、同一场景下标题唯一
- 模板跟随医生档案，医生删除时模板自动级联删除

### 2.2 升级脚本

新增：

- `sql/mysql57-upgrade-2026-04-01-doctor-reply-template.sql`

说明：

- 这是可直接执行的 MySQL 5.7 SQL 文件
- 文件内已附带注释，提醒不要把 patch diff 文本直接导入数据库

### 2.3 演示数据

更新：

- `sql/mysql57-demo-data.sql`

补充了 5 位演示医生的模板样例，覆盖以下场景：

- `handle_summary`
- `medical_advice`
- `follow_up_plan`
- `patient_instruction`
- `followup_summary`
- `followup_advice`
- `followup_next_step`

这样导入演示数据后，医生账号进入模板页和问诊处理页就可以直接看到可选模板。

## 3. 后端能力

新增主要文件：

- `template-backend/src/main/java/cn/gugufish/entity/dto/DoctorReplyTemplate.java`
- `template-backend/src/main/java/cn/gugufish/mapper/DoctorReplyTemplateMapper.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorReplyTemplateCreateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorReplyTemplateUpdateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorReplyTemplateVO.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorReplyTemplateService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorReplyTemplateServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorReplyTemplateController.java`

新增医生端接口：

- `GET /api/doctor/reply-template/list`
- `POST /api/doctor/reply-template/create`
- `POST /api/doctor/reply-template/update`
- `GET /api/doctor/reply-template/delete?id=xxx`

业务规则：

- 模板只归当前登录医生本人所有
- 当前账号必须绑定有效医生档案
- 同一场景下不允许重复标题
- 删除和更新时会校验模板归属，防止越权操作

## 4. 前端能力

### 4.1 新增模板管理页

新增页面：

- `template-front/src/views/doctor/DoctorReplyTemplatePage.vue`

页面功能：

- 查看当前医生所有模板
- 按关键词、场景、状态筛选
- 新增、编辑、删除模板
- 统计模板总数、启用数、覆盖场景数

本轮额外增强：

- 顶部增加“模板填写说明”卡片
- 每个场景都展示用途、适合填写内容和示例
- 弹窗中根据选中场景展示推荐写法
- 支持一键把示例内容填入标题和正文

### 4.2 接入医生工作台

已接入：

- `template-front/src/views/DoctorView.vue`
- `template-front/src/router/index.js`

菜单中新增：

- `常用回复模板`

### 4.3 接入问诊处理页

更新：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

接入效果：

- 医生处理表单可选择模板并快捷填入
- 随访记录表单可选择模板并快捷填入
- 支持覆盖填入和追加填入两种方式
- 模板正在加载时会显示提示
- 没有模板时会显示空状态引导
- 可从问诊处理页直接跳转到模板管理页

目前已接入的快捷填入场景：

- 医生判断摘要
- 处理建议
- 随访计划
- 患者指导要点
- 随访摘要
- 随访建议
- 随访下一步安排

## 5. 本轮涉及文件

### 后端

- `template-backend/src/main/java/cn/gugufish/entity/dto/DoctorReplyTemplate.java`
- `template-backend/src/main/java/cn/gugufish/mapper/DoctorReplyTemplateMapper.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorReplyTemplateCreateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorReplyTemplateUpdateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorReplyTemplateVO.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorReplyTemplateService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorReplyTemplateServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorReplyTemplateController.java`

### 前端

- `template-front/src/views/doctor/DoctorReplyTemplatePage.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/DoctorView.vue`
- `template-front/src/router/index.js`

### SQL

- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-04-01-doctor-reply-template.sql`
- `sql/mysql57-demo-data.sql`

## 6. 验证结果

本轮已完成验证：

- 后端执行 `mvn -q -DskipTests compile` 通过
- 前端执行 `npm run build` 通过

## 7. 当前阶段结论

到这里，医生端围绕“接诊处理”这条主线已经具备：

- 工作台概览
- 问诊列表与认领
- 医生处理与结构化结论
- 完成后随访记录
- 常用回复模板复用

这部分已经可以支撑后续继续往医生侧深挖，也可以开始切到下一阶段功能。

## 8. 下一步建议

建议下一优先级切到以下方向之一：

1. 医生端首页概览增强
2. 医生待办和随访提醒看板
3. 医生个人资料完善
4. 问诊聊天消息流 / 医患沟通界面

如果继续沿着医生侧推进，我更建议优先做“医患问诊消息流”，这样后面 AI 问诊和医生接管之间会更顺。
