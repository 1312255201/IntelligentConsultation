# 2026-03-25 AI 智能导诊基础建设指南

## 1. 文档目的

这份文档用于指导当前项目从“基础信息管理系统”逐步演进到“支持 AI 智能导诊的智能问诊系统”。

目标不是直接开始写 AI 对话，而是先完成 AI 导诊真正依赖的基础建设，避免后面出现以下问题：

- AI 能理解用户描述，但没有足够的结构化数据做可靠推荐
- AI 给出了推荐结果，但系统没有医生、科室、排班、服务能力等基础数据承接
- AI 导诊过程没有留痕，后续无法复盘、优化、审计
- 用户只做了一次对话，但没有形成“患者档案 + 导诊记录 + 推荐结果”的完整闭环

这份指南会把后续应建设的内容拆为：

- 业务目标
- 基础数据建设
- AI 导诊核心链路建设
- 风控与安全边界
- 后台管理能力
- 前台用户页面
- 推荐数据表设计方向
- 建议开发顺序

## 2. 当前项目现状

截至目前，项目已经具备以下能力：

- 账号注册、登录、找回密码
- 用户基础资料管理：头像、邮箱、密码
- 管理员端：科室管理
- 管理员端：医生管理
- 管理员端：首页内容管理
- 公共网站首页展示
- MySQL 5.7 初始化脚本
- MinIO 图片上传与图片访问

当前已存在的核心表：

- `db_account`
- `db_image_store`
- `db_department`
- `db_doctor`
- `db_homepage_config`
- `db_homepage_recommend_doctor`
- `db_homepage_case`

这些能力已经足够支撑“门户展示”，但距离“AI 智能导诊”还缺少大量业务基础层。

## 3. AI 智能导诊的最终目标

这里建议把 AI 智能导诊定义为一个“分诊与推荐助手”，而不是“自动诊断系统”。

### 3.1 建议的 AI 导诊职责

- 理解用户自然语言描述的症状、持续时间、严重程度、伴随症状
- 根据规则和模型综合判断紧急程度
- 推荐更适合的科室
- 推荐更匹配的医生或医生候选列表
- 推荐下一步动作
  - 立即线下急诊
  - 尽快门诊
  - 在线咨询
  - 复诊随访
  - 上传检查结果继续咨询
- 自动生成导诊摘要，供用户与医生后续查看

### 3.2 不建议让 AI 单独承担的职责

- 直接给出明确疾病诊断结论
- 在高风险场景下替代医生判断
- 对孕妇、儿童、急危重症场景完全自动决策
- 在没有规则兜底时单独决定分配医生

### 3.3 推荐的系统策略

建议采用“规则优先 + AI 理解增强 + 人工可兜底”的模式。

也就是：

- 紧急风险、禁忌情况、红旗症状由规则优先拦截
- 症状理解、语义归类、推荐排序由 AI 增强
- 最终结果需可解释、可复盘、可人工修正

## 4. AI 导诊落地前必须补齐的基础建设

下面这些内容不是“可选优化”，而是 AI 导诊真正落地前的基础依赖。

## 5. 基础建设一：账号与就诊人分离

### 5.1 为什么必须做

当前系统只有账号，没有真正意义上的“患者/就诊人”。

但真实场景里，一个账号往往会替多人发起问诊，例如：

- 本人为自己咨询
- 家长为孩子咨询
- 子女为老人咨询
- 家属代患者咨询

如果不把“账号”和“就诊人”拆开，后面所有导诊、病史、问诊记录都会混乱。

### 5.2 建议建设内容

- 一个账号可以维护多个就诊人
- 支持标记默认就诊人
- 支持“本人 / 子女 / 父母 / 配偶 / 其他”关系
- 支持就诊人独立健康档案
- AI 导诊时先选择就诊人，再进入对话

### 5.3 推荐数据表

#### `db_patient_profile`

用于存放就诊人基础资料。

建议字段：

- `id`
- `account_id`
- `name`
- `gender`
- `birth_date`
- `phone`
- `id_card`
- `relation_to_account`
- `is_self`
- `is_default`
- `height`
- `weight`
- `blood_type`
- `remark`
- `status`
- `create_time`
- `update_time`

### 5.4 后续页面

- 用户端：就诊人管理页
- 用户端：新增/编辑就诊人弹窗
- 用户端：发起导诊前的就诊人选择页
- 管理员端：必要时支持查看就诊人基础数据

## 6. 基础建设二：健康档案与病史管理

### 6.1 为什么必须做

