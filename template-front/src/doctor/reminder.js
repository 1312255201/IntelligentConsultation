import { isRecommendedToDoctor, normalizeSmartDispatch } from '@/triage/dispatch'

function ensureArray(value) {
  return Array.isArray(value) ? value.filter(item => item != null) : []
}

function ensureObject(value) {
  return value && typeof value === 'object' ? value : null
}

function sortByCreateTimeDesc(records = []) {
  return ensureArray(records).slice().sort((left, right) => {
    const timeDiff = compareDateDesc(left?.createTime || left?.updateTime, right?.createTime || right?.updateTime)
    if (timeDiff !== 0) return timeDiff
    return compareNumber(Number(right?.id || 0), Number(left?.id || 0))
  })
}

function sortBySortAsc(records = []) {
  return ensureArray(records).slice().sort((left, right) => {
    const sortDiff = compareNumber(Number(left?.sort || 0), Number(right?.sort || 0))
    if (sortDiff !== 0) return sortDiff
    return compareNumber(Number(left?.id || 0), Number(right?.id || 0))
  })
}

function normalizeArchiveSummary(summary) {
  const record = ensureObject(summary)
  if (!record) return null
  return {
    ...record,
    riskFlags: ensureArray(record.riskFlags).filter(Boolean),
    conclusionTags: ensureArray(record.conclusionTags).filter(Boolean),
    nextActions: ensureArray(record.nextActions).filter(Boolean)
  }
}

function normalizeTriageSession(session) {
  const record = ensureObject(session)
  if (!record) return null
  return {
    ...record,
    messages: ensureArray(record.messages)
  }
}

function normalizePayment(payment) {
  const record = ensureObject(payment)
  if (!record) return null
  const amount = Number(record.amount ?? 0)
  return {
    ...record,
    amount: Number.isFinite(amount) ? amount : 0
  }
}

export function normalizeDoctorMessageSummary(summary) {
  return {
    totalCount: Number(summary?.totalCount || 0),
    userMessageCount: Number(summary?.userMessageCount || 0),
    doctorMessageCount: Number(summary?.doctorMessageCount || 0),
    unreadCount: Number(summary?.unreadCount || 0),
    unreadByDoctorCount: Number(summary?.unreadByDoctorCount ?? summary?.unreadCount ?? 0),
    unreadByPatientCount: Number(summary?.unreadByPatientCount ?? 0),
    latestSenderType: summary?.latestSenderType || '',
    latestSenderName: summary?.latestSenderName || '',
    latestMessageType: summary?.latestMessageType || '',
    latestMessagePreview: summary?.latestMessagePreview || '',
    latestTime: summary?.latestTime || null
  }
}

export function normalizeDoctorServiceFeedback(feedback) {
  const record = ensureObject(feedback)
  if (!record) return null
  return {
    ...record,
    serviceScore: record?.serviceScore == null ? null : Number(record.serviceScore),
    isResolved: record?.isResolved == null ? null : Number(record.isResolved),
    doctorHandleStatus: record?.doctorHandleStatus == null ? null : Number(record.doctorHandleStatus),
    doctorHandleAccountId: record?.doctorHandleAccountId == null ? null : Number(record.doctorHandleAccountId),
    doctorHandleDoctorId: record?.doctorHandleDoctorId == null ? null : Number(record.doctorHandleDoctorId)
  }
}

export function normalizeDoctorReminderRecord(record = {}) {
  const source = ensureObject(record) || {}
  return {
    ...source,
    answers: sortBySortAsc(source?.answers),
    prescriptions: ensureArray(source?.prescriptions),
    checkSuggestions: sortBySortAsc(source?.checkSuggestions),
    reportFeedbacks: sortByCreateTimeDesc(source?.reportFeedbacks).map(item => ({
      ...item,
      attachments: ensureArray(item?.attachments)
    })),
    medicationFeedbacks: sortByCreateTimeDesc(source?.medicationFeedbacks).map(item => ({
      ...item,
      attachments: ensureArray(item?.attachments)
    })),
    recommendedDoctors: ensureArray(source?.recommendedDoctors),
    doctorAssignment: ensureObject(source?.doctorAssignment),
    doctorHandle: ensureObject(source?.doctorHandle),
    doctorConclusion: ensureObject(source?.doctorConclusion),
    doctorFollowUps: sortByCreateTimeDesc(source?.doctorFollowUps),
    smartDispatch: normalizeSmartDispatch(source?.smartDispatch),
    messageSummary: normalizeDoctorMessageSummary(source?.messageSummary),
    archiveSummary: normalizeArchiveSummary(source?.archiveSummary),
    aiComparison: ensureObject(source?.aiComparison),
    triageSession: normalizeTriageSession(source?.triageSession),
    triageResult: ensureObject(source?.triageResult),
    triageFeedback: ensureObject(source?.triageFeedback),
    ruleHits: ensureArray(source?.ruleHits),
    payment: normalizePayment(source?.payment),
    serviceFeedback: normalizeDoctorServiceFeedback(source?.serviceFeedback)
  }
}

