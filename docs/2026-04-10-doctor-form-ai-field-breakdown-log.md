# 2026-04-10 医生表单 AI 字段维度排行开发记录

## 1. 本轮目标

在上一轮里，医生端表单 AI 已经可以统计：

- 整份草稿生成
- 字段级 `AI重写`
- `AI+模板拼装`
- 最终保存采纳

但管理端还缺少一个更细的视角：

- 到底哪些字段最常被 `AI重写`
- 哪些字段最常被 `AI+模板拼装`
- 这些字段被触达后，后续有没有形成带入和保存

所以本轮继续在管理端补“字段维度排行”。

## 2. 本轮完成

### 2.1 表单 AI 使用概览新增字段排行结构

新增：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageFieldVO.java`

并更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageOverviewVO.java`

在表单 AI 使用概览中新增：

- `fieldBreakdown`

每个字段排行项包括：

- `fieldKey`
- `fieldLabel`
- `sceneType / sceneLabel`
- `regenerateCount`
- `templateComposeCount`
- `appliedCount`
- `savedCount`

### 2.2 后端按字段聚合重写 / 拼装 / 带入 / 保存

更新：

- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`

本轮新增了字段级统计聚合逻辑，覆盖以下字段：

- 判断摘要
- 处理建议
- 随访安排
- 患者指导
- 随访摘要
- 随访建议
- 下一步安排

聚合口径如下：

- `regenerateCount`
  - 统计该字段被字段级 `AI重写` 的次数
- `templateComposeCount`
  - 统计该字段被 `AI+模板拼装` 的次数
- `appliedCount`
  - 统计围绕该字段的重写或拼装日志，后续形成带入的次数
- `savedCount`
  - 统计围绕该字段的重写或拼装日志，后续形成保存的次数

这层统计是独立于“场景维度概览”的补充，重点回答字段层问题。

### 2.3 管理端页面展示字段排行

更新：

- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

在“医生端 AI 处理 / 随访草稿使用”区域中，新增了字段排行卡片。

每个字段会展示：

- 字段名
- 重写次数
- 拼装次数
- 带入次数
- 保存次数

这样管理员打开页面后，不需要切换页面，就能快速判断：

- 医生目前更依赖 AI 去写哪一类字段
- 哪些字段经常需要再拼模板
- 哪些字段即便经常重写，最终保存转化仍然偏低

## 3. 设计取舍

本轮没有把字段排行做成独立新页面，而是直接放在已有：

- `ConsultationAiConfigPage.vue`

原因是这部分本质上还是“医生端 AI 助理治理”视角，不值得为了单一排行再拆新入口。

同时本轮没有新增数据库字段，而是完全复用已有：

- `regenerate_field`
- `template_scene_type`
- `apply_count`
- `saved_status`

也就是说，字段排行是基于现有日志能力做的一层聚合统计，而不是新起一套采集链路。

## 4. 涉及文件

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageFieldVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageOverviewVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

## 5. 验证

本轮已完成验证：

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 6. 下一步建议

建议下一轮继续优先推进：

1. 为字段排行补导出能力，方便进一步离线分析
2. 增加“字段保存率”或“字段带入率”排序方式，快速识别高价值字段
3. 把字段排行和模板标题继续联动，下钻到“哪条模板最常参与该字段拼装”
