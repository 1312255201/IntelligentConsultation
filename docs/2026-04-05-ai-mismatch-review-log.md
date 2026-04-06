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

## 9. 本轮继续开发补充二

在上一轮“按医生拆分”基础上，继续把 AI 复盘往“知道偏差具体发生在哪些结论字段上”推进。

### 9.1 目标

让管理端不只知道：

- 哪些问诊与 AI 不一致
- 为什么不一致

还能够继续看到：

- 偏差主要集中在病情等级、处理去向还是随访安排
- 每个字段的偏差背后，最常关联的是哪些人工调整原因

### 9.2 后端扩展

更新文件：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiSummaryVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiFieldVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`

新增内容：

- `ai-summary` 响应新增 `fieldBreakdown`
- 新增字段级统计对象 `AdminConsultationAiFieldVO`

统计口径：

- 基于现有 `ConsultationAiComparisonUtils` 的字段对比结果
- 聚合三个关键字段：
- `condition_level`
- `disposition`
- `follow_up`
- 每个字段输出：
- 总样本数
- 已对比数
- 一致数
- 偏差数
- 待补充数
- 偏差率
- 该字段偏差关联的差异原因分布

实现细节：

- 为避免逐条调用 query service 造成过多查询，本轮在管理端统计中补充了 triage session、triage message、triage result 的批量加载逻辑
- 字段偏差原因仍复用现有医生留痕数据
- 若字段出现偏差，但医生未沉淀原因，则该字段只统计偏差次数，不强行补造原因

### 9.3 前端扩展

更新文件：

- `template-front/src/views/admin/ConsultationRecordPage.vue`

新增界面：

- `AI 采纳总览` 下新增“字段偏差分析”
- 逐卡展示：
- 病情等级
- 处理去向
- 随访安排
- 每张卡片展示：
- 已对比数
- 偏差率
- 一致 / 偏差 / 待补充数量
- 偏差关联原因

### 9.4 当前复盘能力

到这里，管理端围绕 AI 采纳效果已经形成五层复盘视角：

- 总体采纳概览
- 按科室拆分
- 按问诊分类拆分
- 按医生拆分
- 按关键字段偏差拆分

## 10. 本轮继续开发补充三

继续把“字段偏差分析”从静态统计推进到可下钻复盘。

### 10.1 目标

让管理端在看到字段偏差后，可以继续直接查看对应问诊单样本，而不是停留在汇总数字层面。

本轮补充后，管理员可以从字段卡片直接进入：

- 偏差样本
- 待补充样本

### 10.2 后端扩展

更新文件：

- `template-backend/src/main/java/cn/gugufish/service/ConsultationRecordAdminService.java`
- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationRecordController.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationAiFieldSampleVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`

新增接口：

- `GET /api/admin/consultation-record/field-samples`

请求参数：

- `fieldKey`
- `status`
- `limit`

支持字段：

- `condition_level`
- `disposition`
- `follow_up`

支持样本类型：

- `mismatch`
- `pending`

返回内容包括：

- 问诊单 ID / 编号
- 患者
- 分类 / 科室
- 处理医生
- 字段名称
- AI 取值
- 医生取值
- 对比状态
- 差异原因
- 差异备注
- 更新时间

### 10.3 前端扩展

更新文件：

- `template-front/src/views/admin/ConsultationRecordPage.vue`

界面补充：

- 字段偏差卡片新增：
- `查看偏差样本`
- `查看待补充样本`
- 点击后弹出字段样本下钻弹窗
- 弹窗内支持在“偏差样本 / 待补充样本”之间切换
- 每条样本可继续跳转到问诊详情页

### 10.4 当前效果

到这里，管理端 AI 复盘链路已经从：

- 看整体结果
- 看分布
- 看原因

继续推进到：

- 看具体是哪类字段偏差
- 看这些字段偏差对应的是哪些实际问诊单

## 11. 本轮继续开发补充四

继续把字段样本下钻能力补成可筛选的复盘面板。

### 11.1 目标

管理员在打开字段样本弹窗后，不再只看到一组默认列表，还可以围绕当前字段继续筛选：

- 关键词
- 医生
- 问诊分类
- 科室

这样在复盘“某个字段为什么经常偏差”时，可以快速缩小到具体人群或业务场景。

### 11.2 后端扩展

更新文件：

- `template-backend/src/main/java/cn/gugufish/service/ConsultationRecordAdminService.java`
- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationRecordController.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`

接口扩展：

- `GET /api/admin/consultation-record/field-samples`

新增可选筛选参数：

- `keyword`
- `doctorName`
- `categoryName`
- `departmentName`

筛选策略：

- 医生 / 分类 / 科室使用精确匹配
- 关键词支持在以下字段中模糊搜索：
- 问诊单号
- 患者姓名
- 处理医生
- 问诊分类
- 科室
- AI 取值
- 医生取值
- 差异备注

### 11.3 前端扩展

更新文件：

- `template-front/src/views/admin/ConsultationRecordPage.vue`

弹窗新增筛选条：

- 搜索框
- 医生下拉
- 分类下拉
- 科室下拉
- 筛选按钮
- 重置按钮

实现说明：

- 医生选项复用 `doctorBreakdown`
- 分类 / 科室选项复用当前管理端已加载的问诊记录
- 切换“偏差样本 / 待补充样本”时仍复用当前筛选条件

### 11.4 当前效果

到这里，“字段偏差分析”已经具备三层能力：

- 看字段偏差统计
- 看字段对应的样本
- 对样本继续做业务维度筛选
## 12. 本轮继续开发补充五

继续把管理端 AI 复盘从“能看字段样本”推进到“能按医生和差异原因直接下钻偏差问诊单”，让复盘链路从汇总统计进一步走向可执行排查。

### 12.1 后端扩展

更新文件：
- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationRecordController.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationRecordAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`

