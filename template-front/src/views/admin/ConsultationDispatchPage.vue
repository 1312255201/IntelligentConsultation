<template>
  <div class="dispatch-config-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>推荐候选医生</span>
        <strong>{{ form.recommendDoctorLimit }}</strong>
      </article>
      <article class="stat-card">
        <span>待接手超时阈值</span>
        <strong>{{ form.waitingOverdueHours }}h</strong>
      </article>
      <article class="stat-card">
        <span>推荐分范围</span>
        <strong>{{ `${form.minRecommendationScore} - ${form.maxRecommendationScore}` }}</strong>
      </article>
      <article class="stat-card">
        <span>当前总权重</span>
        <strong>{{ `${totalWeight}%` }}</strong>
      </article>
    </section>

    <section class="panel-card">
      <div class="section-head">
        <div>
          <h3>智能分配策略</h3>
          <p>配置会同时影响患者导诊推荐医生排序、医生侧推荐理由，以及后台智能分配运营看板中的超时统计口径。</p>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
          <el-button @click="resetToDefault">恢复默认</el-button>
          <el-button type="primary" :loading="submitting" @click="saveConfig">保存配置</el-button>
        </div>
      </div>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="推荐分会按各因子基础得分乘以对应权重计算。下方对比预览会同时展示已保存策略与当前表单参数的推荐差异。"
      />

      <div class="factor-grid">
        <article v-for="item in factorCards" :key="item.key" class="factor-card">
          <div class="factor-head">
            <strong>{{ item.title }}</strong>
            <el-tag effect="light" type="primary">{{ `${item.weight}%` }}</el-tag>
          </div>
          <p>{{ item.description }}</p>
          <div class="factor-bar">
            <span :style="{ width: `${Math.min(item.weight, 300) / 3}%` }"></span>
          </div>
        </article>
      </div>
    </section>

    <section class="panel-card" v-loading="loading">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="section-head compact">
          <div>
            <h3>排序权重</h3>
            <p>控制各个维度在推荐排序中的影响比例，100% 表示沿用当前默认力度。</p>
          </div>
        </div>

        <div class="weight-grid">
          <article v-for="item in weightItems" :key="item.field" class="weight-card">
            <el-form-item :label="item.label" :prop="item.field">
              <el-input-number
                v-model="form[item.field]"
                :min="0"
                :max="300"
                :step="10"
                style="width: 100%"
              />
            </el-form-item>
            <p>{{ item.tip }}</p>
          </article>
        </div>

        <div class="section-head compact">
          <div>
            <h3>评分与运营阈值</h3>
            <p>控制标签命中折算、候选数量、分值区间，以及后台“超时待接手”的统计阈值。</p>
          </div>
        </div>

        <div class="form-grid">
          <el-form-item label="标签命中单项分" prop="tagMatchScorePerHit">
            <el-input-number v-model="form.tagMatchScorePerHit" :min="0" :max="30" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最多参与加分的匹配标签数" prop="maxMatchedTags">
            <el-input-number v-model="form.maxMatchedTags" :min="1" :max="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="推荐候选医生数" prop="recommendDoctorLimit">
            <el-input-number v-model="form.recommendDoctorLimit" :min="1" :max="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最低推荐分" prop="minRecommendationScore">
            <el-input-number v-model="form.minRecommendationScore" :min="0" :max="100" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最高推荐分" prop="maxRecommendationScore">
            <el-input-number v-model="form.maxRecommendationScore" :min="1" :max="100" style="width: 100%" />
          </el-form-item>
          <el-form-item label="待接手超时阈值（小时）" prop="waitingOverdueHours">
            <el-input-number v-model="form.waitingOverdueHours" :min="1" :max="168" style="width: 100%" />
          </el-form-item>
        </div>
      </el-form>
    </section>

    <section class="panel-card">
      <div class="section-head">
        <div>
          <h3>策略对比预览</h3>
          <p>选择真实问诊样本后，可同时查看“已保存策略”和“当前页面参数”下的推荐结果，方便调参前后做直接对比。</p>
        </div>
        <div class="toolbar-actions">
          <el-button :loading="previewRecordsLoading" @click="loadPreviewRecords">刷新样本</el-button>
          <el-button type="primary" :loading="previewLoading" :disabled="!previewConsultationId" @click="runComparePreview">
            对比预览
          </el-button>
        </div>
      </div>

      <div class="preview-toolbar">
        <el-select
          v-model="previewConsultationId"
          filterable
          clearable
          style="width: min(560px, 100%)"
          placeholder="请选择要预览的问诊样本"
        >
          <el-option
            v-for="item in previewRecords"
            :key="item.id"
            :label="previewOptionLabel(item)"
            :value="item.id"
          />
        </el-select>
        <span class="preview-hint">推荐结果会同时读取已保存策略和当前表单参数，首推变化、候选变化会直接展示出来。</span>
      </div>

      <template v-if="previewBaseRecord">
        <div class="preview-meta-grid">
          <article class="preview-meta-card">
            <span>问诊样本</span>
            <strong>{{ previewBaseRecord.consultationNo }}</strong>
            <small>{{ previewBaseRecord.patientName || '-' }}</small>
          </article>
          <article class="preview-meta-card">
            <span>分类 / 科室</span>
            <strong>{{ previewBaseRecord.categoryName || '-' }}</strong>
            <small>{{ previewBaseRecord.departmentName || '未匹配科室' }}</small>
          </article>
          <article class="preview-meta-card">
            <span>建议动作</span>
            <strong>{{ triageActionLabel(previewBaseRecord.triageActionType) }}</strong>
            <small>{{ previewBaseRecord.triageLevelName || '待评估' }}</small>
          </article>
          <article class="preview-meta-card">
            <span>创建时间</span>
            <strong>{{ formatDate(previewBaseRecord.createTime) }}</strong>
            <small>{{ compareHeadline }}</small>
          </article>
        </div>

        <section class="preview-brief">
          <p class="copy"><strong>标题：</strong>{{ previewBaseRecord.title || '未填写标题' }}</p>
          <p class="copy"><strong>主诉：</strong>{{ previewBaseRecord.chiefComplaint || '暂无主诉摘要' }}</p>
          <p v-if="previewBaseRecord.triageSuggestion" class="copy"><strong>导诊建议：</strong>{{ previewBaseRecord.triageSuggestion }}</p>
          <p v-if="previewBaseRecord.triageRuleSummary" class="copy"><strong>风险提示：</strong>{{ previewBaseRecord.triageRuleSummary }}</p>
        </section>

        <div class="compare-summary-grid">
          <article class="compare-summary-card">
            <span>首推变化</span>
            <strong>{{ topDoctorChanged ? '已变化' : '保持一致' }}</strong>
            <small>{{ compareHeadline }}</small>
          </article>
          <article class="compare-summary-card">
            <span>已保存策略首推</span>
            <strong>{{ savedTopDoctor?.name || '-' }}</strong>
            <small>{{ savedTopDoctor?.title || '暂无推荐结果' }}</small>
          </article>
          <article class="compare-summary-card">
            <span>当前参数首推</span>
            <strong>{{ currentTopDoctor?.name || '-' }}</strong>
            <small>{{ currentTopDoctor?.title || '暂无推荐结果' }}</small>
          </article>
          <article class="compare-summary-card">
            <span>候选重叠</span>
            <strong>{{ sharedDoctorCount }}</strong>
            <small>{{ `已保存 ${savedPreviewDoctors.length} / 当前 ${currentPreviewDoctors.length}` }}</small>
          </article>
        </div>

        <div class="compare-grid">
          <section class="compare-panel is-saved">
            <div class="compare-head">
              <div>
                <strong>已保存策略</strong>
                <p>线上当前生效的推荐结果</p>
              </div>
              <el-tag effect="light" type="info">当前线上版本</el-tag>
            </div>
            <div v-if="savedPreviewDoctors.length" class="doctor-list is-compare">
              <article v-for="item in savedPreviewDoctors" :key="`saved-${item.id}`" class="doctor-card">
                <img v-if="item.photo" :src="resolveImagePath(item.photo)" :alt="item.name" class="doctor-avatar" />
                <div v-else class="doctor-avatar doctor-avatar-fallback">{{ (item.name || '医').slice(0, 1) }}</div>
                <div class="doctor-copy">
                  <div class="doctor-copy-head">
                    <strong>{{ item.name }}</strong>
                    <span v-if="doctorRecommendationScoreText(item)" class="recommend-score">{{ doctorRecommendationScoreText(item) }}</span>
                  </div>
                  <span>{{ item.title || '在线医生' }}</span>
                  <p>{{ item.expertise || '暂无擅长信息' }}</p>
                  <div v-if="item.matchedServiceTags?.length" class="chip-row is-accent">
                    <span v-for="tag in item.matchedServiceTags" :key="`saved-${item.id}-${tag}`">匹配 {{ tag }}</span>
                  </div>
                  <div v-if="item.recommendationReasons?.length" class="chip-row is-subtle">
                    <span v-for="reason in item.recommendationReasons.slice(0, 3)" :key="`saved-${item.id}-${reason}`">{{ reason }}</span>
                  </div>
                  <p v-if="item.recommendationSummary" class="copy doctor-recommend-copy"><strong>排序说明：</strong>{{ item.recommendationSummary }}</p>
                  <small>{{ item.nextScheduleText || '暂无后续排班' }}</small>
                </div>
              </article>
            </div>
            <el-empty v-else description="已保存策略下暂无可推荐医生" />
          </section>

          <section class="compare-panel is-current">
            <div class="compare-head">
              <div>
                <strong>当前页面参数</strong>
                <p>未保存也可以先看调参效果</p>
              </div>
              <el-tag effect="light" :type="topDoctorChanged ? 'warning' : 'success'">
                {{ topDoctorChanged ? '与已保存策略不同' : '与已保存策略一致' }}
              </el-tag>
            </div>
            <div v-if="currentPreviewDoctors.length" class="doctor-list is-compare">
              <article v-for="item in currentPreviewDoctors" :key="`current-${item.id}`" class="doctor-card">
                <img v-if="item.photo" :src="resolveImagePath(item.photo)" :alt="item.name" class="doctor-avatar" />
                <div v-else class="doctor-avatar doctor-avatar-fallback">{{ (item.name || '医').slice(0, 1) }}</div>
                <div class="doctor-copy">
                  <div class="doctor-copy-head">
                    <strong>{{ item.name }}</strong>
                    <span v-if="doctorRecommendationScoreText(item)" class="recommend-score">{{ doctorRecommendationScoreText(item) }}</span>
                  </div>
                  <span>{{ item.title || '在线医生' }}</span>
                  <p>{{ item.expertise || '暂无擅长信息' }}</p>
                  <div v-if="item.matchedServiceTags?.length" class="chip-row is-accent">
                    <span v-for="tag in item.matchedServiceTags" :key="`current-${item.id}-${tag}`">匹配 {{ tag }}</span>
                  </div>
                  <div v-if="item.recommendationReasons?.length" class="chip-row is-subtle">
                    <span v-for="reason in item.recommendationReasons.slice(0, 3)" :key="`current-${item.id}-${reason}`">{{ reason }}</span>
                  </div>
                  <p v-if="item.recommendationSummary" class="copy doctor-recommend-copy"><strong>排序说明：</strong>{{ item.recommendationSummary }}</p>
                  <small>{{ item.nextScheduleText || '暂无后续排班' }}</small>
                </div>
              </article>
            </div>
            <el-empty v-else description="当前页面参数下暂无可推荐医生" />
          </section>
        </div>
      </template>

      <el-empty v-else description="选择问诊样本后即可查看已保存策略与当前参数的推荐对比" />
    </section>
    <section class="panel-card">
      <div class="section-head">
        <div>
          <h3>批量样本对比</h3>
          <p>一次挑选多个真实问诊样本，快速观察当前调参与已保存策略相比会让多少样本的首推医生发生变化。</p>
        </div>
        <div class="toolbar-actions">
          <el-button :loading="previewRecordsLoading" @click="fillRecentBatchSamples">使用最近样本</el-button>
          <el-button type="primary" :loading="batchLoading" :disabled="!batchConsultationIds.length" @click="runBatchCompare">
            批量对比
          </el-button>
        </div>
      </div>

      <div class="preview-toolbar">
        <el-select
          v-model="batchConsultationIds"
          multiple
          filterable
          clearable
          collapse-tags
          collapse-tags-tooltip
          style="width: min(760px, 100%)"
          placeholder="请选择要批量对比的问诊样本，建议 3-8 个"
        >
          <el-option
            v-for="item in previewRecords"
            :key="`batch-${item.id}`"
            :label="previewOptionLabel(item)"
            :value="item.id"
          />
        </el-select>
        <span class="preview-hint">系统会同时读取已保存策略和当前页面参数，对每个样本输出首推变化和候选重合情况。</span>
      </div>

      <template v-if="batchCompareResult">
        <div class="compare-summary-grid">
          <article class="compare-summary-card">
            <span>样本数</span>
            <strong>{{ batchCompareResult.totalCount || 0 }}</strong>
            <small>当前已参与批量对比的真实问诊样本</small>
          </article>
          <article class="compare-summary-card">
            <span>首推发生变化</span>
            <strong>{{ batchCompareResult.changedCount || 0 }}</strong>
            <small>说明当前调参已经改变首推医生</small>
          </article>
          <article class="compare-summary-card">
            <span>首推保持一致</span>
            <strong>{{ batchCompareResult.unchangedCount || 0 }}</strong>
            <small>当前调参与已保存策略结果一致</small>
          </article>
          <article class="compare-summary-card">
            <span>两侧均无推荐</span>
            <strong>{{ batchCompareResult.bothNoRecommendationCount || 0 }}</strong>
            <small>这类样本需要补医生供给或调整筛选条件</small>
          </article>
        </div>

        <div v-if="batchCompareItems.length" class="batch-list">
          <article
            v-for="item in batchCompareItems"
            :key="item.consultationId"
            class="batch-card"
            :class="{
              'is-changed': item.topDoctorChanged,
              'is-empty': item.bothNoRecommendation
            }"
          >
            <div class="batch-card-head">
              <div>
                <strong>{{ item.consultationNo }}</strong>
                <p>{{ [item.patientName, item.categoryName, item.departmentName].filter(Boolean).join(' / ') || '未命名样本' }}</p>
              </div>
              <div class="batch-card-actions">
                <el-tag effect="light" :type="batchItemTagType(item)">{{ batchItemTagText(item) }}</el-tag>
                <el-button text type="primary" @click="focusPreviewRecord(item.consultationId)">查看单样本</el-button>
              </div>
            </div>

            <p v-if="item.title" class="copy batch-copy">{{ item.title }}</p>

            <div class="batch-card-grid">
              <div class="batch-card-col">
                <span>已保存策略首推</span>
                <strong>{{ item.savedTopDoctorName || '-' }}</strong>
                <small>{{ item.savedTopDoctorTitle || '暂无推荐医生' }}</small>
                <small>{{ scoreText(item.savedTopDoctorScore, item.savedDoctorCount) }}</small>
              </div>
              <div class="batch-card-col">
                <span>当前参数首推</span>
                <strong>{{ item.currentTopDoctorName || '-' }}</strong>
                <small>{{ item.currentTopDoctorTitle || '暂无推荐医生' }}</small>
                <small>{{ scoreText(item.currentTopDoctorScore, item.currentDoctorCount) }}</small>
              </div>
              <div class="batch-card-col">
                <span>对比结论</span>
                <strong>{{ item.sharedDoctorCount || 0 }}</strong>
                <small>{{ `候选重合 ${item.sharedDoctorCount || 0} 人` }}</small>
                <small>{{ `${triageActionLabel(item.triageActionType)} · ${item.triageLevelName || '待评估'}` }}</small>
              </div>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前批量样本没有可对比结果" />
      </template>

      <el-empty v-else description="选择多个问诊样本后即可查看批量对比结果" />
    </section>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { get, post, resolveImagePath } from '@/net'

