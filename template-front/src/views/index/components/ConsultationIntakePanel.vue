<template>
  <section class="entry-layout">
    <div class="side-card">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">步骤一</span>
          <h3>描述本次不适症状</h3>
          <p>先告诉系统你哪里不舒服，系统会先匹配更合适的专科方向，再进入对应的问诊模板。</p>
        </div>
        <el-button text @click="emit('refresh-categories')">刷新模板数据</el-button>
      </div>

      <div class="intake-block">
        <el-input
          v-model="intakeChiefComplaintModel"
          type="textarea"
          :rows="5"
          maxlength="200"
          show-word-limit
          placeholder="例如：我的脚疼，走路时更明显，已经两天了。"
        />
        <div class="quick-chip-row">
          <button type="button" class="quick-chip" @click="intakeChiefComplaintModel = '我的脚疼，走路时更明显，已经两天了'">脚疼</button>
          <button type="button" class="quick-chip" @click="intakeChiefComplaintModel = '孩子发烧咳嗽，精神不太好'">孩子发烧</button>
          <button type="button" class="quick-chip" @click="intakeChiefComplaintModel = '身上起了红疹，还很痒'">皮疹瘙痒</button>
          <button type="button" class="quick-chip" @click="intakeChiefComplaintModel = '我想看体检报告，帮我解读一下'">报告解读</button>
        </div>
      </div>

      <div class="patient-block">
        <div class="panel-head compact">
          <div>
            <span class="panel-kicker">步骤二</span>
            <h3>选择就诊人</h3>
          </div>
          <el-button text @click="emit('go-patient-management')">去管理</el-button>
        </div>

        <el-select
          v-model="selectedPatientIdModel"
          filterable
          style="width: 100%"
          placeholder="请选择就诊人"
        >
          <el-option
            v-for="item in state.patients"
            :key="item.id"
            :label="`${item.name} / ${helpers.relationLabel(item.relationType)}${item.isDefault === 1 ? ' / 默认' : ''}`"
            :value="item.id"
          />
        </el-select>

        <div v-if="state.selectedPatient" class="patient-card">
          <div class="patient-top">
            <div>
              <strong>{{ state.selectedPatient.name }}</strong>
              <span>{{ helpers.relationLabel(state.selectedPatient.relationType) }}</span>
            </div>
            <el-tag :type="state.selectedPatient.status === 1 ? 'success' : 'info'" effect="light">
              {{ state.selectedPatient.status === 1 ? '可用' : '停用' }}
            </el-tag>
          </div>
          <div class="patient-meta">
            <span>性别：{{ helpers.genderLabel(state.selectedPatient.gender) }}</span>
            <span>年龄：{{ state.selectedPatient.age ?? '-' }}</span>
            <span>电话：{{ state.selectedPatient.phone || '-' }}</span>
          </div>
          <div class="health-summary">
            <strong>健康档案摘要</strong>
            <p>{{ state.currentHealthSummary }}</p>
            <el-button text @click="emit('go-health-management')">去完善健康档案</el-button>
          </div>
        </div>
      </div>

      <div class="route-action-row">
        <el-button
          plain
          :loading="state.routingLoading"
          :disabled="!canMatchRoute"
          @click="emit('match-route', 'quick')"
        >
          快速匹配
        </el-button>
        <el-button
          type="primary"
          :loading="state.routingLoading"
          :disabled="!canMatchRoute"
          @click="emit('match-route', 'ai')"
        >
          智能匹配
        </el-button>
      </div>

      <div v-if="state.currentRouteSummary && state.currentCategory" class="route-result-card">
        <div class="route-result-head">
          <div>
            <span class="panel-kicker">匹配结果</span>
            <h4>{{ state.currentCategory.departmentName || '综合问诊' }}</h4>
          </div>
          <div class="route-result-tags">
            <el-tag :type="state.currentRouteSummary.matchMode === 'quick' ? 'info' : 'primary'" effect="light">
              {{ state.routeModeLabel }}
            </el-tag>
            <el-tag type="success" effect="light">
              {{ state.routeSelectionLabel }}
            </el-tag>
          </div>
        </div>
        <p>{{ state.routeDescriptionText }}</p>
        <div class="template-meta">
          <span>问诊分类：{{ state.currentCategory.name }}</span>
          <span>参考费用：{{ helpers.formatAmount(state.currentCategory.priceAmount) }}</span>
          <span>模板：{{ state.template?.name || state.currentRouteSummary.template?.name || '-' }}</span>
          <span v-if="state.currentRouteSummary.confidenceScore !== null && state.currentRouteSummary.confidenceScore !== undefined">
            匹配置信度：{{ state.currentRouteSummary.confidenceScore }}
          </span>
        </div>
      </div>
    </div>

    <div class="form-card">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">步骤三</span>
          <h3>填写专科问诊资料</h3>
          <p v-if="state.currentCategory">
            {{ state.currentCategory.name }}{{ state.currentCategory.departmentName ? ` · ${state.currentCategory.departmentName}` : '' }}
          </p>
          <p v-else>完成智能匹配后，系统会自动加载对应专科模板。</p>
        </div>
        <div class="panel-actions">
          <el-button @click="emit('reset-form')" :disabled="!state.template">重置表单</el-button>
          <el-button type="primary" :loading="state.submitting" :disabled="!state.routeReady || !selectedPatientIdModel" @click="emit('submit-consultation')">
            提交并进入智能导诊
          </el-button>
        </div>
      </div>

      <div v-if="state.currentCategory" class="template-meta">
        <span>分类：{{ state.currentCategory.name }}</span>
        <span>参考费用：{{ helpers.formatAmount(state.currentCategory.priceAmount) }}</span>
        <span v-if="state.currentCategory.departmentName">科室：{{ state.currentCategory.departmentName }}</span>
      </div>

      <el-skeleton v-if="state.templateLoading || state.routingLoading" :rows="8" animated />

      <template v-else-if="state.template && state.currentRouteSummary">
        <el-alert
          :title="state.template.description || state.routeDescriptionText"
          type="info"
          :closable="false"
          class="template-alert"
        />

        <div class="template-meta">
          <span>模板：{{ state.template.name }}</span>
          <span>版本：V{{ state.template.version }}</span>
          <span>字段数：{{ state.visibleFields.length }}</span>
        </div>

        <div class="field-list">
          <article
            v-for="field in state.visibleFields"
            :key="field.fieldCode"
            class="field-card"
          >
            <div class="field-head">
              <div>
                <strong>{{ field.fieldLabel }}</strong>
                <span>{{ helpers.fieldTypeLabel(field.fieldType) }}{{ field.isRequired === 1 ? ' · 必填' : ' · 选填' }}</span>
              </div>
              <small v-if="field.helpText">{{ field.helpText }}</small>
            </div>

            <template v-if="field.fieldType === 'input'">
              <el-input v-model="state.formData[field.fieldCode]" :placeholder="field.placeholder || `请输入${field.fieldLabel}`" />
            </template>

            <template v-else-if="field.fieldType === 'textarea'">
              <el-input
                v-model="state.formData[field.fieldCode]"
                type="textarea"
                :rows="4"
                :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
              />
            </template>

            <template v-else-if="field.fieldType === 'single_select'">
              <el-select v-model="state.formData[field.fieldCode]" style="width: 100%" :placeholder="field.placeholder || `请选择${field.fieldLabel}`">
                <el-option
                  v-for="option in helpers.fieldOptions(field)"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </template>

            <template v-else-if="field.fieldType === 'multi_select'">
              <el-select
                v-model="state.formData[field.fieldCode]"
                multiple
                filterable
                collapse-tags
                collapse-tags-tooltip
                style="width: 100%"
                :placeholder="field.placeholder || `请选择${field.fieldLabel}`"
              >
                <el-option
                  v-for="option in helpers.fieldOptions(field)"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </template>

            <template v-else-if="field.fieldType === 'date'">
              <el-date-picker
                v-model="state.formData[field.fieldCode]"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                :placeholder="field.placeholder || `请选择${field.fieldLabel}`"
              />
            </template>

            <template v-else-if="field.fieldType === 'number'">
              <el-input-number v-model="state.formData[field.fieldCode]" :min="0" :max="999999" style="width: 100%" />
            </template>

            <template v-else-if="field.fieldType === 'switch'">
              <el-switch
                v-model="state.formData[field.fieldCode]"
                active-value="1"
                inactive-value="0"
                inline-prompt
                active-text="是"
                inactive-text="否"
              />
            </template>

            <template v-else-if="field.fieldType === 'upload'">
              <div class="upload-row">
                <el-upload
                  :action="state.uploadAction"
                  :headers="state.uploadHeaders"
                  :show-file-list="false"
                  accept="image/*"
                  :before-upload="helpers.beforeImageUpload"
                  :on-success="(response) => helpers.handleUploadSuccess(field, response)"
                  :on-error="helpers.handleUploadError"
                >
                  <el-button type="primary" plain>上传图片资料</el-button>
                </el-upload>
                <span class="upload-tip">{{ field.helpText || '当前阶段支持上传图片资料。' }}</span>
              </div>
              <div v-if="state.formData[field.fieldCode]" class="upload-preview">
                <img :src="helpers.resolveImagePath(state.formData[field.fieldCode])" :alt="field.fieldLabel" />
                <div class="upload-actions">
                  <span>已上传</span>
                  <el-button link type="danger" @click="state.formData[field.fieldCode] = ''">移除</el-button>
                </div>
              </div>
            </template>
          </article>
        </div>
      </template>

      <el-empty v-else description="请先完成症状描述与智能匹配，再开始填写专科问诊资料">
        <div class="route-action-row">
          <el-button plain :disabled="!canMatchRoute" @click="emit('match-route', 'quick')">快速匹配</el-button>
          <el-button type="primary" :disabled="!canMatchRoute" @click="emit('match-route', 'ai')">智能匹配</el-button>
        </div>
      </el-empty>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  state: {
    type: Object,
    required: true
  },
  helpers: {
    type: Object,
    required: true
  },
  selectedPatientId: {
    type: [Number, String],
    default: null
  },
  intakeChiefComplaint: {
    type: String,
    default: ''
  }
})

