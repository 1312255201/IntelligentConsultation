# 2026-04-08 医生端 AI 沟通草稿开发记录

## 1. 本轮目标

继续完善智能问诊系统中“AI 导诊 -> 医生接管 -> 医患沟通”这条主线。

本轮不再停留在患者侧 AI 导诊结果展示，而是把 AI 真正接入医生接诊环节，为医生生成一段可编辑的沟通回复草稿，帮助医生更快完成：

- 首次接诊开场
- 补充追问
- 风险提醒
- 下一步就医建议

## 2. 本轮完成

### 2.1 新增医生侧 AI 沟通草稿接口

新增文件：

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationMessageDraftGenerateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorConsultationMessageDraftVO.java`

更新文件：

- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorWorkspaceService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

新增接口：

- `POST /api/doctor/consultation/message/ai-draft`

请求参数：

- `recordId`

返回内容：

- `content`：建议回复正文
- `summary`：生成依据摘要
- `riskFlags`：当前风险提示
- `promptVersion`
- `source`
- `fallback`

### 2.2 后端接入 DeepSeek 生成医生沟通建议

本轮在医生工作台服务里新增了医生侧 AI 草稿生成逻辑：

- 会先校验当前账号是否为有效医生
- 会复用现有问诊详情聚合能力，读取：
  - 主诉
  - 健康摘要
  - 问诊答案
  - 分诊结果
  - AI 导诊历史
  - 最近医患沟通消息
  - 医生当前处理进度

然后再调用 Spring AI + DeepSeek 生成一段结构化沟通草稿。

本轮 Prompt 重点约束了：

- 只能生成医生可编辑回复，不替代医生判断
- 不允许输出确定性诊断
- 高风险场景必须明确提示线下或急诊
- 信息不足时优先补充关键追问
- 不提 AI，不输出解释过程

### 2.3 增加规则兜底草稿

考虑到开发和演示环境下 DeepSeek 可能未启用，或请求可能失败，本轮没有让按钮直接失效，而是补了一层规则兜底草稿：

- DeepSeek 可用时返回 `source=deepseek`
- DeepSeek 不可用或调用失败时返回 `source=fallback`
- 前端会明确提示当前是：
  - `DeepSeek 草稿`
  - 或 `规则兜底草稿`

这样医生端在任何环境下都能生成一段可编辑建议，不会出现“点了没反应”的体验断层。

### 2.4 医生问诊页接入 AI 沟通草稿

更新文件：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

本轮在“医患沟通”区块中新增：

- `AI 生成建议`
- `覆盖带入`
- `追加带入`

新增展示内容：

- AI 建议回复正文
- 生成依据
- 风险标签
- Prompt 版本
- 当前来源（DeepSeek / 规则兜底）

交互策略：

- AI 不会自动覆盖医生输入
- 只有医生主动点击“覆盖带入”或“追加带入”时，才会写入消息输入框
- 问诊切换、抽屉关闭、消息发送成功后，会自动清空上一条草稿，避免旧建议串到新问诊里

### 2.5 增加 AI 沟通草稿场景模式

更新文件：

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationMessageDraftGenerateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorConsultationMessageDraftVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`

本轮继续把 AI 沟通草稿从“统一回复”推进到“按场景生成”：

- `opening`：首次接诊
- `clarify`：补充追问
- `check_result`：结果解读
- `follow_up`：复诊随访

后端现在会根据不同场景：

- 调整 DeepSeek Prompt 的重点
- 调整规则兜底草稿的开场和建议文案
- 在返回结果中保留本次使用的 `sceneType`
- 在 `promptVersion` 中追加当前场景标识

前端对应新增：

- AI 草稿场景选择器
- 场景说明提示
- 草稿卡片中显示当前生成场景

另外页面还补了一层默认推荐逻辑：

- 已完成问诊默认更偏向 `follow_up`
- 已有沟通记录的进行中问诊默认更偏向 `clarify`
- 其余情况下默认使用 `opening`

这样医生在常见沟通阶段里就不需要每次都从一段泛化回复里自己重新改结构，而是能直接得到更接近当前任务的初稿。

### 2.6 打通 AI 草稿与医生沟通模板

更新文件：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/doctor/DoctorReplyTemplatePage.vue`
- `sql/mysql57-demo-data.sql`

本轮继续把 AI 沟通草稿往“可真正落地发送”推进了一步，把医生个人模板系统接入到了消息沟通区：

- 模板管理页新增 4 个消息沟通场景：
  - `message_opening`
  - `message_clarify`
  - `message_check_result`
  - `message_follow_up`
- 医生问诊页会根据当前 AI 草稿场景自动映射到对应模板场景
- 医生可直接执行：
  - `模板覆盖带入`
  - `模板追加带入`
  - `AI+模板合成带入`
- 当前选中的沟通模板会在页面内直接预览，减少来回切页确认内容

这意味着医生现在不只是“生成一段 AI 回复”，而是可以把：

- AI 根据当前问诊生成的实时建议
- 医生自己沉淀的规范化话术模板

组合成一条更接近实际发送内容的消息草稿。

### 2.7 补充沟通模板演示数据

为了让演示环境直接可见，本轮也同步在 `sql/mysql57-demo-data.sql` 中补充了 4 组消息沟通模板样例，导入后演示医生账号可直接体验：

- 首次接诊开场
- 补充追问
- 检查结果沟通
- 复诊随访提醒

## 3. 价值

- AI 能力第一次真正进入“医生接管后”的沟通流程，不再只停留在患者导诊阶段
- 医生在处理高风险或信息不足样本时，可以更快发出第一条专业回复
- 即便 DeepSeek 未启用，系统也能给出规则兜底建议，保持产品体验连续
- AI 草稿已经能按接诊阶段切换生成逻辑，医生在首次接诊、补充追问和随访场景下都更容易直接使用
- AI 草稿已经和医生模板系统打通，医生可以把实时建议与规范话术快速拼装成最终消息
- 后续如果继续扩展医生侧 AI，可以直接沿着这条链路增加：
  - 多种沟通场景切换
  - 随访话术草稿
  - 自动提炼患者最近关注点
  - 医生回复采纳率分析

## 4. 涉及文件

### 后端

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationMessageDraftGenerateVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/DoctorConsultationMessageDraftVO.java`
- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorWorkspaceService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

### 前端

- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/doctor/DoctorReplyTemplatePage.vue`

### SQL

- `sql/mysql57-demo-data.sql`

## 5. 验证

本轮已完成验证：

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 6. 下一步建议

建议下一轮继续沿着医生侧 AI 能力推进以下方向之一：

1. 在后台增加医生侧 AI 草稿使用留痕，统计生成次数、带入次数和发送采纳率
2. 为医生处理表单和随访表单增加独立 AI 草稿入口，形成完整医生 AI 助理工作流
3. 继续细化结果解读场景，支持围绕检查值异常项生成更定向的沟通建议
4. 把医生消息发送结果回流成训练样本，沉淀高采纳模板和优质 Prompt 策略
