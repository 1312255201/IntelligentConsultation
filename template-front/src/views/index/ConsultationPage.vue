<template>
  <div class="consultation-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>可用分类</span>
        <strong>{{ categories.length }}</strong>
      </article>
      <article class="stat-card">
        <span>就诊人</span>
        <strong>{{ patients.length }}</strong>
      </article>
      <article class="stat-card">
        <span>已提交问诊</span>
        <strong>{{ records.length }}</strong>
      </article>
      <article class="stat-card">
        <span>待支付</span>
        <strong>{{ pendingPaymentCount }}</strong>
      </article>
      <article class="stat-card">
        <span>今日新增</span>
        <strong>{{ todayCount }}</strong>
      </article>
      <article class="stat-card">
        <span>医生新回复</span>
        <strong>{{ unreadDoctorReplyCount }}</strong>
      </article>
      <article class="stat-card">
        <span>待医生处理</span>
        <strong>{{ waitingDoctorHandleCount }}</strong>
      </article>
      <article class="stat-card">
        <span>待随访</span>
        <strong>{{ pendingFollowUpCount }}</strong>
      </article>
      <article class="stat-card">
        <span>待服务评价</span>
        <strong>{{ pendingServiceFeedbackCount }}</strong>
      </article>

      <article class="reminder-card">
        <div class="reminder-head">
          <div>
            <span class="panel-kicker">Feedback</span>
            <h3>待服务评价</h3>
            <p>医生完成处理后，可在这里快速补充服务评分、问题是否解决和本次问诊体验。</p>
          </div>
          <el-tag type="info" effect="light">{{ pendingServiceFeedbackCount }}</el-tag>
        </div>
        <div v-if="pendingServiceFeedbackRecords.length" class="reminder-list">
          <button
            v-for="item in pendingServiceFeedbackRecords"
            :key="`feedback-${item.id}`"
            type="button"
            class="reminder-item"
            @click="openRecordDetail(item, { action: 'feedback' })"
          >
            <div class="reminder-item-head">
              <strong>{{ item.patientName || '未命名就诊人' }}</strong>
              <span>{{ serviceFeedbackTimeText(item) }}</span>
            </div>
            <p>{{ serviceFeedbackReminderText(item) }}</p>
            <div class="reminder-item-meta">
              <span>{{ item.categoryName || '未分类' }}</span>
              <span>待提交服务评价</span>
            </div>
          </button>
        </div>
        <el-empty v-else description="当前没有待服务评价的问诊" />
        <div class="reminder-foot">
          <el-button text @click="applyRecordQuickFilter({ progress: 'pending_feedback' })">只看待服务评价</el-button>
        </div>
      </article>
    </section>

    <section class="reminder-grid">
      <article class="reminder-card">
        <div class="reminder-head">
          <div>
            <span class="panel-kicker">Reminder</span>
            <h3>医生新回复</h3>
            <p>优先查看医生刚回复的问诊，避免错过进一步处理建议。</p>
          </div>
          <el-tag type="success" effect="light">{{ unreadDoctorReplyCount }}</el-tag>
        </div>
        <div v-if="unreadReplyRecords.length" class="reminder-list">
          <button
            v-for="item in unreadReplyRecords"
            :key="`reply-${item.id}`"
            type="button"
            class="reminder-item"
            @click="openRecordDetail(item, { action: 'conversation' })"
          >
            <div class="reminder-item-head">
              <strong>{{ item.patientName || '未命名就诊人' }}</strong>
              <span>{{ formatDate(getMessageSummary(item).latestTime) }}</span>
            </div>
            <p>{{ recordMessagePreview(item) }}</p>
            <div class="reminder-item-meta">
              <span>{{ item.categoryName || '未分类' }}</span>
              <span>未读 {{ getMessageSummary(item).unreadCount }} 条</span>
            </div>
          </button>
        </div>
        <el-empty v-else description="当前没有新的医生回复" />
        <div class="reminder-foot">
          <el-button text @click="applyRecordQuickFilter({ progress: 'doctor_replied' })">只看医生新回复</el-button>
        </div>
      </article>

      <article class="reminder-card">
        <div class="reminder-head">
          <div>
            <span class="panel-kicker">Queue</span>
            <h3>待医生处理</h3>
            <p>这些问诊还在等待医生接手或继续处理，可随时补充更多症状与资料。</p>
          </div>
          <el-tag type="warning" effect="light">{{ waitingDoctorHandleCount }}</el-tag>
        </div>
        <div v-if="waitingDoctorRecords.length" class="reminder-list">
          <button
            v-for="item in waitingDoctorRecords"
            :key="`waiting-${item.id}`"
            type="button"
            class="reminder-item"
            @click="openRecordDetail(item, { action: 'conversation' })"
          >
            <div class="reminder-item-head">
              <strong>{{ item.patientName || '未命名就诊人' }}</strong>
              <span>{{ formatDate(item.createTime) }}</span>
            </div>
            <p>{{ recordProgressHint(item) }}</p>
            <div class="reminder-item-meta">
              <span>{{ item.categoryName || '未分类' }}</span>
              <span>{{ smartDispatchStatusLabel(item.smartDispatch) }}</span>
            </div>
          </button>
        </div>
        <el-empty v-else description="当前没有待医生处理的问诊" />
        <div class="reminder-foot">
          <el-button text @click="applyRecordQuickFilter({ progress: 'waiting_doctor' })">只看待处理问诊</el-button>
        </div>
      </article>

      <article class="reminder-card reminder-card-followup">
        <div class="reminder-head">
          <div>
            <span class="panel-kicker">Follow-up</span>
            <h3>待随访提醒</h3>
            <p>及时查看需要继续跟进的问诊，尤其是今日到期和已逾期的随访。</p>
          </div>
          <div class="reminder-tags">
            <el-tag type="primary" effect="light">{{ pendingFollowUpCount }}</el-tag>
            <el-tag v-if="dueTodayFollowUpCount" type="warning" effect="light">今日 {{ dueTodayFollowUpCount }}</el-tag>
            <el-tag v-if="overdueFollowUpCount" type="danger" effect="light">逾期 {{ overdueFollowUpCount }}</el-tag>
          </div>
        </div>
        <div v-if="followUpReminderRecords.length" class="reminder-list">
          <button
            v-for="item in followUpReminderRecords"
            :key="`followup-${item.id}`"
            type="button"
            :class="['reminder-item', followUpReminderItemClass(item)]"
            @click="openRecordDetail(item, { action: 'followup' })"
          >
            <div class="reminder-item-head">
              <strong>{{ item.patientName || '未命名就诊人' }}</strong>
              <span>{{ followUpLine(item) }}</span>
            </div>
            <p>{{ item?.doctorConclusion?.patientInstruction || item?.doctorHandle?.followUpPlan || '医生已建议继续关注恢复情况。' }}</p>
            <div class="reminder-item-meta">
              <span>{{ item.categoryName || '未分类' }}</span>
              <span>{{ followUpTagLabel(item) }}</span>
            </div>
          </button>
        </div>
        <el-empty v-else description="当前没有待随访问诊" />
        <div class="reminder-foot">
          <el-button text @click="applyRecordQuickFilter({ followUp: 'pending' })">只看待随访</el-button>
          <el-button v-if="overdueFollowUpCount" text type="danger" @click="applyRecordQuickFilter({ followUp: 'overdue' })">优先看逾期</el-button>
        </div>
      </article>
    </section>

    <section class="entry-layout">
      <div class="side-card">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">Step 1</span>
            <h3>选择问诊分类</h3>
          </div>
          <el-button text @click="loadCategories">刷新分类</el-button>
        </div>

        <div v-if="categories.length" class="category-list">
          <button
            v-for="item in categories"
            :key="item.id"
            type="button"
            :class="['category-card', { active: item.id === activeCategoryId }]"
            @click="selectCategory(item)"
          >
            <span class="category-dept">{{ item.departmentName || '未配置科室' }}</span>
            <strong>{{ item.name }}</strong>
            <p>{{ item.description || item.defaultTemplateDescription || '用于装配对应场景下的问诊前置资料。' }}</p>
            <div class="category-meta">
              <span>{{ formatAmount(item.priceAmount) }}</span>
              <span>{{ item.defaultTemplateName }}</span>
              <span>{{ item.defaultTemplateFieldCount || 0 }} 项字段</span>
            </div>
          </button>
        </div>
        <el-empty v-else description="管理员尚未配置可用的问诊分类">
          <el-button type="primary" @click="loadCategories">重新加载</el-button>
        </el-empty>

        <div class="patient-block">
          <div class="panel-head compact">
            <div>
              <span class="panel-kicker">Step 2</span>
              <h3>选择就诊人</h3>
            </div>
            <el-button text @click="router.push('/index/patient')">去管理</el-button>
          </div>

          <el-select
            v-model="selectedPatientId"
            filterable
            style="width: 100%"
            placeholder="请选择就诊人"
          >
            <el-option
              v-for="item in patients"
              :key="item.id"
              :label="`${item.name} / ${relationLabel(item.relationType)}${item.isDefault === 1 ? ' / 默认' : ''}`"
              :value="item.id"
            />
          </el-select>

          <div v-if="selectedPatient" class="patient-card">
            <div class="patient-top">
              <div>
                <strong>{{ selectedPatient.name }}</strong>
                <span>{{ relationLabel(selectedPatient.relationType) }}</span>
              </div>
              <el-tag :type="selectedPatient.status === 1 ? 'success' : 'info'" effect="light">
                {{ selectedPatient.status === 1 ? '可用' : '停用' }}
              </el-tag>
            </div>
            <div class="patient-meta">
              <span>性别：{{ genderLabel(selectedPatient.gender) }}</span>
              <span>年龄：{{ selectedPatient.age ?? '-' }}</span>
              <span>电话：{{ selectedPatient.phone || '-' }}</span>
            </div>
            <div class="health-summary">
              <strong>健康档案摘要</strong>
              <p>{{ currentHealthSummary }}</p>
              <el-button text @click="router.push('/index/health')">去完善健康档案</el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="form-card">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">Step 3</span>
            <h3>填写问诊资料</h3>
            <p v-if="currentCategory">
              {{ currentCategory.name }}{{ currentCategory.departmentName ? ` · ${currentCategory.departmentName}` : '' }}
            </p>
          </div>
          <div class="panel-actions">
            <el-button @click="resetForm" :disabled="!template">重置表单</el-button>
            <el-button type="primary" :loading="submitting" :disabled="!template || !selectedPatientId" @click="submitConsultation">
              提交并进入 AI 导诊
            </el-button>
          </div>
        </div>

        <div v-if="currentCategory" class="template-meta">
          <span>分类：{{ currentCategory.name }}</span>
          <span>参考费用：{{ formatAmount(currentCategory.priceAmount) }}</span>
          <span v-if="currentCategory.departmentName">科室：{{ currentCategory.departmentName }}</span>
        </div>

        <el-skeleton v-if="templateLoading" :rows="8" animated />

        <template v-else-if="template">
          <el-alert
            :title="template.description || '请根据实际情况填写当前问诊资料，提交后系统会自动进入 AI 导诊工作区，继续生成建议和追问。'"
            type="info"
            :closable="false"
            class="template-alert"
          />

          <div class="template-meta">
            <span>模板：{{ template.name }}</span>
            <span>版本：V{{ template.version }}</span>
            <span>字段数：{{ visibleFields.length }}</span>
          </div>

          <div class="field-list">
            <article
              v-for="field in visibleFields"
              :key="field.fieldCode"
              class="field-card"
            >
              <div class="field-head">
                <div>
                  <strong>{{ field.fieldLabel }}</strong>
                  <span>{{ fieldTypeLabel(field.fieldType) }}{{ field.isRequired === 1 ? ' · 必填' : ' · 选填' }}</span>
                </div>
                <small v-if="field.helpText">{{ field.helpText }}</small>
              </div>

              <template v-if="field.fieldType === 'input'">
                <el-input v-model="formData[field.fieldCode]" :placeholder="field.placeholder || `请输入${field.fieldLabel}`" />
              </template>

              <template v-else-if="field.fieldType === 'textarea'">
                <el-input
                  v-model="formData[field.fieldCode]"
                  type="textarea"
                  :rows="4"
                  :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
                />
              </template>

              <template v-else-if="field.fieldType === 'single_select'">
                <el-select v-model="formData[field.fieldCode]" style="width: 100%" :placeholder="field.placeholder || `请选择${field.fieldLabel}`">
                  <el-option
                    v-for="option in fieldOptions(field)"
                    :key="option"
                    :label="option"
                    :value="option"
                  />
                </el-select>
              </template>

              <template v-else-if="field.fieldType === 'multi_select'">
                <el-select
                  v-model="formData[field.fieldCode]"
                  multiple
                  filterable
                  collapse-tags
                  collapse-tags-tooltip
                  style="width: 100%"
                  :placeholder="field.placeholder || `请选择${field.fieldLabel}`"
                >
                  <el-option
                    v-for="option in fieldOptions(field)"
                    :key="option"
                    :label="option"
                    :value="option"
                  />
                </el-select>
              </template>

              <template v-else-if="field.fieldType === 'date'">
                <el-date-picker
                  v-model="formData[field.fieldCode]"
                  type="date"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                  :placeholder="field.placeholder || `请选择${field.fieldLabel}`"
                />
              </template>

              <template v-else-if="field.fieldType === 'number'">
                <el-input-number v-model="formData[field.fieldCode]" :min="0" :max="999999" style="width: 100%" />
              </template>

              <template v-else-if="field.fieldType === 'switch'">
                <el-switch
                  v-model="formData[field.fieldCode]"
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
                    :action="uploadAction"
                    :headers="uploadHeaders"
                    :show-file-list="false"
                    accept="image/*"
                    :before-upload="beforeImageUpload"
                    :on-success="(response) => handleUploadSuccess(field, response)"
                    :on-error="handleUploadError"
                  >
                    <el-button type="primary" plain>上传图片资料</el-button>
                  </el-upload>
                  <span class="upload-tip">{{ field.helpText || '当前阶段支持上传图片资料。' }}</span>
                </div>
                <div v-if="formData[field.fieldCode]" class="upload-preview">
                  <img :src="resolveImagePath(formData[field.fieldCode])" :alt="field.fieldLabel" />
                  <div class="upload-actions">
                    <span>已上传</span>
                    <el-button link type="danger" @click="formData[field.fieldCode] = ''">移除</el-button>
                  </div>
                </div>
              </template>
            </article>
          </div>
        </template>

        <el-empty v-else description="请选择问诊分类后开始填写资料">
          <el-button type="primary" @click="loadCategories">重新加载分类</el-button>
        </el-empty>
      </div>
    </section>

    <section ref="historySectionRef" class="history-card">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">History</span>
          <h3>问诊记录</h3>
        </div>
        <el-button @click="loadRecords">刷新记录</el-button>
      </div>

      <div class="history-toolbar">
        <el-input v-model="recordKeyword" clearable placeholder="搜索标题、分类、就诊人或消息" style="width: 260px" />
        <el-select v-model="recordStatusFilter" clearable placeholder="全部状态" style="width: 150px">
          <el-option label="已提交" value="submitted" />
          <el-option label="已分诊" value="triaged" />
          <el-option label="处理中" value="processing" />
          <el-option label="已完成" value="completed" />
        </el-select>
        <el-select v-model="recordProgressFilter" style="width: 170px">
          <el-option label="待服务评价" value="pending_feedback" />
          <el-option label="全部进度" value="all" />
          <el-option label="医生新回复" value="doctor_replied" />
          <el-option label="待医生处理" value="waiting_doctor" />
          <el-option label="医生处理中" value="doctor_processing" />
          <el-option label="已完成" value="completed" />
        </el-select>
        <el-select v-model="recordFollowUpFilter" style="width: 170px">
          <el-option label="全部随访" value="all" />
          <el-option label="待随访" value="pending" />
          <el-option label="今日到期" value="due_today" />
          <el-option label="已逾期" value="overdue" />
        </el-select>
      </div>

      <el-table :data="filteredRecords" v-loading="historyLoading" border :row-class-name="recordRowClassName">
        <el-table-column label="初步分诊" min-width="140" align="center">
          <template #default="{ row }">
            <span class="triage-badge" :style="triageBadgeStyle(row.triageLevelColor)">
              {{ row.triageLevelName || '待评估' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="consultationNo" label="记录编号" min-width="170" />
        <el-table-column prop="patientName" label="就诊人" min-width="120" />
        <el-table-column prop="categoryName" label="分类" min-width="140" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="warning" effect="light">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="收费" min-width="170">
          <template #default="{ row }">
            <div class="record-message-cell">
              <el-tag :type="paymentStatusTagType(row.payment)" effect="light">{{ paymentStatusLabel(row.payment) }}</el-tag>
              <span>{{ formatAmount(row.payment?.amount) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="智能分配" min-width="220">
          <template #default="{ row }">
            <div class="record-message-cell">
              <strong>{{ smartDispatchStatusLabel(row.smartDispatch) }}</strong>
              <span>{{ smartDispatchLine(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="沟通状态" min-width="220">
          <template #default="{ row }">
            <div class="record-message-cell">
              <strong>{{ recordMessageStatus(row) }}</strong>
              <span>{{ recordMessagePreview(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="问诊进度" min-width="180">
          <template #default="{ row }">
            <div class="record-message-cell">
              <el-tag :type="recordProgressTagType(row)" effect="light">{{ recordProgressLabel(row) }}</el-tag>
              <span>{{ recordProgressHint(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="随访提醒" min-width="180">
          <template #default="{ row }">
            <div class="record-message-cell">
              <el-tag :type="followUpTagType(row)" effect="light">{{ followUpTagLabel(row) }}</el-tag>
              <span>{{ followUpLine(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="healthSummary" label="健康摘要" min-width="220" show-overflow-tooltip />
        <el-table-column label="提交时间" min-width="170">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRecordDetail(row)">查看详情</el-button>
            <el-button link type="success" @click="openTriageWorkspace(row.id)">AI 导诊</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="detailVisible" title="问诊记录详情" width="900px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detailRecord">
          <div class="detail-meta">
            <article><span>记录编号</span><strong>{{ detailRecord.consultationNo }}</strong></article>
            <article><span>就诊人</span><strong>{{ detailRecord.patientName }}</strong></article>
            <article><span>问诊分类</span><strong>{{ detailRecord.categoryName }}</strong></article>
            <article><span>状态</span><strong>{{ statusLabel(detailRecord.status) }}</strong></article>
            <article><span>收费金额</span><strong>{{ formatAmount(detailPayment?.amount) }}</strong></article>
            <article><span>收费状态</span><strong>{{ paymentStatusLabel(detailPayment) }}</strong></article>
          </div>

          <div class="detail-summary">
            <p><strong>标题：</strong>{{ detailRecord.title }}</p>
            <p><strong>主诉摘要：</strong>{{ detailRecord.chiefComplaint || '未自动提取' }}</p>
            <p><strong>健康摘要：</strong>{{ detailRecord.healthSummary || '未关联健康档案摘要' }}</p>
          </div>

          <div
            v-if="detailPayment"
            ref="paymentPanelRef"
            :class="['result-panel', { 'focus-action-panel': focusedDetailSection === 'payment' }]"
          >
            <div class="doctor-recommend-head">
              <div>
                <strong>问诊收费</strong>
                <span>{{ detailPayment.status === 'paid' ? '当前已完成模拟支付，可继续按原流程查看 AI 导诊与问诊详情。' : '当前为演示版模拟收费，不接真实支付，点击按钮即可完成付款。' }}</span>
              </div>
              <el-tag :type="paymentStatusTagType(detailPayment)" effect="light">
                {{ paymentStatusLabel(detailPayment) }}
              </el-tag>
            </div>
            <div class="session-meta">
              <span>应付金额 {{ formatAmount(detailPayment.amount) }}</span>
              <span v-if="detailPayment.paymentNo">支付单号 {{ detailPayment.paymentNo }}</span>
              <span v-if="detailPayment.paymentChannel">支付方式 {{ paymentChannelLabel(detailPayment.paymentChannel) }}</span>
              <span v-if="detailPayment.paidTime">支付时间 {{ formatDate(detailPayment.paidTime) }}</span>
            </div>
            <p class="result-copy">{{ paymentSummaryText(detailPayment) }}</p>
            <div class="journey-actions">
              <el-button
                v-if="detailPayment.status === 'pending'"
                type="primary"
                :loading="paymentSubmitting"
                @click="submitMockPayment()"
              >
                模拟支付
              </el-button>
              <el-button plain @click="openTriageWorkspace(detailRecord.id)">打开 AI 导诊</el-button>
            </div>
          </div>

          <el-alert
            v-if="detailFollowUpReminder"
            :title="detailFollowUpReminder"
            :type="followUpReminderType(detailRecord)"
            :closable="false"
            class="conversation-alert"
          />

          <div v-if="patientJourneyCards.length" class="result-panel journey-panel">
            <div class="doctor-recommend-head">
              <div>
                <strong>接下来怎么做</strong>
                <span>{{ patientJourneyLeadText }}</span>
              </div>
              <div v-if="patientJourneyTags.length" class="chip-row">
                <span v-for="item in patientJourneyTags" :key="`journey-tag-${item}`">{{ item }}</span>
              </div>
            </div>
            <p class="result-copy journey-lead">{{ patientJourneySummaryText }}</p>
            <div class="journey-grid">
              <article v-for="item in patientJourneyCards" :key="item.key" class="journey-card">
                <div class="journey-card-head">
                  <strong>{{ item.title }}</strong>
                  <span :class="['journey-status', `is-${item.tone}`]">{{ item.status }}</span>
                </div>
                <p>{{ item.description }}</p>
                <div class="journey-actions">
                  <el-button text type="primary" @click="focusDetailActionSection(item.action, detailRecord.id)">{{ item.actionLabel }}</el-button>
                </div>
              </article>
            </div>
          </div>

          <div
            v-if="detailArchiveSummary"
            ref="archiveSummaryPanelRef"
            :class="['result-panel', 'archive-panel', { 'focus-action-panel': focusedDetailSection === 'archive' }]"
          >
            <div class="doctor-recommend-head">
              <div>
                <strong>问诊归档摘要</strong>
                <span>把本次问诊的重要结论、随访进展和后续建议整理到一起，便于回看与留存。</span>
              </div>
              <div class="archive-toolbar">
                <span v-if="detailArchiveSummary.lastUpdateTime" class="archive-updated">最近更新 {{ formatDate(detailArchiveSummary.lastUpdateTime) }}</span>
                <el-button text @click="downloadArchiveSummary">下载摘要</el-button>
                <el-button text type="primary" @click="copyArchiveSummary">复制摘要</el-button>
              </div>
            </div>
            <div class="archive-metrics">
              <span>{{ detailArchiveSummary.stageText }}</span>
              <span v-if="detailArchiveSummary.doctorName">跟进医生 {{ detailArchiveSummary.doctorName }}</span>
              <span>沟通 {{ detailArchiveSummary.messageCount || 0 }} 条</span>
              <span>随访 {{ detailArchiveSummary.followUpCount || 0 }} 次</span>
              <span v-if="detailArchiveSummary.serviceScore !== null && detailArchiveSummary.serviceScore !== undefined">服务评分 {{ detailArchiveSummary.serviceScore }}/5</span>
            </div>
            <p class="result-copy archive-lead">{{ detailArchiveSummary.archiveConclusion || detailArchiveSummary.patientActionHint }}</p>
            <el-alert
              v-if="detailArchiveSummary.patientActionHint"
              :title="detailArchiveSummary.patientActionHint"
              type="success"
              :closable="false"
              class="conversation-alert"
            />
            <div class="archive-grid">
              <article class="archive-card">
                <strong>问诊概览</strong>
                <p>{{ detailArchiveSummary.overview }}</p>
              </article>
              <article class="archive-card">
                <strong>导诊结论</strong>
                <p>{{ detailArchiveSummary.triageSummary }}</p>
              </article>
              <article class="archive-card">
                <strong>医生处理</strong>
                <p>{{ detailArchiveSummary.doctorSummary }}</p>
              </article>
              <article class="archive-card">
                <strong>随访进展</strong>
                <p>{{ detailArchiveSummary.followUpSummary }}</p>
              </article>
              <article class="archive-card">
                <strong>服务评价</strong>
                <p>{{ detailArchiveSummary.serviceSummary }}</p>
              </article>
              <article class="archive-card">
                <strong>最近沟通</strong>
                <p>{{ detailArchiveSummary.latestMessageSummary }}</p>
              </article>
            </div>
            <div v-if="detailArchiveSummary.riskFlags?.length" class="chip-row danger">
              <span v-for="item in detailArchiveSummary.riskFlags" :key="`risk-${item}`">{{ item }}</span>
            </div>
            <div v-if="detailArchiveSummary.conclusionTags?.length" class="chip-row">
              <span v-for="item in detailArchiveSummary.conclusionTags" :key="`tag-${item}`">{{ item }}</span>
            </div>
            <div v-if="detailArchiveSummary.nextActions?.length" class="archive-next-list">
              <article
                v-for="(item, index) in detailArchiveSummary.nextActions"
                :key="`${index}-${item}`"
                class="archive-next-item"
              >
                <span>{{ index + 1 }}</span>
                <p>{{ item }}</p>
              </article>
            </div>
          </div>

          <div class="result-panel timeline-panel">
            <div class="doctor-recommend-head">
              <strong>问诊进度时间线</strong>
              <span>从提交、分诊、接诊到随访，帮助你快速了解当前处理阶段。</span>
            </div>
            <div class="timeline-list">
              <article v-for="item in detailTimelineItems" :key="item.key" class="timeline-item">
                <div class="timeline-dot" :class="`is-${item.tone}`"></div>
                <div class="timeline-content">
                  <div class="timeline-head">
                    <strong>{{ item.title }}</strong>
                    <span>{{ item.timeText }}</span>
                  </div>
                  <p>{{ item.description }}</p>
                </div>
              </article>
            </div>
          </div>

          <div class="triage-panel">
            <div class="triage-grid">
              <article>
                <span>初步分诊</span>
                <strong class="triage-strong">
                  <span class="triage-badge" :style="triageBadgeStyle(detailRecord.triageLevelColor)">
                    {{ detailRecord.triageLevelName || '待系统评估' }}
                  </span>
                </strong>
              </article>
              <article>
                <span>建议动作</span>
                <strong>{{ triageActionLabel(detailRecord.triageActionType) }}</strong>
              </article>
              <article>
                <span>推荐科室</span>
                <strong>{{ detailRecord.departmentName || '待分配' }}</strong>
              </article>
            </div>
            <p><strong>系统建议：</strong>{{ detailRecord.triageSuggestion || '已保存当前问诊资料，请留意后续处理结果。' }}</p>
            <p v-if="detailRecord.triageRuleSummary"><strong>风险提示：</strong>{{ detailRecord.triageRuleSummary }}</p>
          </div>

          <div class="result-panel">
            <div class="doctor-recommend-head">
              <div>
                <strong>智能分配进度</strong>
                <span>{{ smartDispatchHintText(detailRecord.smartDispatch) }}</span>
              </div>
              <el-tag :type="smartDispatchTagType(detailRecord.smartDispatch)" effect="light">
                {{ smartDispatchStatusLabel(detailRecord.smartDispatch) }}
              </el-tag>
            </div>
            <div class="session-meta">
              <span v-if="getSmartDispatch(detailRecord).suggestedDoctorName">
                首推医生 {{ getSmartDispatch(detailRecord).suggestedDoctorName }}{{ getSmartDispatch(detailRecord).suggestedDoctorTitle ? ` / ${getSmartDispatch(detailRecord).suggestedDoctorTitle}` : '' }}
              </span>
              <span v-if="claimedDoctorName(detailRecord)">当前接诊 {{ claimedDoctorName(detailRecord) }}</span>
              <span v-if="detailRecord.doctorAssignment?.claimTime">认领时间 {{ formatDate(detailRecord.doctorAssignment.claimTime) }}</span>
              <span v-if="getSmartDispatch(detailRecord).candidateCount">候选 {{ getSmartDispatch(detailRecord).candidateCount }} 位</span>
              <span v-if="getSmartDispatch(detailRecord).suggestedDoctorNextScheduleText">{{ getSmartDispatch(detailRecord).suggestedDoctorNextScheduleText }}</span>
            </div>
            <p class="result-copy">{{ smartDispatchHintText(detailRecord.smartDispatch) }}</p>
            <p v-if="smartDispatchReason(detailRecord)" class="result-copy"><strong>推荐依据：</strong>{{ smartDispatchReason(detailRecord) }}</p>
            <p v-if="getSmartDispatch(detailRecord).suggestedDoctorExpertise" class="result-copy">
              <strong>医生专长：</strong>{{ getSmartDispatch(detailRecord).suggestedDoctorExpertise }}
            </p>
          </div>

          <div v-if="detailRecord.triageSession" class="session-panel">
            <div class="doctor-recommend-head">
              <div>
                <strong>导诊留痕</strong>
                <span>保存本次分诊的摘要、系统结果与规则命中过程</span>
              </div>
              <el-button text type="primary" @click="openTriageWorkspace(detailRecord.id)">在独立工作区打开</el-button>
            </div>
            <div class="session-meta">
              <span>Session {{ detailRecord.triageSession.sessionNo }}</span>
              <span>{{ triageSessionStatusLabel(detailRecord.triageSession.status) }}</span>
              <span>{{ detailRecord.triageSession.messageCount || 0 }} messages</span>
            </div>
            <div class="session-message-list">
              <article
                v-for="message in detailTriageMessages"
                :key="message.id"
                class="session-message-card"
              >
                <div class="session-message-head">
                  <div>
                    <strong>{{ message.title }}</strong>
                    <span>{{ messageTypeLabel(message.messageType) }}</span>
                  </div>
                  <el-tag size="small" effect="light">{{ messageRoleLabel(message.roleType) }}</el-tag>
                </div>
                <p>{{ message.content }}</p>
                <div v-if="message.insight" class="session-message-insight">
                  <div class="session-message-insight-meta">
                    <span v-if="message.insight.recommendedVisitType">建议方式：{{ message.insight.recommendedVisitType }}</span>
                    <span v-if="message.insight.recommendedDepartmentName">建议科室：{{ message.insight.recommendedDepartmentName }}</span>
                    <span v-if="message.insight.confidenceText">置信度：{{ message.insight.confidenceText }}</span>
                  </div>
                  <p v-if="message.insight.doctorRecommendationReason" class="session-message-insight-copy">
                    <strong>推荐依据：</strong>{{ message.insight.doctorRecommendationReason }}
                  </p>
                  <div v-if="message.insight.recommendedDoctors.length" class="session-message-insight-tags">
                    <span v-for="item in message.insight.recommendedDoctors" :key="item">{{ item }}</span>
                  </div>
                  <div v-if="message.insight.riskFlags.length" class="session-message-insight-tags danger">
                    <span v-for="item in message.insight.riskFlags" :key="item">{{ item }}</span>
                  </div>
                </div>
              </article>
            </div>
            <div class="triage-ai-composer">
              <div class="session-message-head">
                <div>
                  <strong>继续 AI 导诊</strong>
                  <span>可继续补充症状变化、持续时间、体温或检查结果，AI 会继续追问并更新建议。</span>
                </div>
              </div>
              <el-input
                v-model="triageAiDraft.content"
                type="textarea"
                :rows="3"
                maxlength="1000"
                show-word-limit
                :disabled="!canSendTriageAiMessage"
                placeholder="例如：今晚开始发热到 38.5 度，咳嗽比白天更频繁，已经持续两小时。"
              />
              <div class="triage-ai-toolbar">
                <span class="triage-ai-tip">{{ triageAiSendHint }}</span>
                <el-button
                  type="primary"
                  plain
                  :loading="triageAiSending"
                  :disabled="!canSendTriageAiMessage"
                  @click="sendTriageAiMessage"
                >
                  发送给 AI
                </el-button>
              </div>
            </div>
          </div>

          <div class="result-panel">
            <div class="doctor-recommend-head">
              <div>
                <strong>医患沟通</strong>
                <span>可在这里补充病情变化、检查图片和恢复情况，医生接手后会继续查看。</span>
              </div>
              <div class="conversation-summary">
                <span>{{ detailMessageSummary.totalCount }} 条消息</span>
                <span v-if="detailMessageSummary.unreadCount">医生新回复 {{ detailMessageSummary.unreadCount }} 条</span>
                <span v-if="detailMessageSummary.latestTime">最近更新 {{ formatDate(detailMessageSummary.latestTime) }}</span>
                <span
                  :class="['conversation-sync-text', { 'is-failed': conversationSyncStatus === 'failed' }]"
                >
                  {{ conversationSyncText }}
                </span>
              </div>
            </div>
            <el-alert
              v-if="messageSendHint"
              :title="messageSendHint"
              type="info"
              :closable="false"
              class="conversation-alert"
            />
            <div v-loading="messageLoading" class="conversation-board">
              <div
                v-if="consultationMessages.length"
                ref="conversationBoardRef"
                class="conversation-list"
                @scroll="handleConversationBoardScroll"
              >
                <article
                  v-for="item in consultationMessages"
                  :key="item.id"
                  :class="['conversation-card', { mine: isUserMessage(item) }]"
                >
                  <div class="conversation-meta">
                    <strong>{{ messageSenderLabel(item) }}</strong>
                    <span>{{ formatDate(item.createTime) }}</span>
                  </div>
                  <p v-if="item.content" class="conversation-content">{{ item.content }}</p>
                  <div v-if="messageAttachments(item).length" class="conversation-image-list">
                    <img
                      v-for="path in messageAttachments(item)"
                      :key="path"
                      :src="resolveImagePath(path)"
                      alt="消息附件"
                      class="conversation-image"
                    />
                  </div>
                </article>
              </div>
              <el-empty v-else description="当前还没有沟通消息" />
            </div>
            <div v-if="conversationPendingNewMessageCount > 0" class="conversation-jump-bar">
              <el-button type="primary" plain @click="jumpConversationToLatest">
                {{ conversationPendingNewMessageText }}
              </el-button>
            </div>
            <div class="conversation-composer">
              <el-input
                ref="conversationInputRef"
                :class="{ 'focus-action-input': focusedDetailSection === 'conversation' }"
                v-model="messageDraft.content"
                type="textarea"
                :rows="3"
                maxlength="2000"
                show-word-limit
                :disabled="!canSendMessage"
                placeholder="可补充症状变化、上传检查图片，或向医生说明当前恢复情况。"
              />
              <div v-if="messageDraft.attachments.length" class="conversation-attachments">
                <article v-for="(path, index) in messageDraft.attachments" :key="path" class="conversation-attachment-item">
                  <img :src="resolveImagePath(path)" alt="待发送附件" class="conversation-image" />
                  <div class="conversation-attachment-actions">
                    <span>图片 {{ index + 1 }}</span>
                    <el-button link type="danger" @click="removeMessageAttachment(index)">移除</el-button>
                  </div>
                </article>
              </div>
              <div class="conversation-toolbar">
                <el-upload
                  :action="uploadAction"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  accept="image/*"
                  :before-upload="beforeMessageUpload"
                  :disabled="!canSendMessage || messageDraft.attachments.length >= 6"
                  :on-success="handleMessageUploadSuccess"
                >
                  <el-button plain :disabled="!canSendMessage || messageDraft.attachments.length >= 6">上传沟通图片</el-button>
                </el-upload>
                <span class="conversation-tip">支持文字和图片，单次最多 6 张。</span>
                <el-button type="primary" :loading="messageSending" :disabled="!canSendMessage" @click="sendConsultationMessage">发送给医生</el-button>
              </div>
            </div>
          </div>

          <div v-if="detailRecord.triageResult" class="result-panel">
            <div class="doctor-recommend-head">
              <strong>导诊结果归档</strong>
              <span>保存本次导诊的最终推荐结果，便于后续复盘和 AI 训练使用</span>
            </div>
            <div class="session-meta">
              <span>{{ detailRecord.triageResult.triageLevelName || '待评估' }}</span>
              <span>{{ detailRecord.triageResult.departmentName || '未匹配科室' }}</span>
              <span>置信度 {{ formatConfidence(detailRecord.triageResult.confidenceScore) }}</span>
              <span v-if="detailRecord.triageResult.doctorName">推荐医生 {{ detailRecord.triageResult.doctorName }}</span>
            </div>
            <p class="result-copy">{{ detailRecord.triageResult.reasonText || detailRecord.triageSuggestion || '当前暂无更多结果说明' }}</p>
            <div v-if="parseJsonArray(detailRecord.triageResult.riskFlagsJson).length" class="chip-row">
              <span v-for="item in parseJsonArray(detailRecord.triageResult.riskFlagsJson)" :key="item">{{ item }}</span>
            </div>
            <div v-if="parseDoctorCandidates(detailRecord.triageResult.doctorCandidatesJson).length" class="candidate-list">
              <article
                v-for="item in parseDoctorCandidates(detailRecord.triageResult.doctorCandidatesJson)"
                :key="item.id || item.name"
                class="candidate-card"
              >
                <div class="doctor-copy-head">
                  <strong>{{ item.name }}</strong>
                  <span v-if="doctorRecommendationScoreText(item)" class="recommend-score">{{ doctorRecommendationScoreText(item) }}</span>
                </div>
                <span>{{ item.title || '医生' }}</span>
                <div v-if="item.matchedServiceTags?.length" class="chip-row is-accent">
                  <span v-for="tag in item.matchedServiceTags" :key="`${item.id}-matched-${tag}`">匹配 {{ tag }}</span>
                </div>
                <div v-if="item.recommendationReasons?.length" class="chip-row is-subtle">
                  <span v-for="reason in item.recommendationReasons.slice(0, 3)" :key="`${item.id}-reason-${reason}`">{{ reason }}</span>
                </div>
                <p v-if="item.recommendationSummary" class="result-copy doctor-recommend-copy"><strong>排序说明：</strong>{{ item.recommendationSummary }}</p>
                <p v-if="item.nextScheduleText" class="doctor-schedule">{{ item.nextScheduleText }}</p>
              </article>
            </div>
          </div>

          <div
            v-if="detailRecord.doctorHandle"
            ref="doctorHandlePanelRef"
            :class="['result-panel', { 'focus-action-panel': focusedDetailSection === 'doctor_handle' }]"
          >
            <div class="doctor-recommend-head">
              <strong>医生处理结果</strong>
              <span>查看医生是否已接手当前问诊，以及本次处理建议和随访安排。</span>
            </div>
            <div class="session-meta">
              <span>{{ detailRecord.doctorHandle.doctorName || '未指派医生' }}</span>
              <span>{{ doctorHandleStatusLabel(detailRecord.doctorHandle.status) }}</span>
              <span>接手时间 {{ formatDate(detailRecord.doctorHandle.receiveTime) }}</span>
              <span v-if="detailRecord.doctorHandle.completeTime">完成时间 {{ formatDate(detailRecord.doctorHandle.completeTime) }}</span>
            </div>
            <p class="result-copy">{{ detailRecord.doctorHandle.summary || '医生已接手，正在整理处理意见。' }}</p>
            <div class="summary-panel">
              <p><strong>处理建议：</strong>{{ detailRecord.doctorHandle.medicalAdvice || '当前暂无详细建议，请稍后查看。' }}</p>
              <p><strong>随访计划：</strong>{{ detailRecord.doctorHandle.followUpPlan || '当前暂无随访安排。' }}</p>
            </div>
          </div>

          <div v-if="detailRecord.prescriptions?.length" class="result-panel">
            <div class="doctor-recommend-head">
              <strong>医生处方</strong>
              <span>展示医生开具的药品方案，以及系统自动识别出的用药禁忌和联用冲突提醒。</span>
            </div>
            <div class="doctor-recommend-list">
              <article
                v-for="item in detailRecord.prescriptions"
                :key="`prescription-${item.id || item.medicineId || item.medicineName}`"
                class="doctor-recommend-card"
              >
                <div class="session-meta">
                  <span>{{ item.medicineName }}</span>
                  <span v-if="item.specification">{{ item.specification }}</span>
                  <span>{{ item.dosage || '-' }}</span>
                  <span>{{ item.frequency || '-' }}</span>
                  <span>{{ item.durationDays ? `${item.durationDays} 天` : '-' }}</span>
                </div>
                <p class="result-copy"><strong>服药说明：</strong>{{ item.medicationInstruction || '请按医嘱规范使用。' }}</p>
                <p v-if="item.warningSummary" class="result-copy"><strong>禁忌提醒：</strong>{{ item.warningSummary }}</p>
              </article>
            </div>
          </div>

          <div
            v-if="detailRecord.doctorConclusion"
            ref="doctorConclusionPanelRef"
            :class="['result-panel', { 'focus-action-panel': focusedDetailSection === 'doctor_conclusion' }]"
          >
            <div class="doctor-recommend-head">
              <strong>结构化结论</strong>
              <span>这是医生最终沉淀的标准化结论，可用于后续复诊、统计和系统优化。</span>
            </div>
            <div class="session-meta">
              <span>{{ conditionLevelLabel(detailRecord.doctorConclusion.conditionLevel) }}</span>
              <span>{{ dispositionLabel(detailRecord.doctorConclusion.disposition) }}</span>
              <span>{{ aiConsistencyLabel(detailRecord.doctorConclusion.isConsistentWithAi) }}</span>
              <span>{{ detailRecord.doctorConclusion.needFollowUp === 1 ? '需要随访' : '无需随访' }}</span>
              <span v-if="detailRecord.doctorConclusion.followUpWithinDays">
                建议 {{ detailRecord.doctorConclusion.followUpWithinDays }} 天内随访
              </span>
            </div>
            <div class="summary-panel">
              <p><strong>诊断方向：</strong>{{ detailRecord.doctorConclusion.diagnosisDirection || '暂无明确方向说明。' }}</p>
              <p><strong>患者指导：</strong>{{ detailRecord.doctorConclusion.patientInstruction || '暂无补充指导。' }}</p>
            </div>
            <div v-if="parseJsonArray(detailRecord.doctorConclusion.conclusionTagsJson).length" class="chip-row">
              <span v-for="item in parseJsonArray(detailRecord.doctorConclusion.conclusionTagsJson)" :key="item">{{ item }}</span>
            </div>
          </div>

          <div v-if="detailRecord.aiComparison" class="result-panel compare-panel">
            <div class="doctor-recommend-head">
              <strong>AI 建议采纳情况</strong>
              <span>{{ detailRecord.aiComparison.summary }}</span>
            </div>
            <div class="session-meta">
              <span :class="['compare-badge', comparisonStatusClass(detailRecord.aiComparison.overallStatus)]">
                {{ comparisonStatusLabel(detailRecord.aiComparison.overallStatus) }}
              </span>
              <span v-if="detailRecord.doctorConclusion">{{ aiConsistencyLabel(detailRecord.doctorConclusion.isConsistentWithAi) }}</span>
            </div>
            <div class="compare-grid">
              <article class="compare-card">
                <strong>AI 建议</strong>
                <div class="compare-kv">
                  <label>病情等级</label>
                  <span>{{ detailRecord.aiComparison.aiConditionLevel ? conditionLevelLabel(detailRecord.aiComparison.aiConditionLevel) : '未提供' }}</span>
                </div>
                <div class="compare-kv">
                  <label>处理去向</label>
                  <span>{{ detailRecord.aiComparison.aiDisposition ? dispositionLabel(detailRecord.aiComparison.aiDisposition) : '未提供' }}</span>
                </div>
                <div class="compare-kv">
                  <label>建议科室</label>
                  <span>{{ detailRecord.aiComparison.aiDepartmentName || '未提供' }}</span>
                </div>
                <div class="compare-kv">
                  <label>随访建议</label>
                  <span>{{ detailRecord.aiComparison.aiFollowUpText || '未提供' }}</span>
                </div>
                <div class="compare-kv">
                  <label>置信度</label>
                  <span>{{ detailRecord.aiComparison.aiConfidenceText || '未提供' }}</span>
                </div>
                <p v-if="detailRecord.aiComparison.aiReasonText" class="result-copy compare-copy"><strong>推荐依据：</strong>{{ detailRecord.aiComparison.aiReasonText }}</p>
              </article>
              <article class="compare-card">
                <strong>医生最终结论</strong>
                <div class="compare-kv">
                  <label>病情等级</label>
                  <span>{{ detailRecord.doctorConclusion?.conditionLevel ? conditionLevelLabel(detailRecord.doctorConclusion.conditionLevel) : '待医生判断' }}</span>
                </div>
                <div class="compare-kv">
                  <label>处理去向</label>
                  <span>{{ detailRecord.doctorConclusion?.disposition ? dispositionLabel(detailRecord.doctorConclusion.disposition) : '待医生判断' }}</span>
                </div>
                <div class="compare-kv">
                  <label>诊断方向</label>
                  <span>{{ detailRecord.doctorConclusion?.diagnosisDirection || '待医生填写' }}</span>
                </div>
                <div class="compare-kv">
                  <label>随访建议</label>
                  <span>{{ doctorFollowUpText(detailRecord.doctorConclusion) || '待医生判断' }}</span>
                </div>
                <div class="compare-kv">
                  <label>AI 一致性</label>
                  <span>{{ detailRecord.doctorConclusion ? aiConsistencyLabel(detailRecord.doctorConclusion.isConsistentWithAi) : '待医生判断' }}</span>
                </div>
              </article>
            </div>
            <div class="compare-list">
              <article class="compare-item">
                <div>
                  <strong>病情等级</strong>
                  <p>AI：{{ detailRecord.aiComparison.aiConditionLevel ? conditionLevelLabel(detailRecord.aiComparison.aiConditionLevel) : '未提供' }}</p>
                  <p>医生：{{ detailRecord.doctorConclusion?.conditionLevel ? conditionLevelLabel(detailRecord.doctorConclusion.conditionLevel) : '待医生判断' }}</p>
                </div>
                <span :class="['compare-badge', comparisonStatusClass(detailRecord.aiComparison.conditionLevelStatus)]">{{ comparisonStatusLabel(detailRecord.aiComparison.conditionLevelStatus) }}</span>
              </article>
              <article class="compare-item">
                <div>
                  <strong>处理去向</strong>
                  <p>AI：{{ detailRecord.aiComparison.aiDisposition ? dispositionLabel(detailRecord.aiComparison.aiDisposition) : '未提供' }}</p>
                  <p>医生：{{ detailRecord.doctorConclusion?.disposition ? dispositionLabel(detailRecord.doctorConclusion.disposition) : '待医生判断' }}</p>
                </div>
                <span :class="['compare-badge', comparisonStatusClass(detailRecord.aiComparison.dispositionStatus)]">{{ comparisonStatusLabel(detailRecord.aiComparison.dispositionStatus) }}</span>
              </article>
              <article class="compare-item">
                <div>
                  <strong>随访安排</strong>
                  <p>AI：{{ detailRecord.aiComparison.aiFollowUpText || '未提供' }}</p>
                  <p>医生：{{ doctorFollowUpText(detailRecord.doctorConclusion) || '待医生判断' }}</p>
                </div>
                <span :class="['compare-badge', comparisonStatusClass(detailRecord.aiComparison.followUpStatus)]">{{ comparisonStatusLabel(detailRecord.aiComparison.followUpStatus) }}</span>
              </article>
            </div>
            <div v-if="detailRecord.aiComparison.aiRecommendedDoctors?.length" class="chip-row">
              <span v-for="item in detailRecord.aiComparison.aiRecommendedDoctors" :key="item">{{ item }}</span>
            </div>
            <div v-if="detailRecord.aiComparison.aiRiskFlags?.length" class="chip-row danger">
              <span v-for="item in detailRecord.aiComparison.aiRiskFlags" :key="item">{{ item }}</span>
            </div>
          </div>

          <div
            ref="followUpPanelRef"
            :class="['result-panel', { 'focus-action-panel': focusedDetailSection === 'followup' }]"
          >
            <div class="doctor-recommend-head">
              <strong>随访记录</strong>
              <span>如医生在问诊完成后继续跟进，这里会展示后续随访摘要和建议。</span>
            </div>
            <div v-if="detailRecord.doctorFollowUps?.length" class="detail-answers">
              <article v-for="item in detailRecord.doctorFollowUps" :key="item.id" class="detail-answer-card">
                <div class="chip-row">
                  <span>{{ followUpTypeLabel(item.followUpType) }}</span>
                  <span>{{ patientStatusLabel(item.patientStatus) }}</span>
                  <span>{{ item.doctorName || '-' }}</span>
                  <span>{{ formatDate(item.createTime) }}</span>
                  <span v-if="item.needRevisit === 1">需再次随访</span>
                  <span v-if="item.nextFollowUpDate">下次 {{ formatDate(item.nextFollowUpDate) }}</span>
                </div>
                <div class="detail-answer-value">
                  <p><strong>随访摘要：</strong>{{ item.summary }}</p>
                  <p><strong>随访建议：</strong>{{ item.advice || '暂无补充建议。' }}</p>
                  <p><strong>下一步安排：</strong>{{ item.nextStep || '暂无下一步安排。' }}</p>
                </div>
              </article>
            </div>
            <el-empty v-else description="当前暂无随访记录" />
          </div>

          <div
            v-if="showGuidanceAcknowledgementPanel"
            ref="guidanceAckPanelRef"
            :class="['result-panel', { 'focus-action-panel': focusedDetailSection === 'guidance_ack' }]"
          >
            <div class="doctor-recommend-head">
              <strong>查看确认</strong>
              <span>如果你已经阅读本轮医生结论或随访建议，可以在这里确认，医生侧会同步看到。</span>
            </div>
            <div class="feedback-summary">
              <span>{{ doctorGuidanceAckStatusText }}</span>
              <span v-if="latestDoctorGuidanceTime">最近建议时间：{{ formatDate(latestDoctorGuidanceTime) }}</span>
              <span v-if="latestPatientGuidanceAck?.createTime">确认时间：{{ formatDate(latestPatientGuidanceAck.createTime) }}</span>
            </div>
            <el-alert
              :title="doctorGuidanceAckHint"
              :type="hasPatientAcknowledgedGuidance ? 'success' : 'info'"
              :closable="false"
              class="conversation-alert"
            />
            <div class="feedback-actions">
              <el-button
                v-if="canSubmitDoctorGuidanceAck"
                type="primary"
                :loading="doctorGuidanceAckSubmitting"
                @click="submitDoctorGuidanceAck"
              >
                确认已查看
              </el-button>
              <el-button
                v-else-if="latestDoctorGuidanceFocusAction"
                plain
                @click="focusDetailActionSection(latestDoctorGuidanceFocusAction, detailRecord?.id || null)"
              >
                查看对应内容
              </el-button>
            </div>
          </div>

          <div
            v-if="canSubmitCheckResultUpdate"
            ref="checkResultUpdatePanelRef"
            :class="['result-panel', { 'focus-action-panel': focusedDetailSection === 'check_result' }]"
          >
            <div class="doctor-recommend-head">
              <strong>检查结果补充</strong>
              <span>把化验、影像、病理或其他检查结果结构化补充给医生，便于继续判断和处理。</span>
            </div>
            <el-alert
              :title="checkResultUpdateHint"
              type="info"
              :closable="false"
              class="conversation-alert"
            />
            <article v-if="latestPatientCheckResultUpdate" class="detail-answer-card">
              <div class="chip-row">
                <span>Latest result update {{ formatDate(latestPatientCheckResultUpdate.createTime) }}</span>
                <span v-if="messageAttachments(latestPatientCheckResultUpdate).length">Images {{ messageAttachments(latestPatientCheckResultUpdate).length }}</span>
              </div>
              <div class="detail-answer-value">
                <p>{{ latestPatientCheckResultUpdate.content || 'The latest check-result update only contains image attachments.' }}</p>
              </div>
            </article>
            <div class="feedback-form followup-update-form">
              <el-select
                v-model="checkResultUpdateForm.resultType"
                style="width: 220px"
                placeholder="选择检查类型"
              >
                <el-option
                  v-for="item in checkResultTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-input
                v-model="checkResultUpdateForm.resultSummary"
                type="textarea"
                :rows="3"
                maxlength="600"
                show-word-limit
                placeholder="请概括关键结果，例如：血常规白细胞 11.2，CRP 18，胸片提示右下肺轻度炎症。"
              />
              <el-input
                v-model="checkResultUpdateForm.doctorQuestion"
                type="textarea"
                :rows="2"
                maxlength="300"
                show-word-limit
                placeholder="可以补充想请医生判断的问题，例如：这些结果是否提示需要调整用药或线下复查？"
              />
              <span class="conversation-tip">如需上传报告图片，可先带入消息框，再在沟通区继续上传图片。</span>
              <div class="feedback-actions">
                <el-button plain @click="applyCheckResultUpdateToMessageDraft">带入消息框</el-button>
                <el-button type="primary" :loading="checkResultUpdateSubmitting" @click="submitCheckResultUpdate">
                  发送检查结果
                </el-button>
              </div>
            </div>
          </div>

          <div
            v-if="canSubmitFollowUpUpdate"
            ref="followUpUpdatePanelRef"
            :class="['result-panel', { 'focus-action-panel': focusedDetailSection === 'followup_update' }]"
          >
            <div class="doctor-recommend-head">
              <strong>恢复更新</strong>
              <span>将最近的症状变化、恢复进展和仍需帮助的问题同步给医生，方便继续随访。</span>
            </div>
            <el-alert
              :title="followUpUpdateHint"
              :type="followUpReminderType(detailRecord)"
              :closable="false"
              class="conversation-alert"
            />
            <article v-if="latestPatientFollowUpUpdate" class="detail-answer-card">
              <div class="chip-row">
                <span>Latest update {{ formatDate(latestPatientFollowUpUpdate.createTime) }}</span>
                <span v-if="messageAttachments(latestPatientFollowUpUpdate).length">Images {{ messageAttachments(latestPatientFollowUpUpdate).length }}</span>
              </div>
              <div class="detail-answer-value">
                <p>{{ latestPatientFollowUpUpdate.content || 'The latest recovery update only contains image attachments.' }}</p>
              </div>
            </article>
            <div class="feedback-form followup-update-form">
              <el-select
                v-model="followUpUpdateForm.recoveryStatus"
                style="width: 220px"
                placeholder="Select recovery status"
              >
                <el-option
                  v-for="item in followUpRecoveryStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-input
                v-model="followUpUpdateForm.progressNote"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="Describe recent changes, for example: fever is gone but coughing is still obvious at night."
              />
              <el-input
                v-model="followUpUpdateForm.helpRequest"
                type="textarea"
                :rows="2"
                maxlength="300"
                show-word-limit
                placeholder="Describe what still needs help, for example: whether another review visit is needed."
              />
              <div class="feedback-actions">
                <el-button plain @click="applyFollowUpUpdateToMessageDraft">带入消息框</el-button>
                <el-button type="primary" :loading="followUpUpdateSubmitting" @click="submitFollowUpUpdate">
                  发送恢复更新
                </el-button>
              </div>
            </div>
          </div>

          <div
            v-if="canSubmitServiceFeedback"
            ref="serviceFeedbackPanelRef"
            :class="['feedback-panel', { 'focus-action-panel': focusedDetailSection === 'feedback' }]"
          >
            <div class="doctor-recommend-head">
              <strong>问诊服务评价</strong>
              <span>医生已完成本轮处理后，你可以补充对本次线上问诊服务的评价，帮助后续持续优化。</span>
            </div>
            <div v-if="detailRecord.serviceFeedback" class="feedback-summary">
              <span>当前评分：{{ detailRecord.serviceFeedback.serviceScore }}/5</span>
              <span>{{ serviceFeedbackResolvedLabel(detailRecord.serviceFeedback.isResolved) }}</span>
              <span>评价时间：{{ formatDate(detailRecord.serviceFeedback.updateTime || detailRecord.serviceFeedback.createTime) }}</span>
            </div>
            <el-alert
              v-else
              title="当前医生处理已完成，欢迎补充本次线上问诊体验和问题是否得到解决。"
              type="success"
              :closable="false"
              class="conversation-alert"
            />
            <div class="feedback-form">
              <el-select v-model="serviceFeedbackForm.serviceScore" style="width: 180px" placeholder="选择服务评分">
                <el-option v-for="item in [5, 4, 3, 2, 1]" :key="`service-score-${item}`" :label="`${item} 分`" :value="item" />
              </el-select>
              <el-select v-model="serviceFeedbackForm.isResolved" style="width: 220px" placeholder="问题是否已解决">
                <el-option label="本次问题已解决" :value="1" />
                <el-option label="仍需继续处理" :value="0" />
              </el-select>
              <el-input
                v-model="serviceFeedbackForm.feedbackText"
                type="textarea"
                :rows="3"
                maxlength="1000"
                show-word-limit
                placeholder="可补充医生回复是否及时、建议是否清晰、目前还希望继续得到哪些帮助。"
              />
              <div class="feedback-actions">
                <el-button type="primary" :loading="serviceFeedbackSubmitting" @click="submitServiceFeedback">
                  {{ detailRecord.serviceFeedback ? '更新评价' : '提交评价' }}
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="detailRecord.triageSession" class="feedback-panel">
            <div class="doctor-recommend-head">
              <strong>导诊反馈</strong>
              <span>请告诉系统这次导诊是否有帮助，后续会用于分诊纠偏和效果优化</span>
            </div>
            <div v-if="detailRecord.triageFeedback" class="feedback-summary">
              <span>当前状态：{{ feedbackAdoptLabel(detailRecord.triageFeedback.isAdopted) }}</span>
              <span>评分：{{ detailRecord.triageFeedback.userScore }}/5</span>
              <span v-if="detailRecord.triageFeedback.manualCorrectDepartmentName">修正科室：{{ detailRecord.triageFeedback.manualCorrectDepartmentName }}</span>
              <span v-if="detailRecord.triageFeedback.manualCorrectDoctorName">修正医生：{{ detailRecord.triageFeedback.manualCorrectDoctorName }}</span>
            </div>
            <div class="feedback-form">
              <el-select v-model="feedbackForm.userScore" style="width: 180px" placeholder="选择评分">
                <el-option v-for="item in [5, 4, 3, 2, 1]" :key="item" :label="`${item} 分`" :value="item" />
              </el-select>
              <el-select v-model="feedbackForm.isAdopted" style="width: 180px" placeholder="是否采纳">
                <el-option label="已采纳" :value="1" />
                <el-option label="未采纳" :value="0" />
              </el-select>
              <el-select
                v-if="feedbackForm.isAdopted === 0"
                v-model="feedbackForm.manualCorrectDepartmentId"
                clearable
                style="width: 220px"
                placeholder="修正科室"
              >
                <el-option
                  v-for="item in feedbackOptions.departments || []"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
              <el-select
                v-if="feedbackForm.isAdopted === 0"
                v-model="feedbackForm.manualCorrectDoctorId"
                clearable
                filterable
                style="width: 240px"
                placeholder="修正医生"
              >
                <el-option
                  v-for="item in availableFeedbackDoctors"
                  :key="item.id"
                  :label="item.title ? `${item.name} / ${item.title}` : item.name"
                  :value="item.id"
                />
              </el-select>
              <el-input
                v-model="feedbackForm.feedbackText"
                type="textarea"
                :rows="3"
                placeholder="可填写为什么采纳或不采纳，也可以补充你更希望被分到哪个科室/医生"
              />
              <div class="feedback-actions">
                <el-button type="primary" :loading="feedbackSubmitting" @click="submitFeedback">
                  {{ detailRecord.triageFeedback ? '更新反馈' : '提交反馈' }}
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="(detailRecord.recommendedDoctors || []).length" class="doctor-recommend">
            <div class="doctor-recommend-head">
              <strong>推荐医生</strong>
              <span>结合当前科室与排班，优先展示可继续关注的医生</span>
            </div>
            <div class="doctor-list">
              <article v-for="doctor in detailRecord.recommendedDoctors" :key="doctor.id" class="doctor-card">
                <div class="doctor-top">
                  <img
                    v-if="doctor.photo"
                    class="doctor-avatar"
                    :src="resolveImagePath(doctor.photo)"
                    :alt="doctor.name"
                  />
                  <div v-else class="doctor-avatar doctor-avatar-fallback">{{ doctor.name?.slice(0, 1) || '医' }}</div>
                  <div class="doctor-copy">
                    <div class="doctor-copy-head">
                      <strong>{{ doctor.name }}</strong>
                      <span v-if="doctorRecommendationScoreText(doctor)" class="recommend-score">{{ doctorRecommendationScoreText(doctor) }}</span>
                    </div>
                    <span>{{ doctor.title || '医生' }}</span>
                  </div>
                </div>
                <p class="doctor-text">{{ doctor.expertise || doctor.introduction || '暂未配置更多医生介绍信息' }}</p>
                <div v-if="doctor.matchedServiceTags?.length" class="chip-row is-accent">
                  <span v-for="tag in doctor.matchedServiceTags" :key="`${doctor.id}-matched-${tag}`">匹配 {{ tag }}</span>
                </div>
                <div v-if="doctor.recommendationReasons?.length" class="chip-row is-subtle">
                  <span v-for="reason in doctor.recommendationReasons.slice(0, 3)" :key="`${doctor.id}-reason-${reason}`">{{ reason }}</span>
                </div>
                <p v-if="doctor.recommendationSummary" class="doctor-text doctor-recommend-copy"><strong>排序说明：</strong>{{ doctor.recommendationSummary }}</p>
                <div v-if="doctor.serviceTags?.length" class="chip-row">
                  <span v-for="tag in doctor.serviceTags" :key="tag">{{ tag }}</span>
                </div>
                <p class="doctor-schedule">{{ doctor.nextScheduleText || '暂未配置接诊时段' }}</p>
              </article>
            </div>
          </div>

          <div class="detail-answers">
            <article v-for="answer in detailRecord.answers || []" :key="answer.id" class="detail-answer-card">
              <strong>{{ answer.fieldLabel }}</strong>
              <div class="detail-answer-value">
                <template v-if="answer.fieldType === 'upload' && answer.fieldValue">
                  <img :src="resolveImagePath(answer.fieldValue)" :alt="answer.fieldLabel" />
                </template>
                <template v-else-if="answer.fieldType === 'multi_select'">
                  <div class="chip-row">
                    <span v-for="item in parseMultiValue(answer.fieldValue)" :key="item">{{ item }}</span>
                  </div>
                </template>
                <template v-else>
                  {{ displayAnswer(answer) }}
                </template>
              </div>
            </article>
          </div>
        </template>
        <el-empty v-else-if="!detailLoading" description="问诊详情暂未加载成功，请刷新后重试" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authHeader, backendBaseUrl, download, get, post, resolveImagePath } from '@/net'
import { comparisonStatusClass, comparisonStatusLabel } from '@/triage/comparison'
import { normalizeSmartDispatch, smartDispatchHintText, smartDispatchStatusLabel, smartDispatchTagType } from '@/triage/dispatch'
import { resolveTriageMessageInsight } from '@/triage/insight'
import { isPendingServiceFeedbackRecord, serviceFeedbackBaseTime, serviceFeedbackReminderText } from '@/triage/reminder'

const route = useRoute()
const router = useRouter()
const categories = ref([])
const patients = ref([])
const histories = ref([])
const records = ref([])
const template = ref(null)
const activeCategoryId = ref(null)
const selectedPatientId = ref(null)
const historySectionRef = ref(null)
const syncingRecordRoute = ref(false)
const conversationInputRef = ref(null)
const conversationBoardRef = ref(null)
const conversationPendingNewMessageCount = ref(0)
const conversationPendingNewMessageLabel = ref('')
const conversationLastSyncedAt = ref(null)
const conversationSyncStatus = ref('idle')
const archiveSummaryPanelRef = ref(null)
const paymentPanelRef = ref(null)
const doctorHandlePanelRef = ref(null)
const doctorConclusionPanelRef = ref(null)
const followUpPanelRef = ref(null)
const guidanceAckPanelRef = ref(null)
const checkResultUpdatePanelRef = ref(null)
const followUpUpdatePanelRef = ref(null)
const serviceFeedbackPanelRef = ref(null)
const focusedDetailSection = ref('')
const recordKeyword = ref('')
const recordStatusFilter = ref('')
const recordProgressFilter = ref('all')
const recordFollowUpFilter = ref('all')
const templateLoading = ref(false)
const historyLoading = ref(false)
const submitting = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRecord = ref(null)
const triageAiSending = ref(false)
const consultationMessages = ref([])
const messageLoading = ref(false)
const messageSending = ref(false)
const followUpUpdateSubmitting = ref(false)
const checkResultUpdateSubmitting = ref(false)
const doctorGuidanceAckSubmitting = ref(false)
const feedbackOptions = ref({ departments: [], doctors: [] })
const feedbackSubmitting = ref(false)
const serviceFeedbackSubmitting = ref(false)
const paymentSubmitting = ref(false)
const formData = reactive({})
const triageAiDraft = reactive({ content: '' })
const messageDraft = reactive({ content: '', attachments: [], sceneType: '' })
let detailSectionFocusTimer = null
let conversationPollTimer = null
let conversationPollBusy = false
const CONVERSATION_POLL_INTERVAL = 12000
const followUpRecoveryStatusOptions = [
  { label: '鏄庢樉濂借浆', value: 'improved' },
  { label: '鍩烘湰绋冲畾', value: 'stable' },
  { label: '鍑虹幇鍔犻噸', value: 'worsened' },
  { label: '鍏朵粬鎯呭喌', value: 'other' }
]
const checkResultTypeOptions = [
  { label: '化验检查', value: 'lab' },
  { label: '影像检查', value: 'imaging' },
  { label: '病理结果', value: 'pathology' },
  { label: '其他结果', value: 'other' }
]
const serviceFeedbackForm = reactive({
  serviceScore: 5,
  isResolved: 1,
  feedbackText: ''
})
const followUpUpdateForm = reactive({
  recoveryStatus: 'stable',
  progressNote: '',
  helpRequest: ''
})
const checkResultUpdateForm = reactive({
  resultType: 'lab',
  resultSummary: '',
  doctorQuestion: ''
})
const feedbackForm = reactive({
  userScore: 5,
  isAdopted: 1,
  feedbackText: '',
  manualCorrectDepartmentId: null,
  manualCorrectDoctorId: null
})

const uploadAction = computed(() => `${backendBaseUrl()}/api/image/cache`)
const uploadHeaders = computed(() => authHeader())

const currentCategory = computed(() => categories.value.find(item => item.id === activeCategoryId.value) || null)
const selectedPatient = computed(() => patients.value.find(item => item.id === selectedPatientId.value) || null)
const historyMap = computed(() => {
  const map = new Map()
  histories.value.forEach(item => map.set(item.patientId, item))
  return map
})
const currentHistory = computed(() => selectedPatientId.value ? historyMap.value.get(selectedPatientId.value) : null)
const currentHealthSummary = computed(() => buildHealthSummary(currentHistory.value))
const visibleFields = computed(() => {
  const fields = template.value?.fields || []
  return fields.filter(field => isFieldVisible(field))
})
const detailTriageMessages = computed(() => ensureArray(detailRecord.value?.triageSession?.messages).map(message => ({
  ...message,
  insight: resolveTriageMessageInsight(message)
})))
const latestTriageInsight = computed(() => [...detailTriageMessages.value]
  .reverse()
  .find(message => message.insight)?.insight || null)
const canSendTriageAiMessage = computed(() => !!detailRecord.value?.triageSession)
const triageAiSendHint = computed(() => {
  if (!detailRecord.value?.triageSession) return '当前问诊还没有导诊会话。'
  if (detailRecord.value.triageActionType === 'emergency') return '当前已有高风险提示，如症状继续加重请优先线下就医。'
  if (detailRecord.value.triageActionType === 'offline') return '可以继续补充变化情况，但若不适明显加重仍建议优先线下就医。'
  return 'AI 会基于已提交问诊资料和导诊历史继续分析。'
})
const canSendMessage = computed(() => !!detailRecord.value)
const messageSendHint = computed(() => {
  if (!detailRecord.value) return ''
  const doctorName = currentDoctorName(detailRecord.value)
  if (doctorName) return `当前由医生 ${doctorName} 跟进，可继续补充资料或反馈恢复情况。`
  return '如需补充症状、检查图片或恢复情况，可在这里留言，医生接诊后会继续查看。'
})
const availableFeedbackDoctors = computed(() => {
  const doctors = feedbackOptions.value?.doctors || []
  return feedbackForm.manualCorrectDepartmentId
    ? doctors.filter(item => item.departmentId === feedbackForm.manualCorrectDepartmentId)
    : doctors
})
const todayCount = computed(() => {
  const today = new Date().toDateString()
  return records.value.filter(item => new Date(item.createTime).toDateString() === today).length
})
const unreadDoctorReplyCount = computed(() => records.value.filter(recordHasUnreadDoctorReply).length)
const waitingDoctorHandleCount = computed(() => records.value.filter(item => recordProgressStage(item) === 'waiting_doctor').length)
const pendingPaymentCount = computed(() => records.value.filter(isPendingPaymentRecord).length)
const pendingFollowUpCount = computed(() => records.value.filter(item => ['pending', 'due_today', 'overdue'].includes(followUpState(item))).length)
const pendingServiceFeedbackCount = computed(() => records.value.filter(isPendingServiceFeedbackRecord).length)
const dueTodayFollowUpCount = computed(() => records.value.filter(item => followUpState(item) === 'due_today').length)
const overdueFollowUpCount = computed(() => records.value.filter(item => followUpState(item) === 'overdue').length)
const unreadReplyRecords = computed(() => records.value
  .filter(recordHasUnreadDoctorReply)
  .slice()
  .sort((left, right) => compareDateDesc(getMessageSummary(left).latestTime || left?.updateTime || left?.createTime, getMessageSummary(right).latestTime || right?.updateTime || right?.createTime))
  .slice(0, 4))
const waitingDoctorRecords = computed(() => records.value
  .filter(item => recordProgressStage(item) === 'waiting_doctor')
  .slice()
  .sort((left, right) => compareDateDesc(left?.createTime, right?.createTime))
  .slice(0, 4))
const followUpReminderRecords = computed(() => records.value
  .filter(item => ['pending', 'due_today', 'overdue'].includes(followUpState(item)))
  .slice()
  .sort(compareRecordOrder)
  .slice(0, 4))
const pendingServiceFeedbackRecords = computed(() => records.value
  .filter(isPendingServiceFeedbackRecord)
  .slice()
  .sort((left, right) => compareDateDesc(serviceFeedbackBaseTime(left), serviceFeedbackBaseTime(right)))
  .slice(0, 4))
const filteredRecords = computed(() => records.value
  .filter(item => {
    const search = recordKeyword.value.trim().toLowerCase()
    const matchesKeyword = !search || [item.title, item.categoryName, item.patientName, item.chiefComplaint, recordMessagePreview(item), recordProgressLabel(item), followUpLine(item), serviceFeedbackReminderText(item)]
      .filter(Boolean)
      .some(text => `${text}`.toLowerCase().includes(search))
    const matchesStatus = !recordStatusFilter.value || item.status === recordStatusFilter.value
    const matchesProgress = recordProgressFilter.value === 'all'
      || (recordProgressFilter.value === 'pending_feedback'
        ? isPendingServiceFeedbackRecord(item)
        : recordProgressStage(item) === recordProgressFilter.value)
    const matchesFollowUp = recordFollowUpFilter.value === 'all'
      || (recordFollowUpFilter.value === 'pending' && ['pending', 'due_today', 'overdue'].includes(followUpState(item)))
      || followUpState(item) === recordFollowUpFilter.value
    return matchesKeyword && matchesStatus && matchesProgress && matchesFollowUp
  })
  .slice()
  .sort(compareRecordOrder))
const detailArchiveSummary = computed(() => detailRecord.value?.archiveSummary || null)
const detailPayment = computed(() => normalizePayment(detailRecord.value?.payment))
const detailMessageSummary = computed(() => getMessageSummary(detailRecord.value))
const conversationSyncText = computed(() => {
  if (conversationSyncStatus.value === 'failed') {
    return conversationLastSyncedAt.value
      ? `同步失败，最近成功同步 ${formatSyncTime(conversationLastSyncedAt.value)}`
      : '同步失败，稍后重试'
  }
  const parts = []
  if (detailVisible.value) parts.push('实时同步中')
  if (conversationLastSyncedAt.value) parts.push(`最近同步 ${formatSyncTime(conversationLastSyncedAt.value)}`)
  return parts.join(' · ') || '等待首次同步'
})
const conversationPendingNewMessageText = computed(() => {
  const count = conversationPendingNewMessageCount.value
  if (count <= 0) return ''
  const label = trimText(conversationPendingNewMessageLabel.value)
  if (count > 1) {
    return label
      ? `${label}，另有 ${count - 1} 条新消息，跳到最新`
      : `收到 ${count} 条新消息，跳到最新`
  }
  return label ? `${label}，跳到最新` : '收到新消息，跳到最新'
})
const patientJourneyLeadText = computed(() => {
  const record = detailRecord.value
  if (!record) return ''
  if (isPendingPaymentRecord(record)) return '当前问诊已生成收费记录，建议先完成模拟支付，再继续查看 AI 导诊和后续沟通内容。'
  const stage = recordProgressStage(record)
  if (stage === 'waiting_doctor') return '医生接手前，你仍可以继续补充症状变化、检查结果和相关图片资料。'
  if (recordHasUnreadDoctorReply(record)) return '医生刚有新回复，建议先查看本轮处理结果，再决定是否继续补充资料。'
  if (canSubmitDoctorGuidanceAck.value) return '医生已经给出本轮结论或随访建议，建议查看后点击“确认已查看”，让医生知道你已经收到。'
  if (hasPatientAcknowledgedGuidance.value) return '你已经确认查看本轮医生结论与后续建议，如恢复情况有变化，仍可继续补充。'
  if (canSubmitServiceFeedback.value && !record.serviceFeedback) return '医生已完成本轮处理，建议查看结论后补充服务评价，帮助后续继续优化。'
  if (canSubmitFollowUpUpdate.value) return '当前已进入恢复观察阶段，建议按计划同步最新恢复情况和仍需帮助的问题。'
  return detailArchiveSummary.value?.patientActionHint
    || detailRecord.value?.doctorConclusion?.patientInstruction
    || '下面整理了本次问诊最关键的查看入口和后续动作，可以按需快速跳转。'
})
const patientJourneySummaryText = computed(() => {
  const record = detailRecord.value
  if (!record) return ''
  const parts = []
  if (record.payment) parts.push(paymentSummaryText(record.payment))
  const doctorName = currentDoctorName(record)
  if (record?.doctorHandle?.completeTime) {
    parts.push(`${doctorName || '医生'}已于 ${formatDate(record.doctorHandle.completeTime)} 完成本轮处理`)
  } else if (record?.doctorHandle?.receiveTime) {
    parts.push(`${doctorName || '医生'}已接手，正在跟进处理`)
  } else {
    parts.push('当前问诊仍在等待医生接手')
  }
  if (detailMessageSummary.value.unreadCount > 0) {
    parts.push(`有 ${detailMessageSummary.value.unreadCount} 条医生新回复待查看`)
  }
  if (hasDoctorGuidanceToAcknowledge.value) {
    parts.push(hasPatientAcknowledgedGuidance.value ? '已确认查看医生结论与后续建议' : '医生结论与后续建议待确认查看')
  }
  if (canSubmitServiceFeedback.value) {
    parts.push(record.serviceFeedback ? '服务评价已提交，可随时补充修改' : '本次服务评价仍待提交')
  }
  return parts.join('；')
})
const patientJourneyTags = computed(() => {
  const record = detailRecord.value
  if (!record) return []
  const tags = [recordProgressLabel(record)]
  if (record.payment) tags.push(paymentStatusLabel(record.payment))
  const doctorName = currentDoctorName(record)
  if (doctorName) tags.push(`跟进医生 ${doctorName}`)
  const followState = followUpState(record)
  if (followState !== 'none') {
    tags.push(followUpState(record) === 'done' ? '随访已完成' : followUpTagLabel(record))
  }
  if (hasDoctorGuidanceToAcknowledge.value) {
    tags.push(hasPatientAcknowledgedGuidance.value ? '已确认查看医生建议' : '待确认查看医生建议')
  }
  if (canSubmitServiceFeedback.value) {
    tags.push(record.serviceFeedback ? '已提交服务评价' : '待提交服务评价')
  }
  return tags.slice(0, 4)
})
const patientJourneyCards = computed(() => {
  const record = detailRecord.value
  if (!record) return []
  const cards = []
  if (record.payment) {
    cards.push({
      key: 'payment',
      title: '问诊收费',
      status: paymentStatusLabel(record.payment),
      description: abbreviateText(paymentSummaryText(record.payment), 96),
      action: 'payment',
      actionLabel: isPendingPaymentRecord(record) ? '去支付' : '查看收费',
      tone: isPendingPaymentRecord(record) ? 'warning' : 'success'
    })
  }
  cards.push({
    key: 'conversation',
    title: '医患沟通',
    status: recordMessageStatus(record),
    description: abbreviateText(recordMessagePreview(record), 96),
    action: 'conversation',
    actionLabel: recordHasUnreadDoctorReply(record) ? '查看回复' : '打开沟通',
    tone: recordHasUnreadDoctorReply(record) ? 'success' : recordProgressStage(record) === 'waiting_doctor' ? 'warning' : 'info'
  })
  if (record.doctorHandle || record.doctorConclusion) {
    const hasConclusion = !!record.doctorConclusion
    cards.push({
      key: 'doctor_result',
      title: hasConclusion ? '最终结论' : '医生处理结果',
      status: hasConclusion
        ? (record.doctorConclusion.needFollowUp === 1 ? '需继续随访' : '已形成结论')
        : doctorHandleStatusLabel(record.doctorHandle?.status),
      description: abbreviateText(
        trimText(record.doctorConclusion?.patientInstruction || record.doctorConclusion?.diagnosisDirection || record.doctorHandle?.summary || record.doctorHandle?.medicalAdvice || '医生正在整理处理意见。'),
        96
      ),
      action: hasConclusion ? 'doctor_conclusion' : 'doctor_handle',
      actionLabel: hasConclusion ? '查看结论' : '查看处理',
      tone: hasConclusion || record.doctorHandle?.status === 'completed' ? 'success' : 'info'
    })
  }
  if (hasDoctorGuidanceToAcknowledge.value) {
    cards.push({
      key: 'guidance_ack',
      title: '查看确认',
      status: hasPatientAcknowledgedGuidance.value ? '已确认查看' : '待确认查看',
      description: abbreviateText(
        trimText(
          hasPatientAcknowledgedGuidance.value
            ? (latestPatientGuidanceAck.value?.content || '你已确认查看本轮医生结论与后续建议。')
            : doctorGuidanceAckHint.value
        ),
        96
      ),
      action: 'guidance_ack',
      actionLabel: hasPatientAcknowledgedGuidance.value ? '查看状态' : '去确认',
      tone: hasPatientAcknowledgedGuidance.value ? 'success' : 'warning'
    })
  }
  if (canSubmitCheckResultUpdate.value) {
    cards.push({
      key: 'check_result',
      title: '检查结果补充',
      status: latestPatientCheckResultUpdate.value ? '最近已补充' : '可补充',
      description: abbreviateText(trimText(latestPatientCheckResultUpdate.value?.content || checkResultUpdateHint.value || '如有新的化验、影像或病理结果，可继续补充给医生。'), 96),
      action: 'check_result',
      actionLabel: latestPatientCheckResultUpdate.value ? '继续补充' : '去补充',
      tone: latestPatientCheckResultUpdate.value ? 'success' : 'info'
    })
  }
  const followState = followUpState(record)
  if (followState !== 'none' || Array.isArray(record.doctorFollowUps) && record.doctorFollowUps.length) {
    const hasPendingFollowUpUpdate = canSubmitFollowUpUpdate.value
    const latestDoctorFollowUp = latestFollowUp(record)
    cards.push({
      key: 'followup',
      title: hasPendingFollowUpUpdate ? '恢复更新' : '随访安排',
      status: hasPendingFollowUpUpdate
        ? (followState === 'done' ? '随访已完成' : followUpTagLabel(record))
        : (latestDoctorFollowUp ? `已记录 ${record.doctorFollowUps.length} 次随访` : '暂无新的随访安排'),
      description: abbreviateText(
        trimText(
          latestPatientFollowUpUpdate.value?.content
          || latestDoctorFollowUp?.summary
          || detailFollowUpReminder.value
          || followUpUpdateHint.value
          || followUpLine(record)
          || '当前暂无新的随访进展。'
        ),
        96
      ),
      action: hasPendingFollowUpUpdate ? 'followup_update' : 'followup',
      actionLabel: hasPendingFollowUpUpdate ? '去更新' : '查看随访',
      tone: hasPendingFollowUpUpdate ? journeyToneFromFollowUpState(followState) : latestDoctorFollowUp ? 'success' : 'info'
    })
  }
  if (canSubmitServiceFeedback.value) {
    const feedback = record.serviceFeedback
    cards.push({
      key: 'feedback',
      title: '服务评价',
      status: feedback ? '已提交评价' : '待提交评价',
      description: abbreviateText(
        trimText(
          feedback?.feedbackText
          || (feedback ? `${feedback.serviceScore || '-'} 分，${serviceFeedbackResolvedLabel(feedback.isResolved)}` : serviceFeedbackReminderText(record) || '欢迎补充本次线上问诊体验，以及问题是否已经得到解决。')
        ),
        96
      ),
      action: 'feedback',
      actionLabel: feedback ? '更新评价' : '去评价',
      tone: feedback ? (feedback.isResolved === 1 && Number(feedback.serviceScore || 0) >= 3 ? 'success' : 'warning') : 'warning'
    })
  }
  if (detailArchiveSummary.value) {
    cards.push({
      key: 'archive',
      title: '问诊归档摘要',
      status: '已生成摘要',
      description: abbreviateText(trimText(detailArchiveSummary.value.archiveConclusion || detailArchiveSummary.value.patientActionHint || detailArchiveSummary.value.overview || '可集中回看本次问诊的重要结论与后续建议。'), 96),
      action: 'archive',
      actionLabel: '查看摘要',
      tone: 'info'
    })
  }
  return cards
})
const detailFollowUpReminder = computed(() => followUpReminderText(detailRecord.value))
const detailTimelineItems = computed(() => buildTimelineItems(detailRecord.value))
const latestPatientFollowUpUpdate = computed(() => [...consultationMessages.value]
  .reverse()
  .find(item => isFollowUpUpdateMessage(item)) || null)
const latestPatientCheckResultUpdate = computed(() => [...consultationMessages.value]
  .reverse()
  .find(item => isCheckResultUpdateMessage(item)) || null)
const latestPatientGuidanceAck = computed(() => [...consultationMessages.value]
  .reverse()
  .find(item => item?.senderType === 'user' && isDoctorGuidanceAckMessage(item)) || null)
const hasDoctorGuidanceToAcknowledge = computed(() => !!(detailRecord.value?.doctorConclusion || latestFollowUp(detailRecord.value)))
const latestDoctorGuidanceTime = computed(() => {
  const timestamps = [
    toTimestamp(detailRecord.value?.doctorConclusion?.updateTime),
    toTimestamp(latestFollowUp(detailRecord.value)?.createTime)
  ].filter(value => value != null)
  if (!timestamps.length) return null
  return new Date(Math.max(...timestamps))
})
const hasPatientAcknowledgedGuidance = computed(() => {
  if (!hasDoctorGuidanceToAcknowledge.value) return false
  const ackTime = toTimestamp(latestPatientGuidanceAck.value?.createTime)
  if (ackTime == null) return false
  const guidanceTime = toTimestamp(latestDoctorGuidanceTime.value)
  return guidanceTime == null || ackTime >= guidanceTime
})
const canSubmitDoctorGuidanceAck = computed(() => hasDoctorGuidanceToAcknowledge.value && !hasPatientAcknowledgedGuidance.value)
const showGuidanceAcknowledgementPanel = computed(() => hasDoctorGuidanceToAcknowledge.value || !!latestPatientGuidanceAck.value)
const latestDoctorGuidanceFocusAction = computed(() => {
  const followUpTime = toTimestamp(latestFollowUp(detailRecord.value)?.createTime)
  const conclusionTime = toTimestamp(detailRecord.value?.doctorConclusion?.updateTime)
  if (followUpTime != null && (conclusionTime == null || followUpTime >= conclusionTime)) return 'followup'
  if (detailRecord.value?.doctorConclusion) return 'doctor_conclusion'
  return ''
})
const doctorGuidanceAckStatusText = computed(() => {
  if (hasPatientAcknowledgedGuidance.value) {
    return latestPatientGuidanceAck.value?.createTime
      ? `已于 ${formatDate(latestPatientGuidanceAck.value.createTime)} 确认查看`
      : '已确认查看'
  }
  if (hasDoctorGuidanceToAcknowledge.value) return '待确认查看'
  return '暂无待确认内容'
})
const doctorGuidanceAckHint = computed(() => {
  if (hasPatientAcknowledgedGuidance.value) {
    return '你已确认查看本轮医生结论与后续建议，如恢复情况有变化，仍可继续补充。'
  }
  if (!hasDoctorGuidanceToAcknowledge.value) {
    return '当前还没有需要确认查看的医生结论或随访建议。'
  }
  if (detailRecord.value?.doctorConclusion?.needFollowUp === 1) {
    return '建议先完整阅读医生结论和随访安排，再确认已查看，方便医生判断你已经收到后续计划。'
  }
  return '建议先完整阅读本轮医生结论和患者指导，再确认已查看。'
})
const canSubmitCheckResultUpdate = computed(() => {
  if (!detailRecord.value) return false
  const status = `${detailRecord.value.status || ''}`.trim().toLowerCase()
  return ['triaged', 'processing', 'completed'].includes(status) || !!`${detailRecord.value.triageLevelName || ''}`.trim()
})
const checkResultUpdateHint = computed(() => {
  if (!detailRecord.value) return ''
  if (detailRecord.value.status === 'completed') return '问诊已完成，如有新的化验、影像或病理结果，仍可继续补充给医生，便于判断是否需要调整建议。'
  if (detailRecord.value.status === 'processing') return '如果医生让你补充化验单、影像报告或其它检查结果，可在这里结构化提交。'
  return '导诊完成后，可先把关键检查结果结构化补充给医生，医生接手后能更快继续判断。'
})
const canSubmitFollowUpUpdate = computed(() => !!detailRecord.value && ['pending', 'due_today', 'overdue'].includes(followUpState(detailRecord.value)))
const followUpUpdateHint = computed(() => {
  if (!detailRecord.value) return ''
  const state = followUpState(detailRecord.value)
  if (state === 'overdue') return '褰撳墠宸叉槸閫炬湡闅忚锛屽缓璁敖蹇妸鏈€鏂版仮澶嶆儏鍐点€佺棁鐘跺彉鍖栧拰浠嶉渶甯姪鐨勫湴鏂瑰悓姝ョ粰鍖荤敓銆?'
  if (state === 'due_today') return '褰撳墠闅忚璁″垝浠婂ぉ鍒版湡锛屽彲鍏堣ˉ鍏呬粖澶╃殑鎭㈠鎯呭喌锛屾柟渚垮尰鐢熷強鏃跺洖鐪嬨€?'
  return '濡傜棁鐘朵粛鏈夊彉鍖栨垨杩橀渶甯姪锛屽彲鍦ㄨ繖閲岀敤缁撴瀯鍖栨柟寮忚ˉ鍏呮仮澶嶆洿鏂般€?'
})
const canSubmitServiceFeedback = computed(() => !!(
  detailRecord.value
  && (detailRecord.value.status === 'completed' || detailRecord.value?.doctorHandle?.status === 'completed')
))

watch(activeCategoryId, (value) => {
  if (value) loadTemplate(value)
})

watch(() => route.query, () => {
  syncingRecordRoute.value = true
  applyRouteRecordFilters()
  nextTick(() => { syncingRecordRoute.value = false })
}, { immediate: true })

watch(() => [route.query.id, route.query.action], () => {
  autoOpenRecordDetail()
})

watch(() => feedbackForm.manualCorrectDepartmentId, (value) => {
  if (!feedbackForm.manualCorrectDoctorId) return
  const currentDoctor = (feedbackOptions.value?.doctors || []).find(item => item.id === feedbackForm.manualCorrectDoctorId)
  if (currentDoctor && value && currentDoctor.departmentId !== value) {
    feedbackForm.manualCorrectDoctorId = null
  }
})

watch([recordKeyword, recordStatusFilter, recordProgressFilter, recordFollowUpFilter], () => {
  if (syncingRecordRoute.value) return
  const routeState = normalizeRecordRouteQuery(route.query)
  const nextState = normalizeRecordRouteQuery(buildRecordRouteQuery())
  if (sameRecordRouteQuery(routeState, nextState)) return
  router.replace({ path: route.path, query: buildRecordRouteQuery() })
})

function loadData() {
  loadPatients()
  loadHistories()
  loadCategories()
  loadRecords()
  loadFeedbackOptions()
}

function normalizeRecordRouteQuery(query = {}) {
  const keyword = typeof query.keyword === 'string' ? query.keyword.trim() : ''
  const status = typeof query.status === 'string' && ['submitted', 'triaged', 'processing', 'completed'].includes(query.status)
    ? query.status
    : ''
  const progress = typeof query.progress === 'string' && ['doctor_replied', 'waiting_doctor', 'doctor_processing', 'pending_feedback', 'completed'].includes(query.progress)
    ? query.progress
    : 'all'
  const followUp = typeof query.followUp === 'string' && ['pending', 'due_today', 'overdue'].includes(query.followUp)
    ? query.followUp
    : 'all'
  return { keyword, status, progress, followUp }
}

function resolveConsultationAction(value) {
  const action = typeof value === 'string' ? value.trim() : ''
  return ['conversation', 'payment', 'followup', 'feedback', 'archive', 'doctor_handle', 'doctor_conclusion', 'guidance_ack', 'check_result', 'followup_update'].includes(action) ? action : ''
}

function buildRecordRouteQuery({ includeId = true, action = null } = {}) {
  const query = { ...route.query }
  const keyword = recordKeyword.value.trim()
  const routeAction = action === null ? resolveConsultationAction(route.query.action) : resolveConsultationAction(action)
  delete query.keyword
  delete query.status
  delete query.progress
  delete query.followUp
  delete query.action
  if (!includeId) delete query.id
  if (keyword) query.keyword = keyword
  if (recordStatusFilter.value) query.status = recordStatusFilter.value
  if (recordProgressFilter.value !== 'all') query.progress = recordProgressFilter.value
  if (recordFollowUpFilter.value !== 'all') query.followUp = recordFollowUpFilter.value
  if (routeAction) query.action = routeAction
  return query
}

function applyRouteRecordFilters() {
  const query = normalizeRecordRouteQuery(route.query)
  recordKeyword.value = query.keyword
  recordStatusFilter.value = query.status
  recordProgressFilter.value = query.progress
  recordFollowUpFilter.value = query.followUp
}

function sameRecordRouteQuery(left, right) {
  return left.keyword === right.keyword
    && left.status === right.status
    && left.progress === right.progress
    && left.followUp === right.followUp
}

function applyRecordQuickFilter({ status = '', progress = 'all', followUp = 'all', keyword = '' } = {}) {
  recordKeyword.value = keyword
  recordStatusFilter.value = status
  recordProgressFilter.value = progress
  recordFollowUpFilter.value = followUp
  nextTick(() => focusHistorySection())
}

function focusHistorySection() {
  historySectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function clearFocusedDetailSection() {
  focusedDetailSection.value = ''
  if (detailSectionFocusTimer) {
    clearTimeout(detailSectionFocusTimer)
    detailSectionFocusTimer = null
  }
}

function focusDetailActionSection(action, recordId = null) {
  const targetAction = resolveConsultationAction(action)
  if (!targetAction) return
  nextTick(() => {
    let target = null
    if (targetAction === 'conversation') {
      target = conversationInputRef.value?.$el || conversationInputRef.value?.textarea || conversationInputRef.value
    } else if (targetAction === 'payment') {
      target = paymentPanelRef.value?.$el || paymentPanelRef.value
    } else if (targetAction === 'followup') {
      target = followUpPanelRef.value?.$el || followUpPanelRef.value
    } else if (targetAction === 'feedback') {
      target = serviceFeedbackPanelRef.value?.$el || serviceFeedbackPanelRef.value
    } else if (targetAction === 'archive') {
      target = archiveSummaryPanelRef.value?.$el || archiveSummaryPanelRef.value
    } else if (targetAction === 'doctor_handle') {
      target = doctorHandlePanelRef.value?.$el || doctorHandlePanelRef.value
    } else if (targetAction === 'doctor_conclusion') {
      target = doctorConclusionPanelRef.value?.$el || doctorConclusionPanelRef.value
    } else if (targetAction === 'guidance_ack') {
      target = guidanceAckPanelRef.value?.$el || guidanceAckPanelRef.value
    } else if (targetAction === 'check_result') {
      target = checkResultUpdatePanelRef.value?.$el || checkResultUpdatePanelRef.value
    } else {
      target = followUpUpdatePanelRef.value?.$el || followUpUpdatePanelRef.value
    }
    if (!target?.scrollIntoView) return
    clearFocusedDetailSection()
    focusedDetailSection.value = targetAction
    target.scrollIntoView({ behavior: 'smooth', block: 'center' })
    if (targetAction === 'conversation' && canSendMessage.value) {
      setTimeout(() => conversationInputRef.value?.focus?.(), 180)
    }
    detailSectionFocusTimer = setTimeout(() => {
      if (focusedDetailSection.value === targetAction) focusedDetailSection.value = ''
      detailSectionFocusTimer = null
    }, 2400)
    const nextQuery = buildRecordRouteQuery({ action: '' })
    if (recordId) nextQuery.id = recordId
    router.replace({ path: route.path, query: nextQuery })
  })
}

function loadPatients() {
  get('/api/user/patient/list', (data) => {
    try {
      patients.value = ensureArray(data)
      if (!selectedPatientId.value && patients.value.length) {
        selectedPatientId.value = patients.value.find(item => item.isDefault === 1)?.id || patients.value[0].id
      }
    } catch (error) {
      console.error(error)
      patients.value = []
      selectedPatientId.value = null
      ElMessage.warning('就诊人数据处理失败，请刷新后重试')
    }
  })
}

function loadHistories() {
  get('/api/user/medical-history/list', (data) => {
    try {
      histories.value = ensureArray(data)
    } catch (error) {
      console.error(error)
      histories.value = []
      ElMessage.warning('健康档案数据处理失败，请刷新后重试')
    }
  })
}

function loadCategories() {
  get('/api/user/consultation/category/list', (data) => {
    try {
      categories.value = ensureArray(data)
      if (!activeCategoryId.value && categories.value.length) {
        activeCategoryId.value = categories.value[0].id
      }
    } catch (error) {
      console.error(error)
      categories.value = []
      activeCategoryId.value = null
      ElMessage.warning('问诊分类数据处理失败，请刷新后重试')
    }
  })
}

function loadTemplate(categoryId) {
  templateLoading.value = true
  get(`/api/user/consultation/template/default?categoryId=${categoryId}`, (data) => {
    try {
      template.value = ensureObject(data)
      resetForm()
    } catch (error) {
      console.error(error)
      template.value = null
      clearFormData()
      ElMessage.warning('问诊模板处理失败，请刷新后重试')
    } finally {
      templateLoading.value = false
    }
  }, (message) => {
    template.value = null
    clearFormData()
    templateLoading.value = false
    ElMessage.warning(message || '模板加载失败')
  })
}

function loadRecords(callback) {
  historyLoading.value = true
  get('/api/user/consultation/record/list', (data) => {
    try {
      records.value = ensureArray(data)
        .map(item => normalizeConsultationRecord(item))
        .filter(Boolean)
      callback?.(records.value)
      autoOpenRecordDetail()
    } catch (error) {
      console.error(error)
      records.value = []
      ElMessage.warning('问诊记录处理失败，请刷新后重试')
    } finally {
      historyLoading.value = false
    }
  }, () => {
    historyLoading.value = false
  })
}

function loadFeedbackOptions() {
  get('/api/user/consultation/feedback/options', (data) => {
    try {
      const nextOptions = ensureObject(data) || {}
      feedbackOptions.value = {
        departments: ensureArray(nextOptions.departments),
        doctors: ensureArray(nextOptions.doctors)
      }
    } catch (error) {
      console.error(error)
      feedbackOptions.value = { departments: [], doctors: [] }
      ElMessage.warning('反馈选项处理失败，请刷新后重试')
    }
  })
}

function selectCategory(item) {
  if (activeCategoryId.value === item.id) return
  activeCategoryId.value = item.id
}

function resetForm() {
  clearFormData()
  ;(template.value?.fields || []).forEach(field => {
    formData[field.fieldCode] = initialFieldValue(field)
  })
}

function clearFormData() {
  Object.keys(formData).forEach(key => delete formData[key])
}

function initialFieldValue(field) {
  if (field.fieldType === 'multi_select') return []
  if (field.fieldType === 'switch') return field.defaultValue === '1' ? '1' : '0'
  if (field.fieldType === 'number') return field.defaultValue ? Number(field.defaultValue) : null
  return field.defaultValue || ''
}

function fieldOptions(field) {
  if (!field.optionsJson) return []
  try {
    return JSON.parse(field.optionsJson)
  } catch {
    return []
  }
}

function relationLabel(value) {
  return {
    self: '本人',
    child: '子女',
    parent: '父母',
    spouse: '配偶',
    other: '其他'
  }[value] || '其他'
}

function genderLabel(value) {
  return {
    male: '男',
    female: '女',
    unknown: '未知'
  }[value] || '未知'
}

function fieldTypeLabel(value) {
  return {
    input: '单行输入',
    textarea: '多行输入',
    single_select: '单选',
    multi_select: '多选',
    date: '日期',
    number: '数字',
    upload: '上传',
    switch: '开关'
  }[value] || value
}

function statusLabel(value) {
  return {
    submitted: '已提交',
    triaged: '已分诊',
    processing: '处理中',
    completed: '已完成'
  }[value] || value || '-'
}

function normalizePayment(payment) {
  const record = ensureObject(payment)
  if (!record) return null
  const amount = Number(record.amount ?? 0)
  return {
    ...record,
    amount: Number.isFinite(amount) ? amount : 0
  }
}

function paymentStatusLabel(payment) {
  const record = normalizePayment(payment)
  if (!record) return '未收费'
  if (record.amount <= 0) return '免费'
  if (record.status === 'paid') return '已支付'
  if (record.status === 'pending') return '待支付'
  return record.status || '未收费'
}

function paymentStatusTagType(payment) {
  const record = normalizePayment(payment)
  if (!record) return 'info'
  if (record.amount <= 0 || record.status === 'paid') return 'success'
  if (record.status === 'pending') return 'warning'
  return 'info'
}

function paymentChannelLabel(channel) {
  return {
    mock: '模拟支付',
    free: '免费'
  }[`${channel || ''}`.trim().toLowerCase()] || (channel || '-')
}

function paymentSummaryText(payment) {
  const record = normalizePayment(payment)
  if (!record) return '当前问诊尚未生成收费记录。'
  if (record.amount <= 0) return '当前问诊无需额外支付，可直接继续后续 AI 导诊与医患沟通流程。'
  if (record.status === 'paid') {
    const paidTimeText = record.paidTime ? `，支付时间 ${formatDate(record.paidTime)}` : ''
    return `本次问诊已完成模拟支付，金额 ${formatAmount(record.amount)}${paidTimeText}。`
  }
  return `本次问诊待支付 ${formatAmount(record.amount)}，点击“模拟支付”即可完成演示版付款。`
}

function isPendingPaymentRecord(record) {
  const payment = normalizePayment(record?.payment)
  return !!payment && payment.amount > 0 && payment.status === 'pending'
}

function formatAmount(value) {
  const amount = Number(value ?? 0)
  return `￥${(Number.isFinite(amount) ? amount : 0).toFixed(2)}`
}

function doctorHandleStatusLabel(value) {
  return value === 'completed' ? '处理完成' : '处理中'
}

function conditionLevelLabel(value) {
  return {
    low: '轻度',
    medium: '中度',
    high: '较高风险',
    critical: '危急'
  }[value] || '未填写'
}

function dispositionLabel(value) {
  return {
    observe: '继续观察',
    online_followup: '线上随访',
    offline_visit: '线下就医',
    emergency: '立即急诊'
  }[value] || '未填写'
}

function aiConsistencyLabel(value) {
  return value === 1 ? '与 AI 一致' : value === 0 ? '与 AI 不一致' : '未判断'
}

function doctorFollowUpText(conclusion) {
  if (!conclusion) return ''
  if (conclusion.needFollowUp === 1) return conclusion.followUpWithinDays ? `${conclusion.followUpWithinDays} 天内随访` : '需要随访'
  if (conclusion.needFollowUp === 0) return '暂不需要随访'
  return ''
}

function followUpTypeLabel(value) {
  return {
    platform: '平台随访',
    phone: '电话随访',
    offline: '线下随访',
    other: '其他方式'
  }[value] || '其他方式'
}

function patientStatusLabel(value) {
  return {
    improved: '明显好转',
    stable: '基本稳定',
    worsened: '出现加重',
    other: '其他情况'
  }[value] || '其他情况'
}

function triageActionLabel(value) {
  return {
    emergency: '立即急诊',
    offline: '尽快线下',
    followup: '复诊随访',
    online: '线上继续'
  }[value] || '继续关注'
}

function triageBadgeStyle(color) {
  if (!color) return {}
  return {
    color,
    borderColor: `${color}33`,
    backgroundColor: `${color}14`
  }
}

function buildHealthSummary(history) {
  if (!history) return '当前就诊人尚未建立健康档案，建议先补充后再发起问诊。'
  const segments = []
  if (history.allergyHistory) segments.push(`过敏史：${history.allergyHistory}`)
  if (history.chronicHistory) segments.push(`慢病史：${history.chronicHistory}`)
  if (history.pastHistory) segments.push(`既往史：${history.pastHistory}`)
  if (history.medicationHistory) segments.push(`长期用药：${history.medicationHistory}`)
  if (history.pregnancyStatus && history.pregnancyStatus !== 'unknown') segments.push(`孕期状态：${history.pregnancyStatus}`)
  if (history.lactationStatus && history.lactationStatus !== 'unknown') segments.push(`哺乳状态：${history.lactationStatus}`)
  return segments.length ? segments.join('；') : '健康档案已创建，但尚未填写更多重点信息。'
}

function isFieldVisible(field) {
  const rule = `${field.conditionRule || ''}`.trim()
  if (!rule) return true
  if (rule.includes('||')) {
    return rule.split('||').some(item => evaluateClause(item.trim()))
  }
  return rule.split('&&').every(item => evaluateClause(item.trim()))
}

function evaluateClause(clause) {
  if (!clause) return true
  const operator = clause.includes('!=') ? '!=' : '='
  const index = clause.indexOf(operator)
  if (index <= 0) return true

  const key = clause.slice(0, index).trim()
  const expected = clause.slice(index + operator.length).trim()
  const expectedValues = expected.split(/[|,]/).map(item => item.trim()).filter(Boolean)
  const currentValues = currentConditionValues(key)
  const matched = currentValues.some(item => expectedValues.includes(item))
  return operator === '=' ? matched : !matched
}

function currentConditionValues(key) {
  if (key === 'gender' || key === 'patientGender') {
    return selectedPatient.value?.gender ? [selectedPatient.value.gender] : []
  }
  const value = formData[key]
  if (Array.isArray(value)) return value.filter(Boolean)
  if (value === null || value === undefined || value === '') return []
  return [String(value)]
}

function beforeImageUpload(file) {
  const isImage = `${file.type || ''}`.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 <= 5
  if (!isImage) {
    ElMessage.error('当前阶段仅支持上传图片资料')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('上传图片不能超过 5MB')
    return false
  }
  return true
}

function handleUploadSuccess(field, response) {
  if (response?.code !== 200) {
    ElMessage.error(response?.message || '资料上传失败')
    return
  }
  formData[field.fieldCode] = response.data
  ElMessage.success(`${field.fieldLabel}上传成功`)
}

function handleUploadError() {
  ElMessage.error('资料上传失败，请稍后再试')
}

function submitConsultation() {
  if (!selectedPatientId.value) {
    ElMessage.warning('请先选择就诊人')
    return
  }
  if (!activeCategoryId.value || !template.value) {
    ElMessage.warning('请先选择问诊分类')
    return
  }

  for (const field of visibleFields.value) {
    if (field.isRequired === 1 && isEmptyField(field)) {
      ElMessage.warning(`请完善必填字段“${field.fieldLabel}”`)
      return
    }
  }

  const answers = visibleFields.value
    .map(field => ({
      fieldCode: field.fieldCode,
      fieldValue: serializeFieldValue(field)
    }))
    .filter(item => shouldSubmitValue(item.fieldValue))

  if (!answers.length) {
    ElMessage.warning('请至少填写一项问诊资料后再提交')
    return
  }

  submitting.value = true
  post('/api/user/consultation/record/create', {
    patientId: selectedPatientId.value,
    categoryId: activeCategoryId.value,
    templateId: template.value.id,
    answers
  }, () => {
    submitting.value = false
    ElMessage.success('问诊资料提交成功，正在进入 AI 导诊工作区')
    loadRecords((list) => {
      const latestRecord = list?.[0]
      if (latestRecord?.id) {
        resetForm()
        if (isPendingPaymentRecord(latestRecord)) {
          openRecordDetail(latestRecord, { action: 'payment' })
          return
        }
        openTriageWorkspace(latestRecord.id)
        return
      }
      resetForm()
    })
  }, (message) => {
    submitting.value = false
    ElMessage.warning(message || '问诊资料提交失败')
  })
}

function isEmptyField(field) {
  const value = formData[field.fieldCode]
  if (field.fieldType === 'multi_select') return !Array.isArray(value) || !value.length
  if (field.fieldType === 'number') return value === null || value === undefined || value === ''
  if (field.fieldType === 'switch') return value === null || value === undefined || value === ''
  return !`${value || ''}`.trim()
}

function serializeFieldValue(field) {
  const value = formData[field.fieldCode]
  if (field.fieldType === 'multi_select') return JSON.stringify(Array.isArray(value) ? value : [])
  if (field.fieldType === 'number') return value === null || value === undefined || value === '' ? '' : String(value)
  if (field.fieldType === 'switch') return value ?? '0'
  return `${value || ''}`.trim()
}

function shouldSubmitValue(value) {
  return value !== null && value !== undefined && value !== '' && value !== '[]'
}

function submitMockPayment() {
  const recordId = detailRecord.value?.id
  if (!recordId || !isPendingPaymentRecord(detailRecord.value) || paymentSubmitting.value) return
  paymentSubmitting.value = true
  post('/api/user/consultation/payment/mock-pay', {
    recordId
  }, (data) => {
    paymentSubmitting.value = false
    const nextPayment = normalizePayment(data)
    if (detailRecord.value?.id === recordId) {
      detailRecord.value = {
        ...detailRecord.value,
        payment: nextPayment
      }
      syncRecordSnapshot(detailRecord.value)
    }
    ElMessage.success('模拟支付成功')
    refreshRecordDetail(recordId)
  }, (message) => {
    paymentSubmitting.value = false
    ElMessage.warning(message || '模拟支付失败')
  })
}

function resolveConversationBoardElement() {
  return conversationBoardRef.value?.$el || conversationBoardRef.value || null
}

function isConversationNearBottom(threshold = 96) {
  const element = resolveConversationBoardElement()
  if (!element) return true
  return element.scrollHeight - element.scrollTop - element.clientHeight <= threshold
}

function scrollConversationToBottom(force = false) {
  nextTick(() => {
    const element = resolveConversationBoardElement()
    if (!element) return
    if (!force && !isConversationNearBottom()) return
    element.scrollTop = element.scrollHeight
  })
}

function clearConversationNewMessageState() {
  conversationPendingNewMessageCount.value = 0
  conversationPendingNewMessageLabel.value = ''
}

function resetConversationSyncState() {
  conversationLastSyncedAt.value = null
  conversationSyncStatus.value = 'idle'
}

function recordConversationSyncSuccess() {
  conversationLastSyncedAt.value = new Date()
  conversationSyncStatus.value = 'live'
}

function buildConversationPendingNewMessageLabel(message) {
  if (!message) return ''
  const senderName = trimText(message?.senderName) || '医生'
  const preview = trimText(buildLocalMessagePreview(message))
  if (preview) return `${senderName}有新回复：${abbreviateText(preview, 20)}`
  return `${senderName}有新回复`
}

function handleConversationBoardScroll() {
  if (isConversationNearBottom()) {
    clearConversationNewMessageState()
  }
}

function jumpConversationToLatest() {
  clearConversationNewMessageState()
  scrollConversationToBottom(true)
}

function canPollConversation() {
  return detailVisible.value
    && !!detailRecord.value?.id
    && !detailLoading.value
    && typeof document !== 'undefined'
    && document.visibilityState === 'visible'
}

function stopConversationPolling() {
  if (conversationPollTimer) {
    clearInterval(conversationPollTimer)
    conversationPollTimer = null
  }
  conversationPollBusy = false
}

function pollConversationMessages() {
  const recordId = detailRecord.value?.id
  if (!recordId || !canPollConversation() || conversationPollBusy || messageLoading.value || messageSending.value || triageAiSending.value) return
  conversationPollBusy = true
  loadConsultationMessages(recordId, {
    silent: true,
    onFinally: () => {
      conversationPollBusy = false
    }
  })
}

function startConversationPolling() {
  stopConversationPolling()
  if (!detailVisible.value) return
  conversationPollTimer = setInterval(() => {
    pollConversationMessages()
  }, CONVERSATION_POLL_INTERVAL)
}

function handleConversationVisibilityChange() {
  if (typeof document === 'undefined') return
  if (document.visibilityState === 'visible') {
    if (detailVisible.value) {
      startConversationPolling()
      pollConversationMessages()
    }
    return
  }
  stopConversationPolling()
}

function autoOpenRecordDetail() {
  const id = Number(route.query.id || 0)
  const action = resolveConsultationAction(route.query.action)
  if (!id) return
  if (detailVisible.value && detailRecord.value?.id === id && !detailLoading.value) {
    if (action) focusDetailActionSection(action, id)
    return
  }
  const record = records.value.find(item => item.id === id)
  if (!record) {
    openRecordDetailById(id, { action })
    return
  }
  if (detailVisible.value && (detailRecord.value?.id === id || detailLoading.value)) return
  openRecordDetail(record, { syncRoute: false, action })
}

function applyDetailRecordPayload(data) {
  const nextRecord = normalizeConsultationRecord(data, { detail: true })
  detailRecord.value = nextRecord
  syncRecordSnapshot(nextRecord)
  applyServiceFeedbackForm(nextRecord?.serviceFeedback)
  applyFeedbackForm(nextRecord?.triageFeedback)
}

function openRecordDetailById(recordId, options = {}) {
  const { action = '' } = options
  if (!recordId) return
  if (detailVisible.value && (detailRecord.value?.id === recordId || detailLoading.value)) return

  detailVisible.value = true
  detailLoading.value = true
  detailRecord.value = null
  stopConversationPolling()
  clearConversationNewMessageState()
  resetConversationSyncState()
  clearFocusedDetailSection()
  triageAiSending.value = false
  consultationMessages.value = []
  messageLoading.value = false
  messageSending.value = false
  resetTriageAiDraft()
  resetMessageDraft()
  resetFollowUpUpdateForm()
  resetCheckResultUpdateForm()

  get(`/api/user/consultation/record/detail?recordId=${recordId}`, (data) => {
    try {
      applyDetailRecordPayload(data)
      loadConsultationMessages(recordId, { preserveScroll: false })
      startConversationPolling()
      const focusAction = resolveConsultationAction(action || route.query.action)
      if (focusAction) focusDetailActionSection(focusAction, recordId)
    } catch (error) {
      console.error(error)
      stopConversationPolling()
      ElMessage.warning('问诊记录详情处理失败，请刷新后重试')
    } finally {
      detailLoading.value = false
    }
  }, (message) => {
    detailLoading.value = false
    detailVisible.value = false
    stopConversationPolling()
    clearFocusedDetailSection()
    if (route.query.id || route.query.action) {
      router.replace({ path: route.path, query: buildRecordRouteQuery({ includeId: false, action: '' }) })
    }
    ElMessage.warning(message || '问诊记录详情加载失败')
  })
}

function openRecordDetail(row, options = {}) {
  const { syncRoute = true, action = '' } = options
  const actionValue = resolveConsultationAction(action)
  if (syncRoute && (Number(route.query.id || 0) !== row.id || resolveConsultationAction(route.query.action) !== actionValue)) {
    router.replace({
      path: route.path,
      query: { ...buildRecordRouteQuery({ includeId: false, action: actionValue }), id: row.id }
    })
  }
  detailVisible.value = true
  detailLoading.value = true
  detailRecord.value = null
  stopConversationPolling()
  clearConversationNewMessageState()
  resetConversationSyncState()
  clearFocusedDetailSection()
  triageAiSending.value = false
  consultationMessages.value = []
  messageLoading.value = false
  messageSending.value = false
  resetTriageAiDraft()
  resetMessageDraft()
  resetFollowUpUpdateForm()
  resetCheckResultUpdateForm()
  get(`/api/user/consultation/record/detail?recordId=${row.id}`, (data) => {
    try {
      applyDetailRecordPayload(data)
      loadConsultationMessages(row.id, { preserveScroll: false })
      startConversationPolling()
      const focusAction = actionValue || resolveConsultationAction(route.query.action)
      if (focusAction) focusDetailActionSection(focusAction, row.id)
    } catch (error) {
      console.error(error)
      stopConversationPolling()
      ElMessage.warning('问诊记录详情处理失败，请刷新后重试')
    } finally {
      detailLoading.value = false
    }
  }, (message) => {
    detailLoading.value = false
    detailVisible.value = false
    stopConversationPolling()
    clearFocusedDetailSection()
    if (route.query.id || route.query.action) {
      router.replace({ path: route.path, query: buildRecordRouteQuery({ includeId: false, action: '' }) })
    }
    ElMessage.warning(message || '问诊记录详情加载失败')
  })
}

function openTriageWorkspace(recordId = detailRecord.value?.id) {
  if (!recordId) return
  router.push({ path: '/index/triage', query: { recordId } })
}

function refreshRecordDetail(recordId = detailRecord.value?.id, options = {}) {
  if (!recordId) return
  const { reloadConversation = false } = options
  get(`/api/user/consultation/record/detail?recordId=${recordId}`, (data) => {
    try {
      applyDetailRecordPayload(data)
      if (reloadConversation) loadConsultationMessages(recordId)
    } catch (error) {
      console.error(error)
      ElMessage.warning('问诊记录刷新后处理失败，请稍后重试')
    }
  }, (message) => {
    ElMessage.warning(message || '问诊记录详情刷新失败')
  })
}

function resetTriageAiDraft() {
  triageAiDraft.content = ''
}

function sendTriageAiMessage() {
  const recordId = detailRecord.value?.id
  if (!recordId || !detailRecord.value?.triageSession) return
  const content = `${triageAiDraft.content || ''}`.trim()
  if (!content) {
    ElMessage.warning('请先补充想继续告诉 AI 的内容')
    return
  }

  triageAiSending.value = true
  post('/api/user/consultation/triage/message/send', {
    recordId,
    content
  }, () => {
    triageAiSending.value = false
    resetTriageAiDraft()
    ElMessage.success('AI 导诊已结合你的补充信息继续分析')
    refreshRecordDetail(recordId)
  }, (message) => {
    triageAiSending.value = false
    ElMessage.warning(message || 'AI 导诊继续分析失败')
  })
}

function loadConsultationMessages(recordId = detailRecord.value?.id, options = {}) {
  if (!recordId) return
  const {
    silent = false,
    preserveScroll = true,
    onFinally = null
  } = options
  const shouldStickToBottom = !preserveScroll || isConversationNearBottom() || !consultationMessages.value.length
  const previousCount = consultationMessages.value.length
  const previousLatestId = Number(consultationMessages.value[consultationMessages.value.length - 1]?.id || 0)
  conversationSyncStatus.value = 'syncing'
  if (!silent) messageLoading.value = true
  get(`/api/user/consultation/message/list?recordId=${recordId}`, (data) => {
    try {
      consultationMessages.value = ensureArray(data)
      syncRecordMessageSummary(recordId, buildLocalMessageSummary(consultationMessages.value))
      recordConversationSyncSuccess()
      const nextLatestId = Number(consultationMessages.value[consultationMessages.value.length - 1]?.id || 0)
      if (shouldStickToBottom) {
        clearConversationNewMessageState()
        scrollConversationToBottom(true)
      } else if (nextLatestId > 0 && nextLatestId !== previousLatestId) {
        const newMessages = consultationMessages.value.filter(item => Number(item?.id || 0) > previousLatestId)
        const incomingMessages = newMessages.filter(item => !isUserMessage(item))
        const deltaCount = Math.max(incomingMessages.length || consultationMessages.value.length - previousCount, 1)
        conversationPendingNewMessageCount.value += deltaCount
        conversationPendingNewMessageLabel.value = buildConversationPendingNewMessageLabel(
          incomingMessages[incomingMessages.length - 1] || newMessages[newMessages.length - 1] || consultationMessages.value[consultationMessages.value.length - 1]
        )
      }
    } catch (error) {
      console.error(error)
      conversationSyncStatus.value = 'failed'
      ElMessage.warning('问诊沟通消息处理失败，请稍后重试')
    } finally {
      if (!silent) messageLoading.value = false
      onFinally?.()
    }
  }, (message) => {
    if (!silent) consultationMessages.value = []
    if (!silent) messageLoading.value = false
    conversationSyncStatus.value = 'failed'
    if (silent) return onFinally?.()
    onFinally?.()
    ElMessage.warning(message || '问诊沟通消息加载失败')
  })
}

function normalizeMessageSummary(summary) {
  return {
    totalCount: Number(summary?.totalCount || 0),
    userMessageCount: Number(summary?.userMessageCount || 0),
    doctorMessageCount: Number(summary?.doctorMessageCount || 0),
    unreadCount: Number(summary?.unreadCount || 0),
    latestSenderType: summary?.latestSenderType || '',
    latestSenderName: summary?.latestSenderName || '',
    latestMessageType: summary?.latestMessageType || '',
    latestMessagePreview: summary?.latestMessagePreview || '',
    latestTime: summary?.latestTime || null
  }
}

function ensureArray(value) {
  return Array.isArray(value) ? value : []
}

function ensureObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : null
}

function normalizeConsultationRecord(record, options = {}) {
  const { detail = false } = options
  const nextRecord = ensureObject(record)
  if (!nextRecord) return null

  const normalizedRecord = {
    ...nextRecord,
    smartDispatch: normalizeSmartDispatch(nextRecord.smartDispatch),
    messageSummary: normalizeMessageSummary(nextRecord.messageSummary),
    payment: normalizePayment(nextRecord.payment)
  }

  if (!detail) return normalizedRecord

  normalizedRecord.answers = ensureArray(nextRecord.answers)
  normalizedRecord.prescriptions = ensureArray(nextRecord.prescriptions)
  normalizedRecord.recommendedDoctors = ensureArray(nextRecord.recommendedDoctors)
  normalizedRecord.doctorFollowUps = ensureArray(nextRecord.doctorFollowUps)
  normalizedRecord.aiComparison = ensureObject(nextRecord.aiComparison)
    ? {
        ...nextRecord.aiComparison,
        aiRecommendedDoctors: ensureArray(nextRecord.aiComparison.aiRecommendedDoctors),
        aiRiskFlags: ensureArray(nextRecord.aiComparison.aiRiskFlags)
      }
    : null
  normalizedRecord.archiveSummary = ensureObject(nextRecord.archiveSummary)
    ? {
        ...nextRecord.archiveSummary,
        riskFlags: ensureArray(nextRecord.archiveSummary.riskFlags),
        conclusionTags: ensureArray(nextRecord.archiveSummary.conclusionTags),
        nextActions: ensureArray(nextRecord.archiveSummary.nextActions)
      }
    : null
  normalizedRecord.triageSession = ensureObject(nextRecord.triageSession)
    ? {
        ...nextRecord.triageSession,
        messages: ensureArray(nextRecord.triageSession.messages)
      }
    : null

  return normalizedRecord
}

function getMessageSummary(record) {
  return normalizeMessageSummary(record?.messageSummary)
}

function recordHasUnreadDoctorReply(record) {
  return getMessageSummary(record).unreadCount > 0
}

function recordMessageStatus(record) {
  const summary = getMessageSummary(record)
  if (summary.totalCount <= 0) return '暂无沟通'
  if (summary.unreadCount > 0) return `医生新回复 ${summary.unreadCount} 条`
  if (summary.latestSenderType === 'user' && isDoctorGuidanceAckMessage({ messageType: summary.latestMessageType })) return '已确认查看医生建议'
  if (summary.latestSenderType === 'user') return '已发送，待医生处理'
  if (summary.latestSenderType === 'doctor') return '医生已回复'
  return '沟通中'
}

function recordMessagePreview(record) {
  return getMessageSummary(record).latestMessagePreview || '暂未产生沟通消息'
}

function claimedDoctorName(record) {
  return `${record?.doctorAssignment?.doctorName || getSmartDispatch(record).claimedDoctorName || ''}`.trim()
}

function currentDoctorName(record) {
  const name = `${record?.doctorHandle?.doctorName || claimedDoctorName(record) || ''}`.trim()
  if (name) return name
  const summary = getMessageSummary(record)
  return summary.latestSenderType === 'doctor' ? `${summary.latestSenderName || ''}`.trim() : ''
}

function hasDoctorClaimed(record) {
  if (`${record?.doctorAssignment?.status || ''}`.trim().toLowerCase() === 'claimed') return true
  return `${getSmartDispatch(record).status || ''}`.includes('claimed')
}

function hasDoctorTakenOver(record) {
  const summary = getMessageSummary(record)
  return !!(
    record?.doctorHandle?.doctorName
    || record?.status === 'processing'
    || hasDoctorClaimed(record)
    || summary.latestSenderType === 'doctor'
  )
}

function recordProgressStage(record) {
  if (!record) return 'waiting_doctor'
  if (recordHasUnreadDoctorReply(record)) return 'doctor_replied'
  if (record.status === 'completed' || record?.doctorHandle?.status === 'completed') return 'completed'
  if (hasDoctorTakenOver(record)) return 'doctor_processing'
  return 'waiting_doctor'
}

function recordProgressLabel(record) {
  return ({
    doctor_replied: '医生已回复',
    waiting_doctor: '待医生处理',
    doctor_processing: '医生处理中',
    completed: '已完成'
  })[recordProgressStage(record)] || '待医生处理'
}

function recordProgressTagType(record) {
  return ({
    doctor_replied: 'success',
    waiting_doctor: 'warning',
    doctor_processing: 'primary',
    completed: 'info'
  })[recordProgressStage(record)] || 'info'
}

function recordProgressHint(record) {
  const stage = recordProgressStage(record)
  if (stage === 'completed' && followUpState(record) === 'none' && isPendingServiceFeedbackRecord(record)) return serviceFeedbackReminderText(record)
  if (stage === 'doctor_replied') return recordMessagePreview(record)
  if (stage === 'doctor_processing') return currentDoctorName(record) ? `当前由 ${currentDoctorName(record)} 跟进处理` : '医生已接手，正在整理处理意见'
  if (stage === 'completed') return followUpLine(record)
  const dispatch = getSmartDispatch(record)
  return dispatch.hint || '已提交问诊，等待医生进一步处理'
}

function latestFollowUp(record) {
  return Array.isArray(record?.doctorFollowUps) && record.doctorFollowUps.length
    ? record.doctorFollowUps[0]
    : null
}

function followUpDueDate(record) {
  const latest = latestFollowUp(record)
  if (latest?.nextFollowUpDate) return latest.nextFollowUpDate
  if (record?.doctorConclusion?.needFollowUp !== 1) return null
  const followUpWithinDays = Number(record?.doctorConclusion?.followUpWithinDays || 0)
  if (!Number.isFinite(followUpWithinDays) || followUpWithinDays <= 0) return null
  const baseTime = record?.doctorConclusion?.updateTime || record?.doctorHandle?.completeTime || record?.updateTime
  if (!baseTime) return null
  const dueDate = new Date(baseTime)
  dueDate.setHours(0, 0, 0, 0)
  dueDate.setDate(dueDate.getDate() + followUpWithinDays)
  return dueDate
}

function followUpState(record) {
  if (!record?.doctorConclusion || record.doctorConclusion.needFollowUp !== 1) return 'none'
  if (latestFollowUp(record)?.needRevisit === 0) return 'done'
  const dueValue = followUpDueDate(record)
  if (!dueValue) return 'pending'
  const dueDate = new Date(dueValue)
  dueDate.setHours(0, 0, 0, 0)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  if (dueDate.getTime() < today.getTime()) return 'overdue'
  if (dueDate.getTime() === today.getTime()) return 'due_today'
  return 'pending'
}

function followUpTagLabel(record) {
  return ({
    overdue: '已逾期',
    due_today: '今日到期',
    pending: '待随访',
    done: '本轮已完成',
    none: '无需随访'
  })[followUpState(record)] || '无需随访'
}

function followUpTagType(record) {
  return ({
    overdue: 'danger',
    due_today: 'warning',
    pending: 'primary',
    done: 'success',
    none: 'info'
  })[followUpState(record)] || 'info'
}

function followUpLine(record) {
  const latest = latestFollowUp(record)
  const dueValue = followUpDueDate(record)
  const state = followUpState(record)
  if (state === 'overdue') return `应于 ${formatDate(dueValue, true)} 前完成随访`
  if (state === 'due_today') return '建议今天完成本轮随访'
  if (state === 'pending') return dueValue ? `计划于 ${formatDate(dueValue, true)} 跟进` : '医生已建议继续随访'
  if (state === 'done') return latest?.createTime ? `最近随访 ${formatDate(latest.createTime)}` : '当前轮次已完成'
  return '当前暂无随访安排'
}

function followUpReminderText(record) {
  const state = followUpState(record)
  if (state === 'overdue') return `当前问诊的随访计划已逾期，建议尽快留意医生回访或主动补充恢复情况。计划时间：${formatDate(followUpDueDate(record), true)}`
  if (state === 'due_today') return '当前问诊的随访计划今天到期，如症状仍有变化，建议今天补充最新情况。'
  if (state === 'pending') return `当前问诊仍处于待随访状态${followUpDueDate(record) ? `，计划时间：${formatDate(followUpDueDate(record), true)}` : ''}`
  return ''
}

function serviceFeedbackTimeText(record) {
  const time = serviceFeedbackBaseTime(record)
  return time ? `完成于 ${formatDate(time)}` : '待提交服务评价'
}

function followUpReminderType(record) {
  const state = followUpState(record)
  if (state === 'overdue') return 'error'
  if (state === 'due_today') return 'warning'
  return 'info'
}

function compareRecordOrder(left, right) {
  const followUpDiff = compareFollowUpPriority(left, right)
  if (followUpDiff !== 0) return followUpDiff
  if (recordHasUnreadDoctorReply(left) !== recordHasUnreadDoctorReply(right)) return recordHasUnreadDoctorReply(left) ? -1 : 1
  return compareDateDesc(left?.createTime, right?.createTime)
}

function compareFollowUpPriority(left, right) {
  const priorityMap = { overdue: 0, due_today: 1, pending: 2, done: 3, none: 4 }
  const stateDiff = compareNumber(priorityMap[followUpState(left)] ?? 9, priorityMap[followUpState(right)] ?? 9)
  if (stateDiff !== 0) return stateDiff
  const dueDiff = compareDateAsc(followUpDueDate(left), followUpDueDate(right))
  if (dueDiff !== 0) return dueDiff
  return 0
}

function compareDateAsc(left, right) {
  const leftTime = toTimestamp(left)
  const rightTime = toTimestamp(right)
  if (leftTime === rightTime) return 0
  if (leftTime === null) return 1
  if (rightTime === null) return -1
  return compareNumber(leftTime, rightTime)
}

function compareDateDesc(left, right) {
  return compareDateAsc(right, left)
}

function compareNumber(left, right) {
  if (left === right) return 0
  return left < right ? -1 : 1
}

function toTimestamp(value) {
  if (!value) return null
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? null : time
}

function recordRowClassName({ row }) {
  if (recordHasUnreadDoctorReply(row)) return 'record-row-unread'
  const state = followUpState(row)
  if (state === 'overdue') return 'record-row-overdue'
  if (state === 'due_today') return 'record-row-due-today'
  return ''
}

function followUpReminderItemClass(record) {
  const state = followUpState(record)
  if (state === 'overdue') return 'is-overdue'
  if (state === 'due_today') return 'is-due-today'
  return ''
}

function getSmartDispatch(record) {
  return normalizeSmartDispatch(record?.smartDispatch)
}

function smartDispatchLine(record) {
  const summary = getSmartDispatch(record)
  if (summary.suggestedDoctorName) return `首推 ${summary.suggestedDoctorName}`
  return smartDispatchHintText(summary)
}

function smartDispatchReason(record) {
  const summary = getSmartDispatch(record)
  if (record?.id === detailRecord.value?.id) {
    return summary.recommendationReason || latestTriageInsight.value?.doctorRecommendationReason || ''
  }
  return summary.recommendationReason || ''
}

function buildLocalMessageSummary(messages) {
  const summary = normalizeMessageSummary(null)
  ensureArray(messages).forEach(item => {
    summary.totalCount += 1
    if (item?.senderType === 'user') summary.userMessageCount += 1
    if (item?.senderType === 'doctor') summary.doctorMessageCount += 1
    if (item?.senderType === 'doctor' && item?.readStatus !== 1) summary.unreadCount += 1
    summary.latestSenderType = item?.senderType || ''
    summary.latestSenderName = item?.senderName || ''
    summary.latestMessageType = item?.messageType || ''
    summary.latestMessagePreview = buildLocalMessagePreview(item)
    summary.latestTime = item?.createTime || null
  })
  return summary
}

function buildLocalMessagePreview(message) {
  const content = `${message?.content || ''}`.trim()
  const attachmentCount = messageAttachments(message).length
  const decoratePreview = (preview) => {
    if (!preview) return preview
    if (isDoctorGuidanceAckMessage(message)) {
      return preview.startsWith('[已确认查看]') ? preview : `[已确认查看] ${preview}`
    }
    if (isCheckResultUpdateMessage(message)) {
      return preview.startsWith('[检查结果]') ? preview : `[检查结果] ${preview}`
    }
    if (isFollowUpUpdateMessage(message)) {
      return preview.startsWith('[恢复更新]') || preview.startsWith('[Recovery Update]') ? preview : `[恢复更新] ${preview}`
    }
    return preview
  }
  const imageSuffix = attachmentCount <= 0
    ? ''
    : attachmentCount === 1 ? '[图片]' : `[图片 x${attachmentCount}]`
  if (content && imageSuffix) return decoratePreview(abbreviateText(`${content} ${imageSuffix}`, 72))
  if (content) return decoratePreview(abbreviateText(content, 72))
  if (imageSuffix) return decoratePreview(imageSuffix)
  return '暂未产生沟通消息'
}

function formatSyncTime(value) {
  if (!value) return '--:--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--:--'
  const now = new Date()
  const sameDay = date.toDateString() === now.toDateString()
  return new Intl.DateTimeFormat('zh-CN', sameDay
    ? {
        hour: '2-digit',
        minute: '2-digit'
      }
    : {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      }).format(date)
}

function downloadArchiveSummary() {
  const recordId = detailRecord.value?.id
  if (!recordId) {
    ElMessage.warning('当前问诊记录还未完成加载')
    return
  }
  download(`/api/user/consultation/record/archive/export?recordId=${recordId}`, 'consultation-archive.txt', () => {
    ElMessage.success('问诊归档摘要已开始下载')
  })
}

async function copyArchiveSummary() {
  const content = `${detailArchiveSummary.value?.plainText || ''}`.trim()
  if (!content) {
    ElMessage.warning('当前暂无可复制的问诊归档摘要')
    return
  }
  const copied = await copyText(content)
  if (copied) {
    ElMessage.success('问诊归档摘要已复制')
  } else {
    ElMessage.warning('当前环境不支持自动复制，请手动复制摘要内容')
  }
}

async function copyText(value) {
  const text = `${value || ''}`.trim()
  if (!text) return false
  if (navigator?.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text)
      return true
    } catch {
      // Fall through to legacy copy below.
    }
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()

  let copied = false
  try {
    copied = document.execCommand('copy')
  } catch {
    copied = false
  }
  document.body.removeChild(textarea)
  return copied
}

function syncRecordMessageSummary(recordId, summary) {
  const nextSummary = normalizeMessageSummary(summary)
  records.value = records.value.map(item => item.id === recordId
    ? { ...item, messageSummary: nextSummary }
    : item)
  if (detailRecord.value?.id === recordId) {
    detailRecord.value = {
      ...detailRecord.value,
      messageSummary: nextSummary
    }
  }
}

function syncRecordSnapshot(record) {
  if (!record?.id) return
  records.value = records.value.map(item => item.id === record.id
    ? {
        ...item,
        status: record.status,
        triageLevelName: record.triageLevelName,
        triageLevelColor: record.triageLevelColor,
        triageActionType: record.triageActionType,
        departmentName: record.departmentName,
        doctorHandle: record.doctorHandle,
        doctorConclusion: record.doctorConclusion,
        doctorFollowUps: record.doctorFollowUps,
        smartDispatch: normalizeSmartDispatch(record.smartDispatch),
        messageSummary: normalizeMessageSummary(record.messageSummary),
        payment: normalizePayment(record.payment)
      }
    : item)
}

function resetMessageDraft() {
  messageDraft.content = ''
  messageDraft.attachments = []
  messageDraft.sceneType = ''
}

function resetFollowUpUpdateForm() {
  followUpUpdateForm.recoveryStatus = 'stable'
  followUpUpdateForm.progressNote = ''
  followUpUpdateForm.helpRequest = ''
}

function resetCheckResultUpdateForm() {
  checkResultUpdateForm.resultType = 'lab'
  checkResultUpdateForm.resultSummary = ''
  checkResultUpdateForm.doctorQuestion = ''
}

function messageAttachments(message) {
  return Array.isArray(message?.attachments) && message.attachments.length
    ? message.attachments
    : parseJsonArray(message?.attachmentsJson)
}

function isUserMessage(message) {
  return message?.senderType === 'user'
}

function isFollowUpUpdateMessage(message) {
  return `${message?.messageType || ''}`.trim().toLowerCase() === 'followup_update'
}

function isCheckResultUpdateMessage(message) {
  return `${message?.messageType || ''}`.trim().toLowerCase() === 'check_result_update'
}

function isDoctorGuidanceAckMessage(message) {
  return `${message?.messageType || ''}`.trim().toLowerCase() === 'doctor_guidance_ack'
}


function messageSenderLabel(message) {
  if (message?.senderType === 'user') return message.senderName || detailRecord.value?.patientName || '患者'
  const roleName = message?.senderRoleName ? ` · ${message.senderRoleName}` : ''
  return `${message?.senderName || '医生'}${roleName}`
}

function beforeMessageUpload(file) {
  const isImage = `${file.type || ''}`.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 <= 5
  if (!isImage) {
    ElMessage.error('当前阶段仅支持上传图片资料')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('上传图片不能超过 5MB')
    return false
  }
  if (messageDraft.attachments.length >= 6) {
    ElMessage.warning('单次最多上传 6 张图片')
    return false
  }
  return true
}

function handleMessageUploadSuccess(response) {
  if (response?.code !== 200) {
    ElMessage.error(response?.message || '消息图片上传失败')
    return
  }
  if (!response?.data || messageDraft.attachments.includes(response.data) || messageDraft.attachments.length >= 6) return
  messageDraft.attachments = [...messageDraft.attachments, response.data]
  ElMessage.success('沟通图片上传成功')
}

function removeMessageAttachment(index) {
  messageDraft.attachments.splice(index, 1)
}

function sendConsultationMessage() {
  const recordId = detailRecord.value?.id
  if (!recordId) return
  const content = `${messageDraft.content || ''}`.trim()
  if (!content && !messageDraft.attachments.length) {
    ElMessage.warning('请先输入消息内容或上传图片')
    return
  }
  messageSending.value = true
  post('/api/user/consultation/message/send', {
    recordId,
    content,
    attachments: messageDraft.attachments,
    sceneType: messageDraft.sceneType || null
  }, () => {
    messageSending.value = false
    resetMessageDraft()
    ElMessage.success('消息已发送')
    loadConsultationMessages(recordId, { preserveScroll: false })
  }, (message) => {
    messageSending.value = false
    ElMessage.warning(message || '问诊消息发送失败')
  })
}

function followUpRecoveryStatusLabel(value) {
  return followUpRecoveryStatusOptions.find(item => item.value === value)?.label || 'Other'
}

function checkResultTypeLabel(value) {
  return checkResultTypeOptions.find(item => item.value === value)?.label || '其他结果'
}

function buildFollowUpUpdateContent() {
  const segments = [`Recovery status: ${followUpRecoveryStatusLabel(followUpUpdateForm.recoveryStatus)}`]
  const progressNote = `${followUpUpdateForm.progressNote || ''}`.trim()
  const helpRequest = `${followUpUpdateForm.helpRequest || ''}`.trim()
  if (progressNote) segments.push(`Current changes: ${progressNote}`)
  if (helpRequest) segments.push(`Still need help: ${helpRequest}`)
  return segments.filter(Boolean).join('\n')
}

function buildCheckResultUpdateContent() {
  const resultSummary = `${checkResultUpdateForm.resultSummary || ''}`.trim()
  const doctorQuestion = `${checkResultUpdateForm.doctorQuestion || ''}`.trim()
  if (!resultSummary && !doctorQuestion) return ''
  const segments = [`检查类型：${checkResultTypeLabel(checkResultUpdateForm.resultType)}`]
  if (resultSummary) segments.push(`结果摘要：${resultSummary}`)
  if (doctorQuestion) segments.push(`希望医生帮助判断：${doctorQuestion}`)
  return segments.join('\n')
}

function applyFollowUpUpdateToMessageDraft() {
  if (!canSubmitFollowUpUpdate.value) {
    ElMessage.warning('No pending follow-up for this consultation right now.')
    return
  }
  const content = buildFollowUpUpdateContent()
  if (!content) {
    ElMessage.warning('Please complete the recovery update first.')
    return
  }
  const current = `${messageDraft.content || ''}`.trim()
  messageDraft.content = current && current !== content ? `${current}\n\n${content}` : content
  messageDraft.sceneType = 'followup_update'
  ElMessage.success('Recovery update was added to the message box.')
  focusDetailActionSection('conversation', detailRecord.value?.id || null)
}

function submitFollowUpUpdate() {
  const recordId = detailRecord.value?.id
  if (!recordId || !canSubmitFollowUpUpdate.value) {
    ElMessage.warning('No pending follow-up for this consultation right now.')
    return
  }
  const content = buildFollowUpUpdateContent()
  if (!content) {
    ElMessage.warning('Please complete the recovery update first.')
    return
  }
  followUpUpdateSubmitting.value = true
  post('/api/user/consultation/message/send', {
    recordId,
    content,
    sceneType: 'followup_update'
  }, () => {
    followUpUpdateSubmitting.value = false
    resetFollowUpUpdateForm()
    ElMessage.success('Recovery update was sent to the doctor.')
    loadConsultationMessages(recordId, { preserveScroll: false })
    refreshRecordDetail(recordId)
  }, (message) => {
    followUpUpdateSubmitting.value = false
    ElMessage.warning(message || 'Failed to send the recovery update.')
  })
}

function buildDoctorGuidanceAckContent() {
  if (detailRecord.value?.doctorConclusion?.needFollowUp === 1) {
    return '已查看本轮医生结论与随访建议，我会按当前计划继续观察并及时补充恢复情况。'
  }
  return '已查看本轮医生结论与后续建议，如有新的变化我会继续补充。'
}

function submitDoctorGuidanceAck() {
  const recordId = detailRecord.value?.id
  if (!recordId || !canSubmitDoctorGuidanceAck.value) {
    ElMessage.warning(doctorGuidanceAckHint.value || '当前没有需要确认查看的医生建议。')
    return
  }
  doctorGuidanceAckSubmitting.value = true
  post('/api/user/consultation/message/send', {
    recordId,
    content: buildDoctorGuidanceAckContent(),
    sceneType: 'doctor_guidance_ack'
  }, () => {
    doctorGuidanceAckSubmitting.value = false
    ElMessage.success('已确认查看医生结论与后续建议')
    loadConsultationMessages(recordId, { preserveScroll: false })
    refreshRecordDetail(recordId)
  }, (message) => {
    doctorGuidanceAckSubmitting.value = false
    ElMessage.warning(message || '查看确认提交失败')
  })
}

function applyCheckResultUpdateToMessageDraft() {
  if (!canSubmitCheckResultUpdate.value) {
    ElMessage.warning('The current consultation is not ready for structured check-result updates yet.')
    return
  }
  const content = buildCheckResultUpdateContent()
  if (!content) {
    ElMessage.warning('Please complete the check-result update first.')
    return
  }
  const current = `${messageDraft.content || ''}`.trim()
  messageDraft.content = current && current !== content ? `${current}\n\n${content}` : content
  messageDraft.sceneType = 'check_result_update'
  ElMessage.success('Check-result update was added to the message box.')
  focusDetailActionSection('conversation', detailRecord.value?.id || null)
}

function submitCheckResultUpdate() {
  const recordId = detailRecord.value?.id
  if (!recordId || !canSubmitCheckResultUpdate.value) {
    ElMessage.warning('The current consultation is not ready for structured check-result updates yet.')
    return
  }
  const content = buildCheckResultUpdateContent()
  if (!content) {
    ElMessage.warning('Please complete the check-result update first.')
    return
  }
  checkResultUpdateSubmitting.value = true
  post('/api/user/consultation/message/send', {
    recordId,
    content,
    sceneType: 'check_result_update'
  }, () => {
    checkResultUpdateSubmitting.value = false
    resetCheckResultUpdateForm()
    ElMessage.success('Check-result update was sent to the doctor.')
    loadConsultationMessages(recordId, { preserveScroll: false })
    refreshRecordDetail(recordId)
  }, (message) => {
    checkResultUpdateSubmitting.value = false
    ElMessage.warning(message || 'Failed to send the check-result update.')
  })
}

function applyFeedbackForm(feedback) {
  feedbackForm.userScore = feedback?.userScore || 5
  feedbackForm.isAdopted = feedback?.isAdopted ?? 1
  feedbackForm.feedbackText = feedback?.feedbackText || ''
  feedbackForm.manualCorrectDepartmentId = feedback?.manualCorrectDepartmentId || null
  feedbackForm.manualCorrectDoctorId = feedback?.manualCorrectDoctorId || null
}

function applyServiceFeedbackForm(feedback) {
  serviceFeedbackForm.serviceScore = feedback?.serviceScore || 5
  serviceFeedbackForm.isResolved = feedback?.isResolved ?? 1
  serviceFeedbackForm.feedbackText = feedback?.feedbackText || ''
}

function submitServiceFeedback() {
  if (!detailRecord.value?.id) return
  if (!canSubmitServiceFeedback.value) {
    ElMessage.warning('当前问诊尚未完成医生处理，暂不可提交服务评价')
    return
  }
  if (!serviceFeedbackForm.serviceScore) {
    ElMessage.warning('请先选择服务评分')
    return
  }
  serviceFeedbackSubmitting.value = true
  post('/api/user/consultation/service-feedback/submit', {
    recordId: detailRecord.value.id,
    serviceScore: serviceFeedbackForm.serviceScore,
    isResolved: serviceFeedbackForm.isResolved,
    feedbackText: `${serviceFeedbackForm.feedbackText || ''}`.trim()
  }, () => {
    serviceFeedbackSubmitting.value = false
    ElMessage.success('问诊服务评价已保存')
    refreshRecordDetail(detailRecord.value.id)
  }, (message) => {
    serviceFeedbackSubmitting.value = false
    ElMessage.warning(message || '问诊服务评价保存失败')
  })
}

function submitFeedback() {
  if (!detailRecord.value?.id) return
  if (!feedbackForm.userScore) {
    ElMessage.warning('请先选择评分')
    return
  }
  feedbackSubmitting.value = true
  post('/api/user/consultation/feedback/submit', {
    recordId: detailRecord.value.id,
    userScore: feedbackForm.userScore,
    isAdopted: feedbackForm.isAdopted,
    feedbackText: `${feedbackForm.feedbackText || ''}`.trim(),
    manualCorrectDepartmentId: feedbackForm.isAdopted === 0 ? feedbackForm.manualCorrectDepartmentId : null,
    manualCorrectDoctorId: feedbackForm.isAdopted === 0 ? feedbackForm.manualCorrectDoctorId : null
  }, () => {
    feedbackSubmitting.value = false
    ElMessage.success('导诊反馈已保存')
    get(`/api/user/consultation/record/detail?recordId=${detailRecord.value.id}`, (data) => {
      applyDetailRecordPayload(data)
    })
  }, (message) => {
    feedbackSubmitting.value = false
    ElMessage.warning(message || '导诊反馈保存失败')
  })
}

function triageSessionStatusLabel(value) {
  return {
    completed: '已完成',
    in_progress: '进行中',
    closed: '已关闭'
  }[value] || value || '-'
}

function messageRoleLabel(value) {
  return {
    user: '用户',
    system: '系统',
    rule_engine: '规则引擎',
    assistant: 'AI 导诊'
  }[value] || value || '-'
}

function messageTypeLabel(value) {
  if (value === 'followup_update') return '恢复更新'
  if (value === 'check_result_update') return '检查结果补充'
  if (value === 'doctor_guidance_ack') return '已确认查看'
  return {
    intake_summary: '问诊摘要',
    health_summary: '健康摘要',
    triage_result: '分诊结果',
    rule_summary: '规则摘要',
    rule_hit: '命中详情',
    ai_triage_summary: 'AI 导诊建议',
    ai_followup_questions: 'AI 建议补充',
    ai_user_followup: '患者补充',
    ai_chat_reply: 'AI 导诊回复'
  }[value] || value || '-'
}

function feedbackAdoptLabel(value) {
  return value === 1 ? '已采纳' : '未采纳'
}

function serviceFeedbackResolvedLabel(value) {
  return value === 1 ? '本次问题已解决' : '仍需继续处理'
}

function journeyToneFromFollowUpState(value) {
  if (value === 'overdue') return 'danger'
  if (value === 'due_today') return 'warning'
  if (value === 'done') return 'success'
  return 'info'
}

function parseJsonArray(value) {
  if (!value) return []
  try {
    const result = JSON.parse(value)
    return Array.isArray(result) ? result : []
  } catch {
    return []
  }
}

function parseDoctorCandidates(value) {
  return parseJsonArray(value).filter(item => item && typeof item === 'object')
}

function doctorRecommendationScoreText(item) {
  const number = Number(item?.recommendationScore)
  return Number.isFinite(number) && number > 0 ? `优先分 ${number}` : ''
}

function buildTimelineItems(record) {
  if (!record) return []
  const items = []
  items.push(createTimelineItem('submitted', '已提交问诊', record.createTime, `已提交 ${record.categoryName || '当前'} 问诊，等待系统分诊和医生处理。`, 'primary'))
  if (record.triageResult?.createTime || record.triageSession?.createTime || record.triageLevelName) {
    items.push(createTimelineItem(
      'triage',
      '已完成导诊',
      record.triageResult?.createTime || record.triageSession?.createTime || record.updateTime,
      `${record.triageLevelName || record.triageResult?.triageLevelName || '系统已生成导诊结论'}，${triageActionLabel(record.triageActionType)}`,
      'info'
    ))
  }
  if (record.smartDispatch?.status) {
    items.push(createTimelineItem(
      'dispatch',
      '智能分配中',
      record.triageResult?.updateTime || record.updateTime,
      smartDispatchHintText(record.smartDispatch),
      record.smartDispatch.status.includes('claimed') ? 'success' : 'info'
    ))
  }
  if (hasDoctorClaimed(record)) {
    items.push(createTimelineItem(
      'doctor_claim',
      '医生已认领',
      record.doctorAssignment?.claimTime || record.doctorAssignment?.updateTime || record.updateTime,
      claimedDoctorName(record) ? `${claimedDoctorName(record)} 已接手当前问诊，后续会继续通过消息跟进。` : '当前问诊已被医生接手，可继续通过消息补充情况。',
      'success'
    ))
  }
  if (record.doctorHandle?.receiveTime || record.doctorHandle?.doctorName) {
    items.push(createTimelineItem(
      'doctor_receive',
      '医生已接手',
      record.doctorHandle.receiveTime || record.doctorHandle.updateTime,
      record.doctorHandle.doctorName ? `${record.doctorHandle.doctorName} 已开始跟进处理。` : '医生已接手当前问诊。',
      'success'
    ))
  }
  if (detailMessageSummary.value.totalCount > 0) {
    items.push(createTimelineItem(
      'conversation',
      '医患沟通更新',
      detailMessageSummary.value.latestTime,
      detailMessageSummary.value.unreadCount > 0
        ? `医生有 ${detailMessageSummary.value.unreadCount} 条新回复，建议及时查看。`
        : recordMessageStatus(record),
      detailMessageSummary.value.unreadCount > 0 ? 'warning' : 'info'
    ))
  }
  if (record.doctorHandle?.completeTime || record.doctorConclusion) {
    items.push(createTimelineItem(
      'doctor_complete',
      '医生处理完成',
      record.doctorHandle?.completeTime || record.doctorConclusion?.updateTime,
      record.doctorHandle?.summary || doctorFollowUpText(record.doctorConclusion) || '医生已完成本轮处理。',
      'success'
    ))
  }
  if (hasPatientAcknowledgedGuidance.value && latestPatientGuidanceAck.value) {
    items.push(createTimelineItem(
      'guidance_ack',
      '已确认查看医生建议',
      latestPatientGuidanceAck.value.createTime,
      latestPatientGuidanceAck.value.content || '患者已确认查看本轮医生结论与后续建议。',
      'success'
    ))
  }
  if (followUpState(record) !== 'none') {
    items.push(createTimelineItem(
      'follow_up',
      followUpTagLabel(record),
      latestFollowUp(record)?.createTime || followUpDueDate(record) || record.doctorConclusion?.updateTime,
      followUpReminderText(record) || followUpLine(record),
      followUpState(record) === 'overdue' ? 'danger' : followUpState(record) === 'due_today' ? 'warning' : 'info'
    ))
  }
  return items.filter(item => item.time || item.description)
}

function createTimelineItem(key, title, time, description, tone = 'info') {
  return {
    key,
    title,
    time,
    tone,
    description,
    timeText: formatDate(time)
  }
}

function formatConfidence(value) {
  const number = Number(value)
  if (Number.isNaN(number) || number <= 0) return '-'
  return `${Math.round(number * 100)}%`
}

function parseMultiValue(value) {
  return parseJsonArray(value)
}

function displayAnswer(answer) {
  if (answer.fieldType === 'switch') return answer.fieldValue === '1' ? '是' : '否'
  return answer.fieldValue || '-'
}

function abbreviateText(value, maxLength = 80) {
  const text = `${value || ''}`.trim()
  if (!text || text.length <= maxLength) return text
  return `${text.slice(0, Math.max(maxLength - 3, 0))}...`
}

function trimText(value) {
  return `${value ?? ''}`.trim()
}

function formatDate(value, onlyDate = false) {
  if (!value) return '-'
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('zh-CN', onlyDate
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
      }).format(date)
}

watch(detailVisible, (value) => {
  if (!value) {
    stopConversationPolling()
    clearConversationNewMessageState()
    resetConversationSyncState()
    detailRecord.value = null
    triageAiSending.value = false
    consultationMessages.value = []
    messageLoading.value = false
    messageSending.value = false
    doctorGuidanceAckSubmitting.value = false
    followUpUpdateSubmitting.value = false
    checkResultUpdateSubmitting.value = false
    clearFocusedDetailSection()
    resetTriageAiDraft()
    resetMessageDraft()
    resetFollowUpUpdateForm()
    resetCheckResultUpdateForm()
    if (route.query.id || route.query.action) {
      router.replace({ path: route.path, query: buildRecordRouteQuery({ includeId: false, action: '' }) })
    }
  }
})

onBeforeUnmount(() => {
  stopConversationPolling()
  if (typeof document !== 'undefined') {
    document.removeEventListener('visibilitychange', handleConversationVisibilityChange)
  }
  clearFocusedDetailSection()
})

onMounted(() => {
  loadData()
  if (typeof document !== 'undefined') {
    document.addEventListener('visibilitychange', handleConversationVisibilityChange)
  }
})
</script>

<style scoped>
.consultation-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stat-card,
.side-card,
.form-card,
.history-card,
.field-card,
.detail-answer-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 18px;
}

.stat-card {
  padding: 22px 24px;
}

.reminder-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.reminder-card {
  padding: 22px;
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.reminder-card.reminder-card-followup {
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.06), rgba(255, 255, 255, 0.96));
}

.reminder-head,
.reminder-item-head,
.reminder-item-meta,
.reminder-foot,
.reminder-tags {
  display: flex;
  gap: 12px;
}

.reminder-head,
.reminder-item-head,
.reminder-item-meta {
  justify-content: space-between;
  align-items: flex-start;
}

.reminder-head {
  margin-bottom: 14px;
}

.reminder-head h3 {
  margin: 6px 0 0;
}

.reminder-head p,
.reminder-item p,
.reminder-item-head span,
.reminder-item-meta span {
  color: var(--app-muted);
}

.reminder-head p,
.reminder-item p {
  margin: 8px 0 0;
  line-height: 1.7;
}

.reminder-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reminder-item {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(17, 70, 77, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.reminder-item:hover {
  transform: translateY(-1px);
  border-color: rgba(15, 102, 101, 0.26);
  box-shadow: 0 12px 24px rgba(19, 73, 80, 0.08);
}

.reminder-item.is-overdue {
  border-color: rgba(214, 95, 80, 0.24);
  background: linear-gradient(180deg, rgba(214, 95, 80, 0.08), rgba(255, 255, 255, 0.98));
}

.reminder-item.is-due-today {
  border-color: rgba(210, 155, 47, 0.24);
  background: linear-gradient(180deg, rgba(210, 155, 47, 0.08), rgba(255, 255, 255, 0.98));
}

.reminder-item strong {
  color: #31474d;
}

.reminder-item-meta {
  margin-top: 10px;
  flex-wrap: wrap;
}

.reminder-foot,
.reminder-tags {
  margin-top: 14px;
  flex-wrap: wrap;
  align-items: center;
}

.stat-card span,
.panel-kicker,
.field-head span,
.field-head small {
  color: var(--app-muted);
}

.stat-card strong {
  display: block;
  margin-top: 14px;
  font-size: 30px;
}

.entry-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
}

.side-card,
.form-card,
.history-card {
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

.panel-head h3,
.detail-answer-card strong {
  margin: 6px 0 0;
}

.panel-head p {
  margin: 6px 0 0;
  color: var(--app-muted);
}

.history-toolbar,
.timeline-head {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.history-toolbar {
  margin-bottom: 16px;
  align-items: center;
}

.timeline-head {
  justify-content: space-between;
  align-items: flex-start;
}

.compact {
  margin-top: 20px;
}

.category-list,
.field-list,
.detail-answers {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.category-card {
  padding: 18px;
  border: 1px solid rgba(17, 70, 77, 0.08);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.category-card.active {
  border-color: rgba(15, 102, 101, 0.42);
  box-shadow: 0 20px 36px rgba(15, 102, 101, 0.12);
  transform: translateY(-2px);
}

.category-card strong,
.patient-card strong {
  display: block;
}

.category-card p,
.health-summary p,
.detail-summary p {
  margin: 10px 0 0;
  color: #637b84;
  line-height: 1.75;
}

.category-dept,
.category-meta,
.patient-meta,
.template-meta,
.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.category-meta,
.patient-meta,
.template-meta {
  margin-top: 12px;
}

.category-meta span,
.patient-meta span,
.template-meta span,
.chip-row span {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #48656d;
  font-size: 12px;
}

.chip-row.danger span {
  background: rgba(214, 95, 80, 0.12);
  color: #9f4336;
}

.patient-card {
  margin-top: 16px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(17, 70, 77, 0.08);
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

.field-head strong {
  display: block;
}

.upload-row {
  align-items: center;
  flex-wrap: wrap;
}

.upload-tip {
  color: var(--app-muted);
  font-size: 13px;
}

.upload-preview,
.detail-meta {
  margin-top: 16px;
}

.upload-preview img,
.detail-answer-value img {
  width: 220px;
  height: 150px;
  object-fit: cover;
  border-radius: 18px;
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.detail-meta article {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.detail-meta span {
  color: var(--app-muted);
  font-size: 12px;
}

.detail-meta strong {
  display: block;
  margin-top: 10px;
}

.detail-summary {
  margin: 18px 0;
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.timeline-panel {
  margin-bottom: 18px;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr);
  gap: 14px;
  align-items: flex-start;
}

.timeline-dot {
  position: relative;
  width: 12px;
  height: 12px;
  margin: 6px 0 0 4px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.3);
}

.timeline-dot::after {
  content: '';
  position: absolute;
  top: 16px;
  left: 50%;
  width: 2px;
  height: calc(100% + 16px);
  transform: translateX(-50%);
  background: rgba(19, 73, 80, 0.1);
}

.timeline-item:last-child .timeline-dot::after {
  display: none;
}

.timeline-dot.is-primary,
.timeline-dot.is-info {
  background: rgba(15, 102, 101, 0.32);
}

.timeline-dot.is-success {
  background: rgba(77, 168, 132, 0.36);
}

.timeline-dot.is-warning {
  background: rgba(210, 155, 47, 0.36);
}

.timeline-dot.is-danger {
  background: rgba(214, 95, 80, 0.42);
}

.timeline-content {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.timeline-content strong {
  display: block;
}

.timeline-content span,
.timeline-content p {
  color: var(--app-muted);
}

.timeline-content p {
  margin: 8px 0 0;
  line-height: 1.7;
}

.triage-panel,
.doctor-card,
.result-panel,
.feedback-panel,
.session-panel,
.session-message-card,
.candidate-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.triage-panel,
.doctor-recommend,
.result-panel,
.feedback-panel,
.session-panel {
  margin-bottom: 18px;
}

.triage-grid,
.doctor-list,
.candidate-list {
  display: grid;
  gap: 14px;
}

.triage-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.triage-grid article span,
.doctor-recommend-head span,
.doctor-copy span,
.doctor-schedule {
  color: var(--app-muted);
}

.triage-grid article strong {
  display: block;
  margin-top: 10px;
}

.triage-strong {
  margin-top: 12px;
}

.triage-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid rgba(15, 102, 101, 0.18);
  background: rgba(15, 102, 101, 0.08);
  color: #0f6665;
  font-size: 12px;
  font-weight: 600;
}

.doctor-recommend-head,
.doctor-top {
  display: flex;
  gap: 14px;
}

.doctor-recommend-head {
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.record-message-cell,
.conversation-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.record-message-cell strong {
  color: #31474d;
}

.record-message-cell span,
.conversation-summary span {
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.conversation-sync-text {
  color: #0f6665;
  font-weight: 600;
}

.conversation-sync-text.is-failed {
  color: #b94c3b;
}

.doctor-list {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.candidate-list {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 14px;
}

.candidate-card strong {
  display: block;
}

.candidate-card span,
.result-copy,
.feedback-summary span {
  color: var(--app-muted);
}

.result-copy {
  margin: 0 0 14px;
  line-height: 1.8;
}

.compare-panel {
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.08), rgba(255, 255, 255, 0.78));
}

.journey-panel {
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.1), rgba(255, 255, 255, 0.94));
}

.journey-lead {
  color: #31474d;
}

.journey-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.journey-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.journey-card-head,
.journey-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.journey-card-head strong {
  color: #31474d;
}

.journey-card p {
  margin: 0;
  color: var(--app-muted);
  line-height: 1.8;
}

.journey-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(19, 73, 80, 0.08);
  color: #27646d;
  font-size: 12px;
  white-space: nowrap;
}

.journey-status.is-success {
  background: rgba(77, 168, 132, 0.16);
  color: #1f6f4f;
}

.journey-status.is-warning {
  background: rgba(210, 155, 47, 0.14);
  color: #8f6514;
}

.journey-status.is-danger {
  background: rgba(214, 95, 80, 0.14);
  color: #9f4336;
}

.compare-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.compare-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.compare-card strong,
.compare-item strong {
  display: block;
  margin-bottom: 8px;
}

.compare-kv {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.compare-kv + .compare-kv {
  margin-top: 10px;
}

.compare-kv label,
.compare-item p {
  color: var(--app-muted);
}

.compare-kv span {
  text-align: right;
  color: #31474d;
}

.compare-copy {
  margin-top: 12px;
}

.compare-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}

.compare-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.compare-item p {
  margin: 0;
  line-height: 1.7;
}

.compare-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(19, 73, 80, 0.08);
  color: #27646d;
  font-size: 12px;
}

.compare-badge.is-match {
  background: rgba(77, 168, 132, 0.16);
  color: #1f6f4f;
}

.compare-badge.is-mismatch {
  background: rgba(214, 95, 80, 0.14);
  color: #9f4336;
}

.compare-badge.is-partial,
.compare-badge.is-pending {
  background: rgba(210, 155, 47, 0.14);
  color: #8f6514;
}

.focus-action-panel {
  border-color: rgba(15, 102, 101, 0.28);
  box-shadow: 0 0 0 3px rgba(15, 102, 101, 0.12);
}

.feedback-summary,
.feedback-form,
.feedback-actions,
.archive-toolbar,
.archive-metrics {
  display: flex;
  gap: 12px;
}

.feedback-summary,
.feedback-form,
.archive-metrics {
  flex-wrap: wrap;
}

.feedback-summary {
  margin-bottom: 14px;
}

.feedback-summary span {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  font-size: 12px;
}

.feedback-form :deep(.el-textarea) {
  width: 100%;
}

.archive-panel {
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.08), rgba(255, 255, 255, 0.92));
}

.archive-toolbar {
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.archive-updated {
  color: var(--app-muted);
  font-size: 13px;
}

.archive-metrics {
  margin-bottom: 14px;
}

.archive-metrics span {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #48656d;
  font-size: 12px;
}

.archive-lead {
  margin-bottom: 14px;
  color: #31474d;
}

.archive-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.summary-panel,
.archive-card,
.archive-next-item {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.summary-panel {
  margin-top: 14px;
}

.summary-panel p,
.archive-card p,
.archive-next-item p {
  margin: 10px 0 0;
  color: var(--app-muted);
  line-height: 1.8;
}

.archive-card strong {
  display: block;
}

.archive-next-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.archive-next-item {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 12px;
  align-items: flex-start;
}

.archive-next-item span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.12);
  color: #0f6665;
  font-size: 13px;
  font-weight: 600;
}

.archive-next-item p {
  margin: 0;
}

.conversation-alert {
  margin-bottom: 14px;
}

.conversation-board {
  margin-bottom: 14px;
}

.conversation-jump-bar {
  margin-bottom: 14px;
  display: flex;
  justify-content: center;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;
}

.conversation-card {
  max-width: 82%;
  padding: 14px 16px;
  border-radius: 20px;
  border: 1px solid rgba(15, 102, 101, 0.1);
  background: rgba(15, 102, 101, 0.05);
}

.conversation-card.mine {
  align-self: flex-end;
  background: rgba(15, 102, 101, 0.12);
}

.conversation-meta,
.conversation-toolbar,
.conversation-attachment-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.conversation-meta span,
.conversation-tip,
.conversation-attachment-actions span {
  color: var(--app-muted);
  font-size: 13px;
}

.conversation-content {
  margin: 10px 0 0;
  line-height: 1.8;
  color: #41575d;
  white-space: pre-wrap;
}

.conversation-image-list,
.conversation-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}

.conversation-composer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

:deep(.focus-action-input .el-textarea__inner) {
  border-color: rgba(15, 102, 101, 0.28);
  box-shadow: 0 0 0 3px rgba(15, 102, 101, 0.12);
}

.conversation-attachment-item {
  width: 140px;
  padding: 10px;
  border-radius: 18px;
  background: rgba(15, 102, 101, 0.05);
}

.conversation-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 16px;
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.session-meta,
.session-message-head {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.session-meta {
  margin-bottom: 14px;
}

.session-meta span {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #48656d;
  font-size: 12px;
}

.session-message-list {
  display: grid;
  gap: 14px;
}

.triage-ai-composer {
  margin-top: 14px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(15, 102, 101, 0.05);
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.session-message-head {
  justify-content: space-between;
  align-items: flex-start;
}

.session-message-head strong {
  display: block;
}

.session-message-head span,
.session-message-card p {
  color: var(--app-muted);
}

.session-message-card p {
  margin: 12px 0 0;
  line-height: 1.8;
}

.session-message-insight {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(15, 102, 101, 0.06);
  border: 1px solid rgba(15, 102, 101, 0.1);
}

.session-message-insight-meta,
.session-message-insight-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.session-message-insight-meta span,
.session-message-insight-tags span {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.1);
  color: #48656d;
  font-size: 12px;
}

.session-message-insight-copy {
  margin: 10px 0 0;
  line-height: 1.7;
  color: #48656d;
}

.session-message-insight-tags {
  margin-top: 10px;
}

.session-message-insight-tags.danger span {
  background: rgba(214, 95, 80, 0.12);
  color: #9f4336;
}

.triage-ai-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  margin-top: 12px;
}

.triage-ai-tip {
  color: var(--app-muted);
  font-size: 13px;
}

.doctor-avatar {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  object-fit: cover;
  border: 1px solid rgba(17, 70, 77, 0.08);
  background: rgba(15, 102, 101, 0.08);
}

.doctor-avatar-fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #0f6665;
  font-size: 22px;
  font-weight: 700;
}

.doctor-copy strong {
  display: block;
}

.doctor-copy-head {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
}

.recommend-score {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.12);
  color: #0f6665;
  font-size: 12px;
  white-space: nowrap;
}

.chip-row.is-accent span {
  background: rgba(77, 168, 132, 0.14);
  color: #1f6f4f;
}

.chip-row.is-subtle span {
  background: rgba(19, 73, 80, 0.06);
  color: #48656d;
}

.doctor-recommend-copy {
  margin-top: 10px;
}
.doctor-text,
.doctor-schedule {
  margin: 12px 0 0;
  line-height: 1.75;
}

.detail-answer-card {
  padding: 16px 18px;
}

.detail-answer-value {
  margin-top: 10px;
  color: #405f68;
  line-height: 1.8;
}

:deep(.el-table .record-row-unread td.el-table__cell) {
  background: rgba(77, 168, 132, 0.08);
}

:deep(.el-table .record-row-overdue td.el-table__cell) {
  background: rgba(214, 95, 80, 0.09);
}

:deep(.el-table .record-row-due-today td.el-table__cell) {
  background: rgba(210, 155, 47, 0.09);
}

@media (max-width: 1180px) {
  .stat-grid,
  .reminder-grid,
  .entry-layout,
  .detail-meta,
  .journey-grid,
  .triage-grid,
  .doctor-list,
  .candidate-list,
  .compare-grid,
  .archive-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .panel-head,
  .patient-top,
  .reminder-head,
  .reminder-item-head,
  .reminder-item-meta,
  .reminder-foot,
  .reminder-tags,
  .history-toolbar,
  .timeline-head,
  .doctor-recommend-head,
  .doctor-top,
  .session-message-head,
  .triage-ai-toolbar,
  .compare-item,
  .compare-kv,
  .archive-toolbar,
  .journey-card-head,
  .journey-actions,
  .feedback-actions,
  .conversation-meta,
  .conversation-toolbar,
  .conversation-attachment-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .panel-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .compare-kv span {
    text-align: left;
  }

  .conversation-card {
    max-width: 100%;
  }
}
</style>
