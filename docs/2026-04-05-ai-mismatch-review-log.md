# 2026-04-05 医生侧 AI 差异原因留痕与管理端汇总开发记录

## 1. 本轮目标

在已经完成：

- 医生侧 AI 接诊草稿
- 医生侧 AI/医生结论对比
- 管理端 AI 采纳总览
- 管理端按科室与分类拆分

之后，继续把“AI 采纳分析”从只有结果推进到“能看到原因”：

- 医生在提交结构化结论时，不再只选择“与 AI 一致/不一致”
- 当医生认为与 AI 不一致时，可补充结构化差异原因和说明
- 管理端可以汇总最常见的 AI 偏差原因，便于后续优化 Prompt、规则和流程

## 2. 数据结构与 SQL

本轮扩展表：

- `db_consultation_doctor_conclusion`

新增字段：

- `ai_mismatch_reasons_json`
- `ai_mismatch_remark`

更新文件：

- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-03-31-doctor-conclusion.sql`

新增升级脚本：

- `sql/mysql57-upgrade-2026-04-05-ai-mismatch-review.sql`

设计说明：

- 差异原因使用 JSON 数组保存，便于后续做统计聚合
- 补充说明单独存文本，便于保留医生的自由描述
- 仍然复用现有 `db_consultation_doctor_conclusion`，不额外新建复杂审计表，先用低风险方式沉淀数据

## 3. 后端实现

### 3.1 新增差异原因工具

新增文件：

- `template-backend/src/main/java/cn/gugufish/utils/ConsultationAiMismatchReasonUtils.java`

作用：

- 统一维护 AI 差异原因 code 与 label
- 统一做原因列表的合法化、去重、截断和 JSON 转换
- 为医生提交保存和管理端统计汇总提供同一套口径

当前内置原因包括：

- 补充信息后判断调整
- 结合临床经验调整
- 需结合线下检查结果
- 出于安全考虑调整
- 结合患者意愿与依从性
- 其他原因

### 3.2 扩展医生处理提交链路

更新文件：

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationHandleSubmitVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/dto/ConsultationDoctorConclusion.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationDoctorConclusionVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

新增提交字段：

- `aiMismatchReasons`
- `aiMismatchRemark`

处理策略：

- 当 `isConsistentWithAi != 0` 时，后端自动清空差异原因与说明，避免残留旧数据
- 当医生完成处理且选择“与 AI 不一致”时，要求至少满足以下其一：
  - 选择一个差异原因
  - 填写补充说明
- 保存结构化结论时，将原因列表转为 JSON 存入结论表

### 3.3 扩展管理端统计摘要

更新文件：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiSummaryVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiMismatchVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`

新增文件：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiReasonVO.java`

扩展内容：

- `ai-summary` 响应新增 `mismatchReasonBreakdown`
- 最近差异记录新增：
  - `mismatchReasonCodes`
  - `mismatchRemark`

统计逻辑：

- 只统计最新医生结构化结论中 `isConsistentWithAi = 0` 的记录
- 优先按医生显式标记的差异原因汇总
- 若医生只填写说明、未选原因，则兜底计入 `other`

## 4. 前端实现

### 4.1 扩展 AI 对比前端 helper

更新文件：

- `template-front/src/triage/comparison.js`

新增能力：

- `aiMismatchReasonOptions`
- `aiMismatchReasonLabel`

用于统一：

- 医生侧差异原因选项
- 管理端差异原因文本展示

### 4.2 医生侧新增差异原因留痕

更新文件：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

新增内容：

- 结构化结论表单新增“与 AI 不一致原因”多选
- 新增“差异补充说明”文本框
- 对比区块中显示当前已选差异原因与说明
- 提交完成处理时增加前端校验，避免只标记“不一致”却没有任何原因信息

### 4.3 管理端新增差异原因汇总

更新文件：

- `template-front/src/views/admin/ConsultationRecordPage.vue`

新增内容：

- `AI 采纳总览` 下新增“差异原因汇总”卡片
- 最近差异记录卡新增原因标签与补充说明展示
- 单条记录详情里的结构化结论区新增差异原因展示

这样管理端已经可以同时看到：

- 整体采纳情况
- 拆分维度表现
- 最近差异样本
- 差异为什么发生

## 5. 涉及文件

### 后端

- `template-backend/src/main/java/cn/gugufish/entity/dto/ConsultationDoctorConclusion.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationHandleSubmitVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationDoctorConclusionVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiSummaryVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiMismatchVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiReasonVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/utils/ConsultationAiMismatchReasonUtils.java`

### 前端

- `template-front/src/triage/comparison.js`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/admin/ConsultationRecordPage.vue`

### SQL

- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-03-31-doctor-conclusion.sql`
- `sql/mysql57-upgrade-2026-04-05-ai-mismatch-review.sql`

### 文档

- `docs/2026-04-05-ai-mismatch-review-log.md`

## 6. 验证结果

本轮已完成验证：

- 后端执行 `mvn -q -DskipTests compile` 通过
- 前端执行 `npm run build` 通过

## 7. 当前阶段结论

到这里，项目围绕 AI 效果闭环已经从“能比较 AI 与医生结论是否一致”继续推进到“能沉淀 AI 差异原因”：

1. 医生可以在提交结论时结构化标记与 AI 的差异原因
2. 系统可以保留医生的补充说明，便于后续复盘
3. 管理端可以看到差异原因的汇总分布
4. 后续优化 Prompt、规则和流程时，已经有了更具体的数据抓手

下一步更适合继续推进：

- 管理端按医生维度拆分 AI 采纳与差异原因
- AI 差异原因与具体字段偏差联动分析
- AI 纠偏闭环，例如“已优化/待优化/无需优化”的运营标记

## 8. 本轮继续开发补充

围绕上一轮遗留问题和下一步方向，继续完成了两项收口与扩展：

- 清理医生端页面中的重复“AI 差异原因/差异补充说明”输入区
- 管理端新增按医生维度的 AI 采纳与差异原因统计

### 8.1 医生端页面收口

更新文件：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

处理内容：

- 删除误放在“医生处理”表单中的重复差异原因录入区
- 保留“结构化结论”中的正式录入入口，避免同一批字段在页面中出现两次
- 这样医生填写路径更清晰，也能减少后续继续扩展结论表单时的混淆

### 8.2 管理端新增按医生维度复盘

更新文件：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiSummaryVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiDoctorVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`
- `template-front/src/views/admin/ConsultationRecordPage.vue`

新增能力：

- `ai-summary` 响应新增 `doctorBreakdown`
- 按医生聚合其已提交的结构化结论，统计：
- 结论数
- 已对比数
- AI 一致率
- 差异数
- 该医生最常见的差异原因
- 管理端“AI 采纳总览”中新增“按医生拆分”卡片

实现说明：

- 继续复用现有 `ai-summary` 接口，不额外新增新接口
- 统计口径基于最新结构化结论，而不是单纯按认领记录计算
- 医生维度的差异原因复用已有 `AdminConsultationAiReasonVO`
- 若某条差异记录只有备注、没有勾选结构化原因，仍按 `other` 兜底纳入统计

这样管理端当前已经可以从四个角度复盘 AI 采纳情况：

- 按科室拆分
- 按问诊分类拆分
- 按医生拆分
- 差异原因汇总
