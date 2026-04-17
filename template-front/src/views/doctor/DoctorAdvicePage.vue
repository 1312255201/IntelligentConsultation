<template>
  <div class="doctor-advice-page">
    <section class="hero-card">
      <div>
        <h3>患者医嘱管理</h3>
        <p>集中查看已经形成的处理建议、患者指导和随访安排，方便医生从“医嘱执行”视角持续跟进患者。</p>
      </div>
      <el-button type="primary" @click="loadRecords">刷新医嘱列表</el-button>
    </section>

    <section class="stat-grid">
      <article class="stat-card">
        <span>有医嘱记录的问诊</span>
        <strong>{{ adviceRecords.length }}</strong>
      </article>
      <article class="stat-card">
        <span>待继续随访</span>
        <strong>{{ followUpNeededCount }}</strong>
      </article>
      <article class="stat-card">
        <span>待查看服务评价</span>
        <strong>{{ feedbackPendingCount }}</strong>
      </article>
      <article class="stat-card">
        <span>已完成处理</span>
        <strong>{{ completedCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索患者、问诊号或医嘱内容"
            style="max-width: 320px"
          />
          <el-select v-model="followUpFilter" clearable placeholder="随访状态" style="width: 180px">
            <el-option label="需继续随访" value="need" />
            <el-option label="已完成本轮随访" value="done" />
          </el-select>
        </div>
      </div>

      <el-table :data="filteredAdviceRecords" v-loading="loading" border>
        <el-table-column prop="consultationNo" label="问诊号" min-width="180" />
        <el-table-column prop="patientName" label="患者" min-width="120" />
        <el-table-column label="处理建议" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">{{ row.doctorHandle?.medicalAdvice || row.doctorHandle?.summary || '—' }}</template>
        </el-table-column>
        <el-table-column label="患者指导" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.doctorConclusion?.patientInstruction || '—' }}</template>
        </el-table-column>
        <el-table-column label="随访安排" min-width="180">
          <template #default="{ row }">{{ followUpText(row) }}</template>
        </el-table-column>
        <el-table-column label="服务评价" min-width="140">
          <template #default="{ row }">{{ serviceFeedbackText(row) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">{{ formatDate(lastAdviceTime(row)) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openConsultation(row)">进入问诊</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { get } from '@/net'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const followUpFilter = ref('')
const records = ref([])

const adviceRecords = computed(() => records.value.filter(hasAdviceContent))

const filteredAdviceRecords = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return adviceRecords.value.filter(item => {
    if (followUpFilter.value === 'need' && item.doctorConclusion?.needFollowUp !== 1) return false
    if (followUpFilter.value === 'done' && item.doctorConclusion?.needFollowUp === 1) return false
    if (!search) return true
    return [
      item.consultationNo,
      item.patientName,
      item.doctorHandle?.medicalAdvice,
      item.doctorConclusion?.patientInstruction
    ].filter(Boolean).some(text => `${text}`.toLowerCase().includes(search))
  })
})

const followUpNeededCount = computed(() => adviceRecords.value.filter(item => item.doctorConclusion?.needFollowUp === 1).length)
const feedbackPendingCount = computed(() => adviceRecords.value.filter(item => item.serviceFeedback && item.serviceFeedback.doctorHandleStatus !== 1).length)
const completedCount = computed(() => adviceRecords.value.filter(item => item.doctorHandle?.status === 'completed').length)

function loadRecords() {
  loading.value = true
  get('/api/doctor/consultation/list', (data) => {
    records.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function hasAdviceContent(record) {
  return Boolean(
    record?.doctorHandle?.medicalAdvice
    || record?.doctorHandle?.followUpPlan
    || record?.doctorConclusion?.patientInstruction
    || record?.doctorConclusion?.needFollowUp === 1
  )
}

function followUpText(row) {
  if (row?.doctorConclusion?.needFollowUp === 1) {
    const days = Number(row?.doctorConclusion?.followUpWithinDays || 0)
    return days > 0 ? `${days} 天内继续随访` : '需要继续随访'
  }
  const latest = Array.isArray(row?.doctorFollowUps) ? row.doctorFollowUps[0] : null
  if (latest?.needRevisit === 1 && latest?.nextFollowUpDate) {
    return `下次随访：${formatDate(latest.nextFollowUpDate)}`
  }
  return row?.doctorHandle?.followUpPlan || '本轮无继续随访要求'
}

function serviceFeedbackText(row) {
  if (!row?.serviceFeedback) return '暂无评价'
  const score = row.serviceFeedback.serviceScore ? `评分 ${row.serviceFeedback.serviceScore}/5` : '已评价'
  const handle = row.serviceFeedback.doctorHandleStatus === 1 ? '已处理' : '待处理'
  return `${score} · ${handle}`
}

function lastAdviceTime(row) {
  return row?.doctorConclusion?.updateTime
    || row?.doctorHandle?.updateTime
    || row?.updateTime
  }

function openConsultation(row) {
  router.push({
    path: '/doctor/consultation',
    query: {
      id: row.id,
      action: row?.doctorConclusion ? 'doctor_conclusion' : 'doctor_handle'
    }
  })
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadRecords)
</script>

<style scoped>
.doctor-advice-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.stat-card,
.table-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 40px rgba(17, 70, 77, 0.08);
}

.hero-card,
.table-card {
  padding: 24px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  flex-wrap: wrap;
}

.hero-card h3 {
  margin: 0;
  font-size: 24px;
  color: #17373d;
}

.hero-card p {
  margin: 8px 0 0;
  color: #687c84;
  line-height: 1.7;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 20px 22px;
}

.stat-card span {
  display: block;
  color: #6c7f86;
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 30px;
  color: #17373d;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.toolbar-filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
