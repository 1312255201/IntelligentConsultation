function safeText(value) {
  return typeof value === 'string' ? value.trim() : ''
}

function parseObject(value) {
  if (!value) return null
  if (typeof value === 'object' && !Array.isArray(value)) return value
  if (typeof value !== 'string') return null
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : null
  } catch {
    return null
  }
}

function normalizeStringList(value) {
  if (Array.isArray(value)) {
    return value
      .map(item => safeText(`${item ?? ''}`))
      .filter(Boolean)
  }
  if (typeof value === 'string') {
    const parsed = parseObject(value)
    if (parsed) return []
    try {
      const array = JSON.parse(value)
      return Array.isArray(array)
        ? array.map(item => safeText(`${item ?? ''}`)).filter(Boolean)
        : []
    } catch {
      const text = safeText(value)
      return text ? [text] : []
    }
  }
  return []
}

function normalizeDepartmentList(value) {
  if (Array.isArray(value)) {
    return value
      .map(item => {
        if (!item || typeof item !== 'object' || Array.isArray(item)) return null
        const name = safeText(item.name)
        if (!name) return null
        return {
          name,
          code: safeText(item.code),
          description: safeText(item.description),
          fallback: item.fallback === 1 || item.fallback === true ? 1 : 0,
          current: item.current === 1 || item.current === true ? 1 : 0
        }
      })
      .filter(Boolean)
  }
  if (typeof value === 'string') {
    try {
      const array = JSON.parse(value)
      return normalizeDepartmentList(array)
    } catch {
      return []
    }
  }
  return []
}

function formatConfidence(value) {
  const number = Number(value)
  return Number.isNaN(number) || number <= 0 ? '' : `${Math.round(number * 100)}%`
}

function normalizeFlag(value) {
  return value === 1 || value === true ? 1 : 0
}

function normalizeNullableId(value) {
  if (value === null || value === undefined || value === '') return null
  const number = Number(value)
  return Number.isNaN(number) ? safeText(`${value}`) || null : number
}

function visitTypeLabel(value) {
  return ({
    emergency: '立即急诊',
    offline: '尽快线下就医',
    followup: '复诊随访',
    online: '线上继续沟通'
  })[`${value || ''}`.toLowerCase()] || ''
}

function extractTriageMessageInsight(message) {
  const payload = parseObject(message?.structuredContent)
  if (!payload) return null

  const summary = safeText(payload.summary)
  const reply = safeText(payload.reply)
  const riskFlags = normalizeStringList(payload.riskFlags).slice(0, 5)
  const recommendedDoctorNames = normalizeStringList(payload.recommendedDoctorNames).slice(0, 5)
  const recommendedDoctorIds = Array.isArray(payload.recommendedDoctorIds)
    ? payload.recommendedDoctorIds.filter(item => item !== null && item !== undefined).slice(0, 5)
    : []
  const recommendedDoctors = recommendedDoctorNames.length
    ? recommendedDoctorNames
    : recommendedDoctorIds.map(item => `医生ID ${item}`)
  const doctorRecommendationReason = safeText(payload.doctorRecommendationReason)
  const recommendedDepartmentName = safeText(payload.recommendedDepartmentName)
  const recommendedVisitTypeCode = safeText(payload.recommendedVisitType).toLowerCase()
  const recommendedVisitType = visitTypeLabel(payload.recommendedVisitType)
  const confidenceText = formatConfidence(payload.confidenceScore)
  const shouldEscalateToHuman = normalizeFlag(payload.shouldEscalateToHuman)
  const suggestOfflineImmediately = normalizeFlag(payload.suggestOfflineImmediately)
  const nextQuestions = normalizeStringList(payload.nextQuestions).slice(0, 5)
  const availableDepartments = normalizeDepartmentList(payload.availableDepartments).slice(0, 20)
  const departmentSelectionMode = safeText(payload.departmentSelectionMode).toLowerCase()
  const entryDepartmentId = normalizeNullableId(payload.entryDepartmentId)
  const entryDepartmentName = safeText(payload.entryDepartmentName)
  const finalDepartmentId = normalizeNullableId(payload.finalDepartmentId)
  const finalDepartmentName = safeText(payload.finalDepartmentName)
  const departmentRerouted = normalizeFlag(payload.departmentRerouted)
  const promptVersion = safeText(payload.promptVersion)
  const source = safeText(payload.source).toLowerCase()

  return {
    summary,
    reply,
    riskFlags,
    recommendedDoctors,
    recommendedDoctorNames,
    recommendedDoctorIds,
    doctorRecommendationReason,
    recommendedDepartmentName,
    recommendedVisitTypeCode,
    recommendedVisitType,
    shouldEscalateToHuman,
    suggestOfflineImmediately,
    confidenceText,
    nextQuestions,
    availableDepartments,
    departmentSelectionMode,
    entryDepartmentId,
    entryDepartmentName,
    finalDepartmentId,
    finalDepartmentName,
    departmentRerouted,
    promptVersion,
    source
  }
}

