<template>
  <div class="prescription-page">
    <section class="hero-card">
      <div>
        <h3>个人处方查询</h3>
        <p>集中查看当前账号下的全部电子处方、用法用量和禁忌提醒，并支持快速跳转回对应问诊记录继续反馈。</p>
      </div>
      <el-button type="primary" @click="router.push('/index/consultation')">查看病例详情</el-button>
    </section>

    <section class="stat-grid">
      <article class="stat-card">
        <span>处方条目总数</span>
        <strong>{{ prescriptions.length }}</strong>
      </article>
      <article class="stat-card">
        <span>涉及问诊</span>
        <strong>{{ consultationCount }}</strong>
      </article>
      <article class="stat-card">
        <span>有禁忌提醒</span>
        <strong>{{ warningCount }}</strong>
      </article>
      <article class="stat-card">
        <span>完成问诊处方</span>
        <strong>{{ completedCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索药品、患者、医生或问诊号"
            style="max-width: 360px"
          />
          <el-select v-model="statusFilter" clearable placeholder="问诊状态" style="width: 180px">
            <el-option label="已完成" value="completed" />
            <el-option label="处理中" value="processing" />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadPrescriptions">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredPrescriptions" v-loading="loading" border>
        <el-table-column prop="consultationNo" label="问诊号" min-width="180" />
        <el-table-column prop="patientName" label="患者" min-width="120" />
        <el-table-column prop="doctorName" label="医生" min-width="120" />
        <el-table-column prop="medicineName" label="药品名称" min-width="160" />
        <el-table-column label="用法用量" min-width="220">
          <template #default="{ row }">
            {{ [row.dosage, row.frequency, row.durationDays ? `${row.durationDays} 天` : ''].filter(Boolean).join(' / ') || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="医嘱说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.medicationInstruction || '—' }}</template>
        </el-table-column>
        <el-table-column label="禁忌提醒" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.warningSummary || '—' }}</template>
        </el-table-column>
        <el-table-column label="问诊状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.consultationStatus === 'completed' ? 'success' : 'warning'" effect="light">
              {{ row.consultationStatus === 'completed' ? '已完成' : '处理中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开具时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openConsultation(row)">关联问诊</el-button>
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
const statusFilter = ref('')
const prescriptions = ref([])

const filteredPrescriptions = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return prescriptions.value.filter(item => {
    if (statusFilter.value === 'completed' && item.consultationStatus !== 'completed') return false
    if (statusFilter.value === 'processing' && item.consultationStatus === 'completed') return false
    if (!search) return true
    return [
      item.consultationNo,
      item.patientName,
      item.doctorName,
      item.medicineName,
      item.genericName
    ].filter(Boolean).some(text => `${text}`.toLowerCase().includes(search))
  })
})

const consultationCount = computed(() => new Set(prescriptions.value.map(item => item.consultationId)).size)
const warningCount = computed(() => prescriptions.value.filter(item => item.warningSummary).length)
const completedCount = computed(() => prescriptions.value.filter(item => item.consultationStatus === 'completed').length)

function loadPrescriptions() {
  loading.value = true
  get('/api/user/prescription/list', (data) => {
    prescriptions.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function openConsultation(row) {
  router.push({
    path: '/index/consultation',
    query: {
      id: row.consultationId,
      action: 'medication_feedback'
    }
  })
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadPrescriptions)
</script>

<style scoped>
.prescription-page {
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

.toolbar-filters,
.toolbar-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
