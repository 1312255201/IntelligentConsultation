<template>
  <div class="doctor-reminder-page" v-loading="loading || doctorSummaryLoading">
    <el-empty
      v-if="!doctorSummaryLoading && doctorSummary.bound !== 1"
      :description="doctorSummary.bindingMessage || '当前账号尚未绑定医生档案'"
      class="panel-card"
    />
    <template v-else>
      <section class="hero-card">
        <div class="hero-copy">
          <span class="section-tag">Task Center</span>
          <h2>消息与待办中心</h2>
          <p>{{ heroSummary }}</p>
          <div class="chip-row">
            <span>{{ actionableRecordCount }} 条问诊需要优先关注</span>
            <span v-if="unclaimedCount">待认领 {{ unclaimedCount }}</span>
            <span v-if="recommendedCount">系统推荐 {{ recommendedCount }}</span>
            <span v-if="unreadCount">患者新消息 {{ unreadCount }}</span>
            <span v-if="waitingReplyCount">待回复 {{ waitingReplyCount }}</span>
            <span v-if="attentionServiceFeedbackCount">待关注评价 {{ attentionServiceFeedbackCount }}</span>
            <span v-if="pendingFollowUpCount">待随访 {{ pendingFollowUpCount }}</span>
            <span v-if="overdueFollowUpCount" class="chip-danger">逾期 {{ overdueFollowUpCount }}</span>
          </div>
          <div class="hero-actions">
            <el-button type="primary" @click="openConsultationList(feedListQuery(feedFilter))">进入问诊列表</el-button>
            <el-button @click="openWorkbench">返回工作台</el-button>
            <el-button @click="loadRecords">刷新待办</el-button>
          </div>
        </div>
        <div class="stat-grid">
          <article class="stat-card"><span>总待办</span><strong>{{ actionableRecordCount }}</strong></article>
          <article class="stat-card stat-card-accent"><span>待认领</span><strong>{{ unclaimedCount }}</strong></article>
          <article class="stat-card"><span>系统推荐</span><strong>{{ recommendedCount }}</strong></article>
          <article class="stat-card stat-card-danger"><span>患者新消息</span><strong>{{ unreadCount }}</strong></article>
          <article class="stat-card stat-card-warning"><span>待医生回复</span><strong>{{ waitingReplyCount }}</strong></article>
          <article class="stat-card stat-card-warning"><span>待关注评价</span><strong>{{ attentionServiceFeedbackCount }}</strong></article>
          <article class="stat-card"><span>待随访</span><strong>{{ pendingFollowUpCount }}</strong></article>
          <article class="stat-card stat-card-warning"><span>今日到期</span><strong>{{ dueTodayFollowUpCount }}</strong></article>
          <article class="stat-card stat-card-danger"><span>逾期随访</span><strong>{{ overdueFollowUpCount }}</strong></article>
        </div>
      </section>

      <section class="focus-grid">
        <button type="button" class="focus-card" @click="openConsultationList({ ownerFilter: 'unclaimed' })">
          <strong>待认领问诊</strong>
          <span>{{ unclaimedCount }}</span>
          <small>优先接手尚未认领的问诊，尤其是高优先级记录。</small>
        </button>
        <button type="button" class="focus-card" @click="openConsultationList({ dispatchFilter: 'recommended_to_me' })">
          <strong>系统推荐给我</strong>
          <span>{{ recommendedCount }}</span>
          <small>快速查看系统建议由你优先接手的问诊。</small>
        </button>
        <button type="button" class="focus-card" @click="openConsultationList({ messageFilter: 'unread' })">
          <strong>患者新消息</strong>
          <span>{{ unreadCount }}</span>
          <small>集中处理患者刚补充的病情变化和检查结果。</small>
        </button>
        <button type="button" class="focus-card" @click="openConsultationList({ status: 'completed', feedbackFilter: 'attention' })">
          <strong>待关注服务评价</strong>
          <span>{{ attentionServiceFeedbackCount }}</span>
          <small>优先回看低分或未解决的服务评价，补足问诊服务反馈闭环。</small>
        </button>
        <button type="button" class="focus-card" @click="openConsultationList({ followUpFilter: 'pending', sortMode: 'follow_up_due' })">
          <strong>待随访</strong>
          <span>{{ pendingFollowUpCount }}</span>
          <small>按计划继续回访恢复情况，优先处理今日到期和逾期。</small>
        </button>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h3>待办流</h3>
            <p>按业务类型聚合当前所有需要关注的问诊，可直接带上下文进入问诊详情。</p>
          </div>
          <el-radio-group v-model="feedFilter" size="small" class="feed-tabs">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="unclaimed">待认领</el-radio-button>
            <el-radio-button label="recommended">系统推荐</el-radio-button>
            <el-radio-button label="unread">患者新消息</el-radio-button>
            <el-radio-button label="reply">待回复</el-radio-button>
            <el-radio-button label="followup">待随访</el-radio-button>
            <el-radio-button label="overdue">逾期</el-radio-button>
            <el-radio-button label="feedback">待关注评价</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="filteredFeedRecords.length" class="feed-list">
          <article v-for="item in filteredFeedRecords" :key="`feed-${item.id}`" :class="['feed-item', feedItemClass(item)]">
            <div class="feed-item-head">
              <div>
                <strong>{{ item.patientName || '未命名就诊人' }}</strong>
                <p>{{ feedItemSummary(item) }}</p>
              </div>
              <span>{{ feedItemTimeText(item) }}</span>
            </div>
            <div class="chip-row">
              <span>{{ primaryTaskLabel(item) }}</span>
              <span>{{ item.categoryName || '未分类' }}</span>
              <span>{{ messageProgressLabel(item) }}</span>
              <span>{{ ownerLabel(item) }}</span>
              <span v-if="hasPatientActionInsight(item)" :class="patientActionChipClass(item)">{{ patientActionLabel(item) }}</span>
              <span v-if="isAttentionServiceFeedbackRecord(item, doctorId)">{{ serviceFeedbackLabel(item, doctorId) }}</span>
              <span v-if="isPendingFollowUpRecord(item)">{{ followUpTagLabel(item) }}</span>
            </div>
            <p v-if="hasPatientActionInsight(item)" class="patient-action-line">患者后续：{{ patientActionSummary(item) }}</p>
            <div class="feed-actions">
              <el-button
                type="primary"
                :plain="primaryActionMode(item) !== 'claim'"
                :loading="isAssignmentLoading(item, 'claim')"
                :disabled="isAssignmentLoading(item, 'release')"
                @click="handlePrimaryAction(item)"
              >
                {{ primaryActionLabel(item) }}
              </el-button>
              <el-button text :disabled="isAssignmentBusy(item)" @click="openConsultationList(reminderQuery(item))">查看列表上下文</el-button>
              <el-button
                v-if="canQuickRelease(item)"
                text
                type="warning"
                :loading="isAssignmentLoading(item, 'release')"
                :disabled="isAssignmentLoading(item, 'claim')"
                @click="submitAssignment('release', item)"
              >
                释放
              </el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前筛选下没有需要处理的待办">
          <el-button type="primary" @click="feedFilter = 'all'">查看全部待办</el-button>
        </el-empty>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, post } from '@/net'
