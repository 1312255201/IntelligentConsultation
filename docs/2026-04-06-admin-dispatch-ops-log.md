# 2026-04-06 管理端智能分配运营看板开发记录

## 1. 本轮目标

在已经完成：

- 管理端智能分配状态归档
- 患者端智能分配进度展示
- 医生端“系统推荐给我”与智能分配筛选

之后，继续把后台能力从“看得到状态”推进到“看得出效果”，让运营可以直接回答下面几个问题：

- 当前有多少问诊真正进入了智能推荐链路
- 已经被接手的推荐单里，有多少是由首推医生接手
- 有多少推荐单最终转给了其他医生
- 从问诊提交到医生接手，大致需要多久
- 哪些首推医生目前积压更多待接手问诊

## 2. 后端实现

### 2.1 新增智能分配运营摘要接口

更新：

- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationRecordController.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationRecordAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`

新增接口：

- `GET /api/admin/consultation-record/smart-dispatch-summary`

这一层不新增数据库表，继续复用已有：

- 问诊记录
- 最新分诊结果
- 最新医生认领记录

统一聚合出管理端运营摘要。

### 2.2 新增运营摘要 VO

新增：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationDispatchSummaryVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationDispatchDoctorVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationDispatchWaitVO.java`

主要输出内容包括：

- 推荐覆盖问诊数
- 等待首推医生数量
- 首推医生接手数量
- 其他医生承接数量
- 科室分诊队列数量
- 超 24 小时待接手数量
- 首推命中率
- 转其他医生率
- 平均接手时长
- 首推医生维度的承接拆分
- 长时间待接手问诊列表

### 2.3 聚合策略

本轮聚合继续复用 `ConsultationSmartDispatchUtils`，统一以 `smartDispatch.status` 为状态来源。

关键口径如下：

- `首推命中率`
  - 分母：已被接手的推荐单
  - 分子：由首推医生接手的推荐单
- `转其他医生率`
  - 分母：已被接手的推荐单
  - 分子：由其他医生承接的推荐单
- `平均接手时长`
  - 统计有明确推荐且已经被接手的问诊
  - 使用问诊创建时间到认领时间的时长
- `超 24h 待接手`
  - 当前仍处于 `waiting_accept`
  - 且创建时间距今超过 24 小时

这样可以先把“推荐有没有真正被接住”这件事量化出来，为后续自动分配策略提供观察基础。

## 3. 前端实现

更新：

- `template-front/src/views/admin/ConsultationRecordPage.vue`

### 3.1 新增智能分配运营总览区块

在管理端问诊记录页新增“智能分配运营总览”，包含 6 张指标卡：

- 推荐覆盖问诊
- 首推命中率
- 转其他医生率
- 平均接手时长
- 超 24h 待接手
- 科室分诊队列

这样进入页面后，除了能看原来的 AI 采纳效果，也能直接看到分配链路是否顺畅。

### 3.2 新增首推医生承接拆分表

新增首推医生维度的运营表，展示：

- 被推荐量
- 待接手
- 已接手
- 他人承接
- 命中率
- 平均接手时长

这一层适合用来快速定位：

- 哪些医生是系统经常首推的对象
- 哪些医生当前积压更多
- 哪些医生的首推命中率更高

### 3.3 新增长时间待接手问诊列表

新增“长时间待接手问诊”卡片列表，展示：

- 问诊单号
- 患者
- 分类
- 科室
- 提交时间
- 优先医生
- 当前等待时长
- 推荐依据

并支持直接跳转到问诊详情继续排查。

## 4. 涉及文件

### 后端

- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationRecordController.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationRecordAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationDispatchSummaryVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationDispatchDoctorVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationDispatchWaitVO.java`

### 前端

- `template-front/src/views/admin/ConsultationRecordPage.vue`

### 文档

- `docs/2026-04-06-admin-dispatch-ops-log.md`

## 5. 验证结果

本轮已完成验证：

- 后端执行 `mvn -q -DskipTests compile` 通过
- 前端执行 `npm run build` 通过

## 6. 当前阶段结论

到这里，管理端关于智能分配的能力已经从“状态归档”继续推进到“运营复盘”：

1. 能看当前每条问诊的智能分配状态
2. 能看首推是否命中
3. 能看是否被其他医生承接
4. 能看接手效率
5. 能看哪些推荐单积压过久

这会让后续继续推进：

- 半自动分配策略
- 自动分配实验开关
- 智能分配运营报表

变得更顺畅。

## 7. 下一步建议

建议下一步继续优先推进：

1. 把医生容量、排班空闲度、风险等级纳入智能分配排序
2. 增加“不同分类 / 不同科室”的智能分配命中率拆分
3. 为推荐未命中的问诊补充原因归档，逐步形成自动分配优化闭环
