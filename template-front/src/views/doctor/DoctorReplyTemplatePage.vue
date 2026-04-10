<template>
  <div class="template-page">
    <section class="guide-card">
      <div class="guide-head">
        <div>
          <h3>模板填写说明</h3>
          <p>这些模板仅对当前登录医生生效，会在医生处理问诊、医患沟通和追加随访时作为快捷内容插入，适合沉淀高频、标准化的话术。</p>
        </div>
        <el-button type="primary" plain @click="openCreateDialog">立即新建模板</el-button>
      </div>
      <div class="guide-grid">
        <article v-for="item in sceneGuides" :key="item.value" class="guide-item">
          <div class="guide-item__top">
            <strong>{{ item.label }}</strong>
            <el-tag size="small" effect="light">{{ item.shortHint }}</el-tag>
          </div>
          <p>{{ item.description }}</p>
          <div class="guide-item__label">适合填写</div>
          <p class="guide-tip">{{ item.tip }}</p>
          <div class="guide-item__label">示例</div>
          <p class="guide-example">{{ item.exampleTitle }}：{{ item.exampleContent }}</p>
        </article>
      </div>
    </section>

    <section class="stat-grid">
      <article class="stat-card">
        <span>模板总数</span>
        <strong>{{ templates.length }}</strong>
      </article>
      <article class="stat-card">
        <span>启用中</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="stat-card">
        <span>覆盖场景</span>
        <strong>{{ sceneCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input v-model="keyword" clearable placeholder="搜索标题、内容或场景" style="max-width:320px" />
          <el-select v-model="sceneFilter" clearable placeholder="全部场景" style="width:180px">
            <el-option v-for="item in sceneOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="statusFilter" clearable placeholder="全部状态" style="width:140px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadTemplates">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增模板</el-button>
        </div>
      </div>

      <el-table :data="filteredTemplates" v-loading="loading" border>
        <el-table-column label="场景" min-width="150">
          <template #default="{ row }">{{ sceneLabel(row.sceneType) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="模板标题" min-width="180" />
        <el-table-column prop="content" label="模板内容" min-width="360" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="removeTemplate(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑常用回复模板' : '新增常用回复模板'" width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="使用场景" prop="sceneType">
            <el-select v-model="form.sceneType" style="width:100%" placeholder="请选择模板场景">
              <el-option v-for="item in sceneOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="模板标题" prop="title">
            <el-input v-model="form.title" maxlength="100" placeholder="例如：发热观察建议" />
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="form.sort" :min="0" :max="999" style="width:100%" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" style="width:100%">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
        </div>
        <section v-if="currentSceneGuide" class="scene-helper">
          <div class="scene-helper__head">
            <div>
              <strong>{{ currentSceneGuide.label }}</strong>
              <p>{{ currentSceneGuide.description }}</p>
            </div>
            <el-button text type="primary" @click="applySceneExample">填入示例</el-button>
          </div>
          <div class="scene-helper__meta">适合填写：{{ currentSceneGuide.tip }}</div>
          <div class="scene-helper__example">
            <strong>推荐标题</strong>
            <p>{{ currentSceneGuide.exampleTitle }}</p>
            <strong>推荐内容</strong>
            <p>{{ currentSceneGuide.exampleContent }}</p>
          </div>
        </section>
        <el-form-item label="模板内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" maxlength="2000" show-word-limit placeholder="填写常用说明、建议或标准回复内容。" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTemplate">{{ isEditing ? '保存修改' : '确认新增' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { get, post } from '@/net'

const sceneMetaMap = {
  handle_summary: {
    label: '医生判断摘要',
    shortHint: '处理阶段',
    description: '适合沉淀“当前判断是什么、风险高不高、是否适合继续线上处理”这类结论性表达。',
    tip: '病情判断、风险提示、当前处理结论',
    exampleTitle: '通用病情判断',
    exampleContent: '结合当前主诉和已提交资料，暂未见明确急症指征，建议先按线上方案继续观察，并根据症状变化决定是否转线下就医。'
  },
  medical_advice: {
    label: '处理建议',
    shortHint: '处理阶段',
    description: '适合沉淀生活方式建议、观察点、用药提醒、线下复诊建议等高频回复。',
    tip: '观察要点、生活建议、复诊提醒',
    exampleTitle: '常规观察建议',
    exampleContent: '建议近期保持规律作息、清淡饮食并记录症状变化；如症状持续加重、出现高热或明显不适，请尽快到线下门诊进一步评估。'
  },
  follow_up_plan: {
    label: '随访计划',
    shortHint: '处理阶段',
    description: '适合沉淀“多久后复诊、什么情况下提前联系、下一步看什么指标”的计划性内容。',
    tip: '复诊时点、复查条件、复联安排',
    exampleTitle: '三日复诊计划',
    exampleContent: '建议 3 天后再次复诊；若中途症状明显加重、体温持续升高或出现新的不适，请提前发起问诊或线下就医。'
  },
  patient_instruction: {
    label: '患者指导要点',
    shortHint: '处理阶段',
    description: '适合沉淀给患者看的注意事项和风险提示，通常写得更直接、更容易执行。',
    tip: '风险提示、居家注意事项、就医提醒',
    exampleTitle: '就医提醒',
    exampleContent: '请继续观察体温、精神状态和饮水情况；若出现持续高热、呼吸困难、明显嗜睡或疼痛加重，请立即前往线下医院就诊。'
  },
  message_opening: {
    label: '接诊开场消息',
    shortHint: '沟通阶段',
    description: '适合沉淀医生首次接手问诊后的开场说明，告诉患者当前已开始处理，并说明接下来需要配合补充的内容。',
    tip: '接诊说明、资料补充提醒、下一步沟通方式',
    exampleTitle: '首次接诊开场',
    exampleContent: '您好，我已接手本次问诊。为了更准确判断，请您补充症状开始时间、目前最明显的不适，以及近期是否自行用药；如已有检查结果，也可以一并上传。'
  },
  message_clarify: {
    label: '补充追问消息',
    shortHint: '沟通阶段',
    description: '适合沉淀高频追问话术，帮助医生快速补齐症状变化、诱因、伴随表现和既往处理情况。',
    tip: '症状持续时间、诱因、伴随症状、已用药情况',
    exampleTitle: '关键信息补充追问',
    exampleContent: '为了进一步判断，请再补充一下：1）症状从什么时候开始；2）这两天是持续存在还是间断出现；3）是否伴随发热、疼痛加重或其他新的不适；4）目前是否已经自行用药。'
  },
  message_check_result: {
    label: '结果解读消息',
    shortHint: '沟通阶段',
    description: '适合沉淀围绕化验、影像或检查结果的解释话术，帮助医生快速同步关注点和下一步安排。',
    tip: '检查结果说明、异常项提醒、后续观察或复查建议',
    exampleTitle: '检查结果沟通',
    exampleContent: '从您上传的检查结果来看，当前需要重点关注的是异常指标的变化趋势。建议您结合目前症状继续观察，如后续出现明显加重或新的不适，请尽快线下复诊，并补充完整检查报告。'
  },
  message_follow_up: {
    label: '复诊随访消息',
    shortHint: '沟通阶段',
    description: '适合沉淀处理完成后的跟进话术，用于确认恢复情况、提醒复诊节点和再次联系条件。',
    tip: '恢复情况询问、复诊安排、风险再提醒',
    exampleTitle: '复诊随访提醒',
    exampleContent: '这两天请继续观察主要症状是否缓解，并记录体温或相关指标变化。如果仍反复发作、明显加重，或出现新的高风险表现，请提前联系医生或安排线下就诊。'
  },
  followup_summary: {
    label: '随访摘要',
    shortHint: '随访阶段',
    description: '适合沉淀随访时的阶段性结论，例如“症状较前缓解”“仍有反复但总体稳定”。',
    tip: '本次变化、当前状态、阶段性判断',
    exampleTitle: '症状缓解摘要',
    exampleContent: '本次随访较前反馈显示主要不适已有缓解，当前整体状态趋于稳定，但仍建议继续观察近期变化。'
  },
  followup_advice: {
    label: '随访建议',
    shortHint: '随访阶段',
    description: '适合沉淀随访后的观察建议、执行动作和持续管理话术。',
    tip: '继续观察、复查建议、执行动作',
    exampleTitle: '继续观察建议',
    exampleContent: '建议继续按既定方案观察，并做好体温、症状频次或相关指标记录；如再次明显加重，请及时复诊。'
  },
  followup_next_step: {
    label: '随访下一步安排',
    shortHint: '随访阶段',
    description: '适合沉淀后续节点安排，例如“几天后继续平台随访”或“安排线下检查”。',
    tip: '下一次联系、复查安排、转线下条件',
    exampleTitle: '下一步复联安排',
    exampleContent: '建议 2 到 3 天后继续平台随访，如期间出现新发高风险症状，可提前联系医生或安排线下检查。'
  }
}

const sceneOptions = Object.entries(sceneMetaMap).map(([value, item]) => ({
  label: item.label,
  value
}))

const sceneGuides = Object.entries(sceneMetaMap).map(([value, item]) => ({
  value,
  ...item
}))

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const sceneFilter = ref('')
const statusFilter = ref(null)
const formRef = ref()
const templates = ref([])
const form = reactive(createEmptyForm())

const rules = {
  sceneType: [{ required: true, message: '请选择模板场景', trigger: 'change' }],
  title: [{ required: true, message: '请输入模板标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入模板内容', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序值', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const filteredTemplates = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  return templates.value.filter(item => {
    const matchesKeyword = !value || [item.title, item.content, sceneLabel(item.sceneType)].filter(Boolean).some(text => `${text}`.toLowerCase().includes(value))
    const matchesScene = !sceneFilter.value || item.sceneType === sceneFilter.value
    const matchesStatus = statusFilter.value === null || statusFilter.value === undefined || item.status === statusFilter.value
    return matchesKeyword && matchesScene && matchesStatus
  })
})

const enabledCount = computed(() => templates.value.filter(item => item.status === 1).length)
const sceneCount = computed(() => new Set(templates.value.map(item => item.sceneType)).size)
const isEditing = computed(() => !!form.id)
const currentSceneGuide = computed(() => form.sceneType ? sceneMetaMap[form.sceneType] || null : null)

function createEmptyForm() {
  return {
    id: null,
    sceneType: '',
    title: '',
    content: '',
    sort: 0,
    status: 1
  }
}

function loadTemplates() {
  loading.value = true
  get('/api/doctor/reply-template/list', data => {
    templates.value = data || []
    loading.value = false
  }, message => {
    loading.value = false
    ElMessage.warning(message || '常用回复模板加载失败')
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

function applySceneExample() {
  if (!currentSceneGuide.value) return
  form.title = currentSceneGuide.value.exampleTitle
  form.content = currentSceneGuide.value.exampleContent
}

function submitTemplate() {
  formRef.value.validate(valid => {
    if (!valid) return
    const payload = {
      sceneType: form.sceneType,
      title: form.title,
      content: form.content,
      sort: form.sort,
      status: form.status
    }
    if (isEditing.value) payload.id = form.id
    submitting.value = true
    post(isEditing.value ? '/api/doctor/reply-template/update' : '/api/doctor/reply-template/create', payload, () => {
      submitting.value = false
      dialogVisible.value = false
      ElMessage.success(isEditing.value ? '模板更新成功' : '模板新增成功')
      loadTemplates()
    }, message => {
      submitting.value = false
      ElMessage.warning(message || '模板提交失败')
    })
  })
}

function removeTemplate(row) {
  ElMessageBox.confirm(`确认删除模板“${row.title}”吗？`, '删除确认', { type: 'warning' }).then(() => {
    get(`/api/doctor/reply-template/delete?id=${row.id}`, () => {
      ElMessage.success('模板删除成功')
      loadTemplates()
    }, message => {
      ElMessage.warning(message || '模板删除失败')
    })
  }).catch(() => {})
}

function sceneLabel(value) {
  return sceneMetaMap[value]?.label || value || '-'
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

onMounted(() => loadTemplates())
</script>

<style scoped>
.template-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.guide-card,
.stat-card,
.table-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.guide-card,
.stat-grid {
  gap: 18px;
}

.guide-card {
  padding: 22px;
}

.guide-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.guide-head h3 {
  margin: 0;
}

.guide-head p {
  margin: 8px 0 0;
  line-height: 1.7;
  color: var(--app-muted);
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.guide-item {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(19, 73, 80, 0.05);
}

.guide-item__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.guide-item__top strong {
  font-size: 15px;
  color: #214f57;
}

.guide-item p {
  margin: 10px 0 0;
  line-height: 1.7;
  color: #476168;
}

.guide-item__label {
  margin-top: 12px;
  font-size: 12px;
  font-weight: 600;
  color: #2a6972;
}

.guide-tip,
.guide-example {
  white-space: pre-line;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.scene-helper {
  margin-bottom: 18px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(19, 73, 80, 0.05);
}

.scene-helper__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
}

.scene-helper__head strong {
  display: block;
  font-size: 15px;
  color: #214f57;
}

.scene-helper__head p,
.scene-helper__meta,
.scene-helper__example p {
  margin: 8px 0 0;
  line-height: 1.7;
  color: #476168;
}

.scene-helper__meta {
  margin-top: 12px;
}

.scene-helper__example {
  margin-top: 12px;
}

.scene-helper__example strong {
  display: block;
  margin-top: 10px;
  color: #2a6972;
}

@media (max-width: 960px) {
  .guide-grid,
  .stat-grid,
  .dialog-grid {
    grid-template-columns: 1fr;
  }

  .guide-head,
  .toolbar,
  .scene-helper__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