function hasPrimaryInsight(insight) {
  return !!(insight?.summary
    || insight?.reply
    || insight?.riskFlags?.length
    || insight?.recommendedDoctors?.length
    || insight?.recommendedDoctorIds?.length
    || insight?.doctorRecommendationReason
    || insight?.recommendedDepartmentName
    || insight?.recommendedVisitType
    || insight?.confidenceText
    || insight?.shouldEscalateToHuman === 1
    || insight?.suggestOfflineImmediately === 1
    || insight?.availableDepartments?.length)
}

function hasAuditInsight(insight) {
  return hasPrimaryInsight(insight)
    || !!insight?.nextQuestions?.length
    || !!insight?.departmentSelectionMode
    || !!insight?.promptVersion
    || !!insight?.source
}

function resolveTriageMessageInsight(message) {
  const insight = extractTriageMessageInsight(message)
  return hasPrimaryInsight(insight) ? insight : null
}

function resolveTriageMessageAuditInsight(message) {
  const insight = extractTriageMessageInsight(message)
  return hasAuditInsight(insight) ? insight : null
}

function resolveDepartmentRoutingSnapshot(insight, fallbackDepartmentName = '') {
  const source = insight && typeof insight === 'object' ? insight : null
  if (!source) return null

  const entryDepartmentName = safeText(source.entryDepartmentName)
  const recommendedDepartmentName = safeText(source.recommendedDepartmentName)
  const finalDepartmentName = safeText(source.finalDepartmentName || fallbackDepartmentName)
  const departmentSelectionMode = safeText(source.departmentSelectionMode).toLowerCase()
  const departmentRerouted = normalizeFlag(source.departmentRerouted)

  if (!entryDepartmentName
    && !recommendedDepartmentName
    && !finalDepartmentName
    && !departmentSelectionMode
    && departmentRerouted !== 1) {
    return null
  }

  const changedByName = !!entryDepartmentName && !!finalDepartmentName && entryDepartmentName !== finalDepartmentName
  const changed = departmentRerouted === 1 || changedByName
  const modeLabel = departmentSelectionMode === 'general_entry'
    ? '综合入口智能分诊'
    : departmentSelectionMode === 'locked_department'
      ? '固定科室内判断'
      : ''

  let statusLabel = ''
  if (changed) {
    statusLabel = finalDepartmentName ? `已改派至${finalDepartmentName}` : '已完成智能改派'
  } else if (recommendedDepartmentName && finalDepartmentName && recommendedDepartmentName === finalDepartmentName) {
    statusLabel = `建议保留${finalDepartmentName}`
  } else if (finalDepartmentName) {
    statusLabel = `当前落到${finalDepartmentName}`
  }

  return {
    mode: departmentSelectionMode,
    modeLabel,
    entryDepartmentName,
    recommendedDepartmentName,
    finalDepartmentName,
    changed,
    statusLabel
  }
}

export {
  resolveDepartmentRoutingSnapshot,
  resolveTriageMessageAuditInsight,
  resolveTriageMessageInsight
}
