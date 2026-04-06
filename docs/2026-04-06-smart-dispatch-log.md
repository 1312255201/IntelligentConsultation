# 2026-04-06 智能分配医生联动开发记录

## 1. 本轮目标

继续沿着“AI 导诊 -> 推荐医生 -> 医生接手”的主链路往下补，把当前项目里还比较松散的医生推荐信息，进一步整理成可直接展示、可直接进入工作台处理的“智能分配”视图。

本轮重点不是新增一个全新的分配算法，而是先把现有链路中已经具备的数据真正串起来：

- 患者提交问诊后，能看到系统当前优先推荐给谁
- 患者在 AI 导诊工作区里，能看到当前接手进度
- 医生工作台里，能看到“系统推荐给我”的问诊待办
- 医生问诊列表里，能按智能分配状态筛选和查看

## 2. 现状问题

在本轮之前，项目虽然已经有：

- 分诊结果里的推荐医生与候选医生
- AI 导诊里的推荐依据解释
- 医生认领问诊单的机制
- 患者和医生双方的消息沟通机制

但这些信息还没有真正形成一个统一的“分配进度视图”。

结果就是：

- 患者端能看到推荐医生，但不清楚当前是否已经有人接手
- 医生端能看到待认领问诊，却看不出哪些其实是系统优先推荐给自己的
- 工作台和问诊列表之间还缺少一层更明确的智能分配承接

## 3. 本轮实现

### 3.1 后端新增智能分配摘要模型

新增：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationSmartDispatchVO.java`
- `template-backend/src/main/java/cn/gugufish/utils/ConsultationSmartDispatchUtils.java`

统一把以下信息聚合成一个可复用的分配摘要：

- 当前系统首推医生
- 候选医生数量
- 当前是否已被认领
- 是由首推医生接手，还是由其他医生接手
- 当前分配提示文案
- 当前推荐依据文案

这一层主要基于现有的：

- `db_triage_result`
- `db_consultation_doctor_assignment`

进行聚合，不破坏原有认领流程。

### 3.2 用户问诊记录接口补充分配信息

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationRecordVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceImpl.java`

增强内容：

- 问诊记录列表增加 `smartDispatch`
- 问诊记录详情增加 `smartDispatch`

这样患者侧无论是在问诊记录列表、问诊详情弹窗，还是后续 AI 导诊工作区，都能拿到统一的智能分配摘要。

### 3.3 医生工作台补“系统推荐给我”待办

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorWorkbenchVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`

新增内容：

- `recommendedConsultationCount`
- `recommendedConsultations`

工作台新增：

- 统计卡：`系统推荐给我`
- 待办卡片：`系统推荐给我`

当前规则为：

- 系统首推医生等于当前医生
- 当前问诊仍处于等待接手状态

这样医生进入工作台后，可以更快看到系统认为更适合自己先处理的问诊单。

### 3.4 医生问诊列表增加智能分配状态和筛选

更新：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

增强内容：

- 顶部统计新增：`系统推荐给我`
- 工具栏新增：智能分配筛选
- 列表新增：智能分配状态列
- 详情抽屉新增：智能分配卡片

支持的筛选项：

- 全部分配
- 系统推荐给我
- 等待首推医生
- 已被其他医生接手

这样工作台跳转到问诊列表后，不会丢失“为什么优先看这条”的上下文。

### 3.5 患者端增加智能分配进度展示

更新：

- `template-front/src/triage/dispatch.js`
- `template-front/src/views/index/ConsultationPage.vue`
- `template-front/src/views/index/ConsultationTriagePage.vue`

增强内容：

- 问诊记录列表增加“智能分配”列
- 问诊详情弹窗增加“智能分配进度”卡片
- AI 导诊工作区增加“智能分配进度”卡片

患者现在可以更清楚看到：

- 系统当前是否已有优先推荐医生
- 当前是否仍在等待接手
- 是否已由首推医生接手
- 是否已由其他医生接手

## 4. 本轮设计取舍

本轮有意没有直接做“强制自动派单”，而是采用了更稳妥的过渡方案：

- 先保留现有医生认领机制
- 先把系统推荐结果变成明确可见的分配视图
- 先把“系统推荐给我”的工作台入口做出来

这样做的好处是：

- 不会破坏现在已经可用的认领流程
- 以后要演进成半自动分配或自动分配时，改动面更小
- 患者、医生、运营三端都会先得到更清晰的状态信息

## 5. 本轮涉及文件

### 后端

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationSmartDispatchVO.java`
- `template-backend/src/main/java/cn/gugufish/utils/ConsultationSmartDispatchUtils.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationRecordVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationRecordVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorWorkbenchVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

### 前端

- `template-front/src/triage/dispatch.js`
- `template-front/src/views/doctor/DoctorWorkbenchPage.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/index/ConsultationPage.vue`
- `template-front/src/views/index/ConsultationTriagePage.vue`

### 文档

- `docs/2026-04-06-smart-dispatch-log.md`

## 6. 验证结果

本轮已完成验证：

- 后端执行 `mvn -q -DskipTests compile` 通过
- 前端执行 `npm run build` 通过

## 7. 当前阶段结论

到这里，项目在“AI 导诊之后如何顺滑进入医生处理”这一段又往前推了一步：

1. 患者提交问诊
2. 系统完成分诊和医生推荐
3. 患者可看到当前智能分配进度
4. 医生工作台能看到“系统推荐给我”的待办
5. 医生问诊列表能按智能分配状态继续处理

这让项目从“能推荐医生”进一步走向“推荐结果能真正驱动接诊流转”。

## 8. 下一步建议

建议下一步继续优先推进以下方向之一：

1. 把智能分配从“展示 + 待办”继续推进到“半自动分配策略”
2. 为智能分配增加容量、风险级别、消息紧急度等优先级排序
3. 在后台增加智能分配命中率、接单率、转人工率等运营视图
4. 把 AI 推荐依据和医生最终接单结果做闭环复盘，为后续自动分配做训练数据准备
