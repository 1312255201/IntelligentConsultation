# 2026-04-12 医生端问诊归档摘要补齐记录

## 本次目标

在患者端已经具备问诊归档摘要和导出能力的基础上，继续补齐医生端详情页的归档视图，让医生可以在一个面板中快速回看当前问诊的分诊、处理、随访、沟通和服务评价状态，并支持复制、下载留档。

## 已完成内容

### 1. 后端补充医生端归档摘要聚合

- 新增 [ConsultationArchiveSummaryUtils.java](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-backend/src/main/java/cn/gugufish/utils/ConsultationArchiveSummaryUtils.java)
- 扩展 [AdminConsultationRecordVO.java](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationRecordVO.java)
- 更新 [DoctorWorkspaceServiceImpl.java](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java)

本次医生端归档摘要已聚合以下信息：

- 当前处理阶段
- 问诊概览
- 分诊结论摘要
- 医生处理摘要
- 归档结论
- 随访进展
- 服务评价摘要
- 最近沟通摘要
- 风险标签、结论标签、后续动作
- 可直接复制和导出的纯文本摘要

其中“最近沟通”和“后续动作”文案已经切换为医生视角，例如：

- 展示待处理患者新消息/恢复更新
- 提醒先认领问诊、补结构化结论、安排下一次随访
- 对低分或未解决服务评价给出回看处理提醒

### 2. 新增医生端归档摘要下载接口

- 更新 [DoctorWorkspaceController.java](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java)
- 新增接口：`GET /api/doctor/consultation/archive/export?id=...`

接口行为：

- 复用医生侧 `consultationDetail` 返回中的 `archiveSummary.plainText`
- 下载文件名格式为 `doctor-consultation-archive-<consultationNo>.txt`
- 以 `text/plain;charset=UTF-8` 返回
- 写入 UTF-8 BOM，兼容 Windows 文本工具直接打开中文内容

### 3. 医生端详情页新增归档摘要面板

- 更新 [DoctorConsultationPage.vue](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-front/src/views/doctor/DoctorConsultationPage.vue)

新增能力：

- 在“基本信息”下方新增“问诊归档摘要”卡片
- 展示阶段、跟进医生、沟通条数、随访次数、服务评分
- 分块展示问诊概览、分诊结论、医生处理、随访进展、服务评价、最近沟通
- 展示风险标签、结论标签和后续动作
- 支持“下载摘要”
- 支持“复制摘要”

### 4. 样式与现有医生工作台风格保持一致

- 复用医生端现有 `panel / head / subcard / notice / chips` 风格
- 新增归档卡片渐变背景、指标胶囊和后续动作卡片
- 响应式下自动切换为单列，保持移动端可读性

## 当前效果

- 医生打开某条问诊详情后，可以优先看到当前病例的归档总览，而不必在多个模块之间来回切换
- 交接、复盘和继续跟进时，可以直接复制纯文本摘要或下载留档
- 医生端与患者端都已经具备摘要导出能力，在线问诊闭环又补齐了一块核心能力

## 验证

- 前端：`cmd /c npm run build` 通过
- 后端：`"C:\Users\13122\AppData\Local\Programs\IntelliJ IDEA Ultimate\plugins\maven\lib\maven3\bin\mvn.cmd" -q -DskipTests compile` 通过

## 后续建议

1. 继续补医生端“检查结果回看 / 用药建议 / 复诊建议”等更强业务字段的结构化沉淀
2. 将同一套归档摘要继续复用到管理端问诊详情页，便于运营和质控查看
3. 在在线问诊主流程继续补齐医生开具建议单、检查单回填、处置完成后的患者确认等闭环能力
