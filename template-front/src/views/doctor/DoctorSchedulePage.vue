<template>
  <div class="doctor-schedule-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>未来排班总数</span>
        <strong>{{ schedules.length }}</strong>
      </article>
      <article class="stat-card">
        <span>7 日内排班</span>
        <strong>{{ weekCount }}</strong>
      </article>
      <article class="stat-card">
        <span>已满号时段</span>
        <strong>{{ fullCount }}</strong>
      </article>
      <article class="stat-card">
        <span>可线上接诊</span>
        <strong>{{ onlineCapableCount }}</strong>
      </article>
    </section>

    <section class="panel-card">
      <div class="panel-head">
        <div>
          <h3>我的排班概览</h3>
          <p>这里先提供只读排班视图，方便医生确认接诊时段、方式与号源余量。</p>
        </div>
        <el-button @click="loadSchedules">刷新</el-button>
      </div>

      <el-table :data="schedules" v-loading="loading" border>
        <el-table-column label="日期" min-width="140">
          <template #default="{ row }">
            {{ formatDate(row.scheduleDate, true) }}
          </template>
        </el-table-column>
        <el-table-column label="时段" min-width="100">
          <template #default="{ row }">
            {{ timePeriodLabel(row.timePeriod) }}
          </template>
        </el-table-column>
        <el-table-column label="接诊方式" min-width="120">
          <template #default="{ row }">
            {{ visitTypeLabel(row.visitType) }}
          </template>
        </el-table-column>
        <el-table-column label="容量" min-width="220">
          <template #default="{ row }">
            <div class="capacity-cell">
              <el-progress
                :percentage="capacityPercentage(row)"
                :stroke-width="10"
                :show-text="false"
              />
              <span>{{ row.usedCapacity || 0 }} / {{ row.maxCapacity || 0 }}，剩余 {{ remainingCapacity(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
      </el-table>

      <el-empty v-if="!loading && !schedules.length" description="当前未配置未来排班" />
    </section>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { get } from '@/net'

const loading = ref(false)
const schedules = ref([])

const weekCount = computed(() => {
  const today = new Date()
  const weekLater = new Date(today)
  weekLater.setDate(today.getDate() + 7)
  return schedules.value.filter(item => {
    const date = new Date(item.scheduleDate)
    return date >= today && date <= weekLater
  }).length
})

const fullCount = computed(() => schedules.value.filter(item => remainingCapacity(item) <= 0).length)
const onlineCapableCount = computed(() => schedules.value.filter(item => ['online', 'both'].includes(item.visitType)).length)

function loadSchedules() {
  loading.value = true
  get('/api/doctor/schedule/list', (data) => {
    schedules.value = data || []
    loading.value = false
  }, (message) => {
    loading.value = false
    ElMessage.warning(message || '医生排班加载失败，请稍后再试')
  })
}

function remainingCapacity(item) {
  const maxCapacity = Number(item?.maxCapacity || 0)
  const usedCapacity = Number(item?.usedCapacity || 0)
  return Math.max(maxCapacity - usedCapacity, 0)
}

function capacityPercentage(item) {
  const maxCapacity = Number(item?.maxCapacity || 0)
  if (!maxCapacity) return 0
  return Math.min(100, Math.round((Number(item?.usedCapacity || 0) / maxCapacity) * 100))
}

function timePeriodLabel(value) {
  if (value === 'morning') return '上午'
  if (value === 'afternoon') return '下午'
  if (value === 'evening') return '晚上'
  return '待定时段'
}

function visitTypeLabel(value) {
  if (value === 'online') return '线上问诊'
  if (value === 'offline') return '线下面诊'
  if (value === 'followup') return '复诊随访'
  if (value === 'both') return '线上 / 线下'
  return '待定方式'
}

function formatDate(value, onlyDate = false) {
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

onMounted(() => loadSchedules())
</script>

<style scoped>
.doctor-schedule-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.stat-card,
.panel-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.stat-card {
  padding: 22px 24px;
}

.stat-card span {
  color: var(--app-muted);
}

.stat-card strong {
  display: block;
  margin-top: 14px;
  font-size: 30px;
}

.panel-card {
  padding: 24px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-head h3 {
  margin: 0;
  font-size: 22px;
}

.panel-head p {
  margin: 8px 0 0;
  color: var(--app-muted);
  line-height: 1.7;
}

.capacity-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.capacity-cell span {
  color: var(--app-muted);
}

@media (max-width: 1100px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }

  .panel-head {
    flex-direction: column;
  }
}
</style>
