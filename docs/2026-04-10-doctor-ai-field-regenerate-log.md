# 2026-04-10 医生处理 / 随访草稿字段级 AI 重写开发记录

## 1. 本轮目标

在已有医生端 AI 助理能力中，处理草稿和随访草稿已经支持：

- 整份草稿生成
- 一键带入表单
- `AI+模板拼装`
- 使用留痕与治理统计

但医生实际使用时，经常只想局部调整某一个字段，例如：

- 只重写处理建议
- 只重写患者提示
- 只重写随访建议
- 只重写下一步安排

因此本轮继续沿着医生端 AI 助理主线，补齐“按字段重新生成”。

## 2. 本轮完成

### 2.1 草稿生成接口支持目标字段

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationAiDraftGenerateVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

现有接口继续复用：

- `POST /api/doctor/consultation/handle/ai-draft`
- `POST /api/doctor/consultation/follow-up/ai-draft`

新增可选参数：

- `regenerateField`

处理草稿支持：

- `doctor_summary`
- `medical_advice`
- `follow_up_plan`
- `patient_instruction`

随访草稿支持：

- `followup_summary`
- `followup_advice`
- `followup_next_step`

这样前端不需要再新增一组接口，仍沿用现有草稿生成入口。

### 2.2 Prompt 支持“聚焦重写某字段”

本轮没有把字段重写做成只返回单字段，而是继续返回完整草稿，但会在 Prompt 中显式告知：

- 当前重点重写哪个字段
- 该字段要更贴合上下文、更具体、更可执行
- 其他字段仍要返回，但除非确有必要，不要无关大改

这样做的原因是：

- 不破坏当前整份草稿的使用方式
- 不需要新增单字段返回结构
- 仍然能保持现有草稿日志与带入链路

同时本轮还将 Prompt 版本细化到了字段维度，例如：

- `doctor-handle-medical_advice-v1`
- `doctor-follow-up-followup_next_step-v1`

便于后续继续观察不同字段重写效果。

### 2.3 医生端页面新增字段级 `AI重写`

更新：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

在以下字段模板工具条中，新增了 `AI重写`：

- 医生判断摘要
- 处理建议
- 随访计划
- 患者指导
- 随访摘要
- 随访建议
- 下一步安排

点击后会：

1. 调用原有草稿生成接口
2. 传入当前字段对应的 `regenerateField`
3. 返回一份新的完整草稿
4. 用新的草稿结果刷新当前 AI 草稿区

页面交互上补了轻量 loading 状态，避免整份草稿生成和字段重写同时触发。

### 2.4 设计取舍

本轮没有做成“只返回一个字段”的接口，而是保留“返回完整草稿”的模式。

主要考虑：

- 现有页面、日志、带入逻辑都围绕整份草稿展开
- 改成单字段返回会让日志归属和带入追踪复杂度明显上升
- 当前方案更适合在现有项目风格里持续演进

因此现在的字段级 `AI重写` 本质上是：

- 按字段聚焦提示
- 返回完整草稿
- 优先重写目标字段
- 其余字段尽量保持稳定

## 3. 涉及文件

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationAiDraftGenerateVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`

## 4. 验证

本轮已完成验证：

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 5. 下一步建议

建议下一轮继续优先推进：

1. 为字段级 `AI重写` 增加“基于当前草稿继续改写”的上下文输入，进一步降低其他字段漂移
2. 将字段重写行为也补入管理端治理统计，区分整份草稿生成与字段级重写
3. 给处理建议、随访建议、下一步安排增加“多候选草稿”能力，方便医生快速选择
