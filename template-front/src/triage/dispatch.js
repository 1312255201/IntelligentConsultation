function normalizeSmartDispatch(dispatch) {
  return {
    status: dispatch?.status || '',
    statusText: dispatch?.statusText || '科室分诊队列',
    hint: dispatch?.hint || '当前先进入科室分诊队列，等待医生进一步接手。',
    recommendationSource: dispatch?.recommendationSource || '',
    candidateCount: Number(dispatch?.candidateCount || 0),
    suggestedDoctorId: dispatch?.suggestedDoctorId ?? null,
    suggestedDoctorName: dispatch?.suggestedDoctorName || '',
    suggestedDoctorTitle: dispatch?.suggestedDoctorTitle || '',
    suggestedDoctorPhoto: dispatch?.suggestedDoctorPhoto || '',
    suggestedDoctorExpertise: dispatch?.suggestedDoctorExpertise || '',
    suggestedDoctorNextScheduleText: dispatch?.suggestedDoctorNextScheduleText || '',
    claimedDoctorId: dispatch?.claimedDoctorId ?? null,
    claimedDoctorName: dispatch?.claimedDoctorName || '',
    recommendationReason: dispatch?.recommendationReason || ''
  }
}

function smartDispatchTagType(dispatch) {
  const summary = normalizeSmartDispatch(dispatch)
  return ({
    waiting_accept: 'warning',
    claimed_by_suggested: 'success',
    claimed_by_other: 'info',
    claimed_in_department: 'success',
    department_queue: 'info'
  })[summary.status] || 'info'
}

function smartDispatchStatusLabel(dispatch) {
  return normalizeSmartDispatch(dispatch).statusText
}

function smartDispatchHintText(dispatch) {
  return normalizeSmartDispatch(dispatch).hint
}

function smartDispatchDoctorLabel(dispatch) {
  const summary = normalizeSmartDispatch(dispatch)
  return summary.suggestedDoctorName || summary.claimedDoctorName || ''
}

function isRecommendedToDoctor(dispatch, doctorId) {
  const summary = normalizeSmartDispatch(dispatch)
  return Number(summary.suggestedDoctorId || 0) > 0
    && Number(summary.suggestedDoctorId) === Number(doctorId || 0)
    && summary.status === 'waiting_accept'
}

export {
  isRecommendedToDoctor,
  normalizeSmartDispatch,
  smartDispatchDoctorLabel,
  smartDispatchHintText,
  smartDispatchStatusLabel,
  smartDispatchTagType
}