import {
  compareDateAsc, compareDateDesc, compareRecentDoctorRecord, followUpDueDate, followUpLine,
  followUpReminderText, followUpState, followUpTagLabel, formatDoctorReminderDate,
  doctorMessagePreview,
  getDoctorMessageSummary, hasUnreadMessages, isAttentionServiceFeedbackRecord, isPendingFollowUpRecord,
  isRecommendedConsultation, isRiskConsultation, messageProgressLabel, normalizeDoctorReminderRecords,
  ownerType, serviceFeedbackLabel, serviceFeedbackReminderText, serviceFeedbackState, serviceFeedbackTime,
  waitingDoctorReply, patientActionLabel, patientActionState, patientActionSummary, patientActionTagType
} from '@/doctor/reminder'

const router = useRouter()
const route = useRoute()
const accountContext = inject('account-context', null)
const loading = ref(false)
const records = ref([])
const feedFilter = ref(resolveFeedFilter(route.query.feed))
const loadedOnce = ref(false)
const assignmentLoadingId = ref(0)
const assignmentType = ref('')

const doctorSummary = computed(() => accountContext?.doctorWorkspaceSummary || {})
const doctorSummaryLoading = computed(() => accountContext?.doctorWorkspaceSummaryLoading ?? false)
const doctorId = computed(() => doctorSummary.value?.doctorId || null)
const doctorBound = computed(() => doctorSummary.value?.bound === 1)

