function comparisonStatusLabel(value) {
  return ({
    match: '一致',
    mismatch: '存在差异',
    partial: '部分可比',
    pending: '待对比'
  })[`${value || ''}`] || '待对比'
}

function comparisonStatusClass(value) {
  return ({
    match: 'is-match',
    mismatch: 'is-mismatch',
    partial: 'is-partial',
    pending: 'is-pending'
  })[`${value || ''}`] || 'is-pending'
}

const aiMismatchReasonOptions = [
  { value: 'supplementary_information', label: '补充信息后判断调整' },
  { value: 'clinical_judgment', label: '结合临床经验调整' },
  { value: 'inspection_needed', label: '需结合线下检查结果' },
  { value: 'risk_control', label: '出于安全考虑调整' },
  { value: 'patient_preference', label: '结合患者意愿与依从性' },
  { value: 'other', label: '其他原因' }
]

function aiMismatchReasonLabel(value) {
  return aiMismatchReasonOptions.find(item => item.value === `${value || ''}`)?.label || `${value || ''}`
}

export {
  aiMismatchReasonLabel,
  aiMismatchReasonOptions,
  comparisonStatusClass,
  comparisonStatusLabel
}