AI 导诊不能只看当前一句话，还需要结合基础病史。

例如：

- “胸闷”对于普通成年人和有冠心病史的人风险完全不同
- “发热”对于孕妇、婴幼儿、术后患者的判断路径不同
- “咳嗽”如果合并哮喘史、长期吸烟史、既往肺部疾病，推荐逻辑应不同

### 6.2 建议建设内容

- 既往病史
- 过敏史
- 手术史
- 家族史
- 慢性病史
- 长期用药
- 特殊人群标识
  - 孕期
  - 哺乳期
  - 儿童
  - 老年人
  - 术后恢复期

### 6.3 推荐数据表

#### `db_patient_medical_history`

建议字段：

- `id`
- `patient_id`
- `allergy_history`
- `past_history`
- `chronic_history`
- `surgery_history`
- `family_history`
- `medication_history`
- `pregnancy_status`
- `lactation_status`
- `infectious_history`
- `create_time`
- `update_time`

#### `db_patient_health_tag`

用于存放结构化健康标签，便于 AI 和规则快速判断。

建议字段：

- `id`
- `patient_id`
- `tag_code`
- `tag_name`
- `tag_type`
- `status`
- `create_time`

### 6.4 后续页面

- 用户端：健康档案页
- 用户端：病史维护表单
- 用户端：长期用药维护
- 管理员端：健康标签字典维护

## 7. 基础建设三：医生服务能力与排班管理

### 7.1 为什么必须做

现在系统里虽然有医生和科室，但 AI 如果要做“智能推荐分配”，仅靠“医生属于哪个科室”远远不够。

AI 导诊推荐医生时，至少需要知道：

- 医生当前是否启用
- 医生擅长哪些问题
- 医生支持什么服务方式
- 医生当前是否有可接诊时段
- 医生是否支持初诊 / 复诊 / 图文 / 报告解读

### 7.2 需要补齐的内容

- 医生服务标签
- 医生适用问题范围
- 医生接诊方式
- 医生接诊状态
- 医生排班
- 医生最大接诊量
- 是否接受新患者

### 7.3 推荐数据表

#### `db_doctor_service_tag`

建议字段：

- `id`
- `doctor_id`
- `tag_code`
- `tag_name`
- `sort`
- `status`

标签示例：

- `respiratory`
- `fever`
- `child_common_disease`
- `report_interpretation`
- `chronic_followup`
- `postoperative_consultation`

#### `db_doctor_schedule`

建议字段：

- `id`
- `doctor_id`
- `schedule_date`
- `time_period`
- `visit_type`
- `max_capacity`
- `used_capacity`
- `status`
- `remark`
- `create_time`
- `update_time`

#### `db_doctor_service_setting`

建议字段：

- `doctor_id`
- `supports_online_consultation`
- `supports_followup`
- `supports_report_interpretation`
- `supports_prescription_consultation`
- `accepts_new_patient`
- `priority_weight`
- `update_time`

### 7.4 后续页面

- 管理员端：医生排班管理
- 管理员端：医生服务标签管理
- 管理员端：医生接诊设置管理

## 8. 基础建设四：问诊分类与前置资料模板

### 8.1 为什么必须做

AI 导诊不是所有用户都进入同一条对话链路。

不同问诊场景，前置采集的信息完全不同，例如：

- 图文问诊
- 复诊续方
- 检查报告解读
- 慢病随访
- 儿童常见病咨询
- 女性健康咨询

如果没有“问诊分类”和“前置资料模板”，AI 每次都只能从零开始问，效率和准确率都会很差。

### 8.2 建议建设内容

- 问诊分类管理
- 每种分类的前置表单模板
- 必填项控制
- 条件显示字段
- 文件上传要求
- 分类与科室/医生的映射

### 8.3 推荐数据表

#### `db_consultation_category`

建议字段：

- `id`
- `name`
- `code`
- `description`
- `department_id`
- `sort`
- `status`
- `create_time`
- `update_time`

#### `db_consultation_intake_template`

建议字段：

- `id`
- `category_id`
- `name`
- `description`
- `status`
- `version`
- `create_time`
- `update_time`

#### `db_consultation_intake_field`

建议字段：

- `id`
- `template_id`
- `field_code`
- `field_label`
- `field_type`
- `is_required`
- `options_json`
- `default_value`
- `placeholder`
- `validation_rule`
- `sort`
- `status`

字段类型可包括：

- `input`
- `textarea`
- `single_select`
- `multi_select`
- `date`
- `number`
- `upload`
- `switch`

