# 2026-04-10 医生表单 AI 字段模板下钻开发记录

## 1. 本轮目标

上一轮已经完成：

- 医生表单 AI 字段治理排行
- 字段级相关日志数、带入率、保存率
- 管理端字段排行的排序与场景筛选

但治理视角还缺一层关键下钻：

- 只能知道哪个字段常被 AI + 模板拼装
- 还不知道具体是哪条模板最常参与该字段拼装

因此本轮继续在现有治理页中补充“字段 + 模板标题”层的拼装排行。

## 2. 本轮完成

### 2.1 后端新增模板下钻响应结构

新增：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageTemplateVO.java`

并更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageOverviewVO.java`

表单 AI 使用概览新增：

- `templateBreakdown`

每个模板下钻项包含：

- `fieldKey / fieldLabel`
- `sceneType / sceneLabel`
- `templateSceneType`
- `templateId`
- `templateTitle`
- `templateLabel`
- `composeCount`
- `appliedCount`
- `savedCount`
- `applyRate`
- `saveRate`

### 2.2 后端聚合“字段 + 模板标题”组合排行

更新：

- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`

聚合口径：

- 仅统计 `templateUsed = 1` 的医生表单 AI 日志
- 使用 `templateSceneType` 映射出对应字段
- 以“字段 + 模板 ID + 模板标题”作为组合维度
- 统计模板参与拼装次数、带入次数、保存次数
- 输出模板保存率，方便识别高频且高转化的模板

模板标题缺失时，后端会用模板场景类型生成一个可读兜底标签，避免管理端出现空白项。

### 2.3 管理端新增字段模板拼装排行

更新：

- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

在“医生端 AI 处理 / 随访草稿使用”区域中，新增第二层治理卡片：

- 字段模板拼装排行

交互包括：

- 复用现有场景筛选
- 新增字段筛选
- 支持按以下指标排序
  - 拼装次数
  - 保存次数
  - 保存率
  - 带入次数

每个组合卡片展示：

- 字段名
- 场景名
- 模板标题
- 当前排序主指标
- 拼装次数 / 带入次数 / 保存次数 / 保存率

这样管理端就能继续回答：

- 哪个字段最常依赖模板拼装
- 哪条模板最常参与某个字段
- 哪些模板参与频率高但保存转化低，值得继续优化

## 3. 设计取舍

本轮没有单独新开一个模板治理页面，而是继续放在：

- `ConsultationAiConfigPage.vue`

原因是这层能力仍然属于医生端表单 AI 治理的下钻分析，不值得为了一个维度单独拆入口。

同时，本轮模板排行按“字段 + 模板标题”聚合，而不是只按模板标题聚合：

- 因为同一模板可能服务不同字段
- 字段维度能直接对接当前治理主线
- 也更方便后续继续往“字段重写 -> 模板拼装 -> 保存采纳”链路延展

## 4. 涉及文件

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageTemplateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageOverviewVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

## 5. 验证

本轮已完成验证：

- 前端执行 `cmd /c npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 6. 下一步建议

建议下一轮继续优先推进：

1. 给字段模板拼装排行增加导出能力，便于离线分析。
2. 给模板下钻补“最近命中样本”，帮助快速回看真实问诊。
3. 医生端补“基于当前草稿继续改写”，降低字段级重写时其他字段漂移。
