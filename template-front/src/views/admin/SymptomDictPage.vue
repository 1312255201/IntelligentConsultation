<template>
  <div class="symptom-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>症状总数</span>
        <strong>{{ symptoms.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>覆盖部位</span>
        <strong>{{ coveredBodyPartCount }}</strong>
      </article>
      <article class="stat-card">
        <span>已配关键词</span>
        <strong>{{ keywordConfiguredCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索症状名称、编码、关键词、别名或说明"
            style="max-width: 360px"
          />
          <el-select
            v-model="selectedBodyPartId"
            clearable
            filterable
            style="width: 240px"
            placeholder="全部身体部位"
          >
            <el-option
              v-for="item in bodyParts"
              :key="item.id"
              :label="bodyPartPath(item.id)"
              :value="item.id"
            />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增症状</el-button>
        </div>
      </div>

      <el-table :data="filteredSymptoms" v-loading="loading" border>
        <el-table-column prop="name" label="症状名称" min-width="160" />
        <el-table-column prop="code" label="症状编码" min-width="150" />
        <el-table-column label="身体部位" min-width="220">
          <template #default="{ row }">
            {{ bodyPartPath(row.bodyPartId) }}
          </template>
        </el-table-column>
        <el-table-column prop="keywords" label="关键词" min-width="200" show-overflow-tooltip />
        <el-table-column prop="aliasKeywords" label="别名" min-width="200" show-overflow-tooltip />
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
        <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="removeSymptom(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑症状字典' : '新增症状字典'"
      width="760px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="身体部位" prop="bodyPartId">
            <el-select v-model="form.bodyPartId" filterable style="width: 100%" placeholder="请选择身体部位">
              <el-option
                v-for="item in bodyParts"
                :key="item.id"
                :label="bodyPartPath(item.id)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="症状名称" prop="name">
            <el-input v-model="form.name" maxlength="50" placeholder="例如：胸痛" />
          </el-form-item>
          <el-form-item label="症状编码" prop="code">
            <el-input v-model="form.code" maxlength="50" placeholder="例如：CHEST_PAIN" />
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

        <el-form-item label="识别关键词" prop="keywords">
          <el-input
            v-model="form.keywords"
            maxlength="255"
            placeholder="例如：胸口痛,胸闷疼,心口疼"
          />
        </el-form-item>
        <el-form-item label="别名关键词" prop="aliasKeywords">
          <el-input
            v-model="form.aliasKeywords"
            maxlength="255"
            placeholder="例如：胸前区疼痛,胸部刺痛"
          />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="用于说明该症状适用的归类范围、典型表达方式和业务备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitSymptom">
          {{ isEditing ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { get, post } from '@/net'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const selectedBodyPartId = ref(null)
const formRef = ref()
const symptoms = ref([])
const bodyParts = ref([])

const form = reactive(createEmptyForm())

const rules = {
  bodyPartId: [
    { required: true, message: '请选择身体部位', trigger: 'change' }
  ],
  name: [
    { required: true, message: '请输入症状名称', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入症状编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '症状编码仅支持字母、数字、下划线和中划线', trigger: ['blur', 'change'] }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const bodyPartMap = computed(() => {
  const map = new Map()
  bodyParts.value.forEach(item => map.set(item.id, item))
  return map
})

const filteredSymptoms = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  return symptoms.value.filter(item => {
    if (selectedBodyPartId.value && item.bodyPartId !== selectedBodyPartId.value) return false
    if (!value) return true

    return [
      item.name,
      item.code,
      item.keywords,
      item.aliasKeywords,
      item.description,
      bodyPartPath(item.bodyPartId)
    ]
      .filter(Boolean)
      .some(text => String(text).toLowerCase().includes(value))
  })
})

const enabledCount = computed(() => symptoms.value.filter(item => item.status === 1).length)
const coveredBodyPartCount = computed(() => new Set(symptoms.value.map(item => item.bodyPartId)).size)
const keywordConfiguredCount = computed(() => symptoms.value.filter(item => item.keywords || item.aliasKeywords).length)
const isEditing = computed(() => !!form.id)

function createEmptyForm() {
  return {
    id: null,
    bodyPartId: null,
    name: '',
    code: '',
    keywords: '',
    aliasKeywords: '',
    description: '',
    sort: 0,
    status: 1
  }
}

function loadData() {
  loadBodyParts()
  loadSymptoms()
}

function loadBodyParts() {
  get('/api/admin/body-part/list', (data) => {
    bodyParts.value = data || []
  })
}

function loadSymptoms() {
  loading.value = true
  get('/api/admin/symptom/list', (data) => {
    symptoms.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function openCreateDialog() {
  if (!bodyParts.value.length) {
    ElMessage.warning('请先维护身体部位字典，再新增症状')
    return
  }
  Object.assign(form, createEmptyForm(), {
    bodyPartId: selectedBodyPartId.value || null
  })
  dialogVisible.value = true
}

function openEditDialog(row) {
  Object.assign(form, createEmptyForm(), row)
  dialogVisible.value = true
}

function submitSymptom() {
  formRef.value.validate((valid) => {
    if (!valid) return

    const payload = {
      bodyPartId: form.bodyPartId,
      name: form.name.trim(),
      code: form.code.trim(),
      keywords: normalizeText(form.keywords),
      aliasKeywords: normalizeText(form.aliasKeywords),
      description: normalizeText(form.description),
      sort: form.sort,
      status: form.status
    }
    if (isEditing.value) payload.id = form.id

    submitting.value = true
    post(isEditing.value ? '/api/admin/symptom/update' : '/api/admin/symptom/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '症状字典更新成功' : '症状字典新增成功')
      loadSymptoms()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '提交失败，请稍后重试')
    })
  })
}

function removeSymptom(row) {
  ElMessageBox.confirm(`确认删除症状“${row.name}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/symptom/delete?id=${row.id}`, () => {
      ElMessage.success('症状字典删除成功')
      loadSymptoms()
    }, (message) => {
      ElMessage.warning(message || '症状字典删除失败')
    })
  }).catch(() => {})
}

function bodyPartPath(id) {
  const names = []
  let current = bodyPartMap.value.get(id)
  const visited = new Set()

  while (current && !visited.has(current.id)) {
    names.unshift(current.name)
    visited.add(current.id)
    current = current.parentId ? bodyPartMap.value.get(current.parentId) : null
  }

  return names.join(' / ') || '-'
}

function normalizeText(value) {
  const text = String(value || '').trim()
  return text || ''
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
.symptom-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stat-card,
.table-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.stat-card {
  padding: 22px 24px;
}

.stat-card span {
  color: var(--app-muted);
}

.stat-card strong {
  display: block;
  margin-top: 14px;
  font-size: 30px;
}

.table-card {
  padding: 22px;
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

@media (max-width: 1100px) {
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
