# 2026-04-10 医生首条消息自动进入处理中记录

## 本轮目标

继续补齐在线问诊的非 AI 主流程，解决下面这类状态断层：

- 医生发送首条消息后虽然会自动认领，但问诊状态仍可能停留在“已提交 / 已分诊”
- 详情里可以看到消息往来，但后端没有真正记录“开始接诊”
- 患者端、医生端和后续统计口径之间容易出现“已经沟通了，但还没开始处理”的语义不一致

## 本轮完成

- 医生发送首条消息成功后，自动同步问诊单进入 `processing`
- 若当前还没有医生处理记录，则自动创建一条 `db_consultation_doctor_handle`
- 自动写入医生、科室和 `receiveTime`
- 已完成问诊不会因为后续沟通被错误回退成处理中
- 医生端发送成功提示和发送前说明同步更新

## 后端改动

更新文件：

- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationMessageServiceImpl.java`

新增逻辑：

- `sendDoctorMessage(...)` 在消息保存成功后，继续执行接诊状态同步
- 新增 `syncDoctorProcessingState(...)`

处理规则：

- 若问诊未完成，医生首条消息会将问诊状态推进到 `processing`
- 若还没有 `ConsultationDoctorHandle`，则自动插入一条处理中记录
- 若已存在处理中记录，则补齐接诊时间和医生快照
- 若问诊或处理单已经完成，则不会回退状态

## 前端改动

更新文件：

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

调整内容：

- 发送前提示改为“发送首条消息会自动认领并进入处理中”
- 发送成功提示会根据当前状态提示：
  - 自动认领并进入处理中
  - 仅自动认领
  - 已进入处理中
  - 普通发送成功

## 用户价值

- 医生开始与患者沟通后，系统会把这次沟通正式视为已接诊
- 患者侧看到的“处理中”不再只是前端推断，而是后端真实状态
- 后续处理、提醒、统计、随访都能基于一致的状态流转继续扩展

## 验证

已完成验证：

- 前端：`cmd /c npm run build`
- 后端：`mvn -q -DskipTests compile`