const loading = ref(false)
const submitting = ref(false)
const previewLoading = ref(false)
const previewRecordsLoading = ref(false)
const batchLoading = ref(false)
const formRef = ref()

const previewConsultationId = ref(null)
const batchConsultationIds = ref([])
const previewRecords = ref([])
const savedPreviewResult = ref(null)
const currentPreviewResult = ref(null)
const batchCompareResult = ref(null)
const savedConfig = ref(createDefaultForm())

const form = reactive(createDefaultForm())

const weightItems = [
  {
    field: 'visitTypeWeight',
    label: '接诊方式匹配权重',
    tip: '提高后，线上、线下、复诊等接诊方式与导诊动作越匹配的医生会更靠前。'
  },
  {
    field: 'scheduleWeight',
    label: '近期排班权重',
    tip: '提高后，近期可接诊且排班更近的医生会获得更多排序优势。'
  },
  {
    field: 'capacityWeight',
    label: '号源容量权重',
    tip: '提高后，剩余号源更充足的排班会更容易被优先推荐。'
  },
  {
    field: 'workloadWeight',
    label: '医生负载权重',
    tip: '提高后，当前待处理问诊更少的医生会得到更多加分。'
  },
  {
    field: 'tagMatchWeight',
    label: '服务标签权重',
    tip: '提高后，医生服务标签与问诊标题、主诉、分诊摘要匹配时会更明显地拉开差距。'
  }
]