export function normalizeDoctorReminderRecords(records = []) {
  return ensureArray(records).map(item => normalizeDoctorReminderRecord(item))
}

export function getDoctorMessageSummary(record) {
  return normalizeDoctorMessageSummary(record?.messageSummary)
}

export function hasUnreadMessages(record) {
  return getDoctorMessageSummary(record).unreadCount > 0
}

export function isFollowUpUpdateMessageType(messageType) {
  return `${messageType || ''}`.trim().toLowerCase() === 'followup_update'
}

export function isCheckResultUpdateMessageType(messageType) {
  return `${messageType || ''}`.trim().toLowerCase() === 'check_result_update'
}

export function isMedicationFeedbackMessageType(messageType) {
  return `${messageType || ''}`.trim().toLowerCase() === 'medication_feedback'
}

export function isDoctorGuidanceAckMessageType(messageType) {
  return `${messageType || ''}`.trim().toLowerCase() === 'doctor_guidance_ack'
}

export function waitingDoctorReply(record) {
  const summary = getDoctorMessageSummary(record)
  return summary.totalCount > 0
    && summary.latestSenderType === 'user'
    && !isDoctorGuidanceAckMessageType(summary.latestMessageType)
}

export function messageProgressLabel(record) {
  const summary = getDoctorMessageSummary(record)
  const isFollowUpUpdate = isFollowUpUpdateMessageType(summary.latestMessageType)
  const isCheckResultUpdate = isCheckResultUpdateMessageType(summary.latestMessageType)
  const isMedicationFeedback = isMedicationFeedbackMessageType(summary.latestMessageType)
  const isGuidanceAck = isDoctorGuidanceAckMessageType(summary.latestMessageType)
  if (summary.totalCount <= 0) return '暂无沟通'
  if (summary.latestSenderType === 'user' && isGuidanceAck) return '患者已确认查看'
  if (summary.unreadCount > 0 && isMedicationFeedback) return '患者提交用药反馈'
  if (summary.latestSenderType === 'user' && isMedicationFeedback) return '待查看用药反馈'
  if (summary.unreadCount > 0 && isCheckResultUpdate) return '患者补充检查结果'
  if (summary.latestSenderType === 'user' && isCheckResultUpdate) return '待查看检查结果'
  if (summary.totalCount <= 0) return '未沟通'
  if (summary.unreadCount > 0) return isFollowUpUpdate ? '患者恢复更新' : '患者新消息'
  if (summary.latestSenderType === 'user') return isFollowUpUpdate ? '待跟进恢复更新' : '待医生回复'
  if (summary.latestSenderType === 'doctor') return '已回复患者'
  return '沟通中'
}

export function messageProgressType(record) {
  const summary = getDoctorMessageSummary(record)
  const isFollowUpUpdate = isFollowUpUpdateMessageType(summary.latestMessageType)
  const isCheckResultUpdate = isCheckResultUpdateMessageType(summary.latestMessageType)
  const isMedicationFeedback = isMedicationFeedbackMessageType(summary.latestMessageType)
  const isGuidanceAck = isDoctorGuidanceAckMessageType(summary.latestMessageType)
  if (summary.totalCount <= 0) return 'info'
  if (summary.latestSenderType === 'user' && isGuidanceAck) return 'success'
  if (summary.unreadCount > 0 && isMedicationFeedback) return 'warning'
  if (summary.latestSenderType === 'user' && isMedicationFeedback) return 'primary'
  if (summary.unreadCount > 0 && isCheckResultUpdate) return 'success'
  if (summary.latestSenderType === 'user' && isCheckResultUpdate) return 'success'
  if (summary.totalCount <= 0) return 'info'
  if (summary.unreadCount > 0) return isFollowUpUpdate ? 'warning' : 'danger'
  if (summary.latestSenderType === 'user') return isFollowUpUpdate ? 'primary' : 'warning'
  if (summary.latestSenderType === 'doctor') return 'success'
  return 'info'
}

