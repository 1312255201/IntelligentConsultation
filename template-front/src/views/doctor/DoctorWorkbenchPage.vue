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
          <article class="stat-card stat-card-danger">
            <span>高优先级问诊</span>
            <strong>{{ summary.riskConsultationCount || 0 }}</strong>
          </article>
          <article class="stat-card stat-card-accent">
            <span>待认领问诊</span>
            <strong>{{ summary.unclaimedConsultationCount || 0 }}</strong>
          </article>
          <article class="stat-card">
            <span>我认领的</span>
            <strong>{{ summary.myClaimedConsultationCount || 0 }}</strong>
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
          <article class="stat-card stat-card-warning">
            <span>今日到期随访</span>
            <strong>{{ summary.dueTodayFollowUpCount || 0 }}</strong>
          </article>
          <article class="stat-card stat-card-danger">
            <span>已逾期随访</span>
            <strong>{{ summary.overdueFollowUpCount || 0 }}</strong>
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
            <p>把“先认领，再处理，再随访”的关键入口集中在首页，方便连续处理医生侧全流程。</p>
          </div>
          <el-button text @click="openConsultationList()">进入问诊列表</el-button>
        </div>

        <div class="todo-grid">
          <article class="todo-card todo-card-claim">
            <div class="todo-card-head">
              <div>
                <strong>待认领问诊</strong>
                <p>先处理尚未认领的问诊，尤其优先接手高优先级和系统判定风险更高的记录。</p>
              </div>
              <div class="todo-card-tags">
                <el-tag type="primary" effect="light">{{ summary.unclaimedConsultationCount || 0 }}</el-tag>
                <el-tag v-if="summary.highPriorityUnclaimedCount" type="danger" effect="light">
                  高优先级 {{ summary.highPriorityUnclaimedCount }}
                </el-tag>
              </div>
            </div>
            <div v-if="summary.unclaimedConsultations?.length" class="todo-list">
              <button
                v-for="item in summary.unclaimedConsultations"
                :key="`claim-${item.id}`"
                type="button"
                :class="['todo-item', claimTodoItemClass(item)]"
                @click="goToConsultation(item.id, claimRecordQuery(item))"
              >
                <div class="todo-item-head">
                  <strong>{{ item.patientName || '未命名就诊人' }}</strong>
                  <span>{{ formatDate(item.messageSummary?.latestTime || item.createTime) }}</span>
                </div>
                <p>{{ item.smartDispatch?.hint || item.chiefComplaint || '当前问诊尚未认领，可先接手后继续处理。' }}</p>
                <div class="todo-item-meta">
                  <span>{{ item.categoryName || '未分类' }}</span>
                  <span>{{ item.triageLevelName || triageActionLabel(item.triageActionType) }}</span>
                  <span>{{ claimPriorityText(item) }}</span>
                </div>
              </button>
            </div>
            <el-empty v-else description="当前没有待认领的问诊" />
            <div class="todo-foot">
              <el-button text @click="openConsultationList({ ownerFilter: 'unclaimed' })">查看全部待认领</el-button>
              <el-button
                v-if="summary.highPriorityUnclaimedCount"
                text
                type="danger"
                @click="openConsultationList({ ownerFilter: 'unclaimed', riskFilter: 'high_priority' })"
              >
                优先处理高优先级
              </el-button>
            </div>
          </article>

          <article class="todo-card">
            <div class="todo-card-head">
              <div>
                <strong>系统推荐给我</strong>
                <p>系统结合当前分诊结果、科室和排班情况，优先建议由你先接手处理。</p>
              </div>
              <el-tag type="success" effect="light">{{ summary.recommendedConsultationCount || 0 }}</el-tag>
            </div>
            <div v-if="summary.recommendedConsultations?.length" class="todo-list">
              <button
                v-for="item in summary.recommendedConsultations"
                :key="`recommended-${item.id}`"
                type="button"
                class="todo-item"
                @click="goToConsultation(item.id, { dispatchFilter: 'recommended_to_me' })"
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
            <el-empty v-else description="当前没有系统推荐给你的问诊" />
            <div class="todo-foot">
              <el-button text @click="openConsultationList({ dispatchFilter: 'recommended_to_me' })">查看全部推荐问诊</el-button>
            </div>
          </article>

          <article class="todo-card">
            <div class="todo-card-head">
              <div>
                <strong>患者新消息</strong>
                <p>患者刚补充了病情变化、检查结果或恢复情况，适合尽快回看。</p>
              </div>
              <el-tag type="danger" effect="light">{{ summary.unreadConsultationCount || 0 }}</el-tag>
            </div>
            <div v-if="summary.unreadConsultations?.length" class="todo-list">
              <button
                v-for="item in summary.unreadConsultations"
                :key="`unread-${item.id}`"
                type="button"
                class="todo-item"
                @click="goToConsultation(item.id, { messageFilter: 'unread' })"
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
                <p>最后一条消息来自患者，建议尽快继续回复或接手处理，避免链路中断。</p>
              </div>
              <el-tag type="warning" effect="light">{{ summary.waitingReplyConsultationCount || 0 }}</el-tag>
            </div>
            <div v-if="summary.waitingReplyConsultations?.length" class="todo-list">
              <button
                v-for="item in summary.waitingReplyConsultations"
                :key="`reply-${item.id}`"
                type="button"
                class="todo-item"
                @click="goToConsultation(item.id, { messageFilter: 'waiting_reply' })"
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
                <p>已经完成接诊，但仍需要继续跟进恢复情况或安排下一轮随访。</p>
              </div>
              <div class="todo-card-tags">
                <el-tag type="success" effect="light">{{ summary.pendingFollowUpCount || 0 }}</el-tag>
                <el-tag v-if="summary.dueTodayFollowUpCount" type="warning" effect="light">今日 {{ summary.dueTodayFollowUpCount }}</el-tag>
                <el-tag v-if="summary.overdueFollowUpCount" type="danger" effect="light">逾期 {{ summary.overdueFollowUpCount }}</el-tag>
              </div>
            </div>
            <div v-if="summary.pendingFollowUpConsultations?.length" class="todo-list">
              <button
                v-for="item in summary.pendingFollowUpConsultations"
                :key="`followup-${item.id}`"
                type="button"
                :class="['todo-item', followUpTodoItemClass(item)]"
                @click="goToConsultation(item.id, { followUpFilter: 'pending', sortMode: 'follow_up_due' })"
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
              <el-button text @click="openConsultationList({ followUpFilter: 'pending', sortMode: 'follow_up_due' })">查看全部待随访</el-button>
              <el-button
                v-if="summary.overdueFollowUpCount"
                text
                type="danger"
                @click="openConsultationList({ followUpFilter: 'overdue', sortMode: 'follow_up_due' })"
              >
                优先处理逾期
              </el-button>
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
              <p>优先展示未来排班，方便医生确认接诊时段和容量安排。</p>
            </div>
            <el-button text @click="router.push('/doctor/schedule')">查看全部</el-button>
          </div>
          <div v-if="summary.upcomingSchedules?.length" class="schedule-list">
            <div v-for="item in summary.upcomingSchedules" :key="item.id" class="schedule-item">
              <div>
                <strong>{{ formatDate(item.scheduleDate, true) }}</strong>
                <span>{{ timePeriodLabel(item.timePeriod) }} / {{ visitTypeLabel(item.visitType) }}</span>
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
            <p>这里按科室展示最近问诊记录，方便医生快速回到详情继续处理。</p>
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
import { computed, inject, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { get, resolveImagePath } from '@/net'
import { smartDispatchHintText, smartDispatchStatusLabel } from '@/triage/dispatch'

const router = useRouter()
const accountContext = inject('account-context', null)

const summary = reactive(createEmptySummary())

const profileTitle = computed(() => [summary.doctorTitle, summary.expertise].filter(Boolean).join(' / ') || '暂未完善医生职称与专长')

function loadSummary() {
  get('/api/doctor/workbench/summary', (data) => {
    Object.assign(summary, createEmptySummary(), data || {})
    accountContext?.patchDoctorWorkspaceSummary?.(data || {})
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
    unclaimedConsultationCount: 0,
    myClaimedConsultationCount: 0,
    highPriorityUnclaimedCount: 0,
    unreadConsultationCount: 0,
    waitingReplyConsultationCount: 0,
    pendingFollowUpCount: 0,
    dueTodayFollowUpCount: 0,
    overdueFollowUpCount: 0,
    recommendedConsultationCount: 0,
    upcomingScheduleCount: 0,
    serviceTagCount: 0,
    serviceTags: [],
    recentConsultations: [],
    unclaimedConsultations: [],
    recommendedConsultations: [],
    unreadConsultations: [],
    waitingReplyConsultations: [],
    pendingFollowUpConsultations: [],
    upcomingSchedules: []
  }
}

function goToConsultation(id, query = {}) {
  router.push({
    path: '/doctor/consultation',
    query: { ...query, id }
  })
}

function openConsultationList(query = {}) {
  router.push({
    path: '/doctor/consultation',
    query
  })
}

function isRiskConsultation(item) {
  return ['emergency', 'offline'].includes(item?.triageActionType)
}

function claimRecordQuery(item) {
  return isRiskConsultation(item)
    ? { ownerFilter: 'unclaimed', riskFilter: 'high_priority' }
    : { ownerFilter: 'unclaimed' }
}

function claimPriorityText(item) {
  if (item?.triageActionType === 'emergency') return '立即关注'
  if (item?.triageActionType === 'offline') return '尽快接手'
  return '可认领'
}

function claimTodoItemClass(item) {
  return isRiskConsultation(item) ? 'is-urgent' : ''
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

function latestFollowUp(item) {
  return Array.isArray(item?.doctorFollowUps) && item.doctorFollowUps.length
    ? item.doctorFollowUps[0]
    : null
}

function followUpDueDate(item) {
  const latest = latestFollowUp(item)
  if (latest?.nextFollowUpDate) return latest.nextFollowUpDate
  const days = Number(item?.doctorConclusion?.followUpWithinDays || 0)
  if (!Number.isFinite(days) || days <= 0) return null
  const baseTime = item?.doctorConclusion?.updateTime || item?.doctorHandle?.completeTime || item?.updateTime
  if (!baseTime) return null
  const dueDate = new Date(baseTime)
  dueDate.setHours(0, 0, 0, 0)
  dueDate.setDate(dueDate.getDate() + days)
  return dueDate
}

function followUpState(item) {
  const dueValue = followUpDueDate(item)
  if (!dueValue) return 'pending'
  const dueDate = new Date(dueValue)
  dueDate.setHours(0, 0, 0, 0)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  if (dueDate.getTime() < today.getTime()) return 'overdue'
  if (dueDate.getTime() === today.getTime()) return 'due_today'
  return 'pending'
}

function followUpDueText(item) {
  const dueDate = followUpDueDate(item)
  const state = followUpState(item)
  if (state === 'overdue') return `已逾期 ${formatDate(dueDate, true)}`
  if (state === 'due_today') return `今日到期 ${formatDate(dueDate, true)}`
  if (dueDate) return `下次随访 ${formatDate(dueDate, true)}`
  const days = Number(item?.doctorConclusion?.followUpWithinDays || 0)
  if (days > 0) return `${days} 天内随访`
  return '待安排随访'
}

function followUpPlanText(item) {
  const state = followUpState(item)
  if (state === 'overdue') return '建议优先回访'
  if (state === 'due_today') return '建议今日完成'
  const latest = latestFollowUp(item)
  if (latest?.needRevisit === 1) return '仍需继续随访'
  if (latest?.needRevisit === 0) return '当前轮次已记录'
  return item?.doctorConclusion?.needFollowUp === 1 ? '建议继续跟进' : '待确认'
}

function followUpTodoItemClass(item) {
  const state = followUpState(item)
  if (state === 'overdue') return 'is-overdue'
  if (state === 'due_today') return 'is-due-today'
  return ''
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
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
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

.stat-card.stat-card-accent {
  border-color: rgba(15, 102, 101, 0.24);
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.08), rgba(255, 255, 255, 0.96));
}

.stat-card.stat-card-warning {
  border-color: rgba(210, 155, 47, 0.24);
  background: linear-gradient(180deg, rgba(210, 155, 47, 0.1), rgba(255, 255, 255, 0.96));
}

.stat-card.stat-card-danger {
  border-color: rgba(214, 95, 80, 0.24);
  background: linear-gradient(180deg, rgba(214, 95, 80, 0.1), rgba(255, 255, 255, 0.96));
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
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.todo-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(19, 73, 80, 0.04);
  border: 1px solid rgba(19, 73, 80, 0.08);
}

.todo-card.todo-card-claim {
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.06), rgba(255, 255, 255, 0.92));
}

.todo-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.todo-card-tags {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
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

.todo-item.is-urgent,
.todo-item.is-overdue {
  border-color: rgba(214, 95, 80, 0.24);
  background: linear-gradient(180deg, rgba(214, 95, 80, 0.08), rgba(255, 255, 255, 0.98));
}

.todo-item.is-due-today {
  border-color: rgba(210, 155, 47, 0.24);
  background: linear-gradient(180deg, rgba(210, 155, 47, 0.08), rgba(255, 255, 255, 0.98));
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

.todo-item p {
  margin: 8px 0 0;
  color: #41575d;
  line-height: 1.7;
}

.todo-item-meta {
  margin-top: 10px;
  flex-wrap: wrap;
}

.todo-foot {
  margin-top: 14px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
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
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .doctor-profile,
  .schedule-item,
  .panel-head,
  .todo-card-head,
  .todo-item-head,
  .todo-item-meta,
  .todo-foot {
    flex-direction: column;
    align-items: flex-start;
  }

  .schedule-meta {
    text-align: left;
  }
}
</style>
