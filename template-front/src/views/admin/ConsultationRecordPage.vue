<template>
  <div class="consultation-record-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>问诊记录</span>
        <strong>{{ records.length }}</strong>
      </article>
      <article class="stat-card">
        <span>AI 已分诊</span>
        <strong>{{ triagedCount }}</strong>
      </article>
      <article class="stat-card">
        <span>医生已处理</span>
        <strong>{{ completedCount }}</strong>
      </article>
      <article class="stat-card">
        <span>今日新增</span>
        <strong>{{ todayCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索记录编号、就诊人、标题或主诉"
            style="max-width: 320px"
          />
          <el-select v-model="selectedCategory" clearable style="width: 180px" placeholder="全部分类">
            <el-option
              v-for="item in categoryOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
          <el-select v-model="selectedTriageLevel" clearable style="width: 180px" placeholder="全部分诊等级">
            <el-option
              v-for="item in triageLevelOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
          <el-select v-model="selectedStatus" clearable style="width: 180px" placeholder="全部状态">
            <el-option label="已提交" value="submitted" />
            <el-option label="已分诊" value="triaged" />
            <el-option label="处理中" value="processing" />
            <el-option label="已完成" value="completed" />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredRecords" v-loading="loading" border>
        <el-table-column prop="consultationNo" label="记录编号" min-width="170" />
        <el-table-column prop="patientName" label="就诊人" min-width="120" />
        <el-table-column prop="categoryName" label="问诊分类" min-width="140" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="初步分诊" min-width="150" align="center">
          <template #default="{ row }">
            <span class="triage-badge" :style="triageBadgeStyle(row.triageLevelColor)">
              {{ row.triageLevelName || '待评估' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130" align="center">
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
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="detailVisible"
      title="问诊记录详情"
      width="1080px"
      destroy-on-close
    >
      <div v-loading="detailLoading">
        <template v-if="detailRecord">
          <div class="detail-meta">
            <article><span>记录编号</span><strong>{{ detailRecord.consultationNo }}</strong></article>
            <article><span>就诊人</span><strong>{{ detailRecord.patientName }}</strong></article>
            <article><span>问诊分类</span><strong>{{ detailRecord.categoryName }}</strong></article>
            <article><span>推荐科室</span><strong>{{ detailRecord.departmentName || '待分配' }}</strong></article>
            <article>
              <span>初步分诊</span>
              <strong>
                <span class="triage-badge" :style="triageBadgeStyle(detailRecord.triageLevelColor)">
                  {{ detailRecord.triageLevelName || '待评估' }}
                </span>
              </strong>
            </article>
            <article><span>当前状态</span><strong>{{ statusLabel(detailRecord.status) }}</strong></article>
          </div>

          <div class="summary-panel">
            <p><strong>标题：</strong>{{ detailRecord.title }}</p>
            <p><strong>主诉摘要：</strong>{{ detailRecord.chiefComplaint || '未自动提取' }}</p>
            <p><strong>健康摘要：</strong>{{ detailRecord.healthSummary || '未关联健康档案摘要' }}</p>
            <p><strong>系统建议：</strong>{{ detailRecord.triageSuggestion || '当前暂无额外建议' }}</p>
            <p><strong>规则摘要：</strong>{{ detailRecord.triageRuleSummary || '未命中红旗规则，当前为默认分诊结果' }}</p>
          </div>

          <section v-if="detailRecord.doctorHandle" class="detail-section">
            <div class="section-head">
              <h3>医生处理归档</h3>
              <span>查看医生接手情况、处理建议和内部备注。</span>
            </div>
            <div class="session-meta">
              <span>{{ detailRecord.doctorHandle.doctorName || '未指派医生' }}</span>
              <span>{{ handleStatusLabel(detailRecord.doctorHandle.status) }}</span>
              <span>接手 {{ formatDate(detailRecord.doctorHandle.receiveTime) }}</span>
              <span v-if="detailRecord.doctorHandle.completeTime">完成 {{ formatDate(detailRecord.doctorHandle.completeTime) }}</span>
            </div>
            <div class="summary-panel">
              <p><strong>判断摘要：</strong>{{ detailRecord.doctorHandle.summary || '暂无摘要' }}</p>
              <p><strong>处理建议：</strong>{{ detailRecord.doctorHandle.medicalAdvice || '暂无处理建议' }}</p>
              <p><strong>随访计划：</strong>{{ detailRecord.doctorHandle.followUpPlan || '暂无随访安排' }}</p>
              <p><strong>内部备注：</strong>{{ detailRecord.doctorHandle.internalRemark || '暂无内部备注' }}</p>
            </div>
          </section>

          <section v-if="detailRecord.triageResult" class="detail-section">
            <div class="section-head">
              <h3>分诊结果归档</h3>
              <span>查看本次导诊最终归档结果、候选医生与风险摘要。</span>
            </div>
            <div class="session-meta">
              <span>{{ detailRecord.triageResult.triageLevelName || '待评估' }}</span>
              <span>{{ detailRecord.triageResult.departmentName || '未匹配科室' }}</span>
              <span>置信度 {{ formatConfidence(detailRecord.triageResult.confidenceScore) }}</span>
              <span v-if="detailRecord.triageResult.doctorName">推荐医生 {{ detailRecord.triageResult.doctorName }}</span>
            </div>
            <div class="summary-panel">
              <p><strong>结果说明：</strong>{{ detailRecord.triageResult.reasonText || '暂无结果说明' }}</p>
              <p><strong>风险摘要：</strong>{{ parseJsonArray(detailRecord.triageResult.riskFlagsJson).join('、') || '暂无风险标签' }}</p>
            </div>
            <div v-if="parseDoctorCandidates(detailRecord.triageResult.doctorCandidatesJson).length" class="answer-list">
              <article
                v-for="item in parseDoctorCandidates(detailRecord.triageResult.doctorCandidatesJson)"
                :key="item.id || item.name"
                class="answer-card"
              >
                <strong>{{ item.name }}</strong>
                <div class="answer-value">{{ item.title || '医生' }}</div>
              </article>
            </div>
          </section>

          <section v-if="detailRecord.triageFeedback" class="detail-section">
            <div class="section-head">
              <h3>用户反馈</h3>
              <span>查看用户对本次导诊的采纳情况和人工纠偏信息。</span>
            </div>
            <div class="session-meta">
              <span>{{ feedbackAdoptLabel(detailRecord.triageFeedback.isAdopted) }}</span>
              <span>评分 {{ detailRecord.triageFeedback.userScore }}/5</span>
              <span v-if="detailRecord.triageFeedback.manualCorrectDepartmentName">修正科室 {{ detailRecord.triageFeedback.manualCorrectDepartmentName }}</span>
              <span v-if="detailRecord.triageFeedback.manualCorrectDoctorName">修正医生 {{ detailRecord.triageFeedback.manualCorrectDoctorName }}</span>
            </div>
            <div class="summary-panel">
              <p><strong>反馈内容：</strong>{{ detailRecord.triageFeedback.feedbackText || '用户未填写额外说明' }}</p>
              <p><strong>反馈时间：</strong>{{ formatDate(detailRecord.triageFeedback.updateTime || detailRecord.triageFeedback.createTime) }}</p>
            </div>
          </section>

          <section v-if="detailRecord.triageSession" class="detail-section">
            <div class="section-head">
              <h3>导诊留痕</h3>
              <span>查看本次分诊会话的系统摘要与规则命中消息。</span>
            </div>
            <div class="session-meta">
              <span>Session {{ detailRecord.triageSession.sessionNo }}</span>
              <span>{{ triageSessionStatusLabel(detailRecord.triageSession.status) }}</span>
              <span>{{ detailRecord.triageSession.messageCount || 0 }} messages</span>
            </div>
            <div class="session-list">
              <article
                v-for="message in detailRecord.triageSession.messages || []"
                :key="message.id"
                class="session-card"
              >
                <div class="session-card-head">
                  <div>
                    <strong>{{ message.title }}</strong>
                    <span>{{ messageTypeLabel(message.messageType) }}</span>
                  </div>
                  <el-tag size="small" effect="light">{{ messageRoleLabel(message.roleType) }}</el-tag>
                </div>
                <p>{{ message.content }}</p>
              </article>
            </div>
          </section>

          <section class="detail-section">
            <div class="section-head">
              <h3>规则命中日志</h3>
              <span>用于复盘本次初步分诊的依据。</span>
            </div>
            <el-table
              v-if="(detailRecord.ruleHits || []).length"
              :data="detailRecord.ruleHits"
              border
            >
              <el-table-column label="主规则" width="90" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.isPrimary === 1" type="danger" effect="light">主规则</el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="ruleName" label="规则名称" min-width="170" />
              <el-table-column prop="ruleCode" label="规则编码" min-width="150" />
              <el-table-column label="触发方式" min-width="120" align="center">
                <template #default="{ row }">
                  <el-tag effect="light">{{ triggerTypeLabel(row.triggerType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="triageLevelName" label="规则等级" min-width="120" />
              <el-table-column prop="matchedSummary" label="命中依据" min-width="240" show-overflow-tooltip />
              <el-table-column prop="suggestion" label="规则建议" min-width="220" show-overflow-tooltip />
            </el-table>
            <el-empty v-else description="当前记录未命中红旗规则，使用的是默认分诊策略" />
          </section>

          <section class="detail-section">
            <div class="section-head">
              <h3>问诊答案</h3>
              <span>展示用户提交的结构化问诊资料。</span>
            </div>
            <div class="answer-list">
              <article v-for="answer in detailRecord.answers || []" :key="answer.id" class="answer-card">
                <strong>{{ answer.fieldLabel }}</strong>
                <div class="answer-value">
                  <template v-if="answer.fieldType === 'upload' && answer.fieldValue">
                    <img :src="resolveImagePath(answer.fieldValue)" :alt="answer.fieldLabel" />
                  </template>
                  <template v-else-if="answer.fieldType === 'multi_select'">
                    <div class="chip-row">
                      <span v-for="item in parseMultiValue(answer.fieldValue)" :key="item">{{ item }}</span>
                    </div>
                  </template>
                  <template v-else>
                    {{ displayAnswer(answer) }}
                  </template>
                </div>
              </article>
            </div>
          </section>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { get, resolveImagePath } from '@/net'

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const records = ref([])
const detailRecord = ref(null)
const keyword = ref('')
const selectedCategory = ref('')
const selectedTriageLevel = ref('')
const selectedStatus = ref('')

const triagedCount = computed(() => records.value.filter(item => ['triaged', 'processing', 'completed'].includes(item.status)).length)
const completedCount = computed(() => records.value.filter(item => item.status === 'completed').length)
const todayCount = computed(() => {
  const today = new Date().toDateString()
  return records.value.filter(item => item.createTime && new Date(item.createTime).toDateString() === today).length
})
const categoryOptions = computed(() => [...new Set(records.value.map(item => item.categoryName).filter(Boolean))])
const triageLevelOptions = computed(() => [...new Set(records.value.map(item => item.triageLevelName).filter(Boolean))])
const filteredRecords = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return records.value.filter(item => {
    const matchesKeyword = !search || [
      item.consultationNo,
      item.patientName,
      item.title,
      item.chiefComplaint
    ].filter(Boolean).some(text => `${text}`.toLowerCase().includes(search))
    const matchesCategory = !selectedCategory.value || item.categoryName === selectedCategory.value
    const matchesTriage = !selectedTriageLevel.value || item.triageLevelName === selectedTriageLevel.value
    const matchesStatus = !selectedStatus.value || item.status === selectedStatus.value
    return matchesKeyword && matchesCategory && matchesTriage && matchesStatus
  })
})

function loadData() {
  loading.value = true
  get('/api/admin/consultation-record/list', (data) => {
    records.value = data || []
    loading.value = false
  }, (message) => {
    loading.value = false
    ElMessage.warning(message || '问诊记录加载失败')
  })
}

function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detailRecord.value = null
  get(`/api/admin/consultation-record/detail?id=${row.id}`, (data) => {
    detailRecord.value = data
    detailLoading.value = false
  }, (message) => {
    detailLoading.value = false
    detailVisible.value = false
    ElMessage.warning(message || '问诊记录详情加载失败')
  })
}

function statusLabel(value) {
  return {
    submitted: '已提交',
    triaged: '已分诊',
    processing: '处理中',
    completed: '已完成'
  }[value] || value || '-'
}

function handleStatusLabel(value) {
  return value === 'completed' ? '处理完成' : '处理中'
}

function statusTagType(value) {
  return {
    completed: 'success',
    processing: 'warning',
    triaged: 'primary',
    submitted: 'info'
  }[value] || 'info'
}

function triggerTypeLabel(value) {
  return {
    symptom_match: '症状匹配',
    keyword_match: '关键词匹配',
    body_part_match: '部位匹配',
    combination: '组合规则'
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

function triageSessionStatusLabel(value) {
  return {
    completed: '已完成',
    in_progress: '进行中',
    closed: '已关闭'
  }[value] || value || '-'
}

function messageRoleLabel(value) {
  return {
    user: '用户',
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
    rule_hit: '命中详情'
  }[value] || value || '-'
}

function feedbackAdoptLabel(value) {
  return value === 1 ? '已采纳' : '未采纳'
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

function formatConfidence(value) {
  const number = Number(value)
  if (Number.isNaN(number) || number <= 0) return '-'
  return `${Math.round(number * 100)}%`
}

function parseMultiValue(value) {
  return parseJsonArray(value)
}

function displayAnswer(answer) {
  if (answer.fieldType === 'switch') return answer.fieldValue === '1' ? '是' : '否'
  return answer.fieldValue || '-'
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

onMounted(() => loadData())
</script>

<style scoped>
.consultation-record-page {
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
.table-card,
.summary-panel,
.answer-card,
.detail-meta article {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.stat-card,
.table-card {
  padding: 22px;
}

.stat-card span,
.section-head span,
.detail-meta span {
  color: var(--app-muted);
}

.stat-card strong {
  display: block;
  margin-top: 14px;
  font-size: 30px;
}

.toolbar,
.toolbar-filters,
.toolbar-actions,
.section-head {
  display: flex;
  gap: 12px;
}

.toolbar,
.section-head {
  justify-content: space-between;
  align-items: flex-start;
}

.toolbar {
  margin-bottom: 18px;
}

.toolbar-filters {
  flex-wrap: wrap;
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.detail-meta article,
.summary-panel,
.answer-card,
.session-card {
  padding: 18px;
}

.detail-meta strong {
  display: block;
  margin-top: 10px;
}

.summary-panel {
  margin: 18px 0;
}

.summary-panel p {
  margin: 0 0 10px;
  line-height: 1.8;
  color: #405f68;
}

.summary-panel p:last-child {
  margin-bottom: 0;
}

.detail-section + .detail-section {
  margin-top: 20px;
}

.section-head {
  margin-bottom: 14px;
}

.section-head h3 {
  margin: 0;
}

.answer-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.session-meta,
.session-card-head {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.session-meta {
  margin-bottom: 14px;
}

.session-meta span {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #48656d;
  font-size: 12px;
}

.session-list {
  display: grid;
  gap: 14px;
}

.session-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.session-card-head {
  justify-content: space-between;
  align-items: flex-start;
}

.session-card-head strong {
  display: block;
}

.session-card-head span,
.session-card p {
  color: var(--app-muted);
}

.session-card p {
  margin: 12px 0 0;
  line-height: 1.8;
}

.answer-card strong {
  display: block;
}

.answer-value {
  margin-top: 10px;
  color: #405f68;
  line-height: 1.8;
}

.answer-value img {
  width: 220px;
  height: 150px;
  border-radius: 18px;
  object-fit: cover;
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip-row span {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #48656d;
  font-size: 12px;
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

@media (max-width: 1180px) {
  .stat-grid,
  .detail-meta,
  .answer-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .toolbar,
  .section-head,
  .session-card-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
