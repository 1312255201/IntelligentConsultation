<template>
  <div class="doctor-knowledge-page">
    <section class="hero-card">
      <div>
        <h3>医学知识维护</h3>
        <p>沉淀个人高频问诊经验、注意事项和标准化解释，既方便后续复用，也能为项目里的智能问诊知识资产提供补充。</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">新增知识</el-button>
    </section>

    <section class="stat-grid">
      <article class="stat-card">
        <span>个人知识条目</span>
        <strong>{{ knowledgeList.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>分诊策略类</span>
        <strong>{{ strategyCount }}</strong>
      </article>
      <article class="stat-card">
        <span>患者宣教类</span>
        <strong>{{ educationCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索标题、标签或正文"
            style="max-width: 320px"
          />
          <el-select v-model="typeFilter" clearable placeholder="全部类型" style="width: 180px">
            <el-option v-for="item in knowledgeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="statusFilter" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadKnowledge">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredKnowledgeList" v-loading="loading" border>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="140" align="center">
          <template #default="{ row }">
            <el-tag effect="light">{{ knowledgeTypeLabel(row.knowledgeType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="180" show-overflow-tooltip />
        <el-table-column label="内容摘要" min-width="320" show-overflow-tooltip>
          <template #default="{ row }">{{ contentPreview(row.content) }}</template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="removeKnowledge(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑医学知识' : '新增医学知识'" width="860px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="知识类型" prop="knowledgeType">
            <el-select v-model="form.knowledgeType" style="width: 100%">
              <el-option v-for="item in knowledgeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" maxlength="100" placeholder="例如：轻症发热线上观察要点" />
          </el-form-item>
          <el-form-item label="版本" prop="version">
            <el-input-number v-model="form.version" :min="1" :max="99" style="width: 100%" />
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="form.sort" :min="0" :max="999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="标签">
          <el-input v-model="form.tags" maxlength="255" placeholder="多个标签用英文逗号分隔，例如：发热,宣教,复诊" />
        </el-form-item>

        <el-form-item label="知识正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            maxlength="10000"
            show-word-limit
            placeholder="请填写适用场景、主要判断点、患者需要注意的事项，以及何时建议线下就医。"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitKnowledge">{{ isEditing ? '保存修改' : '确认新增' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { get, post } from '@/net'

const knowledgeTypeOptions = [
  { value: 'triage_strategy', label: '分诊策略' },
  { value: 'symptom_guide', label: '症状要点' },
  { value: 'patient_education', label: '患者宣教' },
  { value: 'followup_advice', label: '随访建议' },
  { value: 'doctor_experience', label: '接诊经验' }
]

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const typeFilter = ref('')
const statusFilter = ref(null)
const formRef = ref()
const knowledgeList = ref([])

const form = reactive(createEmptyForm())

const rules = {
  knowledgeType: [{ required: true, message: '请选择知识类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入知识正文', trigger: 'blur' }],
  version: [{ required: true, message: '请填写版本号', trigger: 'change' }],
  sort: [{ required: true, message: '请填写排序值', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const filteredKnowledgeList = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return knowledgeList.value.filter(item => {
    if (typeFilter.value && item.knowledgeType !== typeFilter.value) return false
    if (statusFilter.value !== null && item.status !== statusFilter.value) return false
    if (!search) return true
    return [item.title, item.tags, item.content]
      .filter(Boolean)
      .some(text => `${text}`.toLowerCase().includes(search))
  })
})

const enabledCount = computed(() => knowledgeList.value.filter(item => item.status === 1).length)
const strategyCount = computed(() => knowledgeList.value.filter(item => item.knowledgeType === 'triage_strategy').length)
const educationCount = computed(() => knowledgeList.value.filter(item => ['patient_education', 'followup_advice'].includes(item.knowledgeType)).length)
const isEditing = computed(() => !!form.id)

function createEmptyForm() {
  return {
    id: null,
    knowledgeType: 'doctor_experience',
    title: '',
    content: '',
    tags: '',
    version: 1,
    sort: 0,
    status: 1
  }
}

function loadKnowledge() {
  loading.value = true
  get('/api/doctor/knowledge/list', (data) => {
    knowledgeList.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function openCreateDialog() {
  Object.assign(form, createEmptyForm())
  dialogVisible.value = true
}

function openEditDialog(row) {
  Object.assign(form, createEmptyForm(), row)
  dialogVisible.value = true
}

function submitKnowledge() {
  formRef.value.validate((valid) => {
    if (!valid) return
    const payload = {
      knowledgeType: form.knowledgeType,
      title: form.title,
      content: form.content,
      tags: form.tags,
      version: form.version,
      sort: form.sort,
      status: form.status,
      sourceType: 'doctor_manual'
    }
    if (isEditing.value) payload.id = form.id

    submitting.value = true
    post(isEditing.value ? '/api/doctor/knowledge/update' : '/api/doctor/knowledge/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '医学知识已更新' : '医学知识已新增')
      loadKnowledge()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '保存失败，请稍后重试')
    })
  })
}

function removeKnowledge(row) {
  ElMessageBox.confirm(`确认删除“${row.title}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/doctor/knowledge/delete?id=${row.id}`, () => {
      ElMessage.success('医学知识已删除')
      loadKnowledge()
    })
  }).catch(() => {})
}

function knowledgeTypeLabel(value) {
  return knowledgeTypeOptions.find(item => item.value === value)?.label || value || '未分类'
}

function contentPreview(value) {
  if (!value) return '—'
  const text = `${value}`.trim()
  return text.length > 90 ? `${text.slice(0, 90)}...` : text
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadKnowledge)
</script>

<style scoped>
.doctor-knowledge-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.stat-card,
.table-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 40px rgba(17, 70, 77, 0.08);
}

.hero-card,
.table-card {
  padding: 24px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  flex-wrap: wrap;
}

.hero-card h3 {
  margin: 0;
  font-size: 24px;
  color: #17373d;
}

.hero-card p {
  margin: 8px 0 0;
  color: #687c84;
  line-height: 1.7;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 20px 22px;
}

.stat-card span {
  display: block;
  color: #6c7f86;
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 30px;
  color: #17373d;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.toolbar-filters,
.toolbar-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 768px) {
  .dialog-grid {
    grid-template-columns: 1fr;
  }
}
</style>
