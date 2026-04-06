<template>
  <div class="triage-workspace">
    <section class="stat-grid">
      <article class="stat-card">
        <span>导诊记录</span>
        <strong>{{ records.length }}</strong>
      </article>
      <article class="stat-card">
        <span>进行中</span>
        <strong>{{ activeCount }}</strong>
      </article>
      <article class="stat-card">
        <span>线下优先</span>
        <strong>{{ offlineFirstCount }}</strong>
      </article>
      <article class="stat-card">
        <span>已完成</span>
        <strong>{{ completedCount }}</strong>
      </article>
    </section>

    <section class="workspace-layout">
      <aside class="record-panel" v-loading="loading">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">Sessions</span>
            <h3>AI 导诊会话</h3>
          </div>
          <el-button text @click="loadRecords">刷新</el-button>
        </div>

        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索就诊人、分类或主诉"
        />

        <div v-if="filteredRecords.length" class="record-list">
          <button
            v-for="item in filteredRecords"
            :key="item.id"
            type="button"
            :class="['record-card', { active: item.id === activeRecordId }]"
            @click="navigateToRecord(item.id)"
          >
            <div class="record-card-head">
              <strong>{{ item.patientName }}</strong>
              <span class="record-status">{{ statusLabel(item.status) }}</span>
            </div>
            <p>{{ item.categoryName }}{{ item.departmentName ? ` / ${item.departmentName}` : '' }}</p>
            <div class="record-chip-row">
              <span>{{ item.triageLevelName || '待评估' }}</span>
              <span>{{ triageActionLabel(item.triageActionType) }}</span>
            </div>
            <small>{{ smartDispatchStatusLabel(item.smartDispatch) }}</small>
            <small>{{ item.chiefComplaint || '暂无主诉摘要' }}</small>
            <div class="record-foot">
              <span>{{ item.consultationNo }}</span>
              <span>{{ formatDate(item.createTime) }}</span>
            </div>
          </button>
        </div>

        <el-empty v-else description="当前还没有可查看的 AI 导诊记录">
          <el-button type="primary" @click="router.push('/index/consultation')">去发起问诊</el-button>
        </el-empty>
      </aside>

      <section class="detail-panel" v-loading="detailLoading">
        <template v-if="detailRecord">
          <div class="panel-head">
            <div>
              <span class="panel-kicker">Workspace</span>
              <h3>{{ detailRecord.patientName }} 的 AI 导诊工作区</h3>
              <p>{{ detailRecord.categoryName }}{{ detailRecord.departmentName ? ` / ${detailRecord.departmentName}` : '' }}</p>
            </div>
            <div class="detail-actions">
              <el-button @click="refreshDetail">刷新会话</el-button>
              <el-button type="primary" plain @click="router.push('/index/consultation')">返回问诊页</el-button>
            </div>
          </div>

          <div class="summary-grid">
            <article class="summary-card">
              <span>分诊等级</span>
              <strong>
                <span class="triage-badge" :style="triageBadgeStyle(detailRecord.triageLevelColor)">
                  {{ detailRecord.triageLevelName || '待评估' }}
                </span>
              </strong>
            </article>
            <article class="summary-card">
              <span>建议动作</span>
              <strong>{{ triageActionLabel(detailRecord.triageActionType) }}</strong>
            </article>
            <article class="summary-card">
              <span>推荐科室</span>
              <strong>{{ detailRecord.departmentName || '未匹配科室' }}</strong>
            </article>
            <article class="summary-card">
              <span>记录状态</span>
              <strong>{{ statusLabel(detailRecord.status) }}</strong>
            </article>
          </div>

          <div class="content-grid">
            <section class="card-section">
              <div class="section-head">
                <strong>问诊摘要</strong>
                <span>{{ detailRecord.consultationNo }}</span>
              </div>
              <p class="copy"><strong>主诉：</strong>{{ detailRecord.chiefComplaint || '暂无主诉摘要' }}</p>
              <p class="copy"><strong>健康摘要：</strong>{{ detailRecord.healthSummary || '暂无健康摘要' }}</p>
              <p class="copy"><strong>系统建议：</strong>{{ detailRecord.triageSuggestion || '当前暂无更多建议' }}</p>
              <p v-if="detailRecord.triageRuleSummary" class="copy"><strong>风险提示：</strong>{{ detailRecord.triageRuleSummary }}</p>
            </section>

            <section v-if="detailRecord.triageResult" class="card-section">
              <div class="section-head">
                <strong>导诊结果归档</strong>
                <span>置信度 {{ formatConfidence(detailRecord.triageResult.confidenceScore) }}</span>
              </div>
              <p class="copy">{{ detailRecord.triageResult.reasonText || '暂无结果说明' }}</p>
              <div v-if="parseJsonArray(detailRecord.triageResult.riskFlagsJson).length" class="record-chip-row">
                <span v-for="item in parseJsonArray(detailRecord.triageResult.riskFlagsJson)" :key="item">{{ item }}</span>
              </div>
            </section>
          </div>

          <section v-if="doctorCandidates.length" class="card-section">
            <div class="section-head">
              <strong>推荐医生</strong>
              <span>{{ doctorCandidates.length }} 位候选</span>
            </div>
            <div class="doctor-list">
              <article v-for="item in doctorCandidates" :key="item.id" class="doctor-card">
                <img v-if="item.photo" :src="resolveImagePath(item.photo)" :alt="item.name" class="doctor-avatar" />
                <div v-else class="doctor-avatar doctor-avatar-fallback">{{ (item.name || '医').slice(0, 1) }}</div>
                <div class="doctor-copy">
                  <div class="doctor-copy-head">
                    <strong>{{ item.name }}</strong>
                    <span v-if="doctorRecommendationScoreText(item)" class="recommend-score">{{ doctorRecommendationScoreText(item) }}</span>
                  </div>
                  <span>{{ item.title || '在线医生' }}</span>
                  <p>{{ item.expertise || '暂无擅长信息' }}</p>
                  <div v-if="item.matchedServiceTags?.length" class="record-chip-row is-accent">
                    <span v-for="tag in item.matchedServiceTags" :key="`${item.id}-matched-${tag}`">匹配 {{ tag }}</span>
                  </div>
                  <div v-if="item.recommendationReasons?.length" class="record-chip-row is-subtle">
                    <span v-for="reason in item.recommendationReasons.slice(0, 3)" :key="`${item.id}-reason-${reason}`">{{ reason }}</span>
                  </div>
                  <p v-if="item.recommendationSummary" class="copy doctor-recommend-copy"><strong>排序说明：</strong>{{ item.recommendationSummary }}</p>
                  <small>{{ item.nextScheduleText || '暂无后续排班' }}</small>
                </div>
              </article>
            </div>
          </section>

          <section class="card-section">
            <div class="section-head">
              <strong>智能分配进度</strong>
              <el-tag :type="smartDispatchTagType(detailRecord.smartDispatch)" effect="light">
                {{ smartDispatchStatusLabel(detailRecord.smartDispatch) }}
              </el-tag>
            </div>
            <p class="copy">{{ smartDispatchHintText(detailRecord.smartDispatch) }}</p>
            <div class="record-chip-row">
              <span v-if="getSmartDispatch(detailRecord).suggestedDoctorName">
                首推 {{ getSmartDispatch(detailRecord).suggestedDoctorName }}{{ getSmartDispatch(detailRecord).suggestedDoctorTitle ? ` / ${getSmartDispatch(detailRecord).suggestedDoctorTitle}` : '' }}
              </span>
              <span v-if="getSmartDispatch(detailRecord).candidateCount">候选 {{ getSmartDispatch(detailRecord).candidateCount }} 位</span>
              <span v-if="getSmartDispatch(detailRecord).suggestedDoctorNextScheduleText">{{ getSmartDispatch(detailRecord).suggestedDoctorNextScheduleText }}</span>
            </div>
            <p v-if="getSmartDispatch(detailRecord).recommendationReason" class="copy">
              <strong>推荐依据：</strong>{{ getSmartDispatch(detailRecord).recommendationReason }}
            </p>
          </section>

          <section class="card-section">
            <div class="section-head">
              <strong>导诊留痕</strong>
              <span>{{ detailRecord.triageSession?.messageCount || 0 }} 条消息</span>
            </div>
            <div class="message-list">
              <article
                v-for="message in triageMessages"
                :key="message.id"
                :class="['message-card', { user: message.roleType === 'user', assistant: message.roleType === 'assistant' }]"
              >
                <div class="message-head">
                  <div>
                    <strong>{{ message.title }}</strong>
                    <span>{{ messageTypeLabel(message.messageType) }}</span>
                  </div>
                  <el-tag size="small" effect="light">{{ messageRoleLabel(message.roleType) }}</el-tag>
                </div>
                <p>{{ message.content }}</p>
                <div v-if="message.insight" class="message-insight">
                  <div class="message-insight-meta">
                    <span v-if="message.insight.recommendedVisitType">建议方式：{{ message.insight.recommendedVisitType }}</span>
                    <span v-if="message.insight.recommendedDepartmentName">建议科室：{{ message.insight.recommendedDepartmentName }}</span>
                    <span v-if="message.insight.confidenceText">置信度：{{ message.insight.confidenceText }}</span>
                  </div>
                  <p v-if="message.insight.doctorRecommendationReason" class="message-insight-copy">
                    <strong>推荐依据：</strong>{{ message.insight.doctorRecommendationReason }}
                  </p>
                  <div v-if="message.insight.recommendedDoctors.length" class="message-insight-tags">
                    <span v-for="item in message.insight.recommendedDoctors" :key="item">{{ item }}</span>
                  </div>
                  <div v-if="message.insight.riskFlags.length" class="message-insight-tags danger">
                    <span v-for="item in message.insight.riskFlags" :key="item">{{ item }}</span>
                  </div>
                </div>
                <small>{{ formatDate(message.createTime) }}</small>
              </article>
            </div>
          </section>

          <section class="card-section composer-card">
            <div class="section-head">
              <strong>继续 AI 导诊</strong>
              <span>{{ triageAiSendHint }}</span>
            </div>
            <el-input
              v-model="triageAiDraft.content"
              type="textarea"
              :rows="4"
              maxlength="1000"
              show-word-limit
              :disabled="!canSendTriageAiMessage"
              placeholder="补充症状变化、持续时间、体温、检查结果或你最关心的问题，AI 会继续导诊。"
            />
            <div class="composer-actions">
              <span class="composer-tip">当前导诊消息会完整保留，后续医生也能继续参考。</span>
              <el-button
                type="primary"
                :loading="triageAiSending"
                :disabled="!canSendTriageAiMessage"
                @click="sendTriageAiMessage"
              >
                发送给 AI
              </el-button>
            </div>
          </section>
        </template>

        <el-empty v-else description="请选择一条导诊记录开始查看">
          <el-button type="primary" @click="router.push('/index/consultation')">去发起问诊</el-button>
        </el-empty>
      </section>
    </section>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, post, resolveImagePath } from '@/net'
