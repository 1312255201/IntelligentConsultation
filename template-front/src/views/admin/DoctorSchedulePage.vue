<template>
  <div class="schedule-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>排班总数</span>
        <strong>{{ schedules.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>未来 7 天</span>
        <strong>{{ upcomingCount }}</strong>
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
          placeholder="搜索医生、科室、时段或接诊方式"
          style="max-width: 360px"
        />
        <div class="toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增排班</el-button>
        </div>
      </div>

      <el-table :data="filteredSchedules" v-loading="loading" border>
        <el-table-column label="医生" min-width="140">
          <template #default="{ row }">
            {{ doctorName(row.doctorId) }}
          </template>
        </el-table-column>
        <el-table-column label="科室" min-width="130">
          <template #default="{ row }">
            {{ doctorDepartmentName(row.doctorId) }}
          </template>
        </el-table-column>
        <el-table-column label="排班日期" min-width="120">
          <template #default="{ row }">
            {{ row.scheduleDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="时段" min-width="110">
          <template #default="{ row }">
            {{ timePeriodLabel(row.timePeriod) }}
          </template>
        </el-table-column>
        <el-table-column label="接诊方式" min-width="120">
          <template #default="{ row }">
            {{ visitTypeLabel(row.visitType) }}
          </template>
        </el-table-column>
        <el-table-column label="容量" min-width="120" align="center">
          <template #default="{ row }">
            {{ row.usedCapacity }} / {{ row.maxCapacity }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="removeSchedule(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑医生排班' : '新增医生排班'"
      width="760px"
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
          <el-form-item label="排班日期" prop="scheduleDate">
            <el-date-picker
              v-model="form.scheduleDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              placeholder="请选择排班日期"
            />
          </el-form-item>
          <el-form-item label="时段" prop="timePeriod">
            <el-select v-model="form.timePeriod" style="width: 100%">
              <el-option v-for="item in timePeriodOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="接诊方式" prop="visitType">
            <el-select v-model="form.visitType" style="width: 100%">
              <el-option v-for="item in visitTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="最大接诊量" prop="maxCapacity">
            <el-input-number v-model="form.maxCapacity" :min="0" :max="999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="已接诊量" prop="usedCapacity">
            <el-input-number v-model="form.usedCapacity" :min="0" :max="999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option :value="1" label="启用" />
              <el-option :value="0" label="停用" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="例如：优先接复诊、仅限上午线上问诊等"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitSchedule">
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
const schedules = ref([])
const doctors = ref([])
const departments = ref([])

const timePeriodOptions = [
  { label: '上午', value: 'morning' },
  { label: '下午', value: 'afternoon' },
  { label: '晚上', value: 'evening' },
  { label: '全天', value: 'full_day' }
]

const visitTypeOptions = [
  { label: '线上', value: 'online' },
  { label: '线下', value: 'offline' },
  { label: '线上/线下', value: 'both' },
  { label: '复诊随访', value: 'followup' },
  { label: '报告解读', value: 'report' }
]

const form = reactive(createEmptyForm())

const rules = {
  doctorId: [
    { required: true, message: '请选择医生', trigger: 'change' }
  ],
  scheduleDate: [
    { required: true, message: '请选择排班日期', trigger: 'change' }
  ],
  timePeriod: [
    { required: true, message: '请选择时段', trigger: 'change' }
  ],
  visitType: [
    { required: true, message: '请选择接诊方式', trigger: 'change' }
  ],
  maxCapacity: [
    { required: true, message: '请输入最大接诊量', trigger: 'change' }
  ],
  usedCapacity: [
    { required: true, message: '请输入已接诊量', trigger: 'change' }
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

const filteredSchedules = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return schedules.value
  return schedules.value.filter(item =>
    [
      doctorName(item.doctorId),
      doctorDepartmentName(item.doctorId),
      timePeriodLabel(item.timePeriod),
      visitTypeLabel(item.visitType),
      item.scheduleDate,
      item.remark
    ]
      .filter(Boolean)
      .some(text => String(text).toLowerCase().includes(value))
  )
})

const enabledCount = computed(() => schedules.value.filter(item => item.status === 1).length)
const coveredDoctorCount = computed(() => new Set(schedules.value.map(item => item.doctorId)).size)
const upcomingCount = computed(() => {
  const now = new Date()
  const end = new Date(now)
  end.setDate(end.getDate() + 7)
  return schedules.value.filter(item => {
    if (!item.scheduleDate) return false
    const date = new Date(item.scheduleDate)
    return date >= new Date(now.toDateString()) && date <= end
  }).length
})
const isEditing = computed(() => !!form.id)

function createEmptyForm() {
  return {
    id: null,
    doctorId: null,
    scheduleDate: '',
    timePeriod: 'morning',
    visitType: 'both',
    maxCapacity: 10,
    usedCapacity: 0,
    status: 1,
    remark: ''
  }
}

function loadData() {
  loadDepartments()
  loadDoctors()
  loadSchedules()
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

function loadSchedules() {
  loading.value = true
  get('/api/admin/doctor-schedule/list', (data) => {
    schedules.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function openCreateDialog() {
  if (!doctors.value.length) {
    ElMessage.warning('请先新增医生，再维护医生排班')
    return
  }
  Object.assign(form, createEmptyForm())
  dialogVisible.value = true
}

function openEditDialog(row) {
  Object.assign(form, createEmptyForm(), row, {
    scheduleDate: row.scheduleDate || ''
  })
  dialogVisible.value = true
}

function submitSchedule() {
  formRef.value.validate((valid) => {
    if (!valid) return
    if (form.usedCapacity > form.maxCapacity) {
      ElMessage.warning('已接诊量不能大于最大接诊量')
      return
    }

    const payload = {
      doctorId: form.doctorId,
      scheduleDate: form.scheduleDate,
      timePeriod: form.timePeriod,
      visitType: form.visitType,
      maxCapacity: form.maxCapacity,
      usedCapacity: form.usedCapacity,
      status: form.status,
      remark: form.remark
    }
    if (isEditing.value) payload.id = form.id

    submitting.value = true
    post(isEditing.value ? '/api/admin/doctor-schedule/update' : '/api/admin/doctor-schedule/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '医生排班更新成功' : '医生排班新增成功')
      loadSchedules()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '提交失败，请稍后重试')
    })
  })
}

function removeSchedule(row) {
  ElMessageBox.confirm(`确认删除 ${doctorName(row.doctorId)} 在 ${row.scheduleDate} 的排班吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/doctor-schedule/delete?id=${row.id}`, () => {
      ElMessage.success('医生排班删除成功')
      loadSchedules()
    }, (message) => {
      ElMessage.warning(message || '医生排班删除失败')
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

function timePeriodLabel(value) {
  return timePeriodOptions.find(item => item.value === value)?.label || value || '-'
}

function visitTypeLabel(value) {
  return visitTypeOptions.find(item => item.value === value)?.label || value || '-'
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

onMounted(() => loadData())
</script>

<style scoped>
.schedule-page {
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