const rules = {
  visitTypeWeight: [{ required: true, message: '请输入接诊方式匹配权重', trigger: 'change' }],
  scheduleWeight: [{ required: true, message: '请输入近期排班权重', trigger: 'change' }],
  capacityWeight: [{ required: true, message: '请输入号源容量权重', trigger: 'change' }],
  workloadWeight: [{ required: true, message: '请输入医生负载权重', trigger: 'change' }],
  tagMatchWeight: [{ required: true, message: '请输入服务标签权重', trigger: 'change' }],
  tagMatchScorePerHit: [{ required: true, message: '请输入标签命中单项分', trigger: 'change' }],
  maxMatchedTags: [{ required: true, message: '请输入最多参与加分的匹配标签数', trigger: 'change' }],
  recommendDoctorLimit: [{ required: true, message: '请输入推荐候选医生数', trigger: 'change' }],
  minRecommendationScore: [{ required: true, message: '请输入最低推荐分', trigger: 'change' }],
  maxRecommendationScore: [{ required: true, message: '请输入最高推荐分', trigger: 'change' }],
  waitingOverdueHours: [{ required: true, message: '请输入待接手超时阈值', trigger: 'change' }]
}

const totalWeight = computed(() => {
  return [
    form.visitTypeWeight,
    form.scheduleWeight,
    form.capacityWeight,
    form.workloadWeight,
    form.tagMatchWeight
  ].reduce((total, item) => total + Number(item || 0), 0)
})

