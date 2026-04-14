<template>
  <div class="category-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>分类总数</span>
        <strong>{{ categories.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>关联科室</span>
        <strong>{{ linkedDepartmentCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索分类名称、编码、所属科室或说明"
          style="max-width: 360px"
        />
        <div class="toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增问诊分类</el-button>
        </div>
      </div>

      <el-table :data="filteredCategories" v-loading="loading" border>
        <el-table-column prop="name" label="分类名称" min-width="180" />
        <el-table-column prop="code" label="分类编码" min-width="150" />
        <el-table-column label="默认科室" min-width="160">
          <template #default="{ row }">
            {{ departmentName(row.departmentId) }}
          </template>
        </el-table-column>
        <el-table-column label="收费标准" width="120" align="center">
          <template #default="{ row }">
            {{ formatAmount(row.priceAmount) }}
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
        <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="removeCategory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑问诊分类' : '新增问诊分类'"
      width="680px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="分类名称" prop="name">
            <el-input v-model="form.name" maxlength="50" placeholder="例如：图文问诊" />
          </el-form-item>
          <el-form-item label="分类编码" prop="code">
            <el-input v-model="form.code" maxlength="50" placeholder="例如：consult_text" />
          </el-form-item>
          <el-form-item label="默认科室" prop="departmentId">
            <el-select v-model="form.departmentId" filterable style="width: 100%" placeholder="请选择默认科室">
              <el-option
                v-for="item in departments"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="form.sort" :min="0" :max="999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="收费金额" prop="priceAmount">
            <el-input-number
              v-model="form.priceAmount"
              :min="0"
              :step="0.01"
              :precision="2"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option :value="1" label="启用" />
              <el-option :value="0" label="停用" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="分类说明" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="用于说明该问诊入口的适用场景和分诊方向"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCategory">
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
const categories = ref([])
const departments = ref([])

const form = reactive(createEmptyForm())

const rules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入分类编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '分类编码仅支持字母、数字、下划线和中划线', trigger: ['blur', 'change'] }
  ],
  departmentId: [
    { required: true, message: '请选择默认科室', trigger: 'change' }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'change' }
  ],
  priceAmount: [
    { required: true, message: '请输入收费金额', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const departmentMap = computed(() => {
  const map = new Map()
  departments.value.forEach(item => map.set(item.id, item.name))
  return map
})

const filteredCategories = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return categories.value
  return categories.value.filter(item =>
    [item.name, item.code, item.description, departmentName(item.departmentId)]
      .filter(Boolean)
      .some(text => String(text).toLowerCase().includes(value))
  )
})

const enabledCount = computed(() => categories.value.filter(item => item.status === 1).length)
const linkedDepartmentCount = computed(() => new Set(categories.value.map(item => item.departmentId)).size)
const isEditing = computed(() => !!form.id)

function createEmptyForm() {
  return {
    id: null,
    departmentId: null,
    name: '',
    code: '',
    description: '',
    priceAmount: 0,
    sort: 0,
    status: 1
  }
}

function loadData() {
  loadDepartments()
  loadCategories()
}

function loadDepartments() {
  get('/api/admin/department/list', (data) => {
    departments.value = data || []
  })
}

function loadCategories() {
  loading.value = true
  get('/api/admin/consultation-category/list', (data) => {
    categories.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function openCreateDialog() {
  if (!departments.value.length) {
    ElMessage.warning('请先维护科室信息，再新增问诊分类')
    return
  }
  Object.assign(form, createEmptyForm())
  dialogVisible.value = true
}

function openEditDialog(row) {
  Object.assign(form, createEmptyForm(), row)
  dialogVisible.value = true
}

function submitCategory() {
  formRef.value.validate((valid) => {
    if (!valid) return

    const payload = {
      departmentId: form.departmentId,
      name: form.name,
      code: form.code,
      description: form.description,
      priceAmount: form.priceAmount,
      sort: form.sort,
      status: form.status
    }
    if (isEditing.value) payload.id = form.id

    submitting.value = true
    post(isEditing.value ? '/api/admin/consultation-category/update' : '/api/admin/consultation-category/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '问诊分类更新成功' : '问诊分类新增成功')
      loadCategories()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '提交失败，请稍后重试')
    })
  })
}

function removeCategory(row) {
  ElMessageBox.confirm(`确认删除问诊分类“${row.name}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/consultation-category/delete?id=${row.id}`, () => {
      ElMessage.success('问诊分类删除成功')
      loadCategories()
    }, (message) => {
      ElMessage.warning(message || '问诊分类删除失败')
    })
  }).catch(() => {})
}

function departmentName(departmentId) {
  return departmentMap.value.get(departmentId) || '-'
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

function formatAmount(value) {
  const amount = Number(value || 0)
  if (Number.isNaN(amount)) return '¥0.00'
  return `¥${amount.toFixed(2)}`
}

onMounted(() => loadData())
</script>

<style scoped>
.category-page {
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
