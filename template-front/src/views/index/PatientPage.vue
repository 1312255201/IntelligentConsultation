<template>
  <div class="patient-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>就诊人总数</span>
        <strong>{{ patients.length }}</strong>
      </article>
      <article class="stat-card">
        <span>默认就诊人</span>
        <strong>{{ defaultPatientName }}</strong>
      </article>
      <article class="stat-card">
        <span>本人档案</span>
        <strong>{{ selfProfileCount }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
    </section>

    <section class="panel-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索姓名、手机号或证件号"
          style="max-width: 320px"
        />
        <div class="toolbar-actions">
          <el-button @click="loadPatients">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增就诊人</el-button>
        </div>
      </div>

      <div v-loading="loading">
        <el-empty v-if="!filteredPatients.length" :description="emptyDescription">
          <el-button type="primary" @click="openCreateDialog">新增就诊人</el-button>
        </el-empty>

        <div v-else class="patient-grid">
          <article v-for="item in filteredPatients" :key="item.id" class="patient-card">
            <div class="patient-head">
              <div>
                <strong>{{ item.name }}</strong>
                <p>{{ patientMeta(item) }}</p>
              </div>
              <div class="tag-row">
                <el-tag v-if="item.isDefault === 1" type="success" effect="light">默认</el-tag>
                <el-tag v-if="item.isSelf === 1" effect="light">本人</el-tag>
                <el-tag :type="item.status === 1 ? 'success' : 'info'" effect="light">
                  {{ item.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </div>
            </div>

            <div class="info-grid">
              <div>
                <span>出生日期</span>
                <strong>{{ item.birthDate || '-' }}</strong>
              </div>
              <div>
                <span>联系电话</span>
                <strong>{{ item.phone || '-' }}</strong>
              </div>
              <div>
                <span>证件号</span>
                <strong>{{ item.idCard || '-' }}</strong>
              </div>
              <div>
                <span>更新时间</span>
                <strong>{{ formatDateTime(item.updateTime) }}</strong>
              </div>
            </div>

            <p class="remark-text">{{ item.remark || '该就诊人档案可用于后续 AI 导诊、问诊和预约流程。' }}</p>

            <div class="action-row">
              <el-button link @click="goHealthPage(item)">健康档案</el-button>
              <el-button link type="primary" @click="openEditDialog(item)">编辑</el-button>
              <el-button link type="danger" @click="removePatient(item)">删除</el-button>
            </div>
          </article>
        </div>
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑就诊人' : '新增就诊人'"
      width="720px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" maxlength="50" placeholder="请输入就诊人姓名" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-select v-model="form.gender" style="width: 100%">
              <el-option v-for="item in genderOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="出生日期" prop="birthDate">
            <el-date-picker
              v-model="form.birthDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择出生日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" maxlength="20" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="证件号" prop="idCard">
            <el-input v-model="form.idCard" maxlength="30" placeholder="请输入证件号，可选填" />
          </el-form-item>
          <el-form-item label="与账号关系" prop="relationType">
            <el-select v-model="form.relationType" style="width: 100%">
              <el-option v-for="item in relationOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="默认就诊人" prop="isDefault">
            <el-select v-model="form.isDefault" style="width: 100%">
              <el-option :value="1" label="是" />
              <el-option :value="0" label="否" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option :value="1" label="启用" />
              <el-option :value="0" label="停用" />
            </el-select>
          </el-form-item>
        </div>

        <el-alert
          v-if="form.relationType === 'self'"
          type="info"
          :closable="false"
          show-icon
          title="一个账号仅能维护一个“本人”就诊人档案。"
          class="relation-alert"
        />

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="例如：常用就诊人、儿童问诊档案等"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPatient">
          {{ isEditing ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { get, post } from '@/net'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const formRef = ref()
const patients = ref([])

const genderOptions = [
  { label: '男', value: 'male' },
  { label: '女', value: 'female' },
  { label: '暂不说明', value: 'unknown' }
]

const relationOptions = [
  { label: '本人', value: 'self' },
  { label: '子女', value: 'child' },
  { label: '父母', value: 'parent' },
  { label: '配偶', value: 'spouse' },
  { label: '其他', value: 'other' }
]

const form = reactive(createEmptyForm())

const rules = {
  name: [
    { required: true, message: '请输入就诊人姓名', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  phone: [
    { pattern: /(^$|^[0-9+\-\s]{6,20}$)/, message: '联系电话格式不正确', trigger: ['blur', 'change'] }
  ],
  relationType: [
    { required: true, message: '请选择与账号关系', trigger: 'change' }
  ],
  isDefault: [
    { required: true, message: '请选择是否默认就诊人', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const filteredPatients = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return patients.value
  return patients.value.filter(item =>
    [item.name, item.phone, item.idCard, item.remark]
      .filter(Boolean)
      .some(text => String(text).toLowerCase().includes(value))
  )
})

const isEditing = computed(() => !!form.id)
const enabledCount = computed(() => patients.value.filter(item => item.status === 1).length)
const selfProfileCount = computed(() => patients.value.filter(item => item.isSelf === 1).length)
const defaultPatientName = computed(() => patients.value.find(item => item.isDefault === 1)?.name || '-')
const emptyDescription = computed(() => keyword.value.trim()
  ? '没有匹配到符合条件的就诊人'
  : '还没有就诊人档案，建议先补充本人或家属档案'
)

function createEmptyForm() {
  return {
    id: null,
    name: '',
    gender: 'unknown',
    birthDate: '',
    phone: '',
    idCard: '',
    relationType: 'self',
    isDefault: 1,
    remark: '',
    status: 1
  }
}

function loadPatients() {
  loading.value = true
  get('/api/user/patient/list', (data) => {
    patients.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function openCreateDialog() {
  const hasSelfProfile = patients.value.some(item => item.isSelf === 1)
  Object.assign(form, createEmptyForm(), {
    relationType: hasSelfProfile ? 'child' : 'self',
    isDefault: patients.value.length ? 0 : 1
  })
  dialogVisible.value = true
}

function openEditDialog(row) {
  Object.assign(form, createEmptyForm(), row, {
    birthDate: row.birthDate || ''
  })
  dialogVisible.value = true
}

function submitPatient() {
  formRef.value.validate((valid) => {
    if (!valid) return

    const payload = {
      name: form.name,
      gender: form.gender,
      birthDate: form.birthDate || null,
      phone: form.phone,
      idCard: form.idCard,
      relationType: form.relationType,
      isDefault: form.isDefault,
      remark: form.remark,
      status: form.status
    }

    if (isEditing.value) {
      payload.id = form.id
    }

    submitting.value = true
    post(isEditing.value ? '/api/user/patient/update' : '/api/user/patient/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '就诊人档案更新成功' : '就诊人档案新增成功')
      loadPatients()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '提交失败，请稍后重试')
    })
  })
}

function removePatient(row) {
  ElMessageBox.confirm(`确认删除就诊人“${row.name}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/user/patient/delete?patientId=${row.id}`, () => {
      ElMessage.success('就诊人删除成功')
      loadPatients()
    }, (message) => {
      ElMessage.warning(message || '就诊人删除失败')
    })
  }).catch(() => {})
}

function goHealthPage(row) {
  router.push({
    path: '/index/health',
    query: { patientId: String(row.id) }
  })
}

function patientMeta(item) {
  const parts = [relationLabel(item.relationType), genderLabel(item.gender)]
  if (item.age !== null && item.age !== undefined) {
    parts.push(`${item.age} 岁`)
  }
  return parts.join(' · ')
}

function genderLabel(value) {
  return genderOptions.find(item => item.value === value)?.label || '未知'
}

function relationLabel(value) {
  return relationOptions.find(item => item.value === value)?.label || '其他'
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

onMounted(() => loadPatients())
</script>

<style scoped>
.patient-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-card,
.stat-card,
.patient-card {
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
  font-size: 28px;
}

.panel-card {
  padding: 22px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.toolbar-actions,
.tag-row,
.action-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.patient-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.patient-card {
  padding: 22px;
}

.patient-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.patient-head strong {
  display: block;
  font-size: 24px;
}

.patient-head p {
  margin: 10px 0 0;
  color: var(--app-muted);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.info-grid div {
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(19, 73, 80, 0.04);
}

.info-grid span {
  display: block;
  color: var(--app-muted);
  font-size: 13px;
}

.info-grid strong {
  display: block;
  margin-top: 8px;
  font-size: 15px;
  line-height: 1.6;
  word-break: break-all;
}

.remark-text {
  margin: 18px 0 0;
  color: #5f747c;
  line-height: 1.8;
}

.action-row {
  margin-top: 14px;
  justify-content: flex-end;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.relation-alert {
  margin-bottom: 18px;
}

@media (max-width: 1100px) {
  .stat-grid,
  .patient-grid,
  .dialog-grid,
  .info-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .toolbar,
  .patient-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