import { normalizeSmartDispatch, smartDispatchHintText, smartDispatchStatusLabel, smartDispatchTagType } from '@/triage/dispatch'
import { resolveTriageMessageInsight } from '@/triage/insight'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const detailLoading = ref(false)
const triageAiSending = ref(false)
const records = ref([])
const detailRecord = ref(null)
const activeRecordId = ref(null)
const keyword = ref('')
const triageAiDraft = reactive({ content: '' })

const filteredRecords = computed(() => {
  const text = `${keyword.value || ''}`.trim().toLowerCase()
  if (!text) return records.value
  return records.value.filter(item => {
    const source = [
      item.patientName,
      item.categoryName,
      item.departmentName,
      item.chiefComplaint,
      item.consultationNo
    ].filter(Boolean).join(' ').toLowerCase()
    return source.includes(text)
  })
})

const activeCount = computed(() => records.value.filter(item => item.status !== 'completed').length)
const offlineFirstCount = computed(() => records.value.filter(item => ['offline', 'emergency'].includes(item.triageActionType)).length)
const completedCount = computed(() => records.value.filter(item => item.status === 'completed').length)
const doctorCandidates = computed(() => parseDoctorCandidates(detailRecord.value?.triageResult?.doctorCandidatesJson))
const triageMessages = computed(() => (detailRecord.value?.triageSession?.messages || []).map(message => ({
  ...message,
  insight: resolveTriageMessageInsight(message)
})))
const canSendTriageAiMessage = computed(() => !!detailRecord.value?.triageSession && !triageAiSending.value)
const triageAiSendHint = computed(() => {
  if (!detailRecord.value?.triageSession) return '当前记录还没有生成导诊会话。'
  if (detailRecord.value.triageActionType === 'emergency') return '当前已有高风险提示，如症状继续加重请优先急诊就医。'
  if (detailRecord.value.triageActionType === 'offline') return '可以继续补充变化情况，但线下就医仍然是当前优先建议。'
  return 'AI 会结合已有问诊答案和导诊历史继续分析。'
})