const factorCards = computed(() => [
  {
    key: 'visit-type',
    title: '接诊方式匹配',
    weight: Number(form.visitTypeWeight || 0),
    description: '让导诊动作与排班接诊方式更一致，避免线下需求被过多推给仅线上排班医生。'
  },
  {
    key: 'schedule',
    title: '近期排班可接诊',
    weight: Number(form.scheduleWeight || 0),
    description: '优先考虑近期确实可接诊的医生，减少推荐后长时间等待或无法承接。'
  },
  {
    key: 'capacity',
    title: '剩余号源容量',
    weight: Number(form.capacityWeight || 0),
    description: '平衡医生号源紧张程度，避免把推荐过度集中到已接近满负荷的排班上。'
  },
  {
    key: 'workload',
    title: '当前处理负载',
    weight: Number(form.workloadWeight || 0),
    description: '结合医生正在处理的问诊数量，提升整体接手效率和平均响应速度。'
  },
  {
    key: 'tag-match',
    title: '服务标签命中',
    weight: Number(form.tagMatchWeight || 0),
    description: '让医生擅长方向和问诊场景之间的匹配结果，对排序产生更直接的影响。'
  }
])

const savedPreviewDoctors = computed(() => savedPreviewResult.value?.recommendedDoctors || [])
const currentPreviewDoctors = computed(() => currentPreviewResult.value?.recommendedDoctors || [])
const previewBaseRecord = computed(() => currentPreviewResult.value || savedPreviewResult.value || null)
const savedTopDoctor = computed(() => savedPreviewDoctors.value[0] || null)
const currentTopDoctor = computed(() => currentPreviewDoctors.value[0] || null)
const batchCompareItems = computed(() => batchCompareResult.value?.items || [])