### 8.4 后续页面

- 管理员端：问诊分类管理
- 管理员端：问诊前置模板管理
- 用户端：发起问诊前置资料填写页

## 9. 基础建设五：症状、部位、严重程度等导诊字典

### 9.1 为什么必须做

AI 模型虽然能理解自然语言，但系统需要结构化词典，才能稳定完成：

- 症状归一化
- 关键词命中
- 规则判断
- 数据统计
- 推荐解释

### 9.2 需要建设的字典

- 身体部位字典
- 症状字典
- 严重程度字典
- 持续时间字典
- 伴随症状字典
- 特殊风险标签字典
- 问诊标签字典
- 紧急等级字典

### 9.3 推荐数据表

#### `db_body_part_dict`

- `id`
- `name`
- `code`
- `parent_id`
- `sort`
- `status`

#### `db_symptom_dict`

- `id`
- `name`
- `code`
- `body_part_id`
- `keywords`
- `alias_keywords`
- `description`
- `sort`
- `status`

#### `db_triage_level_dict`

紧急程度建议至少分为：

- `emergency`：立即急诊
- `urgent`：尽快线下就医
- `normal`：可在线咨询
- `followup`：复诊/随访

#### `db_red_flag_rule`

用于存放高危词与高危组合规则。

建议字段：

- `id`
- `rule_name`
- `rule_code`
- `trigger_type`
- `trigger_condition`
- `triage_level`
- `suggestion`
- `status`
- `priority`

### 9.4 后续页面

- 管理员端：症状字典管理
- 管理员端：身体部位管理
- 管理员端：紧急等级管理
- 管理员端：红旗规则管理

## 10. 基础建设六：AI 导诊知识库与案例库

### 10.1 为什么必须做

AI 导诊不能只依赖通用模型知识。

系统需要有“本平台自己的知识层”，否则 AI 无法稳定输出和你业务一致的结果。

比如：

- 平台有哪些科室
- 各科室适合处理什么问题
- 各医生擅长什么
- 哪些情况必须建议线下就医
- 哪类报告适合哪些医生解读

### 10.2 建议建设的知识类型

- 科室适用范围知识
- 医生专长知识
- 导诊规则知识
- 常见症状问答知识
- 用户教育类知识
- 历史案例摘要知识

### 10.3 推荐数据表

#### `db_triage_knowledge`

建议字段：

- `id`
- `knowledge_type`
- `title`
- `content`
- `tags`
- `department_id`
- `doctor_id`
- `source_type`
- `status`
- `version`
- `create_time`
- `update_time`

#### `db_triage_case_reference`

与首页展示案例不同，这里建议保留“供 AI 学习和检索的内部结构化案例”。

建议字段：

- `id`
- `title`
- `chief_complaint`
- `symptom_summary`
- `triage_result`
- `department_id`
- `doctor_id`
- `risk_level`
- `tags`
- `status`
- `create_time`

### 10.4 后续页面

- 管理员端：导诊知识库管理
- 管理员端：导诊案例库管理
- 管理员端：知识启停与版本管理

## 11. 基础建设七：AI 导诊会话、消息与结果留痕

### 11.1 为什么必须做

AI 导诊不是一次接口调用就结束。

必须要有完整留痕，后续才能做：

- 结果回看
- 医生接诊前摘要查看
- 投诉与异常排查
- 模型效果评估
- 规则优化
- AI 输出审计

### 11.2 推荐会话模型

一次完整导诊建议拆为：

- 一个导诊会话
- 多条对话消息
- 一个或多个阶段性推荐结果
- 一个最终导诊结果

### 11.3 推荐数据表

#### `db_triage_session`

建议字段：

- `id`
- `account_id`
- `patient_id`
- `category_id`
- `session_no`
- `chief_complaint`
- `current_status`
- `triage_level`
- `final_department_id`
- `final_doctor_id`
- `recommended_visit_type`
- `summary`
- `risk_flag`
- `source`
- `create_time`
- `update_time`

#### `db_triage_message`

建议字段：

- `id`
- `session_id`
- `sender_type`
- `message_type`
- `content`
- `structured_payload`
- `model_name`
- `prompt_version`
- `create_time`

`sender_type` 建议包括：

- `user`
- `assistant`
- `system`
- `rule_engine`

#### `db_triage_result`

建议字段：

- `id`
- `session_id`
- `result_type`
- `triage_level`
- `department_id`
- `doctor_id`
- `doctor_candidates_json`
- `reason_text`
- `risk_flags_json`
- `symptom_extract_json`
- `confidence_score`
- `is_final`
- `create_time`

