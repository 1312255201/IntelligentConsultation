<template>
  <div class="triage-level-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>等级总数</span>
        <strong>{{ levels.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>最高优先级</span>
        <strong>{{ topPriorityText }}</strong>
      </article>
      <article class="stat-card">
        <span>已配建议</span>
        <strong>{{ suggestionConfiguredCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索等级名称、编码、建议或说明"
          style="max-width: 360px"
        />
        <div class="toolbar-actions">
          <el-button @click="loadLevels">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增分诊等级</el-button>
        </div>
      </div>

      <el-table :data="filteredLevels" v-loading="loading" border>
        <el-table-column prop="name" label="等级名称" min-width="150" />
        <el-table-column prop="code" label="等级编码" min-width="150" />
        <el-table-column prop="priority" label="优先级" width="90" align="center" />
        <el-table-column prop="sort" label="排序" width="90" align="center" />
        <el-table-column label="颜色" width="120" align="center">
          <template #default="{ row }">
            <span class="color-badge">
              <i class="color-dot" :style="{ backgroundColor: row.color || '#d7e4e5' }"></i>
              {{ row.color || '未设置' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="suggestion" label="建议动作" min-width="260" show-overflow-tooltip />
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">
            {{ formatDate(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="removeLevel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑分诊等级' : '新增分诊等级'"
      width="760px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="等级名称" prop="name">
            <el-input v-model="form.name" maxlength="50" placeholder="例如：立即急诊" />
          </el-form-item>
          <el-form-item label="等级编码" prop="code">
            <el-input v-model="form.code" maxlength="50" placeholder="例如：EMERGENCY" />
          </el-form-item>
          <el-form-item label="优先级" prop="priority">
            <el-input-number v-model="form.priority" :min="0" :max="999" style="width: 100%" />
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
          <el-form-item label="颜色标识" prop="color">
            <div class="color-input">
              <el-input v-model="form.color" maxlength="20" placeholder="例如：#D03050" />
              <span class="color-preview" :style="{ backgroundColor: form.color || '#d7e4e5' }"></span>
            </div>
          </el-form-item>
        </div>

        <el-form-item label="建议动作" prop="suggestion">
          <el-input
            v-model="form.suggestion"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="例如：建议立即前往急诊科线下就医，不建议继续线上等待"
          />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="用于说明该等级适用的业务场景、风险含义和排序依据"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitLevel">
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
const levels = ref([])

const form = reactive(createEmptyForm())

const rules = {
  name: [
    { required: true, message: '请输入等级名称', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入等级编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '等级编码仅支持字母、数字、下划线和中划线', trigger: ['blur', 'change'] }
  ],
  priority: [
    { required: true, message: '请输入优先级', trigger: 'change' }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const filteredLevels = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return levels.value
  return levels.value.filter(item =>
    [item.name, item.code, item.suggestion, item.description, item.color]
      .filter(Boolean)
      .some(text => String(text).toLowerCase().includes(value))
  )
})

const enabledCount = computed(() => levels.value.filter(item => item.status === 1).length)
const suggestionConfiguredCount = computed(() => levels.value.filter(item => item.suggestion).length)
const topPriorityLevel = computed(() => {
  if (!levels.value.length) return null
  return [...levels.value].sort((a, b) => Number(b.priority || 0) - Number(a.priority || 0))[0]
})
const topPriorityText = computed(() => topPriorityLevel.value ? topPriorityLevel.value.name : '-')
const isEditing = computed(() => !!form.id)

function createEmptyForm() {
  return {
    id: null,
    name: '',
    code: '',
    description: '',
    suggestion: '',
    color: '',
    priority: 0,
    sort: 0,
    status: 1
  }
}

function loadLevels() {
  loading.value = true
  get('/api/admin/triage-level/list', (data) => {
    levels.value = data || []
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

function submitLevel() {
  formRef.value.validate((valid) => {
    if (!valid) return

    const payload = {
      name: form.name.trim(),
      code: form.code.trim(),
      description: normalizeText(form.description),
      suggestion: normalizeText(form.suggestion),
      color: normalizeText(form.color),
      priority: form.priority,
      sort: form.sort,
      status: form.status
    }
    if (isEditing.value) payload.id = form.id

    submitting.value = true
    post(isEditing.value ? '/api/admin/triage-level/update' : '/api/admin/triage-level/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '分诊等级更新成功' : '分诊等级新增成功')
      loadLevels()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '提交失败，请稍后重试')
    })
  })
}

function removeLevel(row) {
  ElMessageBox.confirm(`确认删除分诊等级“${row.name}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/triage-level/delete?id=${row.id}`, () => {
      ElMessage.success('分诊等级删除成功')
      loadLevels()
    }, (message) => {
      ElMessage.warning(message || '分诊等级删除失败')
    })
  }).catch(() => {})
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

onMounted(() => loadLevels())
</script>

<style scoped>
.triage-level-page {
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
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.color-badge,
.color-input {
  display: flex;
  align-items: center;
  gap: 10px;
}

.color-dot,
.color-preview {
  width: 16px;
  height: 16px;
  border-radius: 999px;
  border: 1px solid rgba(14, 58, 63, 0.12);
}

.color-preview {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
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
