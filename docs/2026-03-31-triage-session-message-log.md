# 2026-03-31 导诊会话与消息留痕开发记录

## 本次目标

在现有智能导诊基础建设上，继续补齐“导诊会话 / 消息留痕”能力，让系统在用户提交问诊资料后，自动生成一份可追溯的导诊会话记录，便于：

- 后续 AI 导诊接入时复用上下文
- 管理员复盘分诊依据
- 用户查看系统给出的结构化分诊结果

## 本次完成内容

### 1. 数据库结构

在 `sql/mysql57-init.sql` 中新增两张表：

- `db_triage_session`
  - 一次问诊提交对应一次导诊会话
  - 保存会话编号、患者、分类、科室、分诊等级、建议动作、摘要、消息数等快照信息
- `db_triage_message`
  - 保存会话内消息
  - 当前预置消息角色包括：`user`、`system`、`rule_engine`
  - 当前预置消息类型包括：`intake_summary`、`health_summary`、`triage_result`、`rule_summary`、`rule_hit`

### 2. 后端能力

新增后端对象与查询服务：

- `TriageSession`
- `TriageMessage`
- `TriageSessionMapper`
- `TriageMessageMapper`
- `TriageSessionVO`
- `TriageMessageVO`
- `TriageSessionQueryService`
- `TriageSessionQueryServiceImpl`

并完成以下接入：

- 在 `ConsultationServiceImpl#createRecord` 中，用户提交问诊成功后，自动写入导诊会话与消息留痕
- 会话消息当前自动生成：
  - 用户问诊摘要
  - 健康档案摘要
  - 系统分诊结果
  - 规则命中摘要
  - 每条规则命中详情
- 用户端问诊记录详情接口返回 `triageSession`
- 管理端问诊记录详情接口返回 `triageSession`

### 3. 前端展示

新增了“导诊留痕”展示区：

- 用户端 `ConsultationPage.vue`
  - 在问诊记录详情弹窗中可查看本次导诊会话及消息
- 管理端 `ConsultationRecordPage.vue`
  - 在管理详情弹窗中可查看完整导诊留痕

展示内容包括：

- 会话编号
- 会话状态
- 消息数量
- 每条摘要/结果/规则消息内容

## 设计说明

### 为什么现在先做会话留痕

因为后续 AI 导诊如果要做到：

- 复盘用户最初描述了什么
- 解释为什么分到某个科室
- 展示命中了哪些规则
- 保留后续 AI 回复上下文

就必须先有一套稳定的“会话 + 消息”底座。

### 当前版本的定位

当前这一版是“同步生成的基础留痕”，还不是完整的多轮 AI 对话系统。

也就是说，目前：

- 用户提交问诊时会自动生成一份导诊会话
- 会话里的消息由系统自动整理生成
- 暂未接入真正的多轮 AI 对话消息流

这一步完成后，后续就可以继续扩展：

- AI 多轮追问消息
- AI 最终分诊结论
- 人工修正结果
- 用户反馈

## 本次验证

已完成验证：

- 后端：`mvn -q -DskipTests compile`
- 前端：`npm run build`

均通过。

## 下一步建议

建议继续按下面顺序推进：

1. 导诊结果表
   - 将“会话消息”与“最终导诊结果”拆分管理
   - 便于后续 AI 结果、多次重算、人工修订
2. 用户反馈 / 纠错
   - 让用户标记“推荐科室是否合适”
   - 为后续 AI 优化积累反馈数据
3. AI 多轮追问
   - 基于当前 `session + message` 结构自然延展
4. 管理端导诊复盘中心
   - 提供按规则、科室、分诊等级检索与统计

## 相关文件

- `sql/mysql57-init.sql`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/TriageSessionQueryServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/TriageSessionQueryService.java`
- `template-backend/src/main/java/cn/gugufish/entity/dto/TriageSession.java`
- `template-backend/src/main/java/cn/gugufish/entity/dto/TriageMessage.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/TriageSessionVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/TriageMessageVO.java`
- `template-backend/src/main/java/cn/gugufish/mapper/TriageSessionMapper.java`
- `template-backend/src/main/java/cn/gugufish/mapper/TriageMessageMapper.java`
- `template-front/src/views/index/ConsultationPage.vue`
- `template-front/src/views/admin/ConsultationRecordPage.vue`
