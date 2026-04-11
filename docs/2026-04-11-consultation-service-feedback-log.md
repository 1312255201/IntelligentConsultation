# 2026-04-11 问诊服务评价闭环记录
## 本轮目标

继续围绕“先把主要的在线问诊写完，AI 先放一放”的方向，补齐医生完成问诊后的最后一段闭环能力：

- 患者完成线上问诊后，可以对医生服务进行评分
- 患者可以标记本次问题是否已经解决
- 患者可以补充文字反馈
- 医生端和管理端可以直接查看该评价结果

这一轮不扩展新的 AI 能力，重点是把在线问诊主链路补完整。

## 本轮完成

- 新增问诊服务评价数据表 `db_consultation_service_feedback`
- 新增患者提交问诊服务评价接口 `POST /api/user/consultation/service-feedback/submit`
- 患者端问诊详情支持提交和更新服务评价
- 医生端问诊详情支持查看患者提交的服务评价
- 管理端问诊记录详情支持查看患者提交的服务评价
- 问诊详情返回结构补充 `serviceFeedback` 字段，前后端统一读取

## 后端改动

新增文件：

- `template-backend/src/main/java/cn/gugufish/entity/dto/ConsultationServiceFeedback.java`
- `template-backend/src/main/java/cn/gugufish/mapper/ConsultationServiceFeedbackMapper.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/ConsultationServiceFeedbackSubmitVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationServiceFeedbackVO.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationServiceFeedbackService.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationServiceFeedbackQueryService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceFeedbackServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceFeedbackQueryServiceImpl.java`
- `sql/mysql57-upgrade-2026-04-11-consultation-service-feedback.sql`

更新文件：

- `sql/mysql57-init.sql`
- `template-backend/src/main/java/cn/gugufish/controller/ConsultationController.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationRecordVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationRecordVO.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`

实现说明：

- 评价数据与问诊记录一对一关联，支持首次提交和后续更新
- 仅允许当前问诊所属患者提交评价
- 仅允许已完成的医生处理记录提交评价
- 评价信息会自动带出患者、医生、科室等快照信息，便于后台查看

## 前端改动

更新文件：

- `template-front/src/views/index/ConsultationPage.vue`
- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/admin/ConsultationRecordPage.vue`

实现说明：

- 患者端在问诊详情中新增“问诊服务评价”板块
- 患者可选择 1-5 分服务评分、标记问题是否解决，并填写补充说明
- 已存在评价时，页面会自动回填当前内容，支持二次更新
- 医生端详情中新增服务评价展示，便于医生复盘服务质量
- 管理端详情中新增服务评价展示，便于统一巡检和运营分析

## 用户价值

- 在线问诊从“提交 -> 分诊 -> 医生接诊 -> 医患沟通 -> 结论/随访”延伸到“服务反馈”，链路更完整
- 患者可以明确表达本次问诊体验和问题是否解决
- 医生和后台可以更直接地看到问诊服务结果，为后续优化提供依据

## 备注

- 本轮仍未继续扩展 DeepSeek / Spring AI 接入能力
- 下一步可以继续完善在线问诊本身的运营能力，例如评价统计、医生服务质量看板、患者回访与催评机制
