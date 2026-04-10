<template>
  <div class="reminder-page" v-loading="loading">
    <section class="hero-card">
      <div class="hero-copy">
        <span class="section-tag">Reminder Center</span>
        <h2>消息与提醒中心</h2>
        <p>{{ heroSummary }}</p>
        <div class="chip-row">
          <span>{{ totalReminderCount }} 条需要优先关注</span>
          <span v-if="unreadDoctorReplyCount">医生新回复 {{ unreadDoctorReplyCount }}</span>
          <span v-if="waitingDoctorHandleCount">待医生处理 {{ waitingDoctorHandleCount }}</span>
          <span v-if="pendingFollowUpCount">待随访 {{ pendingFollowUpCount }}</span>
          <span v-if="overdueFollowUpCount" class="chip-danger">逾期 {{ overdueFollowUpCount }}</span>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="openConsultationList()">进入问诊记录</el-button>
          <el-button @click="loadRecords">刷新提醒</el-button>
        </div>
      </div>

      <div class="stat-grid">
        <article class="stat-card">
          <span>总提醒</span>
          <strong>{{ totalReminderCount }}</strong>
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
      </div>
    </section>

    <section class="panel-card">
      <div class="panel-head">
        <div>
          <h3>优先提醒</h3>
          <p>把医生回复、待处理和随访事项集中到一个页面，方便你按优先级逐条处理。</p>
        </div>
        <el-button text @click="openConsultationList()">查看全部问诊记录</el-button>
      </div>

      <div class="queue-grid">
        <article class="queue-card">
          <div class="queue-card-head">
            <div>
              <strong>医生新回复</strong>
              <p>优先查看医生刚回复的问诊，避免错过下一步处理建议。</p>
            </div>
            <el-tag type="success" effect="light">{{ unreadDoctorReplyCount }}</el-tag>
          </div>
          <div v-if="unreadReplyPreviewRecords.length" class="queue-list">
            <button
              v-for="item in unreadReplyPreviewRecords"
              :key="`unread-${item.id}`"
              type="button"
              class="queue-item"
              @click="openConsultationRecord(item, { progress: 'doctor_replied' })"
            >
              <div class="queue-item-head">
                <strong>{{ item.patientName || '未命名就诊人' }}</strong>
                <span>{{ formatDate(getMessageSummary(item).latestTime || item.updateTime) }}</span>
              </div>
              <p>{{ recordMessagePreview(item) }}</p>
              <div class="queue-item-meta">
                <span>{{ item.categoryName || '未分类' }}</span>
                <span>未读 {{ getMessageSummary(item).unreadCount }} 条</span>
              </div>
            </button>
          </div>
          <el-empty v-else description="当前没有新的医生回复" />
          <div class="queue-foot">
            <el-button text @click="openConsultationList({ progress: 'doctor_replied' })">只看医生新回复</el-button>
          </div>
        </article>

        <article class="queue-card">
          <div class="queue-card-head">
            <div>
              <strong>待医生处理</strong>
              <p>这些问诊仍在等待医生接手或继续处理，可以继续补充病情资料。</p>
            </div>
            <el-tag type="warning" effect="light">{{ waitingDoctorHandleCount }}</el-tag>
          </div>
          <div v-if="waitingDoctorPreviewRecords.length" class="queue-list">
            <button
              v-for="item in waitingDoctorPreviewRecords"
              :key="`waiting-${item.id}`"
              type="button"
              class="queue-item"
              @click="openConsultationRecord(item, { progress: 'waiting_doctor' })"
            >
              <div class="queue-item-head">
                <strong>{{ item.patientName || '未命名就诊人' }}</strong>
                <span>{{ formatDate(item.createTime) }}</span>
              </div>
              <p>{{ recordProgressHint(item) }}</p>
              <div class="queue-item-meta">
                <span>{{ item.categoryName || '未分类' }}</span>
                <span>{{ smartDispatchStatusLabel(item.smartDispatch) }}</span>
              </div>
            </button>
          </div>
          <el-empty v-else description="当前没有待医生处理的问诊" />
          <div class="queue-foot">
            <el-button text @click="openConsultationList({ progress: 'waiting_doctor' })">只看待处理问诊</el-button>
          </div>
        </article>

        <article class="queue-card queue-card-followup">
          <div class="queue-card-head">
            <div>
              <strong>待随访提醒</strong>
              <p>尤其关注今日到期和已逾期的问诊，及时回看医生建议并继续跟进恢复情况。</p>
            </div>
            <div class="queue-card-tags">
              <el-tag type="primary" effect="light">{{ pendingFollowUpCount }}</el-tag>
              <el-tag v-if="dueTodayFollowUpCount" type="warning" effect="light">今日 {{ dueTodayFollowUpCount }}</el-tag>
              <el-tag v-if="overdueFollowUpCount" type="danger" effect="light">逾期 {{ overdueFollowUpCount }}</el-tag>
            </div>
          </div>
          <div v-if="followUpPreviewRecords.length" class="queue-list">
            <button
              v-for="item in followUpPreviewRecords"
              :key="`followup-${item.id}`"
              type="button"
              :class="['queue-item', feedItemClass(item)]"
              @click="openConsultationRecord(item, followUpState(item) === 'overdue' ? { followUp: 'overdue' } : { followUp: 'pending' })"
            >
              <div class="queue-item-head">
                <strong>{{ item.patientName || '未命名就诊人' }}</strong>
                <span>{{ followUpLine(item) }}</span>
              </div>
              <p>{{ followUpReminderText(item) }}</p>
              <div class="queue-item-meta">
                <span>{{ item.categoryName || '未分类' }}</span>
                <span>{{ followUpTagLabel(item) }}</span>
              </div>
            </button>
          </div>
          <el-empty v-else description="当前没有待随访提醒" />
          <div class="queue-foot">
            <el-button text @click="openConsultationList({ followUp: 'pending' })">只看待随访</el-button>
            <el-button v-if="overdueFollowUpCount" text type="danger" @click="openConsultationList({ followUp: 'overdue' })">优先看逾期</el-button>
          </div>
        </article>
      </div>
    </section>

    <section class="panel-card">
      <div class="panel-head">
        <div>
          <h3>提醒流</h3>
          <p>按优先级串联所有需要你处理的问诊，并可一键进入问诊详情或 AI 导诊工作区。</p>
        </div>
        <el-radio-group v-model="feedFilter" size="small">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="unread">医生回复</el-radio-button>
          <el-radio-button label="waiting">待处理</el-radio-button>
          <el-radio-button label="followup">待随访</el-radio-button>
          <el-radio-button label="overdue">逾期</el-radio-button>
        </el-radio-group>
      </div>

      <div v-if="filteredFeedRecords.length" class="feed-list">
        <article
          v-for="item in filteredFeedRecords"
          :key="`feed-${item.id}`"
          :class="['feed-item', feedItemClass(item)]"
        >
          <div class="feed-item-head">
            <div>
              <strong>{{ item.patientName || '未命名就诊人' }}</strong>
              <p>{{ feedItemSummary(item) }}</p>
            </div>
            <span>{{ feedItemTimeText(item) }}</span>
          </div>

          <div class="chip-row">
            <span>{{ primaryReminderLabel(item) }}</span>
            <span>{{ item.categoryName || '未分类' }}</span>
            <span>{{ recordProgressLabel(item) }}</span>
            <span v-if="isPendingFollowUpRecord(item)">{{ followUpLine(item) }}</span>
          </div>

          <div class="feed-actions">
            <el-button type="primary" plain @click="openConsultationRecord(item, reminderQuery(item))">查看问诊</el-button>
            <el-button text @click="openTriageWorkspace(item.id)">AI 导诊</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="当前筛选下没有需要关注的提醒">
        <el-button type="primary" @click="feedFilter = 'all'">查看全部提醒</el-button>
      </el-empty>
    </section>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get } from '@/net'
