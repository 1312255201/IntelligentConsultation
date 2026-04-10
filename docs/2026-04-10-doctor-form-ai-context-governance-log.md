# 2026-04-10 医生端表单 AI 续写留痕与 Prompt 治理增强记录
## 1. 本轮目标

上一轮已经完成医生端“基于当前草稿继续改写”能力，但还缺两个治理层面的闭环：

- 后台无法判断某次 AI 草稿是否使用了“当前草稿上下文”
- 管理端无法从日志侧观察 `v1 / v2 Prompt` 的生成、带入与保存效果差异

因此本轮继续补齐：

- 续写留痕
- Prompt 版本治理
- 初始化 SQL 与运行中升级脚本同步

## 2. 本轮完成

### 2.1 医生表单 AI 日志新增续写留痕字段

更新：

- `template-backend/src/main/java/cn/gugufish/entity/dto/DoctorFormAiLog.java`

新增字段：

- `draftContextUsed`
- `rewriteRequirement`

含义：

- `draftContextUsed`：本次生成是否携带了当前草稿上下文
- `rewriteRequirement`：医生补充的简短改写要求

### 2.2 生成链路写入续写留痕

更新：

- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

本轮将医生端处理草稿、随访草稿生成过程中的上下文信息落入日志：

- 当 `rewriteRequirement` 或 `currentDraftJson` 任一存在时，记为使用了续写上下文
- 在保存 `db_doctor_form_ai_log` 时同步记录：
  - `draft_context_used`
  - `rewrite_requirement`

这样后续就能区分：

- 首次整份生成
- 普通字段重写
- 基于当前草稿的上下文续写

### 2.3 管理端医生表单 AI 使用概览新增续写与 Prompt 统计

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageOverviewVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageItemVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsagePromptVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`

新增概览指标：

- `contextRewriteCount`
- `contextRewriteSavedCount`
- `contextRewriteSaveRate`
- `promptBreakdown`

同时近期记录项中补充：

- `promptVersion`
- `draftContextUsed`
- `rewriteRequirement`

### 2.4 管理端页面新增 Prompt 版本与续写效果面板

更新：

- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

增强点包括：

- 医生端 AI 处理/随访草稿使用概览新增：
  - 上下文续写次数
  - 续写已保存次数
  - 续写保存率
- 审计摘要新增“上下文续写”统计提示
- 新增“Prompt 版本与续写效果”面板
  - 支持按生成次数、上下文续写数、字段重写数、带入次数、保存次数、带入率、保存率排序
- 最近使用记录中新增：
  - Prompt 版本标签
  - “基于当前草稿续写”标签
  - 改写要求展示

这样管理端现在可以直接观察：

- `doctor-handle-v1 / v2`
- `doctor-follow-up-v1 / v2`
- 各版本在上下文续写场景下的真实保存效果

### 2.5 初始化 SQL 与升级脚本同步

更新：

- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-04-10-doctor-form-ai-context-rewrite.sql`

本轮顺手修正了初始化表结构与当前代码使用字段之间的偏差：

- `db_doctor_form_ai_log` 初始化表新增 `regenerate_field`
- 同步新增：
  - `draft_context_used`
  - `rewrite_requirement`

并补充升级脚本，保证已有环境可以平滑加字段。

## 3. 设计取舍

### 3.1 当前只记录“是否使用上下文”，不落完整草稿快照

本轮没有把 `currentDraftJson` 全量落库，而是只保留：

- 是否使用上下文
- 医生补充的改写要求

原因是：

- 当前草稿快照体积较大
- 其中包含医生实时编辑内容，后续是否长期保留需要再评估
- 本轮更优先的是先建立治理可观测性

### 3.2 Prompt 治理先按版本聚合，不额外开新页面

本轮没有新增独立“Prompt 版本中心”，而是继续落在：

- `ConsultationAiConfigPage.vue`

原因是当前医生端表单 AI 治理仍处于“可见性增强”阶段，放在已有 AI 配置治理页中更符合现有项目入口与节奏。

## 4. 涉及文件

- `template-backend/src/main/java/cn/gugufish/entity/dto/DoctorFormAiLog.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageOverviewVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageItemVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsagePromptVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`
- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-04-10-doctor-form-ai-context-rewrite.sql`

## 5. 验证

本轮已完成验证：

- 前端执行 `cmd /c npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 6. 下一步建议

建议下一轮继续优先推进：

1. 在字段治理排行中继续下钻“带上下文续写”的字段保存率，识别最适合做续写的字段
2. 给管理端增加 `v1 vs v2` 对比视图，进一步突出 Prompt 升级收益
3. 评估是否需要把医生最终保存内容与 AI 草稿差异做自动 diff，用于续写质量复盘
