<template>
  <div class="template-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>模板总数</span>
        <strong>{{ templates.length }}</strong>
      </article>
      <article class="stat-card">
        <span>默认模板</span>
        <strong>{{ defaultCount }}</strong>
      </article>
      <article class="stat-card">
        <span>字段配置数</span>
        <strong>{{ totalFieldCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索模板名称、分类、科室或说明"
            style="max-width: 320px"
          />
          <el-select v-model="selectedCategoryId" clearable placeholder="全部问诊分类" style="width: 220px">
            <el-option
              v-for="item in categories"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增前置模板</el-button>
        </div>
      </div>

      <el-table :data="filteredTemplates" v-loading="loading" border>
        <el-table-column prop="name" label="模板名称" min-width="190" />
        <el-table-column label="问诊分类" min-width="180">
          <template #default="{ row }">
            {{ categoryName(row.categoryId) }}
          </template>
        </el-table-column>
        <el-table-column label="所属科室" min-width="160">
          <template #default="{ row }">
            {{ departmentNameByCategory(row.categoryId) }}
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="90" align="center" />
        <el-table-column label="默认模板" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isDefault === 1 ? 'success' : 'info'" effect="light">
              {{ row.isDefault === 1 ? '默认' : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fieldCount" label="字段数" width="90" align="center" />
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
            <el-button link type="danger" @click="removeTemplate(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑前置模板' : '新增前置模板'"
      width="1100px"
      top="4vh"
      destroy-on-close
    >
      <div v-loading="detailLoading">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="dialog-grid">
            <el-form-item label="模板名称" prop="name">
              <el-input v-model="form.name" maxlength="100" placeholder="例如：图文问诊标准模板" />
            </el-form-item>
            <el-form-item label="问诊分类" prop="categoryId">
              <el-select v-model="form.categoryId" filterable style="width: 100%" placeholder="请选择问诊分类">
                <el-option
                  v-for="item in categories"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="版本号" prop="version">
              <el-input-number v-model="form.version" :min="1" :max="999" style="width: 100%" />
            </el-form-item>
            <el-form-item label="模板类型" prop="isDefault">
              <el-select v-model="form.isDefault" style="width: 100%">
                <el-option :value="1" label="默认模板" />
                <el-option :value="0" label="普通模板" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option :value="1" label="启用" />
                <el-option :value="0" label="停用" />
              </el-select>
            </el-form-item>
          </div>

          <el-form-item label="模板说明" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              maxlength="255"
              show-word-limit
              placeholder="用于说明该模板的适用对象、采集重点和使用条件"
            />
          </el-form-item>
        </el-form>

        <div class="field-panel">
          <div class="field-panel-header">
            <div>
              <h3>采集字段配置</h3>
              <p>用于定义发起问诊前需要用户填写的结构化资料。</p>
            </div>
            <el-button type="primary" plain @click="addField">新增字段</el-button>
          </div>

          <div class="field-list">
            <article
              v-for="(field, index) in form.fields"
              :key="`${field.id || 'new'}-${index}`"
              class="field-card"
            >
              <div class="field-card-header">
                <strong>字段 {{ index + 1 }}</strong>
                <el-button link type="danger" @click="removeField(index)">移除</el-button>
              </div>

              <div class="field-grid">
                <el-form-item :label="`字段名称 ${index + 1}`" required>
                  <el-input v-model="field.fieldLabel" maxlength="100" placeholder="例如：主要症状" />
                </el-form-item>
                <el-form-item :label="`字段编码 ${index + 1}`" required>
                  <el-input v-model="field.fieldCode" maxlength="50" placeholder="例如：chief_complaint" />
                </el-form-item>
                <el-form-item label="字段类型" required>
                  <el-select v-model="field.fieldType" style="width: 100%">
                    <el-option
                      v-for="item in fieldTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="排序">
                  <el-input-number v-model="field.sort" :min="0" :max="999" style="width: 100%" />
                </el-form-item>
                <el-form-item label="是否必填">
                  <el-select v-model="field.isRequired" style="width: 100%">
                    <el-option :value="1" label="必填" />
                    <el-option :value="0" label="选填" />
                  </el-select>
                </el-form-item>
                <el-form-item label="字段状态">
                  <el-select v-model="field.status" style="width: 100%">
                    <el-option :value="1" label="启用" />
                    <el-option :value="0" label="停用" />
                  </el-select>
                </el-form-item>
                <el-form-item label="占位提示">
                  <el-input v-model="field.placeholder" maxlength="255" placeholder="请输入提示文案" />
                </el-form-item>
                <el-form-item label="默认值">
                  <el-input v-model="field.defaultValue" maxlength="255" placeholder="可选" />
                </el-form-item>
                <el-form-item label="帮助文案">
                  <el-input v-model="field.helpText" maxlength="255" placeholder="字段补充说明" />
                </el-form-item>
                <el-form-item label="显示条件">
                  <el-input v-model="field.conditionRule" maxlength="255" placeholder="例如：gender=female" />
                </el-form-item>
                <el-form-item label="校验规则">
                  <el-input v-model="field.validationRule" maxlength="255" placeholder="例如：length<=200" />
                </el-form-item>
              </div>

              <el-form-item v-if="requiresOptions(field.fieldType)" label="选项配置" required>
                <el-input
                  v-model="field.optionsText"
                  type="textarea"
                  :rows="4"
                  placeholder="每行一个选项，例如：&#10;发热&#10;咳嗽&#10;腹痛"
                />
              </el-form-item>
            </article>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTemplate">
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

const fieldTypeOptions = [
  { value: 'input', label: '单行输入' },
  { value: 'textarea', label: '多行输入' },
  { value: 'single_select', label: '单选' },
  { value: 'multi_select', label: '多选' },
  { value: 'date', label: '日期' },
  { value: 'number', label: '数字' },
  { value: 'upload', label: '上传' },
  { value: 'switch', label: '开关' }
]

const loading = ref(false)
const submitting = ref(false)
const detailLoading = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const selectedCategoryId = ref(null)
const formRef = ref()
const templates = ref([])
const categories = ref([])
const departments = ref([])

const form = reactive(createEmptyForm())

const rules = {
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择问诊分类', trigger: 'change' }
  ],
  version: [
    { required: true, message: '请输入版本号', trigger: 'change' }
  ],
  isDefault: [
    { required: true, message: '请选择模板类型', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const categoryMap = computed(() => {
  const map = new Map()
  categories.value.forEach(item => map.set(item.id, item))
  return map
})

const departmentMap = computed(() => {
  const map = new Map()
  departments.value.forEach(item => map.set(item.id, item.name))
  return map
})

const filteredTemplates = computed(() => {
  const keywordValue = keyword.value.trim().toLowerCase()
  return templates.value.filter(item => {
    if (selectedCategoryId.value && item.categoryId !== selectedCategoryId.value) return false
    if (!keywordValue) return true
    return [
      item.name,
      item.description,
      categoryName(item.categoryId),
      departmentNameByCategory(item.categoryId)
    ]
      .filter(Boolean)
      .some(text => String(text).toLowerCase().includes(keywordValue))
  })
})

const defaultCount = computed(() => templates.value.filter(item => item.isDefault === 1).length)
const totalFieldCount = computed(() => templates.value.reduce((sum, item) => sum + Number(item.fieldCount || 0), 0))
const isEditing = computed(() => !!form.id)

function createEmptyField(sort = 0) {
  return {
    id: null,
    fieldCode: '',
    fieldLabel: '',
    fieldType: 'input',
    isRequired: 0,
    optionsText: '',
    defaultValue: '',
    placeholder: '',
    helpText: '',
    conditionRule: '',
    validationRule: '',
    sort,
    status: 1
  }
}

function createEmptyForm() {
  return {
    id: null,
    categoryId: null,
    name: '',
    description: '',
    version: 1,
    isDefault: 1,
    status: 1,
    fields: [createEmptyField()]
  }
}

function loadData() {
  loadDepartments()
  loadCategories()
  loadTemplates()
}

function loadDepartments() {
  get('/api/admin/department/list', (data) => {
    departments.value = data || []
  })
}

function loadCategories() {
  get('/api/admin/consultation-category/list', (data) => {
    categories.value = data || []
  })
}

function loadTemplates() {
  loading.value = true
  get('/api/admin/consultation-template/list', (data) => {
    templates.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function openCreateDialog() {
  if (!categories.value.length) {
    ElMessage.warning('请先维护问诊分类，再新增前置模板')
    return
  }
  Object.assign(form, createEmptyForm(), {
    categoryId: selectedCategoryId.value || null,
    fields: [createEmptyField()]
  })
  dialogVisible.value = true
}

function openEditDialog(row) {
  dialogVisible.value = true
  detailLoading.value = true
  get(`/api/admin/consultation-template/detail?id=${row.id}`, (data) => {
    Object.assign(form, createEmptyForm(), {
      id: data.id,
      categoryId: data.categoryId,
      name: data.name,
      description: data.description || '',
      version: data.version,
      isDefault: data.isDefault,
      status: data.status,
      fields: (data.fields || []).map((item, index) => ({
        id: item.id,
        fieldCode: item.fieldCode,
        fieldLabel: item.fieldLabel,
        fieldType: item.fieldType,
        isRequired: item.isRequired,
        optionsText: decodeOptionsText(item.optionsJson),
        defaultValue: item.defaultValue || '',
        placeholder: item.placeholder || '',
        helpText: item.helpText || '',
        conditionRule: item.conditionRule || '',
        validationRule: item.validationRule || '',
        sort: item.sort ?? index,
        status: item.status
      }))
    })
    detailLoading.value = false
  }, (message) => {
    detailLoading.value = false
    dialogVisible.value = false
    ElMessage.warning(message || '模板详情加载失败')
  })
}

function addField() {
  const nextSort = form.fields.length ? Math.max(...form.fields.map(item => Number(item.sort || 0))) + 1 : 0
  form.fields.push(createEmptyField(nextSort))
}

function removeField(index) {
  if (form.fields.length <= 1) {
    ElMessage.warning('请至少保留一个采集字段')
    return
  }
  form.fields.splice(index, 1)
}

function submitTemplate() {
  formRef.value.validate((valid) => {
    if (!valid) return

    const manualMessage = validateTemplateForm()
    if (manualMessage) {
      ElMessage.warning(manualMessage)
      return
    }

    const payload = {
      categoryId: form.categoryId,
      name: form.name.trim(),
      description: normalizeText(form.description),
      version: form.version,
      isDefault: form.isDefault,
      status: form.status,
      fields: form.fields.map((field, index) => ({
        id: field.id,
        fieldCode: field.fieldCode.trim(),
        fieldLabel: field.fieldLabel.trim(),
        fieldType: field.fieldType,
        isRequired: field.isRequired,
        optionsJson: requiresOptions(field.fieldType) ? JSON.stringify(parseOptionsText(field.optionsText)) : '',
        defaultValue: normalizeText(field.defaultValue),
        placeholder: normalizeText(field.placeholder),
        helpText: normalizeText(field.helpText),
        conditionRule: normalizeText(field.conditionRule),
        validationRule: normalizeText(field.validationRule),
        sort: Number(field.sort ?? index),
        status: field.status
      }))
    }
    if (isEditing.value) payload.id = form.id

    submitting.value = true
    post(isEditing.value ? '/api/admin/consultation-template/update' : '/api/admin/consultation-template/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '前置模板更新成功' : '前置模板新增成功')
      loadTemplates()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '提交失败，请稍后重试')
    })
  })
}

function removeTemplate(row) {
  ElMessageBox.confirm(`确认删除前置模板“${row.name}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/consultation-template/delete?id=${row.id}`, () => {
      ElMessage.success('前置模板删除成功')
      loadTemplates()
    }, (message) => {
      ElMessage.warning(message || '前置模板删除失败')
    })
  }).catch(() => {})
}

function validateTemplateForm() {
  if (!form.fields.length) return '请至少配置一个采集字段'

  const codes = new Set()
  for (let index = 0; index < form.fields.length; index++) {
    const field = form.fields[index]
    if (!field.fieldLabel.trim()) return `请完善第 ${index + 1} 个字段的名称`
    if (!field.fieldCode.trim()) return `请完善第 ${index + 1} 个字段的编码`
    if (!/^[A-Za-z0-9_-]+$/.test(field.fieldCode.trim())) {
      return `第 ${index + 1} 个字段编码仅支持字母、数字、下划线和中划线`
    }
    const normalizedCode = field.fieldCode.trim().toLowerCase()
    if (codes.has(normalizedCode)) return `第 ${index + 1} 个字段编码重复，请调整后再提交`
    codes.add(normalizedCode)

    if (requiresOptions(field.fieldType) && !parseOptionsText(field.optionsText).length) {
      return `第 ${index + 1} 个字段需要至少配置一个选项`
    }
  }
  return ''
}

function parseOptionsText(value) {
  return String(value || '')
    .split('\n')
    .map(item => item.trim())
    .filter(Boolean)
}

function decodeOptionsText(optionsJson) {
  if (!optionsJson) return ''
  try {
    const parsed = JSON.parse(optionsJson)
    return Array.isArray(parsed) ? parsed.join('\n') : ''
  } catch {
    return ''
  }
}

function requiresOptions(fieldType) {
  return fieldType === 'single_select' || fieldType === 'multi_select'
}

function normalizeText(value) {
  const text = String(value || '').trim()
  return text || ''
}

function categoryName(categoryId) {
  return categoryMap.value.get(categoryId)?.name || '-'
}

function departmentNameByCategory(categoryId) {
  const category = categoryMap.value.get(categoryId)
  return category ? departmentMap.value.get(category.departmentId) || '-' : '-'
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
.template-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stat-card,
.table-card,
.field-card {
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

.stat-card span,
.field-panel-header p {
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
.toolbar-actions,
.field-panel-header,
.field-card-header {
  display: flex;
  gap: 12px;
}

.toolbar,
.field-panel-header,
.field-card-header {
  justify-content: space-between;
  align-items: center;
}

.toolbar {
  margin-bottom: 18px;
}

.toolbar-filters {
  flex-wrap: wrap;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.field-panel {
  margin-top: 8px;
}

.field-panel-header h3 {
  margin: 0;
  font-size: 18px;
}

.field-panel-header p {
  margin: 6px 0 0;
}

.field-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 18px;
}

.field-card {
  padding: 18px 20px;
}

.field-card-header {
  margin-bottom: 12px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 1080px) {
  .dialog-grid,
  .field-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .stat-grid,
  .dialog-grid,
  .field-grid {
    grid-template-columns: 1fr;
  }

  .toolbar,
  .field-panel-header,
  .field-card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