import { smartDispatchHintText, smartDispatchStatusLabel } from '@/triage/dispatch'
import { normalizeReminderRecords } from '@/triage/reminder'

const router = useRouter()
const route = useRoute()
const accountContext = inject('account-context', null)

const loading = ref(false)
const records = ref([])
const feedFilter = ref(resolveFeedFilter(route.query.feed))

const reminderRecords = computed(() => records.value
  .filter(isReminderRecord)
  .slice()
  .sort(compareReminderRecords))
const unreadReplyRecords = computed(() => reminderRecords.value.filter(recordHasUnreadDoctorReply))
const waitingDoctorRecords = computed(() => reminderRecords.value.filter(item => recordProgressStage(item) === 'waiting_doctor'))
const followUpRecords = computed(() => reminderRecords.value.filter(isPendingFollowUpRecord))
const unreadReplyPreviewRecords = computed(() => unreadReplyRecords.value.slice(0, 4))
const waitingDoctorPreviewRecords = computed(() => waitingDoctorRecords.value.slice(0, 4))
const followUpPreviewRecords = computed(() => followUpRecords.value.slice(0, 4))

const unreadDoctorReplyCount = computed(() => unreadReplyRecords.value.length)
const waitingDoctorHandleCount = computed(() => waitingDoctorRecords.value.length)
const pendingFollowUpCount = computed(() => followUpRecords.value.length)
const dueTodayFollowUpCount = computed(() => followUpRecords.value.filter(item => followUpState(item) === 'due_today').length)
const overdueFollowUpCount = computed(() => followUpRecords.value.filter(item => followUpState(item) === 'overdue').length)
const totalReminderCount = computed(() => reminderRecords.value.length)

