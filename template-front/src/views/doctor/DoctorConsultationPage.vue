<template>
  <div class="doctor-consultation-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>科室问诊总量</span>
        <strong>{{ records.length }}</strong>
      </article>
      <article class="stat-card">
        <span>今日新增</span>
        <strong>{{ todayCount }}</strong>
      </article>
      <article class="stat-card">
        <span>高优先级</span>
        <strong>{{ riskCount }}</strong>
      </article>
      <article class="stat-card">
        <span>已进入处理</span>
        <strong>{{ handledCount }}</strong>
      </article>
    </section>

    <section class="panel-card">
      <div class="panel-head">
        <div>
          <h3>科室问诊列表</h3>
          <p>当前按医生所属科室展示问诊记录，医生可以查看 AI 分诊结果并录入处理意见。</p>
        </div>
        <div class="toolbar">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索就诊人、分类、主诉或状态"
            style="width: 320px"
          />
          <el-button @click="loadRecords()">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredRecords" v-loading="loading" border>
        <el-table-column prop="patientName" label="就诊人" min-width="100" />
        <el-table-column prop="categoryName" label="问诊分类" min-width="120" />
        <el-table-column prop="chiefComplaint" label="主诉" min-width="260" show-overflow-tooltip />
        <el-table-column label="分诊等级" min-width="120">
          <template #default="{ row }">
            {{ row.triageLevelName || '待评估' }}
          </template>
        </el-table-column>
        <el-table-column label="建议动作" min-width="120">
          <template #default="{ row }">
            {{ triageActionLabel(row.triageActionType) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="170">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !records.length" description="当前科室暂无问诊记录" />
    </section>

    <el-drawer
      v-model="detailVisible"
      title="问诊详情"
      size="66%"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detailRecord">
          <section class="detail-card">
            <h3>基本信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="问诊单号">{{ detailRecord.consultationNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="就诊人">{{ detailRecord.patientName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="问诊分类">{{ detailRecord.categoryName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所属科室">{{ detailRecord.departmentName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="分诊等级">{{ detailRecord.triageLevelName || '待评估' }}</el-descriptions-item>
              <el-descriptions-item label="建议动作">{{ triageActionLabel(detailRecord.triageActionType) }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ statusLabel(detailRecord.status) }}</el-descriptions-item>
              <el-descriptions-item label="提交时间">{{ formatDate(detailRecord.createTime) }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="detail-card">
            <h3>主诉与健康摘要</h3>
            <p class="detail-text">{{ detailRecord.chiefComplaint || '暂无主诉信息' }}</p>
            <p class="detail-text">{{ detailRecord.healthSummary || '暂无健康摘要' }}</p>
          </section>

          <section class="detail-card">
            <h3>问诊答案</h3>
            <div v-if="detailRecord.answers?.length" class="answer-list">
              <article
                v-for="item in detailRecord.answers"
                :key="`${item.fieldCode}-${item.id || item.fieldLabel}`"
                class="answer-item"
              >
                <strong>{{ item.fieldLabel }}</strong>
                <div class="answer-value">
                  <template v-if="item.fieldType === 'upload' && item.fieldValue">
                    <img :src="resolveImagePath(item.fieldValue)" :alt="item.fieldLabel" class="answer-image" />
                  </template>
                  <template v-else-if="item.fieldType === 'multi_select'">
                    <div class="chip-row">
                      <span v-for="tag in parseJsonArray(item.fieldValue)" :key="tag">{{ tag }}</span>
                    </div>
                  </template>
                  <template v-else>
                    {{ displayAnswer(item) }}
                  </template>
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无问诊答案" />
          </section>

          <section class="detail-card">
            <div class="section-head">
              <div>
                <h3>医生处理</h3>
                <p>在这里记录当前问诊单的判断摘要、处理建议和随访安排。</p>
              </div>
              <div v-if="detailRecord.doctorHandle" class="chip-row">
                <span>{{ handleStatusLabel(detailRecord.doctorHandle.status) }}</span>
                <span>接手 {{ formatDate(detailRecord.doctorHandle.receiveTime) }}</span>
                <span v-if="detailRecord.doctorHandle.completeTime">完成 {{ formatDate(detailRecord.doctorHandle.completeTime) }}</span>
              </div>
            </div>

            <div v-if="detailRecord.doctorHandle" class="handle-summary">
              <article>
                <span>当前处理医生</span>
                <strong>{{ detailRecord.doctorHandle.doctorName || '-' }}</strong>
              </article>
              <article>
                <span>最后更新时间</span>
                <strong>{{ formatDate(detailRecord.doctorHandle.updateTime) }}</strong>
              </article>
            </div>

            <el-form label-position="top" class="handle-form">
              <el-form-item label="医生判断摘要">
                <el-input
                  v-model="handleForm.summary"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                  placeholder="例如：当前为轻中度上呼吸道症状，暂无紧急风险，建议继续线上处理并观察体温变化。"
                />
              </el-form-item>
              <el-form-item label="处理建议">
                <el-input
                  v-model="handleForm.medicalAdvice"
                  type="textarea"
                  :rows="5"
                  maxlength="4000"
                  show-word-limit
                  placeholder="可填写生活方式建议、复诊建议、注意事项等。完成处理时建议填写完整。"
                />
              </el-form-item>
              <el-form-item label="随访计划">
                <el-input
                  v-model="handleForm.followUpPlan"
                  maxlength="500"
                  show-word-limit
                  placeholder="例如：建议 3 天后复诊，如持续高热或加重请线下就医。"
                />
              </el-form-item>
              <el-form-item label="内部备注">
                <el-input
                  v-model="handleForm.internalRemark"
                  type="textarea"
                  :rows="2"
                  maxlength="500"
                  show-word-limit
                  placeholder="仅医生和管理员可见，用于内部协作或风险备注。"
                />
              </el-form-item>
            </el-form>

            <div class="handle-actions">
              <el-button
                type="warning"
                plain
                :loading="handleSubmitting && submittingStatus === 'processing'"
                :disabled="detailRecord.status === 'completed'"
                @click="submitHandle('processing')"
              >
                标记处理中
              </el-button>
              <el-button
                type="primary"
                :loading="handleSubmitting && submittingStatus === 'completed'"
                @click="submitHandle('completed')"
              >
                完成处理
              </el-button>
            </div>
          </section>

          <section class="detail-card">
            <h3>AI 分诊结果</h3>
            <div v-if="detailRecord.triageResult" class="triage-panel">
              <div class="chip-row">
                <span>{{ detailRecord.triageResult.triageLevelName || '待评估' }}</span>
                <span>{{ detailRecord.triageResult.departmentName || '未匹配科室' }}</span>
                <span v-if="detailRecord.triageResult.doctorName">推荐医生：{{ detailRecord.triageResult.doctorName }}</span>
              </div>
              <p class="detail-text">{{ detailRecord.triageResult.reasonText || '暂无分诊说明' }}</p>
              <div v-if="doctorCandidates.length" class="chip-row">
                <span v-for="item in doctorCandidates" :key="`${item.id}-${item.name}`">
                  {{ item.name }}<template v-if="item.title"> / {{ item.title }}</template>
                </span>
              </div>
            </div>
            <el-empty v-else description="暂无 AI 分诊结果" />
          </section>

          <section class="detail-card">
            <h3>规则命中</h3>
            <el-table v-if="detailRecord.ruleHits?.length" :data="detailRecord.ruleHits" border>
              <el-table-column prop="ruleName" label="规则名称" min-width="180" />
              <el-table-column prop="matchedSummary" label="命中摘要" min-width="220" show-overflow-tooltip />
              <el-table-column prop="triageLevelName" label="等级" min-width="100" />
              <el-table-column prop="actionType" label="动作" min-width="110">
                <template #default="{ row }">
                  {{ triageActionLabel(row.actionType) }}
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无规则命中记录" />
          </section>

          <section class="detail-card">
            <h3>用户反馈</h3>
            <div v-if="detailRecord.triageFeedback" class="triage-panel">
              <div class="chip-row">
                <span>评分：{{ detailRecord.triageFeedback.userScore ?? '-' }}</span>
                <span>是否采纳：{{ detailRecord.triageFeedback.isAdopted === 1 ? '已采纳' : '未采纳' }}</span>
                <span v-if="detailRecord.triageFeedback.manualCorrectDepartmentName">
                  修正科室：{{ detailRecord.triageFeedback.manualCorrectDepartmentName }}
                </span>
                <span v-if="detailRecord.triageFeedback.manualCorrectDoctorName">
                  修正医生：{{ detailRecord.triageFeedback.manualCorrectDoctorName }}
                </span>
              </div>
              <p class="detail-text">{{ detailRecord.triageFeedback.feedbackText || '用户未填写补充反馈' }}</p>
            </div>
            <el-empty v-else description="用户暂未提交反馈" />
          </section>

          <section class="detail-card">
            <h3>分诊轨迹</h3>
            <div v-if="detailRecord.triageSession?.messages?.length" class="message-list">
              <article v-for="item in detailRecord.triageSession.messages" :key="item.id" class="message-item">
                <div class="message-head">
                  <strong>{{ item.title || item.messageType || '系统消息' }}</strong>
                  <span>{{ item.roleType || '-' }}</span>
                </div>
                <p>{{ item.content || '-' }}</p>
              </article>
            </div>
            <el-empty v-else description="暂无分诊轨迹消息" />
          </section>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, post, resolveImagePath } from '@/net'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const handleSubmitting = ref(false)
const submittingStatus = ref('')
const keyword = ref('')
const records = ref([])
const detailRecord = ref(null)
const handleForm = reactive({
  summary: '',
  medicalAdvice: '',
  followUpPlan: '',
  internalRemark: ''
})

const filteredRecords = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return records.value
  return records.value.filter(item => [
    item.patientName,
    item.categoryName,
    item.chiefComplaint,
    item.status
  ].filter(Boolean).some(text => String(text).toLowerCase().includes(value)))
})

const todayCount = computed(() => {
  const today = new Date().toDateString()
  return records.value.filter(item => item.createTime && new Date(item.createTime).toDateString() === today).length
})

const riskCount = computed(() => records.value.filter(item => ['emergency', 'offline'].includes(item.triageActionType)).length)
const handledCount = computed(() => records.value.filter(item => ['processing', 'completed'].includes(item.status)).length)

const doctorCandidates = computed(() => {
  const source = detailRecord.value?.triageResult?.doctorCandidatesJson
  return parseDoctorCandidates(source)
})

function loadRecords(callback) {
  loading.value = true
  get('/api/doctor/consultation/list', (data) => {
    records.value = data || []
    loading.value = false
    callback?.()
    autoOpenFromQuery()
  }, (message) => {
    loading.value = false
    ElMessage.warning(message || '医生问诊列表加载失败，请稍后再试')
  })
}

function autoOpenFromQuery() {
  const queryId = Number(route.query.id || 0)
  if (!queryId) return
  const exists = records.value.some(item => item.id === queryId)
  if (exists && (!detailRecord.value || detailRecord.value.id !== queryId)) {
    openDetail(queryId)
  }
}

function openDetail(id) {
  detailLoading.value = true
  detailVisible.value = true
  get(`/api/doctor/consultation/detail?id=${id}`, (data) => {
    detailRecord.value = data || null
    syncHandleForm()
    detailLoading.value = false
    if (Number(route.query.id || 0) !== id) {
      router.replace({
        path: '/doctor/consultation',
        query: { id }
      })
    }
  }, (message) => {
    detailLoading.value = false
    detailVisible.value = false
    ElMessage.warning(message || '问诊详情加载失败，请稍后再试')
  })
}

function syncHandleForm() {
  const handle = detailRecord.value?.doctorHandle
  handleForm.summary = handle?.summary || ''
  handleForm.medicalAdvice = handle?.medicalAdvice || ''
  handleForm.followUpPlan = handle?.followUpPlan || ''
  handleForm.internalRemark = handle?.internalRemark || ''
}

function submitHandle(status) {
  if (!detailRecord.value?.id) return
  if (!`${handleForm.summary || ''}`.trim()) {
    ElMessage.warning('请先填写医生判断摘要')
    return
  }
  if (status === 'completed' && !`${handleForm.medicalAdvice || ''}`.trim()) {
    ElMessage.warning('完成处理时请填写处理建议')
    return
  }

  handleSubmitting.value = true
  submittingStatus.value = status
  post('/api/doctor/consultation/handle', {
    consultationId: detailRecord.value.id,
    status,
    summary: `${handleForm.summary || ''}`.trim(),
    medicalAdvice: `${handleForm.medicalAdvice || ''}`.trim(),
    followUpPlan: `${handleForm.followUpPlan || ''}`.trim(),
    internalRemark: `${handleForm.internalRemark || ''}`.trim()
  }, () => {
    handleSubmitting.value = false
    submittingStatus.value = ''
    ElMessage.success(status === 'completed' ? '医生处理结果已完成保存' : '已标记为处理中')
    loadRecords(() => openDetail(detailRecord.value.id))
  }, (message) => {
    handleSubmitting.value = false
    submittingStatus.value = ''
    ElMessage.warning(message || '医生处理结果保存失败，请稍后再试')
  })
}

function parseJsonArray(value) {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function parseDoctorCandidates(value) {
  return parseJsonArray(value).filter(item => item && typeof item === 'object')
}

function displayAnswer(answer) {
  if (answer.fieldType === 'switch') return answer.fieldValue === '1' ? '是' : '否'
  return answer.fieldValue || '-'
}

function statusLabel(value) {
  if (value === 'submitted') return '已提交'
  if (value === 'triaged') return '已分诊'
  if (value === 'processing') return '处理中'
  if (value === 'completed') return '已完成'
  return value || '未标记'
}

function handleStatusLabel(value) {
  return value === 'completed' ? '处理完成' : '处理中'
}

function statusTagType(value) {
  if (value === 'completed') return 'success'
  if (value === 'processing') return 'warning'
  if (value === 'triaged') return 'primary'
  return 'info'
}

function triageActionLabel(value) {
  if (value === 'emergency') return '立即急诊'
  if (value === 'offline') return '尽快线下就医'
  if (value === 'followup') return '复诊随访'
  if (value === 'online') return '线上继续'
  return '继续关注'
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

watch(detailVisible, (value) => {
  if (!value) {
    detailRecord.value = null
    syncHandleForm()
    if (route.query.id) {
      router.replace({ path: '/doctor/consultation' })
    }
  }
})

onMounted(() => loadRecords())
</script>

<style scoped>
.doctor-consultation-page {
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
.panel-card,
.detail-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.stat-card {
  padding: 22px 24px;
}

.stat-card span,
.section-head p {
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

.panel-head h3,
.section-head h3 {
  margin: 0;
  font-size: 22px;
}

.panel-head p {
  margin: 8px 0 0;
  color: var(--app-muted);
  line-height: 1.7;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-card {
  padding: 20px;
}

.detail-card h3 {
  margin: 0 0 16px;
  font-size: 20px;
}

.detail-text {
  margin: 0;
  line-height: 1.8;
  color: #41575d;
}

.detail-text + .detail-text {
  margin-top: 10px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.section-head p {
  margin: 6px 0 0;
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

.answer-list,
.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.answer-item,
.message-item,
.triage-panel,
.handle-summary article {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(19, 73, 80, 0.05);
}

.answer-item strong,
.message-head strong {
  display: block;
  margin-bottom: 8px;
}

.answer-value,
.message-item p {
  margin: 0;
  color: #41575d;
  line-height: 1.7;
}

.answer-image {
  width: 220px;
  height: 150px;
  border-radius: 18px;
  object-fit: cover;
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.message-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.message-head span {
  color: var(--app-muted);
  font-size: 13px;
}

.handle-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.handle-summary span {
  display: block;
  color: var(--app-muted);
  font-size: 13px;
}

.handle-summary strong {
  display: block;
  margin-top: 8px;
}

.handle-form {
  margin-top: 8px;
}

.handle-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 1100px) {
  .stat-grid,
  .handle-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .stat-grid,
  .handle-summary {
    grid-template-columns: 1fr;
  }

  .panel-head,
  .toolbar,
  .message-head,
  .section-head,
  .handle-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