export function doctorMessagePreview(record) {
  const summary = getDoctorMessageSummary(record)
  const preview = summary.latestMessagePreview || ''
  if (!preview) return ''
  if (isDoctorGuidanceAckMessageType(summary.latestMessageType)) {
    return preview.startsWith('[已确认查看]') ? preview : `[已确认查看] ${preview}`
  }
  if (isMedicationFeedbackMessageType(summary.latestMessageType)) {
    return preview.startsWith('[用药反馈]') ? preview : `[用药反馈] ${preview}`
  }
  if (isCheckResultUpdateMessageType(summary.latestMessageType)) {
    return preview.startsWith('[检查结果]') ? preview : `[检查结果] ${preview}`
  }
  if (!isFollowUpUpdateMessageType(summary.latestMessageType)) return preview
  if (preview.startsWith('[恢复更新]') || preview.startsWith('[Recovery Update]')) return preview
  return `[恢复更新] ${preview}`
}

export function ownerType(record, doctorId) {
  const assignment = record?.doctorAssignment
  if (!assignment || assignment.status !== 'claimed') return 'unclaimed'
  return Number(assignment.doctorId || 0) === Number(doctorId || 0) ? 'mine' : 'others'
}

export function isRecommendedConsultation(record, doctorId) {
  return isRecommendedToDoctor(record?.smartDispatch, doctorId)
}

export function latestFollowUp(record) {
  return Array.isArray(record?.doctorFollowUps) && record.doctorFollowUps.length
    ? record.doctorFollowUps[0]
    : null
}

export function hasServiceFeedback(record) {
  return !!record?.serviceFeedback
}

export function isDoctorServiceFeedbackRecord(record, doctorId) {
  if (record?.status !== 'completed' || !hasServiceFeedback(record)) return false
  const feedbackDoctorId = Number(record?.serviceFeedback?.doctorId || 0)
  return feedbackDoctorId > 0 && feedbackDoctorId === Number(doctorId || 0)
}

export function isAttentionServiceFeedbackRecord(record, doctorId) {
  if (!isDoctorServiceFeedbackRecord(record, doctorId)) return false
  if (record?.serviceFeedback?.doctorHandleStatus === 1) return false
  if (record?.serviceFeedback?.isResolved !== 1) return true
  return Number(record?.serviceFeedback?.serviceScore || 0) <= 2
}

export function serviceFeedbackState(record, doctorId) {
  if (!isDoctorServiceFeedbackRecord(record, doctorId)) return 'none'
  if (record?.serviceFeedback?.doctorHandleStatus === 1) return 'handled'
  const score = Number(record?.serviceFeedback?.serviceScore || 0)
  if (record?.serviceFeedback?.isResolved !== 1 && score > 0 && score <= 2) return 'critical'
  if (record?.serviceFeedback?.isResolved !== 1) return 'pending'
  if (score > 0 && score <= 2) return 'low_score'
  return 'resolved'
}

export function serviceFeedbackTime(record) {
  return record?.serviceFeedback?.updateTime || record?.serviceFeedback?.createTime || null
}

export function serviceFeedbackLabel(record, doctorId) {
  const state = serviceFeedbackState(record, doctorId)
  if (state === 'handled') return '已处理回看'
  if (state === 'critical') return '低分且未解决'
  if (state === 'pending') return '未解决评价'
  if (state === 'low_score') return '低分评价'
  if (state === 'resolved') return '已评价'
  return '暂无评价'
}