const filteredFeedRecords = computed(() => reminderRecords.value.filter((item) => {
  if (feedFilter.value === 'unread') return recordHasUnreadDoctorReply(item)
  if (feedFilter.value === 'waiting') return recordProgressStage(item) === 'waiting_doctor'
  if (feedFilter.value === 'followup') return isPendingFollowUpRecord(item)
  if (feedFilter.value === 'overdue') return followUpState(item) === 'overdue'
  return true
}))

const heroSummary = computed(() => {
  if (!records.value.length) return '当前还没有历史问诊记录，可以先发起一次问诊并进入 AI 导诊工作区。'
  if (overdueFollowUpCount.value > 0) return `当前有 ${overdueFollowUpCount.value} 条问诊已进入逾期随访，建议优先回看医生结论并尽快继续跟进。`
  if (unreadDoctorReplyCount.value > 0) return `医生刚回复了 ${unreadDoctorReplyCount.value} 条问诊，建议先查看最新回复并决定是否继续补充信息。`
  if (waitingDoctorHandleCount.value > 0) return `仍有 ${waitingDoctorHandleCount.value} 条问诊在等待医生进一步处理，你可以继续补充症状变化、检查结果或恢复情况。`
  if (pendingFollowUpCount.value > 0) return `当前共有 ${pendingFollowUpCount.value} 条问诊处于待随访状态，适合按计划继续观察并记录恢复进展。`
  return '当前没有需要特别跟进的提醒，系统会在医生回复、处理进展变化或随访到期时继续汇总到这里。'
})

function loadRecords() {
  loading.value = true
  get('/api/user/consultation/record/list', (data) => {
    records.value = normalizeReminderRecords(data || [])
    accountContext?.refreshWorkspaceSummary?.()
    loading.value = false
  }, (message) => {
    loading.value = false
    ElMessage.warning(message || '提醒记录加载失败')
  })
}

function openConsultationRecord(record, query = {}) {
  if (!record?.id) return
  router.push({
    path: '/index/consultation',
    query: { ...query, id: record.id }
  })
}

function openConsultationList(query = {}) {
  router.push({
    path: '/index/consultation',
    query
  })
}

function openTriageWorkspace(recordId) {
  if (!recordId) return
  router.push({
    path: '/index/triage',
    query: { recordId }
  })
}

function resolveFeedFilter(value) {
  return ['all', 'unread', 'waiting', 'followup', 'overdue'].includes(value) ? value : 'all'
}