#### `db_triage_feedback`

建议字段：

- `id`
- `session_id`
- `user_score`
- `is_adopted`
- `feedback_text`
- `manual_correct_department_id`
- `manual_correct_doctor_id`
- `create_time`

### 11.4 用户与医生端能力

- 用户端可以查看历史导诊记录
- 医生端可以查看 AI 生成的导诊摘要
- 管理员端可以回溯某次推荐为什么得出该结论

## 12. 基础建设八：规则引擎与 AI 结合机制

### 12.1 为什么不能只靠模型

如果只让模型自由回答，会带来三个问题：

- 紧急情况不稳定
- 推荐结果难以解释
- 不同时间同类输入输出可能波动较大

### 12.2 推荐机制

建议拆成三层：

#### 第一层：硬规则拦截

处理绝对不能放给模型自由发挥的场景，例如：

- 胸痛 + 呼吸困难
- 大出血
- 持续高热 + 抽搐
- 意识不清
- 严重外伤
- 婴幼儿持续高危症状

输出直接为：

- 紧急级别
- 是否终止在线导诊
- 是否建议立即急诊

#### 第二层：AI 语义理解

用于完成：

- 主诉归纳
- 症状抽取
- 风险标签补充
- 科室候选排序
- 医生候选排序
- 导诊总结生成

#### 第三层：业务规则校正

根据平台现有真实资源再做修正，例如：

- 科室是否启用
- 医生是否启用
- 医生是否有排班
- 医生是否支持对应问诊类型
- 医生优先级与接诊容量

### 12.3 推荐结果结构

建议 AI 最终输出为结构化 JSON，而不是只返回一段自然语言。

建议结果字段：

- `triageLevel`
- `riskFlags`
- `recommendedDepartmentId`
- `recommendedDepartmentName`
- `recommendedDoctorIds`
- `recommendedVisitType`
- `reasonSummary`
- `nextQuestions`
- `shouldEscalateToHuman`
- `suggestOfflineImmediately`

## 13. 基础建设九：安全、合规与风控边界

### 13.1 这是必须建设的底层能力

AI 导诊属于高风险业务场景，必须从一开始就设计边界。

### 13.2 建议建设内容

- 问诊前免责声明
- 紧急情况提示
- 隐私政策与授权确认
- 数据脱敏策略
- AI 回答敏感词审查
- 高风险场景强提醒
- 人工接管开关
- 模型开关与回退策略

### 13.3 必须有的系统能力

- 每次 AI 调用留痕
- 每次 prompt/version 留痕
- 每次规则命中留痕
- 每次人工纠正留痕
- 支持关闭某个模型或某条规则
- 支持将某类问诊临时切换为人工模式

### 13.4 风险提示建议

页面层面必须明确提示：

- AI 导诊仅作健康咨询和分诊参考
- 不能替代医生面诊
- 出现急危重症请立即线下就医

## 14. 基础建设十：后台运营与配置中心

AI 导诊不是一次性开发，后续需要持续运营。

所以管理员后台需要新增专门的导诊配置能力。

### 14.1 建议新增后台模块

- 就诊人/健康档案规则支持
- 医生排班管理
- 医生服务能力标签管理
- 问诊分类管理
- 前置资料模板管理
- 症状字典管理
- 红旗规则管理
- 导诊知识库管理
- AI 模型配置管理
- Prompt 模板管理
- 导诊记录查看
- 导诊反馈与纠偏统计

### 14.2 推荐新增后台菜单

- `就诊人管理`
- `健康标签管理`
- `医生排班管理`
- `问诊分类管理`
- `导诊字典管理`
- `红旗规则管理`
- `导诊知识库`
- `AI 导诊配置`
- `导诊记录中心`
- `导诊效果分析`

## 15. 用户端需要新增的页面

为了真正让 AI 导诊能被用户使用，前台至少要有以下页面。

### 15.1 第一批建议新增

- 就诊人管理页
- 健康档案页
- 发起咨询前置资料页
- AI 导诊对话页
- 导诊结果页
- 导诊历史页

### 15.2 导诊结果页建议展示内容

- 推荐科室
- 推荐医生列表
- 紧急程度
- 推荐理由
- 已识别症状摘要
- 是否建议线下就医
- 下一步操作按钮
  - 去在线咨询
  - 去预约医生
  - 继续补充信息
  - 线下就医提醒

## 16. 推荐的数据表建设清单

下面给出一个更适合后续继续落地的推荐表清单。

