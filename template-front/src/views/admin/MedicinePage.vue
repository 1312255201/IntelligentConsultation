<template>
  <div class="medicine-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>药品总数</span>
        <strong>{{ medicines.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>已配禁忌</span>
        <strong>{{ warningConfiguredCount }}</strong>
      </article>
      <article class="stat-card">
        <span>已配冲突</span>
        <strong>{{ interactionConfiguredCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索药品名称、通用名、分类、规格或禁忌"
            style="max-width: 360px"
          />
          <el-select v-model="statusFilter" clearable style="width: 180px" placeholder="全部状态">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="停用" />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadMedicines">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增药品</el-button>
        </div>
      </div>

      <el-table :data="filteredMedicines" v-loading="loading" border>
        <el-table-column prop="name" label="药品名称" min-width="160" />
        <el-table-column prop="genericName" label="通用名" min-width="160" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" min-width="120" show-overflow-tooltip />
        <el-table-column prop="specification" label="规格" min-width="140" show-overflow-tooltip />
        <el-table-column label="禁忌提醒" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ warningPreview(row.warningTexts) || '未配置' }}
          </template>
        </el-table-column>
        <el-table-column label="联用冲突" min-width="180">
          <template #default="{ row }">
            {{ interactionPreview(row.conflictMedicineIds) || '未配置' }}
          </template>
        </el-table-column>
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
            <el-button link type="danger" @click="removeMedicine(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑药品' : '新增药品'"
      width="820px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="药品名称" prop="name">
            <el-input v-model="form.name" maxlength="100" placeholder="例如：头孢克肟片" />
          </el-form-item>
          <el-form-item label="通用名" prop="genericName">
            <el-input v-model="form.genericName" maxlength="100" placeholder="可选" />
          </el-form-item>
          <el-form-item label="分类" prop="categoryName">
            <el-input v-model="form.categoryName" maxlength="50" placeholder="例如：抗菌药物" />
          </el-form-item>
          <el-form-item label="规格" prop="specification">
            <el-input v-model="form.specification" maxlength="100" placeholder="例如：0.1g*6片" />
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

        <el-form-item label="用药禁忌 / 提醒">
          <div class="warning-editor">
            <div
              v-for="(item, index) in form.warningTexts"
              :key="`warning-${index}`"
              class="warning-row"
            >
              <el-input
                v-model="form.warningTexts[index]"
                maxlength="255"
                placeholder="例如：服药期间禁止饮酒；对头孢类过敏者禁用"
              />
              <el-button text type="danger" @click="removeWarning(index)">删除</el-button>
            </div>
            <el-button plain @click="addWarning">新增一条禁忌</el-button>
          </div>
        </el-form-item>

        <el-form-item label="不可同时用药">
          <el-select
            v-model="form.conflictMedicineIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            style="width: 100%"
            placeholder="选择不能与当前药品同时开具的药品"
          >
            <el-option
              v-for="item in conflictOptions"
              :key="item.id"
              :label="medicineLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMedicine">
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
const statusFilter = ref(null)
const formRef = ref()
const medicines = ref([])

const form = reactive(createEmptyForm())

const rules = {
  name: [
    { required: true, message: '请输入药品名称', trigger: 'blur' }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const filteredMedicines = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return medicines.value.filter(item => {
    if (statusFilter.value !== null && item.status !== statusFilter.value) return false
    if (!search) return true
    return [
      item.name,
      item.genericName,
      item.categoryName,
      item.specification,
      ...(Array.isArray(item.warningTexts) ? item.warningTexts : []),
      interactionPreview(item.conflictMedicineIds)
    ]
      .filter(Boolean)
      .some(text => String(text).toLowerCase().includes(search))
  })
})

const enabledCount = computed(() => medicines.value.filter(item => item.status === 1).length)
const warningConfiguredCount = computed(() => medicines.value.filter(item => item.warningTexts?.length).length)
const interactionConfiguredCount = computed(() => medicines.value.filter(item => item.conflictMedicineIds?.length).length)
const isEditing = computed(() => !!form.id)
const medicineMap = computed(() => {
  const map = new Map()
  medicines.value.forEach(item => map.set(item.id, item))
  return map
})
const conflictOptions = computed(() => medicines.value.filter(item => item.id !== form.id))

function createEmptyForm() {
  return {
    id: null,
    name: '',
    genericName: '',
    categoryName: '',
    specification: '',
    warningTexts: [],
    conflictMedicineIds: [],
    sort: 0,
    status: 1
  }
}

function normalizeMedicine(item) {
  return {
    ...item,
    warningTexts: Array.isArray(item?.warningTexts) ? item.warningTexts.filter(Boolean) : [],
    conflictMedicineIds: Array.isArray(item?.conflictMedicineIds)
      ? item.conflictMedicineIds.map(value => Number(value)).filter(value => Number.isInteger(value) && value > 0)
      : [],
    status: Number(item?.status || 0) === 1 ? 1 : 0
  }
}

function loadMedicines() {
  loading.value = true
  get('/api/admin/medicine/list', data => {
    medicines.value = Array.isArray(data) ? data.map(item => normalizeMedicine(item)) : []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function openCreateDialog() {
  Object.assign(form, createEmptyForm())
  dialogVisible.value = true
}

function openEditDialog(item) {
  Object.assign(form, createEmptyForm(), {
    id: item.id,
    name: item.name || '',
    genericName: item.genericName || '',
    categoryName: item.categoryName || '',
    specification: item.specification || '',
    warningTexts: Array.isArray(item.warningTexts) ? [...item.warningTexts] : [],
    conflictMedicineIds: Array.isArray(item.conflictMedicineIds) ? [...item.conflictMedicineIds] : [],
    sort: Number(item.sort || 0),
    status: item.status === 1 ? 1 : 0
  })
  dialogVisible.value = true
}

function addWarning() {
  form.warningTexts.push('')
}

function removeWarning(index) {
  form.warningTexts.splice(index, 1)
}

function warningPreview(values) {
  return Array.isArray(values) && values.length
    ? values.slice(0, 2).join('；')
    : ''
}

function interactionPreview(values) {
  if (!Array.isArray(values) || !values.length) return ''
  const names = values
    .map(id => medicineMap.value.get(id))
    .filter(Boolean)
    .map(item => item.name)
  return names.slice(0, 2).join('、')
}

function medicineLabel(item) {
  return [item.name, item.specification].filter(Boolean).join(' / ')
}

function buildPayload() {
  return {
    ...(form.id ? { id: form.id } : {}),
    name: `${form.name || ''}`.trim(),
    genericName: `${form.genericName || ''}`.trim(),
    categoryName: `${form.categoryName || ''}`.trim(),
    specification: `${form.specification || ''}`.trim(),
    warningTexts: form.warningTexts
      .map(item => `${item || ''}`.trim())
      .filter(Boolean),
    conflictMedicineIds: [...new Set(form.conflictMedicineIds.map(value => Number(value)).filter(value => Number.isInteger(value) && value > 0))],
    sort: Number(form.sort || 0),
    status: form.status === 1 ? 1 : 0
  }
}

function submitMedicine() {
  formRef.value?.validate(valid => {
    if (!valid) return
    const payload = buildPayload()
    submitting.value = true
    post(isEditing.value ? '/api/admin/medicine/update' : '/api/admin/medicine/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '药品已更新' : '药品已新增')
      loadMedicines()
    }, message => {
      submitting.value = false
      ElMessage.warning(message || '药品保存失败')
    })
  })
}

function removeMedicine(item) {
  ElMessageBox.confirm(`确认删除药品“${item.name}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/medicine/delete?id=${item.id}`, () => {
      ElMessage.success('药品已删除')
      loadMedicines()
    })
  }).catch(() => {})
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

onMounted(() => {
  loadMedicines()
})
</script>

<style scoped>
.medicine-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stat-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.stat-card,
.table-card {
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.stat-card {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-card span {
  color: #64748b;
  font-size: 13px;
}

.stat-card strong {
  color: #0f172a;
  font-size: 28px;
  line-height: 1;
}

.table-card {
  padding: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.toolbar-filters,
.toolbar-actions,
.dialog-grid,
.warning-row {
  display: grid;
  gap: 12px;
}

.toolbar-filters,
.toolbar-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.dialog-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.warning-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.warning-row {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

@media (max-width: 768px) {
  .dialog-grid {
    grid-template-columns: 1fr;
  }
}
</style>
