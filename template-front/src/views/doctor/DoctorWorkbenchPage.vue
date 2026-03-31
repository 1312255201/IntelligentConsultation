<template>
  <div class="doctor-workbench-page">
    <el-empty
      v-if="!summary.bound"
      :description="summary.bindingMessage || '当前账号尚未绑定医生档案'"
      class="panel-card"
    />

    <template v-else>
      <section class="hero-card">
        <div class="doctor-profile">
          <el-avatar :size="88" :src="resolveImagePath(summary.photo) || undefined">
            {{ summary.doctorName?.slice(0, 1) || 'D' }}
          </el-avatar>
          <div class="profile-copy">
            <span class="section-tag">Doctor Profile</span>
            <h2>{{ summary.doctorName }}</h2>
            <p>{{ profileTitle }}</p>
            <div class="chip-row">
              <span>{{ summary.departmentName || '未分配科室' }}</span>
              <span v-if="summary.doctorStatus === 1">档案启用中</span>
              <span v-else>档案已停用</span>
            </div>
            <p class="profile-text">{{ summary.introduction || summary.expertise || '暂未填写医生介绍。' }}</p>
          </div>
        </div>

        <div class="stat-grid">
          <article class="stat-card">
            <span>科室问诊总量</span>
            <strong>{{ summary.consultationCount || 0 }}</strong>
          </article>
          <article class="stat-card">
            <span>今日新增问诊</span>
            <strong>{{ summary.todayConsultationCount || 0 }}</strong>
          </article>
          <article class="stat-card">
            <span>高优先级问诊</span>
            <strong>{{ summary.riskConsultationCount || 0 }}</strong>
          </article>
          <article class="stat-card">
            <span>近期排班</span>
            <strong>{{ summary.upcomingScheduleCount || 0 }}</strong>
          </article>
        </div>
      </section>

      <section class="content-grid">
        <article class="panel-card">
          <div class="panel-head">
            <div>
              <h3>服务标签</h3>
              <p>这些标签来自管理员维护，可作为后续 AI 导诊和医生展示的基础信息。</p>
            </div>
          </div>
          <div v-if="summary.serviceTags?.length" class="chip-row">
            <span v-for="tag in summary.serviceTags" :key="tag">{{ tag }}</span>
          </div>
          <el-empty v-else description="当前医生尚未配置服务标签" />
        </article>

        <article class="panel-card">
          <div class="panel-head">
            <div>
              <h3>近期排班</h3>
              <p>先展示未来排班，方便医生确认接诊时段和容量安排。</p>
            </div>
            <el-button text @click="router.push('/doctor/schedule')">查看全部</el-button>
          </div>
          <div v-if="summary.upcomingSchedules?.length" class="schedule-list">
            <div v-for="item in summary.upcomingSchedules" :key="item.id" class="schedule-item">
              <div>
                <strong>{{ formatDate(item.scheduleDate, true) }}</strong>
                <span>{{ timePeriodLabel(item.timePeriod) }} · {{ visitTypeLabel(item.visitType) }}</span>
              </div>
              <div class="schedule-meta">
                <span>{{ remainingCapacity(item) }} / {{ item.maxCapacity }} 剩余</span>
                <el-tag :type="item.status === 1 ? 'success' : 'info'" effect="light">
                  {{ item.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </div>
            </div>
          </div>
          <el-empty v-else description="当前未配置未来排班" />
        </article>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h3>最近问诊</h3>
            <p>这里按科室展示近期问诊记录，方便医生快速进入查看详情。</p>
          </div>
          <el-button text @click="router.push('/doctor/consultation')">进入问诊列表</el-button>
        </div>

        <el-table :data="summary.recentConsultations || []" border>
          <el-table-column prop="patientName" label="就诊人" min-width="100" />
          <el-table-column prop="categoryName" label="问诊分类" min-width="120" />
          <el-table-column prop="chiefComplaint" label="主诉" min-width="220" show-overflow-tooltip />
          <el-table-column label="分诊结果" min-width="120">
            <template #default="{ row }">
              {{ row.triageLevelName || '待评估' }}
            </template>
          </el-table-column>
          <el-table-column label="提交时间" min-width="160">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="goToConsultation(row.id)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { get, resolveImagePath } from '@/net'

const router = useRouter()

const summary = reactive({
  bound: 0,
  bindingMessage: '',
  doctorId: null,
  doctorStatus: 0,
  accountId: null,
  departmentId: null,
  departmentName: '',
  doctorName: '',
  doctorTitle: '',
  photo: '',
  introduction: '',
  expertise: '',
  consultationCount: 0,
  todayConsultationCount: 0,
  riskConsultationCount: 0,
  upcomingScheduleCount: 0,
  serviceTagCount: 0,
  serviceTags: [],
  recentConsultations: [],
  upcomingSchedules: []
})

const profileTitle = computed(() => [summary.doctorTitle, summary.expertise].filter(Boolean).join(' · ') || '暂未完善医生职称与专长')

function loadSummary() {
  get('/api/doctor/workbench/summary', (data) => {
    Object.assign(summary, {
      bound: 0,
      bindingMessage: '',
      doctorId: null,
      doctorStatus: 0,
      accountId: null,
      departmentId: null,
      departmentName: '',
      doctorName: '',
      doctorTitle: '',
      photo: '',
      introduction: '',
      expertise: '',
      consultationCount: 0,
      todayConsultationCount: 0,
      riskConsultationCount: 0,
      upcomingScheduleCount: 0,
      serviceTagCount: 0,
      serviceTags: [],
      recentConsultations: [],
      upcomingSchedules: []
    }, data || {})
  }, (message) => {
    ElMessage.warning(message || '医生工作台加载失败，请稍后再试')
  })
}

function goToConsultation(id) {
  router.push({
    path: '/doctor/consultation',
    query: { id }
  })
}

function remainingCapacity(item) {
  const maxCapacity = Number(item?.maxCapacity || 0)
  const usedCapacity = Number(item?.usedCapacity || 0)
  return Math.max(maxCapacity - usedCapacity, 0)
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

onMounted(() => loadSummary())
</script>

<style scoped>
.doctor-workbench-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-card,
.panel-card,
.stat-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.hero-card {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr);
  gap: 18px;
  padding: 26px;
}

.doctor-profile {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  padding: 8px 4px;
}

.profile-copy h2 {
  margin: 12px 0 8px;
  font-size: 30px;
}

.profile-copy > p {
  margin: 0;
  color: var(--app-muted);
  line-height: 1.7;
}

.profile-text {
  margin-top: 12px !important;
}

.section-tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(19, 73, 80, 0.08);
  color: #27646d;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  padding: 20px 22px;
}

.stat-card span {
  color: var(--app-muted);
}

.stat-card strong {
  display: block;
  margin-top: 14px;
  font-size: 30px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
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

.chip-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.chip-row span {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(19, 73, 80, 0.08);
  color: #27646d;
}

.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.schedule-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(19, 73, 80, 0.05);
}

.schedule-item strong {
  display: block;
  margin-bottom: 6px;
}

.schedule-item span {
  color: var(--app-muted);
}

.schedule-meta {
  text-align: right;
}

@media (max-width: 1100px) {
  .hero-card,
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .doctor-profile,
  .schedule-item {
    flex-direction: column;
  }

  .schedule-meta {
    text-align: left;
  }
}
</style>
