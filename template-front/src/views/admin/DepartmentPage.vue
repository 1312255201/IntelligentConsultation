<template>
  <div class="department-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>科室总数</span>
        <strong>{{ departments.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>停用中</span>
        <strong>{{ disabledCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索科室名称、编码或位置"
          style="max-width: 320px"
        />
        <div class="toolbar-actions">
          <el-button @click="loadDepartments">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增科室</el-button>
        </div>
      </div>

      <el-table :data="filteredDepartments" v-loading="loading" border>
        <el-table-column prop="name" label="科室名称" min-width="160" />
        <el-table-column prop="code" label="科室编码" min-width="120" />
        <el-table-column prop="location" label="位置" min-width="160" />
        <el-table-column prop="phone" label="联系电话" min-width="140" />
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
        <el-table-column prop="description" label="简介" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="removeDepartment(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑科室' : '新增科室'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="科室名称" prop="name">
            <el-input v-model="form.name" maxlength="50" placeholder="例如：心内科" />
          </el-form-item>
          <el-form-item label="科室编码" prop="code">
            <el-input v-model="form.code" maxlength="30" placeholder="例如：CARDIOLOGY" />
          </el-form-item>
          <el-form-item label="位置" prop="location">
            <el-input v-model="form.location" maxlength="100" placeholder="例如：门诊楼 3 层" />
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" maxlength="30" placeholder="例如：0755-88888888" />
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
        <el-form-item label="科室简介" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="请输入科室简介"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDepartment">
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
const departments = ref([])

const form = reactive(createEmptyForm())

const rules = {
  name: [
    { required: true, message: '请输入科室名称', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入科室编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '科室编码仅支持字母、数字、下划线和中划线', trigger: ['blur', 'change'] }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const filteredDepartments = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return departments.value
  return departments.value.filter(item =>
    [item.name, item.code, item.location, item.phone]
      .filter(Boolean)
      .some(text => text.toLowerCase().includes(value))
  )
})

const enabledCount = computed(() => departments.value.filter(item => item.status === 1).length)
const disabledCount = computed(() => departments.value.filter(item => item.status !== 1).length)
const isEditing = computed(() => !!form.id)

function createEmptyForm() {
  return {
    id: null,
    name: '',
    code: '',
    description: '',
    location: '',
    phone: '',
    sort: 0,
    status: 1
  }
}

function loadDepartments() {
  loading.value = true
  get('/api/admin/department/list', (data) => {
    departments.value = data || []
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

function submitDepartment() {
  formRef.value.validate((valid) => {
    if (!valid) return

    const payload = {
      name: form.name,
      code: form.code,
      description: form.description,
      location: form.location,
      phone: form.phone,
      sort: form.sort,
      status: form.status
    }

    if (isEditing.value) {
      payload.id = form.id
    }

    submitting.value = true
    post(isEditing.value ? '/api/admin/department/update' : '/api/admin/department/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '科室信息更新成功' : '科室新增成功')
      loadDepartments()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '提交失败，请稍后重试')
    })
  })
}

function removeDepartment(row) {
  ElMessageBox.confirm(`确认删除科室“${row.name}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/department/delete?id=${row.id}`, () => {
      ElMessage.success('科室删除成功')
      loadDepartments()
    }, (message) => {
      ElMessage.warning(message || '科室删除失败')
    })
  }).catch(() => {})
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

onMounted(() => loadDepartments())
</script>

<style scoped>
.department-page {
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
  grid-template-columns: repeat(3, minmax(0, 1fr));
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

@media (max-width: 960px) {
  .stat-grid,
  .dialog-grid {
    grid-template-columns: 1fr;
  }

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