const unclaimedRecords = computed(() => records.value.filter(item => item.status !== 'completed' && ownerType(item, doctorId.value) === 'unclaimed').slice().sort(compareUnclaimedRecords))
const recommendedRecords = computed(() => records.value.filter(item => isRecommendedConsultation(item, doctorId.value)).slice().sort(compareRecommendedRecords))
const unreadRecords = computed(() => records.value.filter(hasUnreadMessages).slice().sort(compareMessageRecords))
const waitingReplyRecords = computed(() => records.value.filter(waitingDoctorReply).slice().sort(compareMessageRecords))
const pendingFollowUpRecords = computed(() => records.value.filter(isPendingFollowUpRecord).slice().sort(compareFollowUpRecords))
const attentionServiceFeedbackRecords = computed(() => records.value
  .filter(item => isAttentionServiceFeedbackRecord(item, doctorId.value))
  .slice()
  .sort(compareServiceFeedbackRecords))
const actionableRecords = computed(() => records.value.filter(isActionableRecord).slice().sort(compareFeedRecords))

const unclaimedCount = computed(() => unclaimedRecords.value.length)
const recommendedCount = computed(() => recommendedRecords.value.length)
const unreadCount = computed(() => unreadRecords.value.length)
const waitingReplyCount = computed(() => waitingReplyRecords.value.length)
const pendingFollowUpCount = computed(() => pendingFollowUpRecords.value.length)
const attentionServiceFeedbackCount = computed(() => attentionServiceFeedbackRecords.value.length)
const dueTodayFollowUpCount = computed(() => pendingFollowUpRecords.value.filter(item => followUpState(item) === 'due_today').length)
const overdueFollowUpCount = computed(() => pendingFollowUpRecords.value.filter(item => followUpState(item) === 'overdue').length)
const actionableRecordCount = computed(() => actionableRecords.value.length)

const filteredFeedRecords = computed(() => actionableRecords.value.filter(item => {
  if (feedFilter.value === 'unclaimed') return ownerType(item, doctorId.value) === 'unclaimed'
  if (feedFilter.value === 'recommended') return isRecommendedConsultation(item, doctorId.value)
  if (feedFilter.value === 'unread') return hasUnreadMessages(item)
  if (feedFilter.value === 'reply') return waitingDoctorReply(item)
  if (feedFilter.value === 'feedback') return isAttentionServiceFeedbackRecord(item, doctorId.value)
  if (feedFilter.value === 'followup') return isPendingFollowUpRecord(item)
  if (feedFilter.value === 'overdue') return followUpState(item) === 'overdue'
  return true
}))

const heroSummary = computed(() => {
  if (!records.value.length) return '当前科室还没有可处理的问诊待办，新的认领、消息和随访事项会统一汇总到这里。'
  if (overdueFollowUpCount.value > 0) return `当前有 ${overdueFollowUpCount.value} 条问诊已经逾期随访，建议优先完成本轮回访。`
  if (attentionServiceFeedbackCount.value > 0) return `当前有 ${attentionServiceFeedbackCount.value} 条服务评价需要重点关注，建议优先回看低分或未解决反馈。`
  if (unclaimedCount.value > 0) return `当前仍有 ${unclaimedCount.value} 条问诊待认领，可优先接手后继续处理。`
  if (unreadCount.value > 0) return `患者刚补充了 ${unreadCount.value} 条新消息，建议尽快回看病情变化。`
  if (waitingReplyCount.value > 0) return `仍有 ${waitingReplyCount.value} 条问诊在等待医生继续回复，可在这里集中推进。`
  if (pendingFollowUpCount.value > 0) return `当前共有 ${pendingFollowUpCount.value} 条问诊处于待随访状态，可按计划继续跟进。`
  return '当前没有需要立即处理的高优先级待办，你可以回到问诊列表继续浏览其他记录。'
})