const topDoctorChanged = computed(() => {
  const savedDoctor = savedTopDoctor.value
  const currentDoctor = currentTopDoctor.value
  if (!savedDoctor && !currentDoctor) return false
  if (!savedDoctor || !currentDoctor) return true
  if (savedDoctor.id != null && currentDoctor.id != null) {
    return savedDoctor.id !== currentDoctor.id
  }
  return `${savedDoctor.name || ''}` !== `${currentDoctor.name || ''}`
})

const sharedDoctorCount = computed(() => {
  const currentKeys = new Set(currentPreviewDoctors.value.map(item => doctorKey(item)))
  return savedPreviewDoctors.value.filter(item => currentKeys.has(doctorKey(item))).length
})

const compareHeadline = computed(() => {
  if (!savedTopDoctor.value && !currentTopDoctor.value) {
    return '两组参数下都没有生成推荐医生'
  }
  if (topDoctorChanged.value) {
    return '当前参数已改变首推医生'
  }
  if (savedTopDoctor.value && currentTopDoctor.value) {
    return '当前参数与已保存策略首推一致'
  }
  return '当前推荐结果与已保存策略保持一致'
})

function createDefaultForm() {
  return {
    id: 1,
    visitTypeWeight: 100,
    scheduleWeight: 100,
    capacityWeight: 100,
    workloadWeight: 100,
    tagMatchWeight: 100,
    tagMatchScorePerHit: 4,
    maxMatchedTags: 3,
    recommendDoctorLimit: 3,
    minRecommendationScore: 24,
    maxRecommendationScore: 99,
    waitingOverdueHours: 24,
    updateTime: ''
  }
}

