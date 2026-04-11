import { isRecommendedToDoctor, normalizeSmartDispatch } from '@/triage/dispatch'

export function normalizeDoctorMessageSummary(summary) {
  return {
    totalCount: Number(summary?.totalCount || 0),
    userMessageCount: Number(summary?.userMessageCount || 0),
    doctorMessageCount: Number(summary?.doctorMessageCount || 0),
    unreadCount: Number(summary?.unreadCount || 0),
    latestSenderType: summary?.latestSenderType || '',
    latestSenderName: summary?.latestSenderName || '',
    latestMessageType: summary?.latestMessageType || '',
    latestMessagePreview: summary?.latestMessagePreview || '',
    latestTime: summary?.latestTime || null
  }
}

export function normalizeDoctorServiceFeedback(feedback) {
  if (!feedback) return null
  return {
    ...feedback,
    serviceScore: feedback?.serviceScore == null ? null : Number(feedback.serviceScore),
    isResolved: feedback?.isResolved == null ? null : Number(feedback.isResolved),
    doctorHandleStatus: feedback?.doctorHandleStatus == null ? null : Number(feedback.doctorHandleStatus),
    doctorHandleAccountId: feedback?.doctorHandleAccountId == null ? null : Number(feedback.doctorHandleAccountId),
    doctorHandleDoctorId: feedback?.doctorHandleDoctorId == null ? null : Number(feedback.doctorHandleDoctorId)
  }
}

export function normalizeDoctorReminderRecord(record = {}) {
  return {
    ...record,
    smartDispatch: normalizeSmartDispatch(record?.smartDispatch),
    messageSummary: normalizeDoctorMessageSummary(record?.messageSummary),
    serviceFeedback: normalizeDoctorServiceFeedback(record?.serviceFeedback)
  }
}

export function normalizeDoctorReminderRecords(records = []) {
  return (records || []).map(item => normalizeDoctorReminderRecord(item))
}

export function getDoctorMessageSummary(record) {
  return normalizeDoctorMessageSummary(record?.messageSummary)
}

export function hasUnreadMessages(record) {
  return getDoctorMessageSummary(record).unreadCount > 0
}

export function waitingDoctorReply(record) {
  const summary = getDoctorMessageSummary(record)
  return summary.totalCount > 0 && summary.latestSenderType === 'user'
}

export function messageProgressLabel(record) {
  const summary = getDoctorMessageSummary(record)
  if (summary.totalCount <= 0) return '未沟通'
  if (summary.unreadCount > 0) return '患者新消息'
  if (summary.latestSenderType === 'user') return '待医生回复'
  if (summary.latestSenderType === 'doctor') return '已回复患者'
  return '沟通中'
}

export function messageProgressType(record) {
  const summary = getDoctorMessageSummary(record)
  if (summary.totalCount <= 0) return 'info'
  if (summary.unreadCount > 0) return 'danger'
  if (summary.latestSenderType === 'user') return 'warning'
  if (summary.latestSenderType === 'doctor') return 'success'
  return 'info'
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
