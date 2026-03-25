<template>
  <div class="knowledge-page">
    <section class="helper-card">
      <p>
        这里维护供规则引擎、运营复盘和后续 AI 检索使用的导诊知识。
        可以录入分诊策略、科室接诊范围、医生服务画像、典型案例总结和服务流程说明。
      </p>
    </section>

    <section class="stat-grid">
      <article class="stat-card">
        <span>知识条目</span>
        <strong>{{ knowledgeList.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>绑定科室</span>
        <strong>{{ departmentScopedCount }}</strong>
      </article>
      <article class="stat-card">
        <span>医生专属</span>
        <strong>{{ doctorScopedCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索标题、标签、正文、科室或医生"
            style="max-width: 320px"
          />
          <el-select
            v-model="selectedKnowledgeType"
            clearable
            style="width: 180px"
            placeholder="全部知识类型"
          >
            <el-option
              v-for="item in knowledgeTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
          <el-button type="primary" @click="openCreateDialog">新增导诊知识</el-button>
        </div>
      </div>

      <el-table :data="filteredKnowledgeList" v-loading="loading" border>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="知识类型" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="knowledgeTypeTagType(row.knowledgeType)" effect="light">
              {{ knowledgeTypeLabel(row.knowledgeType) }}
            </el-tag>
          </template>
        </el-table-column>
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
        <el-table-column label="来源" width="130" align="center">
          <template #default="{ row }">
            <el-tag effect="light">{{ sourceTypeLabel(row.sourceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="180" show-overflow-tooltip />
        <el-table-column label="内容摘要" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            {{ contentPreview(row.content) }}
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
            <el-button link type="danger" @click="removeKnowledge(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑导诊知识' : '新增导诊知识'"
      width="920px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="知识类型" prop="knowledgeType">
            <el-select v-model="form.knowledgeType" style="width: 100%" placeholder="请选择知识类型">
              <el-option
                v-for="item in knowledgeTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" maxlength="100" placeholder="例如：胸痛场景优先线下评估原则" />
          </el-form-item>
          <el-form-item label="关联科室" prop="departmentId">
            <el-select
              v-model="form.departmentId"
              clearable
              filterable
              style="width: 100%"
              placeholder="可选，用于绑定适用科室"
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
          <el-form-item label="版本号" prop="version">
            <el-input-number v-model="form.version" :min="1" :max="99" style="width: 100%" />
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
            placeholder="多个标签用英文逗号分隔，例如：胸痛,急诊风险,线下优先"
          />
        </el-form-item>

        <el-form-item label="知识正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            maxlength="10000"
            show-word-limit
            placeholder="填写可复用的导诊知识、判断依据、服务边界或案例总结。建议写清适用场景、风险提示和推荐动作。"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitKnowledge">
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
const selectedKnowledgeType = ref('')
const selectedDepartmentId = ref(null)
const selectedStatus = ref(null)
const formRef = ref()
const knowledgeList = ref([])
const departments = ref([])
const doctors = ref([])

const knowledgeTypeOptions = [
  { value: 'triage_strategy', label: '分诊策略' },
  { value: 'symptom_guide', label: '症状要点' },
  { value: 'department_guide', label: '科室接诊' },
  { value: 'doctor_profile', label: '医生画像' },
  { value: 'case_reference', label: '案例参考' },
  { value: 'service_notice', label: '服务说明' }
]

const sourceTypeOptions = [
  { value: 'manual', label: '人工整理' },
  { value: 'guideline', label: '指南规范' },
  { value: 'case_review', label: '病例复盘' },
  { value: 'operation', label: '运营沉淀' }
]

const form = reactive(createEmptyForm())

const rules = {
  knowledgeType: [
    { required: true, message: '请选择知识类型', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' }
  ],
  sourceType: [
    { required: true, message: '请选择来源类型', trigger: 'change' }
  ],
  version: [
    { required: true, message: '请输入版本号', trigger: 'change' }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入知识正文', trigger: 'blur' }
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

const filteredKnowledgeList = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return knowledgeList.value.filter(item => {
    const matchesKeyword = !search || [
      item.title,
      item.tags,
      item.content,
      knowledgeTypeLabel(item.knowledgeType),
      departmentName(item.departmentId),
      doctorName(item.doctorId)
    ].filter(Boolean).some(text => `${text}`.toLowerCase().includes(search))
    const matchesType = !selectedKnowledgeType.value || item.knowledgeType === selectedKnowledgeType.value
    const matchesDepartment = !selectedDepartmentId.value || item.departmentId === selectedDepartmentId.value
    const matchesStatus = selectedStatus.value === null || item.status === selectedStatus.value
    return matchesKeyword && matchesType && matchesDepartment && matchesStatus
  })
})

const enabledCount = computed(() => knowledgeList.value.filter(item => item.status === 1).length)
const departmentScopedCount = computed(() => knowledgeList.value.filter(item => !!item.departmentId).length)
const doctorScopedCount = computed(() => knowledgeList.value.filter(item => !!item.doctorId).length)
const isEditing = computed(() => !!form.id)

watch(() => form.departmentId, (value) => {
  if (!form.doctorId) return
  const doctor = doctorMap.value.get(form.doctorId)
  if (!doctor) {
    form.doctorId = null
    return
  }
  if (value && doctor.departmentId !== value) {
    form.doctorId = null
  }
})

function createEmptyForm() {
  return {
    id: null,
    knowledgeType: 'triage_strategy',
    title: '',
    content: '',
    tags: '',
    departmentId: null,
    doctorId: null,
    sourceType: 'manual',
    version: 1,
    sort: 0,
    status: 1
  }
}

function loadData() {
  loadDepartments()
  loadDoctors()
  loadKnowledgeList()
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

function loadKnowledgeList() {
  loading.value = true
  get('/api/admin/triage-knowledge/list', (data) => {
    knowledgeList.value = data || []
    loading.value = false
  }, (message) => {
    loading.value = false
    ElMessage.warning(message || '导诊知识加载失败')
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

function submitKnowledge() {
  formRef.value.validate((valid) => {
    if (!valid) return

    const payload = {
      knowledgeType: form.knowledgeType,
      title: form.title.trim(),
      content: form.content.trim(),
      tags: normalizeText(form.tags),
      departmentId: form.departmentId || null,
      doctorId: form.doctorId || null,
      sourceType: form.sourceType,
      version: form.version,
      sort: form.sort,
      status: form.status
    }
    if (isEditing.value) payload.id = form.id

    submitting.value = true
    post(isEditing.value ? '/api/admin/triage-knowledge/update' : '/api/admin/triage-knowledge/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '导诊知识更新成功' : '导诊知识新增成功')
      loadKnowledgeList()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '提交失败，请稍后重试')
    })
  })
}

function removeKnowledge(row) {
  ElMessageBox.confirm(`确认删除导诊知识“${row.title}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/triage-knowledge/delete?id=${row.id}`, () => {
      ElMessage.success('导诊知识删除成功')
      loadKnowledgeList()
    }, (message) => {
      ElMessage.warning(message || '导诊知识删除失败')
    })
  }).catch(() => {})
}

function normalizeText(value) {
  const text = String(value || '').trim()
  return text || ''
}

function knowledgeTypeLabel(value) {
  return knowledgeTypeOptions.find(item => item.value === value)?.label || value || '-'
}

function knowledgeTypeTagType(value) {
  return {
    triage_strategy: 'danger',
    symptom_guide: 'warning',
    department_guide: 'success',
    doctor_profile: 'primary',
    case_reference: '',
    service_notice: 'info'
  }[value] ?? ''
}

function sourceTypeLabel(value) {
  return sourceTypeOptions.find(item => item.value === value)?.label || value || '-'
}

function departmentName(id) {
  return departmentMap.value.get(id)?.name || '-'
}

function doctorName(id) {
  return doctorMap.value.get(id)?.name || '-'
}

function contentPreview(value) {
  if (!value) return '-'
  return value.length > 72 ? `${value.slice(0, 72)}...` : value
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
.knowledge-page {
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
