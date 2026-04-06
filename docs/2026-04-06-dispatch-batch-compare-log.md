# 2026-04-06 智能分配批量样本对比记录

## 本轮完成

- 新增后台接口 `POST /api/admin/consultation-dispatch/batch-preview`
- 支持基于“当前页面参数”与“已保存策略”对同一批问诊样本做推荐结果对比
- 支持输出批量样本的首推是否变化、两侧候选重合数、两侧首推医生和优先分
- 后台“智能分配策略”页面新增“批量样本对比”区块
- 支持从最近样本中快速填充批量对比样本
- 支持从批量对比结果一键切回单样本对比预览

## 后端改动

- 新增批量对比请求对象：`ConsultationDispatchBatchPreviewRequestVO`
- 新增批量对比响应对象：
  - `ConsultationDispatchBatchCompareVO`
  - `ConsultationDispatchBatchCompareItemVO`
- 扩展 `ConsultationDispatchAdminService`
- `AdminConsultationDispatchController` 新增批量对比接口
- `ConsultationDispatchAdminServiceImpl` 新增批量对比实现：
  - 读取已保存策略配置
  - 对每个样本分别计算“已保存策略”和“当前参数”下的推荐医生
  - 汇总首推变化数、首推一致数、两侧均无推荐数

## 前端改动

- `ConsultationDispatchPage.vue` 新增“批量样本对比”面板
- 支持多选真实问诊样本进行批量对比
- 支持展示：
  - 样本总数
  - 首推发生变化数
  - 首推保持一致数
  - 两侧均无推荐数
  - 每个样本的已保存策略首推 / 当前参数首推 / 候选重合情况
- 支持从批量卡片直接切换到单样本对比预览

## 验证结果

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 后续建议

- 增加“按科室 / 按分类”的批量变化统计
- 增加“只看首推发生变化样本”的快捷筛选
- 为批量对比结果补充导出能力，方便调参复盘
