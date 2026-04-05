# 2026-04-04 管理端 AI 采纳拆分看板开发记录

## 1. 本轮目标

在已经完成：

- 管理端 AI 采纳总览
- 最近 AI/医生差异样本
- 患者端 AI 采纳摘要展示
- 医生端 AI 草稿与 AI/医生结论对比

之后，继续把管理端统计从“只看整体”推进到“可按维度拆分复盘”，让运营和产品可以更快定位：

- 哪些科室的 AI 采纳率更高
- 哪些问诊分类更容易出现 AI 与医生结论偏差
- 当前哪些维度还缺少医生最终判断，仍处于待比较状态

## 2. 后端实现

本轮继续复用现有接口：

- `GET /api/admin/consultation-record/ai-summary`

没有新增独立统计接口，而是在原有总览响应上扩展拆分字段，降低前后端改动范围。

### 2.1 新增拆分 VO

新增文件：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiGroupVO.java`

用途：

- 统一承载单个维度分组的 AI 采纳统计结果
- 输出总量、可比较数量、一致数量、差异数量、待判断数量、覆盖文本和一致率文本

### 2.2 扩展总览响应

更新文件：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiSummaryVO.java`

新增字段：

- `departmentBreakdown`
- `categoryBreakdown`

这样管理端在请求 AI 总览时，可以一次拿到：

- 整体统计
- 最近差异样本
- 按科室拆分
- 按问诊分类拆分

### 2.3 扩展统计服务

更新文件：

- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`

本轮新增与补齐的统计逻辑包括：

- `buildBreakdown(...)`
- `normalizeGroupName(...)`
- `GroupAccumulator`

处理策略：

- 遍历问诊记录并按指定维度分组
- 优先复用最新医生结构化结论中的 `isConsistentWithAi`
- 将尚未形成可比较结论的数据归入 `pendingCount`
- 对空科室、空分类做兜底名称归一化，避免页面出现空白分组
- 按差异数、可比较数、总量排序，优先展示更值得复盘的分组

当前已输出：

- 科室拆分统计
- 问诊分类拆分统计

## 3. 前端实现

更新页面：

- `template-front/src/views/admin/ConsultationRecordPage.vue`

### 3.1 管理端新增拆分看板

在 `AI 采纳总览` 下方新增两张拆分表：

- 按科室拆分
- 按问诊分类拆分

每个维度展示：

- 分组名称
- 总量
- 可对比数量
- 一致率
- 差异数
- 待判断数

### 3.2 补齐空态与 fallback

为避免接口未返回新字段时导致页面渲染异常，本轮抽出了统一空对象：

- `createEmptyAiSummary()`

用于：

- `const aiSummary = ref(createEmptyAiSummary())`
- `loadAiSummary()` 成功回调中的兜底合并

这样即使后端暂时缺字段、返回 `null` 或部分字段缺失，管理端也仍然能稳定渲染。

### 3.3 补齐样式

本轮补充了以下样式结构：

- `breakdown-grid`
- `breakdown-card`
- `breakdown-head`

同时在响应式下把拆分看板纳入单列布局，保证中小屏下仍可阅读。

## 4. 涉及文件

### 后端

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiGroupVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiSummaryVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`

### 前端

- `template-front/src/views/admin/ConsultationRecordPage.vue`

### 文档

- `docs/2026-04-04-admin-ai-breakdown-log.md`

## 5. 验证结果

本轮已完成验证：

- 后端执行 `mvn -q -DskipTests compile` 通过
- 前端执行 `npm run build` 通过

## 6. 当前阶段结论

到这里，管理端围绕 AI 采纳效果的能力已经从“能看整体数据”继续推进到“能按业务维度拆分复盘”：

1. 可以看整体 AI 采纳概况
2. 可以看最近 AI/医生差异样本
3. 可以按科室定位 AI 采纳率与差异集中区
4. 可以按问诊分类定位更需要继续优化的场景

这为后续继续做：

- 按医生维度拆分 AI 采纳表现
- AI 偏差类型标签化分析
- 管理端 AI 效果运营看板

提供了可以直接复用的统计接口和展示骨架。
