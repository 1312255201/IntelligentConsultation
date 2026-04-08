<template>
  <div class="ai-config-page" v-loading="loading">
    <section class="hero-card">
      <div class="hero-copy">
        <span class="section-tag">AI Governance</span>
        <h2>AI 导诊配置与状态</h2>
        <p>{{ overview.runtimeStatus || '当前正在读取 AI 导诊运行状态。' }}</p>
        <div class="chip-row">
          <span>{{ availabilityLabel(overview.runtimeAvailable, '运行可用', '当前不可用') }}</span>
          <span>{{ availabilityLabel(overview.triageEnabled, '总开关已启用', '总开关已关闭') }}</span>
          <span>{{ availabilityLabel(overview.providerEnabled, 'DeepSeek 接入已启用', 'DeepSeek 接入未启用') }}</span>
          <span>{{ availabilityLabel(overview.apiKeyConfigured, 'API Key 已配置', 'API Key 未配置') }}</span>
          <span v-if="overview.promptVersion">Prompt {{ overview.promptVersion }}</span>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="loadOverview">刷新状态</el-button>
          <el-button @click="router.push('/admin/consultation-record')">导诊记录中心</el-button>
          <el-button plain @click="router.push('/admin/consultation-dispatch')">智能分配策略</el-button>
        </div>
      </div>

      <div class="hero-side">
        <article class="hero-metric">
          <span>AI 运行态</span>
          <strong :class="statusToneClass(overview.runtimeAvailable)">
            {{ overview.runtimeAvailable ? 'Ready' : 'Fallback' }}
          </strong>
          <p>{{ runtimeModeSummary }}</p>
        </article>
        <article class="hero-metric hero-metric-accent">
          <span>最近 AI 输出</span>
          <strong>{{ formatDate(overview.latestAiMessageTime, true) }}</strong>
          <p>{{ totalAiMessages }} 条 AI 导诊消息已经写入导诊留痕。</p>
        </article>
      </div>
    </section>

    <section class="status-grid">
      <article class="status-card">
        <span>AI 导诊总开关</span>
        <strong>{{ availabilityLabel(overview.triageEnabled) }}</strong>
        <el-tag :type="statusTagType(overview.triageEnabled)" effect="light">{{ statusTagLabel(overview.triageEnabled) }}</el-tag>
      </article>
      <article class="status-card">
        <span>DeepSeek Chat 接入</span>
        <strong>{{ availabilityLabel(overview.providerEnabled) }}</strong>
        <el-tag :type="statusTagType(overview.providerEnabled)" effect="light">{{ statusTagLabel(overview.providerEnabled) }}</el-tag>
      </article>
      <article class="status-card">
        <span>API Key</span>
        <strong>{{ availabilityLabel(overview.apiKeyConfigured) }}</strong>
        <el-tag :type="statusTagType(overview.apiKeyConfigured)" effect="light">{{ statusTagLabel(overview.apiKeyConfigured) }}</el-tag>
      </article>
      <article class="status-card">
        <span>ChatModel Bean</span>
        <strong>{{ availabilityLabel(overview.modelBeanReady) }}</strong>
        <el-tag :type="statusTagType(overview.modelBeanReady)" effect="light">{{ statusTagLabel(overview.modelBeanReady) }}</el-tag>
      </article>
    </section>

    <section class="content-grid">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>当前模型参数</h3>
            <p>这里展示当前 Spring AI + DeepSeek 已解析到的运行参数，便于排查环境差异。</p>
          </div>
        </div>

        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="模型供应商">{{ overview.providerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Prompt 版本">{{ overview.promptVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Base URL">{{ overview.providerBaseUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="模型名称">{{ overview.modelName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Temperature">{{ overview.temperature ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="Max Tokens">{{ overview.maxTokens ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="医生候选上限">{{ overview.doctorCandidateLimit ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近 AI 输出">{{ formatDate(overview.latestAiMessageTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="env-board">
          <strong>推荐检查的环境变量</strong>
          <pre>DEEPSEEK_ENABLED
DEEPSEEK_API_KEY
DEEPSEEK_MODEL
DEEPSEEK_TEMPERATURE
DEEPSEEK_MAX_TOKENS
CONSULTATION_AI_TRIAGE_ENABLED
CONSULTATION_AI_TRIAGE_PROMPT_VERSION</pre>
        </div>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>AI 导诊产出概况</h3>
            <p>快速确认 AI 已覆盖多少导诊流程，以及首轮增强与继续追问是否持续产出。</p>
          </div>
        </div>

        <div class="metric-grid">
          <article class="metric-card">
            <span>问诊记录</span>
            <strong>{{ overview.consultationCount || 0 }}</strong>
          </article>
          <article class="metric-card">
            <span>导诊会话</span>
            <strong>{{ overview.triageSessionCount || 0 }}</strong>
          </article>
          <article class="metric-card">
            <span>可继续会话</span>
            <strong>{{ overview.openSessionCount || 0 }}</strong>
          </article>
          <article class="metric-card">
            <span>导诊结果</span>
            <strong>{{ overview.triageResultCount || 0 }}</strong>
          </article>
          <article class="metric-card metric-card-accent">
            <span>AI 导诊总结</span>
            <strong>{{ overview.aiSummaryMessageCount || 0 }}</strong>
          </article>
          <article class="metric-card">
            <span>AI 追问建议</span>
            <strong>{{ overview.aiFollowupQuestionCount || 0 }}</strong>
          </article>
          <article class="metric-card">
            <span>AI 对话回复</span>
            <strong>{{ overview.aiChatReplyCount || 0 }}</strong>
          </article>
          <article class="metric-card">
            <span>患者补充追问</span>
            <strong>{{ overview.userFollowupMessageCount || 0 }}</strong>
          </article>
        </div>
      </article>
    </section>

    <section class="panel-card queue-panel" v-loading="queueLoading">
      <div class="panel-head">
        <div>
          <h3>高风险待复核队列</h3>
          <p>优先聚合仍未完成医生复核，或医生已明确标记与 AI 存在差异的高风险样本，方便管理员先盯重点。</p>
        </div>
        <div class="audit-toolbar">
          <el-input
            v-model="reviewQueueKeyword"
            clearable
            placeholder="搜索单号、患者、分类或科室"
            style="width: 240px"
            @keyup.enter="loadReviewQueue"
          />
          <el-button type="primary" plain @click="loadReviewQueue">筛选</el-button>
          <el-button @click="resetReviewQueueFilters">重置</el-button>
          <el-button plain :loading="queueExporting" @click="exportReviewQueue">Export CSV</el-button>
        </div>
      </div>

      <div class="audit-summary-bar">
        <span>当前待复核 {{ reviewQueueItems.length }} 条</span>
        <span>待医生接手 {{ queuePendingTakeoverCount }} 条</span>
        <span>待形成结论 {{ queuePendingConclusionCount }} 条</span>
        <span v-if="queueMismatchCount">医生判定有差异 {{ queueMismatchCount }} 条</span>
      </div>

      <div v-if="reviewQueueItems.length" class="queue-list">
        <article v-for="item in reviewQueueItems" :key="`queue-${item.messageId}`" class="audit-item queue-item">
          <div class="chip-row audit-head-chips">
            <span>{{ item.patientName || '未命名患者' }}</span>
            <span>{{ item.consultationNo || item.sessionNo || '未生成单号' }}</span>
            <span>{{ item.categoryName || '未分类' }}</span>
            <span>{{ formatDate(item.createTime, true) }}</span>
            <span class="chip-danger">{{ reviewQueueStatusLabel(item) }}</span>
          </div>

          <p class="copy"><strong>高风险信号：</strong>{{ reviewQueueReasonText(item) }}</p>
          <p class="copy"><strong>AI 判断：</strong>{{ queueAiSummaryText(item) }}</p>
          <p class="copy"><strong>当前处理：</strong>{{ reviewQueueProgressText(item) }}</p>
          <p v-if="item.doctorReview?.compareText" class="copy"><strong>对比结果：</strong>{{ item.doctorReview.compareText }}</p>

          <div v-if="reviewQueueTags(item).length" class="audit-tag-row danger">
            <span v-for="tag in reviewQueueTags(item)" :key="`${item.messageId}-${tag}`">{{ tag }}</span>
          </div>
          <div class="audit-actions">
            <el-button
              type="primary"
              plain
              size="small"
              :disabled="!item.consultationId"
              @click="goToConsultationDetail(item)"
            >
              立即复盘
            </el-button>
            <el-button
              size="small"
              @click="goToConsultationCenter(item)"
            >
              导诊记录中心
            </el-button>
          </div>
        </article>
      </div>
      <el-empty v-else :description="queueEmptyDescription" />
    </section>

    <section class="panel-card audit-panel" v-loading="auditLoading">
      <div class="panel-head">
        <div>
          <h3>最近 AI 导诊输出审计</h3>
          <p>从导诊消息留痕中直接抽样最近 AI 输出，方便复盘 Prompt 版本、风险提示与推荐结果是否稳定。</p>
        </div>
        <div class="audit-toolbar">
          <el-input
            v-model="auditKeyword"
            clearable
            placeholder="搜索单号、患者、分类或科室"
            style="width: 240px"
            @keyup.enter="loadAuditList"
          />
          <el-select v-model="auditMessageType" style="width: 200px" @change="loadAuditList">
            <el-option
              v-for="item in auditMessageOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-switch
            v-model="highRiskOnly"
            inline-prompt
            active-text="高风险"
            inactive-text="全部"
            @change="loadAuditList"
          />
          <el-button type="primary" plain @click="loadAuditList">筛选</el-button>
          <el-button @click="resetAuditFilters">重置</el-button>
          <el-button plain :loading="auditExporting" @click="exportAuditList">Export CSV</el-button>
        </div>
      </div>

      <div class="audit-summary-bar">
        <span>当前抽样 {{ auditItems.length }} 条</span>
        <span>高风险关注 {{ highRiskAuditCount }} 条</span>
        <span>医生已复核 {{ reviewedAuditCount }} 条</span>
        <span v-if="reviewMismatchCount">存在差异 {{ reviewMismatchCount }} 条</span>
        <span v-if="highRiskOnly">已切换为仅显示高风险输出</span>
      </div>

      <div v-if="displayAuditItems.length" class="audit-list">
        <article v-for="item in displayAuditItems" :key="item.messageId" class="audit-item">
          <div class="chip-row audit-head-chips">
            <span>{{ auditTypeLabel(item.messageType) }}</span>
            <span>{{ item.patientName || '未命名患者' }}</span>
            <span>{{ item.consultationNo || item.sessionNo || '未生成单号' }}</span>
            <span>{{ item.categoryName || '未分类' }}</span>
            <span>{{ formatDate(item.createTime, true) }}</span>
            <span v-if="isHighRiskAuditItem(item)" class="chip-danger">高风险关注</span>
          </div>

          <p class="copy"><strong>问诊上下文：</strong>{{ auditContextText(item) }}</p>
          <p v-if="item.insight?.summary" class="copy"><strong>AI 总结：</strong>{{ item.insight.summary }}</p>
          <p v-if="showAuditReply(item)" class="copy"><strong>AI 回复：</strong>{{ item.insight.reply }}</p>
          <p v-if="item.insight?.nextQuestions?.length" class="copy"><strong>建议补充：</strong>{{ item.insight.nextQuestions.join('；') }}</p>
          <p v-else-if="showAuditContent(item)" class="copy"><strong>消息内容：</strong>{{ abbreviateText(item.content, 220) }}</p>
          <p v-if="item.insight?.doctorRecommendationReason" class="copy"><strong>推荐依据：</strong>{{ item.insight.doctorRecommendationReason }}</p>

          <div v-if="auditMetaTags(item).length" class="audit-tag-row">
            <span v-for="tag in auditMetaTags(item)" :key="tag">{{ tag }}</span>
          </div>
          <div v-if="item.insight?.recommendedDoctors?.length" class="audit-tag-row">
            <span v-for="doctor in item.insight.recommendedDoctors" :key="`${item.messageId}-${doctor}`">推荐 {{ doctor }}</span>
          </div>
          <div v-if="item.insight?.riskFlags?.length" class="audit-tag-row danger">
            <span v-for="flag in item.insight.riskFlags" :key="`${item.messageId}-${flag}`">{{ flag }}</span>
          </div>
          <div v-if="item.doctorReview" class="audit-review-card">
            <div class="audit-review-head">
              <strong>医生复盘</strong>
              <div class="audit-review-chips">
                <span v-if="item.doctorReview.handleDoctorName">{{ item.doctorReview.handleDoctorName }}</span>
                <span>{{ item.doctorReview.progressLabel }}</span>
                <span
                  v-if="item.doctorReview.compareOverallStatus"
                  :class="['compare-badge', comparisonStatusClass(item.doctorReview.compareOverallStatus)]"
                >
                  {{ comparisonStatusLabel(item.doctorReview.compareOverallStatus) }}
                </span>
                <span v-if="item.doctorReview.aiConsistencyLabel">{{ item.doctorReview.aiConsistencyLabel }}</span>
              </div>
            </div>
            <p class="copy"><strong>处理进度：</strong>{{ item.doctorReview.progressText }}</p>
            <p v-if="item.doctorReview.doctorConclusionText" class="copy"><strong>医生结论：</strong>{{ item.doctorReview.doctorConclusionText }}</p>
            <p v-if="item.doctorReview.compareText" class="copy"><strong>对比结果：</strong>{{ item.doctorReview.compareText }}</p>
            <div v-if="item.doctorReview.mismatchReasonLabels.length" class="audit-tag-row danger">
              <span v-for="tag in item.doctorReview.mismatchReasonLabels" :key="`${item.messageId}-${tag}`">{{ tag }}</span>
            </div>
            <p v-if="item.doctorReview.mismatchRemark" class="copy"><strong>差异说明：</strong>{{ item.doctorReview.mismatchRemark }}</p>
          </div>
          <div class="audit-actions">
            <el-button
              type="primary"
              plain
              size="small"
              :disabled="!item.consultationId"
              @click="goToConsultationDetail(item)"
            >
              查看问诊详情
            </el-button>
            <el-button
              size="small"
              @click="goToConsultationCenter(item)"
            >
              导诊记录中心
            </el-button>
          </div>
        </article>
      </div>
      <el-empty v-else :description="auditEmptyDescription" />
    </section>

    <section class="content-grid">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>运行告警</h3>
            <p>如果当前环境仍未真正启用 AI，这里会直接指出最可能的阻塞项。</p>
          </div>
        </div>

        <div v-if="overview.warnings?.length" class="warning-list">
          <el-alert
            v-for="item in overview.warnings"
            :key="item"
            :title="item"
            type="warning"
            :closable="false"
          />
        </div>
        <el-empty v-else description="当前没有额外运行告警，AI 导诊配置看起来已经具备基本可用条件。" />
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>治理建议</h3>
            <p>当前页面先聚焦“可见性”和“排障效率”，后续可以在这个入口继续扩展为 Prompt 版本与审计中心。</p>
          </div>
        </div>

        <div class="guideline-list">
          <article class="guideline-item">
            <strong>先确认运行态，再看业务效果</strong>
            <p>如果 DeepSeek 接入未启用或 API Key 未配置，前台仍会继续走规则分诊兜底，所以要先确认这里的运行状态。</p>
          </article>
          <article class="guideline-item">
            <strong>结合导诊记录中心复盘输出质量</strong>
            <p>当 AI 已可用后，建议继续到“导诊记录中心”查看 AI 与医生结论偏差、用户反馈与命中规则留痕。</p>
          </article>
          <article class="guideline-item">
            <strong>智能分配策略与 AI 导诊要一起看</strong>
            <p>Prompt 版本、候选医生上限和智能分配权重会一起影响最终推荐效果，建议配合“智能分配策略”页共同调整。</p>
          </article>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { download, get } from '@/net'
import { aiMismatchReasonLabel, comparisonStatusClass, comparisonStatusLabel } from '@/triage/comparison'
import { resolveTriageMessageAuditInsight } from '@/triage/insight'

const router = useRouter()
const loading = ref(false)
const queueLoading = ref(false)
const auditLoading = ref(false)
const queueExporting = ref(false)
const auditExporting = ref(false)
const overview = ref(createEmptyOverview())
const auditMessageType = ref('all')
const auditKeyword = ref('')
const highRiskOnly = ref(false)
const reviewQueueKeyword = ref('')
const reviewQueueItems = ref([])
const auditItems = ref([])

const REVIEW_QUEUE_LIMIT = 6
const AUDIT_LIMIT = 12
const auditMessageOptions = [
  { label: '全部 AI 输出', value: 'all' },
  { label: 'AI 导诊总结', value: 'ai_triage_summary' },
  { label: 'AI 追问建议', value: 'ai_followup_questions' },
  { label: 'AI 对话回复', value: 'ai_chat_reply' }
]

const totalAiMessages = computed(() => Number(overview.value.aiSummaryMessageCount || 0)
  + Number(overview.value.aiFollowupQuestionCount || 0)
  + Number(overview.value.aiChatReplyCount || 0))
const displayAuditItems = computed(() => auditItems.value.filter(item => !highRiskOnly.value || isHighRiskAuditItem(item)))
const highRiskAuditCount = computed(() => auditItems.value.filter(item => isHighRiskAuditItem(item)).length)
const reviewedAuditCount = computed(() => auditItems.value.filter(item => item.doctorReview?.hasConclusion).length)
const reviewMismatchCount = computed(() => auditItems.value.filter(item => item.doctorReview?.isMismatch).length)
const queuePendingTakeoverCount = computed(() => reviewQueueItems.value.filter(item => !item.doctorHandle && !item.doctorConclusion).length)
const queuePendingConclusionCount = computed(() => reviewQueueItems.value.filter(item => !!item.doctorHandle && !item.doctorConclusion).length)
const queueMismatchCount = computed(() => reviewQueueItems.value.filter(item => item.doctorReview?.isMismatch).length)
const auditEmptyDescription = computed(() => {
  if (auditLoading.value) return '正在加载最近 AI 导诊输出。'
  if (auditKeyword.value.trim()) return '当前关键词和筛选条件下还没有最近 AI 导诊输出。'
  if (highRiskOnly.value) return '当前抽样范围内还没有命中高风险提示的 AI 输出。'
  return '当前筛选条件下还没有最近 AI 导诊输出。'
})
const queueEmptyDescription = computed(() => {
  if (queueLoading.value) return '正在加载高风险待复核队列。'
  if (reviewQueueKeyword.value.trim()) return '当前关键词下还没有命中的高风险待复核样本。'
  return '当前最近高风险 AI 输出已基本完成医生复核，暂无额外待处理样本。'
})

const runtimeModeSummary = computed(() => {
  if (overview.value.runtimeAvailable) {
    return '当前环境已具备 AI 导诊运行条件，患者提交问诊后可生成 AI 补充建议，并支持继续追问。'
  }
  if (!overview.value.triageEnabled) {
    return '当前总开关已关闭，系统会继续使用规则分诊和现有问诊链路。'
  }
  return '当前环境仍处于规则兜底模式，建议优先排查告警项。'
})

function createEmptyOverview() {
  return {
    providerName: 'DeepSeek',
    providerBaseUrl: '',
    modelName: '',
    temperature: null,
    maxTokens: null,
    promptVersion: '',
    doctorCandidateLimit: 0,
    triageEnabled: false,
    providerEnabled: false,
    apiKeyConfigured: false,
    modelBeanReady: false,
    runtimeAvailable: false,
    runtimeStatus: '',
    warnings: [],
    consultationCount: 0,
    triageSessionCount: 0,
    openSessionCount: 0,
    triageResultCount: 0,
    aiSummaryMessageCount: 0,
    aiFollowupQuestionCount: 0,
    aiChatReplyCount: 0,
    userFollowupMessageCount: 0,
    latestAiMessageTime: null
  }
}

function loadOverview() {
  loading.value = true
  get('/api/admin/consultation-ai/overview', data => {
    overview.value = {
      ...createEmptyOverview(),
      ...(data || {})
    }
    loading.value = false
  }, message => {
    loading.value = false
    ElMessage.warning(message || 'AI 导诊运行概览加载失败')
  })
}

function normalizeAuditItem(item) {
  const insight = resolveTriageMessageAuditInsight(item)
  return {
    ...item,
    insight,
    doctorReview: resolveDoctorReview(item, insight)
  }
}

function buildAuditQuery(includeLimit = true) {
  const query = [
    `messageType=${encodeURIComponent(auditMessageType.value)}`,
    `highRiskOnly=${highRiskOnly.value ? 'true' : 'false'}`
  ]
  if (auditKeyword.value.trim()) {
    query.push(`keyword=${encodeURIComponent(auditKeyword.value.trim())}`)
  }
  if (includeLimit) {
    query.push(`limit=${AUDIT_LIMIT}`)
  }
  return query.join('&')
}

function buildReviewQueueQuery(includeLimit = true) {
  const query = []
  if (reviewQueueKeyword.value.trim()) {
    query.push(`keyword=${encodeURIComponent(reviewQueueKeyword.value.trim())}`)
  }
  if (includeLimit) {
    query.push(`limit=${REVIEW_QUEUE_LIMIT}`)
  }
  return query.join('&')
}

function loadReviewQueue() {
  queueLoading.value = true
  get(`/api/admin/consultation-ai/high-risk-review-queue?${buildReviewQueueQuery(true)}`, data => {
    reviewQueueItems.value = (data || []).map(item => normalizeAuditItem(item))
    queueLoading.value = false
  }, message => {
    queueLoading.value = false
    ElMessage.warning(message || '高风险待复核队列加载失败')
  })
}

function loadAuditList() {
  auditLoading.value = true
  get(`/api/admin/consultation-ai/audit-list?${buildAuditQuery(true)}`, data => {
    auditItems.value = (data || []).map(item => normalizeAuditItem(item))
    auditLoading.value = false
  }, message => {
    auditLoading.value = false
    ElMessage.warning(message || '最近 AI 导诊输出加载失败')
  })
}

function resetAuditFilters() {
  auditKeyword.value = ''
  auditMessageType.value = 'all'
  highRiskOnly.value = false
  loadAuditList()
}

function resetReviewQueueFilters() {
  reviewQueueKeyword.value = ''
  loadReviewQueue()
}

function exportAuditList() {
  auditExporting.value = true
  download(`/api/admin/consultation-ai/audit-list/export?${buildAuditQuery(true)}`, 'consultation-ai-audit.csv', () => {
    auditExporting.value = false
    ElMessage.success('CSV download started')
  }, message => {
    auditExporting.value = false
    ElMessage.warning(message || 'AI audit export failed')
  }, error => {
    auditExporting.value = false
    console.error(error)
    ElMessage.error('AI audit export failed')
  })
}

function exportReviewQueue() {
  queueExporting.value = true
  download(`/api/admin/consultation-ai/high-risk-review-queue/export?${buildReviewQueueQuery(true)}`, 'consultation-ai-high-risk-review.csv', () => {
    queueExporting.value = false
    ElMessage.success('CSV download started')
  }, message => {
    queueExporting.value = false
    ElMessage.warning(message || 'High-risk queue export failed')
  }, error => {
    queueExporting.value = false
    console.error(error)
    ElMessage.error('High-risk queue export failed')
  })
}

function availabilityLabel(value, yesText = '已就绪', noText = '未就绪') {
  return value ? yesText : noText
}

function statusTagType(value) {
  return value ? 'success' : 'warning'
}

function statusTagLabel(value) {
  return value ? '正常' : '待处理'
}

function statusToneClass(value) {
  return value ? 'is-success' : 'is-warning'
}

function auditTypeLabel(value) {
  return ({
    ai_triage_summary: 'AI 导诊总结',
    ai_followup_questions: 'AI 追问建议',
    ai_chat_reply: 'AI 对话回复'
  })[value] || value || 'AI 输出'
}

function consultationStatusLabel(value) {
  return ({
    submitted: '已提交',
    triaged: '已分诊',
    processing: '处理中',
    completed: '已完成'
  })[value] || ''
}

function triageActionLabel(value) {
  return ({
    emergency: '立即急诊',
    offline: '尽快线下就医',
    followup: '复诊随访',
    online: '线上继续沟通'
  })[`${value || ''}`.toLowerCase()] || ''
}

function sourceLabel(value) {
  return ({
    deepseek: 'DeepSeek'
  })[`${value || ''}`.toLowerCase()] || (value || '')
}

function parseJsonArray(value) {
  if (!value) return []
  if (Array.isArray(value)) return value
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function doctorHandleStatusLabel(value) {
  return ({
    completed: '处理完成',
    processing: '处理中',
    received: '已接诊'
  })[`${value || ''}`] || (value ? '已处理' : '待医生处理')
}

function conditionLevelLabel(value) {
  return ({
    low: '轻度',
    medium: '中度',
    high: '较高风险',
    critical: '危急'
  })[`${value || ''}`] || ''
}

function dispositionLabel(value) {
  return ({
    observe: '继续观察',
    online_followup: '线上随访',
    offline_visit: '线下就医',
    emergency: '立即急诊'
  })[`${value || ''}`] || ''
}

function aiConsistencyLabel(value) {
  return value === 1 ? '医生标记与 AI 一致' : value === 0 ? '医生标记与 AI 不一致' : ''
}

function doctorFollowUpText(conclusion) {
  if (!conclusion) return ''
  return conclusion.needFollowUp === 1
    ? (conclusion.followUpWithinDays ? `${conclusion.followUpWithinDays} 天内随访` : '需要随访')
    : conclusion.needFollowUp === 0
      ? '暂不需要随访'
      : ''
}

function resolveAuditAiConditionLevel(insight, item) {
  const text = `${item?.triageLevelName || ''}`.trim()
  if (!text) return ''
  const upper = text.toUpperCase()
  if (upper.includes('EMERGENCY') || upper.includes('CRITICAL') || upper.includes('RED') || text.includes('危')) return 'critical'
  if (upper.includes('OFFLINE') || upper.includes('HIGH') || upper.includes('ORANGE') || text.includes('高')) return 'high'
  if (upper.includes('FOLLOWUP') || upper.includes('MEDIUM') || upper.includes('YELLOW') || text.includes('中')) return 'medium'
  if (upper.includes('ONLINE') || upper.includes('LOW') || upper.includes('GREEN') || text.includes('低') || text.includes('轻')) return 'low'
  return ''
}

function resolveAuditAiDisposition(insight, item) {
  const code = `${insight?.recommendedVisitTypeCode || item?.triageActionType || ''}`.toLowerCase()
  return ({
    emergency: 'emergency',
    offline: 'offline_visit',
    followup: 'online_followup',
    online: 'online_followup'
  })[code] || ''
}

function resolveAuditAiFollowUpText(insight, item) {
  const code = `${insight?.recommendedVisitTypeCode || item?.triageActionType || ''}`.toLowerCase()
  if (!code) return ''
  return code === 'followup' ? '3 天内随访' : '暂不需要随访'
}

function compareAuditField(aiValue, doctorValue) {
  if (!aiValue && !doctorValue) return 'pending'
  if (!aiValue || !doctorValue) return 'pending'
  return aiValue === doctorValue ? 'match' : 'mismatch'
}

function resolveAuditOverallStatus(conditionLevelStatus, dispositionStatus, followUpStatus) {
  const statuses = [conditionLevelStatus, dispositionStatus, followUpStatus]
  if (statuses.every(item => item === 'pending')) return 'pending'
  if (statuses.some(item => item === 'mismatch')) return 'mismatch'
  if (statuses.some(item => item === 'pending')) return 'partial'
  return 'match'
}

function resolveDoctorReview(item, insight) {
  const handle = item?.doctorHandle || null
  const conclusion = item?.doctorConclusion || null
  if (!handle && !conclusion) return null

  const aiConditionLevel = resolveAuditAiConditionLevel(insight, item)
  const aiDisposition = resolveAuditAiDisposition(insight, item)
  const aiFollowUp = resolveAuditAiFollowUpText(insight, item)
  const doctorConditionLevel = `${conclusion?.conditionLevel || ''}`.trim()
  const doctorDisposition = `${conclusion?.disposition || ''}`.trim()
  const doctorFollowUp = doctorFollowUpText(conclusion)
  const compareConditionLevelStatus = conclusion ? compareAuditField(aiConditionLevel, doctorConditionLevel) : ''
  const compareDispositionStatus = conclusion ? compareAuditField(aiDisposition, doctorDisposition) : ''
  const compareFollowUpStatus = conclusion ? compareAuditField(aiFollowUp, doctorFollowUp) : ''
  const compareOverallStatus = conclusion
    ? resolveAuditOverallStatus(compareConditionLevelStatus, compareDispositionStatus, compareFollowUpStatus)
    : ''
  const mismatchReasonLabels = parseJsonArray(conclusion?.aiMismatchReasonsJson)
    .map(item => aiMismatchReasonLabel(item))
    .filter(Boolean)

  const progressLabel = conclusion
    ? '已形成结论'
    : doctorHandleStatusLabel(handle?.status)
  const progressText = conclusion
    ? `${conclusion.doctorName || handle?.doctorName || '医生'}已提交最终结构化结论${formatDate(conclusion.updateTime || conclusion.createTime, true) !== '-' ? `，更新时间 ${formatDate(conclusion.updateTime || conclusion.createTime, true)}` : ''}。`
    : `${handle?.doctorName || '医生'}已接手当前问诊，当前状态为${doctorHandleStatusLabel(handle?.status)}${formatDate(handle?.updateTime || handle?.receiveTime, true) !== '-' ? `，最近时间 ${formatDate(handle?.updateTime || handle?.receiveTime, true)}` : ''}。`

  const doctorConclusionText = conclusion
    ? [
        conditionLevelLabel(conclusion.conditionLevel) ? `病情等级 ${conditionLevelLabel(conclusion.conditionLevel)}` : '',
        dispositionLabel(conclusion.disposition) ? `处理去向 ${dispositionLabel(conclusion.disposition)}` : '',
        doctorFollowUp ? `随访安排 ${doctorFollowUp}` : '',
        conclusion.diagnosisDirection ? `诊断方向 ${conclusion.diagnosisDirection}` : ''
      ].filter(Boolean).join('；')
    : ''

  const compareText = conclusion
    ? [
        `病情等级 ${comparisonStatusLabel(compareConditionLevelStatus)}`,
        `处理去向 ${comparisonStatusLabel(compareDispositionStatus)}`,
        `随访安排 ${comparisonStatusLabel(compareFollowUpStatus)}`
      ].join(' · ')
    : ''

  return {
    hasConclusion: !!conclusion,
    handleDoctorName: handle?.doctorName || conclusion?.doctorName || '',
    progressLabel,
    progressText,
    aiConsistencyLabel: aiConsistencyLabel(conclusion?.isConsistentWithAi),
    doctorConclusionText,
    compareText,
    compareOverallStatus,
    mismatchReasonLabels,
    mismatchRemark: `${conclusion?.aiMismatchRemark || ''}`.trim(),
    isMismatch: conclusion?.isConsistentWithAi === 0 || compareOverallStatus === 'mismatch'
  }
}

function isHighRiskAuditItem(item) {
  const insight = item?.insight
  if (!insight) return false
  return !!(
    insight.shouldEscalateToHuman === 1
    || insight.suggestOfflineImmediately === 1
    || insight.recommendedVisitTypeCode === 'emergency'
    || insight.recommendedVisitTypeCode === 'offline'
    || insight.riskFlags?.length
  )
}

function reviewQueueStatusLabel(item) {
  if (!item?.doctorHandle && !item?.doctorConclusion) return '待医生接手'
  if (!!item?.doctorHandle && !item?.doctorConclusion) return '待形成结论'
  if (item?.doctorReview?.isMismatch) return '医生判定有差异'
  return '待人工复核'
}

function reviewQueueTags(item) {
  const tags = []
  if (item?.doctorReview?.compareOverallStatus) tags.push(`对比：${comparisonStatusLabel(item.doctorReview.compareOverallStatus)}`)
  if (item?.doctorReview?.aiConsistencyLabel) tags.push(item.doctorReview.aiConsistencyLabel)
  if (item?.insight?.shouldEscalateToHuman === 1) tags.push('建议医生接管')
  if (item?.insight?.suggestOfflineImmediately === 1) tags.push('建议尽快线下')
  if (item?.insight?.recommendedVisitType) tags.push(`AI 建议：${item.insight.recommendedVisitType}`)
  return [...tags, ...(item?.doctorReview?.mismatchReasonLabels || [])].slice(0, 6)
}

function reviewQueueReasonText(item) {
  const reasons = []
  if (item?.insight?.riskFlags?.length) reasons.push(`风险标签 ${item.insight.riskFlags.join('、')}`)
  if (item?.insight?.shouldEscalateToHuman === 1) reasons.push('AI 建议尽快由医生接管')
  if (item?.insight?.suggestOfflineImmediately === 1) reasons.push('AI 提醒尽快线下就医')
  if (item?.insight?.recommendedVisitType) reasons.push(`建议方式 ${item.insight.recommendedVisitType}`)
  return reasons.length ? reasons.join('；') : '当前样本已命中高风险判定，请优先人工复核。'
}

function queueAiSummaryText(item) {
  const segments = []
  if (item?.insight?.summary) segments.push(item.insight.summary)
  if (item?.insight?.reply) segments.push(item.insight.reply)
  if (item?.insight?.recommendedDepartmentName) segments.push(`建议科室 ${item.insight.recommendedDepartmentName}`)
  if (item?.insight?.doctorRecommendationReason) segments.push(`推荐依据 ${item.insight.doctorRecommendationReason}`)
  if (item?.insight?.nextQuestions?.length) segments.push(`补充建议 ${item.insight.nextQuestions.join('；')}`)
  if (!segments.length && item?.content) segments.push(item.content)
  return abbreviateText(segments.join('；'), 220) || '当前高风险样本暂无更多 AI 说明。'
}

function reviewQueueProgressText(item) {
  if (!item?.doctorHandle && !item?.doctorConclusion) {
    return '当前还没有医生接手，建议优先进入问诊详情确认是否需要立即分派。'
  }
  if (!!item?.doctorHandle && !item?.doctorConclusion) {
    return item.doctorReview?.progressText || '医生已接手，但还没有形成最终结构化结论。'
  }
  if (item?.doctorReview?.isMismatch) {
    return item.doctorReview?.progressText || '医生已完成处理，并明确指出与 AI 建议存在差异。'
  }
  return item?.doctorReview?.progressText || '当前样本仍建议继续人工复核。'
}

function auditContextText(item) {
  const segments = []
  const status = consultationStatusLabel(item.consultationStatus)
  const triageAction = triageActionLabel(item.triageActionType)
  if (item.departmentName) segments.push(`科室 ${item.departmentName}`)
  if (item.triageLevelName) segments.push(`分诊等级 ${item.triageLevelName}`)
  if (status) segments.push(`问诊状态 ${status}`)
  if (triageAction) segments.push(`建议去向 ${triageAction}`)
  return segments.length ? segments.join(' · ') : '当前消息已写入导诊留痕，可继续到导诊记录中心查看完整上下文。'
}

function auditMetaTags(item) {
  const tags = []
  if (item.insight?.recommendedVisitType) tags.push(`建议方式：${item.insight.recommendedVisitType}`)
  if (item.insight?.recommendedDepartmentName) tags.push(`建议科室：${item.insight.recommendedDepartmentName}`)
  if (item.insight?.confidenceText) tags.push(`置信度：${item.insight.confidenceText}`)
  if (item.insight?.promptVersion) tags.push(`Prompt：${item.insight.promptVersion}`)
  if (item.insight?.source) tags.push(`来源：${sourceLabel(item.insight.source)}`)
  if (item.insight?.shouldEscalateToHuman === 1) tags.push('建议医生接管')
  if (item.insight?.suggestOfflineImmediately === 1) tags.push('建议尽快线下')
  return tags.slice(0, 6)
}

function showAuditReply(item) {
  return !!item?.insight?.reply
}

function showAuditContent(item) {
  return !item?.insight?.summary && !item?.insight?.reply && !(item?.insight?.nextQuestions?.length) && !!item?.content
}

function goToConsultationDetail(item) {
  if (!item?.consultationId) {
    ElMessage.warning('当前消息还没有关联问诊记录')
    return
  }
  router.push({
    name: 'admin-consultation-record',
    query: {
      detailId: `${item.consultationId}`,
      source: 'ai-audit'
    }
  })
}

function goToConsultationCenter(item) {
  const query = { source: 'ai-audit' }
  if (item?.consultationId) query.detailId = `${item.consultationId}`
  router.push({
    name: 'admin-consultation-record',
    query
  })
}

function abbreviateText(value, limit = 180) {
  const text = `${value || ''}`.trim()
  if (!text || text.length <= limit) return text
  return `${text.slice(0, limit)}...`
}

function formatDate(value, withTime = false) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', withTime
    ? { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
    : { year: 'numeric', month: '2-digit', day: '2-digit' }
  ).format(new Date(value))
}

onMounted(() => {
  loadOverview()
  loadReviewQueue()
  loadAuditList()
})
</script>

<style scoped>
.ai-config-page { display: flex; flex-direction: column; gap: 18px; }
.hero-card, .panel-card, .status-card, .metric-card, .hero-metric {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}
.hero-card {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.9fr);
  gap: 18px;
  padding: 26px;
}
.section-tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #27646d;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.hero-copy h2 { margin: 12px 0 8px; font-size: 30px; }
.hero-copy p, .panel-head p, .guideline-item p, .hero-metric p { margin: 0; color: var(--app-muted); line-height: 1.8; }
.chip-row, .hero-actions, .status-grid, .metric-grid, .content-grid { display: flex; gap: 12px; flex-wrap: wrap; }
.chip-row { margin-top: 16px; }
.chip-row span {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #27646d;
}
.hero-actions { margin-top: 18px; }
.hero-side {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.hero-metric { padding: 20px; }
.hero-metric span, .status-card span, .metric-card span { color: var(--app-muted); }
.hero-metric strong, .status-card strong, .metric-card strong {
  display: block;
  margin-top: 14px;
  font-size: 30px;
}
.hero-metric strong.is-success { color: #0f6665; }
.hero-metric strong.is-warning { color: #c77d25; }
.hero-metric-accent {
  border-color: rgba(15, 102, 101, 0.22);
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.08), rgba(255, 255, 255, 0.96));
}
.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}
.status-card { padding: 20px 22px; }
.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
}
.panel-card { padding: 24px; }
.panel-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 18px; }
.panel-head h3 { margin: 0; font-size: 22px; }
.detail-descriptions { margin-bottom: 18px; }
.env-board {
  padding: 16px 18px;
  border-radius: 22px;
  background: rgba(15, 102, 101, 0.04);
}
.env-board strong { display: block; margin-bottom: 10px; }
.env-board pre {
  margin: 0;
  font-family: Consolas, "Courier New", monospace;
  white-space: pre-wrap;
  line-height: 1.8;
  color: #31474d;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}
.metric-card { padding: 18px 20px; }
.metric-card-accent {
  border-color: rgba(15, 102, 101, 0.22);
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.07), rgba(255, 255, 255, 0.98));
}
.copy {
  margin: 0;
  line-height: 1.8;
  color: #41575d;
}
.copy + .copy { margin-top: 6px; }
.warning-list { display: flex; flex-direction: column; gap: 12px; }
.guideline-list { display: flex; flex-direction: column; gap: 14px; }
.guideline-item {
  padding: 18px;
  border-radius: 22px;
  background: rgba(15, 102, 101, 0.04);
  border: 1px solid rgba(15, 102, 101, 0.1);
}
.guideline-item strong { display: block; margin-bottom: 8px; color: #31474d; }
.audit-toolbar,
.audit-tag-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.audit-summary-bar,
.audit-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.queue-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.audit-review-head,
.audit-review-chips {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.audit-summary-bar {
  margin-bottom: 16px;
  color: var(--app-muted);
}
.audit-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.queue-item {
  min-height: 100%;
}
.audit-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(15, 102, 101, 0.04);
  border: 1px solid rgba(15, 102, 101, 0.1);
}
.audit-head-chips { margin: 0 0 2px; }
.audit-tag-row span {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.1);
  color: #27646d;
  font-size: 12px;
}
.audit-tag-row.danger span {
  background: rgba(214, 95, 80, 0.12);
  color: #9f4336;
}
.audit-review-card {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(19, 73, 80, 0.1);
  background: rgba(255, 255, 255, 0.68);
}
.audit-review-head {
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}
.audit-review-head strong {
  color: #31474d;
}
.audit-review-chips span {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #27646d;
  font-size: 12px;
}
.compare-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
}
.compare-badge.is-match {
  background: rgba(77, 168, 132, 0.16);
  color: #1f6f4f;
}
.compare-badge.is-mismatch {
  background: rgba(214, 95, 80, 0.14);
  color: #9f4336;
}
.compare-badge.is-partial,
.compare-badge.is-pending {
  background: rgba(210, 155, 47, 0.14);
  color: #8f6514;
}
.chip-danger {
  background: rgba(214, 95, 80, 0.12) !important;
  color: #9f4336 !important;
}
.audit-actions {
  justify-content: flex-end;
}
@media (max-width: 1100px) {
  .hero-card,
  .content-grid { grid-template-columns: 1fr; }
  .queue-list { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .hero-side { grid-template-columns: 1fr; }
  .panel-head { flex-direction: column; align-items: flex-start; }
  .audit-toolbar,
  .audit-actions { width: 100%; }
  .audit-review-head { flex-direction: column; }
}
</style>
