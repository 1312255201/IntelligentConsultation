# 2026-04-06 智能推荐医生排序策略增强记录

## 1. 本轮目标

在已经完成：

- AI 导诊补充建议
- 推荐医生解释展示
- 智能分配进度展示
- 管理端智能分配运营看板

之后，继续把“推荐医生”这一层从简单候选列表推进到更可解释的排序策略，让系统不仅能给出候选医生，还能说明：

- 为什么这位医生会排在前面
- 当前更看重排班、容量还是工作负载
- 哪些服务标签和当前问诊更匹配

这一轮的重点不是做全自动派单，而是先把推荐排序做得更稳、更可解释，为后续自动分配打基础。

## 2. 后端实现

### 2.1 扩展推荐医生输出结构

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationRecommendDoctorVO.java`

新增字段：

- `activeConsultationCount`
- `recommendationScore`
- `recommendationSummary`
- `recommendationReasons`
- `matchedServiceTags`

这样候选医生不再只有基础资料和排班信息，还会带上排序说明。

### 2.2 推荐排序从“排班优先”升级为“多因子排序”

更新：

- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceImpl.java`

本轮推荐排序综合考虑：

- 当前分诊动作与排班接诊方式是否匹配
- 医生最近排班是否可接诊、是否更靠前
- 当前排班剩余容量
- 当前医生待处理问诊量
- 医生服务标签是否命中当前问诊信息

排序结果会生成：

- 可读的优先分
- 结构化原因列表
- 一条简洁的排序说明摘要

### 2.3 首推医生推荐依据回流到分诊结果

本轮还同步更新了分诊结果说明生成逻辑：

- `buildTriageResultReason(...)`

现在分诊结果除了原来的规则建议、风险提示、主诉摘要外，还会补上首推医生的排序说明摘要。

这样原有：

- 智能分配推荐依据
- 患者端结果说明
- 医生端接诊前导诊解释

都会自然拿到更完整的“为什么推荐这位医生”。

### 2.4 智能分配推荐依据补充兜底

更新：

- `template-backend/src/main/java/cn/gugufish/utils/ConsultationSmartDispatchUtils.java`

如果分诊结果里没有单独的推荐依据文本，智能分配会回退使用首推医生的 `recommendationSummary`，避免推荐状态有了但推荐原因为空。

## 3. 前端实现

更新：

- `template-front/src/views/index/ConsultationPage.vue`
- `template-front/src/views/index/ConsultationTriagePage.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`

### 3.1 患者端补充排序说明

患者在两个入口都能看到更完整的推荐医生信息：

- 问诊详情页
- AI 导诊工作区

新增展示内容：

- 优先分
- 匹配服务标签
- 排序原因 chips
- 排序说明摘要
- 排班信息

这样患者不再只是看到“推荐了哪几位医生”，还能知道系统为什么更优先推荐某位医生。

### 3.2 医生端接诊前可看到排序依据

医生问诊详情中的 AI 导诊上下文，也同步展示：

- 候选医生优先分
- 匹配标签
- 排序原因
- 排序说明

这样医生接诊时，能更快理解当前问诊为什么优先分配给某位医生。

## 4. 当前排序策略口径

当前版本的智能推荐排序，主要采用以下口径：

1. 先看当前分诊动作与排班接诊方式是否匹配
2. 再看最近排班是否可接诊、是否更靠前
3. 再看剩余容量是否更充足
4. 再看当前待处理问诊量是否更低
5. 最后参考服务标签与当前问诊的命中情况

说明：

- 高优先级问诊会更强调近期可接诊和当前负载
- 这一轮仍然属于“推荐排序增强”，不是自动派单
- 当前没有引入新的数据库表，完全基于现有问诊、认领、排班和服务标签数据聚合

## 5. 涉及文件

### 后端

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationRecommendDoctorVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/utils/ConsultationSmartDispatchUtils.java`

### 前端

- `template-front/src/views/index/ConsultationPage.vue`
- `template-front/src/views/index/ConsultationTriagePage.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`

### 文档

- `docs/2026-04-06-doctor-ranking-strategy-log.md`

## 6. 验证结果

本轮已完成验证：

- 后端执行 `mvn -q -DskipTests compile` 通过
- 前端执行 `npm run build` 通过

## 7. 当前阶段结论

到这里，项目在“AI 导诊 -> 推荐医生 -> 智能分配”这条主链路上又往前推进了一步：

1. 系统不再只是列出候选医生
2. 候选医生开始具备可解释排序
3. 首推医生推荐理由会回流到分诊和智能分配展示
4. 患者和医生都能看到更清晰的排序依据

这为后续继续推进：

- 半自动分配策略
- 自动分配实验开关
- 命中率与未命中原因分析

提供了更扎实的基础。

## 8. 下一步建议

建议下一步继续优先推进：

1. 在管理端补“按分类 / 按科室”的首推命中率拆分
2. 为推荐未命中场景沉淀原因标签，形成纠偏数据
3. 继续把医生容量、排班和工作负载做成可配置权重，为后续自动分配预留策略参数