const emit = defineEmits([
  'update:selectedPatientId',
  'update:intakeChiefComplaint',
  'refresh-categories',
  'go-patient-management',
  'go-health-management',
  'match-route',
  'reset-form',
  'submit-consultation'
])

const selectedPatientIdModel = computed({
  get: () => props.selectedPatientId,
  set: (value) => emit('update:selectedPatientId', value)
})

const intakeChiefComplaintModel = computed({
  get: () => props.intakeChiefComplaint,
  set: (value) => emit('update:intakeChiefComplaint', value)
})

const canMatchRoute = computed(() => !!selectedPatientIdModel.value && !!`${intakeChiefComplaintModel.value || ''}`.trim())
</script>

<style scoped>
.entry-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
}

.side-card,
.form-card,
.field-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.side-card,
.form-card {
  padding: 22px;
}

.panel-head,
.panel-actions,
.patient-top,
.upload-row,
.upload-actions {
  display: flex;
  gap: 12px;
}

.panel-head,
.patient-top {
  justify-content: space-between;
  align-items: flex-start;
}

.panel-head h3 {
  margin: 6px 0 0;
}

.panel-head p {
  margin: 6px 0 0;
  color: var(--app-muted);
}

.panel-kicker,
.field-head span,
.field-head small {
  color: var(--app-muted);
}

