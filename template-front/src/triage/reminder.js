import { normalizeSmartDispatch } from '@/triage/dispatch'

export function normalizeMessageSummary(summary) {
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

export function normalizeReminderRecord(record = {}) {
  return {
    ...record,
    smartDispatch: normalizeSmartDispatch(record?.smartDispatch),
    messageSummary: normalizeMessageSummary(record?.messageSummary)
  }
}

export function normalizeReminderRecords(records = []) {
  return (records || []).map(item => normalizeReminderRecord(item))
}

export function getMessageSummary(record) {
  return normalizeMessageSummary(record?.messageSummary)
}

export function recordHasUnreadDoctorReply(record) {
  return getMessageSummary(record).unreadCount > 0
}

export function recordMessagePreview(record) {
  return getMessageSummary(record).latestMessagePreview || '暂无新的医患沟通消息'
}

export function recordProgressStage(record) {
  if (!record) return 'waiting_doctor'
  if (recordHasUnreadDoctorReply(record)) return 'doctor_replied'
  if (record.status === 'completed' || record?.doctorHandle?.status === 'completed') return 'completed'
  if (record?.doctorHandle?.doctorName || record.status === 'processing') return 'doctor_processing'
  return 'waiting_doctor'
}

export function recordProgressLabel(record) {
  return ({
    doctor_replied: '医生已回复',
    waiting_doctor: '待医生处理',
    doctor_processing: '医生处理中',
    completed: '已完成'
  })[recordProgressStage(record)] || '待医生处理'
}

export function latestFollowUp(record) {
  return Array.isArray(record?.doctorFollowUps) && record.doctorFollowUps.length
    ? record.doctorFollowUps[0]
    : null
}

export function followUpDueDate(record) {
  const latest = latestFollowUp(record)
  if (latest?.nextFollowUpDate) return latest.nextFollowUpDate
  if (record?.doctorConclusion?.needFollowUp !== 1) return null
  const days = Number(record?.doctorConclusion?.followUpWithinDays || 0)
  if (!Number.isFinite(days) || days <= 0) return null
  const baseTime = record?.doctorConclusion?.updateTime || record?.doctorHandle?.completeTime || record?.updateTime
  if (!baseTime) return null
  const dueDate = new Date(baseTime)
  dueDate.setHours(0, 0, 0, 0)
  dueDate.setDate(dueDate.getDate() + days)
  return dueDate
}

export function followUpState(record) {
  if (record?.doctorConclusion?.needFollowUp !== 1 && !latestFollowUp(record)?.nextFollowUpDate) return 'none'
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

export function isPendingFollowUpRecord(record) {
  return ['pending', 'due_today', 'overdue'].includes(followUpState(record))
}

export function followUpTagLabel(record) {
  return ({
    overdue: '已逾期随访',
    due_today: '今日到期随访',
    pending: '待随访',
    none: '无需随访'
  })[followUpState(record)] || '待随访'
}

export function formatReminderDate(value, onlyDate = false) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', onlyDate
    ? {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      }
    : {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      }).format(new Date(value))
}

export function followUpLine(record) {
  const dueValue = followUpDueDate(record)
  const state = followUpState(record)
  if (state === 'overdue') return `已逾期 ${formatReminderDate(dueValue, true)}`
  if (state === 'due_today') return `今日到期 ${formatReminderDate(dueValue, true)}`
  if (state === 'pending') return dueValue ? `计划于 ${formatReminderDate(dueValue, true)} 跟进` : '已标记后续继续跟进'
  return '当前暂无随访计划'
}

export function followUpReminderText(record) {
  const state = followUpState(record)
  if (state === 'overdue') {
    return `当前问诊已进入逾期随访状态，建议尽快完成本轮随访。计划时间：${formatReminderDate(followUpDueDate(record), true)}`
  }
  if (state === 'due_today') {
    return '当前问诊今天需要继续跟进，建议及时查看恢复情况并回看医生建议。'
  }
  if (state === 'pending') {
    const dueText = followUpDueDate(record) ? `，计划时间：${formatReminderDate(followUpDueDate(record), true)}` : ''
    return `当前问诊仍处于待随访状态${dueText}`
  }
  return '当前暂无随访提醒'
}

export function primaryReminderLabel(record) {
  const state = followUpState(record)
  if (state === 'overdue') return '逾期随访'
  if (state === 'due_today') return '今日到期'
  if (recordHasUnreadDoctorReply(record)) return '医生新回复'
  if (state === 'pending') return '待随访'
  return '待医生处理'
}

export function reminderQuery(record) {
  const state = followUpState(record)
  if (state === 'overdue') return { followUp: 'overdue' }
  if (state === 'due_today' || state === 'pending') return { followUp: 'pending' }
  if (recordHasUnreadDoctorReply(record)) return { progress: 'doctor_replied' }
  if (recordProgressStage(record) === 'waiting_doctor') return { progress: 'waiting_doctor' }
  return {}
}

export function isReminderRecord(record) {
  return recordHasUnreadDoctorReply(record)
    || recordProgressStage(record) === 'waiting_doctor'
    || isPendingFollowUpRecord(record)
}

export function reminderPriority(record) {
  const state = followUpState(record)
  if (state === 'overdue') return 0
  if (state === 'due_today') return 1
  if (recordHasUnreadDoctorReply(record)) return 2
  if (state === 'pending') return 3
  if (recordProgressStage(record) === 'waiting_doctor') return 4
  return 9
}

export function reminderSortTime(record) {
  if (recordHasUnreadDoctorReply(record)) return getMessageSummary(record).latestTime || record.updateTime || record.createTime
  return followUpDueDate(record) || record.updateTime || record.createTime
}

export function parseTime(value) {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? 0 : time
}

export function compareDateDesc(left, right) {
  return parseTime(right) - parseTime(left)
}

export function compareDateAsc(left, right) {
  const leftTime = parseTime(left)
  const rightTime = parseTime(right)
  if (!leftTime && !rightTime) return 0
  if (!leftTime) return 1
  if (!rightTime) return -1
  return leftTime - rightTime
}

export function compareReminderRecords(left, right) {
  const priorityDiff = reminderPriority(left) - reminderPriority(right)
  if (priorityDiff !== 0) return priorityDiff

  const followUpDiff = compareDateAsc(followUpDueDate(left), followUpDueDate(right))
  if (followUpDiff !== 0 && isPendingFollowUpRecord(left) && isPendingFollowUpRecord(right)) return followUpDiff

  return compareDateDesc(reminderSortTime(left), reminderSortTime(right))
}

export function percentage(part, total) {
  if (!total) return 0
  return Math.min(Math.round((part / total) * 100), 100)
}
