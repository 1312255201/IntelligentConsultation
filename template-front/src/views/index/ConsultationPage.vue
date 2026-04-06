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
        <span>今日新增</span>
        <strong>{{ todayCount }}</strong>
      </article>
      <article class="stat-card">
        <span>医生新回复</span>
        <strong>{{ unreadDoctorReplyCount }}</strong>
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

    <section class="history-card">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">History</span>
          <h3>问诊记录</h3>
        </div>
        <el-button @click="loadRecords">刷新记录</el-button>
      </div>

      <el-table :data="records" v-loading="historyLoading" border>
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
          </div>

          <div class="detail-summary">
            <p><strong>标题：</strong>{{ detailRecord.title }}</p>
            <p><strong>主诉摘要：</strong>{{ detailRecord.chiefComplaint || '未自动提取' }}</p>
            <p><strong>健康摘要：</strong>{{ detailRecord.healthSummary || '未关联健康档案摘要' }}</p>
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
              <div v-if="consultationMessages.length" class="conversation-list">
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
            <div class="conversation-composer">
              <el-input
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
                <strong>{{ item.name }}</strong>
                <span>{{ item.title || '医生' }}</span>
              </article>
            </div>
          </div>

          <div v-if="detailRecord.doctorHandle" class="result-panel">
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

          <div v-if="detailRecord.doctorConclusion" class="result-panel">
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

          <div class="result-panel">
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
                    <strong>{{ doctor.name }}</strong>
                    <span>{{ doctor.title || '医生' }}</span>
                  </div>
                </div>
                <p class="doctor-text">{{ doctor.expertise || doctor.introduction || '暂未配置更多医生介绍信息' }}</p>
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
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { authHeader, backendBaseUrl, get, post, resolveImagePath } from '@/net'
import { comparisonStatusClass, comparisonStatusLabel } from '@/triage/comparison'
import { normalizeSmartDispatch, smartDispatchHintText, smartDispatchStatusLabel, smartDispatchTagType } from '@/triage/dispatch'
import { resolveTriageMessageInsight } from '@/triage/insight'

