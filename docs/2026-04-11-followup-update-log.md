# 2026-04-11 患者恢复更新与医生随访闭环开发记录

## 本次目标

围绕在线问诊主流程，补齐“患者补充恢复情况 -> 医生查看并继续随访”的闭环能力，不依赖 AI 也能独立完成核心流程。

## 已完成内容

### 1. 后端消息语义增强

- 在 `ConsultationMessageSendVO` 中补充 `sceneType`
- 复用原有问诊消息接口 `/api/user/consultation/message/send`
- 当 `sceneType` 为 `followup_update` 时，消息会以“恢复更新”语义保存
- 对患者恢复更新增加待随访校验：
  - 问诊需已完成医生处理
  - 医生结论需仍处于需要随访状态
  - 若最近一条随访记录已标记无需再随访，则不允许继续按恢复更新提交

### 2. 患者端恢复更新入口

- 在 [ConsultationPage.vue](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-front/src/views/index/ConsultationPage.vue) 的问诊详情中新增恢复更新面板
- 用户可选择恢复状态、填写当前变化、补充仍需帮助的问题
- 支持两种操作：
  - 直接发送恢复更新
  - 带入消息输入框后继续补充图片等资料
- 发送后会刷新问诊消息和详情，形成可追踪的随访沟通记录

### 3. 医生端随访承接

- 在 [DoctorConsultationPage.vue](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-front/src/views/doctor/DoctorConsultationPage.vue) 中：
  - 为患者恢复更新消息增加明显标记
  - 在随访区域展示最近一条患者恢复更新
  - 支持一键将患者恢复更新追加到随访摘要，减少医生重复整理

## 当前效果

- 患者可以在待随访问诊中主动补充恢复情况
- 医生进入问诊详情后能快速识别这是恢复更新，而不是普通聊天消息
- 医生登记随访时可以直接吸收患者最新恢复反馈

## 验证

- 前端：`cmd /c npm run build` 通过
- 后端：`mvn -q -DskipTests compile` 通过

## 后续建议

1. 在医生问诊列表和提醒页继续强化“患者恢复更新”优先级展示
2. 为恢复更新补充图片/检验结果分类标签
3. 将恢复更新进一步纳入 AI 随访草稿上下文
