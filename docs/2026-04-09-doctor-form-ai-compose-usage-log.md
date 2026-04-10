# 2026-04-09 医生处理 / 随访 AI+模板拼装留痕开发记录

## 1. 本轮目标

上一轮已经完成：

- 医生处理 / 随访 AI 草稿生成
- 表单字段级 `AI+模板拼装`
- 草稿生成、带入、保存的基础留痕

但当时还缺一层关键治理能力：

- 后端无法区分“纯 AI 带入”
- 后端无法识别“AI+模板拼装”
- 管理端也看不到这两类使用差异

所以本轮重点不是再开新页面，而是在现有留痕链路上补齐这部分统计能力。

## 2. 本轮完成

### 2.1 表单 AI 日志新增拼装元数据

更新：

- `template-backend/src/main/java/cn/gugufish/entity/dto/DoctorFormAiLog.java`
- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-04-09-doctor-form-ai-log.sql`

在 `db_doctor_form_ai_log` 中新增：

- `last_apply_mode`
- `template_scene_type`
- `template_id`
- `template_title`
- `template_used`

这样医生处理 / 随访表单的 AI 草稿日志，除了知道“是否带入”，还能知道：

- 最后一次是直接带入、追加带入，还是 `AI+模板拼装`
- 使用的是哪个模板场景
- 使用了哪条模板

升级脚本也补了兼容旧表结构的字段追加逻辑，避免已有库因为表已存在而缺字段。

### 2.2 表单带入接口支持回传拼装信息

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationFormDraftApplyVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

接口：

- `POST /api/doctor/consultation/form/ai-draft/apply`

现在除了原来的：

- `recordId`
- `logId`
- `applyTarget`

还支持：

- `applyMode`
- `templateSceneType`
- `templateId`
- `templateTitle`

医生页在发生字段级 `AI+模板拼装` 时，会把这些元数据一起回传给后端，日志会同步回写。

### 2.3 医生页拼装操作正式纳入留痕

更新：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

本轮没有改动原有交互，只是把现有 `applyFormAiUsage(...)` 扩展为可携带模板元数据。

现在这些动作都会进入统一留痕：

- 处理草稿直接带入
- 结构化结论带入
- 随访草稿直接带入
- 处理 / 结论 / 随访字段的 `AI+模板拼装`

其中拼装场景会自动记录当前选中的模板信息。

### 2.4 管理端补齐纯 AI / AI+模板统计

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageOverviewVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageSceneVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageItemVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

管理端“医生端 AI 处理 / 随访草稿使用”现在新增可见性：

- 纯 AI 带入数
- `AI+模板拼装` 数
- 场景维度的纯 AI / 拼装 / 保存分布
- 最近拼装或直接带入记录中的带入模式、模板标题

这样后续就能继续回答：

- 医生更常直接采用 AI，还是更常先拼模板
- 哪一类场景更依赖模板拼装
- 拼装后的草稿最终有没有形成保存采纳

## 3. 涉及文件

- `template-backend/src/main/java/cn/gugufish/entity/dto/DoctorFormAiLog.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationFormDraftApplyVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageOverviewVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageSceneVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageItemVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`
- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-04-09-doctor-form-ai-log.sql`

## 4. 验证

本轮已完成验证：

- 前端执行 `npm run build` 通过
- 后端执行 `mvn -q -DskipTests compile` 通过

## 5. 下一步建议

建议下一轮继续优先推进：

1. 给处理草稿和随访草稿补“按字段重新生成”，例如只重写随访建议
2. 基于当前留痕，在医生端推荐高采纳模板或高采纳草稿片段
3. 将“拼装后保存率”继续下钻到模板维度，识别高价值模板