function loadRecords() {
  loading.value = true
  get('/api/user/consultation/record/list', (data) => {
    records.value = (data || []).map(item => ({
      ...item,
      smartDispatch: normalizeSmartDispatch(item?.smartDispatch)
    }))
    loading.value = false
    ensureActiveRecord()
  }, (message) => {
    loading.value = false
    ElMessage.warning(message || '导诊记录加载失败')
  })
}

function ensureActiveRecord() {
  if (!records.value.length) {
    detailRecord.value = null
    activeRecordId.value = null
    return
  }

  const routeRecordId = Number(route.query.recordId || 0)
  const candidate = records.value.find(item => item.id === routeRecordId)
    || records.value.find(item => item.id === activeRecordId.value)
    || records.value[0]

  if (candidate) {
    if (candidate.id !== activeRecordId.value) {
      openRecord(candidate.id, false)
    } else if (!detailRecord.value) {
      refreshDetail()
    }
  }
}

function navigateToRecord(recordId) {
  if (!recordId) return
  router.replace({ path: '/index/triage', query: { recordId } })
  if (recordId === activeRecordId.value) {
    refreshDetail()
  }
}

function openRecord(recordId, updateRoute = true) {
  if (!recordId) return
  activeRecordId.value = recordId
  resetTriageAiDraft()
  if (updateRoute) {
    router.replace({ path: '/index/triage', query: { recordId } })
  }

  detailLoading.value = true
  get(`/api/user/consultation/record/detail?recordId=${recordId}`, (data) => {
    detailRecord.value = data ? {
      ...data,
      smartDispatch: normalizeSmartDispatch(data?.smartDispatch)
    } : null
    detailLoading.value = false
  }, (message) => {
    detailLoading.value = false
    detailRecord.value = null
    ElMessage.warning(message || '导诊记录详情加载失败')
  })
}

