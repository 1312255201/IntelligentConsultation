<template>
  <div class="overview-page" v-loading="loading">
    <section class="hero-card">
      <div class="hero-copy">
        <span class="section-tag">Patient Workbench</span>
        <h2>从提醒到导诊，再到医生处理，你的问诊进展都在这里</h2>
        <p>{{ heroSummary }}</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="router.push('/index/consultation')">发起问诊</el-button>
          <el-button size="large" @click="router.push('/index/reminder')">消息与提醒</el-button>
          <el-button size="large" plain @click="router.push('/index/triage')">AI 导诊工作区</el-button>
        </div>
      </div>

      <div class="hero-side">
        <article class="hero-metric">
          <span>总提醒</span>
          <strong>{{ totalReminderCount }}</strong>
          <p>医生回复、待处理问诊与随访提醒会统一汇总到这里。</p>
        </article>
        <article class="hero-metric hero-metric-accent">
          <span>档案覆盖</span>
          <strong>{{ historyCoverageText }}</strong>
          <p>已为 {{ historyCoverageCount }} 位就诊人建立健康档案，可为 AI 导诊与医生接诊提供更完整上下文。</p>
        </article>
      </div>
    </section>

    <section class="stat-grid">
      <article class="stat-card">
        <span>就诊人</span>
        <strong>{{ patients.length }}</strong>
      </article>
      <article class="stat-card">
        <span>健康档案覆盖</span>
        <strong>{{ historyCoverageCount }}</strong>
      </article>
      <article class="stat-card">
        <span>问诊总数</span>
        <strong>{{ records.length }}</strong>
      </article>
      <article class="stat-card">
        <span>医生新回复</span>
        <strong>{{ unreadDoctorReplyCount }}</strong>
      </article>
      <article class="stat-card">
        <span>待医生处理</span>
        <strong>{{ waitingDoctorHandleCount }}</strong>
      </article>
      <article class="stat-card">
        <span>待随访</span>
        <strong>{{ pendingFollowUpCount }}</strong>
      </article>
      <article class="stat-card stat-card-warning">
        <span>今日到期</span>
        <strong>{{ dueTodayFollowUpCount }}</strong>
      </article>
      <article class="stat-card stat-card-danger">
        <span>逾期随访</span>
        <strong>{{ overdueFollowUpCount }}</strong>
      </article>
    </section>

    <section class="content-grid">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>当前待办</h3>
            <p>优先关注医生新回复、仍在等待处理的问诊，以及需要继续随访的记录。</p>
          </div>
          <el-button text @click="router.push('/index/reminder')">进入提醒中心</el-button>
        </div>

        <div v-if="priorityReminderRecords.length" class="todo-list">
          <button
            v-for="item in priorityReminderRecords"
            :key="`todo-${item.id}`"
            type="button"
            :class="['todo-item', reminderItemClass(item)]"
            @click="openConsultationRecord(item, reminderQuery(item))"
          >
            <div class="todo-item-head">
              <div>
                <strong>{{ item.patientName || '未命名就诊人' }}</strong>
                <p>{{ reminderSummary(item) }}</p>
              </div>
              <span>{{ reminderTimeText(item) }}</span>
            </div>
            <div class="chip-row">
              <span>{{ primaryReminderLabel(item) }}</span>
              <span>{{ item.categoryName || '未分类' }}</span>
              <span>{{ recordProgressLabel(item) }}</span>
              <span v-if="isPendingFollowUpRecord(item)">{{ followUpLine(item) }}</span>
            </div>
          </button>
        </div>
        <el-empty v-else description="当前没有需要优先处理的提醒">
          <el-button type="primary" @click="router.push('/index/consultation')">去发起问诊</el-button>
        </el-empty>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>快捷入口</h3>
            <p>把患者端最常用的几个动作集中到首页，减少来回寻找页面的成本。</p>
          </div>
        </div>

        <div class="action-grid">
          <button type="button" class="action-card" @click="router.push('/index/consultation')">
            <span class="action-kicker">Consultation</span>
            <strong>发起问诊</strong>
            <p>选择分类、填写资料，并提交进入 AI 导诊主链路。</p>
          </button>
          <button type="button" class="action-card" @click="router.push('/index/reminder')">
            <span class="action-kicker">Reminder</span>
            <strong>消息与提醒</strong>
            <p>查看医生新回复、待处理问诊与随访提醒。</p>
          </button>
          <button type="button" class="action-card" @click="router.push('/index/triage')">
            <span class="action-kicker">AI Triage</span>
            <strong>AI 导诊工作区</strong>
            <p>集中查看 AI 导诊会话、结果归档和推荐医生。</p>
          </button>
          <button type="button" class="action-card" @click="router.push('/index/health')">
            <span class="action-kicker">Health</span>
            <strong>健康档案</strong>
            <p>补齐既往病史、过敏史和长期用药，提升导诊准确度。</p>
          </button>
          <button type="button" class="action-card" @click="router.push('/index/patient')">
            <span class="action-kicker">Patient</span>
            <strong>就诊人管理</strong>
            <p>维护本人和家庭成员档案，作为问诊主体入口。</p>
          </button>
          <button type="button" class="action-card action-card-accent" @click="router.push('/index/profile')">
            <span class="action-kicker">Profile</span>
            <strong>账户设置</strong>
            <p>维护邮箱、头像与登录资料，保证工作台信息完整。</p>
          </button>
        </div>
      </article>
    </section>

    <section class="content-grid">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>最近问诊</h3>
            <p>最近提交或仍在推进中的问诊记录，可直接回到详情或 AI 导诊工作区继续处理。</p>
          </div>
          <el-button text @click="router.push('/index/consultation')">查看全部问诊</el-button>
        </div>

        <div v-if="recentRecords.length" class="recent-list">
          <article v-for="item in recentRecords" :key="`recent-${item.id}`" class="recent-item">
            <div class="recent-item-head">
              <div>
                <strong>{{ item.title || item.categoryName || '问诊记录' }}</strong>
                <p>{{ item.patientName || '未命名就诊人' }} · {{ item.categoryName || '未分类' }}</p>
              </div>
              <span>{{ formatDate(item.createTime) }}</span>
            </div>
            <div class="chip-row">
              <span>{{ recordProgressLabel(item) }}</span>
              <span>{{ item.triageLevelName || triageActionLabel(item.triageActionType) }}</span>
              <span v-if="recordHasUnreadDoctorReply(item)">医生新回复 {{ getMessageSummary(item).unreadCount }} 条</span>
              <span v-if="isPendingFollowUpRecord(item)">{{ followUpTagLabel(item) }}</span>
            </div>
            <div class="recent-actions">
              <el-button text type="primary" @click="openConsultationRecord(item, reminderQuery(item))">查看详情</el-button>
              <el-button text @click="openTriageWorkspace(item.id)">AI 导诊</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前还没有问诊记录" />
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>账户与档案摘要</h3>
            <p>从首页快速确认当前账户、档案覆盖和问诊闭环推进情况。</p>
          </div>
        </div>

        <el-descriptions :column="1" border class="summary-descriptions">
          <el-descriptions-item label="用户名">
            {{ profile.username || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="邮箱">
            {{ profile.email || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="角色">
            {{ profile.role || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ registerTimeText }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="coverage-board">
          <article class="coverage-item">
            <div class="coverage-head">
              <strong>健康档案覆盖率</strong>
              <span>{{ historyCoveragePercent }}%</span>
            </div>
            <el-progress :percentage="historyCoveragePercent" :stroke-width="10" :show-text="false" />
            <p>已为 {{ historyCoverageCount }} / {{ patients.length || 0 }} 位就诊人建立健康档案。</p>
          </article>

          <article class="coverage-item">
            <div class="coverage-head">
              <strong>完成问诊占比</strong>
              <span>{{ completedConsultationPercent }}%</span>
            </div>
            <el-progress :percentage="completedConsultationPercent" :stroke-width="10" :show-text="false" status="success" />
            <p>已完成 {{ completedConsultationCount }} / {{ records.length || 0 }} 条问诊，其他记录仍在持续推进。</p>
          </article>

          <article class="coverage-item">
            <div class="coverage-head">
              <strong>提醒待办占比</strong>
              <span>{{ reminderPercent }}%</span>
            </div>
            <el-progress :percentage="reminderPercent" :stroke-width="10" :show-text="false" status="warning" />
            <p>当前有 {{ totalReminderCount }} 条记录仍需要继续关注，建议优先处理提醒中心中的待办。</p>
          </article>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { get } from '@/net'
import { smartDispatchHintText } from '@/triage/dispatch'
import { normalizeReminderRecords } from '@/triage/reminder'

const router = useRouter()
const accountContext = inject('account-context', null)

const loading = ref(false)
const patients = ref([])
const histories = ref([])
const records = ref([])

const profile = computed(() => accountContext?.profile || {})
const registerTimeText = computed(() => formatDate(profile.value.registerTime))
const historyMap = computed(() => {
  const map = new Map()
  histories.value.forEach(item => {
    if (item?.patientId) map.set(item.patientId, item)
  })
  return map
})
const historyCoverageCount = computed(() => patients.value.filter(item => historyMap.value.has(item.id)).length)
const historyCoveragePercent = computed(() => percentage(historyCoverageCount.value, patients.value.length))
const unreadDoctorReplyCount = computed(() => records.value.filter(recordHasUnreadDoctorReply).length)
const waitingDoctorHandleCount = computed(() => records.value.filter(item => recordProgressStage(item) === 'waiting_doctor').length)
const pendingFollowUpCount = computed(() => records.value.filter(isPendingFollowUpRecord).length)
const dueTodayFollowUpCount = computed(() => records.value.filter(item => followUpState(item) === 'due_today').length)
const overdueFollowUpCount = computed(() => records.value.filter(item => followUpState(item) === 'overdue').length)
const totalReminderCount = computed(() => reminderRecords.value.length)
const completedConsultationCount = computed(() => records.value.filter(item => recordProgressStage(item) === 'completed').length)
const completedConsultationPercent = computed(() => percentage(completedConsultationCount.value, records.value.length))
const reminderPercent = computed(() => percentage(totalReminderCount.value, records.value.length))
const historyCoverageText = computed(() => patients.value.length ? `${historyCoverageCount.value} / ${patients.value.length}` : '0 / 0')
const reminderRecords = computed(() => records.value
  .filter(isReminderRecord)
  .slice()
  .sort(compareReminderRecords))
const priorityReminderRecords = computed(() => reminderRecords.value.slice(0, 5))
const recentRecords = computed(() => records.value
  .slice()
  .sort((left, right) => compareDateDesc(left?.createTime, right?.createTime))
  .slice(0, 4))
const heroSummary = computed(() => {
  if (!records.value.length) return '当前还没有问诊记录，可以先选择就诊人并发起一条问诊，系统会在提交后自动进入 AI 导诊主链路。'
  if (overdueFollowUpCount.value > 0) return `当前有 ${overdueFollowUpCount.value} 条问诊已进入逾期随访，建议先进入提醒中心查看详情并继续跟进恢复情况。`
  if (unreadDoctorReplyCount.value > 0) return `医生刚回复了 ${unreadDoctorReplyCount.value} 条问诊，首页已经把它们归到待办区，适合优先回看并决定是否继续补充信息。`
  if (waitingDoctorHandleCount.value > 0) return `仍有 ${waitingDoctorHandleCount.value} 条问诊正在等待医生进一步处理，你可以继续补充症状变化、检查结果或恢复情况。`
  if (pendingFollowUpCount.value > 0) return `当前共有 ${pendingFollowUpCount.value} 条问诊处于待随访状态，适合按计划继续观察并记录恢复进展。`
  return '当前问诊链路运行平稳，没有需要立即处理的待办。你可以继续补齐档案、发起新问诊，或回到 AI 导诊工作区查看历史会话。'
})

function loadData() {
  loading.value = true
  let remaining = 3
  const finish = () => {
    remaining -= 1
    if (remaining <= 0) loading.value = false
  }

  get('/api/user/patient/list', (data) => {
    patients.value = data || []
    finish()
  }, (message) => {
    ElMessage.warning(message || '就诊人数据加载失败')
    finish()
  })

  get('/api/user/medical-history/list', (data) => {
    histories.value = data || []
    finish()
  }, (message) => {
    ElMessage.warning(message || '健康档案数据加载失败')
    finish()
  })

  get('/api/user/consultation/record/list', (data) => {
    records.value = normalizeReminderRecords(data || [])
    accountContext?.refreshWorkspaceSummary?.()
    finish()
  }, (message) => {
    ElMessage.warning(message || '问诊记录加载失败')
    finish()
  })
}

function openConsultationRecord(record, query = {}) {
  if (!record?.id) return
  router.push({
    path: '/index/consultation',
    query: { ...query, id: record.id }
  })
}

function openTriageWorkspace(recordId) {
  if (!recordId) return
  router.push({
    path: '/index/triage',
    query: { recordId }
  })
}

function normalizeMessageSummary(summary) {
  return {
    totalCount: Number(summary?.totalCount || 0),
    userMessageCount: Number(summary?.userMessageCount || 0),
    doctorMessageCount: Number(summary?.doctorMessageCount || 0),
    unreadCount: Number(summary?.unreadCount || 0),
    latestSenderType: summary?.latestSenderType || '',
    latestSenderName: summary?.latestSenderName || '',
    latestMessageType: summary?.latestMessageType || '',
    latestMessagePreview: summary?.latestMessagePreview || '',
    latestTime: summary?.latestTime || null
  }
}

function getMessageSummary(record) {
  return normalizeMessageSummary(record?.messageSummary)
}

function recordHasUnreadDoctorReply(record) {
  return getMessageSummary(record).unreadCount > 0
}

function recordMessagePreview(record) {
  return getMessageSummary(record).latestMessagePreview || '暂未产生沟通消息'
}

function recordProgressStage(record) {
  if (!record) return 'waiting_doctor'
  if (recordHasUnreadDoctorReply(record)) return 'doctor_replied'
  if (record.status === 'completed' || record?.doctorHandle?.status === 'completed') return 'completed'
  if (record?.doctorHandle?.doctorName || record.status === 'processing') return 'doctor_processing'
  return 'waiting_doctor'
}

function recordProgressLabel(record) {
  return ({
    doctor_replied: '医生已回复',
    waiting_doctor: '待医生处理',
    doctor_processing: '医生处理中',
    completed: '已完成'
  })[recordProgressStage(record)] || '待医生处理'
}

function triageActionLabel(value) {
  return ({
    emergency: '立即急诊',
    offline: '尽快线下',
    followup: '复诊随访',
    online: '线上继续'
  })[value] || '继续关注'
}

function latestFollowUp(record) {
  return Array.isArray(record?.doctorFollowUps) && record.doctorFollowUps.length
    ? record.doctorFollowUps[0]
    : null
}

function followUpDueDate(record) {
  const latest = latestFollowUp(record)
  if (latest?.nextFollowUpDate) return latest.nextFollowUpDate
  if (record?.doctorConclusion?.needFollowUp !== 1) return null
  const days = Number(record?.doctorConclusion?.followUpWithinDays || 0)
  if (!Number.isFinite(days) || days <= 0) return null
  const baseTime = record?.doctorConclusion?.updateTime || record?.doctorHandle?.completeTime || record?.updateTime
  if (!baseTime) return null
  const dueDate = new Date(baseTime)
  dueDate.setHours(0, 0, 0, 0)
  dueDate.setDate(dueDate.getDate() + days)
  return dueDate
}

function followUpState(record) {
  if (record?.doctorConclusion?.needFollowUp !== 1 && !latestFollowUp(record)?.nextFollowUpDate) return 'none'
  const dueValue = followUpDueDate(record)
  if (!dueValue) return 'pending'
  const dueDate = new Date(dueValue)
  dueDate.setHours(0, 0, 0, 0)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  if (dueDate.getTime() < today.getTime()) return 'overdue'
  if (dueDate.getTime() === today.getTime()) return 'due_today'
  return 'pending'
}

function isPendingFollowUpRecord(record) {
  return ['pending', 'due_today', 'overdue'].includes(followUpState(record))
}

function followUpTagLabel(record) {
  return ({
    overdue: '已逾期随访',
    due_today: '今日到期随访',
    pending: '待随访',
    none: '无需随访'
  })[followUpState(record)] || '待随访'
}

function followUpLine(record) {
  const dueValue = followUpDueDate(record)
  const state = followUpState(record)
  if (state === 'overdue') return `已逾期 ${formatDate(dueValue, true)}`
  if (state === 'due_today') return `今日到期 ${formatDate(dueValue, true)}`
  if (state === 'pending') return dueValue ? `计划于 ${formatDate(dueValue, true)} 跟进` : '已标记后续继续跟进'
  return '当前暂无随访计划'
}

function followUpReminderText(record) {
  const state = followUpState(record)
  if (state === 'overdue') return `当前问诊已进入逾期随访状态，建议尽快完成本轮随访。计划时间：${formatDate(followUpDueDate(record), true)}`
  if (state === 'due_today') return '当前问诊今天需要继续跟进，建议及时查看恢复情况并回看医生建议。'
  if (state === 'pending') {
    return `当前问诊仍处于待随访状态${followUpDueDate(record) ? `，计划时间：${formatDate(followUpDueDate(record), true)}` : ''}`
  }
  return '当前暂无随访提醒'
}

function primaryReminderLabel(record) {
  const state = followUpState(record)
  if (state === 'overdue') return '逾期随访'
  if (state === 'due_today') return '今日到期'
  if (recordHasUnreadDoctorReply(record)) return '医生新回复'
  if (state === 'pending') return '待随访'
  return '待医生处理'
}

function reminderQuery(record) {
  const state = followUpState(record)
  if (state === 'overdue') return { followUp: 'overdue' }
  if (state === 'due_today' || state === 'pending') return { followUp: 'pending' }
  if (recordHasUnreadDoctorReply(record)) return { progress: 'doctor_replied' }
  if (recordProgressStage(record) === 'waiting_doctor') return { progress: 'waiting_doctor' }
  return {}
}

function reminderSummary(record) {
  const state = followUpState(record)
  if (state === 'overdue' || state === 'due_today' || state === 'pending') return followUpReminderText(record)
  if (recordHasUnreadDoctorReply(record)) return `医生有 ${getMessageSummary(record).unreadCount} 条新回复，最新消息：${recordMessagePreview(record)}`
  return smartDispatchHintText(record?.smartDispatch) || '问诊资料已提交，等待医生进一步处理'
}

function reminderTimeText(record) {
  const state = followUpState(record)
  if (state === 'overdue' || state === 'due_today' || state === 'pending') return followUpLine(record)
  return formatDate(getMessageSummary(record).latestTime || record.updateTime || record.createTime)
}

function reminderItemClass(record) {
  const state = followUpState(record)
  if (state === 'overdue') return 'is-overdue'
  if (state === 'due_today') return 'is-due-today'
  if (recordHasUnreadDoctorReply(record)) return 'is-reply'
  return ''
}

function isReminderRecord(record) {
  return recordHasUnreadDoctorReply(record)
    || recordProgressStage(record) === 'waiting_doctor'
    || isPendingFollowUpRecord(record)
}

function reminderPriority(record) {
  const state = followUpState(record)
  if (state === 'overdue') return 0
  if (state === 'due_today') return 1
  if (recordHasUnreadDoctorReply(record)) return 2
  if (state === 'pending') return 3
  if (recordProgressStage(record) === 'waiting_doctor') return 4
  return 9
}

function compareReminderRecords(left, right) {
  const priorityDiff = reminderPriority(left) - reminderPriority(right)
  if (priorityDiff !== 0) return priorityDiff

  const followUpDiff = compareDateAsc(followUpDueDate(left), followUpDueDate(right))
  if (followUpDiff !== 0 && isPendingFollowUpRecord(left) && isPendingFollowUpRecord(right)) return followUpDiff

  return compareDateDesc(reminderSortTime(left), reminderSortTime(right))
}

function reminderSortTime(record) {
  if (recordHasUnreadDoctorReply(record)) return getMessageSummary(record).latestTime || record.updateTime || record.createTime
  return followUpDueDate(record) || record.updateTime || record.createTime
}

function compareDateDesc(left, right) {
  return parseTime(right) - parseTime(left)
}

function compareDateAsc(left, right) {
  const leftTime = parseTime(left)
  const rightTime = parseTime(right)
  if (!leftTime && !rightTime) return 0
  if (!leftTime) return 1
  if (!rightTime) return -1
  return leftTime - rightTime
}

function parseTime(value) {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? 0 : time
}

function percentage(part, total) {
  if (!total) return 0
  return Math.min(Math.round((part / total) * 100), 100)
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

onMounted(() => loadData())
</script>

<style scoped>
.overview-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-card,
.panel-card,
.stat-card,
.hero-metric,
.todo-item,
.action-card,
.recent-item,
.coverage-item {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.hero-card {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) 320px;
  gap: 18px;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(246, 211, 154, 0.26), transparent 42%),
    radial-gradient(circle at left center, rgba(136, 224, 209, 0.22), transparent 48%),
    var(--app-panel);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-tag {
  display: inline-flex;
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(19, 73, 80, 0.08);
  color: #27646d;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h2 {
  margin: 0;
  font-size: 34px;
  line-height: 1.18;
  color: #21444b;
}

.hero-copy p {
  margin: 0;
  color: var(--app-muted);
  line-height: 1.8;
}

.hero-actions,
.chip-row,
.panel-head,
.todo-item-head,
.recent-item-head,
.recent-actions,
.coverage-head {
  display: flex;
  gap: 12px;
}

.hero-actions,
.chip-row,
.recent-actions {
  flex-wrap: wrap;
  align-items: center;
}

.panel-head,
.todo-item-head,
.recent-item-head,
.coverage-head {
  justify-content: space-between;
  align-items: flex-start;
}

.hero-side {
  display: grid;
  gap: 14px;
}

.hero-metric {
  padding: 20px 22px;
  background: rgba(255, 255, 255, 0.82);
}

.hero-metric-accent {
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.08), rgba(255, 255, 255, 0.94));
}

.hero-metric span,
.stat-card span,
.action-kicker,
.recent-item-head span,
.coverage-head span {
  color: var(--app-muted);
}

.hero-metric strong,
.stat-card strong {
  display: block;
  margin-top: 12px;
  font-size: 30px;
}

.hero-metric p {
  margin: 10px 0 0;
  line-height: 1.7;
}

.stat-grid,
.content-grid,
.action-grid {
  display: grid;
  gap: 18px;
}

.stat-grid {
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.stat-card {
  padding: 22px 24px;
}

.stat-card.stat-card-warning {
  border-color: rgba(210, 155, 47, 0.24);
  background: linear-gradient(180deg, rgba(210, 155, 47, 0.1), rgba(255, 255, 255, 0.96));
}

.stat-card.stat-card-danger {
  border-color: rgba(214, 95, 80, 0.24);
  background: linear-gradient(180deg, rgba(214, 95, 80, 0.1), rgba(255, 255, 255, 0.96));
}

.content-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.panel-card {
  padding: 24px;
}

.panel-head {
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

.todo-list,
.recent-list,
.coverage-board {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.todo-item {
  width: 100%;
  padding: 16px 18px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.todo-item:hover {
  transform: translateY(-1px);
  border-color: rgba(15, 102, 101, 0.28);
  box-shadow: 0 14px 28px rgba(19, 73, 80, 0.08);
}

.todo-item.is-overdue {
  border-color: rgba(214, 95, 80, 0.24);
  background: linear-gradient(180deg, rgba(214, 95, 80, 0.08), rgba(255, 255, 255, 0.98));
}

.todo-item.is-due-today {
  border-color: rgba(210, 155, 47, 0.24);
  background: linear-gradient(180deg, rgba(210, 155, 47, 0.08), rgba(255, 255, 255, 0.98));
}

.todo-item.is-reply {
  border-color: rgba(77, 168, 132, 0.22);
  background: linear-gradient(180deg, rgba(77, 168, 132, 0.08), rgba(255, 255, 255, 0.98));
}

.todo-item strong,
.action-card strong,
.recent-item strong,
.coverage-item strong {
  color: #31474d;
}

.todo-item p,
.recent-item p,
.coverage-item p {
  margin: 8px 0 0;
  color: #41575d;
  line-height: 1.7;
}

.chip-row span {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #48656d;
  font-size: 12px;
}

.action-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.action-card {
  padding: 18px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
  background: rgba(255, 255, 255, 0.82);
}

.action-card:hover {
  transform: translateY(-1px);
  border-color: rgba(15, 102, 101, 0.26);
  box-shadow: 0 14px 28px rgba(19, 73, 80, 0.08);
}

.action-card-accent {
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.07), rgba(255, 255, 255, 0.94));
}

.action-kicker {
  display: inline-flex;
  margin-bottom: 10px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.action-card p {
  margin: 10px 0 0;
  color: #637b84;
  line-height: 1.75;
}

.recent-item,
.coverage-item {
  padding: 16px 18px;
  background: rgba(255, 255, 255, 0.82);
}

.recent-actions {
  margin-top: 14px;
}

.summary-descriptions {
  margin-bottom: 18px;
}

.coverage-board {
  margin-top: 18px;
}

@media (max-width: 1180px) {
  .hero-card,
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .hero-actions,
  .panel-head,
  .todo-item-head,
  .recent-item-head,
  .recent-actions,
  .coverage-head,
  .chip-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .action-grid {
    grid-template-columns: 1fr;
  }
}
</style>