function buildReminderCenterQuery(nextFeed) {
  const query = { ...route.query }
  if (nextFeed && nextFeed !== 'all') {
    query.feed = nextFeed
  } else {
    delete query.feed
  }
  return query
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

function claimedDoctorName(record) {
  return `${record?.doctorAssignment?.doctorName || record?.smartDispatch?.claimedDoctorName || ''}`.trim()
}

function currentDoctorName(record) {
  const name = `${record?.doctorHandle?.doctorName || claimedDoctorName(record) || ''}`.trim()
  if (name) return name
  const summary = getMessageSummary(record)
  return summary.latestSenderType === 'doctor' ? `${summary.latestSenderName || ''}`.trim() : ''
}

function hasDoctorClaimed(record) {
  if (`${record?.doctorAssignment?.status || ''}`.trim().toLowerCase() === 'claimed') return true
  return `${record?.smartDispatch?.status || ''}`.includes('claimed')
}

function hasDoctorTakenOver(record) {
  const summary = getMessageSummary(record)
  return !!(
    record?.doctorHandle?.doctorName
    || record?.status === 'processing'
    || hasDoctorClaimed(record)
    || summary.latestSenderType === 'doctor'
  )
}

function recordProgressStage(record) {
  if (!record) return 'waiting_doctor'
  if (recordHasUnreadDoctorReply(record)) return 'doctor_replied'
  if (record.status === 'completed' || record?.doctorHandle?.status === 'completed') return 'completed'
  if (hasDoctorTakenOver(record)) return 'doctor_processing'
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

function recordProgressHint(record) {
  const stage = recordProgressStage(record)
  if (stage === 'doctor_replied') return recordMessagePreview(record)
  if (stage === 'doctor_processing') {
    return currentDoctorName(record)
      ? `当前由 ${currentDoctorName(record)} 跟进处理`
      : '医生已接手，正在整理处理意见'
  }
  if (stage === 'completed') return followUpLine(record)
  return smartDispatchHintText(record?.smartDispatch) || '问诊资料已提交，等待医生进一步处理'
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
  if (state === 'due_today') return `当前问诊今天需要继续跟进，建议及时查看恢复情况并回看医生建议。`
  if (state === 'pending') return `当前问诊仍处于待随访状态${followUpDueDate(record) ? `，计划时间：${formatDate(followUpDueDate(record), true)}` : ''}`
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

function feedItemSummary(record) {
  const state = followUpState(record)
  if (state === 'overdue' || state === 'due_today' || state === 'pending') return followUpReminderText(record)
  if (recordHasUnreadDoctorReply(record)) return `医生有 ${getMessageSummary(record).unreadCount} 条新回复，最新消息：${recordMessagePreview(record)}`
  return recordProgressHint(record)
}

function feedItemTimeText(record) {
  const state = followUpState(record)
  if (state === 'overdue' || state === 'due_today' || state === 'pending') return followUpLine(record)
  if (recordHasUnreadDoctorReply(record)) return formatDate(getMessageSummary(record).latestTime || record.updateTime)
  return formatDate(record.createTime)
}

function feedItemClass(record) {
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

watch(() => route.query.feed, (value) => {
  const nextFilter = resolveFeedFilter(value)
  if (feedFilter.value !== nextFilter) {
    feedFilter.value = nextFilter
  }
})

watch(feedFilter, (value) => {
  const currentFilter = resolveFeedFilter(route.query.feed)
  if (currentFilter === value) return
  router.replace({
    path: route.path,
    query: buildReminderCenterQuery(value)
  })
})

onMounted(() => loadRecords())
</script>

<style scoped>
.reminder-page {
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
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  gap: 18px;
  padding: 26px;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-copy h2 {
  margin: 0;
  font-size: 32px;
}

.hero-copy p {
  margin: 0;
  color: var(--app-muted);
  line-height: 1.8;
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

.hero-actions,
.panel-head,
.queue-card-head,
.queue-card-tags,
.queue-item-head,
.queue-item-meta,
.queue-foot,
.feed-item-head,
.feed-actions,
.chip-row {
  display: flex;
  gap: 12px;
}

.hero-actions,
.queue-card-tags,
.queue-item-meta,
.queue-foot,
.feed-actions,
.chip-row {
  flex-wrap: wrap;
  align-items: center;
}

.panel-head,
.queue-card-head,
.queue-item-head,
.queue-item-meta,
.feed-item-head {
  justify-content: space-between;
  align-items: flex-start;
}

.chip-row span {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #48656d;
  font-size: 12px;
}

.chip-row .chip-danger {
  background: rgba(214, 95, 80, 0.12);
  color: #9f4336;
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

.queue-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.queue-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(19, 73, 80, 0.04);
  border: 1px solid rgba(19, 73, 80, 0.08);
}

.queue-card.queue-card-followup {
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.06), rgba(255, 255, 255, 0.92));
}

.queue-card-head {
  margin-bottom: 14px;
}

.queue-card-head strong {
  display: block;
  margin-bottom: 6px;
}

.queue-card-head p {
  margin: 0;
  color: var(--app-muted);
  line-height: 1.7;
}

.queue-list,
.feed-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.queue-item,
.feed-item {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(19, 73, 80, 0.08);
  border-radius: 18px;
  background: #fff;
}

.queue-item {
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.queue-item:hover {
  transform: translateY(-1px);
  border-color: rgba(15, 102, 101, 0.26);
  box-shadow: 0 12px 24px rgba(19, 73, 80, 0.08);
}

.queue-item.is-overdue,
.feed-item.is-overdue {
  border-color: rgba(214, 95, 80, 0.24);
  background: linear-gradient(180deg, rgba(214, 95, 80, 0.08), rgba(255, 255, 255, 0.98));
}

.queue-item.is-due-today,
.feed-item.is-due-today {
  border-color: rgba(210, 155, 47, 0.24);
  background: linear-gradient(180deg, rgba(210, 155, 47, 0.08), rgba(255, 255, 255, 0.98));
}

.feed-item.is-reply {
  border-color: rgba(77, 168, 132, 0.24);
  background: linear-gradient(180deg, rgba(77, 168, 132, 0.08), rgba(255, 255, 255, 0.98));
}

.queue-item-head strong,
.feed-item-head strong {
  color: #31474d;
}

.queue-item-head span,
.queue-item-meta span,
.feed-item-head span {
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.queue-item p,
.feed-item p {
  margin: 8px 0 0;
  color: #41575d;
  line-height: 1.7;
}

.queue-item-meta {
  margin-top: 10px;
}

.queue-foot {
  margin-top: 14px;
}

.feed-actions {
  margin-top: 14px;
}

@media (max-width: 1180px) {
  .hero-card,
  .queue-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .panel-head,
  .queue-card-head,
  .queue-item-head,
  .queue-item-meta,
  .queue-foot,
  .feed-item-head,
  .feed-actions,
  .chip-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
