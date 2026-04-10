# 2026-04-09 医生处理与随访 AI 草稿开发记录

## 1. 本轮目标

继续沿着智能问诊系统里的“AI 导诊 -> 医生接管 -> 医生处理 -> 持续随访”主线推进。

在上一轮已经完成“医生沟通消息 AI 草稿”之后，本轮补齐医生侧另外两个核心工作面：

- 医生处理表单 AI 草稿
- 医生随访表单 AI 草稿

目标是不只让医生能和患者快速沟通，也能在真正保存处理结果和追加随访时，拿到一份可编辑、可兜底的结构化草稿。

## 2. 后端能力

### 2.1 新增两个医生侧 AI 草稿接口

新增请求对象：

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationAiDraftGenerateVO.java`

新增返回对象：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorConsultationHandleDraftVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorConsultationFollowUpDraftVO.java`

更新：

- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorWorkspaceService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

新增接口：

- `POST /api/doctor/consultation/handle/ai-draft`
- `POST /api/doctor/consultation/follow-up/ai-draft`

两个接口都只需要传：

- `recordId`

### 2.2 医生处理 AI 草稿支持 DeepSeek + 规则兜底

处理草稿会复用医生工作台现有问诊聚合能力，读取：

- 主诉
- 健康摘要
- 问诊答案摘要
- AI 导诊结论
- 风险标签
- 最近沟通消息
- 已保存医生处理
- 已保存结构化结论
- 历史随访记录

返回内容包括：

- `doctorSummary`
- `medicalAdvice`
- `followUpPlan`
- `conditionLevel`
- `disposition`
- `diagnosisDirection`
- `conclusionTags`
- `needFollowUp`
- `followUpWithinDays`
- `patientInstruction`
- `generationSummary`
- `riskFlags`
- `promptVersion / source / fallback`

如果 DeepSeek 调用失败，后端会自动回退到规则草稿，不会让医生端入口失效。

### 2.3 医生随访 AI 草稿支持 DeepSeek + 规则兜底

随访草稿会综合：

- 当前问诊主诉
- 已保存医生处理和结构化结论
- 历史随访记录
- 最近患者反馈
- 最近沟通记录
- 风险标签

返回内容包括：

- `followUpType`
- `patientStatus`
- `summary`
- `advice`
- `nextStep`
- `needRevisit`
- `nextFollowUpDate`
- `generationSummary`
- `riskFlags`
- `promptVersion / source / fallback`

规则兜底里还补了几层基础判断：

- 从最近患者反馈里粗略识别“好转 / 稳定 / 加重”
- 自动推断是否还需要继续随访
- 自动补一个合法的下次随访日期

## 3. 前端能力

更新：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

### 3.1 医生处理区新增独立 AI 草稿面板

现在“医生处理”区块里新增：

- `生成 AI 草稿`
- `带入处理草稿`
- `带入结构化结论`

页面会展示：

- 处理摘要
- 处理建议
- 随访安排
- 患者提示
- 结构化结论建议
- 风险标签
- Prompt 版本
- DeepSeek / 规则兜底来源

### 3.2 随访区新增独立 AI 草稿面板

现在“随访记录”区块里新增：

- `生成 AI 草稿`
- `带入随访草稿`

页面会展示：

- 随访摘要
- 随访建议
- 下一步安排
- 随访方式
- 患者状态
- 是否继续随访
- 下次随访日期
- 风险标签

### 3.3 页面状态补充重置

为避免旧草稿串到下一条问诊，本轮也补了两套状态重置：

- 切换问诊详情时清空处理 AI 草稿
- 关闭详情抽屉时清空随访 AI 草稿

## 4. 顺手修复

本轮顺手修复了一条原本会在前后端都显示乱码的提示文案：

- “与 AI 不一致时请至少选择一个差异原因或填写补充说明”

## 5. 涉及文件

### 后端

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationAiDraftGenerateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorConsultationHandleDraftVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorConsultationFollowUpDraftVO.java`
- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorWorkspaceService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

### 前端

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

## 6. 验证

本轮已完成验证：

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 7. 下一步建议

建议下一轮优先继续推进以下方向之一：

1. 给“医生处理 AI 草稿”和“随访 AI 草稿”补使用留痕与管理端治理统计
2. 把医生个人模板系统继续接到这两类草稿里，支持 `AI + 模板` 拼装
3. 把患者最近新增反馈做更细的结构化提炼，提升随访草稿质量
4. 在医生工作台增加“待优先处理 / 待优先随访”的 AI 推荐视图