新增接口：
- `GET /api/admin/consultation-record/mismatch-samples`

支持筛选参数：
- `limit`
- `keyword`
- `doctorName`
- `reasonCode`
- `categoryName`
- `departmentName`

实现说明：
- 直接复用现有 `AdminConsultationAiMismatchVO` 作为偏差样本下钻返回对象
- 管理端最近偏差记录与下钻样本统一复用批量构建好的 AI 对比结果，不再逐条回查 triage query service
- `ai_mismatch_remark` 存在但未勾选结构化原因时，样本和筛选都会兜底归入 `other`
- 同步把 `field-samples` 的样本上限收口为最多 `50` 条，和控制器侧保持一致

### 12.2 前端扩展

更新文件：
- `template-front/src/views/admin/ConsultationRecordPage.vue`

新增能力：
- 医生维度卡片新增“查看样本”入口
- 医生维度中的差异原因标签支持点击，直接按“医生 + 原因”组合下钻
- 差异原因汇总卡片新增“查看样本”入口
- 新增 `AI 偏差样本` 弹窗，支持：
- 关键词筛选
- 医生筛选
- 差异原因筛选
- 分类筛选
- 科室筛选
- 从偏差样本继续跳转到问诊详情

实现说明：
- 弹窗样式复用现有字段样本的 toolbar / filter bar / 卡片结构，保持管理端页面风格统一
- 差异样本卡片复用“最近差异记录”的信息结构，避免重复造第二套展示模型

### 12.3 验证结果

本轮已完成验证：
- 后端执行 `mvn -q -DskipTests compile` 通过
- 前端执行 `npm run build` 通过
## 13. 本轮继续开发补充六

继续优化管理端 AI 复盘的样本下钻体验，解决字段样本和偏差样本只能看前 20 条的问题，让管理员可以围绕当前筛选条件持续往下翻看更多问诊单。

### 13.1 后端扩展

更新文件：
- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationRecordController.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationRecordAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`

扩展接口：
- `GET /api/admin/consultation-record/mismatch-samples`
- `GET /api/admin/consultation-record/field-samples`

新增参数：
- `offset`

实现说明：
- 两个样本接口统一支持 `limit + offset` 的分页式下钻
- 排序、筛选逻辑保持不变，在内存聚合完成后再执行 `skip(offset) + limit(limit)`
- 这样可以保持当前统计口径与样本口径一致，不需要为分页额外改动现有复盘逻辑

### 13.2 前端扩展

更新文件：
- `template-front/src/views/admin/ConsultationRecordPage.vue`

新增能力：
- 字段样本弹窗支持“加载更多”
- 偏差样本弹窗支持“加载更多”
- 两个弹窗都会显示当前已加载样本数
- 切换筛选条件、切换字段样本状态时会自动重置分页偏移量

实现说明：
- 前端统一使用 `SAMPLE_PAGE_SIZE = 20`
- 首次加载或重新筛选时从 `offset=0` 开始
- 点击“加载更多”时追加到当前列表，而不是整列表覆盖
- 样本条数达到当前页大小时，继续显示“加载更多”入口

### 13.3 验证结果

本轮已完成验证：
- 后端执行 `mvn -q -DskipTests compile` 通过
- 前端执行 `npm run build` 通过
## 14. 本轮继续开发补充七

继续完善管理端 AI 复盘样本下钻能力，让管理员可以把当前筛选条件下的偏差样本和字段样本直接导出为 CSV，用于离线复盘、质检和二次分析。

### 14.1 后端扩展

更新文件：
- `template-backend/src/main/java/cn/gugufish/controller/admin/AdminConsultationRecordController.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationRecordAdminService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`

新增接口：
- `GET /api/admin/consultation-record/mismatch-samples/export`
- `GET /api/admin/consultation-record/field-samples/export`

实现说明：
- 将样本筛选逻辑抽到共享收集方法，分页查询与 CSV 导出共用同一套过滤口径
- 导出不再受页面样本 `limit <= 50` 的限制，始终导出当前筛选条件下的完整结果集
- CSV 统一使用 UTF-8 BOM 输出，兼容中文内容在常见表格软件中的打开体验

### 14.2 前端扩展

更新文件：
- `template-front/src/net/index.js`
- `template-front/src/views/admin/ConsultationRecordPage.vue`

新增能力：
- 网络层新增 `download(...)` 方法，统一处理鉴权下载、文件名解析和 blob 错误消息解析
- 字段样本弹窗新增 `Export CSV`
- AI 偏差样本弹窗新增 `Export CSV`
- 导出始终复用当前弹窗里的筛选条件，不受当前已加载页数影响

实现说明：
- 样本列表查询与导出共用 query 构造函数，避免分页参数和筛选参数漂移
- 导出成功后给出前端提示，失败时沿用现有消息反馈方式

### 14.3 验证结果

本轮已完成验证：
- 后端执行 `mvn -q -DskipTests compile` 通过
- 前端执行 `npm run build` 通过
