<template>
  <div class="doctor-page">
    <el-alert v-if="doctor.bound !== 1" :title="doctor.bindingMessage || '当前账号尚未绑定有效医生档案。'" type="warning" :closable="false" />
    <section class="stats">
      <article class="card stat"><span>科室问诊</span><strong>{{ records.length }}</strong></article>
      <article class="card stat"><span>待认领</span><strong>{{ unclaimedCount }}</strong></article>
      <article class="card stat"><span>我认领的</span><strong>{{ mineCount }}</strong></article>
      <article class="card stat"><span>高优先级</span><strong>{{ riskCount }}</strong></article>
    </section>

    <section class="card block">
      <div class="head">
        <div>
          <h3>科室问诊列表</h3>
          <p>先认领，再处理，避免多人同时处理同一条问诊记录。</p>
        </div>
        <div class="toolbar">
          <el-input v-model="keyword" clearable placeholder="搜索就诊人、分类、主诉或状态" style="width:240px" />
          <el-select v-model="ownerFilter" style="width:140px">
            <el-option label="全部归属" value="all" />
            <el-option label="待认领" value="unclaimed" />
            <el-option label="我认领的" value="mine" />
            <el-option label="他人认领" value="others" />
          </el-select>
          <el-select v-model="statusFilter" clearable placeholder="全部状态" style="width:140px">
            <el-option label="已提交" value="submitted" />
            <el-option label="已分诊" value="triaged" />
            <el-option label="处理中" value="processing" />
            <el-option label="已完成" value="completed" />
          </el-select>
          <el-button @click="refreshAll">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredRecords" v-loading="loading" border>
        <el-table-column prop="patientName" label="就诊人" min-width="100" />
        <el-table-column prop="categoryName" label="问诊分类" min-width="120" />
        <el-table-column prop="chiefComplaint" label="主诉" min-width="220" show-overflow-tooltip />
        <el-table-column label="分诊等级" min-width="110">
          <template #default="{ row }">{{ row.triageLevelName || '待评估' }}</template>
        </el-table-column>
        <el-table-column label="认领状态" min-width="180">
          <template #default="{ row }">
            <div class="assignment">
              <el-tag :type="assignmentTagType(row.doctorAssignment)" effect="light">{{ assignmentStatusLabel(row.doctorAssignment) }}</el-tag>
              <span v-if="row.doctorAssignment?.doctorName">{{ row.doctorAssignment.doctorName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }"><el-tag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="160">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
              <el-button v-if="canClaim(row)" link type="success" @click="submitAssignment('claim', row.id)">认领</el-button>
              <el-button v-if="canRelease(row)" link type="warning" @click="submitAssignment('release', row.id)">释放</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !records.length" description="当前科室暂无问诊记录" />
    </section>

    <el-drawer v-model="detailVisible" title="问诊详情" size="70%" destroy-on-close>
      <div v-loading="detailLoading" class="drawer-body">
        <template v-if="detail">
          <section class="card panel">
            <h3>基本信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="问诊单号">{{ detail.consultationNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="就诊人">{{ detail.patientName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="问诊分类">{{ detail.categoryName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所属科室">{{ detail.departmentName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="分诊等级">{{ detail.triageLevelName || '待评估' }}</el-descriptions-item>
              <el-descriptions-item label="建议动作">{{ triageActionLabel(detail.triageActionType) }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
              <el-descriptions-item label="提交时间">{{ formatDate(detail.createTime) }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="card panel">
            <div class="head">
              <div>
                <h3>认领信息</h3>
                <p>{{ assignmentHint }}</p>
              </div>
              <div class="chips">
                <span>{{ assignmentStatusLabel(detail.doctorAssignment) }}</span>
                <span v-if="detail.doctorAssignment?.doctorName">医生：{{ detail.doctorAssignment.doctorName }}</span>
                <span v-if="detail.doctorAssignment?.claimTime">认领 {{ formatDate(detail.doctorAssignment.claimTime) }}</span>
                <span v-if="detail.doctorAssignment?.releaseTime">释放 {{ formatDate(detail.doctorAssignment.releaseTime) }}</span>
              </div>
            </div>
            <div class="actions">
              <el-button v-if="canClaimCurrent" type="primary" plain :loading="assignLoading && assignType==='claim'" @click="submitAssignment('claim', detail.id)">认领问诊单</el-button>
              <el-button v-if="canReleaseCurrent" type="warning" plain :loading="assignLoading && assignType==='release'" @click="submitAssignment('release', detail.id)">释放问诊单</el-button>
            </div>
          </section>

          <section class="card panel">
            <h3>主诉与健康摘要</h3>
            <p class="copy">{{ detail.chiefComplaint || '暂无主诉信息' }}</p>
            <p class="copy">{{ detail.healthSummary || '暂无健康摘要' }}</p>
          </section>

          <section class="card panel">
            <h3>问诊答案</h3>
            <div v-if="detail.answers?.length" class="list">
              <article v-for="item in detail.answers" :key="`${item.fieldCode}-${item.id || item.fieldLabel}`" class="subcard">
                <strong>{{ item.fieldLabel }}</strong>
                <div class="copy">
                  <template v-if="item.fieldType === 'upload' && item.fieldValue"><img :src="resolveImagePath(item.fieldValue)" :alt="item.fieldLabel" class="image" /></template>
                  <template v-else-if="item.fieldType === 'multi_select'"><div class="chips"><span v-for="tag in parseJsonArray(item.fieldValue)" :key="tag">{{ tag }}</span></div></template>
                  <template v-else>{{ displayAnswer(item) }}</template>
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无问诊答案" />
          </section>

          <section class="card panel">
            <div class="head">
              <div>
                <h3>医患沟通</h3>
                <p>在处理前补充追问，处理中同步建议，完成后也可以继续跟进恢复情况。</p>
              </div>
              <div class="head-actions">
                <div class="chips">
                  <span>{{ consultationMessages.length }} 条消息</span>
                  <span v-if="consultationMessages.length">最近更新 {{ formatDate(consultationMessages[consultationMessages.length - 1]?.createTime) }}</span>
                </div>
                <el-button text @click="loadConsultationMessages(detail.id)">刷新消息</el-button>
              </div>
            </div>
            <el-alert
              v-if="messageSendHint"
              :title="messageSendHint"
              :type="canSendMessage ? 'info' : 'warning'"
              :closable="false"
              class="notice"
            />
            <div v-loading="messageLoading" class="message-board">
              <div v-if="consultationMessages.length" class="message-list">
                <article
                  v-for="item in consultationMessages"
                  :key="item.id"
                  :class="['message-card', { mine: isCurrentDoctorMessage(item) }]"
                >
                  <div class="message-meta">
                    <strong>{{ messageSenderLabel(item) }}</strong>
                    <span>{{ formatDate(item.createTime) }}</span>
                  </div>
                  <p v-if="item.content" class="message-content">{{ item.content }}</p>
                  <div v-if="messageAttachments(item).length" class="message-image-list">
                    <img
                      v-for="path in messageAttachments(item)"
                      :key="path"
                      :src="resolveImagePath(path)"
                      alt="消息附件"
                      class="message-image"
                    />
                  </div>
                </article>
              </div>
              <el-empty v-else description="当前暂无沟通消息" />
            </div>
            <div class="message-composer">
              <el-input
                v-model="messageDraft.content"
                type="textarea"
                :rows="3"
                maxlength="2000"
                show-word-limit
                :disabled="!canSendMessage"
                placeholder="可向患者追问症状变化、提醒补充资料，或同步处理建议与随访安排。"
              />
              <div v-if="messageDraft.attachments.length" class="message-attachments">
                <article v-for="(path, index) in messageDraft.attachments" :key="path" class="message-attachment-item">
                  <img :src="resolveImagePath(path)" alt="待发送附件" class="message-image" />
                  <div class="message-attachment-actions">
                    <span>图片 {{ index + 1 }}</span>
                    <el-button link type="danger" @click="removeMessageAttachment(index)">移除</el-button>
                  </div>
                </article>
              </div>
              <div class="message-toolbar">
                <el-upload
                  :action="messageUploadAction"
                  :headers="messageUploadHeaders"
                  :show-file-list="false"
                  accept="image/*"
                  :before-upload="beforeMessageUpload"
                  :disabled="!canSendMessage || messageDraft.attachments.length >= 6"
                  :on-success="handleMessageUploadSuccess"
                >
                  <el-button plain :disabled="!canSendMessage || messageDraft.attachments.length >= 6">上传沟通图片</el-button>
                </el-upload>
                <span class="message-tip">支持文字和图片，单次最多 6 张。</span>
                <el-button type="primary" :loading="messageSending" :disabled="!canSendMessage" @click="sendConsultationMessage">
                  {{ detail.doctorAssignment?.status === 'claimed' && detail.doctorAssignment?.doctorId === doctor.doctorId ? '发送消息' : '发送并认领' }}
                </el-button>
              </div>
            </div>
          </section>

          <section class="card panel">
            <div class="head">
              <div>
                <h3>医生处理</h3>
                <p>记录判断摘要、处理建议和随访计划。</p>
              </div>
              <div class="head-actions">
                <div v-if="detail.doctorHandle" class="chips">
                  <span>{{ handleStatusLabel(detail.doctorHandle.status) }}</span>
                  <span>接手 {{ formatDate(detail.doctorHandle.receiveTime) }}</span>
                  <span v-if="detail.doctorHandle.completeTime">完成 {{ formatDate(detail.doctorHandle.completeTime) }}</span>
                </div>
                <el-button text type="primary" @click="openReplyTemplateManager">管理常用模板</el-button>
              </div>
            </div>
            <el-alert v-if="!canEdit" :title="assignmentHint" type="warning" :closable="false" class="notice" />
            <div v-if="templateLoading" class="template-banner">
              <span>正在加载个人常用回复模板...</span>
            </div>
            <div v-else-if="!replyTemplates.length" class="template-banner">
              <span>当前还没有个人常用回复模板，可以先维护常见摘要、处理建议和随访话术，后续处理问诊时可直接快捷填入。</span>
              <el-button link type="primary" @click="openReplyTemplateManager">去维护模板</el-button>
            </div>
            <el-form label-position="top" :disabled="!canEdit">
              <el-form-item label="医生判断摘要">
                <div v-if="sceneTemplates('handle_summary').length" class="template-tools">
                  <el-select v-model="templateSelection.handle_summary" clearable filterable placeholder="选择摘要模板" style="width:240px">
                    <el-option v-for="item in sceneTemplates('handle_summary')" :key="item.id" :label="item.title" :value="item.id" />
                  </el-select>
                  <el-button text @click="applyTemplateToField('handle_summary', 'summary', 'replace')">覆盖填入</el-button>
                  <el-button text @click="applyTemplateToField('handle_summary', 'summary', 'append')">追加填入</el-button>
                </div>
                <el-input v-model="handleForm.summary" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="例如：当前暂无紧急风险，建议继续线上处理并观察变化。" />
              </el-form-item>
              <el-form-item label="处理建议">
                <div v-if="sceneTemplates('medical_advice').length" class="template-tools">
                  <el-select v-model="templateSelection.medical_advice" clearable filterable placeholder="选择处理建议模板" style="width:240px">
                    <el-option v-for="item in sceneTemplates('medical_advice')" :key="item.id" :label="item.title" :value="item.id" />
                  </el-select>
                  <el-button text @click="applyTemplateToField('medical_advice', 'medicalAdvice', 'replace')">覆盖填入</el-button>
                  <el-button text @click="applyTemplateToField('medical_advice', 'medicalAdvice', 'append')">追加填入</el-button>
                </div>
                <el-input v-model="handleForm.medicalAdvice" type="textarea" :rows="4" maxlength="4000" show-word-limit placeholder="填写生活建议、用药建议、复诊建议等。" />
              </el-form-item>
              <el-form-item label="随访计划">
                <div v-if="sceneTemplates('follow_up_plan').length" class="template-tools">
                  <el-select v-model="templateSelection.follow_up_plan" clearable filterable placeholder="选择随访计划模板" style="width:240px">
                    <el-option v-for="item in sceneTemplates('follow_up_plan')" :key="item.id" :label="item.title" :value="item.id" />
                  </el-select>
                  <el-button text @click="applyTemplateToField('follow_up_plan', 'followUpPlan', 'replace')">覆盖填入</el-button>
                  <el-button text @click="applyTemplateToField('follow_up_plan', 'followUpPlan', 'append')">追加填入</el-button>
                </div>
                <el-input v-model="handleForm.followUpPlan" maxlength="500" show-word-limit placeholder="例如：建议 3 天后复诊，如加重请线下就医。" />
              </el-form-item>
              <el-form-item label="内部备注">
                <el-input v-model="handleForm.internalRemark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="仅医生和管理员可见。" />
              </el-form-item>
            </el-form>
          </section>

          <section class="card panel">
            <div class="head">
              <div>
                <h3>结构化结论</h3>
                <p>作为后续 AI 对比分析和统计复盘的标准化数据。</p>
              </div>
              <div v-if="detail.doctorConclusion" class="chips">
                <span>{{ conditionLevelLabel(detail.doctorConclusion.conditionLevel) }}</span>
                <span>{{ dispositionLabel(detail.doctorConclusion.disposition) }}</span>
                <span>{{ aiConsistencyLabel(detail.doctorConclusion.isConsistentWithAi) }}</span>
              </div>
            </div>
            <el-form label-position="top" :disabled="!canEdit">
              <div class="grid">
                <el-form-item label="病情等级">
                  <el-select v-model="conclusionForm.conditionLevel" clearable><el-option v-for="item in conditionLevelOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
                </el-form-item>
                <el-form-item label="处理去向">
                  <el-select v-model="conclusionForm.disposition" clearable><el-option v-for="item in dispositionOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
                </el-form-item>
                <el-form-item label="是否与 AI 一致">
                  <el-select v-model="conclusionForm.isConsistentWithAi" clearable><el-option label="一致" :value="1" /><el-option label="不一致" :value="0" /></el-select>
                </el-form-item>
                <el-form-item label="是否需要随访">
                  <el-switch v-model="conclusionForm.needFollowUp" :active-value="1" :inactive-value="0" inline-prompt active-text="需要" inactive-text="不需要" />
                </el-form-item>
                <el-form-item v-if="conclusionForm.needFollowUp === 1" label="建议随访时效（天）">
                  <el-input-number v-model="conclusionForm.followUpWithinDays" :min="1" :max="365" style="width:100%" />
                </el-form-item>
                <el-form-item label="诊断方向">
                  <el-input v-model="conclusionForm.diagnosisDirection" maxlength="100" show-word-limit placeholder="例如：上呼吸道感染倾向" />
                </el-form-item>
              </div>
              <el-form-item label="结论标签">
                <el-select v-model="conclusionForm.conclusionTags" multiple filterable collapse-tags collapse-tags-tooltip placeholder="选择结论标签">
                  <el-option v-for="item in conclusionTagOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item label="患者指导要点">
                <div v-if="sceneTemplates('patient_instruction').length" class="template-tools">
                  <el-select v-model="templateSelection.patient_instruction" clearable filterable placeholder="选择患者指导模板" style="width:240px">
                    <el-option v-for="item in sceneTemplates('patient_instruction')" :key="item.id" :label="item.title" :value="item.id" />
                  </el-select>
                  <el-button text @click="applyTemplateToField('patient_instruction', 'patientInstruction', 'replace')">覆盖填入</el-button>
                  <el-button text @click="applyTemplateToField('patient_instruction', 'patientInstruction', 'append')">追加填入</el-button>
                </div>
                <el-input v-model="conclusionForm.patientInstruction" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="例如：如出现持续高热或呼吸困难，请立即线下就医。" />
              </el-form-item>
            </el-form>
            <div class="actions">
              <el-button type="warning" plain :disabled="!canEdit || detail.status === 'completed'" :loading="submitLoading && submitStatus==='processing'" @click="submitHandle('processing')">标记处理中</el-button>
              <el-button type="primary" :disabled="!canEdit" :loading="submitLoading && submitStatus==='completed'" @click="submitHandle('completed')">完成处理</el-button>
            </div>
          </section>

          <section class="card panel">
            <div class="head">
              <div>
                <h3>随访记录</h3>
                <p>已完成的问诊单可由原处理医生持续追加随访记录，便于后续复诊和质控追踪。</p>
              </div>
              <div class="chips" v-if="detail.doctorFollowUps?.length">
                <span>共 {{ detail.doctorFollowUps.length }} 条</span>
                <span v-if="detail.doctorFollowUps[0]?.createTime">最近随访 {{ formatDate(detail.doctorFollowUps[0].createTime) }}</span>
              </div>
            </div>

            <div v-if="detail.doctorFollowUps?.length" class="list">
              <article v-for="item in detail.doctorFollowUps" :key="item.id" class="subcard">
                <div class="chips">
                  <span>{{ followUpTypeLabel(item.followUpType) }}</span>
                  <span>{{ patientStatusLabel(item.patientStatus) }}</span>
                  <span>{{ item.doctorName || '-' }}</span>
                  <span>{{ formatDate(item.createTime) }}</span>
                  <span v-if="item.needRevisit === 1">需再次随访</span>
                  <span v-if="item.nextFollowUpDate">下次 {{ formatDate(item.nextFollowUpDate) }}</span>
                </div>
                <p class="copy"><strong>随访摘要：</strong>{{ item.summary }}</p>
                <p class="copy"><strong>随访建议：</strong>{{ item.advice || '暂无补充建议' }}</p>
                <p class="copy"><strong>下一步安排：</strong>{{ item.nextStep || '暂无下一步安排' }}</p>
              </article>
            </div>
            <el-empty v-else description="当前暂无随访记录" />

            <el-alert v-if="!canSubmitFollowUp" :title="followUpHint" type="info" :closable="false" class="notice" />

            <el-form label-position="top" :disabled="!canSubmitFollowUp">
              <div class="grid">
                <el-form-item label="随访方式">
                  <el-select v-model="followUpForm.followUpType">
                    <el-option v-for="item in followUpTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="患者状态变化">
                  <el-select v-model="followUpForm.patientStatus">
                    <el-option v-for="item in patientStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="是否需要再次随访">
                  <el-switch v-model="followUpForm.needRevisit" :active-value="1" :inactive-value="0" inline-prompt active-text="需要" inactive-text="不需要" />
                </el-form-item>
                <el-form-item v-if="followUpForm.needRevisit === 1" label="下次随访日期">
                  <el-date-picker v-model="followUpForm.nextFollowUpDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </div>
              <el-form-item label="随访摘要">
                <div v-if="sceneTemplates('followup_summary').length" class="template-tools">
                  <el-select v-model="templateSelection.followup_summary" clearable filterable placeholder="选择随访摘要模板" style="width:240px">
                    <el-option v-for="item in sceneTemplates('followup_summary')" :key="item.id" :label="item.title" :value="item.id" />
                  </el-select>
                  <el-button text @click="applyTemplateToField('followup_summary', 'summary', 'replace', 'followUp')">覆盖填入</el-button>
                  <el-button text @click="applyTemplateToField('followup_summary', 'summary', 'append', 'followUp')">追加填入</el-button>
                </div>
                <el-input v-model="followUpForm.summary" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="例如：患者发热较前缓解，夜间咳嗽仍存在但频次下降。" />
              </el-form-item>
              <el-form-item label="随访建议">
                <div v-if="sceneTemplates('followup_advice').length" class="template-tools">
                  <el-select v-model="templateSelection.followup_advice" clearable filterable placeholder="选择随访建议模板" style="width:240px">
                    <el-option v-for="item in sceneTemplates('followup_advice')" :key="item.id" :label="item.title" :value="item.id" />
                  </el-select>
                  <el-button text @click="applyTemplateToField('followup_advice', 'advice', 'replace', 'followUp')">覆盖填入</el-button>
                  <el-button text @click="applyTemplateToField('followup_advice', 'advice', 'append', 'followUp')">追加填入</el-button>
                </div>
                <el-input v-model="followUpForm.advice" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="例如：继续按既定方案观察，如 48 小时后仍反复发热建议线下复诊。" />
              </el-form-item>
              <el-form-item label="下一步安排">
                <div v-if="sceneTemplates('followup_next_step').length" class="template-tools">
                  <el-select v-model="templateSelection.followup_next_step" clearable filterable placeholder="选择下一步安排模板" style="width:240px">
                    <el-option v-for="item in sceneTemplates('followup_next_step')" :key="item.id" :label="item.title" :value="item.id" />
                  </el-select>
                  <el-button text @click="applyTemplateToField('followup_next_step', 'nextStep', 'replace', 'followUp')">覆盖填入</el-button>
                  <el-button text @click="applyTemplateToField('followup_next_step', 'nextStep', 'append', 'followUp')">追加填入</el-button>
                </div>
                <el-input v-model="followUpForm.nextStep" maxlength="500" show-word-limit placeholder="例如：3 天后再次平台随访，必要时安排线下检查。" />
              </el-form-item>
            </el-form>
            <div class="actions">
              <el-button type="primary" :disabled="!canSubmitFollowUp" :loading="followUpSubmitting" @click="submitFollowUp">追加随访记录</el-button>
            </div>
          </section>

          <section class="card panel">
            <h3>AI 分诊结果</h3>
            <div v-if="detail.triageResult" class="subcard">
              <div class="chips">
                <span>{{ detail.triageResult.triageLevelName || '待评估' }}</span>
                <span>{{ detail.triageResult.departmentName || '未匹配科室' }}</span>
                <span v-if="detail.triageResult.doctorName">推荐医生：{{ detail.triageResult.doctorName }}</span>
              </div>
              <p class="copy">{{ detail.triageResult.reasonText || '暂无分诊说明' }}</p>
            </div>
            <el-empty v-else description="暂无 AI 分诊结果" />
          </section>

          <section class="card panel">
            <div class="head">
              <div>
                <h3>AI 导诊上下文</h3>
                <p>医生接诊前可直接查看 AI 导诊解释、候选医生和完整导诊留痕，减少接手成本。</p>
              </div>
              <div v-if="detail.triageSession" class="chips">
                <span>Session {{ detail.triageSession.sessionNo || '-' }}</span>
                <span>{{ triageSessionStatusLabel(detail.triageSession.status) }}</span>
                <span>{{ detail.triageSession.messageCount || 0 }} 条消息</span>
              </div>
            </div>

            <template v-if="detail.triageResult">
              <div class="subcard">
                <p class="copy"><strong>结果说明：</strong>{{ detail.triageResult.reasonText || '暂无分诊说明' }}</p>
                <p class="copy"><strong>风险标签：</strong>{{ parseJsonArray(detail.triageResult.riskFlagsJson).join('、') || '暂无风险标签' }}</p>
                <p class="copy"><strong>置信度：</strong>{{ formatConfidence(detail.triageResult.confidenceScore) }}</p>
              </div>

              <div v-if="triageDoctorCandidates.length" class="triage-doctor-list">
                <article v-for="item in triageDoctorCandidates" :key="item.id || item.name" class="subcard triage-doctor-card">
                  <img v-if="item.photo" :src="resolveImagePath(item.photo)" :alt="item.name" class="triage-doctor-avatar" />
                  <div v-else class="triage-doctor-avatar triage-doctor-avatar-fallback">{{ (item.name || '医').slice(0, 1) }}</div>
                  <div class="triage-doctor-copy">
                    <strong>{{ item.name || '未命名医生' }}</strong>
                    <span>{{ item.title || '在线医生' }}</span>
                    <p class="copy">{{ item.expertise || '暂无擅长信息' }}</p>
                    <small>{{ item.nextScheduleText || '暂无后续排班' }}</small>
                  </div>
                </article>
              </div>
            </template>

            <div v-if="detail.triageSession?.messages?.length" class="triage-session-list">
              <article
                v-for="message in triageSessionMessages"
                :key="message.id"
                :class="['triage-session-card', { user: message.roleType === 'user', assistant: message.roleType === 'assistant' }]"
              >
                <div class="triage-session-head">
                  <div>
                    <strong>{{ message.title }}</strong>
                    <span>{{ messageTypeLabel(message.messageType) }}</span>
                  </div>
                  <el-tag size="small" effect="light">{{ messageRoleLabel(message.roleType) }}</el-tag>
                </div>
                <p class="triage-session-content">{{ message.content }}</p>
                <div v-if="message.insight" class="triage-session-insight">
                  <div class="triage-session-insight-meta">
                    <span v-if="message.insight.recommendedVisitType">建议方式：{{ message.insight.recommendedVisitType }}</span>
                    <span v-if="message.insight.recommendedDepartmentName">建议科室：{{ message.insight.recommendedDepartmentName }}</span>
                    <span v-if="message.insight.confidenceText">置信度：{{ message.insight.confidenceText }}</span>
                  </div>
                  <p v-if="message.insight.doctorRecommendationReason" class="triage-session-insight-copy">
                    <strong>推荐依据：</strong>{{ message.insight.doctorRecommendationReason }}
                  </p>
                  <div v-if="message.insight.recommendedDoctors.length" class="triage-session-insight-tags">
                    <span v-for="item in message.insight.recommendedDoctors" :key="item">{{ item }}</span>
                  </div>
                  <div v-if="message.insight.riskFlags.length" class="triage-session-insight-tags danger">
                    <span v-for="item in message.insight.riskFlags" :key="item">{{ item }}</span>
                  </div>
                </div>
                <small>{{ formatDate(message.createTime) }}</small>
              </article>
            </div>
            <el-empty v-else description="暂无 AI 导诊留痕" />
          </section>

          <section class="card panel">
            <h3>规则命中</h3>
            <el-table v-if="detail.ruleHits?.length" :data="detail.ruleHits" border>
              <el-table-column prop="ruleName" label="规则名称" min-width="160" />
              <el-table-column prop="matchedSummary" label="命中摘要" min-width="220" show-overflow-tooltip />
              <el-table-column prop="triageLevelName" label="等级" min-width="100" />
            </el-table>
            <el-empty v-else description="暂无规则命中记录" />
          </section>

          <section class="card panel">
            <h3>用户反馈</h3>
            <div v-if="detail.triageFeedback" class="subcard">
              <div class="chips">
                <span>评分：{{ detail.triageFeedback.userScore ?? '-' }}</span>
                <span>{{ detail.triageFeedback.isAdopted === 1 ? '已采纳' : '未采纳' }}</span>
                <span v-if="detail.triageFeedback.manualCorrectDepartmentName">修正科室：{{ detail.triageFeedback.manualCorrectDepartmentName }}</span>
                <span v-if="detail.triageFeedback.manualCorrectDoctorName">修正医生：{{ detail.triageFeedback.manualCorrectDoctorName }}</span>
              </div>
              <p class="copy">{{ detail.triageFeedback.feedbackText || '用户未填写补充反馈。' }}</p>
            </div>
            <el-empty v-else description="用户暂未提交反馈" />
          </section>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authHeader, backendBaseUrl, get, post, resolveImagePath } from '@/net'
import { resolveTriageMessageInsight } from '@/triage/insight'

const route = useRoute()
const router = useRouter()
const conditionLevelOptions = [{ label: '轻度', value: 'low' }, { label: '中度', value: 'medium' }, { label: '较高风险', value: 'high' }, { label: '危急', value: 'critical' }]
const dispositionOptions = [{ label: '继续观察', value: 'observe' }, { label: '线上随访', value: 'online_followup' }, { label: '线下就医', value: 'offline_visit' }, { label: '立即急诊', value: 'emergency' }]
const conclusionTagOptions = ['适合居家观察', '建议线下检查', '建议药物评估', '需要复诊随访', '过敏风险', '发热监测', '皮肤护理', '慢病管理']
const followUpTypeOptions = [{ label: '平台随访', value: 'platform' }, { label: '电话随访', value: 'phone' }, { label: '线下随访', value: 'offline' }, { label: '其他方式', value: 'other' }]
const patientStatusOptions = [{ label: '明显好转', value: 'improved' }, { label: '基本稳定', value: 'stable' }, { label: '出现加重', value: 'worsened' }, { label: '其他情况', value: 'other' }]
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const submitLoading = ref(false)
const assignLoading = ref(false)
const followUpSubmitting = ref(false)
const templateLoading = ref(false)
const submitStatus = ref('')
const assignType = ref('')
const keyword = ref('')
const ownerFilter = ref('all')
const statusFilter = ref('')
const records = ref([])
const detail = ref(null)
const consultationMessages = ref([])
const replyTemplates = ref([])
const messageLoading = ref(false)
const messageSending = ref(false)
const doctor = reactive({ bound: 1, bindingMessage: '', doctorId: null, doctorName: '' })
const handleForm = reactive({ summary: '', medicalAdvice: '', followUpPlan: '', internalRemark: '' })
const conclusionForm = reactive({ conditionLevel: '', disposition: '', diagnosisDirection: '', conclusionTags: [], needFollowUp: 0, followUpWithinDays: null, isConsistentWithAi: null, patientInstruction: '' })
const followUpForm = reactive({ followUpType: 'platform', patientStatus: 'stable', summary: '', advice: '', nextStep: '', needRevisit: 0, nextFollowUpDate: '' })
const messageDraft = reactive({ content: '', attachments: [] })
const templateSelection = reactive({
  handle_summary: null,
  medical_advice: null,
  follow_up_plan: null,
  patient_instruction: null,
  followup_summary: null,
  followup_advice: null,
  followup_next_step: null
})

const filteredRecords = computed(() => records.value.filter(item => {
  const search = keyword.value.trim().toLowerCase()
  const matchesKeyword = !search || [item.patientName, item.categoryName, item.chiefComplaint, item.status].filter(Boolean).some(text => `${text}`.toLowerCase().includes(search))
  const matchesOwner = ownerFilter.value === 'all' || ownerType(item) === ownerFilter.value
  const matchesStatus = !statusFilter.value || item.status === statusFilter.value
  return matchesKeyword && matchesOwner && matchesStatus
}))
const unclaimedCount = computed(() => records.value.filter(item => ownerType(item) === 'unclaimed').length)
const mineCount = computed(() => records.value.filter(item => ownerType(item) === 'mine').length)
const riskCount = computed(() => records.value.filter(item => ['emergency', 'offline'].includes(item.triageActionType)).length)
const canClaimCurrent = computed(() => canClaim(detail.value))
const canReleaseCurrent = computed(() => canRelease(detail.value))
const canEdit = computed(() => doctor.bound === 1 && !claimedByOther(detail.value?.doctorAssignment))
const canSendMessage = computed(() => doctor.bound === 1 && !!detail.value && !claimedByOther(detail.value?.doctorAssignment))
const triageDoctorCandidates = computed(() => parseDoctorCandidates(detail.value?.triageResult?.doctorCandidatesJson))
const triageSessionMessages = computed(() => (detail.value?.triageSession?.messages || []).map(message => ({
  ...message,
  insight: resolveTriageMessageInsight(message)
})))
const canSubmitFollowUp = computed(() => doctor.bound === 1
  && detail.value?.status === 'completed'
  && detail.value?.doctorHandle?.status === 'completed'
  && detail.value?.doctorHandle?.doctorId === doctor.doctorId)
const assignmentHint = computed(() => {
  if (doctor.bound !== 1) return doctor.bindingMessage || '当前账号尚未绑定有效医生档案。'
  const assignment = detail.value?.doctorAssignment
  if (!assignment || assignment.status !== 'claimed') return '当前问诊单未被锁定，认领后即可开始处理。'
  return assignment.doctorId === doctor.doctorId ? '当前问诊单已由你认领，可继续处理。' : `当前问诊单已由医生 ${assignment.doctorName || '-'} 认领，你现在只能查看。`
})
const followUpHint = computed(() => {
  if (doctor.bound !== 1) return doctor.bindingMessage || '当前账号尚未绑定有效医生档案。'
  if (detail.value?.status !== 'completed') return '仅已完成的问诊单才可追加随访记录。'
  if (detail.value?.doctorHandle?.status !== 'completed') return '当前问诊单尚未完成医生处理，暂不可记录随访。'
  if (detail.value?.doctorHandle?.doctorId !== doctor.doctorId) return '仅处理该问诊单的医生可追加随访记录。'
  return '可继续追加随访记录。'
})
const messageSendHint = computed(() => {
  if (doctor.bound !== 1) return doctor.bindingMessage || '当前账号尚未绑定有效医生档案。'
  const assignment = detail.value?.doctorAssignment
  if (claimedByOther(assignment)) return `当前问诊单已由医生 ${assignment?.doctorName || '-'} 认领，你现在只能查看沟通记录。`
  if (!assignment || assignment.status !== 'claimed') return '发送首条消息时会自动认领当前问诊单，适合先和患者确认补充信息。'
  return '可继续与患者沟通病情变化、检查资料和随访安排。'
})
const messageUploadAction = computed(() => `${backendBaseUrl()}/api/image/cache`)
const messageUploadHeaders = computed(() => authHeader())

function refreshAll() { loadDoctor(); loadRecords() }
function loadReplyTemplates() {
  templateLoading.value = true
  get('/api/doctor/reply-template/list', data => {
    replyTemplates.value = data || []
    templateLoading.value = false
  }, () => {
    templateLoading.value = false
  })
}
function loadDoctor() {
  get('/api/doctor/workbench/summary', data => {
    doctor.bound = data?.bound ?? 0
    doctor.bindingMessage = data?.bindingMessage || ''
    doctor.doctorId = data?.doctorId ?? null
    doctor.doctorName = data?.doctorName || ''
  }, message => ElMessage.warning(message || '医生工作台信息加载失败'))
}
function loadRecords(callback) {
  loading.value = true
  get('/api/doctor/consultation/list', data => {
    records.value = data || []
    loading.value = false
    callback?.()
    autoOpen()
  }, message => {
    loading.value = false
    ElMessage.warning(message || '医生问诊列表加载失败')
  })
}
function autoOpen() {
  const id = Number(route.query.id || 0)
  if (id && records.value.some(item => item.id === id) && (!detail.value || detail.value.id !== id)) openDetail(id)
}
function openDetail(id) {
  detailVisible.value = true
  detailLoading.value = true
  consultationMessages.value = []
  messageLoading.value = false
  messageSending.value = false
  resetMessageDraft()
  get(`/api/doctor/consultation/detail?id=${id}`, data => {
    detail.value = data || null
    syncForms()
    loadConsultationMessages(id)
    detailLoading.value = false
    if (Number(route.query.id || 0) !== id) router.replace({ path: '/doctor/consultation', query: { id } })
  }, message => {
    detailLoading.value = false
    detailVisible.value = false
    ElMessage.warning(message || '问诊详情加载失败')
  })
}
function syncForms() {
  const handle = detail.value?.doctorHandle
  handleForm.summary = handle?.summary || ''
  handleForm.medicalAdvice = handle?.medicalAdvice || ''
  handleForm.followUpPlan = handle?.followUpPlan || ''
  handleForm.internalRemark = handle?.internalRemark || ''
  const conclusion = detail.value?.doctorConclusion
  conclusionForm.conditionLevel = conclusion?.conditionLevel || ''
  conclusionForm.disposition = conclusion?.disposition || ''
  conclusionForm.diagnosisDirection = conclusion?.diagnosisDirection || ''
  conclusionForm.conclusionTags = parseJsonArray(conclusion?.conclusionTagsJson).filter(Boolean)
  conclusionForm.needFollowUp = conclusion?.needFollowUp === 1 ? 1 : 0
  conclusionForm.followUpWithinDays = conclusion?.followUpWithinDays || null
  conclusionForm.isConsistentWithAi = conclusion?.isConsistentWithAi ?? null
  conclusionForm.patientInstruction = conclusion?.patientInstruction || ''
  resetFollowUpForm()
}
function resetFollowUpForm() {
  followUpForm.followUpType = 'platform'
  followUpForm.patientStatus = 'stable'
  followUpForm.summary = ''
  followUpForm.advice = ''
  followUpForm.nextStep = ''
  followUpForm.needRevisit = 0
  followUpForm.nextFollowUpDate = ''
}
function loadConsultationMessages(recordId = detail.value?.id) {
  if (!recordId) return
  messageLoading.value = true
  get(`/api/doctor/consultation/message/list?recordId=${recordId}`, data => {
    consultationMessages.value = data || []
    messageLoading.value = false
  }, message => {
    consultationMessages.value = []
    messageLoading.value = false
    ElMessage.warning(message || '问诊沟通消息加载失败')
  })
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
function isCurrentDoctorMessage(message) {
  return message?.senderType === 'doctor' && message?.senderId === doctor.doctorId
}
function messageSenderLabel(message) {
  if (message?.senderType === 'user') return message.senderName || detail.value?.patientName || '患者'
  const roleName = message?.senderRoleName ? ` · ${message.senderRoleName}` : ''
  return `${message?.senderName || '医生'}${roleName}`
}
function beforeMessageUpload(file) {
  const isImage = `${file.type || ''}`.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 <= 5
  if (!isImage) {
    ElMessage.error('当前阶段仅支持上传图片')
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
  const recordId = detail.value?.id
  if (!recordId) return
  if (!canSendMessage.value) return ElMessage.warning(messageSendHint.value || '当前暂无发送权限')
  const content = `${messageDraft.content || ''}`.trim()
  if (!content && !messageDraft.attachments.length) return ElMessage.warning('请先输入消息内容或上传图片')
  const shouldAutoClaim = detail.value?.doctorAssignment?.status !== 'claimed'
  messageSending.value = true
  post('/api/doctor/consultation/message/send', {
    recordId,
    content,
    attachments: messageDraft.attachments
  }, () => {
    messageSending.value = false
    resetMessageDraft()
    ElMessage.success(shouldAutoClaim ? '消息已发送，当前问诊单已自动认领' : '消息已发送')
    loadRecords(() => openDetail(recordId))
  }, message => {
    messageSending.value = false
    ElMessage.warning(message || '问诊消息发送失败')
  })
}
function sceneTemplates(sceneType) {
  return replyTemplates.value.filter(item => item.status === 1 && item.sceneType === sceneType)
}
function openReplyTemplateManager() {
  router.push('/doctor/reply-template')
}
function applyTemplateToField(sceneType, fieldKey, mode = 'append', formType = 'handle') {
  const templateId = templateSelection[sceneType]
  const template = replyTemplates.value.find(item => item.id === templateId)
  if (!template) return ElMessage.warning('请先选择模板')
  const form = formType === 'followUp' ? followUpForm : fieldKey in handleForm ? handleForm : conclusionForm
  const current = `${form[fieldKey] || ''}`.trim()
  form[fieldKey] = mode === 'replace' || !current ? template.content : `${current}\n${template.content}`
}
function submitAssignment(type, id) {
  assignLoading.value = true
  assignType.value = type
  post(`/api/doctor/consultation/${type}`, { consultationId: id }, () => {
    assignLoading.value = false
    assignType.value = ''
    ElMessage.success(type === 'claim' ? '问诊单认领成功' : '问诊单已释放')
    loadRecords(() => {
      if (detailVisible.value && detail.value?.id === id) openDetail(id)
    })
  }, message => {
    assignLoading.value = false
    assignType.value = ''
    ElMessage.warning(message || (type === 'claim' ? '问诊单认领失败' : '问诊单释放失败'))
  })
}
function submitHandle(status) {
  if (!detail.value?.id) return
  if (!canEdit.value) return ElMessage.warning(assignmentHint.value)
  if (!`${handleForm.summary || ''}`.trim()) return ElMessage.warning('请先填写医生判断摘要')
  if (status === 'completed' && !`${handleForm.medicalAdvice || ''}`.trim()) return ElMessage.warning('完成处理时请填写处理建议')
  if (status === 'completed' && !conclusionForm.conditionLevel) return ElMessage.warning('完成处理时请填写病情等级')
  if (status === 'completed' && !conclusionForm.disposition) return ElMessage.warning('完成处理时请填写处理去向')
  if (status === 'completed' && conclusionForm.isConsistentWithAi === null) return ElMessage.warning('完成处理时请填写是否与 AI 一致')
  if (status === 'completed' && conclusionForm.needFollowUp === 1 && !conclusionForm.followUpWithinDays) return ElMessage.warning('需要随访时请填写建议随访时效')
  submitLoading.value = true
  submitStatus.value = status
  post('/api/doctor/consultation/handle', {
    consultationId: detail.value.id,
    status,
    summary: `${handleForm.summary || ''}`.trim(),
    medicalAdvice: `${handleForm.medicalAdvice || ''}`.trim(),
    followUpPlan: `${handleForm.followUpPlan || ''}`.trim(),
    internalRemark: `${handleForm.internalRemark || ''}`.trim(),
    conditionLevel: conclusionForm.conditionLevel || null,
    disposition: conclusionForm.disposition || null,
    diagnosisDirection: `${conclusionForm.diagnosisDirection || ''}`.trim(),
    conclusionTags: conclusionForm.conclusionTags || [],
    needFollowUp: conclusionForm.needFollowUp,
    followUpWithinDays: conclusionForm.needFollowUp === 1 ? conclusionForm.followUpWithinDays : null,
    isConsistentWithAi: conclusionForm.isConsistentWithAi,
    patientInstruction: `${conclusionForm.patientInstruction || ''}`.trim()
  }, () => {
    submitLoading.value = false
    submitStatus.value = ''
    ElMessage.success(status === 'completed' ? '医生处理结果已保存' : '已标记为处理中')
    loadRecords(() => openDetail(detail.value.id))
  }, message => {
    submitLoading.value = false
    submitStatus.value = ''
    ElMessage.warning(message || '医生处理结果保存失败')
  })
}
function submitFollowUp() {
  if (!detail.value?.id) return
  if (!canSubmitFollowUp.value) return ElMessage.warning(followUpHint.value)
  if (!`${followUpForm.summary || ''}`.trim()) return ElMessage.warning('请先填写随访摘要')
  if (followUpForm.needRevisit === 1 && !followUpForm.nextFollowUpDate) return ElMessage.warning('需要再次随访时请填写下次随访日期')
  followUpSubmitting.value = true
  post('/api/doctor/consultation/follow-up', {
    consultationId: detail.value.id,
    followUpType: followUpForm.followUpType,
    patientStatus: followUpForm.patientStatus,
    summary: `${followUpForm.summary || ''}`.trim(),
    advice: `${followUpForm.advice || ''}`.trim(),
    nextStep: `${followUpForm.nextStep || ''}`.trim(),
    needRevisit: followUpForm.needRevisit,
    nextFollowUpDate: followUpForm.needRevisit === 1 ? followUpForm.nextFollowUpDate : null
  }, () => {
    followUpSubmitting.value = false
    ElMessage.success('随访记录已保存')
    loadRecords(() => openDetail(detail.value.id))
  }, message => {
    followUpSubmitting.value = false
    ElMessage.warning(message || '随访记录保存失败')
  })
}
function ownerType(record) {
  const assignment = record?.doctorAssignment
  if (!assignment || assignment.status !== 'claimed') return 'unclaimed'
  return assignment.doctorId === doctor.doctorId ? 'mine' : 'others'
}
function canClaim(record) { return !!record && doctor.bound === 1 && record.status !== 'completed' && ownerType(record) === 'unclaimed' }
function canRelease(record) {
  const assignment = record?.doctorAssignment
  return !!record && doctor.bound === 1 && record.status !== 'completed' && assignment?.status === 'claimed' && assignment.doctorId === doctor.doctorId
}
function claimedByOther(assignment) { return assignment?.status === 'claimed' && assignment.doctorId !== doctor.doctorId }
function assignmentStatusLabel(assignment) {
  if (!assignment || assignment.status !== 'claimed') return assignment?.status === 'released' ? '已释放' : '待认领'
  return assignment.doctorId === doctor.doctorId ? '我已认领' : '他人已认领'
}
function assignmentTagType(assignment) {
  if (!assignment || assignment.status !== 'claimed') return 'info'
  return assignment.doctorId === doctor.doctorId ? 'success' : 'warning'
}
function parseJsonArray(value) { try { const parsed = value ? JSON.parse(value) : []; return Array.isArray(parsed) ? parsed : [] } catch { return [] } }
function parseDoctorCandidates(value) { return parseJsonArray(value).filter(item => item && typeof item === 'object') }
function displayAnswer(answer) { return answer.fieldType === 'switch' ? (answer.fieldValue === '1' ? '是' : '否') : (answer.fieldValue || '-') }
function statusLabel(value) { return ({ submitted: '已提交', triaged: '已分诊', processing: '处理中', completed: '已完成' })[value] || value || '-' }
function handleStatusLabel(value) { return value === 'completed' ? '处理完成' : '处理中' }
function statusTagType(value) { return ({ submitted: 'info', triaged: 'primary', processing: 'warning', completed: 'success' })[value] || 'info' }
function triageActionLabel(value) { return ({ emergency: '立即急诊', offline: '尽快线下就医', followup: '复诊随访', online: '线上继续' })[value] || '继续关注' }
function triageSessionStatusLabel(value) { return ({ completed: '已完成', in_progress: '进行中', closed: '已关闭' })[value] || value || '-' }
function messageRoleLabel(value) { return ({ user: '患者', assistant: 'AI 导诊', system: '系统', rule_engine: '规则引擎' })[value] || value || '-' }
function messageTypeLabel(value) {
  return ({
    intake_summary: '问诊摘要',
    health_summary: '健康摘要',
    triage_result: '分诊结果',
    rule_summary: '规则摘要',
    rule_hit: '命中详情',
    ai_triage_summary: 'AI 导诊建议',
    ai_followup_questions: 'AI 建议补充',
    ai_user_followup: '患者补充',
    ai_chat_reply: 'AI 导诊回复'
  })[value] || value || '-'
}
function conditionLevelLabel(value) { return ({ low: '轻度', medium: '中度', high: '较高风险', critical: '危急' })[value] || '未填写' }
function dispositionLabel(value) { return ({ observe: '继续观察', online_followup: '线上随访', offline_visit: '线下就医', emergency: '立即急诊' })[value] || '未填写' }
function aiConsistencyLabel(value) { return value === 1 ? '与 AI 一致' : value === 0 ? '与 AI 不一致' : '未判断' }
function followUpTypeLabel(value) { return ({ platform: '平台随访', phone: '电话随访', offline: '线下随访', other: '其他方式' })[value] || '其他方式' }
function patientStatusLabel(value) { return ({ improved: '明显好转', stable: '基本稳定', worsened: '出现加重', other: '其他情况' })[value] || '其他情况' }
function formatConfidence(value) {
  const number = Number(value)
  return Number.isNaN(number) || number <= 0 ? '-' : `${Math.round(number * 100)}%`
}
function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}
watch(detailVisible, value => {
  if (!value) {
    detail.value = null
    consultationMessages.value = []
    messageLoading.value = false
    messageSending.value = false
    resetMessageDraft()
    syncForms()
    if (route.query.id) router.replace({ path: '/doctor/consultation' })
  }
})
watch(() => conclusionForm.needFollowUp, value => { if (value !== 1) conclusionForm.followUpWithinDays = null })
watch(() => followUpForm.needRevisit, value => { if (value !== 1) followUpForm.nextFollowUpDate = '' })
onMounted(() => { refreshAll(); loadReplyTemplates() })
</script>

<style scoped>
.doctor-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.stat {
  padding: 22px 24px;
}

.stat span,
.head p,
.assignment span {
  color: var(--app-muted);
}

.stat strong {
  display: block;
  margin-top: 14px;
  font-size: 30px;
}

.block,
.panel {
  padding: 22px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 16px;
}

.head h3,
.panel h3 {
  margin: 0;
}

.head p {
  margin: 6px 0 0;
  line-height: 1.7;
}

.toolbar,
.row-actions,
.assignment,
.chips,
.actions,
.head-actions,
.template-tools,
.template-banner {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.head-actions {
  justify-content: flex-end;
}

.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.copy {
  margin: 0;
  line-height: 1.8;
  color: #41575d;
}

.copy + .copy {
  margin-top: 10px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.subcard {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(19, 73, 80, 0.05);
}

.triage-doctor-list,
.triage-session-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.triage-doctor-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.triage-doctor-card {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.triage-doctor-avatar {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  object-fit: cover;
  border: 1px solid rgba(17, 70, 77, 0.08);
  background: rgba(15, 102, 101, 0.08);
}

.triage-doctor-avatar-fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #0f6665;
  font-size: 20px;
  font-weight: 700;
}

.triage-doctor-copy {
  flex: 1;
}

.triage-doctor-copy strong {
  display: block;
  margin-bottom: 6px;
}

.triage-doctor-copy span,
.triage-doctor-copy small {
  color: var(--app-muted);
}

.triage-session-card {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(19, 73, 80, 0.1);
  background: rgba(19, 73, 80, 0.05);
}

.triage-session-card.user {
  background: rgba(15, 102, 101, 0.12);
}

.triage-session-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.triage-session-head strong {
  display: block;
}

.triage-session-head span,
.triage-session-card small {
  color: var(--app-muted);
}

.triage-session-content {
  margin: 12px 0 8px;
  line-height: 1.8;
  color: #41575d;
  white-space: pre-wrap;
}

.triage-session-insight {
  margin: 0 0 10px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(15, 102, 101, 0.06);
  border: 1px solid rgba(15, 102, 101, 0.1);
}

.triage-session-insight-meta,
.triage-session-insight-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.triage-session-insight-meta span,
.triage-session-insight-tags span {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.1);
  color: #48656d;
  font-size: 12px;
}

.triage-session-insight-copy {
  margin: 10px 0 0;
  line-height: 1.7;
  color: #48656d;
}

.triage-session-insight-tags {
  margin-top: 10px;
}

.triage-session-insight-tags.danger span {
  background: rgba(214, 95, 80, 0.12);
  color: #9f4336;
}

.subcard strong {
  display: block;
  margin-bottom: 8px;
}

.image {
  width: 220px;
  height: 150px;
  border-radius: 18px;
  object-fit: cover;
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.chips span {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(19, 73, 80, 0.08);
  color: #27646d;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.notice {
  margin-bottom: 16px;
}

.template-banner {
  margin-bottom: 18px;
  padding: 12px 16px;
  border-radius: 18px;
  background: rgba(19, 73, 80, 0.05);
  color: #36555c;
}

.template-tools {
  margin-bottom: 10px;
}

.message-board {
  margin-bottom: 16px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;
}

.message-card {
  max-width: 82%;
  padding: 14px 16px;
  border-radius: 20px;
  border: 1px solid rgba(19, 73, 80, 0.1);
  background: rgba(19, 73, 80, 0.05);
}

.message-card.mine {
  align-self: flex-end;
  background: rgba(15, 102, 101, 0.12);
}

.message-meta,
.message-toolbar,
.message-attachment-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.message-meta span,
.message-tip,
.message-attachment-actions span {
  color: var(--app-muted);
  font-size: 13px;
}

.message-content {
  margin: 10px 0 0;
  line-height: 1.8;
  color: #41575d;
  white-space: pre-wrap;
}

.message-image-list,
.message-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}

.message-composer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-attachment-item {
  width: 140px;
  padding: 10px;
  border-radius: 18px;
  background: rgba(19, 73, 80, 0.05);
}

.message-image {
  width: 120px;
  height: 120px;
  border-radius: 16px;
  object-fit: cover;
  border: 1px solid rgba(17, 70, 77, 0.08);
}

@media (max-width: 1100px) {
  .stats,
  .grid,
  .triage-doctor-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .stats,
  .grid,
  .triage-doctor-list {
    grid-template-columns: 1fr;
  }

  .head,
  .toolbar,
  .actions,
  .head-actions,
  .message-toolbar,
  .message-meta,
  .message-attachment-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .message-card {
    max-width: 100%;
  }

  .triage-session-head {
    flex-direction: column;
  }
}
</style>