function cloneConfig(source = {}) {
  return {
    ...createDefaultForm(),
    ...(source || {})
  }
}

function buildConfigPayload(source) {
  return {
    visitTypeWeight: source.visitTypeWeight,
    scheduleWeight: source.scheduleWeight,
    capacityWeight: source.capacityWeight,
    workloadWeight: source.workloadWeight,
    tagMatchWeight: source.tagMatchWeight,
    tagMatchScorePerHit: source.tagMatchScorePerHit,
    maxMatchedTags: source.maxMatchedTags,
    recommendDoctorLimit: source.recommendDoctorLimit,
    minRecommendationScore: source.minRecommendationScore,
    maxRecommendationScore: source.maxRecommendationScore,
    waitingOverdueHours: source.waitingOverdueHours
  }
}

function buildPreviewPayload(source) {
  return {
    consultationId: previewConsultationId.value,
    ...buildConfigPayload(source)
  }
}

function buildBatchPreviewPayload(source) {
  return {
    consultationIds: batchConsultationIds.value,
    ...buildConfigPayload(source)
  }
}

function postAsync(url, payload) {
  return new Promise((resolve, reject) => {
    post(url, payload, resolve, (message) => reject(new Error(message || '请求失败')))
  })
}

function loadData() {
  loading.value = true
  get('/api/admin/consultation-dispatch/config', (data) => {
    const nextConfig = cloneConfig(data)
    Object.assign(form, nextConfig)
    savedConfig.value = cloneConfig(nextConfig)
    loading.value = false
    if (previewConsultationId.value && !currentPreviewResult.value && !savedPreviewResult.value) {
      runComparePreview()
    }
  }, (message) => {
    loading.value = false
    ElMessage.warning(message || '智能分配策略加载失败')
  })
}

function loadPreviewRecords() {
  previewRecordsLoading.value = true
  get('/api/admin/consultation-dispatch/preview-records?limit=18', (data) => {
    previewRecords.value = data || []
    if (!previewConsultationId.value && previewRecords.value.length) {
      previewConsultationId.value = previewRecords.value[0].id
      if (!loading.value) {
        runComparePreview()
      }
    }
    if (!batchConsultationIds.value.length && previewRecords.value.length) {
      batchConsultationIds.value = previewRecords.value.slice(0, 6).map(item => item.id)
    }
    previewRecordsLoading.value = false
  }, (message) => {
    previewRecordsLoading.value = false
    ElMessage.warning(message || '预览样本加载失败')
  })
}

function fillRecentBatchSamples() {
  batchConsultationIds.value = previewRecords.value.slice(0, 6).map(item => item.id)
  if (!batchConsultationIds.value.length) {
    ElMessage.warning('暂无可用于批量对比的问诊样本')
    return
  }
  runBatchCompare()
}

function resetToDefault() {
  Object.assign(form, createDefaultForm())
  ElMessage.success('已恢复默认参数，保存后生效')
}

function validateScoreRange() {
  if (Number(form.minRecommendationScore) > Number(form.maxRecommendationScore)) {
    ElMessage.warning('最低推荐分不能大于最高推荐分')
    return false
  }
  return true
}

function saveConfig() {
  formRef.value.validate((valid) => {
    if (!valid || !validateScoreRange()) return

    submitting.value = true
    post('/api/admin/consultation-dispatch/config', buildConfigPayload(form), async () => {
      submitting.value = false
      ElMessage.success('智能分配策略已保存')
      loadData()
      if (previewConsultationId.value) {
        await runComparePreview()
      }
      if (batchConsultationIds.value.length) {
        await runBatchCompare()
      }
    }, (message) => {
      submitting.value = false
      ElMessage.warning(message || '智能分配策略保存失败')
    })
  })
}