function refreshDetail() {
  if (!activeRecordId.value) return
  openRecord(activeRecordId.value, false)
}

function resetTriageAiDraft() {
  triageAiDraft.content = ''
}

function sendTriageAiMessage() {
  const recordId = detailRecord.value?.id
  const content = `${triageAiDraft.content || ''}`.trim()
  if (!recordId || !detailRecord.value?.triageSession) return
  if (!content) {
    ElMessage.warning('请先输入想继续补充给 AI 的内容')
    return
  }

  triageAiSending.value = true
  post('/api/user/consultation/triage/message/send', {
    recordId,
    content
  }, () => {
    triageAiSending.value = false
    resetTriageAiDraft()
    ElMessage.success('AI 导诊已更新本轮分析')
    refreshDetail()
  }, (message) => {
    triageAiSending.value = false
    ElMessage.warning(message || 'AI 导诊响应失败')
  })
}

function triageActionLabel(value) {
  return {
    emergency: '立即急诊',
    offline: '尽快线下',
    followup: '复诊随访',
    online: '线上继续'
  }[value] || '继续关注'
}

function statusLabel(value) {
  return {
    submitted: '已提交',
    triaged: '已分诊',
    processing: '处理中',
    completed: '已完成'
  }[value] || value || '-'
}

function messageRoleLabel(value) {
  return {
    user: '患者',
    assistant: 'AI 导诊',
    system: '系统',
    rule_engine: '规则引擎'
  }[value] || value || '-'
}

