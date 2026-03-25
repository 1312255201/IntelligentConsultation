<template>
  <div class="body-part-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>部位总数</span>
        <strong>{{ bodyParts.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>顶级部位</span>
        <strong>{{ rootCount }}</strong>
      </article>
      <article class="stat-card">
        <span>子级部位</span>
        <strong>{{ childCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索部位名称、编码、父级部位或说明"
          style="max-width: 360px"
        />
        <div class="toolbar-actions">
          <el-button @click="loadBodyParts">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增身体部位</el-button>
        </div>
      </div>

      <el-table :data="filteredBodyParts" v-loading="loading" border>
        <el-table-column label="部位名称" min-width="220">
          <template #default="{ row }">
            {{ bodyPartPath(row.id) }}
          </template>
        </el-table-column>
        <el-table-column prop="code" label="部位编码" min-width="150" />
        <el-table-column label="父级部位" min-width="150">
          <template #default="{ row }">
            {{ parentName(row.parentId) }}
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" align="center" />
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
        <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="removeBodyPart(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑身体部位' : '新增身体部位'"
      width="680px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="部位名称" prop="name">
            <el-input v-model="form.name" maxlength="50" placeholder="例如：胸部" />
          </el-form-item>
          <el-form-item label="部位编码" prop="code">
            <el-input v-model="form.code" maxlength="50" placeholder="例如：CHEST" />
          </el-form-item>
          <el-form-item label="父级部位" prop="parentId">
            <el-select
              v-model="form.parentId"
              clearable
              filterable
              style="width: 100%"
              placeholder="可选，不选则为顶级部位"
            >
              <el-option
                v-for="item in parentOptions"
                :key="item.id"
                :label="bodyPartPath(item.id)"
                :value="item.id"
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
        <el-form-item label="说明" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="用于说明该身体部位的识别范围，例如胸痛、胸闷、气短等都可归入胸部"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBodyPart">
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
const formRef = ref()
const bodyParts = ref([])

const form = reactive(createEmptyForm())

const rules = {
  name: [
    { required: true, message: '请输入部位名称', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入部位编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '部位编码仅支持字母、数字、下划线和中划线', trigger: ['blur', 'change'] }
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

const parentOptions = computed(() => bodyParts.value.filter(item => isAvailableParent(item.id)))

const filteredBodyParts = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return bodyParts.value
  return bodyParts.value.filter(item =>
    [bodyPartPath(item.id), item.code, parentName(item.parentId), item.description]
      .filter(Boolean)
      .some(text => String(text).toLowerCase().includes(value))
  )
})

const enabledCount = computed(() => bodyParts.value.filter(item => item.status === 1).length)
const rootCount = computed(() => bodyParts.value.filter(item => !item.parentId).length)
const childCount = computed(() => bodyParts.value.filter(item => item.parentId).length)
const isEditing = computed(() => !!form.id)

function createEmptyForm() {
  return {
    id: null,
    name: '',
    code: '',
    parentId: null,
    description: '',
    sort: 0,
    status: 1
  }
}

function loadBodyParts() {
  loading.value = true
  get('/api/admin/body-part/list', (data) => {
    bodyParts.value = data || []
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

function submitBodyPart() {
  formRef.value.validate((valid) => {
    if (!valid) return

    const payload = {
      name: form.name.trim(),
      code: form.code.trim(),
      parentId: form.parentId,
      description: normalizeText(form.description),
      sort: form.sort,
      status: form.status
    }
    if (isEditing.value) payload.id = form.id

    submitting.value = true
    post(isEditing.value ? '/api/admin/body-part/update' : '/api/admin/body-part/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '身体部位更新成功' : '身体部位新增成功')
      loadBodyParts()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '提交失败，请稍后重试')
    })
  })
}

function removeBodyPart(row) {
  ElMessageBox.confirm(`确认删除身体部位“${bodyPartPath(row.id)}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/body-part/delete?id=${row.id}`, () => {
      ElMessage.success('身体部位删除成功')
      loadBodyParts()
    }, (message) => {
      ElMessage.warning(message || '身体部位删除失败')
    })
  }).catch(() => {})
}

function isAvailableParent(candidateId) {
  if (!form.id) return true
  if (candidateId === form.id) return false

  let current = bodyPartMap.value.get(candidateId)
  const visited = new Set()
  while (current && current.parentId && !visited.has(current.id)) {
    if (current.parentId === form.id) return false
    visited.add(current.id)
    current = bodyPartMap.value.get(current.parentId)
  }
  return true
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

function parentName(parentId) {
  if (!parentId) return '-'
  return bodyPartMap.value.get(parentId)?.name || '-'
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

onMounted(() => loadBodyParts())
</script>

<style scoped>
.body-part-page {
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

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
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