function loadRecords() {
  loading.value = true
  get('/api/doctor/consultation/list', data => {
    records.value = normalizeDoctorReminderRecords(data || [])
    accountContext?.refreshDoctorWorkspaceSummary?.()
    loading.value = false
  }, message => {
    loading.value = false
    ElMessage.warning(message || '医生待办记录加载失败')
  })
}

function submitAssignment(type, record, openAfterSuccess = false) {
  const consultationId = Number(record?.id || 0)
  if (!consultationId || !doctorBound.value) return
  assignmentLoadingId.value = consultationId
  assignmentType.value = type
  post(`/api/doctor/consultation/${type}`, { consultationId }, () => {
    assignmentLoadingId.value = 0
    assignmentType.value = ''
    ElMessage.success(type === 'claim' ? '问诊单认领成功' : '问诊单已释放')
    loadRecords()
    if (type === 'claim' && openAfterSuccess) openConsultationRecord(record, primaryActionQuery(record))
  }, message => {
    assignmentLoadingId.value = 0
    assignmentType.value = ''
    ElMessage.warning(message || (type === 'claim' ? '问诊单认领失败' : '问诊单释放失败'))
  })
}

function openWorkbench() { router.push('/doctor/workbench') }
function openConsultationList(query = {}) { router.push({ path: '/doctor/consultation', query }) }
function openConsultationRecord(record, query = {}) {
  if (!record?.id) return
  router.push({ path: '/doctor/consultation', query: { ...query, id: record.id } })
}
function isAssignmentLoading(record, type = '') {
  return assignmentLoadingId.value === Number(record?.id || 0) && (!type || assignmentType.value === type)
}
function isAssignmentBusy(record) {
  return assignmentLoadingId.value === Number(record?.id || 0)
}
function canQuickClaim(record) {
  return !!record && doctorBound.value && record.status !== 'completed' && ownerType(record, doctorId.value) === 'unclaimed'
}
function canQuickRelease(record) {
  const assignment = record?.doctorAssignment
  return !!record
    && doctorBound.value
    && record.status !== 'completed'
    && assignment?.status === 'claimed'
    && Number(assignment.doctorId || 0) === Number(doctorId.value || 0)
}
function canQuickReply(record) {
  return !!record
    && ownerType(record, doctorId.value) === 'mine'
    && (hasUnreadMessages(record) || waitingDoctorReply(record))
}
function canQuickFollowUp(record) {
  return !!record
    && ownerType(record, doctorId.value) === 'mine'
    && isPendingFollowUpRecord(record)
}
function primaryActionMode(record) {
  if (canQuickClaim(record)) return 'claim'
  if (canQuickFollowUp(record) && ['overdue', 'due_today'].includes(followUpState(record))) return 'followup'
  if (canQuickReply(record)) return 'reply'
  if (canQuickFollowUp(record)) return 'followup'
  if (isAttentionServiceFeedbackRecord(record, doctorId.value)) return 'feedback'
  return 'detail'
}
function primaryActionLabel(record) {
  const mode = primaryActionMode(record)
  if (mode === 'feedback') return '查看评价'
  if (mode === 'claim') return '认领并处理'
  if (mode === 'reply') return '去回复'
  if (mode === 'followup') return '去随访'
  return '查看详情'
}
function replyActionQuery(record) {
  return hasUnreadMessages(record)
    ? { messageFilter: 'unread', action: 'reply' }
    : { messageFilter: 'waiting_reply', action: 'reply' }
}
function followUpActionQuery(record) {
  return followUpState(record) === 'overdue'
    ? { followUpFilter: 'overdue', sortMode: 'follow_up_due', action: 'followup' }
    : { followUpFilter: 'pending', sortMode: 'follow_up_due', action: 'followup' }
}
function feedbackActionQuery() {
  return { status: 'completed', feedbackFilter: 'attention', action: 'feedback' }
}
function primaryActionQuery(record) {
  const mode = primaryActionMode(record)
  if (mode === 'claim' && (hasUnreadMessages(record) || waitingDoctorReply(record))) return replyActionQuery(record)
  if (mode === 'reply') return replyActionQuery(record)
  if (mode === 'followup') return followUpActionQuery(record)
  if (mode === 'feedback') return feedbackActionQuery()
  return reminderQuery(record)
}
function handlePrimaryAction(record) {
  if (canQuickClaim(record)) {
    submitAssignment('claim', record, true)
    return
  }
  openConsultationRecord(record, primaryActionQuery(record))
}

