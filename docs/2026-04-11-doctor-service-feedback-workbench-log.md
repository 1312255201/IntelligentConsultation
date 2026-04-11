# 2026-04-11 医生工作台服务评价概览开发记录

## 本轮目标

在患者端已经支持提交服务评价、患者提醒闭环已经打通的基础上，继续补齐医生侧的反馈可视化，让“问诊完成 -> 患者评价 -> 医生回看服务反馈”形成可运营的闭环。

本轮仍然优先完善在线问诊主流程，不继续扩展新的 AI 能力。

## 本轮完成

- 医生工作台汇总接口新增服务评价统计字段
- 医生工作台新增“服务评价概览”面板
- 医生工作台支持展示近期服务评价列表
- 医生侧可直接从服务评价卡片跳转到对应问诊详情
- 工作台默认摘要对象同步补齐服务评价统计占位字段

## 后端改动

更新文件：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorWorkbenchVO.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationServiceFeedbackQueryService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceFeedbackQueryServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

实现说明：

- 医生工作台返回结构新增服务评价相关统计字段：
  - `serviceFeedbackCount`
  - `resolvedServiceFeedbackCount`
  - `unresolvedServiceFeedbackCount`
  - `lowScoreServiceFeedbackCount`
  - `averageServiceScore`
  - `resolvedServiceFeedbackRate`
  - `recentServiceFeedbacks`
- 服务评价查询服务新增按医生查询反馈列表能力
- 医生工作台汇总逻辑新增对服务评价的聚合计算
- 近期服务评价列表按最新更新时间倒序返回，便于医生优先关注最新反馈

## 前端改动

更新文件：

- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`
- `template-front/src/components/WorkspaceShell.vue`

实现说明：

- 医生工作台新增服务评价概览面板
- 面板中展示累计评价、平均评分、未解决评价、低分评价、问题解决率
- 近期服务评价列表展示：
  - 患者名称
  - 问诊编号
  - 服务评分
  - 是否解决
  - 评价内容
  - 评价时间
- 低分或未解决评价会在卡片样式上做强调
- 点击“查看问诊”可直接进入医生问诊页继续查看详情

## 用户价值

- 医生可以快速回看患者在问诊结束后的服务反馈
- 低分评价和未解决问题能在工作台首页被更早发现
- 在线问诊流程从“完成处置”进一步延伸到“服务反馈回流”

## 下一步建议

- 在医生待办中心加入“待关注服务评价”分组，优先收敛低分且未解决的记录
- 在管理端补充服务评价质量看板，支持按科室、医生做统计分析
- 在问诊详情中增加针对低分评价的回访记录或处理备注能力
