<template>
  <div class="doctor-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>医生总数</span>
        <strong>{{ doctors.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>已绑定科室</span>
        <strong>{{ departmentBoundCount }}</strong>
      </article>
      <article class="stat-card">
        <span>已绑定医生账号</span>
        <strong>{{ accountBoundCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索医生姓名、职称、专长、科室或绑定账号"
          style="max-width: 380px"
        />
        <div class="toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增医生</el-button>
        </div>
      </div>

      <el-table :data="filteredDoctors" v-loading="loading" border>
        <el-table-column label="照片" width="90" align="center">
          <template #default="{ row }">
            <el-avatar :size="46" :src="resolveImagePath(row.photo) || undefined">
              {{ row.name?.slice(0, 1) || 'D' }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="医生姓名" min-width="120" />
        <el-table-column prop="title" label="职称" min-width="120" />
        <el-table-column label="所属科室" min-width="140">
          <template #default="{ row }">
            {{ departmentName(row.departmentId) }}
          </template>
        </el-table-column>
        <el-table-column label="绑定账号" min-width="220">
          <template #default="{ row }">
            <div v-if="accountInfo(row.accountId)" class="account-cell">
              <strong>{{ accountInfo(row.accountId).username }}</strong>
              <span>{{ accountInfo(row.accountId).email }}</span>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="expertise" label="专长" min-width="220" show-overflow-tooltip />
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
        <el-table-column prop="introduction" label="介绍" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="removeDoctor(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑医生' : '新增医生'"
      width="820px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="photo-panel">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :show-file-list="false"
            :before-upload="beforePhotoUpload"
            :on-success="handlePhotoSuccess"
            :on-error="handlePhotoError"
            accept="image/*"
            class="photo-uploader"
          >
            <div class="photo-card">
              <el-avatar :size="88" :src="previewPhoto || undefined">
                {{ form.name?.slice(0, 1) || 'D' }}
              </el-avatar>
              <span>点击上传医生照片</span>
            </div>
          </el-upload>
          <el-button v-if="form.photo" link type="danger" @click="form.photo = ''">移除照片</el-button>
        </div>

        <div class="dialog-grid">
          <el-form-item label="医生姓名" prop="name">
            <el-input v-model="form.name" maxlength="50" placeholder="请输入医生姓名" />
          </el-form-item>
          <el-form-item label="职称" prop="title">
            <el-input v-model="form.title" maxlength="50" placeholder="例如：副主任医师" />
          </el-form-item>
          <el-form-item label="所属科室" prop="departmentId">
            <el-select v-model="form.departmentId" style="width: 100%" placeholder="请选择科室">
              <el-option
                v-for="item in departments"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="绑定医生账号">
            <el-select v-model="form.accountId" clearable filterable style="width: 100%" placeholder="可选，不绑定则医生端无法登录工作台">
              <el-option
                v-for="item in availableAccountOptions"
                :key="item.id"
                :label="accountOptionLabel(item)"
                :value="item.id"
              >
                <div class="account-option">
                  <strong>{{ item.username }}</strong>
                  <span>{{ item.email }}</span>
                  <small v-if="item.bindDoctorId && item.bindDoctorId !== form.id">已绑定 {{ item.bindDoctorName }}</small>
                </div>
              </el-option>
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
          <el-form-item label="专长" prop="expertise">
            <el-input v-model="form.expertise" maxlength="500" placeholder="例如：慢病复诊、报告解读、儿童发热咨询" />
          </el-form-item>
        </div>
        <el-form-item label="医生介绍" prop="introduction">
          <el-input
            v-model="form.introduction"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            placeholder="请输入医生介绍"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDoctor">
          {{ isEditing ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { authHeader, backendBaseUrl, get, post, resolveImagePath } from '@/net'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const formRef = ref()
const doctors = ref([])
const departments = ref([])
const accountOptions = ref([])

const form = reactive(createEmptyForm())

const rules = {
  name: [
    { required: true, message: '请输入医生姓名', trigger: 'blur' }
  ],
  departmentId: [
    { required: true, message: '请选择所属科室', trigger: 'change' }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const uploadAction = computed(() => `${backendBaseUrl()}/api/admin/doctor/upload-photo`)
const uploadHeaders = computed(() => authHeader())
const previewPhoto = computed(() => resolveImagePath(form.photo))

const departmentMap = computed(() => {
  const map = new Map()
  departments.value.forEach(item => map.set(item.id, item.name))
  return map
})

const accountMap = computed(() => {
  const map = new Map()
  accountOptions.value.forEach(item => map.set(item.id, item))
  return map
})

const availableAccountOptions = computed(() => accountOptions.value.filter(item => !item.bindDoctorId || item.bindDoctorId === form.id))

const filteredDoctors = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return doctors.value
  return doctors.value.filter(item => [
    item.name,
    item.title,
    item.expertise,
    departmentName(item.departmentId),
    accountInfo(item.accountId)?.username,
    accountInfo(item.accountId)?.email
  ].filter(Boolean).some(text => String(text).toLowerCase().includes(value)))
})

const enabledCount = computed(() => doctors.value.filter(item => item.status === 1).length)
const departmentBoundCount = computed(() => doctors.value.filter(item => !!item.departmentId).length)
const accountBoundCount = computed(() => doctors.value.filter(item => !!item.accountId).length)
const isEditing = computed(() => !!form.id)

function createEmptyForm() {
  return {
    id: null,
    departmentId: null,
    accountId: null,
    name: '',
    title: '',
    photo: '',
    introduction: '',
    expertise: '',
    sort: 0,
    status: 1
  }
}

function loadDepartments() {
  get('/api/admin/department/list', (data) => {
    departments.value = data || []
  })
}

function loadAccountOptions() {
  get('/api/admin/doctor/account-options', (data) => {
    accountOptions.value = data || []
  })
}

function loadDoctors() {
  loading.value = true
  get('/api/admin/doctor/list', (data) => {
    doctors.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function loadData() {
  loadDepartments()
  loadAccountOptions()
  loadDoctors()
}

function openCreateDialog() {
  if (!departments.value.length) {
    ElMessage.warning('请先新增至少一个科室，再维护医生信息')
    return
  }
  Object.assign(form, createEmptyForm())
  dialogVisible.value = true
}

function openEditDialog(row) {
  Object.assign(form, createEmptyForm(), row)
  dialogVisible.value = true
}

function submitDoctor() {
  formRef.value.validate((valid) => {
    if (!valid) return

    const payload = {
      departmentId: form.departmentId,
      accountId: form.accountId || null,
      name: form.name,
      title: form.title,
      photo: form.photo,
      introduction: form.introduction,
      expertise: form.expertise,
      sort: form.sort,
      status: form.status
    }

    if (isEditing.value) {
      payload.id = form.id
    }

    submitting.value = true
    post(isEditing.value ? '/api/admin/doctor/update' : '/api/admin/doctor/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '医生信息更新成功' : '医生新增成功')
      loadData()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '提交失败，请稍后再试')
    })
  })
}

function removeDoctor(row) {
  ElMessageBox.confirm(`确认删除医生“${row.name}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/doctor/delete?id=${row.id}`, () => {
      ElMessage.success('医生删除成功')
      loadData()
    }, (message) => {
      ElMessage.warning(message || '医生删除失败')
    })
  }).catch(() => {})
}

function beforePhotoUpload(file) {
  const isImage = `${file.type || ''}`.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 <= 2

  if (!isImage) {
    ElMessage.error('医生照片必须是图片格式')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('医生照片不能超过 2MB')
    return false
  }
  return true
}

function handlePhotoSuccess(response) {
  if (response?.code !== 200) {
    ElMessage.error(response?.message || '医生照片上传失败')
    return
  }
  form.photo = response.data
  ElMessage.success('医生照片上传成功')
}

function handlePhotoError() {
  ElMessage.error('医生照片上传失败，请稍后再试')
}

function departmentName(id) {
  return departmentMap.value.get(id) || '-'
}

function accountInfo(id) {
  return accountMap.value.get(id) || null
}

function accountOptionLabel(item) {
  const parts = [item.username, item.email].filter(Boolean)
  return parts.join(' / ')
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
.doctor-page {
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

.photo-panel {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.photo-uploader {
  cursor: pointer;
}

.photo-card {
  width: 180px;
  min-height: 132px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  border: 1px dashed rgba(24, 54, 61, 0.18);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.55);
}

.photo-card span {
  color: var(--app-muted);
  font-size: 13px;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.account-cell,
.account-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.account-cell span,
.account-option span,
.account-option small {
  color: var(--app-muted);
}

@media (max-width: 1100px) {
  .stat-grid,
  .dialog-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .stat-grid,
  .dialog-grid {
    grid-template-columns: 1fr;
  }

  .toolbar,
  .photo-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