async function runComparePreview() {
  if (!previewConsultationId.value) {
    ElMessage.warning('请先选择要预览的问诊样本')
    return
  }
  if (!validateScoreRange()) {
    return
  }

  previewLoading.value = true
  try {
    const [savedPreview, currentPreview] = await Promise.all([
      postAsync('/api/admin/consultation-dispatch/preview', buildPreviewPayload(savedConfig.value)),
      postAsync('/api/admin/consultation-dispatch/preview', buildPreviewPayload(form))
    ])
    savedPreviewResult.value = savedPreview || null
    currentPreviewResult.value = currentPreview || null
  } catch (error) {
    ElMessage.warning(error?.message || '策略对比预览失败')
  } finally {
    previewLoading.value = false
  }
}

async function runBatchCompare() {
  if (!batchConsultationIds.value.length) {
    ElMessage.warning('请先选择要批量对比的问诊样本')
    return
  }
  if (!validateScoreRange()) {
    return
  }

  batchLoading.value = true
  try {
    const result = await postAsync('/api/admin/consultation-dispatch/batch-preview', buildBatchPreviewPayload(form))
    batchCompareResult.value = result || null
  } catch (error) {
    ElMessage.warning(error?.message || '批量样本对比失败')
  } finally {
    batchLoading.value = false
  }
}

function focusPreviewRecord(id) {
  previewConsultationId.value = id
  runComparePreview()
}

function previewOptionLabel(item) {
  return [
    item.consultationNo,
    item.patientName,
    item.categoryName,
    item.departmentName
  ].filter(Boolean).join(' / ')
}

function triageActionLabel(value) {
  return ({
    emergency: '立即急诊',
    offline: '尽快线下',
    followup: '复诊随访',
    online: '线上继续',
    observe: '继续观察'
  })[value] || '继续关注'
}

function doctorRecommendationScoreText(item) {
  const score = Number(item?.recommendationScore)
  return Number.isFinite(score) && score > 0 ? `优先分 ${score}` : ''
}

function doctorKey(item) {
  if (!item) return ''
  return item.id != null ? `id-${item.id}` : `name-${item.name || ''}`
}

function batchItemTagType(item) {
  if (item?.bothNoRecommendation) return 'info'
  return item?.topDoctorChanged ? 'warning' : 'success'
}

function batchItemTagText(item) {
  if (item?.bothNoRecommendation) return '两侧均无推荐'
  return item?.topDoctorChanged ? '首推已变化' : '首推一致'
}

function scoreText(score, doctorCount) {
  const parts = []
  const numericScore = Number(score)
  if (Number.isFinite(numericScore) && numericScore > 0) {
    parts.push(`优先分 ${numericScore}`)
  }
  parts.push(`候选 ${doctorCount || 0} 人`)
  return parts.join(' · ')
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

onMounted(() => {
  loadData()
  loadPreviewRecords()
})
</script>

<style scoped>
.dispatch-config-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.stat-card,
.panel-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.stat-card {
  padding: 22px 24px;
}

.stat-card span,
.section-head p,
.weight-card p,
.factor-card p,
.preview-hint,
.preview-meta-card span,
.preview-meta-card small,
.compare-summary-card span,
.compare-summary-card small,
.compare-head p,
.batch-card p,
.batch-card-col span,
.batch-card-col small {
  color: var(--app-muted);
}

.stat-card strong,
.preview-meta-card strong,
.compare-summary-card strong {
  display: block;
  margin-top: 14px;
  font-size: 28px;
  line-height: 1.1;
}

.panel-card {
  padding: 22px;
}

.section-head,
.toolbar-actions,
.factor-head,
.preview-toolbar,
.doctor-copy-head,
.chip-row,
.compare-head,
.batch-card-head,
.batch-card-actions {
  display: flex;
  gap: 12px;
}

.section-head {
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
}

.section-head.compact {
  margin: 10px 0 16px;
}

.section-head h3,
.preview-toolbar,
.preview-brief p,
.compare-head p {
  margin: 0;
}

.section-head p,
.compare-head p {
  margin-top: 6px;
  line-height: 1.7;
}

.toolbar-actions {
  flex-wrap: wrap;
}

.factor-grid,
.weight-grid,
.form-grid,
.preview-meta-grid,
.compare-summary-grid,
.compare-grid,
.batch-card-grid {
  display: grid;
  gap: 16px;
}

.factor-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 18px;
}

.factor-card,
.weight-card,
.preview-meta-card,
.preview-brief,
.doctor-card,
.compare-summary-card,
.compare-panel,
.batch-card {
  border-radius: 20px;
  background: rgba(19, 73, 80, 0.05);
}