export function serviceFeedbackReminderText(record, doctorId) {
  const feedback = record?.serviceFeedback
  if (!feedback || !isDoctorServiceFeedbackRecord(record, doctorId)) return '当前暂无服务评价提醒'
  const scoreText = feedback.serviceScore ? `${feedback.serviceScore}/5` : '-'
  const content = `${feedback.feedbackText || ''}`.trim()
  const state = serviceFeedbackState(record, doctorId)
  if (state === 'handled') {
    return `${feedback.doctorHandleRemark || '医生已回看并记录本次服务评价处理结果。'}`
  }
  if (state === 'critical') return `患者给出 ${scoreText} 的低分评价，且反馈问题仍未解决，建议优先回看本次问诊。${content ? ` 评价内容：${content}` : ''}`
  if (state === 'pending') return `患者反馈本次问题仍需继续处理，建议结合问诊结论与随访计划继续跟进。${content ? ` 评价内容：${content}` : ''}`
  if (state === 'low_score') return `患者给出 ${scoreText} 的低分服务评价，建议回看本次接诊体验。${content ? ` 评价内容：${content}` : ''}`
  return content || `患者已提交 ${scoreText} 的服务评价。`
}

export function followUpDueDate(record) {
  const latest = latestFollowUp(record)
  if (latest?.nextFollowUpDate) return latest.nextFollowUpDate
  if (record?.doctorConclusion?.needFollowUp !== 1) return null
  const followUpWithinDays = Number(record?.doctorConclusion?.followUpWithinDays || 0)
  if (!Number.isFinite(followUpWithinDays) || followUpWithinDays <= 0) return null
  const baseTime = record?.doctorConclusion?.updateTime || record?.doctorHandle?.completeTime || record?.updateTime
  if (!baseTime) return null
  const dueDate = new Date(baseTime)
  dueDate.setHours(0, 0, 0, 0)
  dueDate.setDate(dueDate.getDate() + followUpWithinDays)
  return dueDate
}

export function isPendingFollowUpRecord(record) {
  if (record?.status !== 'completed') return false
  if (record?.doctorHandle?.status !== 'completed') return false
  if (record?.doctorConclusion?.needFollowUp !== 1) return false
  return latestFollowUp(record)?.needRevisit !== 0
}

export function followUpState(record) {
  if (!record) return 'none'
  if (!isPendingFollowUpRecord(record)) {
    return record?.doctorConclusion?.needFollowUp === 1 && latestFollowUp(record)?.needRevisit === 0
      ? 'done'
      : 'none'
  }
  const dueValue = followUpDueDate(record)
  if (!dueValue) return 'pending'
  const dueDate = new Date(dueValue)
  dueDate.setHours(0, 0, 0, 0)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  if (dueDate.getTime() < today.getTime()) return 'overdue'
  if (dueDate.getTime() === today.getTime()) return 'due_today'
  return 'pending'
}

export function isRiskConsultation(record) {
  return ['emergency', 'offline'].includes(record?.triageActionType)
}

export function formatDoctorReminderDate(value, onlyDate = false) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', onlyDate
    ? { year: 'numeric', month: '2-digit', day: '2-digit' }
    : { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
  ).format(new Date(value))
}

export function followUpTagLabel(record) {
  const state = followUpState(record)
  if (state === 'overdue') return '已逾期'
  if (state === 'due_today') return '今日到期'
  if (state === 'pending') return '待随访'
  if (state === 'done') return '本轮已完成'
  return '无需随访'
}

export function followUpTagType(record) {
  const state = followUpState(record)
  if (state === 'overdue') return 'danger'
  if (state === 'due_today') return 'warning'
  if (state === 'pending') return 'primary'
  if (state === 'done') return 'success'
  return 'info'
}

export function followUpLine(record) {
  const latest = latestFollowUp(record)
  const dueValue = followUpDueDate(record)
  const state = followUpState(record)
  if (state === 'overdue') return `应于 ${formatDoctorReminderDate(dueValue, true)} 前完成随访`
  if (state === 'due_today') return '建议今天完成本轮随访'
  if (state === 'pending') return dueValue ? `计划于 ${formatDoctorReminderDate(dueValue, true)} 跟进` : '已标记后续继续跟进'
  if (state === 'done') return latest?.createTime ? `最近随访 ${formatDoctorReminderDate(latest.createTime)}` : '当前轮次已完成'
  return '当前未设置继续随访'
}

export function followUpReminderText(record) {
  const state = followUpState(record)
  if (state === 'overdue') return `当前问诊已进入逾期随访状态，建议尽快完成本轮随访。计划时间：${formatDoctorReminderDate(followUpDueDate(record), true)}`
  if (state === 'due_today') return '当前问诊的随访计划今天到期，建议本日内完成回访并补录记录。'
  if (state === 'pending') return `当前问诊仍处于待随访状态${followUpDueDate(record) ? `，计划时间：${formatDoctorReminderDate(followUpDueDate(record), true)}` : ''}`
  if (state === 'done') return '本轮随访已完成，如需继续追踪可追加新的随访记录。'
  return '当前暂无随访提醒'
}

