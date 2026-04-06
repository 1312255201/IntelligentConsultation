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
            <span>患者新消息</span>
            <strong>{{ summary.unreadConsultationCount || 0 }}</strong>
          </article>
          <article class="stat-card">
            <span>待医生回复</span>
            <strong>{{ summary.waitingReplyConsultationCount || 0 }}</strong>
          </article>
          <article class="stat-card">
            <span>待随访</span>
            <strong>{{ summary.pendingFollowUpCount || 0 }}</strong>
          </article>
          <article class="stat-card">
            <span>系统推荐给我</span>
            <strong>{{ summary.recommendedConsultationCount || 0 }}</strong>
          </article>
          <article class="stat-card">
            <span>近期排班</span>
            <strong>{{ summary.upcomingScheduleCount || 0 }}</strong>
          </article>
        </div>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h3>待办看板</h3>
            <p>把患者新消息、待回复和待随访集中到一个入口，便于医生快速切入处理。</p>
          </div>
          <el-button text @click="openConsultationList()">进入问诊列表</el-button>
        </div>

        <div class="todo-grid">
          <article class="todo-card">
            <div class="todo-card-head">
              <div>
                <strong>系统推荐给我</strong>
                <p>系统根据当前分诊结果、科室和排班优先建议由你先接手。</p>
              </div>
              <el-tag type="primary" effect="light">{{ summary.recommendedConsultationCount || 0 }}</el-tag>
            </div>
            <div v-if="summary.recommendedConsultations?.length" class="todo-list">
              <button
                v-for="item in summary.recommendedConsultations"
                :key="`recommended-${item.id}`"
                type="button"
                class="todo-item"
                @click="goToConsultation(item.id)"
              >
                <div class="todo-item-head">
                  <strong>{{ item.patientName || '未命名就诊人' }}</strong>
                  <span>{{ formatDate(item.createTime) }}</span>
                </div>
                <p>{{ item.smartDispatch?.hint || item.chiefComplaint || '系统建议由你优先接手当前问诊。' }}</p>
                <div class="todo-item-meta">
                  <span>{{ item.categoryName || '未分类' }}</span>
                  <span>{{ item.triageLevelName || triageActionLabel(item.triageActionType) }}</span>
                </div>
              </button>
            </div>
            <el-empty v-else description="当前没有系统优先推荐给你的问诊" />
            <div class="todo-foot">
              <el-button text @click="openConsultationList({ dispatchFilter: 'recommended_to_me' })">查看全部推荐问诊</el-button>
            </div>
          </article>

          <article class="todo-card">
            <div class="todo-card-head">
              <div>
                <strong>患者新消息</strong>
                <p>患者刚补充了病情变化、检查结果或恢复情况。</p>
              </div>
              <el-tag type="danger" effect="light">{{ summary.unreadConsultationCount || 0 }}</el-tag>
            </div>
            <div v-if="summary.unreadConsultations?.length" class="todo-list">
              <button
                v-for="item in summary.unreadConsultations"
                :key="`unread-${item.id}`"
                type="button"
                class="todo-item"
                @click="goToConsultation(item.id)"
              >
                <div class="todo-item-head">
                  <strong>{{ item.patientName || '未命名就诊人' }}</strong>
                  <span>{{ formatDate(item.messageSummary?.latestTime || item.updateTime) }}</span>
                </div>
                <p>{{ item.messageSummary?.latestMessagePreview || item.chiefComplaint || '暂无更多沟通内容' }}</p>
                <div class="todo-item-meta">
                  <span>{{ item.categoryName || '未分类' }}</span>
                  <span>未读 {{ item.messageSummary?.unreadCount || 0 }} 条</span>
                </div>
              </button>
            </div>
            <el-empty v-else description="当前没有患者新消息" />
            <div class="todo-foot">
              <el-button text @click="openConsultationList({ messageFilter: 'unread' })">查看全部新消息</el-button>
            </div>
          </article>

          <article class="todo-card">
            <div class="todo-card-head">
              <div>
                <strong>待医生回复</strong>
                <p>最后一条消息来自患者，建议尽快继续回复或接手处理。</p>
              </div>
              <el-tag type="warning" effect="light">{{ summary.waitingReplyConsultationCount || 0 }}</el-tag>
            </div>
            <div v-if="summary.waitingReplyConsultations?.length" class="todo-list">
              <button
                v-for="item in summary.waitingReplyConsultations"
                :key="`reply-${item.id}`"
                type="button"
                class="todo-item"
                @click="goToConsultation(item.id)"
              >
                <div class="todo-item-head">
                  <strong>{{ item.patientName || '未命名就诊人' }}</strong>
                  <span>{{ formatDate(item.messageSummary?.latestTime || item.updateTime) }}</span>
                </div>
                <p>{{ item.messageSummary?.latestMessagePreview || item.chiefComplaint || '暂无更多沟通内容' }}</p>
                <div class="todo-item-meta">
                  <span>{{ item.categoryName || '未分类' }}</span>
                  <span>{{ assignmentOwnerText(item) }}</span>
                </div>
              </button>
            </div>
            <el-empty v-else description="当前没有待回复问诊" />
            <div class="todo-foot">
              <el-button text @click="openConsultationList({ messageFilter: 'waiting_reply' })">查看全部待回复</el-button>
            </div>
          </article>

          <article class="todo-card">
            <div class="todo-card-head">
              <div>
                <strong>待随访</strong>
                <p>已经完成接诊，但仍需要继续跟进恢复情况或安排再次随访。</p>
              </div>
              <el-tag type="success" effect="light">{{ summary.pendingFollowUpCount || 0 }}</el-tag>
            </div>
            <div v-if="summary.pendingFollowUpConsultations?.length" class="todo-list">
              <button
                v-for="item in summary.pendingFollowUpConsultations"
                :key="`followup-${item.id}`"
                type="button"
                class="todo-item"
                @click="goToConsultation(item.id)"
              >
                <div class="todo-item-head">
                  <strong>{{ item.patientName || '未命名就诊人' }}</strong>
                  <span>{{ followUpDueText(item) }}</span>
                </div>
                <p>{{ item.doctorHandle?.summary || item.chiefComplaint || '当前暂无更多处理摘要' }}</p>
                <div class="todo-item-meta">
                  <span>{{ item.categoryName || '未分类' }}</span>
                  <span>{{ followUpPlanText(item) }}</span>
                </div>
              </button>
            </div>
            <el-empty v-else description="当前没有待随访问诊" />
            <div class="todo-foot">
              <el-button text @click="openConsultationList({ status: 'completed' })">查看全部已完成问诊</el-button>
            </div>
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
          <el-table-column label="智能分配" min-width="180">
            <template #default="{ row }">
              <div class="recent-message-cell">
                <strong>{{ smartDispatchStatusLabel(row.smartDispatch) }}</strong>
                <span>{{ smartDispatchHintText(row.smartDispatch) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="沟通进展" min-width="220">
            <template #default="{ row }">
              <div class="recent-message-cell">
                <strong>{{ consultationMessageStatus(row) }}</strong>
                <span>{{ row.messageSummary?.latestMessagePreview || '暂无沟通消息' }}</span>
              </div>
            </template>
          </el-table-column>
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
import { smartDispatchHintText, smartDispatchStatusLabel } from '@/triage/dispatch'

const router = useRouter()

const summary = reactive(createEmptySummary())

const profileTitle = computed(() => [summary.doctorTitle, summary.expertise].filter(Boolean).join(' · ') || '暂未完善医生职称与专长')

function loadSummary() {
  get('/api/doctor/workbench/summary', (data) => {
    Object.assign(summary, createEmptySummary(), data || {})
  }, (message) => {
    ElMessage.warning(message || '医生工作台加载失败，请稍后再试')
  })
}

function createEmptySummary() {
  return {
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
    unreadConsultationCount: 0,
    waitingReplyConsultationCount: 0,
    pendingFollowUpCount: 0,
    recommendedConsultationCount: 0,
    upcomingScheduleCount: 0,
    serviceTagCount: 0,
    serviceTags: [],
    recentConsultations: [],
    recommendedConsultations: [],
    unreadConsultations: [],
    waitingReplyConsultations: [],
    pendingFollowUpConsultations: [],
    upcomingSchedules: []
  }
}

function goToConsultation(id) {
  router.push({
    path: '/doctor/consultation',
    query: { id }
  })
}

function openConsultationList(query = {}) {
  router.push({
    path: '/doctor/consultation',
    query
  })
}

function assignmentOwnerText(item) {
  const assignment = item?.doctorAssignment
  if (!assignment || assignment.status !== 'claimed') return '待认领'
  return assignment.doctorId === summary.doctorId ? '我已认领' : `由 ${assignment.doctorName || '其他医生'} 认领`
}

function consultationMessageStatus(item) {
  const summaryInfo = item?.messageSummary
  if (!summaryInfo?.totalCount) return '暂无沟通'
  if ((summaryInfo.unreadCount || 0) > 0) return `患者新消息 ${summaryInfo.unreadCount} 条`
  if (summaryInfo.latestSenderType === 'user') return '待医生回复'
  if (summaryInfo.latestSenderType === 'doctor') return '已回复患者'
  return '沟通中'
}

function followUpDueText(item) {
  const latestFollowUpDate = item?.doctorFollowUps?.[0]?.nextFollowUpDate
  if (latestFollowUpDate) return `下次随访 ${formatDate(latestFollowUpDate, true)}`
  const days = Number(item?.doctorConclusion?.followUpWithinDays || 0)
  if (days > 0) return `${days} 天内随访`
  return '待安排随访'
}

function followUpPlanText(item) {
  const latestFollowUp = item?.doctorFollowUps?.[0]
  if (latestFollowUp?.needRevisit === 1) return '仍需继续随访'
  if (latestFollowUp?.needRevisit === 0) return '当前轮次已记录'
  return item?.doctorConclusion?.needFollowUp === 1 ? '建议继续跟进' : '待确认'
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

function triageActionLabel(value) {
  if (value === 'emergency') return '立即急诊'
  if (value === 'offline') return '尽快线下'
  if (value === 'followup') return '复诊随访'
  if (value === 'online') return '线上继续'
  return '继续关注'
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
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
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

.todo-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.todo-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(19, 73, 80, 0.04);
  border: 1px solid rgba(19, 73, 80, 0.08);
}

.todo-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.todo-card-head strong {
  display: block;
  margin-bottom: 6px;
}

.todo-card-head p {
  margin: 0;
  color: var(--app-muted);
  line-height: 1.7;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-item {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(19, 73, 80, 0.08);
  border-radius: 18px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.todo-item:hover {
  transform: translateY(-1px);
  border-color: rgba(15, 102, 101, 0.26);
  box-shadow: 0 12px 24px rgba(19, 73, 80, 0.08);
}

.todo-item-head,
.todo-item-meta,
.recent-message-cell {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.todo-item-head strong,
.recent-message-cell strong {
  color: #31474d;
}

.todo-item-head span,
.todo-item-meta span,
.recent-message-cell span {
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.5;
}

.todo-item p,
.recent-message-cell span {
  margin: 8px 0 0;
}

.todo-item p {
  color: #41575d;
  line-height: 1.7;
}

.todo-item-meta {
  margin-top: 10px;
  flex-wrap: wrap;
}

.todo-foot {
  margin-top: 14px;
}

.recent-message-cell {
  flex-direction: column;
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
  .content-grid,
  .todo-grid {
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
