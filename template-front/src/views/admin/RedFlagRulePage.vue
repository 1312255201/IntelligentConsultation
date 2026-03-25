<template>
  <div class="red-flag-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>规则总数</span>
        <strong>{{ rules.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>立即急诊动作</span>
        <strong>{{ emergencyActionCount }}</strong>
      </article>
      <article class="stat-card">
        <span>组合规则</span>
        <strong>{{ combinationCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索规则名称、编码、触发条件或建议动作"
            style="max-width: 360px"
          />
          <el-select v-model="selectedTriggerType" clearable style="width: 220px" placeholder="全部触发方式">
            <el-option
              v-for="item in triggerTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增红旗规则</el-button>
        </div>
      </div>

      <el-table :data="filteredRules" v-loading="loading" border>
        <el-table-column prop="ruleName" label="规则名称" min-width="180" />
        <el-table-column prop="ruleCode" label="规则编码" min-width="160" />
        <el-table-column label="触发方式" min-width="120" align="center">
          <template #default="{ row }">
            <el-tag effect="light">{{ triggerTypeLabel(row.triggerType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="触发条件" min-width="320" show-overflow-tooltip>
          <template #default="{ row }">
            {{ ruleConditionSummary(row) }}
          </template>
        </el-table-column>
        <el-table-column label="分诊等级" min-width="140">
          <template #default="{ row }">
            {{ triageLevelName(row.triageLevelId) }}
          </template>
        </el-table-column>
        <el-table-column label="建议动作" min-width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="actionTagType(row.actionType)" effect="light">
              {{ actionTypeLabel(row.actionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" align="center" />
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
            <el-button link type="danger" @click="removeRule(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑红旗规则' : '新增红旗规则'"
      width="920px"
      destroy-on-close
    >
      <el-alert
        :title="triggerTypeTip"
        type="info"
        :closable="false"
        class="dialog-alert"
      />

      <el-form ref="formRef" :model="form" :rules="rulesForm" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="规则名称" prop="ruleName">
            <el-input v-model="form.ruleName" maxlength="100" placeholder="例如：胸痛伴呼吸困难" />
          </el-form-item>
          <el-form-item label="规则编码" prop="ruleCode">
            <el-input v-model="form.ruleCode" maxlength="50" placeholder="例如：CHEST_PAIN_DYSPNEA" />
          </el-form-item>
          <el-form-item label="触发方式" prop="triggerType">
            <el-select v-model="form.triggerType" style="width: 100%">
              <el-option
                v-for="item in triggerTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="身体部位" prop="bodyPartId">
            <el-select
              v-model="form.bodyPartId"
              clearable
              filterable
              style="width: 100%"
              placeholder="可选，用于限定规则适用部位"
            >
              <el-option
                v-for="item in bodyParts"
                :key="item.id"
                :label="bodyPartPath(item.id)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="分诊等级" prop="triageLevelId">
            <el-select v-model="form.triageLevelId" style="width: 100%" placeholder="请选择分诊等级">
              <el-option
                v-for="item in triageLevels"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="建议动作" prop="actionType">
            <el-select v-model="form.actionType" style="width: 100%">
              <el-option
                v-for="item in actionTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级" prop="priority">
            <el-input-number v-model="form.priority" :min="0" :max="999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option :value="1" label="启用" />
              <el-option :value="0" label="停用" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="匹配症状" prop="symptomIds">
          <el-select
            v-model="form.symptomIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            clearable
            style="width: 100%"
            placeholder="可选，多选时会参与规则匹配"
          >
            <el-option
              v-for="item in availableSymptoms"
              :key="item.id"
              :label="`${item.name} / ${bodyPartPath(item.bodyPartId)}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词模式" prop="keywordPattern">
          <el-input
            v-model="form.keywordPattern"
            maxlength="255"
            placeholder="例如：高热|39.5|抽搐|意识不清"
          />
        </el-form-item>
        <el-form-item label="条件说明" prop="conditionDescription">
          <el-input
            v-model="form.conditionDescription"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="用于说明该规则的适用场景和判断逻辑，方便运营复盘"
          />
        </el-form-item>
        <el-form-item label="分诊建议" prop="suggestion">
          <el-input
            v-model="form.suggestion"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="例如：建议尽快线下就医，如伴明显加重请直接前往急诊"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRule">
          {{ isEditing ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { get, post } from '@/net'

const triggerTypeOptions = [
  { label: '症状匹配', value: 'symptom_match' },
  { label: '关键词匹配', value: 'keyword_match' },
  { label: '部位匹配', value: 'body_part_match' },
  { label: '组合规则', value: 'combination' }
]

const actionTypeOptions = [
  { label: '立即急诊', value: 'emergency' },
  { label: '线下就医', value: 'offline' },
  { label: '在线咨询', value: 'online' },
  { label: '居家观察', value: 'observe' }
]

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const selectedTriggerType = ref('')
const formRef = ref()
const rules = ref([])
const bodyParts = ref([])
const symptoms = ref([])
const triageLevels = ref([])

const form = reactive(createEmptyForm())

const rulesForm = {
  ruleName: [
    { required: true, message: '请输入规则名称', trigger: 'blur' }
  ],
  ruleCode: [
    { required: true, message: '请输入规则编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '规则编码仅支持字母、数字、下划线和中划线', trigger: ['blur', 'change'] }
  ],
  triggerType: [
    { required: true, message: '请选择触发方式', trigger: 'change' }
  ],
  triageLevelId: [
    { required: true, message: '请选择分诊等级', trigger: 'change' }
  ],
  actionType: [
    { required: true, message: '请选择建议动作', trigger: 'change' }
  ],
  priority: [
    { required: true, message: '请输入优先级', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ],
  suggestion: [
    { required: true, message: '请输入分诊建议', trigger: 'blur' }
  ]
}

const bodyPartMap = computed(() => {
  const map = new Map()
  bodyParts.value.forEach(item => map.set(item.id, item))
  return map
})

const symptomMap = computed(() => {
  const map = new Map()
  symptoms.value.forEach(item => map.set(item.id, item))
  return map
})

const triageLevelMap = computed(() => {
  const map = new Map()
  triageLevels.value.forEach(item => map.set(item.id, item))
  return map
})

const availableSymptoms = computed(() => {
  if (!form.bodyPartId) return symptoms.value
  return symptoms.value.filter(item => item.bodyPartId === form.bodyPartId)
})

const filteredRules = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  return rules.value.filter(item => {
    if (selectedTriggerType.value && item.triggerType !== selectedTriggerType.value) return false
    if (!value) return true

    return [
      item.ruleName,
      item.ruleCode,
      triggerTypeLabel(item.triggerType),
      actionTypeLabel(item.actionType),
      ruleConditionSummary(item),
      triageLevelName(item.triageLevelId),
      item.suggestion
    ]
      .filter(Boolean)
      .some(text => String(text).toLowerCase().includes(value))
  })
})

const enabledCount = computed(() => rules.value.filter(item => item.status === 1).length)
const emergencyActionCount = computed(() => rules.value.filter(item => item.actionType === 'emergency').length)
const combinationCount = computed(() => rules.value.filter(item => item.triggerType === 'combination').length)
const isEditing = computed(() => !!form.id)
const triggerTypeTip = computed(() => {
  switch (form.triggerType) {
    case 'symptom_match':
      return '症状匹配规则至少需要选择一个症状，适合处理标准化症状直达触发场景。'
    case 'keyword_match':
      return '关键词匹配规则需要填写关键词模式，适合拦截用户自由文本中的高风险表达。'
    case 'body_part_match':
      return '部位匹配规则需要选择身体部位，适合仅凭部位信息先提高分诊优先级。'
    case 'combination':
      return '组合规则至少需要配置两类条件，适合处理“部位 + 症状”或“症状 + 关键词”等复合判断。'
    default:
      return '请先选择规则触发方式。'
  }
})

watch(() => form.bodyPartId, (value) => {
  if (!value) return
  form.symptomIds = form.symptomIds.filter(id => symptomMap.value.get(id)?.bodyPartId === value)
})

function createEmptyForm() {
  return {
    id: null,
    ruleName: '',
    ruleCode: '',
    triggerType: 'symptom_match',
    bodyPartId: null,
    symptomIds: [],
    keywordPattern: '',
    conditionDescription: '',
    triageLevelId: null,
    suggestion: '',
    actionType: 'offline',
    priority: 0,
    status: 1
  }
}

function loadData() {
  loadBodyParts()
  loadSymptoms()
  loadTriageLevels()
  loadRules()
}

function loadBodyParts() {
  get('/api/admin/body-part/list', (data) => {
    bodyParts.value = data || []
  })
}

function loadSymptoms() {
  get('/api/admin/symptom/list', (data) => {
    symptoms.value = data || []
  })
}

function loadTriageLevels() {
  get('/api/admin/triage-level/list', (data) => {
    triageLevels.value = data || []
  })
}

function loadRules() {
  loading.value = true
  get('/api/admin/red-flag/list', (data) => {
    rules.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function openCreateDialog() {
  if (!triageLevels.value.length) {
    ElMessage.warning('请先维护分诊等级，再新增红旗规则')
    return
  }
  Object.assign(form, createEmptyForm(), {
    triageLevelId: triageLevels.value[0]?.id || null
  })
  dialogVisible.value = true
}

function openEditDialog(row) {
  Object.assign(form, createEmptyForm(), {
    id: row.id,
    ruleName: row.ruleName,
    ruleCode: row.ruleCode,
    triggerType: row.triggerType,
    bodyPartId: row.bodyPartId || null,
    symptomIds: [...(row.symptomIds || [])],
    keywordPattern: row.keywordPattern || '',
    conditionDescription: row.conditionDescription || '',
    triageLevelId: row.triageLevelId,
    suggestion: row.suggestion || '',
    actionType: row.actionType,
    priority: row.priority,
    status: row.status
  })
  dialogVisible.value = true
}

function submitRule() {
  formRef.value.validate((valid) => {
    if (!valid) return

    const manualMessage = validateRuleForm()
    if (manualMessage) {
      ElMessage.warning(manualMessage)
      return
    }

    const payload = {
      ruleName: form.ruleName.trim(),
      ruleCode: form.ruleCode.trim(),
      triggerType: form.triggerType,
      bodyPartId: form.bodyPartId,
      symptomIds: [...form.symptomIds],
      keywordPattern: normalizeOptionalText(form.keywordPattern),
      conditionDescription: normalizeOptionalText(form.conditionDescription),
      triageLevelId: form.triageLevelId,
      suggestion: form.suggestion.trim(),
      actionType: form.actionType,
      priority: form.priority,
      status: form.status
    }
    if (isEditing.value) payload.id = form.id

    submitting.value = true
    post(isEditing.value ? '/api/admin/red-flag/update' : '/api/admin/red-flag/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '红旗规则更新成功' : '红旗规则新增成功')
      loadRules()
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '提交失败，请稍后重试')
    })
  })
}

function removeRule(row) {
  ElMessageBox.confirm(`确认删除红旗规则“${row.ruleName}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/red-flag/delete?id=${row.id}`, () => {
      ElMessage.success('红旗规则删除成功')
      loadRules()
    }, (message) => {
      ElMessage.warning(message || '红旗规则删除失败')
    })
  }).catch(() => {})
}

function validateRuleForm() {
  const symptomIds = form.symptomIds.filter(Boolean)
  const keywordPattern = normalizeOptionalText(form.keywordPattern)

  if (form.bodyPartId && symptomIds.some(id => symptomMap.value.get(id)?.bodyPartId !== form.bodyPartId)) {
    return '已选择的症状必须属于当前身体部位'
  }

  const conditionCount =
    Number(!!form.bodyPartId) +
    Number(symptomIds.length > 0) +
    Number(!!keywordPattern)

  switch (form.triggerType) {
    case 'symptom_match':
      return symptomIds.length ? '' : '症状匹配规则至少需要选择一个症状'
    case 'keyword_match':
      return keywordPattern ? '' : '关键词匹配规则请填写关键词模式'
    case 'body_part_match':
      return form.bodyPartId ? '' : '部位匹配规则请选择身体部位'
    case 'combination':
      return conditionCount >= 2 ? '' : '组合规则至少需要配置两类触发条件'
    default:
      return '请选择有效的规则触发方式'
  }
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

function triageLevelName(id) {
  return triageLevelMap.value.get(id)?.name || '-'
}

function symptomNames(symptomIds = []) {
  return symptomIds
    .map(id => symptomMap.value.get(id)?.name)
    .filter(Boolean)
}

function ruleConditionSummary(row) {
  const segments = []
  if (row.bodyPartId) segments.push(`部位：${bodyPartPath(row.bodyPartId)}`)
  const symptomTexts = symptomNames(row.symptomIds)
  if (symptomTexts.length) segments.push(`症状：${symptomTexts.join('、')}`)
  if (row.keywordPattern) segments.push(`关键词：${row.keywordPattern}`)
  if (row.conditionDescription) segments.push(`说明：${row.conditionDescription}`)
  return segments.join(' / ') || '-'
}

function triggerTypeLabel(value) {
  return triggerTypeOptions.find(item => item.value === value)?.label || value || '-'
}

function actionTypeLabel(value) {
  return actionTypeOptions.find(item => item.value === value)?.label || value || '-'
}

function actionTagType(value) {
  switch (value) {
    case 'emergency':
      return 'danger'
    case 'offline':
      return 'warning'
    case 'online':
      return 'success'
    default:
      return 'info'
  }
}

function normalizeOptionalText(value) {
  const text = String(value || '').trim()
  return text || null
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
.red-flag-page {
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

.toolbar,
.toolbar-filters,
.toolbar-actions {
  display: flex;
  gap: 12px;
}

.toolbar {
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.toolbar-filters {
  flex-wrap: wrap;
}

.dialog-alert {
  margin-bottom: 18px;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