export function compareNumber(left, right) {
  if (left === right) return 0
  return left < right ? -1 : 1
}

export function toTimestamp(value) {
  if (!value) return null
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? null : time
}

export function compareDateAsc(left, right) {
  const leftTime = toTimestamp(left)
  const rightTime = toTimestamp(right)
  if (leftTime === rightTime) return 0
  if (leftTime === null) return 1
  if (rightTime === null) return -1
  return compareNumber(leftTime, rightTime)
}

export function compareDateDesc(left, right) {
  return compareDateAsc(right, left)
}

export function compareRecentDoctorRecord(left, right) {
  const createTimeDiff = compareDateDesc(left?.createTime, right?.createTime)
  if (createTimeDiff !== 0) return createTimeDiff
  return compareNumber(Number(right?.id || 0), Number(left?.id || 0))
}

export function patientUnreadDoctorReplyCount(record) {
  return getDoctorMessageSummary(record).unreadByPatientCount
}

export function hasPatientUnreadDoctorReply(record) {
  return patientUnreadDoctorReplyCount(record) > 0
}

export function hasLatestPatientFollowUpUpdate(record) {
  const summary = getDoctorMessageSummary(record)
  return summary.latestSenderType === 'user' && isFollowUpUpdateMessageType(summary.latestMessageType)
}

export function hasLatestPatientCheckResultUpdate(record) {
  const summary = getDoctorMessageSummary(record)
  return summary.latestSenderType === 'user' && isCheckResultUpdateMessageType(summary.latestMessageType)
}

export function hasLatestPatientMedicationFeedback(record) {
  const summary = getDoctorMessageSummary(record)
  return summary.latestSenderType === 'user' && isMedicationFeedbackMessageType(summary.latestMessageType)
}

export function hasLatestPatientGuidanceAck(record) {
  const summary = getDoctorMessageSummary(record)
  return summary.latestSenderType === 'user' && isDoctorGuidanceAckMessageType(summary.latestMessageType)
}

export function patientActionState(record) {
  const summary = getDoctorMessageSummary(record)
  const followState = followUpState(record)
  const feedback = record?.serviceFeedback
  const feedbackHandled = Number(feedback?.doctorHandleStatus || 0) === 1
  const feedbackResolved = Number(feedback?.isResolved || 0) === 1
  const feedbackScore = Number(feedback?.serviceScore || 0)
  if (patientUnreadDoctorReplyCount(record) > 0) return 'doctor_reply_unread_by_patient'
  if (hasLatestPatientMedicationFeedback(record)) return 'patient_medication_feedback'
  if (hasLatestPatientCheckResultUpdate(record)) return 'patient_check_result_update'
  if (hasLatestPatientFollowUpUpdate(record)) return 'patient_followup_update'
  if (feedback) {
    if (feedbackHandled) return 'service_feedback_handled'
    if (!feedbackResolved) return 'service_feedback_pending'
    if (feedbackScore > 0 && feedbackScore <= 2) {
      return 'service_feedback_low_score'
    }
    return 'service_feedback_submitted'
  }
  if (hasLatestPatientGuidanceAck(record)) return 'patient_acknowledged_guidance'
  if (followState === 'overdue') return 'followup_waiting_patient'
  if (followState === 'due_today') return 'followup_due_today'
  if (followState === 'pending') return 'followup_pending'
  if (summary.doctorMessageCount > 0) return 'doctor_reply_read_by_patient'
  return 'none'
}

export function patientActionLabel(record) {
  const state = patientActionState(record)
  if (state === 'doctor_reply_unread_by_patient') {
    const count = patientUnreadDoctorReplyCount(record)
    return count > 1 ? `待患者查看 ${count} 条` : '待患者查看'
  }
  if (state === 'patient_medication_feedback') return '已提交用药反馈'
  if (state === 'patient_check_result_update') return '已补充检查结果'
  if (state === 'patient_followup_update') return '已补充恢复更新'
  if (state === 'service_feedback_handled') return '评价已处理'
  if (state === 'service_feedback_pending') return '已提交评价'
  if (state === 'service_feedback_low_score') return '低分评价'
  if (state === 'service_feedback_submitted') return '已提交评价'
  if (state === 'patient_acknowledged_guidance') return '已确认查看医生建议'
  if (state === 'followup_waiting_patient') return '等待患者反馈'
  if (state === 'followup_due_today') return '今日随访'
  if (state === 'followup_pending') return '随访进行中'
  if (state === 'doctor_reply_read_by_patient') return '患者已读最近回复'
  return '暂无患者后续动作'
}

