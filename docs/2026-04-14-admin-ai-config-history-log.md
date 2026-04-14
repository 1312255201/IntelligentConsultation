# 2026-04-14 管理端 AI 配置变更历史开发记录

## 1. 本轮目标

在已经可保存 AI 配置的基础上，继续补上“配置变更可追溯”能力。

目标是让后台能直接回答几个问题：

- 最近谁改了 AI 配置
- 改动发生在什么时候
- 具体改了哪些字段
- Prompt 版本和候选上限是怎么变的

## 2. 后端改动

### 2.1 新增配置历史表

新增表：

- `db_consultation_ai_config_history`

主要字段：

- `config_id`
- `enabled_before`
- `enabled_after`
- `prompt_version_before`
- `prompt_version_after`
- `doctor_candidate_limit_before`
- `doctor_candidate_limit_after`
- `operator_account_id`
- `operator_username`
- `change_summary`
- `create_time`

同时补充：

- 初始化脚本：`sql/mysql57-init.sql`
- 升级脚本：`sql/mysql57-upgrade-2026-04-14-consultation-ai-config-history.sql`

### 2.2 保存配置时自动写历史

在 `ConsultationAiConfigServiceImpl` 中，保存 AI 配置时会：

1. 先读取当前配置
2. 比较新旧差异
3. 若确实发生变更，则自动插入一条历史记录
4. 把操作人账号和用户名一并写入历史

如果本次保存和当前配置完全一致，则不会重复产生历史噪音。

### 2.3 新增历史查询接口

管理端新增接口：

- `GET /api/admin/consultation-ai/config-history`

用于读取最近的配置变更记录。

## 3. 前端改动

管理端 AI 页面新增“最近配置变更”面板，支持直接查看：

- 操作人
- 变更时间
- 开关变化
- Prompt 版本变化
- 候选医生上限变化
- 自动生成的变更摘要

这样管理端 AI 页面已经从：

- 只看状态

继续推进到了：

- 能改配置
- 能追配置
- 能回看配置变化

## 4. 当前效果

到这里，AI 配置治理已经具备了比较完整的第一版闭环：

1. 配置可保存
2. 保存可立即生效
3. 变更有历史
4. 历史可在后台直接查看

## 5. 后续可继续补的方向

下一步比较适合继续往下做的是：

1. Prompt 多版本预设与快速切换
2. 配置变更备注
3. 配置历史筛选与导出
4. Prompt 版本切换前后样本效果对比
