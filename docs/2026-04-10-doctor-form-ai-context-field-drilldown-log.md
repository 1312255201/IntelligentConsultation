# 2026-04-10 医生端表单 AI 续写字段下钻增强记录
## 1. 本轮目标

上一轮已经在管理端补齐：

- 医生端表单 AI 的上下文续写留痕
- Prompt 版本与续写效果概览

但治理视角还停留在总览层面，仍然缺少两个下钻问题：

- 哪些字段最适合使用“基于当前草稿继续改写”
- 哪些模板拼装组合在续写场景下更容易被最终保存

因此本轮继续把“上下文续写”指标下钻到：

- 字段治理排行
- 字段模板拼装排行

## 2. 本轮完成

### 2.1 字段排行新增续写指标

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageFieldVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`

字段维度新增：

- `contextRewriteCount`
- `contextRewriteSavedCount`
- `contextRewriteSaveRate`

统计口径：

- 只要该条日志命中过当前字段，且 `draftContextUsed = 1`，即计入该字段的续写次数
- 若该日志最终 `savedStatus = 1`，则计入该字段的续写保存次数
- `contextRewriteSaveRate = contextRewriteSavedCount / contextRewriteCount`

### 2.2 模板排行新增续写指标

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageTemplateVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`

模板维度新增：

- `contextRewriteCount`
- `contextRewriteSavedCount`
- `contextRewriteSaveRate`

统计口径：

- 继续沿用当前“字段 + 模板标题”组合聚合
- 在组合内部统计哪些拼装记录来自上下文续写场景
- 进一步看这些续写拼装记录有多少最终保存

### 2.3 管理端排序与展示增强

更新：

- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

本轮在管理端新增：

- 字段治理排行支持按：
  - 上下文续写数
  - 续写保存率
  排序
- 字段模板拼装排行支持按：
  - 上下文续写数
  - 续写保存率
  排序

同时在每张卡片上直接展示：

- 续写次数
- 续写保存率

这样管理端现在可以继续回答：

- 哪个字段最常被医生拿来基于当前草稿继续改写
- 哪个字段虽然续写次数不多，但续写后保存率很高
- 哪些模板拼装在续写场景下更容易被医生保留到最终表单

## 3. 设计取舍

本轮没有单独新增“续写字段排行榜”独立模块，而是继续复用：

- 字段治理排行
- 字段模板拼装排行

原因是续写并不是独立的新能力层，而是医生表单 AI 生成链路中的一个使用模式。把它放进已有排行中，更符合当前治理页结构，也方便直接与：

- 普通字段重写
- 模板拼装
- 带入次数
- 保存次数

做并排比较。

## 4. 涉及文件

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageFieldVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageTemplateVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

## 5. 验证

本轮已完成验证：

- 前端执行 `cmd /c npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 6. 下一步建议

建议下一轮继续优先推进：

1. 给字段治理排行增加“仅看续写场景”的快速筛选
2. 在 Prompt 版本面板中追加 `v1 / v2` 分组对比文案，进一步突出升级收益
3. 继续补“续写后保存内容与 AI 草稿差异”的自动 diff 视图