export function patientActionTagType(record) {
  const state = patientActionState(record)
  if (state === 'doctor_reply_unread_by_patient') return 'warning'
  if (state === 'patient_medication_feedback') return 'warning'
  if (state === 'patient_check_result_update') return 'success'
  if (state === 'patient_followup_update') return 'success'
  if (state === 'service_feedback_handled') return 'success'
  if (state === 'service_feedback_pending') return 'warning'
  if (state === 'service_feedback_low_score') return 'danger'
  if (state === 'service_feedback_submitted') return 'primary'
  if (state === 'patient_acknowledged_guidance') return 'success'
  if (state === 'followup_waiting_patient') return 'warning'
  if (state === 'followup_due_today') return 'primary'
  if (state === 'followup_pending') return 'info'
  if (state === 'doctor_reply_read_by_patient') return 'success'
  return 'info'
}

export function patientActionSummary(record) {
  const state = patientActionState(record)
  const summary = getDoctorMessageSummary(record)
  const preview = doctorMessagePreview(record)
  const followUpText = followUpReminderText(record)
  const feedback = record?.serviceFeedback
  const feedbackScore = feedback?.serviceScore == null ? '' : `评分 ${feedback.serviceScore}/5，`
  if (state === 'doctor_reply_unread_by_patient') {
    const count = patientUnreadDoctorReplyCount(record)
    if (preview) return `最近有 ${count} 条医生回复患者尚未查看，最新内容：${preview}`
    return `最近有 ${count} 条医生回复患者尚未查看。`
  }
  if (state === 'patient_medication_feedback') {
    return preview || '患者刚提交了用药反馈或疑似不良反应，建议优先查看是否需要调整处方。'
  }
  if (state === 'patient_check_result_update') {
    return preview || '患者刚补充了新的检查结果，可优先查看并决定后续处理。'
  }
  if (state === 'patient_followup_update') {
    return preview || '患者刚补充了恢复情况更新，可继续登记随访或补充指导。'
  }
  if (state === 'service_feedback_handled') {
    const remark = shortenDoctorReminderText(feedback?.doctorHandleRemark, 48)
    return remark || '患者服务评价已经登记处理结果。'
  }
  if (state === 'service_feedback_pending') {
    const content = shortenDoctorReminderText(feedback?.feedbackText, 48)
    return `${feedbackScore}患者反馈当前问题仍需继续处理${content ? `：${content}` : '。'}`
  }
  if (state === 'service_feedback_low_score') {
    const content = shortenDoctorReminderText(feedback?.feedbackText, 48)
    return `${feedbackScore}患者已提交低分评价${content ? `：${content}` : '。'}`
  }
  if (state === 'service_feedback_submitted') {
    const content = shortenDoctorReminderText(feedback?.feedbackText, 48)
    return `${feedbackScore}患者已提交本次服务评价${content ? `：${content}` : '。'}`
  }
  if (state === 'patient_acknowledged_guidance') {
    return preview || '患者已确认查看本轮医生结论或随访建议，当前无需额外提醒。'
  }
  if (state === 'followup_waiting_patient' || state === 'followup_due_today' || state === 'followup_pending') {
    return followUpText || '当前仍处于患者恢复跟踪阶段。'
  }
  if (state === 'doctor_reply_read_by_patient') {
    return preview ? `最近医生回复已被患者查看：${preview}` : '最近医生回复已被患者查看。'
  }
  if (summary.latestSenderType === 'user' && preview) return `患者最近补充：${preview}`
  return '当前还没有新的患者补充、检查结果或服务评价。'
}

function trimDoctorReminderText(value) {
  return `${value || ''}`.trim()
}

function shortenDoctorReminderText(value, maxLength = 48) {
  const text = trimDoctorReminderText(value)
  if (!text) return ''
  if (text.length <= maxLength) return text
  return `${text.slice(0, Math.max(maxLength - 3, 1))}...`
}
