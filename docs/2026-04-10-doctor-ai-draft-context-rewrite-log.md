# 2026-04-10 医生端 AI 草稿基于当前草稿继续改写开发记录
## 1. 本轮目标

此前医生端已经支持：

- 生成处理草稿与随访草稿
- 按字段进行 `AI重写`
- 处理草稿/随访草稿带入表单

但在实际使用中还有两个明显问题：

- 医生已经改过一版草稿后，再次点击 AI 重写时，模型看不到当前编辑中的内容
- 字段级重写虽然有“目标字段聚焦”规则，但由于缺少当前草稿上下文，仍可能让其他字段发生不必要漂移

因此本轮重点补齐：

- 基于当前草稿继续改写
- 医生可补充简短改写要求
- 后端 Prompt 明确读取当前草稿快照，尽量只改必要部分

## 2. 本轮完成

### 2.1 后端请求扩展

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationAiDraftGenerateVO.java`

新增可选字段：

- `rewriteRequirement`
- `currentDraftJson`

其中：

- `rewriteRequirement` 用于接收医生本次的补充改写要求
- `currentDraftJson` 用于接收当前页面草稿快照，作为 DeepSeek prompt 的上下文输入

### 2.2 医生端处理 / 随访 Prompt 接入当前草稿上下文

更新：

- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

本轮对处理草稿与随访草稿生成链路都做了增强：

- 在 `generateConsultationHandleDraft(...)`、`generateConsultationFollowUpDraft(...)` 中读取并规范化 `rewriteRequirement`、`currentDraftJson`
- 在 `buildDoctorHandleUserPrompt(...)`、`buildDoctorFollowUpUserPrompt(...)` 中追加“当前草稿快照 + 改写要求”上下文块
- 新增 helper：
  - `normalizeRewriteRequirement(...)`
  - `normalizeCurrentDraftJson(...)`
  - `buildHandleRewriteContextBlock(...)`
  - `buildFollowUpRewriteContextBlock(...)`

Prompt 口径上新增了两层约束：

- System Prompt：如果用户补充了当前草稿快照，优先沿用已确认内容、结构和语气
- User Prompt：明确说明 `editingDraft / conclusionDraft / followUpDraft / currentAiDraft` 的含义，告诉模型哪些内容是医生当前正在编辑的真实草稿

同时将医生端处理草稿、随访草稿的 Prompt 版本从 `v1` 提升到了 `v2`，方便后续在日志和治理面板中识别这次上下文续写能力的效果。

### 2.3 医生端页面新增“继续改写要求”

更新：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

在“AI 处理草稿”“AI 随访草稿”区域中各新增一个轻量输入框：

- 继续改写要求（可选）

前端新增能力：

- 维护 `handleAiRewriteRequirement`、`followUpAiRewriteRequirement`
- 生成请求时自动构造当前草稿快照 JSON
- 同时把当前编辑表单和上一版 AI 草稿都传给后端

其中当前草稿快照包含：

- 处理场景：
  - `editingDraft`
  - `conclusionDraft`
  - `currentAiDraft`
- 随访场景：
  - `followUpDraft`
  - `currentAiDraft`

并额外标识：

- `regenerateField`
- `rewriteMode`

### 2.4 医生端生成提示优化

本轮没有只停留在“把参数传过去”，还顺手优化了医生端成功提示语：

- 普通整份生成
- 基于当前草稿继续生成
- 字段级重写
- 结合当前草稿的字段级续写
- 规则兜底但仍结合当前草稿

这样医生能更清楚地感知当前这次 AI 生成到底是“首次生成”还是“在上一版基础上继续改写”。

## 3. 设计取舍

### 3.1 当前草稿快照先不入库

这轮没有改 `db_doctor_form_ai_log` 结构，也没有新增数据库字段来存：

- `rewriteRequirement`
- `currentDraftJson`

原因是本轮优先目标是把“续写体验”跑通，先验证链路和 prompt 效果。

后续如果要做更深入治理，可再考虑：

- 记录本次是否使用了当前草稿上下文
- 记录改写要求关键词
- 评估“字段重写 + 当前草稿续写”后的保存率提升

### 3.2 当前草稿快照由前端实时构造

没有让后端重新推断“当前草稿”，而是直接由医生页把当前真实编辑内容序列化传给后端。

这样做的好处是：

- 可以覆盖尚未保存到数据库的医生实时修改
- 能准确表达医生当前正在编辑的真实意图
- 不需要额外查询临时草稿表或引入新的存储结构

## 4. 涉及文件

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationAiDraftGenerateVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`

## 5. 验证

本轮已完成验证：

- 前端执行 `cmd /c npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 6. 下一步建议

建议下一轮继续优先推进：

1. 给医生端 AI 续写日志增加“是否使用当前草稿上下文”的留痕，方便后续治理分析
2. 在管理端补充“字段重写 v2 Prompt”保存率对比，验证这轮续写能力是否确实降低了字段漂移
3. 进一步补“仅改写选中字段并锁定其他字段”的更强约束模式，减少复杂问诊下的联动改写
