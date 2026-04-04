# 2026-04-04 AI 推荐医生解释增强记录
## 1. 本轮目标

在已经完成患者侧 AI 导诊工作区和医生侧 AI 导诊可见性的基础上，继续把“AI 为什么这样推荐”展示出来：

- 不只展示 AI 导诊长文本
- 把推荐方式、建议科室、风险标签、推荐依据拆出来
- 让患者和医生都能更清楚理解 AI 推荐逻辑

本轮核心不是再新增一个 AI 能力，而是把已经生成出来但还没有真正用起来的结构化解释信息展示出来。

## 2. 现状问题

当前 `Spring AI + DeepSeek` 返回的 `AiTriageAdvice` 中，其实已经包含这些字段：

- `recommendedVisitType`
- `recommendedDepartmentName`
- `riskFlags`
- `recommendedDoctorNames`
- `recommendedDoctorIds`
- `doctorRecommendationReason`
- `confidenceScore`

后端也已经把这些信息写进了导诊消息的 `structuredContent`，但前端之前主要只展示 `content` 文本，导致：

- AI 推荐依据不够直观
- 推荐医生解释没有真正被看见
- 医生接诊时仍然要从长文本里自己提取重点

## 3. 本轮实现

### 3.1 新增共享解析 helper

新增文件：

- `template-front/src/triage/insight.js`

作用：

- 统一解析 AI 导诊消息中的 `structuredContent`
- 提取推荐方式、建议科室、风险标签、推荐医生、推荐依据、置信度
- 给多个页面复用，避免每个页面各写一套 JSON 解析逻辑

### 3.2 患者侧 AI 导诊工作区增强

更新页面：

- `template-front/src/views/index/ConsultationTriagePage.vue`

增强内容：

- 在 AI 消息卡下方展示结构化解释区
- 展示建议方式、建议科室、置信度
- 展示推荐依据
- 展示推荐医生标签
- 展示风险标签

这样患者在独立 AI 导诊工作区中，不仅能看到 AI 说了什么，还能看到 AI 为什么这么推荐。

### 3.3 患者问诊详情增强

更新页面：

- `template-front/src/views/index/ConsultationPage.vue`

增强内容：

- 问诊详情里的导诊留痕卡片同步展示 AI 推荐解释
- 保持和独立 AI 导诊工作区一致的表达方式

这样用户无论从问诊详情还是 AI 工作区进入，都能得到一致的信息结构。

### 3.4 医生侧问诊详情增强

更新页面：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

增强内容：

- 医生查看 AI 导诊留痕时，能直接看到 AI 的结构化推荐解释
- 可直接看到建议方式、科室、风险标签、推荐依据和推荐对象

这样医生接诊时，不需要只靠长文本理解 AI 逻辑，前置导诊信息会更易读。

## 4. 兜底处理

本轮还补了一个小兜底：

- 如果 AI 返回了 `recommendedDoctorIds` 但没返回 `recommendedDoctorNames`
- 前端会回退显示 `医生ID xxx`

这样可以避免“推荐依据有了，但推荐对象看不到”的空白情况。

## 5. 这次涉及文件

### 前端

- `template-front/src/triage/insight.js`
- `template-front/src/views/index/ConsultationTriagePage.vue`
- `template-front/src/views/index/ConsultationPage.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`

### 文档

- `docs/2026-04-04-ai-doctor-recommendation-explain-log.md`

## 6. 验证结果

本轮已完成验证：

- 前端执行 `npm run build` 通过

## 7. 当前阶段结论

到这里，AI 导诊链路已经进一步从“能生成建议”推进到“建议可解释”：

1. AI 给出导诊结果
2. AI 推荐医生和就诊方式
3. 前端把推荐逻辑结构化展示出来
4. 患者和医生都能读到 AI 推荐依据

这使后续继续做推荐排序优化、人工纠偏、效果复盘时，基础会更稳。

## 8. 下一步建议

建议接下来继续推进以下方向之一：

1. 医生侧增加 AI 摘要卡片，把多轮导诊进一步压缩成一屏摘要
2. 医生侧支持人工标记“AI 推荐是否合理”，沉淀纠偏数据
3. 后端进一步把 AI 推荐解释和候选医生排序结果绑定得更紧，形成更完整的推荐理由链
