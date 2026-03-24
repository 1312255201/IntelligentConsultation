<template>
  <div class="health-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>就诊人总数</span>
        <strong>{{ patients.length }}</strong>
      </article>
      <article class="stat-card">
        <span>已建档</span>
        <strong>{{ historyCount }}</strong>
      </article>
      <article class="stat-card">
        <span>待完善</span>
        <strong>{{ pendingCount }}</strong>
      </article>
      <article class="stat-card">
        <span>特殊关注</span>
        <strong>{{ focusCount }}</strong>
      </article>
    </section>

    <el-empty v-if="!patients.length && !loading" description="请先新增至少一个就诊人，再完善健康档案">
      <el-button type="primary" @click="router.push('/index/patient')">前往就诊人管理</el-button>
    </el-empty>

    <section v-else v-loading="loading" class="layout-grid">
      <aside class="patient-panel">
        <div class="panel-head">
          <div>
            <h3>就诊人选择</h3>
            <p>按就诊人分别维护病史信息，后续 AI 导诊会结合这里的数据做更稳妥的建议。</p>
          </div>
          <el-button text @click="loadPage">刷新</el-button>
        </div>

        <div class="patient-list">
          <button
            v-for="item in patients"
            :key="item.id"
            type="button"
            :class="['patient-item', { active: item.id === selectedPatientId }]"
            @click="selectPatient(item.id)"
          >
            <div>
              <strong>{{ item.name }}</strong>
              <span>{{ patientMeta(item) }}</span>
            </div>
            <div class="item-tags">
              <el-tag v-if="item.isDefault === 1" type="success" effect="light">默认</el-tag>
              <el-tag :type="historyMap[item.id] ? 'success' : 'info'" effect="light">
                {{ historyMap[item.id] ? '已建档' : '待完善' }}
              </el-tag>
            </div>
          </button>
        </div>
      </aside>

      <section class="form-panel">
        <div class="panel-head">
          <div>
            <h3>{{ selectedPatient?.name || '健康档案' }}</h3>
            <p>{{ selectedSummary }}</p>
          </div>
          <div class="toolbar-actions">
            <el-button @click="resetForm">重置表单</el-button>
            <el-button type="danger" plain :disabled="!selectedHistory" @click="removeHistory">
              删除档案
            </el-button>
          </div>
        </div>

        <el-form :model="form" label-position="top" class="history-form">
          <div class="form-grid">
            <el-form-item label="过敏史">
              <el-input
                v-model="form.allergyHistory"
                type="textarea"
                :rows="4"
                maxlength="3000"
                show-word-limit
                placeholder="例如：青霉素过敏、海鲜过敏、粉尘过敏等"
              />
            </el-form-item>
            <el-form-item label="既往病史">
              <el-input
                v-model="form.pastHistory"
                type="textarea"
                :rows="4"
                maxlength="3000"
                show-word-limit
                placeholder="例如：既往肺炎、胃溃疡、高血压住院史等"
              />
            </el-form-item>
            <el-form-item label="慢性病史">
              <el-input
                v-model="form.chronicHistory"
                type="textarea"
                :rows="4"
                maxlength="3000"
                show-word-limit
                placeholder="例如：糖尿病、哮喘、冠心病、甲状腺疾病等"
              />
            </el-form-item>
            <el-form-item label="手术史">
              <el-input
                v-model="form.surgeryHistory"
                type="textarea"
                :rows="4"
                maxlength="3000"
                show-word-limit
                placeholder="例如：剖宫产、阑尾切除、骨折手术等"
              />
            </el-form-item>
            <el-form-item label="家族史">
              <el-input
                v-model="form.familyHistory"
                type="textarea"
                :rows="4"
                maxlength="3000"
                show-word-limit
                placeholder="例如：家族高血压、糖尿病、肿瘤史等"
              />
            </el-form-item>
            <el-form-item label="长期用药">
              <el-input
                v-model="form.medicationHistory"
                type="textarea"
                :rows="4"
                maxlength="3000"
                show-word-limit
                placeholder="例如：阿司匹林、胰岛素、吸入性药物等"
              />
            </el-form-item>
            <el-form-item label="孕期状态">
              <el-select v-model="form.pregnancyStatus" style="width: 100%">
                <el-option v-for="item in pregnancyOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="哺乳状态">
              <el-select v-model="form.lactationStatus" style="width: 100%">
                <el-option v-for="item in lactationOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </div>

          <el-form-item label="传染病史">
            <el-input
              v-model="form.infectiousHistory"
              type="textarea"
              :rows="4"
              maxlength="3000"
              show-word-limit
              placeholder="例如：乙肝、结核病史或相关接触史"
            />
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span class="helper-text">
            当前健康档案主要服务于后续 AI 导诊、问诊前置资料与医生接诊摘要生成。
          </span>
          <el-button type="primary" :loading="submitting" @click="saveHistory">保存健康档案</el-button>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, post } from '@/net'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const patients = ref([])
