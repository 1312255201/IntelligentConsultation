# 2026-04-06 医生端随访优先级优化记录
## 本轮完成

- 医生问诊列表新增“排序方式”，支持“最近提交优先”与“随访到期优先”。
- 随访到期优先排序会把“已逾期”放在最前，其次是“今日到期”“待随访”，最后才是已完成本轮或无需随访的记录。
- 医生问诊列表新增随访行高亮，逾期随访与今日到期随访在表格中可直接区分。
- 工作台汇总新增“今日到期随访”“已逾期随访”统计。
- 工作台待随访卡片新增紧急计数展示，并支持一键跳转到“待随访/已逾期 + 随访到期优先排序”的问诊列表。
- 从工作台不同待办卡片进入问诊详情时，会自动保留对应筛选上下文，方便医生关闭详情后继续批量处理。

## 前端改动

- `template-front/src/views/doctor/DoctorConsultationPage.vue`
  - 增加 `sortMode` 查询状态和排序下拉框。
  - 在现有筛选结果之上补充统一排序逻辑。
  - 新增表格行样式函数，突出逾期与今日到期记录。
  - 路由查询参数增加 `sortMode` 透传与回填。
- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`
  - 工作台统计补充 `dueTodayFollowUpCount`、`overdueFollowUpCount` 展示。
  - 待随访卡片加入紧急度标签和样式。
  - 工作台跳转问诊列表时增加 `sortMode=follow_up_due` 等上下文参数。

## 后端配合

- `DoctorWorkbenchVO` 增加：
  - `dueTodayFollowUpCount`
  - `overdueFollowUpCount`
- `DoctorWorkspaceServiceImpl.workbench(...)` 增加待随访列表的今日到期/逾期统计，并在未绑定医生时返回默认值。

## 价值

- 医生可以先处理最容易超时的随访任务，减少漏访。
- 工作台和问诊列表之间的处理路径更顺，适合连续批量处理。
- 为后续补充“逾期率”“随访完成率”“自动提醒策略”提供了稳定的列表优先级基础。

## 验证

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过
