# 2026-04-14 管理端 AI 配置中心开发记录

## 1. 本轮目标

把原来偏只读的“AI 导诊配置与状态”页面，补成真正可保存、可落库、可实时影响运行配置的后台入口。

本轮聚焦的是会直接影响 AI 导诊业务行为的核心参数：

- AI 导诊总开关
- Prompt 版本
- 推荐医生候选上限

模型提供方、Base URL、API Key、温度、Token 等仍保留为环境变量驱动，避免把部署配置和业务配置混在一起。

## 2. 后端改动

### 2.1 新增 AI 配置表

新增表：

- `db_consultation_ai_config`

字段：

- `id`
- `enabled`
- `prompt_version`
- `doctor_candidate_limit`
- `update_time`

同时补充：

- 初始化脚本：`sql/mysql57-init.sql`
- 升级脚本：`sql/mysql57-upgrade-2026-04-14-consultation-ai-config.sql`

### 2.2 新增后台配置读写接口

在管理端 AI 控制器下新增：

- `GET /api/admin/consultation-ai/config`
- `POST /api/admin/consultation-ai/config`

用于读取和保存 AI 配置。

### 2.3 新增运行时配置服务

新增：

- `ConsultationAiConfigService`
- `ConsultationAiConfigServiceImpl`

职责：

1. 读取数据库配置
2. 首次无配置时自动写入默认行
3. 保存配置后立即更新运行中的 `AiTriageProperties`
4. 应用启动时优先把数据库配置覆盖到运行时属性

这样新的 AI 导诊请求、追问消息和 Prompt 留痕会直接使用最新保存值，而不需要重启服务。

### 2.4 环境默认值兜底

`AiTriageProperties` 新增默认值快照能力，用于保留启动时从 `application.yml / 环境变量` 解析出的原始默认值。

这样数据库配置缺失或异常时，仍然可以稳定回退到部署环境的默认配置。

## 3. 前端改动

管理端页面：

- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

新增“AI 配置中心”面板，支持：

- 加载已保存配置
- 编辑 AI 导诊总开关
- 编辑 Prompt 版本
- 编辑推荐医生候选上限
- 保存配置
- 恢复到已保存状态

同时保留原来的：

- 运行状态总览
- 模型参数只读展示
- AI 输出审计
- 高风险复核队列
- 医生端 AI 使用概况

## 4. 当前效果

到这里，管理端 AI 页面已经不只是“看状态”，而是具备了基础治理能力：

1. 能看当前运行状态
2. 能保存核心 AI 业务配置
3. 保存后能立即影响运行时配置
4. 还能继续结合审计和使用概况做效果复盘

## 5. 后续可继续补的方向

下一步可以继续往更完整的 AI 治理中心推进：

1. Prompt 多版本管理与切换历史
2. 模型参数分环境配置中心
3. 配置变更审计记录
4. Prompt 版本切换前后样本对比
5. 高风险样本人工纠偏回流