const histories = ref([])
const selectedPatientId = ref(null)

const pregnancyOptions = [
  { label: '未知', value: 'unknown' },
  { label: '无', value: 'none' },
  { label: '已怀孕', value: 'pregnant' },
  { label: '可能怀孕', value: 'possible' }
]

const lactationOptions = [
  { label: '未知', value: 'unknown' },
  { label: '无', value: 'none' },
  { label: '哺乳中', value: 'lactating' }
]

const form = reactive(createEmptyForm())

const historyMap = computed(() => Object.fromEntries(histories.value.map(item => [item.patientId, item])))
const selectedPatient = computed(() => patients.value.find(item => item.id === selectedPatientId.value) || null)
const selectedHistory = computed(() => selectedPatientId.value ? historyMap.value[selectedPatientId.value] || null : null)
const historyCount = computed(() => histories.value.length)
const pendingCount = computed(() => Math.max(patients.value.length - histories.value.length, 0))
const focusCount = computed(() => histories.value.filter(item =>
  item.pregnancyStatus === 'pregnant'
  || item.pregnancyStatus === 'possible'
  || item.lactationStatus === 'lactating'
  || !!item.infectiousHistory
).length)
const selectedSummary = computed(() => {
  const patient = selectedPatient.value
  if (!patient) return '请选择一个就诊人后完善健康档案。'
  return `${patientMeta(patient)}${patient.phone ? ` · ${patient.phone}` : ''}`
})

function createEmptyForm() {
  return {
    patientId: null,
    allergyHistory: '',
    pastHistory: '',
    chronicHistory: '',
    surgeryHistory: '',
    familyHistory: '',
    medicationHistory: '',
    pregnancyStatus: 'unknown',
    lactationStatus: 'unknown',
    infectiousHistory: ''
  }
}

function loadPage(preferredPatientId) {
  loading.value = true
  get('/api/user/patient/list', (patientData) => {
    patients.value = patientData || []
    if (!patients.value.length) {
      histories.value = []
      selectedPatientId.value = null
      Object.assign(form, createEmptyForm())
      loading.value = false
      return
    }

    get('/api/user/medical-history/list', (historyData) => {
      histories.value = historyData || []
      const fromQuery = Number(route.query.patientId)
      const fallbackId = patients.value[0]?.id || null
      const targetId = resolveSelectablePatientId(preferredPatientId || fromQuery || selectedPatientId.value || fallbackId)
      selectPatient(targetId)
      loading.value = false
    }, () => {
      histories.value = []
      const fromQuery = Number(route.query.patientId)
      const fallbackId = patients.value[0]?.id || null
      const targetId = resolveSelectablePatientId(preferredPatientId || fromQuery || selectedPatientId.value || fallbackId)
      selectPatient(targetId)
      loading.value = false
    })
  }, () => {
    loading.value = false
  })
}

