<template>
  <div class="tag-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>标签总数</span>
        <strong>{{ tags.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>覆盖医生</span>
        <strong>{{ coveredDoctorCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索医生、科室、标签编码或标签名称"
          style="max-width: 360px"
        />
        <div class="toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增服务标签</el-button>
        </div>
      </div>

      <el-table :data="filteredTags" v-loading="loading" border>
        <el-table-column label="医生" min-width="150">
          <template #default="{ row }">
            {{ doctorName(row.doctorId) }}
          </template>
        </el-table-column>
        <el-table-column label="科室" min-width="140">
          <template #default="{ row }">
            {{ doctorDepartmentName(row.doctorId) }}
          </template>
        </el-table-column>
        <el-table-column prop="tagCode" label="标签编码" min-width="150" />
        <el-table-column prop="tagName" label="标签名称" min-width="180" />
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
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="removeTag(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑服务标签' : '新增服务标签'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="医生" prop="doctorId">
            <el-select v-model="form.doctorId" filterable style="width: 100%" placeholder="请选择医生">
              <el-option
                v-for="item in doctors"
                :key="item.id"
                :label="`${item.name} · ${departmentName(item.departmentId)}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标签编码" prop="tagCode">
            <el-input v-model="form.tagCode" maxlength="50" placeholder="例如：respiratory_followup" />
          </el-form-item>
          <el-form-item label="标签名称" prop="tagName">
            <el-input v-model="form.tagName" maxlength="100" placeholder="例如：呼吸系统随访" />
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
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTag">
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
const tags = ref([])
const doctors = ref([])
const departments = ref([])

const form = reactive(createEmptyForm())

const rules = {
  doctorId: [
    { required: true, message: '请选择医生', trigger: 'change' }
  ],
  tagCode: [
    { required: true, message: '请输入标签编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '标签编码仅支持字母、数字、下划线和中划线', trigger: ['blur', 'change'] }
  ],
  tagName: [
    { required: true, message: '请输入标签名称', trigger: 'blur' }
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
  departments.value.forEach(item => map.set(item.id, item.name))
  return map
})

const doctorMap = computed(() => {
  const map = new Map()
  doctors.value.forEach(item => map.set(item.id, item))
  return map
})

const filteredTags = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return tags.value
  return tags.value.filter(item =>
    [
      item.tagCode,
      item.tagName,
      doctorName(item.doctorId),
      doctorDepartmentName(item.doctorId)
    ]
      .filter(Boolean)
      .some(text => String(text).toLowerCase().includes(value))
  )
})

const enabledCount = computed(() => tags.value.filter(item => item.status === 1).length)
const coveredDoctorCount = computed(() => new Set(tags.value.map(item => item.doctorId)).size)
const isEditing = computed(() => !!form.id)

function createEmptyForm() {
  return {
    id: null,
    doctorId: null,
    tagCode: '',
    tagName: '',
    sort: 0,
    status: 1
  }
}

function loadData() {
  loadDepartments()
  loadDoctors()
  loadTags()
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

function loadTags() {
  loading.value = true
  get('/api/admin/doctor-service-tag/list', (data) => {
    tags.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function openCreateDialog() {
  if (!doctors.value.length) {
    ElMessage.warning('请先新增医生，再维护服务标签')
    return
  }
  Object.assign(form, createEmptyForm())
  dialogVisible.value = true
}

function openEditDialog(row) {
  Object.assign(form, createEmptyForm(), row)
  dialogVisible.value = true
}

function submitTag() {
  formRef.value.validate((valid) => {
    if (!valid) return

    const payload = {
      doctorId: form.doctorId,
      tagCode: form.tagCode,
      tagName: form.tagName,
      sort: form.sort,
      status: form.status
    }
    if (isEditing.value) payload.id = form.id

    submitting.value = true
    post(isEditing.value ? '/api/admin/doctor-service-tag/update' : '/api/admin/doctor-service-tag/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '服务标签更新成功' : '服务标签新增成功')
      loadTags()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '提交失败，请稍后重试')
    })
  })
}

function removeTag(row) {
  ElMessageBox.confirm(`确认删除服务标签“${row.tagName}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/doctor-service-tag/delete?id=${row.id}`, () => {
      ElMessage.success('服务标签删除成功')
      loadTags()
    }, (message) => {
      ElMessage.warning(message || '服务标签删除失败')
    })
  }).catch(() => {})
}

function doctorName(doctorId) {
  return doctorMap.value.get(doctorId)?.name || '-'
}

function doctorDepartmentName(doctorId) {
  const doctor = doctorMap.value.get(doctorId)
  return doctor ? departmentName(doctor.departmentId) : '-'
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

onMounted(() => loadData())
</script>

<style scoped>
.tag-page {
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
