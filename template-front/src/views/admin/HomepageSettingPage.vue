<template>
  <div class="homepage-setting-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>已启用推荐</span>
        <strong>{{ enabledRecommendCount }}</strong>
      </article>
      <article class="stat-card">
        <span>已启用案例</span>
        <strong>{{ enabledCaseCount }}</strong>
      </article>
      <article class="stat-card">
        <span>覆盖科室</span>
        <strong>{{ coveredDepartmentCount }}</strong>
      </article>
      <article class="stat-card">
        <span>首页更新</span>
        <strong>{{ formatDate(configForm.updateTime, true) }}</strong>
      </article>
    </section>

    <section class="panel-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基础信息" name="config">
          <div class="tab-toolbar">
            <div class="toolbar-actions">
              <el-button @click="loadConfig">重置</el-button>
              <el-button type="primary" :loading="configSubmitting" @click="saveConfig">
                保存配置
              </el-button>
            </div>
          </div>

          <el-form
            ref="configFormRef"
            :model="configForm"
            :rules="configRules"
            label-position="top"
            v-loading="configLoading"
          >
            <div class="form-grid">
              <el-form-item label="首页主标题" prop="heroTitle">
                <el-input
                  v-model="configForm.heroTitle"
                  maxlength="100"
                  placeholder="例如：智能问诊系统"
                />
              </el-form-item>
              <el-form-item label="首页副标题" prop="heroSubtitle">
                <el-input
                  v-model="configForm.heroSubtitle"
                  maxlength="255"
                  placeholder="例如：在线问诊、健康管理、智能分诊一体化服务"
                />
              </el-form-item>
              <el-form-item label="服务热线" prop="servicePhone">
                <el-input
                  v-model="configForm.servicePhone"
                  maxlength="30"
                  placeholder="例如：400-800-1234"
                />
              </el-form-item>
              <el-form-item label="首页公告" prop="noticeText">
                <el-input
                  v-model="configForm.noticeText"
                  maxlength="255"
                  placeholder="例如：门诊咨询时间 08:00 - 20:00"
                />
              </el-form-item>
            </div>

            <el-form-item label="平台简介标题" prop="introTitle">
              <el-input
                v-model="configForm.introTitle"
                maxlength="100"
                placeholder="例如：平台简介"
              />
            </el-form-item>

            <el-form-item label="平台简介内容" prop="introContent">
              <el-input
                v-model="configForm.introContent"
                type="textarea"
                :rows="6"
                maxlength="2000"
                show-word-limit
                placeholder="用于首页平台介绍、服务优势、智能问诊入口说明等展示内容"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="推荐医生" name="recommend">
          <div class="tab-toolbar">
            <el-input
              v-model="recommendKeyword"
              clearable
              placeholder="搜索医生姓名、职称、科室或推荐语"
              style="max-width: 360px"
            />
            <div class="toolbar-actions">
              <el-button @click="loadRecommendDoctors">刷新</el-button>
              <el-button type="primary" @click="openRecommendCreateDialog">新增推荐</el-button>
            </div>
          </div>

          <el-table :data="filteredRecommendDoctors" v-loading="recommendLoading" border>
            <el-table-column label="医生" min-width="220">
              <template #default="{ row }">
                <div class="doctor-cell">
                  <el-avatar
                    :size="46"
                    :src="resolveImagePath(doctorInfo(row.doctorId)?.photo) || undefined"
                  >
                    {{ doctorInfo(row.doctorId)?.name?.slice(0, 1) || 'D' }}
                  </el-avatar>
                  <div class="doctor-meta">
                    <strong>{{ doctorInfo(row.doctorId)?.name || '医生已删除' }}</strong>
                    <span>
                      {{ doctorDepartmentText(row.doctorId) }}
                    </span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="displayTitle" label="展示标题" min-width="160" show-overflow-tooltip />
            <el-table-column prop="recommendReason" label="推荐语" min-width="220" show-overflow-tooltip />
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
                <el-button link type="primary" @click="openRecommendEditDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="removeRecommendDoctor(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="经典案例" name="case">
          <div class="tab-toolbar">
            <el-input
              v-model="caseKeyword"
              clearable
              placeholder="搜索案例标题、科室、医生或标签"
              style="max-width: 360px"
            />
            <div class="toolbar-actions">
              <el-button @click="loadCases">刷新</el-button>
              <el-button type="primary" @click="openCaseCreateDialog">新增案例</el-button>
            </div>
          </div>

          <el-table :data="filteredCases" v-loading="caseLoading" border>
            <el-table-column label="封面" width="110" align="center">
              <template #default="{ row }">
                <el-image
                  class="case-cover"
                  :src="resolveImagePath(row.cover)"
                  fit="cover"
                  preview-teleported
                  :preview-src-list="[resolveImagePath(row.cover)]"
                />
              </template>
            </el-table-column>
            <el-table-column prop="title" label="案例标题" min-width="180" />
            <el-table-column label="关联科室" min-width="140">
              <template #default="{ row }">
                {{ departmentName(row.departmentId) }}
              </template>
            </el-table-column>
            <el-table-column label="关联医生" min-width="160">
              <template #default="{ row }">
                {{ doctorInfo(row.doctorId)?.name || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="summary" label="案例摘要" min-width="220" show-overflow-tooltip />
            <el-table-column prop="tags" label="标签" min-width="160" show-overflow-tooltip />
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
                <el-button link type="primary" @click="openCaseEditDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="removeCase(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog
      v-model="recommendDialogVisible"
      :title="recommendEditing ? '编辑推荐医生' : '新增推荐医生'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="recommendFormRef" :model="recommendForm" :rules="recommendRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="选择医生" prop="doctorId">
            <el-select v-model="recommendForm.doctorId" filterable style="width: 100%" placeholder="请选择医生">
              <el-option
                v-for="item in doctors"
                :key="item.id"
                :label="doctorOptionLabel(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="展示标题" prop="displayTitle">
            <el-input
              v-model="recommendForm.displayTitle"
              maxlength="100"
              placeholder="例如：本周热门专家"
            />
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="recommendForm.sort" :min="0" :max="999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="recommendForm.status" style="width: 100%">
              <el-option :value="1" label="启用" />
              <el-option :value="0" label="停用" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="推荐语" prop="recommendReason">
          <el-input
            v-model="recommendForm.recommendReason"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="用于首页展示医生简介、推荐理由或专长概述"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="recommendDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="recommendSubmitting" @click="submitRecommendDoctor">
          {{ recommendEditing ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="caseDialogVisible"
      :title="caseEditing ? '编辑经典案例' : '新增经典案例'"
      width="820px"
      destroy-on-close
    >
      <el-form ref="caseFormRef" :model="caseForm" :rules="caseRules" label-position="top">
        <div class="cover-panel">
          <el-upload
            :action="caseUploadAction"
            :headers="caseUploadHeaders"
            :show-file-list="false"
            :before-upload="beforeCaseCoverUpload"
            :on-success="handleCaseCoverSuccess"
            :on-error="handleCaseCoverError"
            accept="image/*"
            class="cover-uploader"
          >
            <div class="cover-card">
              <el-image v-if="caseCoverPreview" :src="caseCoverPreview" fit="cover" class="cover-preview" />
              <template v-else>
                <span class="cover-placeholder">点击上传案例封面</span>
              </template>
            </div>
          </el-upload>
          <div class="cover-actions">
            <span>建议使用清晰横图，大小不超过 3MB。</span>
            <el-button v-if="caseForm.cover" link type="danger" @click="caseForm.cover = ''">
              移除封面
            </el-button>
          </div>
        </div>

        <div class="form-grid">
          <el-form-item label="案例标题" prop="title">
            <el-input v-model="caseForm.title" maxlength="100" placeholder="请输入案例标题" />
          </el-form-item>
          <el-form-item label="关联科室" prop="departmentId">
            <el-select v-model="caseForm.departmentId" style="width: 100%" placeholder="请选择科室">
              <el-option
                v-for="item in departments"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="关联医生">
            <el-select
              v-model="caseForm.doctorId"
              clearable
              filterable
              style="width: 100%"
              placeholder="可选，若不关联可留空"
            >
              <el-option
                v-for="item in doctorsByDepartment"
                :key="item.id"
                :label="doctorOptionLabel(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标签" prop="tags">
            <el-input v-model="caseForm.tags" maxlength="255" placeholder="例如：慢病管理, 高血压, 远程随访" />
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="caseForm.sort" :min="0" :max="999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="caseForm.status" style="width: 100%">
              <el-option :value="1" label="启用" />
              <el-option :value="0" label="停用" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="案例摘要" prop="summary">
          <el-input
            v-model="caseForm.summary"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="用于首页列表卡片的简要展示"
          />
        </el-form-item>

        <el-form-item label="案例详情" prop="detail">
          <el-input
            v-model="caseForm.detail"
            type="textarea"
            :rows="6"
            maxlength="3000"
            show-word-limit
            placeholder="可填写病情背景、诊疗过程、复诊建议、管理亮点等完整内容"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="caseDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="caseSubmitting" @click="submitCase">
          {{ caseEditing ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { authHeader, backendBaseUrl, get, post, resolveImagePath } from '@/net'

const activeTab = ref('config')

const configLoading = ref(false)
const configSubmitting = ref(false)
const recommendLoading = ref(false)
const recommendSubmitting = ref(false)
const caseLoading = ref(false)
const caseSubmitting = ref(false)

const recommendKeyword = ref('')
const caseKeyword = ref('')

const configFormRef = ref()
const recommendFormRef = ref()
const caseFormRef = ref()

const doctors = ref([])
const departments = ref([])
const recommendDoctors = ref([])
const cases = ref([])

const recommendDialogVisible = ref(false)
const caseDialogVisible = ref(false)

const configForm = reactive(createEmptyConfigForm())
const recommendForm = reactive(createEmptyRecommendForm())
const caseForm = reactive(createEmptyCaseForm())

const configRules = {
  heroTitle: [
    { required: true, message: '请输入首页主标题', trigger: 'blur' }
  ]
}

const recommendRules = {
  doctorId: [
    { required: true, message: '请选择医生', trigger: 'change' }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const caseRules = {
  departmentId: [
    { required: true, message: '请选择科室', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入案例标题', trigger: 'blur' }
  ],
  cover: [
    { required: true, message: '请上传案例封面', trigger: 'change' }
  ],
  summary: [
    { required: true, message: '请输入案例摘要', trigger: 'blur' }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const doctorMap = computed(() => {
  const map = new Map()
  doctors.value.forEach(item => map.set(item.id, item))
  return map
})

const departmentMap = computed(() => {
  const map = new Map()
  departments.value.forEach(item => map.set(item.id, item))
  return map
})

const enabledRecommendCount = computed(() => recommendDoctors.value.filter(item => item.status === 1).length)
const enabledCaseCount = computed(() => cases.value.filter(item => item.status === 1).length)
const coveredDepartmentCount = computed(() => {
  return new Set(cases.value.map(item => item.departmentId).filter(Boolean)).size
})

const filteredRecommendDoctors = computed(() => {
  const value = recommendKeyword.value.trim().toLowerCase()
  if (!value) return recommendDoctors.value
  return recommendDoctors.value.filter(item => {
    const doctor = doctorInfo(item.doctorId)
    return [
      item.displayTitle,
      item.recommendReason,
      doctor?.name,
      doctor?.title,
      departmentName(doctor?.departmentId)
    ]
      .filter(Boolean)
      .some(text => String(text).toLowerCase().includes(value))
  })
})

const filteredCases = computed(() => {
  const value = caseKeyword.value.trim().toLowerCase()
  if (!value) return cases.value
  return cases.value.filter(item =>
    [
      item.title,
      item.summary,
      item.tags,
      departmentName(item.departmentId),
      doctorInfo(item.doctorId)?.name
    ]
      .filter(Boolean)
      .some(text => String(text).toLowerCase().includes(value))
  )
})

const recommendEditing = computed(() => !!recommendForm.id)
const caseEditing = computed(() => !!caseForm.id)
const caseUploadAction = computed(() => `${backendBaseUrl()}/api/admin/homepage/upload-image?type=case`)
const caseUploadHeaders = computed(() => authHeader())
const caseCoverPreview = computed(() => resolveImagePath(caseForm.cover))
const doctorsByDepartment = computed(() => {
  if (!caseForm.departmentId) return doctors.value
  return doctors.value.filter(item => item.departmentId === caseForm.departmentId)
})

watch(() => caseForm.departmentId, () => {
  if (caseForm.doctorId && !doctorsByDepartment.value.some(item => item.id === caseForm.doctorId)) {
    caseForm.doctorId = null
  }
})

function createEmptyConfigForm() {
  return {
    id: 1,
    heroTitle: '',
    heroSubtitle: '',
    noticeText: '',
    introTitle: '',
    introContent: '',
    servicePhone: '',
    updateTime: ''
  }
}

function createEmptyRecommendForm() {
  return {
    id: null,
    doctorId: null,
    displayTitle: '',
    recommendReason: '',
    sort: 0,
    status: 1
  }
}

function createEmptyCaseForm() {
  return {
    id: null,
    departmentId: null,
    doctorId: null,
    title: '',
    cover: '',
    summary: '',
    detail: '',
    tags: '',
    sort: 0,
    status: 1
  }
}

function loadConfig() {
  configLoading.value = true
  get('/api/admin/homepage/config', (data) => {
    Object.assign(configForm, createEmptyConfigForm(), data || {})
    configLoading.value = false
  }, () => {
    configLoading.value = false
  })
}

function loadDoctors() {
  get('/api/admin/doctor/list', (data) => {
    doctors.value = data || []
  })
}

function loadDepartments() {
  get('/api/admin/department/list', (data) => {
    departments.value = data || []
  })
}

function loadRecommendDoctors() {
  recommendLoading.value = true
  get('/api/admin/homepage/recommend-doctor/list', (data) => {
    recommendDoctors.value = data || []
    recommendLoading.value = false
  }, () => {
    recommendLoading.value = false
  })
}

function loadCases() {
  caseLoading.value = true
  get('/api/admin/homepage/case/list', (data) => {
    cases.value = data || []
    caseLoading.value = false
  }, () => {
    caseLoading.value = false
  })
}

function loadData() {
  loadConfig()
  loadDoctors()
  loadDepartments()
  loadRecommendDoctors()
  loadCases()
}

function saveConfig() {
  configFormRef.value.validate((valid) => {
    if (!valid) return

    configSubmitting.value = true
    post('/api/admin/homepage/config', {
      heroTitle: configForm.heroTitle,
      heroSubtitle: configForm.heroSubtitle,
      noticeText: configForm.noticeText,
      introTitle: configForm.introTitle,
      introContent: configForm.introContent,
      servicePhone: configForm.servicePhone
    }, () => {
      configSubmitting.value = false
      ElMessage.success('首页基础信息已保存')
      loadConfig()
    }, (message) => {
      configSubmitting.value = false
      ElMessage.warning(message || '首页基础信息保存失败')
    })
  })
}

function openRecommendCreateDialog() {
  if (!doctors.value.length) {
    ElMessage.warning('请先维护医生信息，再添加推荐医生')
    return
  }
  Object.assign(recommendForm, createEmptyRecommendForm())
  recommendDialogVisible.value = true
}

function openRecommendEditDialog(row) {
  Object.assign(recommendForm, createEmptyRecommendForm(), row)
  recommendDialogVisible.value = true
}

function submitRecommendDoctor() {
  recommendFormRef.value.validate((valid) => {
    if (!valid) return

    const payload = {
      doctorId: recommendForm.doctorId,
      displayTitle: recommendForm.displayTitle,
      recommendReason: recommendForm.recommendReason,
      sort: recommendForm.sort,
      status: recommendForm.status
    }

    if (recommendEditing.value) {
      payload.id = recommendForm.id
    }

    recommendSubmitting.value = true
    post(
      recommendEditing.value
        ? '/api/admin/homepage/recommend-doctor/update'
        : '/api/admin/homepage/recommend-doctor/create',
      payload,
      () => {
        recommendSubmitting.value = false
        recommendDialogVisible.value = false
        ElMessage.success(recommendEditing.value ? '推荐医生已更新' : '推荐医生已新增')
        loadRecommendDoctors()
      },
      (message) => {
        recommendSubmitting.value = false
        ElMessage.warning(message || '推荐医生保存失败')
      }
    )
  })
}

function removeRecommendDoctor(row) {
  ElMessageBox.confirm(`确认删除推荐医生“${doctorInfo(row.doctorId)?.name || '未命名医生'}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/homepage/recommend-doctor/delete?id=${row.id}`, () => {
      ElMessage.success('推荐医生已删除')
      loadRecommendDoctors()
    }, (message) => {
      ElMessage.warning(message || '推荐医生删除失败')
    })
  }).catch(() => {})
}

function openCaseCreateDialog() {
  if (!departments.value.length) {
    ElMessage.warning('请先维护科室信息，再新增经典案例')
    return
  }
  Object.assign(caseForm, createEmptyCaseForm())
  caseDialogVisible.value = true
}

function openCaseEditDialog(row) {
  Object.assign(caseForm, createEmptyCaseForm(), row)
  caseDialogVisible.value = true
}

function submitCase() {
  caseFormRef.value.validate((valid) => {
    if (!valid) return

    const payload = {
      departmentId: caseForm.departmentId,
      doctorId: caseForm.doctorId,
      title: caseForm.title,
      cover: caseForm.cover,
      summary: caseForm.summary,
      detail: caseForm.detail,
      tags: caseForm.tags,
      sort: caseForm.sort,
      status: caseForm.status
    }

    if (caseEditing.value) {
      payload.id = caseForm.id
    }

    caseSubmitting.value = true
    post(
      caseEditing.value ? '/api/admin/homepage/case/update' : '/api/admin/homepage/case/create',
      payload,
      () => {
        caseSubmitting.value = false
        caseDialogVisible.value = false
        ElMessage.success(caseEditing.value ? '经典案例已更新' : '经典案例已新增')
        loadCases()
      },
      (message) => {
        caseSubmitting.value = false
        ElMessage.warning(message || '经典案例保存失败')
      }
    )
  })
}

function removeCase(row) {
  ElMessageBox.confirm(`确认删除经典案例“${row.title}”吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    get(`/api/admin/homepage/case/delete?id=${row.id}`, () => {
      ElMessage.success('经典案例已删除')
      loadCases()
    }, (message) => {
      ElMessage.warning(message || '经典案例删除失败')
    })
  }).catch(() => {})
}

function beforeCaseCoverUpload(file) {
  const isImage = `${file.type || ''}`.startsWith('image/')
  const isLt3M = file.size / 1024 / 1024 <= 3

  if (!isImage) {
    ElMessage.error('案例封面必须是图片格式')
    return false
  }
  if (!isLt3M) {
    ElMessage.error('案例封面不能超过 3MB')
    return false
  }
  return true
}

function handleCaseCoverSuccess(response) {
  if (response?.code !== 200) {
    ElMessage.error(response?.message || '案例封面上传失败')
    return
  }
  caseForm.cover = response.data
  caseFormRef.value?.validateField?.('cover')
  ElMessage.success('案例封面上传成功')
}

function handleCaseCoverError() {
  ElMessage.error('案例封面上传失败，请稍后重试')
}

function doctorInfo(id) {
  if (!id) return null
  return doctorMap.value.get(id) || null
}

function departmentName(id) {
  if (!id) return '-'
  return departmentMap.value.get(id)?.name || '-'
}

function doctorDepartmentText(doctorId) {
  const doctor = doctorInfo(doctorId)
  if (!doctor) return '-'
  const parts = [departmentName(doctor.departmentId), doctor.title].filter(Boolean)
  return parts.join(' / ') || '-'
}

function doctorOptionLabel(item) {
  return [item.name, item.title, departmentName(item.departmentId)].filter(Boolean).join(' / ')
}

function formatDate(value, short = false) {
  if (!value) return '-'
  const formatOptions = short
    ? {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      }
    : {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      }
  return new Intl.DateTimeFormat('zh-CN', formatOptions).format(new Date(value))
}

onMounted(() => loadData())
</script>

<style scoped>
.homepage-setting-page {
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

.stat-card span {
  color: var(--app-muted);
}

.stat-card strong {
  display: block;
  margin-top: 14px;
  font-size: 28px;
  line-height: 1.1;
}

.panel-card {
  padding: 22px;
}

.tab-toolbar {
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.doctor-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.doctor-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.doctor-meta strong,
.doctor-meta span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doctor-meta span {
  color: var(--app-muted);
  font-size: 13px;
}

.case-cover {
  width: 62px;
  height: 62px;
  border-radius: 16px;
  overflow: hidden;
}

.cover-panel {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 18px;
}

.cover-uploader {
  cursor: pointer;
}

.cover-card {
  width: 220px;
  height: 132px;
  border: 1px dashed rgba(24, 54, 61, 0.18);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.cover-preview {
  width: 100%;
  height: 100%;
  display: block;
}

.cover-placeholder {
  color: var(--app-muted);
  font-size: 13px;
}

.cover-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  color: var(--app-muted);
  font-size: 13px;
}

@media (max-width: 1100px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .form-grid,
  .stat-grid {
    grid-template-columns: 1fr;
  }

  .tab-toolbar,
  .cover-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .cover-card {
    width: 100%;
    max-width: 320px;
  }
}
</style>