### 16.1 必做基础表

- `db_patient_profile`
- `db_patient_medical_history`
- `db_patient_health_tag`
- `db_doctor_service_tag`
- `db_doctor_service_setting`
- `db_doctor_schedule`
- `db_consultation_category`
- `db_consultation_intake_template`
- `db_consultation_intake_field`
- `db_body_part_dict`
- `db_symptom_dict`
- `db_triage_level_dict`
- `db_red_flag_rule`

### 16.2 AI 导诊核心表

- `db_triage_session`
- `db_triage_message`
- `db_triage_result`
- `db_triage_feedback`
- `db_triage_knowledge`
- `db_triage_case_reference`

### 16.3 可后置优化表

- `db_triage_prompt_template`
- `db_triage_model_config`
- `db_triage_audit_log`
- `db_triage_rule_hit_log`
- `db_triage_recommend_doctor_log`

## 17. 建议的开发阶段顺序

为了避免一次铺得太大，建议分阶段推进。

## 18. 第一阶段：继续补齐基础信息管理

这一阶段暂时还不接 AI，只做 AI 未来依赖的数据层。

### 第一阶段目标

- 账号与就诊人分离
- 健康档案可维护
- 医生排班与服务能力可维护
- 问诊分类与前置模板可维护

### 第一阶段建议优先级

1. 就诊人管理
2. 健康档案管理
3. 医生排班管理
4. 医生服务标签管理
5. 问诊分类管理
6. 前置模板管理

## 19. 第二阶段：做规则导诊 MVP

这一阶段先不上复杂 AI，对话可先弱化，先把“规则导诊闭环”跑通。

### 第二阶段目标

- 用户填写前置资料
- 系统基于症状字典和红旗规则进行初步分流
- 输出科室与紧急等级建议
- 保存导诊会话与结果

### 这样做的价值

- 先验证业务闭环
- 先积累真实导诊数据
- 为后续 AI 模型接入准备训练与评估样本

## 20. 第三阶段：接入 AI 语义理解

### 第三阶段目标

- 支持用户自由输入主诉
- AI 提取结构化症状信息
- AI 辅助推荐科室、医生和下一步动作
- AI 自动生成导诊摘要

### 这一阶段的重点

- 结构化输出
- Prompt 版本管理
- 模型调用留痕
- 规则与模型冲突处理

## 21. 第四阶段：做推荐分配与持续优化

### 第四阶段目标

- 结合医生排班、服务能力、优先级进行智能分配
- 支持根据用户反馈优化推荐
- 支持管理员查看推荐命中率与人工纠偏结果

### 可优化指标

- 科室推荐命中率
- 医生推荐点击率
- 用户采纳率
- 人工纠正率
- 高风险命中召回率

## 22. 推荐的近期开发路线

结合你当前“先做基础信息管理，再做问诊功能”的思路，最合理的近期路线建议如下：

### 22.1 先做的内容

1. 就诊人管理
2. 健康档案管理
3. 医生排班管理
4. 医生服务标签管理
5. 问诊分类管理
6. 前置资料模板管理
7. 症状字典与红旗规则管理

### 22.2 然后做的内容

1. 发起导诊页
2. 导诊前置表单页
3. 导诊结果页
4. 导诊记录页
5. 导诊会话表与结果表

### 22.3 最后再做的内容

1. AI 对话导诊
2. AI 科室推荐
3. AI 医生推荐分配
4. AI 摘要生成
5. AI 反馈学习与优化

## 23. 结论

如果后面要把 AI 智能导诊做稳，不建议一上来就开发“AI 聊天页”。

更正确的顺序是：

- 先把患者、病史、医生能力、问诊分类、症状字典这些底层基础数据建好
- 再把规则导诊和导诊留痕闭环跑通
- 最后接入 AI 做语义理解、推荐排序和总结增强

一句话概括就是：

先建设“可被 AI 使用的数据系统”，再建设“AI 本身”。

## 24. 下一步建议

如果按这份指南继续推进，建议下一个实际开发任务直接定为：

### 方案 A：优先做患者基础层

- 就诊人管理
- 健康档案管理

### 方案 B：优先做医生承接层

- 医生排班管理
- 医生服务标签管理

### 方案 C：优先做导诊配置层

- 问诊分类管理
- 前置资料模板管理
- 症状字典管理
- 红旗规则管理

从整体收益看，最推荐先做 `方案 A + 方案 B`，因为这两块是后续问诊和 AI 导诊最核心的真实业务基础。