.factor-card {
  padding: 18px;
}

.factor-head {
  align-items: center;
  justify-content: space-between;
}

.factor-head strong,
.weight-card :deep(.el-form-item__label),
.doctor-copy strong,
.compare-head strong {
  display: block;
}

.factor-card p,
.weight-card p {
  margin: 12px 0 0;
  line-height: 1.7;
}

.factor-bar {
  margin-top: 16px;
  height: 10px;
  border-radius: 999px;
  background: rgba(19, 73, 80, 0.08);
  overflow: hidden;
}

.factor-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #0f766e, #14919b);
}

.weight-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.weight-card {
  padding: 18px 18px 8px;
}

.form-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.preview-toolbar {
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.preview-meta-grid,
.compare-summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 16px;
}

.preview-meta-card,
.preview-brief,
.compare-summary-card,
.compare-panel,
.batch-card {
  padding: 18px;
}

.preview-meta-card strong,
.compare-summary-card strong {
  margin-top: 10px;
  font-size: 22px;
}

.preview-meta-card small,
.compare-summary-card small {
  display: block;
  margin-top: 10px;
  line-height: 1.6;
}

.preview-brief {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.compare-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.compare-panel.is-saved {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(19, 73, 80, 0.05));
}

.compare-panel.is-current {
  background: linear-gradient(180deg, rgba(15, 118, 110, 0.08), rgba(255, 255, 255, 0.82));
}

.compare-head {
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}

.copy {
  line-height: 1.8;
  color: #41575d;
}

.doctor-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.doctor-list.is-compare {
  grid-template-columns: 1fr;
}

.doctor-card {
  display: flex;
  gap: 16px;
  padding: 18px;
}

.doctor-avatar {
  width: 72px;
  height: 72px;
  border-radius: 22px;
  object-fit: cover;
  flex-shrink: 0;
}

.doctor-avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.16), rgba(20, 145, 155, 0.24));
  color: #0f766e;
  font-size: 28px;
  font-weight: 700;
}

.doctor-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.doctor-copy-head {
  justify-content: space-between;
  align-items: flex-start;
}

.doctor-copy-head strong {
  margin: 0;
}

.doctor-copy span,
.doctor-copy small {
  color: var(--app-muted);
}

.doctor-copy p {
  margin: 0;
}

.recommend-score {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.12);
  color: #0f766e;
  font-size: 12px;
  white-space: nowrap;
}

.chip-row {
  flex-wrap: wrap;
}

.chip-row span {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(19, 73, 80, 0.08);
  color: #27646d;
  font-size: 12px;
}

.chip-row.is-accent span {
  background: rgba(15, 118, 110, 0.12);
  color: #0f766e;
}

.chip-row.is-subtle span {
  background: rgba(255, 255, 255, 0.6);
}

.doctor-recommend-copy {
  margin-top: 2px;
}

.batch-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.batch-card {
  border: 1px solid transparent;
}

.batch-card.is-changed {
  border-color: rgba(217, 119, 6, 0.24);
  background: linear-gradient(180deg, rgba(255, 251, 235, 0.92), rgba(19, 73, 80, 0.04));
}

.batch-card.is-empty {
  border-color: rgba(100, 116, 139, 0.18);
}

.batch-card-head {
  justify-content: space-between;
  align-items: flex-start;
}

.batch-card-head p {
  margin-top: 6px;
  line-height: 1.7;
}

.batch-card-actions {
  flex-wrap: wrap;
}

.batch-copy {
  margin-top: 12px;
}

.batch-card-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 14px;
}

.batch-card-col {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
}

.batch-card-col strong {
  margin: 0;
  font-size: 20px;
  line-height: 1.2;
}

@media (max-width: 1180px) {
  .stat-grid,
  .factor-grid,
  .weight-grid,
  .form-grid,
  .preview-meta-grid,
  .compare-summary-grid,
  .compare-grid,
  .batch-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .stat-grid,
  .factor-grid,
  .weight-grid,
  .form-grid,
  .preview-meta-grid,
  .compare-summary-grid,
  .compare-grid,
  .batch-card-grid {
    grid-template-columns: 1fr;
  }

  .section-head,
  .preview-toolbar,
  .doctor-card,
  .doctor-copy-head,
  .compare-head,
  .batch-card-head {
    flex-direction: column;
  }

  .toolbar-actions,
  .batch-card-actions {
    width: 100%;
  }
}
</style>