function resolveSelectablePatientId(patientId) {
  return patients.value.some(item => item.id === patientId) ? patientId : (patients.value[0]?.id || null)
}

function selectPatient(patientId) {
  selectedPatientId.value = patientId
  applyHistoryToForm(patientId)
}

function applyHistoryToForm(patientId) {
  const history = histories.value.find(item => item.patientId === patientId)
  Object.assign(form, createEmptyForm(), history || {}, {
    patientId
  })
}

function resetForm() {
  if (!selectedPatientId.value) return
  applyHistoryToForm(selectedPatientId.value)
}

function saveHistory() {
  if (!selectedPatientId.value) return
  submitting.value = true
  post('/api/user/medical-history/save', {
    patientId: selectedPatientId.value,
    allergyHistory: form.allergyHistory,
    pastHistory: form.pastHistory,
    chronicHistory: form.chronicHistory,
    surgeryHistory: form.surgeryHistory,
    familyHistory: form.familyHistory,
    medicationHistory: form.medicationHistory,
    pregnancyStatus: form.pregnancyStatus,
    lactationStatus: form.lactationStatus,
    infectiousHistory: form.infectiousHistory
  }, () => {
    submitting.value = false
    ElMessage.success('健康档案保存成功')
    loadPage(selectedPatientId.value)
  }, (message) => {
    submitting.value = false
    ElMessage.warning(message || '健康档案保存失败，请稍后重试')
  })
}

function removeHistory() {
  if (!selectedPatientId.value || !selectedHistory.value) return
  ElMessageBox.confirm(`确认删除“${selectedPatient.value?.name || '当前就诊人'}”的健康档案吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/user/medical-history/delete?patientId=${selectedPatientId.value}`, () => {
      ElMessage.success('健康档案删除成功')
      loadPage(selectedPatientId.value)
    }, (message) => {
      ElMessage.warning(message || '健康档案删除失败')
    })
  }).catch(() => {})
}

function patientMeta(item) {
  const parts = [relationLabel(item.relationType), genderLabel(item.gender)]
  if (item.age !== null && item.age !== undefined) {
    parts.push(`${item.age} 岁`)
  }
  return parts.join(' · ')
}

function genderLabel(value) {
  if (value === 'male') return '男'
  if (value === 'female') return '女'
  return '暂不说明'
}

function relationLabel(value) {
  const map = {
    self: '本人',
    child: '子女',
    parent: '父母',
    spouse: '配偶',
    other: '其他'
  }
  return map[value] || '其他'
}

onMounted(() => loadPage())
</script>

<style scoped>
.health-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stat-card,
.patient-panel,
.form-panel {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.stat-card {
  padding: 22px 24px;
}

.stat-card span,
.panel-head p,
.patient-item span,
.helper-text {
  color: var(--app-muted);
}

.stat-card strong {
  display: block;
  margin-top: 14px;
  font-size: 28px;
}

.layout-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
}

.patient-panel,
.form-panel {
  padding: 22px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-head h3 {
  margin: 0;
  font-size: 24px;
}

.panel-head p {
  margin: 10px 0 0;
  line-height: 1.8;
}

.patient-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.patient-item {
  width: 100%;
  border: 1px solid rgba(19, 73, 80, 0.08);
  border-radius: 22px;
  background: rgba(19, 73, 80, 0.03);
  padding: 16px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease, transform 0.2s ease;
}

.patient-item:hover,
.patient-item.active {
  border-color: rgba(19, 73, 80, 0.2);
  background: rgba(19, 73, 80, 0.06);
  transform: translateY(-1px);
}

.patient-item strong {
  display: block;
  font-size: 18px;
}

.patient-item span {
  display: block;
  margin-top: 8px;
}

.item-tags,
.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.history-form {
  margin-top: 10px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
}

.helper-text {
  line-height: 1.8;
}

@media (max-width: 1180px) {
  .stat-grid,
  .layout-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .panel-head,
  .form-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
