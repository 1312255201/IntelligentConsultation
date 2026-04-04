# 2026-04-04 患者侧 AI 导诊工作区开发记录

## 1. 本轮目标

在已经支持“患者提交问诊后继续和 AI 导诊交互”的基础上，进一步把患者侧体验从详情弹窗增强为独立工作区页面。

本轮重点是把 AI 导诊从“问诊记录里的一个功能块”升级为“患者端的独立工作区”，方便集中查看：

- 当前分诊状态
- 导诊会话留痕
- 导诊结果归档
- 推荐医生
- 继续 AI 导诊输入区

## 2. 路由与导航

### 2.1 新增用户端页面路由

新增：

- `template-front/src/views/index/ConsultationTriagePage.vue`

新增路由：

- `/index/triage`

作用：

- 作为患者侧 AI 导诊工作区主页
- 可通过 `recordId` 查询参数直接打开指定问诊记录

### 2.2 接入用户侧菜单

更新：

- `template-front/src/views/IndexView.vue`

菜单新增：

- `AI 导诊工作区`

这样患者可以直接从左侧导航进入专门的 AI 导诊页面，而不需要先打开问诊详情弹窗。

## 3. 页面能力

页面文件：

- `template-front/src/views/index/ConsultationTriagePage.vue`

### 3.1 左侧会话列表

支持：

- 查看当前账号下的问诊记录
- 按关键词搜索就诊人、分类、主诉、单号
- 直接切换不同导诊记录
- 通过选中状态突出当前导诊会话

### 3.2 右侧工作区详情

支持集中展示：

- 分诊等级
- 建议动作
- 推荐科室
- 问诊摘要
- 健康摘要
- 导诊结果归档
- 推荐医生列表
- 完整导诊留痕

### 3.3 继续 AI 导诊

在独立工作区内继续支持：

- 患者补充症状变化
- 患者补充持续时间、体温、检查结果
- AI 继续追问
- AI 更新建议

这部分直接复用本轮前面已经接好的后端接口：

- `POST /api/user/consultation/triage/message/send`

## 4. 问诊页联动

更新：

- `template-front/src/views/index/ConsultationPage.vue`

本轮新增联动入口：

- 问诊记录表格操作列增加 `AI 导诊`
- 问诊详情弹窗中的“导诊留痕”区域增加“在独立工作区打开”

这样用户可以从两个入口跳转到独立工作区：

1. 从问诊记录列表直接进入
2. 从问诊详情弹窗继续深入查看

## 5. 本轮涉及文件

### 前端

- `template-front/src/router/index.js`
- `template-front/src/views/IndexView.vue`
- `template-front/src/views/index/ConsultationPage.vue`
- `template-front/src/views/index/ConsultationTriagePage.vue`

### 文档

- `docs/2026-04-04-ai-triage-workspace-log.md`

## 6. 验证结果

本轮已完成验证：

- 前端执行 `npm run build` 通过

## 7. 当前阶段结论

到这里，患者侧 AI 导诊已经具备三个层次：

1. 问诊提交后自动生成首轮 AI 导诊建议
2. 在问诊详情里继续补充内容并让 AI 继续分析
3. 在独立 AI 导诊工作区中集中查看和继续交互

这意味着患者侧 AI 导诊已经开始从“附属功能”演进为“独立业务场景”。

## 8. 下一步建议

建议下一步优先推进以下其中一个方向：

1. 把患者侧“发起问诊”与“AI 导诊工作区”进一步串联为完整主链路
2. 在医生侧问诊详情中加入完整 AI 导诊工作区视图
3. 把 AI 对候选医生的推荐解释做得更结构化

如果继续沿患者端主线推进，最合适的下一步是：

- 让患者在“发起问诊”后可直接进入 AI 导诊工作区
- 支持更自然的会话起始流程，而不是先进入记录详情再继续补充