const router = useRouter()
const categories = ref([])
const patients = ref([])
const histories = ref([])
const records = ref([])
const template = ref(null)
const activeCategoryId = ref(null)
const selectedPatientId = ref(null)
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
const feedbackOptions = ref({ departments: [], doctors: [] })
const feedbackSubmitting = ref(false)
const formData = reactive({})
const triageAiDraft = reactive({ content: '' })
const messageDraft = reactive({ content: '', attachments: [] })
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
const detailTriageMessages = computed(() => (detailRecord.value?.triageSession?.messages || []).map(message => ({
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
  if (detailRecord.value.doctorHandle?.doctorName) return `当前由医生 ${detailRecord.value.doctorHandle.doctorName} 跟进，可继续补充资料或反馈恢复情况。`
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
const detailMessageSummary = computed(() => getMessageSummary(detailRecord.value))

watch(activeCategoryId, (value) => {
  if (value) loadTemplate(value)
})

watch(() => feedbackForm.manualCorrectDepartmentId, (value) => {
  if (!feedbackForm.manualCorrectDoctorId) return
  const currentDoctor = (feedbackOptions.value?.doctors || []).find(item => item.id === feedbackForm.manualCorrectDoctorId)
  if (currentDoctor && value && currentDoctor.departmentId !== value) {
    feedbackForm.manualCorrectDoctorId = null
  }
})

function loadData() {
  loadPatients()
  loadHistories()
  loadCategories()
  loadRecords()
  loadFeedbackOptions()
}

function loadPatients() {
  get('/api/user/patient/list', (data) => {
    patients.value = data || []
    if (!selectedPatientId.value && patients.value.length) {
      selectedPatientId.value = patients.value.find(item => item.isDefault === 1)?.id || patients.value[0].id
    }
  })
}

function loadHistories() {
  get('/api/user/medical-history/list', (data) => {
    histories.value = data || []
  })
}

function loadCategories() {
  get('/api/user/consultation/category/list', (data) => {
    categories.value = data || []
    if (!activeCategoryId.value && categories.value.length) {
      activeCategoryId.value = categories.value[0].id
    }
  })
}

function loadTemplate(categoryId) {
  templateLoading.value = true
  get(`/api/user/consultation/template/default?categoryId=${categoryId}`, (data) => {
    template.value = data
    resetForm()
    templateLoading.value = false
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
    records.value = (data || []).map(item => ({
      ...item,
      smartDispatch: normalizeSmartDispatch(item?.smartDispatch),
      messageSummary: normalizeMessageSummary(item?.messageSummary)
    }))
    historyLoading.value = false
    callback?.(records.value)
  }, () => {
    historyLoading.value = false
  })
}

function loadFeedbackOptions() {
  get('/api/user/consultation/feedback/options', (data) => {
    feedbackOptions.value = data || { departments: [], doctors: [] }
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

function openRecordDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detailRecord.value = null
  triageAiSending.value = false
  consultationMessages.value = []
  messageLoading.value = false
  messageSending.value = false
  resetTriageAiDraft()
  resetMessageDraft()
  get(`/api/user/consultation/record/detail?recordId=${row.id}`, (data) => {
    detailRecord.value = data ? {
      ...data,
      smartDispatch: normalizeSmartDispatch(data?.smartDispatch),
      messageSummary: normalizeMessageSummary(data?.messageSummary)
    } : null
    applyFeedbackForm(data?.triageFeedback)
    loadConsultationMessages(row.id)
    detailLoading.value = false
  }, (message) => {
    detailLoading.value = false
    detailVisible.value = false
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
    detailRecord.value = data ? {
      ...data,
      smartDispatch: normalizeSmartDispatch(data?.smartDispatch),
      messageSummary: normalizeMessageSummary(data?.messageSummary)
    } : null
    applyFeedbackForm(data?.triageFeedback)
    if (reloadConversation) loadConsultationMessages(recordId)
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

function loadConsultationMessages(recordId = detailRecord.value?.id) {
  if (!recordId) return
  messageLoading.value = true
  get(`/api/user/consultation/message/list?recordId=${recordId}`, (data) => {
    consultationMessages.value = data || []
    syncRecordMessageSummary(recordId, buildLocalMessageSummary(consultationMessages.value))
    messageLoading.value = false
  }, (message) => {
    consultationMessages.value = []
    messageLoading.value = false
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
  if (summary.latestSenderType === 'user') return '已发送，待医生处理'
  if (summary.latestSenderType === 'doctor') return '医生已回复'
  return '沟通中'
}

function recordMessagePreview(record) {
  return getMessageSummary(record).latestMessagePreview || '暂未产生沟通消息'
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
  ;(messages || []).forEach(item => {
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
  const imageSuffix = attachmentCount <= 0
    ? ''
    : attachmentCount === 1 ? '[图片]' : `[图片 x${attachmentCount}]`
  if (content && imageSuffix) return abbreviateText(`${content} ${imageSuffix}`, 72)
  if (content) return abbreviateText(content, 72)
  if (imageSuffix) return imageSuffix
  return '暂未产生沟通消息'
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

function resetMessageDraft() {
  messageDraft.content = ''
  messageDraft.attachments = []
}

function messageAttachments(message) {
  return Array.isArray(message?.attachments) && message.attachments.length
    ? message.attachments
    : parseJsonArray(message?.attachmentsJson)
}

function isUserMessage(message) {
  return message?.senderType === 'user'
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
    attachments: messageDraft.attachments
  }, () => {
    messageSending.value = false
    resetMessageDraft()
    ElMessage.success('消息已发送')
    loadConsultationMessages(recordId)
  }, (message) => {
    messageSending.value = false
    ElMessage.warning(message || '问诊消息发送失败')
  })
}

function applyFeedbackForm(feedback) {
  feedbackForm.userScore = feedback?.userScore || 5
  feedbackForm.isAdopted = feedback?.isAdopted ?? 1
  feedbackForm.feedbackText = feedback?.feedbackText || ''
  feedbackForm.manualCorrectDepartmentId = feedback?.manualCorrectDepartmentId || null
  feedbackForm.manualCorrectDoctorId = feedback?.manualCorrectDoctorId || null
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
      detailRecord.value = data ? {
        ...data,
        smartDispatch: normalizeSmartDispatch(data?.smartDispatch),
        messageSummary: normalizeMessageSummary(data?.messageSummary)
      } : null
      applyFeedbackForm(data?.triageFeedback)
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

watch(detailVisible, (value) => {
  if (!value) {
    detailRecord.value = null
    triageAiSending.value = false
    consultationMessages.value = []
    messageLoading.value = false
    messageSending.value = false
    resetTriageAiDraft()
    resetMessageDraft()
  }
})

onMounted(() => loadData())
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

.feedback-summary,
.feedback-form,
.feedback-actions {
  display: flex;
  gap: 12px;
}

.feedback-summary,
.feedback-form {
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

.conversation-alert {
  margin-bottom: 14px;
}

.conversation-board {
  margin-bottom: 14px;
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

@media (max-width: 1180px) {
  .stat-grid,
  .entry-layout,
  .detail-meta,
  .triage-grid,
  .doctor-list,
  .candidate-list,
  .compare-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .panel-head,
  .patient-top,
  .doctor-recommend-head,
  .doctor-top,
  .session-message-head,
  .triage-ai-toolbar,
  .compare-item,
  .compare-kv,
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
