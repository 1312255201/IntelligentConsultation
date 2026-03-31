<template>
  <div class="record-page">
    <section class="stats">
      <article class="card stat"><span>问诊记录</span><strong>{{ records.length }}</strong></article>
      <article class="card stat"><span>已认领</span><strong>{{ assignedCount }}</strong></article>
      <article class="card stat"><span>医生已完成</span><strong>{{ completedCount }}</strong></article>
      <article class="card stat"><span>今日新增</span><strong>{{ todayCount }}</strong></article>
    </section>

    <section class="card block">
      <div class="head">
        <div class="toolbar">
          <el-input v-model="keyword" clearable placeholder="搜索单号、就诊人、标题或主诉" style="width:260px" />
          <el-select v-model="categoryFilter" clearable placeholder="全部分类" style="width:160px">
            <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="triageFilter" clearable placeholder="全部分诊等级" style="width:160px">
            <el-option v-for="item in triageOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="statusFilter" clearable placeholder="全部状态" style="width:140px">
            <el-option label="已提交" value="submitted" />
            <el-option label="已分诊" value="triaged" />
            <el-option label="处理中" value="processing" />
            <el-option label="已完成" value="completed" />
          </el-select>
          <el-select v-model="assignmentFilter" clearable placeholder="全部认领状态" style="width:160px">
            <el-option label="待认领" value="unclaimed" />
            <el-option label="已认领" value="claimed" />
            <el-option label="已释放" value="released" />
          </el-select>
        </div>
        <el-button @click="loadData">刷新</el-button>
      </div>

      <el-table :data="filteredRecords" v-loading="loading" border>
        <el-table-column prop="consultationNo" label="问诊单号" min-width="170" />
        <el-table-column prop="patientName" label="就诊人" min-width="110" />
        <el-table-column prop="categoryName" label="问诊分类" min-width="130" />
        <el-table-column label="初步分诊" min-width="140" align="center">
          <template #default="{ row }">
            <span class="badge" :style="triageBadgeStyle(row.triageLevelColor)">{{ row.triageLevelName || '待评估' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="认领状态" min-width="180">
          <template #default="{ row }">
            <div class="assignment">
              <el-tag :type="assignmentTagType(row.doctorAssignment)" effect="light">{{ assignmentStatusLabel(row.doctorAssignment) }}</el-tag>
              <span v-if="row.doctorAssignment?.doctorName">{{ row.doctorAssignment.doctorName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }"><el-tag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="160">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="openDetail(row.id)">详情</el-button></template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="detailVisible" title="问诊记录详情" width="1080px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <div class="meta">
            <article><span>问诊单号</span><strong>{{ detail.consultationNo }}</strong></article>
            <article><span>就诊人</span><strong>{{ detail.patientName }}</strong></article>
            <article><span>问诊分类</span><strong>{{ detail.categoryName }}</strong></article>
            <article><span>推荐科室</span><strong>{{ detail.departmentName || '待分配' }}</strong></article>
            <article><span>初步分诊</span><strong><span class="badge" :style="triageBadgeStyle(detail.triageLevelColor)">{{ detail.triageLevelName || '待评估' }}</span></strong></article>
            <article><span>当前状态</span><strong>{{ statusLabel(detail.status) }}</strong></article>
          </div>

          <section class="card panel">
            <h3>问诊摘要</h3>
            <p class="copy"><strong>标题：</strong>{{ detail.title }}</p>
            <p class="copy"><strong>主诉摘要：</strong>{{ detail.chiefComplaint || '未自动提取' }}</p>
            <p class="copy"><strong>健康摘要：</strong>{{ detail.healthSummary || '未关联健康档案摘要' }}</p>
            <p class="copy"><strong>系统建议：</strong>{{ detail.triageSuggestion || '当前暂无额外建议' }}</p>
            <p class="copy"><strong>规则摘要：</strong>{{ detail.triageRuleSummary || '当前未命中额外高风险规则' }}</p>
          </section>

          <section class="card panel">
            <div class="head">
              <div>
                <h3>认领归档</h3>
                <p>查看医生接单锁定情况，便于运营和质控追踪。</p>
              </div>
              <div class="chips" v-if="detail.doctorAssignment">
                <span>{{ assignmentStatusLabel(detail.doctorAssignment) }}</span>
                <span>{{ detail.doctorAssignment.doctorName || '-' }}</span>
                <span v-if="detail.doctorAssignment.claimTime">认领 {{ formatDate(detail.doctorAssignment.claimTime) }}</span>
                <span v-if="detail.doctorAssignment.releaseTime">释放 {{ formatDate(detail.doctorAssignment.releaseTime) }}</span>
              </div>
            </div>
            <div class="subcard">
              <p class="copy"><strong>当前状态：</strong>{{ assignmentStatusLabel(detail.doctorAssignment) }}</p>
              <p class="copy"><strong>认领医生：</strong>{{ detail.doctorAssignment?.doctorName || '暂无认领医生' }}</p>
              <p class="copy"><strong>科室快照：</strong>{{ detail.doctorAssignment?.departmentName || detail.departmentName || '-' }}</p>
            </div>
          </section>

          <section v-if="detail.doctorHandle" class="card panel">
            <div class="head">
              <div>
                <h3>医生处理归档</h3>
                <p>查看医生是否已接手当前问诊，以及本次处理建议和随访安排。</p>
              </div>
              <div class="chips">
                <span>{{ detail.doctorHandle.doctorName || '未指派医生' }}</span>
                <span>{{ handleStatusLabel(detail.doctorHandle.status) }}</span>
                <span>接手 {{ formatDate(detail.doctorHandle.receiveTime) }}</span>
                <span v-if="detail.doctorHandle.completeTime">完成 {{ formatDate(detail.doctorHandle.completeTime) }}</span>
              </div>
            </div>
            <div class="subcard">
              <p class="copy"><strong>判断摘要：</strong>{{ detail.doctorHandle.summary || '暂无摘要' }}</p>
              <p class="copy"><strong>处理建议：</strong>{{ detail.doctorHandle.medicalAdvice || '暂无处理建议' }}</p>
              <p class="copy"><strong>随访计划：</strong>{{ detail.doctorHandle.followUpPlan || '暂无随访安排' }}</p>
              <p class="copy"><strong>内部备注：</strong>{{ detail.doctorHandle.internalRemark || '暂无内部备注' }}</p>
            </div>
          </section>

          <section v-if="detail.doctorConclusion" class="card panel">
            <div class="head">
              <div>
                <h3>结构化结论</h3>
                <p>查看医生沉淀的标准化结论，用于 AI 复盘和统计分析。</p>
              </div>
              <div class="chips">
                <span>{{ conditionLevelLabel(detail.doctorConclusion.conditionLevel) }}</span>
                <span>{{ dispositionLabel(detail.doctorConclusion.disposition) }}</span>
                <span>{{ aiConsistencyLabel(detail.doctorConclusion.isConsistentWithAi) }}</span>
                <span>{{ detail.doctorConclusion.needFollowUp === 1 ? '需要随访' : '无需随访' }}</span>
                <span v-if="detail.doctorConclusion.followUpWithinDays">{{ detail.doctorConclusion.followUpWithinDays }} 天内随访</span>
              </div>
            </div>
            <div class="subcard">
              <p class="copy"><strong>诊断方向：</strong>{{ detail.doctorConclusion.diagnosisDirection || '未填写' }}</p>
              <p class="copy"><strong>患者指导：</strong>{{ detail.doctorConclusion.patientInstruction || '暂无患者指导' }}</p>
            </div>
            <div v-if="parseJsonArray(detail.doctorConclusion.conclusionTagsJson).length" class="chips">
              <span v-for="item in parseJsonArray(detail.doctorConclusion.conclusionTagsJson)" :key="item">{{ item }}</span>
            </div>
          </section>

          <section class="card panel">
            <div class="head">
              <div>
                <h3>随访记录</h3>
                <p>查看医生对已完成问诊单的后续跟踪记录。</p>
              </div>
              <div class="chips" v-if="detail.doctorFollowUps?.length">
                <span>共 {{ detail.doctorFollowUps.length }} 条</span>
                <span v-if="detail.doctorFollowUps[0]?.createTime">最近随访 {{ formatDate(detail.doctorFollowUps[0].createTime) }}</span>
              </div>
            </div>
            <div v-if="detail.doctorFollowUps?.length" class="answer-grid">
              <article v-for="item in detail.doctorFollowUps" :key="item.id" class="subcard">
                <div class="chips">
                  <span>{{ followUpTypeLabel(item.followUpType) }}</span>
                  <span>{{ patientStatusLabel(item.patientStatus) }}</span>
                  <span>{{ item.doctorName || '-' }}</span>
                  <span>{{ formatDate(item.createTime) }}</span>
                  <span v-if="item.needRevisit === 1">需再次随访</span>
                  <span v-if="item.nextFollowUpDate">下次 {{ formatDate(item.nextFollowUpDate) }}</span>
                </div>
                <p class="copy"><strong>随访摘要：</strong>{{ item.summary }}</p>
                <p class="copy"><strong>随访建议：</strong>{{ item.advice || '暂无补充建议' }}</p>
                <p class="copy"><strong>下一步安排：</strong>{{ item.nextStep || '暂无下一步安排' }}</p>
              </article>
            </div>
            <el-empty v-else description="当前暂无随访记录" />
          </section>

          <section v-if="detail.triageResult" class="card panel">
            <div class="head">
              <div>
                <h3>分诊结果归档</h3>
                <p>查看最终推荐结果、风险摘要和推荐医生。</p>
              </div>
              <div class="chips">
                <span>{{ detail.triageResult.triageLevelName || '待评估' }}</span>
                <span>{{ detail.triageResult.departmentName || '未匹配科室' }}</span>
                <span>置信度 {{ formatConfidence(detail.triageResult.confidenceScore) }}</span>
                <span v-if="detail.triageResult.doctorName">推荐医生 {{ detail.triageResult.doctorName }}</span>
              </div>
            </div>
            <div class="subcard">
              <p class="copy"><strong>结果说明：</strong>{{ detail.triageResult.reasonText || '暂无结果说明' }}</p>
              <p class="copy"><strong>风险标签：</strong>{{ parseJsonArray(detail.triageResult.riskFlagsJson).join('、') || '暂无风险标签' }}</p>
            </div>
          </section>

          <section v-if="detail.triageFeedback" class="card panel">
            <div class="head">
              <div>
                <h3>用户反馈</h3>
                <p>查看用户对本次导诊的采纳情况和人工纠偏信息。</p>
              </div>
              <div class="chips">
                <span>{{ detail.triageFeedback.isAdopted === 1 ? '已采纳' : '未采纳' }}</span>
                <span>评分 {{ detail.triageFeedback.userScore }}/5</span>
                <span v-if="detail.triageFeedback.manualCorrectDepartmentName">修正科室 {{ detail.triageFeedback.manualCorrectDepartmentName }}</span>
                <span v-if="detail.triageFeedback.manualCorrectDoctorName">修正医生 {{ detail.triageFeedback.manualCorrectDoctorName }}</span>
              </div>
            </div>
            <div class="subcard">
              <p class="copy"><strong>反馈内容：</strong>{{ detail.triageFeedback.feedbackText || '用户未填写额外说明' }}</p>
              <p class="copy"><strong>反馈时间：</strong>{{ formatDate(detail.triageFeedback.updateTime || detail.triageFeedback.createTime) }}</p>
            </div>
          </section>

          <section class="card panel">
            <h3>规则命中日志</h3>
            <el-table v-if="detail.ruleHits?.length" :data="detail.ruleHits" border>
              <el-table-column label="主规则" width="90" align="center">
                <template #default="{ row }"><el-tag v-if="row.isPrimary === 1" type="danger" effect="light">主规则</el-tag><span v-else>-</span></template>
              </el-table-column>
              <el-table-column prop="ruleName" label="规则名称" min-width="170" />
              <el-table-column prop="ruleCode" label="规则编码" min-width="150" />
              <el-table-column prop="triageLevelName" label="规则等级" min-width="120" />
              <el-table-column prop="matchedSummary" label="命中依据" min-width="220" show-overflow-tooltip />
            </el-table>
            <el-empty v-else description="当前记录未命中额外规则" />
          </section>

          <section class="card panel">
            <h3>问诊答案</h3>
            <div v-if="detail.answers?.length" class="answer-grid">
              <article v-for="answer in detail.answers" :key="answer.id" class="subcard">
                <strong>{{ answer.fieldLabel }}</strong>
                <div class="copy">
                  <template v-if="answer.fieldType === 'upload' && answer.fieldValue"><img :src="resolveImagePath(answer.fieldValue)" :alt="answer.fieldLabel" class="image" /></template>
                  <template v-else-if="answer.fieldType === 'multi_select'"><div class="chips"><span v-for="item in parseJsonArray(answer.fieldValue)" :key="item">{{ item }}</span></div></template>
                  <template v-else>{{ displayAnswer(answer) }}</template>
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无问诊答案" />
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
const detail = ref(null)
const keyword = ref('')
const categoryFilter = ref('')
const triageFilter = ref('')
const statusFilter = ref('')
const assignmentFilter = ref('')

const assignedCount = computed(() => records.value.filter(item => item.doctorAssignment?.status === 'claimed').length)
const completedCount = computed(() => records.value.filter(item => item.status === 'completed').length)
const todayCount = computed(() => {
  const today = new Date().toDateString()
  return records.value.filter(item => item.createTime && new Date(item.createTime).toDateString() === today).length
})
const categoryOptions = computed(() => [...new Set(records.value.map(item => item.categoryName).filter(Boolean))])
const triageOptions = computed(() => [...new Set(records.value.map(item => item.triageLevelName).filter(Boolean))])
const filteredRecords = computed(() => records.value.filter(item => {
  const search = keyword.value.trim().toLowerCase()
  const matchesKeyword = !search || [item.consultationNo, item.patientName, item.title, item.chiefComplaint].filter(Boolean).some(text => `${text}`.toLowerCase().includes(search))
  const matchesCategory = !categoryFilter.value || item.categoryName === categoryFilter.value
  const matchesTriage = !triageFilter.value || item.triageLevelName === triageFilter.value
  const matchesStatus = !statusFilter.value || item.status === statusFilter.value
  const matchesAssignment = !assignmentFilter.value || assignmentKey(item.doctorAssignment) === assignmentFilter.value
  return matchesKeyword && matchesCategory && matchesTriage && matchesStatus && matchesAssignment
}))

function loadData() {
  loading.value = true
  get('/api/admin/consultation-record/list', data => {
    records.value = data || []
    loading.value = false
  }, message => {
    loading.value = false
    ElMessage.warning(message || '问诊记录加载失败')
  })
}
function openDetail(id) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  get(`/api/admin/consultation-record/detail?id=${id}`, data => {
    detail.value = data || null
    detailLoading.value = false
  }, message => {
    detailLoading.value = false
    detailVisible.value = false
    ElMessage.warning(message || '问诊记录详情加载失败')
  })
}
function assignmentKey(assignment) {
  if (!assignment) return 'unclaimed'
  return assignment.status === 'claimed' ? 'claimed' : 'released'
}
function assignmentStatusLabel(assignment) {
  if (!assignment) return '待认领'
  return assignment.status === 'claimed' ? '已认领' : '已释放'
}
function assignmentTagType(assignment) {
  if (!assignment) return 'info'
  return assignment.status === 'claimed' ? 'success' : 'warning'
}
function statusLabel(value) { return ({ submitted: '已提交', triaged: '已分诊', processing: '处理中', completed: '已完成' })[value] || value || '-' }
function handleStatusLabel(value) { return value === 'completed' ? '处理完成' : '处理中' }
function conditionLevelLabel(value) { return ({ low: '轻度', medium: '中度', high: '较高风险', critical: '危急' })[value] || '未填写' }
function dispositionLabel(value) { return ({ observe: '继续观察', online_followup: '线上随访', offline_visit: '线下就医', emergency: '立即急诊' })[value] || '未填写' }
function aiConsistencyLabel(value) { return value === 1 ? '与 AI 一致' : value === 0 ? '与 AI 不一致' : '未判断' }
function followUpTypeLabel(value) { return ({ platform: '平台随访', phone: '电话随访', offline: '线下随访', other: '其他方式' })[value] || '其他方式' }
function patientStatusLabel(value) { return ({ improved: '明显好转', stable: '基本稳定', worsened: '出现加重', other: '其他情况' })[value] || '其他情况' }
function statusTagType(value) { return ({ submitted: 'info', triaged: 'primary', processing: 'warning', completed: 'success' })[value] || 'info' }
function triageBadgeStyle(color) { return color ? { color, borderColor: `${color}33`, backgroundColor: `${color}14` } : {} }
function parseJsonArray(value) { try { const parsed = value ? JSON.parse(value) : []; return Array.isArray(parsed) ? parsed : [] } catch { return [] } }
function formatConfidence(value) {
  const number = Number(value)
  return Number.isNaN(number) || number <= 0 ? '-' : `${Math.round(number * 100)}%`
}
function displayAnswer(answer) { return answer.fieldType === 'switch' ? (answer.fieldValue === '1' ? '是' : '否') : (answer.fieldValue || '-') }
function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}
onMounted(() => loadData())
</script>

<style scoped>
.record-page{display:flex;flex-direction:column;gap:18px}.stats{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:18px}.card{border:1px solid var(--app-border);border-radius:28px;background:var(--app-panel);box-shadow:var(--app-shadow)}.stat,.block,.panel{padding:22px}.stat span,.head p,.assignment span,.meta span{color:var(--app-muted)}.stat strong{display:block;margin-top:14px;font-size:30px}.head,.toolbar,.assignment,.chips{display:flex;gap:10px;align-items:flex-start;flex-wrap:wrap}.head{justify-content:space-between;margin-bottom:16px}.head h3,.panel h3{margin:0}.head p{margin:6px 0 0;line-height:1.7}.meta{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.meta article,.subcard{padding:16px 18px;border-radius:18px;background:rgba(19,73,80,.05)}.meta strong{display:block;margin-top:8px}.copy{margin:0;line-height:1.8;color:#41575d}.copy+.copy{margin-top:8px}.chips span{padding:8px 14px;border-radius:999px;background:rgba(19,73,80,.08);color:#27646d}.badge{display:inline-flex;align-items:center;justify-content:center;padding:7px 14px;border-radius:999px;border:1px solid rgba(15,102,101,.18);background:rgba(15,102,101,.08);color:#0f6665;font-size:12px;font-weight:600}.answer-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.image{width:220px;height:150px;border-radius:18px;object-fit:cover;border:1px solid rgba(17,70,77,.08)}
@media (max-width:1180px){.stats,.meta,.answer-grid{grid-template-columns:1fr}}
@media (max-width:760px){.head,.toolbar{flex-direction:column;align-items:flex-start}}
</style>