.compact {
  margin-top: 20px;
}

.intake-block,
.route-result-card {
  margin-top: 16px;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(17, 70, 77, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.route-result-card {
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.08), rgba(255, 255, 255, 0.94));
}

.quick-chip-row,
.route-action-row,
.route-result-head {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.quick-chip-row,
.route-action-row {
  margin-top: 14px;
}

.route-result-head {
  justify-content: space-between;
  align-items: flex-start;
}

.route-result-head h4 {
  margin: 6px 0 0;
}

.route-result-card p,
.health-summary p {
  margin: 10px 0 0;
  color: #637b84;
  line-height: 1.75;
}

.quick-chip {
  padding: 8px 14px;
  border: 1px solid rgba(15, 102, 101, 0.14);
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.06);
  color: #225862;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
}

.quick-chip:hover {
  border-color: rgba(15, 102, 101, 0.3);
  background: rgba(15, 102, 101, 0.12);
  transform: translateY(-1px);
}

.field-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.patient-card {
  margin-top: 16px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.patient-card strong,
.field-head strong {
  display: block;
}

.patient-meta,
.template-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.patient-meta span,
.template-meta span {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #48656d;
  font-size: 12px;
}

.health-summary {
  margin-top: 16px;
}

.template-alert {
  margin-bottom: 16px;
}

.field-card {
  padding: 18px 20px;
}

.field-head {
  margin-bottom: 14px;
}

.upload-row {
  align-items: center;
  flex-wrap: wrap;
}

.upload-tip {
  color: var(--app-muted);
  font-size: 13px;
}

.upload-preview {
  margin-top: 16px;
}

.upload-preview img {
  width: 220px;
  height: 150px;
  object-fit: cover;
  border-radius: 18px;
  border: 1px solid rgba(17, 70, 77, 0.08);
}

@media (max-width: 1180px) {
  .entry-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .panel-head,
  .patient-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .panel-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
