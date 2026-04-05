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

function formatConfidence(value) {
  const number = Number(value)
  return Number.isNaN(number) || number <= 0 ? '' : `${Math.round(number * 100)}%`
}

function visitTypeLabel(value) {
  return ({
    emergency: '立即急诊',
    offline: '尽快线下就医',
    followup: '复诊随访',
    online: '线上继续沟通'
  })[`${value || ''}`.toLowerCase()] || ''
}

function resolveTriageMessageInsight(message) {
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
  const shouldEscalateToHuman = payload.shouldEscalateToHuman === 1 ? 1 : 0
  const suggestOfflineImmediately = payload.suggestOfflineImmediately === 1 ? 1 : 0

  if (!summary
    && !reply
    && !riskFlags.length
    && !recommendedDoctors.length
    && !recommendedDoctorIds.length
    && !doctorRecommendationReason
    && !recommendedDepartmentName
    && !recommendedVisitType
    && !confidenceText
    && shouldEscalateToHuman !== 1
    && suggestOfflineImmediately !== 1) {
    return null
  }

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
    confidenceText
  }
}

export {
  resolveTriageMessageInsight
}