function resolveFeedFilter(value) { return ['all', 'unclaimed', 'recommended', 'unread', 'reply', 'feedback', 'followup', 'overdue'].includes(value) ? value : 'all' }
function buildReminderCenterQuery(nextFeed) {
  const query = { ...route.query }
  if (nextFeed && nextFeed !== 'all') query.feed = nextFeed
  else delete query.feed
  return query
}
function isActionableRecord(record) {
  return ownerType(record, doctorId.value) === 'unclaimed'
    || isRecommendedConsultation(record, doctorId.value)
    || hasUnreadMessages(record)
    || waitingDoctorReply(record)
    || isAttentionServiceFeedbackRecord(record, doctorId.value)
    || isPendingFollowUpRecord(record)
}
function feedListQuery(feed) {
  if (feed === 'unclaimed') return { ownerFilter: 'unclaimed' }
  if (feed === 'recommended') return { dispatchFilter: 'recommended_to_me' }
  if (feed === 'unread') return { messageFilter: 'unread' }
  if (feed === 'reply') return { messageFilter: 'waiting_reply' }
  if (feed === 'feedback') return { status: 'completed', feedbackFilter: 'attention' }
  if (feed === 'followup') return { followUpFilter: 'pending', sortMode: 'follow_up_due' }
  if (feed === 'overdue') return { followUpFilter: 'overdue', sortMode: 'follow_up_due' }
  return {}
}
function reminderQuery(record) {
  if (followUpState(record) === 'overdue') return { followUpFilter: 'overdue', sortMode: 'follow_up_due' }
  if (isPendingFollowUpRecord(record)) return { followUpFilter: 'pending', sortMode: 'follow_up_due' }
  if (hasUnreadMessages(record)) return { messageFilter: 'unread' }
  if (waitingDoctorReply(record)) return { messageFilter: 'waiting_reply' }
  if (isAttentionServiceFeedbackRecord(record, doctorId.value)) return { status: 'completed', feedbackFilter: 'attention' }
  if (isRecommendedConsultation(record, doctorId.value)) return { dispatchFilter: 'recommended_to_me' }
  if (ownerType(record, doctorId.value) === 'unclaimed') return { ownerFilter: 'unclaimed' }
  return {}
}
function ownerLabel(record) {
  const type = ownerType(record, doctorId.value)
  if (type === 'mine') return '我已认领'
  if (type === 'others') return `由 ${record?.doctorAssignment?.doctorName || '其他医生'} 认领`
  return '待认领'
}
function primaryTaskLabel(record) {
  if (isAttentionServiceFeedbackRecord(record, doctorId.value)) return serviceFeedbackLabel(record, doctorId.value)
  if (followUpState(record) === 'overdue') return '逾期随访'
  if (followUpState(record) === 'due_today') return '今日到期随访'
  if (hasUnreadMessages(record) || waitingDoctorReply(record)) return messageProgressLabel(record)
  if (ownerType(record, doctorId.value) === 'unclaimed' && isRiskConsultation(record)) return '高优先级待认领'
  if (ownerType(record, doctorId.value) === 'unclaimed') return '待认领'
  if (isRecommendedConsultation(record, doctorId.value)) return '系统推荐'
  return '处理中'
}
function feedItemSummary(record) {
  if (isAttentionServiceFeedbackRecord(record, doctorId.value)) return serviceFeedbackReminderText(record, doctorId.value)
  if (hasUnreadMessages(record) || waitingDoctorReply(record)) return doctorMessagePreview(record) || record.chiefComplaint || '暂无更多沟通内容'
  if (isPendingFollowUpRecord(record)) return followUpReminderText(record)
  if (isRecommendedConsultation(record, doctorId.value)) return record.smartDispatch?.hint || record.chiefComplaint || '系统建议由你优先接手当前问诊。'
  if (ownerType(record, doctorId.value) === 'unclaimed') return record.smartDispatch?.hint || record.chiefComplaint || '当前问诊尚未认领，可优先接手后继续处理。'
  return record.chiefComplaint || '当前暂无更多待办摘要'
}
function feedItemTimeText(record) {
  if (isAttentionServiceFeedbackRecord(record, doctorId.value)) return formatDate(serviceFeedbackTime(record) || record.updateTime || record.createTime)
  if (hasUnreadMessages(record) || waitingDoctorReply(record)) return formatDate(getDoctorMessageSummary(record).latestTime || record.updateTime || record.createTime)
  if (isPendingFollowUpRecord(record)) return followUpLine(record)
  return formatDate(record.createTime)
}
function feedItemClass(record) {
  const followUp = followUpState(record)
  if (serviceFeedbackState(record, doctorId.value) === 'critical') return 'is-urgent'
  if (['pending', 'low_score'].includes(serviceFeedbackState(record, doctorId.value))) return 'is-warning'
  if (followUp === 'overdue') return 'is-overdue'
  if (followUp === 'due_today') return 'is-due-today'
  if (hasUnreadMessages(record) || (ownerType(record, doctorId.value) === 'unclaimed' && isRiskConsultation(record))) return 'is-urgent'
  if (isRecommendedConsultation(record, doctorId.value)) return 'is-accent'
  return ''
}
function hasPatientActionInsight(record) {
  return patientActionState(record) !== 'none'
}
function patientActionChipClass(record) {
  const type = patientActionTagType(record)
  if (type === 'danger') return 'chip-danger'
  if (type === 'warning') return 'chip-warning'
  if (type === 'success') return 'chip-success'
  if (type === 'primary') return 'chip-primary'
  return 'chip-info'
}
function compareNumber(left, right) {
  if (left === right) return 0
  return left < right ? -1 : 1
}
function compareBool(left, right) {
  if (left === right) return 0
  return left ? -1 : 1
}
function compareUnclaimedRecords(left, right) {
  const riskDiff = compareBool(isRiskConsultation(left), isRiskConsultation(right))
  if (riskDiff !== 0) return riskDiff
  const messageDiff = compareDateDesc(getDoctorMessageSummary(left).latestTime, getDoctorMessageSummary(right).latestTime)
  if (messageDiff !== 0) return messageDiff
  return compareRecentDoctorRecord(left, right)
}
function compareRecommendedRecords(left, right) {
  const riskDiff = compareBool(isRiskConsultation(left), isRiskConsultation(right))
  if (riskDiff !== 0) return riskDiff
  return compareRecentDoctorRecord(left, right)
}
function compareMessageRecords(left, right) {
  const unreadDiff = compareNumber(getDoctorMessageSummary(right).unreadCount || 0, getDoctorMessageSummary(left).unreadCount || 0)
  if (unreadDiff !== 0) return unreadDiff
  const latestDiff = compareDateDesc(getDoctorMessageSummary(left).latestTime || left.updateTime, getDoctorMessageSummary(right).latestTime || right.updateTime)
  if (latestDiff !== 0) return latestDiff
  return compareRecentDoctorRecord(left, right)
}
function compareFollowUpRecords(left, right) {
  const priorityMap = { overdue: 0, due_today: 1, pending: 2, done: 3, none: 4 }
  const stateDiff = compareNumber(priorityMap[followUpState(left)] ?? 9, priorityMap[followUpState(right)] ?? 9)
  if (stateDiff !== 0) return stateDiff
  const dueDiff = compareDateAsc(followUpDueDate(left), followUpDueDate(right))
  if (dueDiff !== 0) return dueDiff
  return compareRecentDoctorRecord(left, right)
}
function compareServiceFeedbackRecords(left, right) {
  const priorityMap = { critical: 0, pending: 1, low_score: 2, resolved: 3, none: 4 }
  const stateDiff = compareNumber(priorityMap[serviceFeedbackState(left, doctorId.value)] ?? 9, priorityMap[serviceFeedbackState(right, doctorId.value)] ?? 9)
  if (stateDiff !== 0) return stateDiff
  const timeDiff = compareDateDesc(serviceFeedbackTime(left), serviceFeedbackTime(right))
  if (timeDiff !== 0) return timeDiff
  return compareRecentDoctorRecord(left, right)
}
function feedPriority(record) {
  const followUp = followUpState(record)
  if (followUp === 'overdue') return 0
  if (followUp === 'due_today') return 1
  if (hasUnreadMessages(record)) return 2
  if (waitingDoctorReply(record)) return 3
  if (serviceFeedbackState(record, doctorId.value) === 'critical') return 4
  if (['pending', 'low_score'].includes(serviceFeedbackState(record, doctorId.value))) return 5
  if (ownerType(record, doctorId.value) === 'unclaimed' && isRiskConsultation(record)) return 6
  if (isRecommendedConsultation(record, doctorId.value)) return 7
  if (ownerType(record, doctorId.value) === 'unclaimed') return 8
  if (isPendingFollowUpRecord(record)) return 9
  return 9
}
function feedSortTime(record) {
  if (isAttentionServiceFeedbackRecord(record, doctorId.value)) return serviceFeedbackTime(record) || record.updateTime || record.createTime
  if (hasUnreadMessages(record) || waitingDoctorReply(record)) return getDoctorMessageSummary(record).latestTime || record.updateTime || record.createTime
  if (isPendingFollowUpRecord(record)) return followUpDueDate(record) || record.updateTime || record.createTime
  return record.createTime || record.updateTime
}
function compareFeedRecords(left, right) {
  const priorityDiff = compareNumber(feedPriority(left), feedPriority(right))
  if (priorityDiff !== 0) return priorityDiff
  const sortTimeDiff = compareDateDesc(feedSortTime(left), feedSortTime(right))
  if (sortTimeDiff !== 0) return sortTimeDiff
  return compareRecentDoctorRecord(left, right)
}
function formatDate(value, onlyDate = false) { return formatDoctorReminderDate(value, onlyDate) }

