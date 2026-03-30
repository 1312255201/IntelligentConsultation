<template>
  <div class="triage-case-page">
    <section class="helper-card">
      <p>
        这里维护给 AI 导诊、推荐解释和运营复盘使用的内部案例。
        与首页展示案例不同，这些内容强调主诉、症状摘要、分诊结论和风险等级等结构化信息。
      </p>
    </section>

    <section class="stat-grid">
      <article class="stat-card">
        <span>案例条目</span>
        <strong>{{ caseList.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>高风险案例</span>
        <strong>{{ highRiskCount }}</strong>
      </article>
      <article class="stat-card">
        <span>医生绑定</span>
        <strong>{{ doctorScopedCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索标题、主诉、症状摘要、标签、科室或医生"
            style="max-width: 340px"
          />
          <el-select
            v-model="selectedDepartmentId"
            clearable
            filterable
            style="width: 180px"
            placeholder="全部科室"
          >
            <el-option
              v-for="item in departments"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-select
            v-model="selectedRiskLevel"
            clearable
            style="width: 180px"
            placeholder="全部风险等级"
          >
            <el-option
              v-for="item in riskLevelOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-select
            v-model="selectedStatus"
            clearable
            style="width: 160px"
            placeholder="全部状态"
          >
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="停用" />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增导诊案例</el-button>
        </div>
      </div>

      <el-table :data="filteredCaseList" v-loading="loading" border>
        <el-table-column prop="title" label="案例标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="关联科室" min-width="140">
          <template #default="{ row }">
            {{ departmentName(row.departmentId) }}
          </template>
        </el-table-column>
        <el-table-column label="关联医生" min-width="140">
          <template #default="{ row }">
            {{ doctorName(row.doctorId) }}
          </template>
        </el-table-column>
        <el-table-column label="风险等级" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="riskLevelTagType(row.riskLevel)" effect="light">
              {{ riskLevelLabel(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分诊结论" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.triageResult }}
          </template>
        </el-table-column>
        <el-table-column label="主诉摘要" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.chiefComplaint }}
          </template>
        </el-table-column>
        <el-table-column label="来源" width="130" align="center">
          <template #default="{ row }">
            <el-tag effect="light">{{ sourceTypeLabel(row.sourceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">
            {{ formatDate(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="removeCase(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑导诊案例' : '新增导诊案例'"
      width="980px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="案例标题" prop="title">
            <el-input v-model="form.title" maxlength="100" placeholder="例如：胸痛伴呼吸困难的线下优先分诊案例" />
          </el-form-item>
          <el-form-item label="风险等级" prop="riskLevel">
            <el-select v-model="form.riskLevel" style="width: 100%" placeholder="请选择风险等级">
              <el-option
                v-for="item in riskLevelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="关联科室" prop="departmentId">
            <el-select
              v-model="form.departmentId"
              filterable
              style="width: 100%"
              placeholder="请选择科室"
            >
              <el-option
                v-for="item in departments"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="关联医生" prop="doctorId">
            <el-select
              v-model="form.doctorId"
              clearable
              filterable
              style="width: 100%"
              placeholder="可选，用于绑定适用医生"
              @change="handleDoctorChange"
            >
              <el-option
                v-for="item in filteredDoctorOptions"
                :key="item.id"
                :label="`${item.name}${item.title ? ` / ${item.title}` : ''}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="来源类型" prop="sourceType">
            <el-select v-model="form.sourceType" style="width: 100%">
              <el-option
                v-for="item in sourceTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="form.sort" :min="0" :max="999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option :value="1" label="启用" />
              <el-option :value="0" label="停用" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="标签" prop="tags">
          <el-input
            v-model="form.tags"
            maxlength="255"
            placeholder="多个标签用英文逗号分隔，例如：胸痛,呼吸困难,线下优先"
          />
        </el-form-item>

        <el-form-item label="主要主诉" prop="chiefComplaint">
          <el-input
            v-model="form.chiefComplaint"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="概括用户最核心的不适描述，例如：胸痛 2 小时并伴呼吸困难、出汗"
          />
        </el-form-item>

        <el-form-item label="症状摘要" prop="symptomSummary">
          <el-input
            v-model="form.symptomSummary"
            type="textarea"
            :rows="6"
            maxlength="5000"
            show-word-limit
            placeholder="补充症状进展、伴随表现、既往处理、关键问诊要点等，供 AI 检索和复盘使用。"
          />
        </el-form-item>

        <el-form-item label="分诊结论" prop="triageResult">
          <el-input
            v-model="form.triageResult"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="填写最终建议，例如：建议立即线下就医并优先排查心肺急症风险。"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCase">
          {{ isEditing ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { get, post } from '@/net'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const selectedDepartmentId = ref(null)
const selectedRiskLevel = ref('')
const selectedStatus = ref(null)
const formRef = ref()
const caseList = ref([])
const departments = ref([])
const doctors = ref([])

const riskLevelOptions = [
  { value: 'low', label: '低风险观察' },
  { value: 'medium', label: '常规跟进' },
  { value: 'high', label: '高风险关注' },
  { value: 'emergency', label: '紧急处理' }
]

const sourceTypeOptions = [
  { value: 'manual', label: '人工整理' },
  { value: 'case_review', label: '病例复盘' },
  { value: 'doctor_summary', label: '医生总结' },
  { value: 'operation', label: '运营沉淀' }
]

const form = reactive(createEmptyForm())

const rules = {
  title: [
    { required: true, message: '请输入案例标题', trigger: 'blur' }
  ],
  chiefComplaint: [
    { required: true, message: '请输入主要主诉', trigger: 'blur' }
  ],
  triageResult: [
    { required: true, message: '请输入分诊结论', trigger: 'blur' }
  ],
  departmentId: [
    { required: true, message: '请选择科室', trigger: 'change' }
  ],
  riskLevel: [
    { required: true, message: '请选择风险等级', trigger: 'change' }
  ],
  sourceType: [
    { required: true, message: '请选择来源类型', trigger: 'change' }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const departmentMap = computed(() => {
  const map = new Map()
  departments.value.forEach(item => map.set(item.id, item))
  return map
})

const doctorMap = computed(() => {
  const map = new Map()
  doctors.value.forEach(item => map.set(item.id, item))
  return map
})

const filteredDoctorOptions = computed(() => {
  if (!form.departmentId) return doctors.value
  return doctors.value.filter(item => item.departmentId === form.departmentId)
})

const filteredCaseList = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return caseList.value.filter(item => {
    const matchesKeyword = !search || [
      item.title,
      item.chiefComplaint,
      item.symptomSummary,
      item.triageResult,
      item.tags,
      departmentName(item.departmentId),
      doctorName(item.doctorId)
    ].filter(Boolean).some(text => `${text}`.toLowerCase().includes(search))
    const matchesDepartment = !selectedDepartmentId.value || item.departmentId === selectedDepartmentId.value
    const matchesRisk = !selectedRiskLevel.value || item.riskLevel === selectedRiskLevel.value
    const matchesStatus = selectedStatus.value === null || item.status === selectedStatus.value
    return matchesKeyword && matchesDepartment && matchesRisk && matchesStatus
  })
})

const enabledCount = computed(() => caseList.value.filter(item => item.status === 1).length)
const highRiskCount = computed(() => caseList.value.filter(item => ['high', 'emergency'].includes(item.riskLevel)).length)
const doctorScopedCount = computed(() => caseList.value.filter(item => !!item.doctorId).length)
const isEditing = computed(() => !!form.id)

watch(() => form.departmentId, (value) => {
  if (!form.doctorId) return
  const doctor = doctorMap.value.get(form.doctorId)
  if (!doctor || doctor.departmentId !== value) {
    form.doctorId = null
  }
})

function createEmptyForm() {
  return {
    id: null,
    title: '',
    chiefComplaint: '',
    symptomSummary: '',
    triageResult: '',
    departmentId: null,
    doctorId: null,
    riskLevel: 'medium',
    tags: '',
    sourceType: 'manual',
    sort: 0,
    status: 1
  }
}

function loadData() {
  loadDepartments()
  loadDoctors()
  loadCases()
}

function loadDepartments() {
  get('/api/admin/department/list', (data) => {
    departments.value = data || []
  })
}

function loadDoctors() {
  get('/api/admin/doctor/list', (data) => {
    doctors.value = data || []
  })
}

function loadCases() {
  loading.value = true
  get('/api/admin/triage-case/list', (data) => {
    caseList.value = data || []
    loading.value = false
  }, (message) => {
    loading.value = false
    ElMessage.warning(message || '导诊案例加载失败')
  })
}

function openCreateDialog() {
  Object.assign(form, createEmptyForm(), {
    departmentId: selectedDepartmentId.value || null
  })
  dialogVisible.value = true
}

function openEditDialog(row) {
  Object.assign(form, createEmptyForm(), row)
  dialogVisible.value = true
}

function handleDoctorChange(doctorId) {
  const doctor = doctorMap.value.get(doctorId)
  if (doctor && doctor.departmentId && !form.departmentId) {
    form.departmentId = doctor.departmentId
  }
}

function submitCase() {
  formRef.value.validate((valid) => {
    if (!valid) return

    const payload = {
      title: form.title.trim(),
      chiefComplaint: form.chiefComplaint.trim(),
      symptomSummary: normalizeText(form.symptomSummary),
      triageResult: form.triageResult.trim(),
      departmentId: form.departmentId,
      doctorId: form.doctorId || null,
      riskLevel: form.riskLevel,
      tags: normalizeText(form.tags),
      sourceType: form.sourceType,
      sort: form.sort,
      status: form.status
    }
    if (isEditing.value) payload.id = form.id

    submitting.value = true
    post(isEditing.value ? '/api/admin/triage-case/update' : '/api/admin/triage-case/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '导诊案例更新成功' : '导诊案例新增成功')
      loadCases()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '提交失败，请稍后重试')
    })
  })
}

function removeCase(row) {
  ElMessageBox.confirm(`确认删除导诊案例“${row.title}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/triage-case/delete?id=${row.id}`, () => {
      ElMessage.success('导诊案例删除成功')
      loadCases()
    }, (message) => {
      ElMessage.warning(message || '导诊案例删除失败')
    })
  }).catch(() => {})
}

function normalizeText(value) {
  const text = String(value || '').trim()
  return text || ''
}

function departmentName(id) {
  return departmentMap.value.get(id)?.name || '-'
}

function doctorName(id) {
  return doctorMap.value.get(id)?.name || '-'
}

function riskLevelLabel(value) {
  return riskLevelOptions.find(item => item.value === value)?.label || value || '-'
}

function riskLevelTagType(value) {
  return {
    low: 'success',
    medium: '',
    high: 'warning',
    emergency: 'danger'
  }[value] ?? ''
}

function sourceTypeLabel(value) {
  return sourceTypeOptions.find(item => item.value === value)?.label || value || '-'
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
.triage-case-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.helper-card,
.stat-card,
.table-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.helper-card,
.stat-card,
.table-card {
  padding: 22px;
}

.helper-card p {
  margin: 0;
  line-height: 1.8;
  color: #48656d;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.stat-card span {
  color: var(--app-muted);
}

.stat-card strong {
  display: block;
  margin-top: 14px;
  font-size: 30px;
}

.toolbar,
.toolbar-filters,
.toolbar-actions {
  display: flex;
  gap: 12px;
}

.toolbar {
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.toolbar-filters {
  flex-wrap: wrap;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

@media (max-width: 1180px) {
  .stat-grid,
  .dialog-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