function messageTypeLabel(value) {
  return {
    intake_summary: '问诊摘要',
    health_summary: '健康摘要',
    triage_result: '分诊结果',
    rule_summary: '规则摘要',
    rule_hit: '命中详情',
    ai_triage_summary: 'AI 导诊建议',
    ai_followup_questions: 'AI 建议补充',
    ai_user_followup: '患者补充',
    ai_chat_reply: 'AI 导诊回复'
  }[value] || value || '-'
}

function triageBadgeStyle(color) {
  if (!color) return {}
  return {
    color,
    borderColor: `${color}33`,
    backgroundColor: `${color}14`
  }
}

function parseJsonArray(value) {
  if (!value) return []
  try {
    const result = JSON.parse(value)
    return Array.isArray(result) ? result : []
  } catch {
    return []
  }
}

function parseDoctorCandidates(value) {
  return parseJsonArray(value).filter(item => item && typeof item === 'object')
}

function doctorRecommendationScoreText(item) {
  const number = Number(item?.recommendationScore)
  return Number.isFinite(number) && number > 0 ? `优先分 ${number}` : ''
}

function getSmartDispatch(record) {
  return normalizeSmartDispatch(record?.smartDispatch)
}

function formatConfidence(value) {
  const number = Number(value)
  if (Number.isNaN(number) || number <= 0) return '-'
  return `${Math.round(number * 100)}%`
}

function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

watch(() => route.query.recordId, (value) => {
  if (!records.value.length) return
  const recordId = Number(value || 0)
  if (!recordId) {
    if (!activeRecordId.value) ensureActiveRecord()
    return
  }
  if (recordId !== activeRecordId.value) {
    openRecord(recordId, false)
  }
})

onMounted(() => loadRecords())
</script>

<style scoped>
.triage-workspace {
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
.record-panel,
.detail-panel,
.summary-card,
.card-section,
.record-card,
.doctor-card,
.message-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.stat-card {
  padding: 22px 24px;
}

.stat-card span,
.panel-kicker,
.section-head span,
.record-card p,
.record-card small,
.record-foot span,
.doctor-copy span,
.doctor-copy small,
.composer-tip {
  color: var(--app-muted);
}

.stat-card strong {
  display: block;
  margin-top: 14px;
  font-size: 30px;
}

.workspace-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
}

.record-panel,
.detail-panel {
  padding: 22px;
}

.panel-head,
.detail-actions,
.section-head,
.message-head,
.composer-actions,
.record-card-head,
.record-foot {
  display: flex;
  gap: 12px;
}

.panel-head,
.section-head,
.record-card-head,
.record-foot,
.composer-actions {
  justify-content: space-between;
  align-items: flex-start;
}

.panel-head h3 {
  margin: 6px 0 0;
}

.panel-head p {
  margin: 6px 0 0;
  color: var(--app-muted);
}