watch(() => route.query.feed, value => { feedFilter.value = resolveFeedFilter(value) })
watch(feedFilter, value => {
  const nextQuery = buildReminderCenterQuery(value)
  if ((route.query.feed || '') === (nextQuery.feed || '')) return
  router.replace({ path: '/doctor/reminder', query: nextQuery })
})
watch(() => [doctorSummaryLoading.value, doctorSummary.value?.bound], ([summaryLoading, bound]) => {
  if (summaryLoading || bound !== 1 || loadedOnce.value) return
  loadedOnce.value = true
  loadRecords()
}, { immediate: true })

onMounted(() => {
  if (!accountContext?.doctorWorkspaceSummary?.doctorId) accountContext?.refreshDoctorWorkspaceSummary?.(true)
})
</script>

<style scoped>
.doctor-reminder-page { display: flex; flex-direction: column; gap: 18px; }
.hero-card, .panel-card, .stat-card { border: 1px solid var(--app-border); border-radius: 28px; background: var(--app-panel); box-shadow: var(--app-shadow); }
.hero-card { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr); gap: 18px; padding: 26px; }
.hero-copy h2 { margin: 12px 0 8px; font-size: 30px; }
.hero-copy p, .panel-head p, .feed-item-head p, .focus-card small { margin: 0; color: var(--app-muted); line-height: 1.7; }
.section-tag { display: inline-flex; padding: 6px 12px; border-radius: 999px; background: rgba(19, 73, 80, 0.08); color: #27646d; font-size: 12px; letter-spacing: 0.08em; text-transform: uppercase; }
.chip-row, .hero-actions, .feed-actions, .feed-tabs { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.chip-row { margin-top: 16px; }
.chip-row span { display: inline-flex; padding: 8px 14px; border-radius: 999px; background: rgba(19, 73, 80, 0.08); color: #27646d; }
.chip-row .chip-info { background: rgba(19, 73, 80, 0.08); color: #27646d; }
.chip-row .chip-primary { background: rgba(49, 93, 196, 0.12); color: #315dc4; }
.chip-row .chip-success { background: rgba(31, 143, 89, 0.12); color: #1f8f59; }
.chip-row .chip-warning { background: rgba(210, 155, 47, 0.14); color: #9a6a16; }
.chip-row .chip-danger { background: rgba(214, 95, 80, 0.14); color: #9f4336; }
.hero-actions { margin-top: 18px; }
.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 14px; }
.stat-card { padding: 20px 22px; }
.stat-card span { color: var(--app-muted); }
.stat-card strong { display: block; margin-top: 14px; font-size: 30px; }
.stat-card-accent { border-color: rgba(15, 102, 101, 0.24); background: linear-gradient(180deg, rgba(15, 102, 101, 0.08), rgba(255, 255, 255, 0.96)); }
.stat-card-warning { border-color: rgba(210, 155, 47, 0.24); background: linear-gradient(180deg, rgba(210, 155, 47, 0.1), rgba(255, 255, 255, 0.96)); }
.stat-card-danger { border-color: rgba(214, 95, 80, 0.24); background: linear-gradient(180deg, rgba(214, 95, 80, 0.1), rgba(255, 255, 255, 0.96)); }
.panel-card { padding: 24px; }
.panel-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 18px; }
.panel-head h3 { margin: 0; font-size: 22px; }
.focus-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; }
.focus-card { padding: 20px; border: 1px solid rgba(19, 73, 80, 0.1); border-radius: 24px; background: rgba(19, 73, 80, 0.04); text-align: left; cursor: pointer; }
.focus-card strong, .feed-item-head strong { display: block; color: #31474d; }
.focus-card span { display: block; margin: 14px 0 10px; font-size: 34px; font-weight: 700; color: #11464d; }
.feed-list { display: flex; flex-direction: column; gap: 12px; }
.feed-item { padding: 16px 18px; border: 1px solid rgba(19, 73, 80, 0.08); border-radius: 22px; background: #fff; }
.feed-item-head { display: flex; justify-content: space-between; gap: 14px; align-items: flex-start; }
.feed-item-head p { margin-top: 8px; color: #41575d; }
.feed-item-head span { color: var(--app-muted); font-size: 13px; line-height: 1.5; }
.patient-action-line { margin: 12px 0 0; color: #41575d; line-height: 1.7; }
.feed-actions { margin-top: 14px; }
.feed-item.is-urgent, .focus-card:hover { border-color: rgba(214, 95, 80, 0.2); box-shadow: 0 10px 22px rgba(19, 73, 80, 0.08); }
.feed-item.is-warning { border-color: rgba(210, 155, 47, 0.24); background: linear-gradient(180deg, rgba(210, 155, 47, 0.08), rgba(255, 255, 255, 0.98)); }
.feed-item.is-overdue { border-color: rgba(214, 95, 80, 0.24); background: linear-gradient(180deg, rgba(214, 95, 80, 0.08), rgba(255, 255, 255, 0.98)); }
.feed-item.is-due-today { border-color: rgba(210, 155, 47, 0.24); background: linear-gradient(180deg, rgba(210, 155, 47, 0.08), rgba(255, 255, 255, 0.98)); }
.feed-item.is-accent { border-color: rgba(15, 102, 101, 0.22); background: linear-gradient(180deg, rgba(15, 102, 101, 0.07), rgba(255, 255, 255, 0.98)); }
@media (max-width: 1100px) { .hero-card { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .panel-head, .feed-item-head { flex-direction: column; align-items: flex-start; } }
</style>
