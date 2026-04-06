# 2026-04-03 医患问诊消息流开发记录

## 1. 本轮目标

继续沿着医生接诊主线推进，补齐“医患问诊消息流 / 沟通界面”能力，让患者与医生可以围绕同一条问诊记录持续沟通，支持：

- 患者补充症状变化、检查结果和恢复情况
- 医生在接诊前追问关键信息，在处理中同步建议
- 问诊完成后继续保留随访沟通入口
- 图片附件上传与留痕，便于查看检查单、皮疹照片等资料

这一轮完成后，前面的导诊留痕、医生认领处理、随访记录和常用回复模板可以通过消息流自然衔接起来。

## 2. SQL 变更

### 2.1 初始化结构

已在 `sql/mysql57-init.sql` 中新增：

- `db_consultation_message`

字段说明：

- `consultation_id`：所属问诊记录
- `sender_type`：发送方类型，当前支持 `user`、`doctor`
- `sender_id`：发送方主键
- `sender_name`：发送方显示名称
- `sender_role_name`：发送方角色补充信息，例如医生职称
- `message_type`：消息类型，当前支持 `text`、`image`、`mixed`
- `content`：文本内容
- `attachments_json`：图片附件路径数组
- `status`：状态
- `create_time` / `update_time`：创建和更新时间

### 2.2 升级脚本

新增：

- `sql/mysql57-upgrade-2026-04-03-consultation-message.sql`

说明：

- 这是已有 MySQL 5.7 库可直接执行的升级脚本
- 结构与初始化脚本保持一致，便于已有环境补齐消息表

## 3. 后端能力

新增主要文件：

- `template-backend/src/main/java/cn/gugufish/entity/dto/ConsultationMessage.java`
- `template-backend/src/main/java/cn/gugufish/mapper/ConsultationMessageMapper.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/ConsultationMessageSendVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationMessageVO.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationMessageService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationMessageServiceImpl.java`

接入控制器：

- `template-backend/src/main/java/cn/gugufish/controller/ConsultationController.java`
- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java`

新增接口：

- `GET /api/user/consultation/message/list`
- `POST /api/user/consultation/message/send`
- `GET /api/doctor/consultation/message/list`
- `POST /api/doctor/consultation/message/send`

业务规则：

- 患者只能查看和发送本人问诊记录下的消息
- 医生必须绑定有效医生档案，且问诊记录归属本医生所在科室
- 医生发送首条消息时，如果问诊单尚未认领，会自动完成认领
- 如果问诊已经进入其他医生的处理流程，会阻止当前医生越权发送
- 单条消息支持纯文字、纯图片或图文混合
- 附件最多 6 张，统一保存为图片路径数组

## 4. 前端能力

### 4.1 用户端问诊记录详情

更新：

- `template-front/src/views/index/ConsultationPage.vue`

接入效果：

- 在问诊记录详情弹窗中新增“医患沟通”区
- 展示当前消息数量、最近更新时间和完整消息列表
- 支持患者发送文字消息
- 支持患者上传图片附件并预览、移除后再发送
- 医生接手后会显示更明确的沟通提示
- 支持手动刷新消息列表

### 4.2 医生端问诊处理详情

更新：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

接入效果：

- 在医生问诊详情抽屉中新增“医患沟通”区
- 医生可查看患者消息、历史图片附件和时间线
- 支持医生发送文字 / 图片消息
- 问诊单未认领时，会明确提示“发送并认领”
- 已被他人认领时仅允许查看，不允许发送
- 发送成功后会同步刷新问诊详情和认领状态

## 5. 本轮涉及文件

### 后端

- `template-backend/src/main/java/cn/gugufish/controller/ConsultationController.java`
- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java`
- `template-backend/src/main/java/cn/gugufish/entity/dto/ConsultationMessage.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/ConsultationMessageSendVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationMessageVO.java`
- `template-backend/src/main/java/cn/gugufish/mapper/ConsultationMessageMapper.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationMessageService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationMessageServiceImpl.java`

### 前端

- `template-front/src/views/index/ConsultationPage.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`

### SQL

- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-04-03-consultation-message.sql`

## 6. 验证结果

本轮已完成验证：

- 后端执行 `mvn -q -DskipTests compile` 通过
- 前端执行 `npm run build` 通过

## 7. 当前阶段结论

到这里，围绕单条问诊记录的核心链路已经具备：

- 用户提交问诊资料
- 系统生成导诊留痕与分诊结果
- 医生认领并处理问诊
- 用户与医生围绕同一记录持续沟通
- 完成后继续补充随访记录

这意味着后续无论接 AI 追问、消息提醒、未读计数还是医生待办看板，都有了稳定的消息底座。

## 8. 下一步建议

建议下一优先级切到以下方向之一：

1. 问诊消息未读状态与提醒
2. 医生待办 / 跟进看板
3. AI 追问消息接入当前消息流
4. 用户端问诊进度时间线增强

## 9. 2026-04-05 增量完善：消息未读状态与摘要

这一轮沿着上面的第 1 项继续推进，把“消息流”从单纯能聊天，升级成“可提醒、可筛选、可形成医生待办”的基础能力。

### 9.1 SQL 与数据结构

更新：

- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-04-05-consultation-message-read-status.sql`

新增字段：

- `read_status`：消息是否已被接收方查看
- `read_time`：首次被查看时间

这样后续无论做患者提醒、医生待办还是消息已读反馈，都可以直接复用同一份消息底座。

### 9.2 后端聚合能力

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationMessageVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationMessageSummaryVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationRecordVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationRecordVO.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationMessageService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationMessageServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceImpl.java`

新增/增强效果：

- 患者发送消息后，默认记为“医生未读”
- 医生发送消息后，默认记为“患者未读”
- 医生查询消息列表时，会自动把患者消息标记为已读
- 患者查询消息列表时，会自动把医生消息标记为已读
- 问诊记录列表和详情都会附带 `messageSummary`

当前摘要字段包含：

- 总消息数
- 患者消息数
- 医生消息数
- 当前查看者未读数
- 最近发送方
- 最近消息类型
- 最近消息预览
- 最近消息时间

### 9.3 医生端列表增强

更新：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

页面增强效果：

- 顶部统计补充“消息未读”“待回复”
- 列表工具栏新增“沟通筛选”
- 列表新增“沟通进展”“最近消息”两列
- 问诊详情消息头部显示消息总数、未读数、最近更新时间
- 医生发送的消息在详情中可看到“患者已读 / 患者未读”

这一步完成后，医生端已经可以较直观地看出：

- 哪些问诊单有患者新消息
- 哪些问诊单最后一条消息来自患者，仍待医生回复
- 最近一条沟通大致说了什么

## 10. 当前更适合继续推进的下一步

在消息摘要和未读状态已经补齐之后，下一优先级建议收敛为：

1. 医生待办 / 跟进看板
2. 患者侧未读提醒与问诊列表状态增强
3. AI 追问消息与医患沟通消息流进一步融合