.record-list,
.message-list,
.doctor-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.record-list {
  margin-top: 16px;
}

.record-card {
  padding: 16px 18px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.record-card.active {
  transform: translateY(-2px);
  border-color: rgba(15, 102, 101, 0.38);
  box-shadow: 0 16px 28px rgba(15, 102, 101, 0.12);
}

.record-card p,
.record-card small {
  display: block;
  margin: 10px 0 0;
  line-height: 1.7;
}

.record-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.record-chip-row span,
.record-status {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #48656d;
  font-size: 12px;
}

.summary-grid,
.content-grid {
  display: grid;
  gap: 16px;
}

.summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 18px;
}

.summary-card,
.card-section {
  padding: 18px;
}

.summary-card span {
  color: var(--app-muted);
  font-size: 12px;
}

.summary-card strong {
  display: block;
  margin-top: 12px;
}

.content-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 18px;
}

.copy {
  margin: 10px 0 0;
  line-height: 1.8;
  color: #41575d;
}

.doctor-list {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.doctor-card {
  padding: 16px;
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.doctor-avatar {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  object-fit: cover;
  background: rgba(15, 102, 101, 0.08);
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.doctor-avatar-fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #0f6665;
  font-size: 22px;
  font-weight: 700;
}

.doctor-copy strong {
  display: block;
}

.doctor-copy-head {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
}

.doctor-copy p {
  margin: 10px 0 0;
  line-height: 1.7;
  color: #41575d;
}

.recommend-score {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.12);
  color: #0f6665;
  font-size: 12px;
  white-space: nowrap;
}

.record-chip-row.is-accent span {
  background: rgba(77, 168, 132, 0.14);
  color: #1f6f4f;
}

.record-chip-row.is-subtle span {
  background: rgba(19, 73, 80, 0.06);
  color: #48656d;
}

.doctor-recommend-copy {
  margin-top: 10px;
}

.message-list {
  margin-top: 14px;
}

.message-card {
  padding: 16px 18px;
  background: rgba(255, 255, 255, 0.72);
}

.message-card.user {
  border-color: rgba(15, 102, 101, 0.18);
  background: rgba(15, 102, 101, 0.05);
}

.message-card.assistant {
  border-color: rgba(222, 162, 72, 0.2);
  background: rgba(244, 197, 119, 0.1);
}

.message-head strong {
  display: block;
}

.message-head span,
.message-card small {
  color: var(--app-muted);
}

.message-card p {
  margin: 12px 0;
  line-height: 1.8;
  color: #41575d;
  white-space: pre-wrap;
}

.message-insight {
  margin: 0 0 12px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(15, 102, 101, 0.06);
  border: 1px solid rgba(15, 102, 101, 0.1);
}

.message-insight-meta,
.message-insight-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.message-insight-meta span,
.message-insight-tags span {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.1);
  color: #48656d;
  font-size: 12px;
}

.message-insight-copy {
  margin: 10px 0 0;
  line-height: 1.7;
  color: #48656d;
}

.message-insight-tags {
  margin-top: 10px;
}

.message-insight-tags.danger span {
  background: rgba(214, 95, 80, 0.12);
  color: #9f4336;
}

.composer-card {
  margin-top: 18px;
}

.composer-actions {
  margin-top: 12px;
  align-items: center;
}

.triage-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid rgba(15, 102, 101, 0.18);
  background: rgba(15, 102, 101, 0.08);
  color: #0f6665;
  font-size: 12px;
  font-weight: 600;
}

@media (max-width: 1260px) {
  .workspace-layout,
  .summary-grid,
  .content-grid,
  .doctor-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .stat-grid,
  .workspace-layout,
  .summary-grid,
  .content-grid,
  .doctor-list {
    grid-template-columns: 1fr;
  }

  .panel-head,
  .detail-actions,
  .section-head,
  .message-head,
  .composer-actions,
  .record-card-head,
  .record-foot {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
