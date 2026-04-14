<template>
  <div class="ai-config-page" v-loading="loading">
    <section class="hero-card">
      <div class="hero-copy">
        <span class="section-tag">AI Governance</span>
        <h2>AI 导诊配置与状态</h2>
        <p>{{ overview.runtimeStatus || '当前正在读取 AI 导诊运行状态。' }}</p>
        <div class="chip-row">
          <span>{{ availabilityLabel(overview.runtimeAvailable, '运行可用', '当前不可用') }}</span>
          <span>{{ availabilityLabel(overview.triageEnabled, '总开关已启用', '总开关已关闭') }}</span>
          <span>{{ availabilityLabel(overview.providerEnabled, 'DeepSeek 接入已启用', 'DeepSeek 接入未启用') }}</span>
          <span>{{ availabilityLabel(overview.apiKeyConfigured, 'API Key 已配置', 'API Key 未配置') }}</span>
          <span v-if="overview.promptVersion">Prompt {{ overview.promptVersion }}</span>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="loadOverview">刷新状态</el-button>
          <el-button @click="router.push('/admin/consultation-record')">导诊记录中心</el-button>
          <el-button plain @click="router.push('/admin/consultation-dispatch')">智能分配策略</el-button>
        </div>
      </div>

      <div class="hero-side">
        <article class="hero-metric">
          <span>AI 运行态</span>
          <strong :class="statusToneClass(overview.runtimeAvailable)">
            {{ overview.runtimeAvailable ? 'Ready' : 'Fallback' }}
          </strong>
          <p>{{ runtimeModeSummary }}</p>
          </article>
        <article class="hero-metric hero-metric-accent">
          <span>最近 AI 输出</span>
          <strong>{{ formatDate(overview.latestAiMessageTime, true) }}</strong>
          <p>{{ totalAiMessages }} 条 AI 导诊消息已经写入导诊留痕。</p>
        </article>
      </div>
    </section>

    <section class="status-grid">
      <article class="status-card">
        <span>AI 导诊总开关</span>
        <strong>{{ availabilityLabel(overview.triageEnabled) }}</strong>
        <el-tag :type="statusTagType(overview.triageEnabled)" effect="light">{{ statusTagLabel(overview.triageEnabled) }}</el-tag>
      </article>
      <article class="status-card">
        <span>DeepSeek Chat 接入</span>
        <strong>{{ availabilityLabel(overview.providerEnabled) }}</strong>
        <el-tag :type="statusTagType(overview.providerEnabled)" effect="light">{{ statusTagLabel(overview.providerEnabled) }}</el-tag>
      </article>
      <article class="status-card">
        <span>API Key</span>
        <strong>{{ availabilityLabel(overview.apiKeyConfigured) }}</strong>
        <el-tag :type="statusTagType(overview.apiKeyConfigured)" effect="light">{{ statusTagLabel(overview.apiKeyConfigured) }}</el-tag>
      </article>
      <article class="status-card">
        <span>ChatModel Bean</span>
        <strong>{{ availabilityLabel(overview.modelBeanReady) }}</strong>
        <el-tag :type="statusTagType(overview.modelBeanReady)" effect="light">{{ statusTagLabel(overview.modelBeanReady) }}</el-tag>
      </article>
    </section>

    <section class="panel-card" v-loading="configLoading">
      <div class="panel-head">
        <div>
          <h3>AI 配置中心</h3>
          <p>这里保存的是会直接影响 AI 导诊运行的业务配置。保存后，新产生的导诊摘要、追问建议和 Prompt 留痕会立即使用最新配置。</p>
        </div>
        <div class="hero-actions">
          <el-button @click="loadAiConfig">刷新配置</el-button>
          <el-button :disabled="!aiConfigDirty" @click="resetAiConfigForm">恢复已保存</el-button>
          <el-button type="primary" :loading="configSubmitting" @click="saveAiConfig">保存配置</el-button>
        </div>
      </div>

      <div class="config-grid">
        <article class="config-note">
          <strong>配置说明</strong>
          <p>当前可编辑的是 AI 导诊总开关、Prompt 版本和推荐医生候选上限。</p>
          <p>模型提供方、Base URL、温度、Token 等运行参数仍然从环境变量读取，方便区分“业务配置”和“部署配置”。</p>
          <p>最近一次保存：{{ formatDate(aiConfigSnapshot.updateTime, true) }}</p>
        </article>

        <el-form label-position="top" class="config-form-grid">
          <el-form-item label="AI 导诊总开关">
            <el-switch
              v-model="aiConfigForm.enabled"
              :active-value="1"
              :inactive-value="0"
              inline-prompt
              active-text="开"
              inactive-text="关"
            />
          </el-form-item>
          <el-form-item label="推荐医生候选上限">
            <el-input-number
              v-model="aiConfigForm.doctorCandidateLimit"
              :min="1"
              :max="10"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="Prompt 版本" class="config-form-item-full">
            <el-input
              v-model="aiConfigForm.promptVersion"
              maxlength="100"
              show-word-limit
              placeholder="例如：deepseek-triage-v2"
            />
          </el-form-item>
        </el-form>
      </div>
    </section>

    <section class="panel-card" v-loading="configHistoryLoading">
      <div class="panel-head">
        <div>
          <h3>最近配置变更</h3>
          <p>每次保存 AI 导诊配置后都会自动记录一条变更历史，方便回溯 Prompt 版本切换、开关变更和候选上限调整。</p>
        </div>
        <div class="hero-actions">
          <el-button @click="loadAiConfigHistory">刷新记录</el-button>
        </div>
      </div>

      <div v-if="configHistoryItems.length" class="config-history-list">
        <article v-for="item in configHistoryItems" :key="`ai-config-history-${item.id}`" class="config-history-item">
          <div class="chip-row audit-head-chips">
            <span>{{ item.operatorUsername || 'system' }}</span>
            <span>{{ formatDate(item.createTime, true) }}</span>
            <span>{{ configEnabledLabel(item.enabledBefore) }} -> {{ configEnabledLabel(item.enabledAfter) }}</span>
          </div>
          <p class="copy"><strong>变更摘要：</strong>{{ item.changeSummary || '本次未识别到差异摘要' }}</p>
          <div class="audit-tag-row">
            <span>Prompt：{{ item.promptVersionBefore || '未设置' }} -> {{ item.promptVersionAfter || '未设置' }}</span>
            <span>候选上限：{{ item.doctorCandidateLimitBefore ?? '未设置' }} -> {{ item.doctorCandidateLimitAfter ?? '未设置' }}</span>
            <span v-if="item.operatorAccountId">账号ID：{{ item.operatorAccountId }}</span>
          </div>
        </article>
      </div>
      <el-empty v-else :description="configHistoryLoading ? '正在加载配置变更历史。' : '当前还没有 AI 配置变更记录。'" />
    </section>

    <section class="content-grid">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>当前模型参数</h3>
            <p>这里展示当前 Spring AI + DeepSeek 已解析到的运行参数，便于排查环境差异。</p>
          </div>
        </div>

        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="模型供应商">{{ overview.providerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Prompt 版本">{{ overview.promptVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Base URL">{{ overview.providerBaseUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="模型名称">{{ overview.modelName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Temperature">{{ overview.temperature ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="Max Tokens">{{ overview.maxTokens ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="医生候选上限">{{ overview.doctorCandidateLimit ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近 AI 输出">{{ formatDate(overview.latestAiMessageTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="env-board">
          <strong>推荐检查的环境变量</strong>
          <pre>DEEPSEEK_ENABLED
DEEPSEEK_API_KEY
DEEPSEEK_MODEL
DEEPSEEK_TEMPERATURE
DEEPSEEK_MAX_TOKENS
CONSULTATION_AI_TRIAGE_ENABLED
CONSULTATION_AI_TRIAGE_PROMPT_VERSION</pre>
        </div>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>AI 导诊产出概况</h3>
            <p>快速确认 AI 已覆盖多少导诊流程，以及首轮增强与继续追问是否持续产出。</p>
          </div>
        </div>

        <div class="metric-grid">
          <article class="metric-card">
            <span>问诊记录</span>
            <strong>{{ overview.consultationCount || 0 }}</strong>
          </article>
          <article class="metric-card">
            <span>导诊会话</span>
            <strong>{{ overview.triageSessionCount || 0 }}</strong>
          </article>
          <article class="metric-card">
            <span>可继续会话</span>
            <strong>{{ overview.openSessionCount || 0 }}</strong>
          </article>
          <article class="metric-card">
            <span>导诊结果</span>
            <strong>{{ overview.triageResultCount || 0 }}</strong>
          </article>
          <article class="metric-card metric-card-accent">
            <span>AI 导诊总结</span>
            <strong>{{ overview.aiSummaryMessageCount || 0 }}</strong>
          </article>
          <article class="metric-card">
            <span>AI 追问建议</span>
            <strong>{{ overview.aiFollowupQuestionCount || 0 }}</strong>
          </article>
          <article class="metric-card">
            <span>AI 对话回复</span>
            <strong>{{ overview.aiChatReplyCount || 0 }}</strong>
          </article>
          <article class="metric-card">
            <span>患者补充追问</span>
            <strong>{{ overview.userFollowupMessageCount || 0 }}</strong>
          </article>
        </div>
      </article>
    </section>

    <section class="panel-card usage-panel" v-loading="doctorMessageUsageLoading">
      <div class="panel-head">
        <div>
          <h3>医生端 AI 沟通草稿使用</h3>
          <p>跟踪医生侧 AI 沟通草稿从生成、带入到发送的完整链路，方便后续评估场景效果和模板拼装采纳情况。</p>
        </div>
        <div class="hero-actions">
          <el-button plain @click="loadDoctorMessageUsage">刷新使用概览</el-button>
        </div>
      </div>

      <div class="metric-grid">
        <article class="metric-card metric-card-accent">
          <span>累计生成</span>
          <strong>{{ doctorMessageUsage.generatedCount || 0 }}</strong>
          <p>医生端 AI 沟通草稿总生成次数</p>
        </article>
        <article class="metric-card">
          <span>已带入</span>
          <strong>{{ doctorMessageUsage.appliedCount || 0 }}</strong>
          <p>带入率 {{ rateText(doctorMessageUsage.applyRate) }}</p>
        </article>
        <article class="metric-card">
          <span>已发送</span>
          <strong>{{ doctorMessageUsage.sentCount || 0 }}</strong>
          <p>发送采纳率 {{ rateText(doctorMessageUsage.sendAdoptionRate) }}</p>
        </article>
        <article class="metric-card">
          <span>模板拼装</span>
          <strong>{{ doctorMessageUsage.templateUsedCount || 0 }}</strong>
          <p>已出现 AI + 模板联合带入</p>
        </article>
        <article class="metric-card">
          <span>DeepSeek 生成</span>
          <strong>{{ doctorMessageUsage.deepseekCount || 0 }}</strong>
          <p>真实模型生成的沟通草稿</p>
        </article>
        <article class="metric-card">
          <span>规则兜底</span>
          <strong>{{ doctorMessageUsage.fallbackCount || 0 }}</strong>
          <p>模型不可用时的沟通草稿兜底</p>
        </article>
      </div>

      <div class="audit-summary-bar">
        <span>场景覆盖 {{ doctorMessageUsage.sceneBreakdown?.length || 0 }} 类</span>
        <span v-if="doctorMessageUsage.templateUsedCount">模板拼装 {{ doctorMessageUsage.templateUsedCount }} 次</span>
        <span v-if="doctorMessageUsage.sentCount">已形成发送采纳 {{ doctorMessageUsage.sentCount }} 条</span>
      </div>

      <div v-if="doctorMessageUsage.sceneBreakdown?.length" class="audit-tag-row">
        <span v-for="item in doctorMessageUsage.sceneBreakdown" :key="`doctor-scene-${item.sceneType}`">
          {{ item.sceneLabel }} {{ item.generatedCount }}/{{ item.appliedCount }}/{{ item.sentCount }}
        </span>
      </div>

      <div v-if="doctorMessageUsage.recentItems?.length" class="usage-list">
        <article v-for="item in doctorMessageUsage.recentItems" :key="`doctor-ai-${item.logId}`" class="audit-item queue-item">
          <div class="chip-row audit-head-chips">
            <span>{{ doctorMessageSceneLabel(item.sceneType) }}</span>
            <span>{{ item.doctorName || '未命名医生' }}</span>
            <span>{{ item.patientName || '未命名患者' }}</span>
            <span>{{ formatDate(item.generatedTime, true) }}</span>
            <span :class="item.sentStatus === 1 ? 'chip-success' : 'chip-warning'">{{ doctorMessageUsageStatusLabel(item) }}</span>
          </div>

          <p class="copy"><strong>草稿摘要：</strong>{{ item.draftSummary || item.draftContent || '当前草稿未生成摘要。' }}</p>
          <p v-if="item.sentContentPreview" class="copy"><strong>最终发送：</strong>{{ item.sentContentPreview }}</p>
          <p v-else class="copy"><strong>发送状态：</strong>当前还没有记录到医生实际发送消息。</p>

          <div class="audit-tag-row">
            <span>{{ doctorMessageSourceLabel(item) }}</span>
            <span>带入 {{ item.applyCount || 0 }} 次</span>
            <span v-if="item.lastApplyMode">{{ doctorMessageApplyModeLabel(item.lastApplyMode) }}</span>
            <span v-if="item.templateUsed === 1">{{ item.templateTitle ? `模板：${item.templateTitle}` : '已拼装模板' }}</span>
          </div>

          <div class="audit-actions">
            <el-button
              type="primary"
              plain
              size="small"
              :disabled="!item.consultationId"
              @click="goToConsultationDetail(item)"
            >
              查看问诊详情
            </el-button>
            <el-button size="small" @click="goToConsultationCenter(item)">导诊记录中心</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else :description="doctorMessageUsageLoading ? '正在加载医生端 AI 沟通草稿使用概况。' : '当前还没有医生端 AI 沟通草稿使用记录。'" />
    </section>

    <section class="panel-card usage-panel" v-loading="doctorFormUsageLoading">
      <div class="panel-head">
        <div>
          <h3>医生端 AI 处理/随访草稿使用</h3>
          <p>跟踪医生处理表单和随访表单中的 AI 草稿从生成、带入到最终保存的闭环，便于继续优化医生 AI 助理效果。</p>
        </div>
        <div class="hero-actions">
          <el-button plain @click="loadDoctorFormUsage">刷新使用概览</el-button>
        </div>
      </div>

      <div class="metric-grid">
        <article class="metric-card metric-card-accent">
          <span>累计生成</span>
          <strong>{{ doctorFormUsage.generatedCount || 0 }}</strong>
          <p>医生处理/随访 AI 草稿总生成次数</p>
        </article>
        <article class="metric-card">
          <span>整份生成</span>
          <strong>{{ doctorFormUsage.fullGeneratedCount || 0 }}</strong>
          <p>完整草稿首轮生成或刷新</p>
        </article>
        <article class="metric-card">
          <span>字段重写</span>
          <strong>{{ doctorFormUsage.fieldRegenerateCount || 0 }}</strong>
          <p>按字段聚焦重写的次数</p>
        </article>
        <article class="metric-card">
          <span>上下文续写</span>
          <strong>{{ doctorFormUsage.contextRewriteCount || 0 }}</strong>
          <p>结合当前草稿继续改写</p>
        </article>
        <article class="metric-card">
          <span>已带入</span>
          <strong>{{ doctorFormUsage.appliedCount || 0 }}</strong>
          <p>带入率 {{ rateText(doctorFormUsage.applyRate) }}</p>
        </article>
        <article class="metric-card">
          <span>纯 AI 带入</span>
          <strong>{{ doctorFormUsage.pureAiAppliedCount || 0 }}</strong>
          <p>未与模板拼装的直接采纳</p>
        </article>
        <article class="metric-card">
          <span>AI+模板拼装</span>
          <strong>{{ doctorFormUsage.templateUsedCount || 0 }}</strong>
          <p>处理/随访字段拼装带入</p>
        </article>
        <article class="metric-card">
          <span>已保存</span>
          <strong>{{ doctorFormUsage.savedCount || 0 }}</strong>
          <p>保存采纳率 {{ rateText(doctorFormUsage.saveAdoptionRate) }}</p>
        </article>
        <article class="metric-card">
          <span>续写已保存</span>
          <strong>{{ doctorFormUsage.contextRewriteSavedCount || 0 }}</strong>
          <p>续写保存率 {{ rateText(doctorFormUsage.contextRewriteSaveRate) }}</p>
        </article>
        <article class="metric-card">
          <span>DeepSeek 生成</span>
          <strong>{{ doctorFormUsage.deepseekCount || 0 }}</strong>
          <p>真实模型生成的表单草稿</p>
        </article>
        <article class="metric-card">
          <span>规则兜底</span>
          <strong>{{ doctorFormUsage.fallbackCount || 0 }}</strong>
          <p>模型不可用时的表单草稿兜底</p>
        </article>
      </div>

      <div class="audit-summary-bar">
        <span>场景覆盖 {{ doctorFormUsage.sceneBreakdown?.length || 0 }} 类</span>
        <span v-if="doctorFormUsage.fullGeneratedCount">整份生成 {{ doctorFormUsage.fullGeneratedCount }} 条</span>
        <span v-if="doctorFormUsage.fieldRegenerateCount">字段重写 {{ doctorFormUsage.fieldRegenerateCount }} 条</span>
        <span v-if="doctorFormUsage.contextRewriteCount">上下文续写 {{ doctorFormUsage.contextRewriteCount }} 条</span>
        <span v-if="doctorFormUsage.pureAiAppliedCount">纯 AI 带入 {{ doctorFormUsage.pureAiAppliedCount }} 条</span>
        <span v-if="doctorFormUsage.templateUsedCount">AI+模板拼装 {{ doctorFormUsage.templateUsedCount }} 条</span>
        <span v-if="doctorFormUsage.savedCount">已形成表单保存采纳 {{ doctorFormUsage.savedCount }} 条</span>
      </div>

      <div v-if="doctorFormUsage.sceneBreakdown?.length" class="audit-tag-row">
        <span v-for="item in doctorFormUsage.sceneBreakdown" :key="`doctor-form-scene-${item.sceneType}`">
          {{ item.sceneLabel }} 生成{{ item.generatedCount }} / 整份{{ item.fullGeneratedCount || 0 }} / 重写{{ item.fieldRegenerateCount || 0 }} / 纯AI{{ item.pureAiAppliedCount || 0 }} / 拼装{{ item.templateUsedCount || 0 }} / 保存{{ item.savedCount }}
        </span>
      </div>

      <div v-if="doctorFormUsage.promptBreakdown?.length" class="field-breakdown-panel">
        <div class="field-breakdown-head">
          <div>
            <h4>Prompt 版本与续写效果</h4>
            <p>观察不同 Prompt 版本在整份生成、字段重写和基于当前草稿续写场景中的真实保存效果。</p>
          </div>
          <div class="audit-toolbar field-breakdown-toolbar">
            <el-select v-model="doctorFormPromptSortKey" size="small" style="width: 180px">
              <el-option
                v-for="option in doctorFormPromptSortOptions"
                :key="`doctor-form-prompt-sort-${option.value}`"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </div>
        </div>

        <div v-if="displayDoctorFormPromptBreakdown.length" class="metric-grid">
          <article v-for="item in displayDoctorFormPromptBreakdown" :key="`doctor-form-prompt-${item.promptVersion}`" class="metric-card">
            <span>{{ doctorFormPromptVersionLabel(item.promptVersion) }}</span>
            <strong>{{ doctorFormPromptPrimaryText(item) }}</strong>
            <p>{{ doctorFormPromptSortLabel(doctorFormPromptSortKey) }} | 生成 {{ item.generatedCount || 0 }}</p>
            <div class="audit-tag-row">
              <span>整份 {{ item.fullGeneratedCount || 0 }}</span>
              <span>重写 {{ item.fieldRegenerateCount || 0 }}</span>
              <span>续写 {{ item.contextRewriteCount || 0 }}</span>
              <span>带入 {{ item.appliedCount || 0 }} / {{ rateText(item.applyRate) }}</span>
              <span>保存 {{ item.savedCount || 0 }} / {{ rateText(item.saveRate) }}</span>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前还没有 Prompt 版本治理记录。" />
      </div>

      <div v-if="doctorFormUsage.fieldBreakdown?.length" class="field-breakdown-panel">
        <div class="field-breakdown-head">
          <div>
            <h4>字段治理排行</h4>
            <p>按字段重写、模板拼装、带入和保存链路观察 AI 草稿的真实落地情况。</p>
          </div>
          <div class="audit-toolbar field-breakdown-toolbar">
            <el-select v-model="doctorFormFieldSceneFilter" size="small" style="width: 140px">
              <el-option
                v-for="option in doctorFormFieldSceneOptions"
                :key="`doctor-form-field-scene-${option.value}`"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-select v-model="doctorFormFieldSortKey" size="small" style="width: 180px">
              <el-option
                v-for="option in doctorFormFieldSortOptions"
                :key="`doctor-form-field-sort-${option.value}`"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </div>
        </div>

        <div v-if="displayDoctorFormFieldBreakdown.length" class="metric-grid">
          <article v-for="item in displayDoctorFormFieldBreakdown" :key="`doctor-form-field-${item.fieldKey}`" class="metric-card">
            <span>{{ item.fieldLabel }}</span>
            <strong>{{ doctorFormFieldPrimaryText(item) }}</strong>
            <p>{{ doctorFormFieldSortLabel(doctorFormFieldSortKey) }} | {{ item.sceneLabel }} | 相关 {{ item.relatedCount || 0 }}</p>
            <div class="audit-tag-row">
              <span>重写 {{ item.regenerateCount || 0 }}</span>
              <span>拼装 {{ item.templateComposeCount || 0 }}</span>
              <span>续写 {{ item.contextRewriteCount || 0 }} / {{ rateText(item.contextRewriteSaveRate) }}</span>
              <span>带入 {{ item.appliedCount || 0 }} / {{ rateText(item.applyRate) }}</span>
              <span>保存 {{ item.savedCount || 0 }} / {{ rateText(item.saveRate) }}</span>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前筛选条件下暂无字段治理记录。" />
      </div>

      <div v-if="doctorFormUsage.templateBreakdown?.length" class="field-breakdown-panel">
        <div class="field-breakdown-head">
          <div>
            <h4>字段模板拼装排行</h4>
            <p>下钻到字段与模板标题的组合，识别哪条模板最常参与医生表单拼装。</p>
          </div>
          <div class="audit-toolbar field-breakdown-toolbar">
            <el-select v-model="doctorFormTemplateFieldFilter" size="small" style="width: 150px">
              <el-option
                v-for="option in doctorFormTemplateFieldOptions"
                :key="`doctor-form-template-field-${option.value}`"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-select v-model="doctorFormTemplateSortKey" size="small" style="width: 180px">
              <el-option
                v-for="option in doctorFormTemplateSortOptions"
                :key="`doctor-form-template-sort-${option.value}`"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </div>
        </div>

        <div v-if="displayDoctorFormTemplateBreakdown.length" class="usage-list template-breakdown-list">
          <article v-for="item in displayDoctorFormTemplateBreakdown" :key="`doctor-form-template-${item.fieldKey}-${item.templateId || item.templateLabel}`" class="audit-item queue-item">
            <div class="chip-row audit-head-chips">
              <span>{{ item.fieldLabel }}</span>
              <span>{{ item.sceneLabel }}</span>
              <span>{{ item.templateLabel }}</span>
            </div>
            <p class="copy"><strong>{{ doctorFormTemplateSortLabel(doctorFormTemplateSortKey) }}：</strong>{{ doctorFormTemplatePrimaryText(item) }}</p>
            <div class="audit-tag-row">
              <span>拼装 {{ item.composeCount || 0 }}</span>
              <span>续写 {{ item.contextRewriteCount || 0 }} / {{ rateText(item.contextRewriteSaveRate) }}</span>
              <span>带入 {{ item.appliedCount || 0 }} / {{ rateText(item.applyRate) }}</span>
              <span>保存 {{ item.savedCount || 0 }} / {{ rateText(item.saveRate) }}</span>
              <span v-if="item.templateId">模板ID {{ item.templateId }}</span>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前筛选条件下暂无字段模板拼装记录。" />
      </div>

      <div v-if="doctorFormUsage.recentItems?.length" class="usage-list">
        <article v-for="item in doctorFormUsage.recentItems" :key="`doctor-form-ai-${item.logId}`" class="audit-item queue-item">
          <div class="chip-row audit-head-chips">
            <span>{{ doctorFormSceneLabel(item.sceneType) }}</span>
            <span>{{ item.doctorName || '未命名医生' }}</span>
            <span>{{ item.patientName || '未命名患者' }}</span>
            <span>{{ formatDate(item.generatedTime, true) }}</span>
            <span :class="item.savedStatus === 1 ? 'chip-success' : 'chip-warning'">{{ doctorFormUsageStatusLabel(item) }}</span>
          </div>

          <p class="copy"><strong>草稿摘要：</strong>{{ item.draftSummary || item.draftPreview || '当前草稿未生成摘要。' }}</p>
          <p v-if="item.rewriteRequirement" class="copy"><strong>改写要求：</strong>{{ abbreviateText(item.rewriteRequirement, 160) }}</p>
          <p v-if="item.savedPreview" class="copy"><strong>最终保存：</strong>{{ item.savedPreview }}</p>
          <p v-else class="copy"><strong>保存状态：</strong>当前还没有记录到医生实际保存处理或随访表单。</p>

          <div class="audit-tag-row">
            <span>{{ doctorFormGenerationLabel(item) }}</span>
            <span>{{ doctorFormSourceLabel(item) }}</span>
            <span v-if="item.promptVersion">Prompt {{ doctorFormPromptVersionLabel(item.promptVersion) }}</span>
            <span v-if="item.draftContextUsed === 1">基于当前草稿续写</span>
            <span>带入 {{ item.applyCount || 0 }} 次</span>
            <span v-if="item.lastApplyMode">{{ doctorFormApplyModeLabel(item.lastApplyMode) }}</span>
            <span v-if="item.lastApplyTarget">{{ doctorFormApplyTargetLabel(item.lastApplyTarget) }}</span>
            <span v-if="item.templateUsed === 1">{{ item.templateTitle ? `模板：${item.templateTitle}` : '已拼装模板' }}</span>
          </div>

          <div class="audit-actions">
            <el-button
              type="primary"
              plain
              size="small"
              :disabled="!item.consultationId"
              @click="goToConsultationDetail(item)"
            >
              查看问诊详情
            </el-button>
            <el-button size="small" @click="goToConsultationCenter(item)">导诊记录中心</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else :description="doctorFormUsageLoading ? '正在加载医生端 AI 处理/随访草稿使用概况。' : '当前还没有医生端 AI 处理/随访草稿使用记录。'" />
    </section>

    <section class="panel-card queue-panel" v-loading="queueLoading">
      <div class="panel-head">
        <div>
          <h3>高风险待复核队列</h3>
          <p>优先聚合仍未完成医生复核，或医生已明确标记与 AI 存在差异的高风险样本，方便管理员先盯重点。</p>
        </div>
        <div class="audit-toolbar">
          <el-input
            v-model="reviewQueueKeyword"
            clearable
            placeholder="搜索单号、患者、分类或科室"
            style="width: 240px"
            @keyup.enter="loadReviewQueue"
          />
          <el-button type="primary" plain @click="loadReviewQueue">筛选</el-button>
          <el-button @click="resetReviewQueueFilters">重置</el-button>
          <el-button plain :loading="queueExporting" @click="exportReviewQueue">Export CSV</el-button>
        </div>
      </div>

      <div class="audit-summary-bar">
        <span>当前待复核 {{ reviewQueueItems.length }} 条</span>
        <span>待医生接手 {{ queuePendingTakeoverCount }} 条</span>
        <span>待形成结论 {{ queuePendingConclusionCount }} 条</span>
        <span v-if="queueMismatchCount">医生判定有差异 {{ queueMismatchCount }} 条</span>
      </div>

      <div v-if="reviewQueueItems.length" class="queue-list">
        <article v-for="item in reviewQueueItems" :key="`queue-${item.messageId}`" class="audit-item queue-item">
          <div class="chip-row audit-head-chips">
            <span>{{ item.patientName || '未命名患者' }}</span>
            <span>{{ item.consultationNo || item.sessionNo || '未生成单号' }}</span>
            <span>{{ item.categoryName || '未分类' }}</span>
            <span>{{ formatDate(item.createTime, true) }}</span>
            <span class="chip-danger">{{ reviewQueueStatusLabel(item) }}</span>
          </div>

          <p class="copy"><strong>高风险信号：</strong>{{ reviewQueueReasonText(item) }}</p>
          <p class="copy"><strong>AI 判断：</strong>{{ queueAiSummaryText(item) }}</p>
          <p class="copy"><strong>当前处理：</strong>{{ reviewQueueProgressText(item) }}</p>
          <p v-if="item.doctorReview?.compareText" class="copy"><strong>对比结果：</strong>{{ item.doctorReview.compareText }}</p>

          <div v-if="reviewQueueTags(item).length" class="audit-tag-row danger">
            <span v-for="tag in reviewQueueTags(item)" :key="`${item.messageId}-${tag}`">{{ tag }}</span>
          </div>
          <div class="audit-actions">
            <el-button
              type="primary"
              plain
              size="small"
              :disabled="!item.consultationId"
              @click="goToConsultationDetail(item)"
            >
              立即复盘
            </el-button>
            <el-button
              size="small"
              @click="goToConsultationCenter(item)"
            >
              导诊记录中心
            </el-button>
          </div>
        </article>
      </div>
      <el-empty v-else :description="queueEmptyDescription" />
    </section>

    <section class="panel-card audit-panel" v-loading="auditLoading">
      <div class="panel-head">
        <div>
          <h3>最近 AI 导诊输出审计</h3>
          <p>从导诊消息留痕中直接抽样最近 AI 输出，方便复盘 Prompt 版本、风险提示与推荐结果是否稳定。</p>
        </div>
        <div class="audit-toolbar">
          <el-input
            v-model="auditKeyword"
            clearable
            placeholder="搜索单号、患者、分类或科室"
            style="width: 240px"
            @keyup.enter="loadAuditList"
          />
          <el-select v-model="auditMessageType" style="width: 200px" @change="loadAuditList">
            <el-option
              v-for="item in auditMessageOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-switch
            v-model="highRiskOnly"
            inline-prompt
            active-text="高风险"
            inactive-text="全部"
            @change="loadAuditList"
          />
          <el-button type="primary" plain @click="loadAuditList">筛选</el-button>
          <el-button @click="resetAuditFilters">重置</el-button>
          <el-button plain :loading="auditExporting" @click="exportAuditList">Export CSV</el-button>
        </div>
      </div>

      <div class="audit-summary-bar">
        <span>当前抽样 {{ auditItems.length }} 条</span>
        <span>高风险关注 {{ highRiskAuditCount }} 条</span>
        <span>医生已复核 {{ reviewedAuditCount }} 条</span>
        <span v-if="reviewMismatchCount">存在差异 {{ reviewMismatchCount }} 条</span>
        <span v-if="highRiskOnly">已切换为仅显示高风险输出</span>
      </div>

      <div v-if="displayAuditItems.length" class="audit-list">
        <article v-for="item in displayAuditItems" :key="item.messageId" class="audit-item">
          <div class="chip-row audit-head-chips">
            <span>{{ auditTypeLabel(item.messageType) }}</span>
            <span>{{ item.patientName || '未命名患者' }}</span>
            <span>{{ item.consultationNo || item.sessionNo || '未生成单号' }}</span>
            <span>{{ item.categoryName || '未分类' }}</span>
            <span>{{ formatDate(item.createTime, true) }}</span>
            <span v-if="isHighRiskAuditItem(item)" class="chip-danger">高风险关注</span>
          </div>

          <p class="copy"><strong>问诊上下文：</strong>{{ auditContextText(item) }}</p>
          <p v-if="item.insight?.summary" class="copy"><strong>AI 总结：</strong>{{ item.insight.summary }}</p>
          <p v-if="showAuditReply(item)" class="copy"><strong>AI 回复：</strong>{{ item.insight.reply }}</p>
          <p v-if="item.insight?.nextQuestions?.length" class="copy"><strong>建议补充：</strong>{{ item.insight.nextQuestions.join('；') }}</p>
          <p v-else-if="showAuditContent(item)" class="copy"><strong>消息内容：</strong>{{ abbreviateText(item.content, 220) }}</p>
          <p v-if="item.insight?.doctorRecommendationReason" class="copy"><strong>推荐依据：</strong>{{ item.insight.doctorRecommendationReason }}</p>

          <div v-if="auditMetaTags(item).length" class="audit-tag-row">
            <span v-for="tag in auditMetaTags(item)" :key="tag">{{ tag }}</span>
          </div>
          <div v-if="item.insight?.recommendedDoctors?.length" class="audit-tag-row">
            <span v-for="doctor in item.insight.recommendedDoctors" :key="`${item.messageId}-${doctor}`">推荐 {{ doctor }}</span>
          </div>
          <div v-if="item.insight?.riskFlags?.length" class="audit-tag-row danger">
            <span v-for="flag in item.insight.riskFlags" :key="`${item.messageId}-${flag}`">{{ flag }}</span>
          </div>
          <div v-if="item.doctorReview" class="audit-review-card">
            <div class="audit-review-head">
              <strong>医生复盘</strong>
              <div class="audit-review-chips">
                <span v-if="item.doctorReview.handleDoctorName">{{ item.doctorReview.handleDoctorName }}</span>
                <span>{{ item.doctorReview.progressLabel }}</span>
                <span
                  v-if="item.doctorReview.compareOverallStatus"
                  :class="['compare-badge', comparisonStatusClass(item.doctorReview.compareOverallStatus)]"
                >
                  {{ comparisonStatusLabel(item.doctorReview.compareOverallStatus) }}
                </span>
                <span v-if="item.doctorReview.aiConsistencyLabel">{{ item.doctorReview.aiConsistencyLabel }}</span>
              </div>
            </div>
            <p class="copy"><strong>处理进度：</strong>{{ item.doctorReview.progressText }}</p>
            <p v-if="item.doctorReview.doctorConclusionText" class="copy"><strong>医生结论：</strong>{{ item.doctorReview.doctorConclusionText }}</p>
            <p v-if="item.doctorReview.compareText" class="copy"><strong>对比结果：</strong>{{ item.doctorReview.compareText }}</p>
            <div v-if="item.doctorReview.mismatchReasonLabels.length" class="audit-tag-row danger">
              <span v-for="tag in item.doctorReview.mismatchReasonLabels" :key="`${item.messageId}-${tag}`">{{ tag }}</span>
            </div>
            <p v-if="item.doctorReview.mismatchRemark" class="copy"><strong>差异说明：</strong>{{ item.doctorReview.mismatchRemark }}</p>
          </div>
          <div class="audit-actions">
            <el-button
              type="primary"
              plain
              size="small"
              :disabled="!item.consultationId"
              @click="goToConsultationDetail(item)"
            >
              查看问诊详情
            </el-button>
            <el-button
              size="small"
              @click="goToConsultationCenter(item)"
            >
              导诊记录中心
            </el-button>
          </div>
        </article>
      </div>
      <el-empty v-else :description="auditEmptyDescription" />
    </section>

    <section class="content-grid">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>运行告警</h3>
            <p>如果当前环境仍未真正启用 AI，这里会直接指出最可能的阻塞项。</p>
          </div>
        </div>

        <div v-if="overview.warnings?.length" class="warning-list">
          <el-alert
            v-for="item in overview.warnings"
            :key="item"
            :title="item"
            type="warning"
            :closable="false"
          />
        </div>
        <el-empty v-else description="当前没有额外运行告警，AI 导诊配置看起来已经具备基本可用条件。" />
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <h3>治理建议</h3>
            <p>当前页面先聚焦“可见性”和“排障效率”，后续可以在这个入口继续扩展为 Prompt 版本与审计中心。</p>
          </div>
        </div>

        <div class="guideline-list">
          <article class="guideline-item">
            <strong>先确认运行态，再看业务效果</strong>
            <p>如果 DeepSeek 接入未启用或 API Key 未配置，前台仍会继续走规则分诊兜底，所以要先确认这里的运行状态。</p>
          </article>
          <article class="guideline-item">
            <strong>结合导诊记录中心复盘输出质量</strong>
            <p>当 AI 已可用后，建议继续到“导诊记录中心”查看 AI 与医生结论偏差、用户反馈与命中规则留痕。</p>
          </article>
          <article class="guideline-item">
            <strong>智能分配策略与 AI 导诊要一起看</strong>
            <p>Prompt 版本、候选医生上限和智能分配权重会一起影响最终推荐效果，建议配合“智能分配策略”页共同调整。</p>
          </article>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { download, get, post } from '@/net'
import { aiMismatchReasonLabel, comparisonStatusClass, comparisonStatusLabel } from '@/triage/comparison'
import { resolveTriageMessageAuditInsight } from '@/triage/insight'

const router = useRouter()
const loading = ref(false)
const configLoading = ref(false)
const configHistoryLoading = ref(false)
const queueLoading = ref(false)
const auditLoading = ref(false)
const doctorMessageUsageLoading = ref(false)
const doctorFormUsageLoading = ref(false)
const queueExporting = ref(false)
const auditExporting = ref(false)
const configSubmitting = ref(false)
const overview = ref(createEmptyOverview())
const aiConfigForm = reactive(createEmptyAiConfigForm())
const aiConfigSnapshot = reactive(createEmptyAiConfigForm())
const configHistoryItems = ref([])
const doctorMessageUsage = ref(createEmptyDoctorMessageUsageOverview())
const doctorFormUsage = ref(createEmptyDoctorFormUsageOverview())
const doctorFormFieldSceneFilter = ref('all')
const doctorFormPromptSortKey = ref('generatedCount')
const doctorFormFieldSortKey = ref('relatedCount')
const doctorFormTemplateFieldFilter = ref('all')
const doctorFormTemplateSortKey = ref('composeCount')
const auditMessageType = ref('all')
const auditKeyword = ref('')
const highRiskOnly = ref(false)
const reviewQueueKeyword = ref('')
const reviewQueueItems = ref([])
const auditItems = ref([])

const REVIEW_QUEUE_LIMIT = 6
const AUDIT_LIMIT = 12
const auditMessageOptions = [
  { label: '全部 AI 输出', value: 'all' },
  { label: 'AI 导诊总结', value: 'ai_triage_summary' },
  { label: 'AI 追问建议', value: 'ai_followup_questions' },
  { label: 'AI 对话回复', value: 'ai_chat_reply' }
]

const doctorFormFieldSortOptions = [
  { label: '按相关日志数', value: 'relatedCount' },
  { label: '按字段重写数', value: 'regenerateCount' },
  { label: '按模板拼装数', value: 'templateComposeCount' },
  { label: '按上下文续写数', value: 'contextRewriteCount' },
  { label: '按带入次数', value: 'appliedCount' },
  { label: '按保存次数', value: 'savedCount' },
  { label: '按带入率', value: 'applyRate' },
  { label: '按保存率', value: 'saveRate' },
  { label: '按续写保存率', value: 'contextRewriteSaveRate' }
]
const doctorFormPromptSortOptions = [
  { label: '按生成次数', value: 'generatedCount' },
  { label: '按上下文续写数', value: 'contextRewriteCount' },
  { label: '按字段重写数', value: 'fieldRegenerateCount' },
  { label: '按带入次数', value: 'appliedCount' },
  { label: '按保存次数', value: 'savedCount' },
  { label: '按带入率', value: 'applyRate' },
  { label: '按保存率', value: 'saveRate' }
]
const doctorFormTemplateSortOptions = [
  { label: '按拼装次数', value: 'composeCount' },
  { label: '按上下文续写数', value: 'contextRewriteCount' },
  { label: '按保存次数', value: 'savedCount' },
  { label: '按保存率', value: 'saveRate' },
  { label: '按带入次数', value: 'appliedCount' },
  { label: '按续写保存率', value: 'contextRewriteSaveRate' }
]

const totalAiMessages = computed(() => Number(overview.value.aiSummaryMessageCount || 0)
  + Number(overview.value.aiFollowupQuestionCount || 0)
  + Number(overview.value.aiChatReplyCount || 0))
const aiConfigDirty = computed(() => Number(aiConfigForm.enabled || 0) !== Number(aiConfigSnapshot.enabled || 0)
  || `${aiConfigForm.promptVersion || ''}` !== `${aiConfigSnapshot.promptVersion || ''}`
  || Number(aiConfigForm.doctorCandidateLimit || 0) !== Number(aiConfigSnapshot.doctorCandidateLimit || 0))
const displayAuditItems = computed(() => auditItems.value.filter(item => !highRiskOnly.value || isHighRiskAuditItem(item)))
const highRiskAuditCount = computed(() => auditItems.value.filter(item => isHighRiskAuditItem(item)).length)
const reviewedAuditCount = computed(() => auditItems.value.filter(item => item.doctorReview?.hasConclusion).length)
const reviewMismatchCount = computed(() => auditItems.value.filter(item => item.doctorReview?.isMismatch).length)
const queuePendingTakeoverCount = computed(() => reviewQueueItems.value.filter(item => !item.doctorHandle && !item.doctorConclusion).length)
const queuePendingConclusionCount = computed(() => reviewQueueItems.value.filter(item => !!item.doctorHandle && !item.doctorConclusion).length)
const queueMismatchCount = computed(() => reviewQueueItems.value.filter(item => item.doctorReview?.isMismatch).length)
const doctorFormFieldSceneOptions = computed(() => {
  const scenes = Array.from(new Set((doctorFormUsage.value.fieldBreakdown || [])
    .map(item => `${item?.sceneType || ''}`.trim().toLowerCase())
    .filter(Boolean)))
  return [
    { label: '全部场景', value: 'all' },
    ...scenes.map(sceneType => ({
      label: doctorFormSceneLabel(sceneType),
      value: sceneType
    }))
  ]
})
const displayDoctorFormPromptBreakdown = computed(() => {
  const metricKey = `${doctorFormPromptSortKey.value || 'generatedCount'}`.trim()
  return (doctorFormUsage.value.promptBreakdown || [])
    .slice()
    .sort((left, right) => {
      const compare = Number(doctorFormPromptMetricValue(right, metricKey) || 0) - Number(doctorFormPromptMetricValue(left, metricKey) || 0)
      if (compare !== 0) return compare
      const generatedCompare = Number(right?.generatedCount || 0) - Number(left?.generatedCount || 0)
      if (generatedCompare !== 0) return generatedCompare
      return `${left?.promptVersion || ''}`.localeCompare(`${right?.promptVersion || ''}`, 'zh-CN')
    })
})
const displayDoctorFormFieldBreakdown = computed(() => {
  const selectedScene = `${doctorFormFieldSceneFilter.value || 'all'}`.trim().toLowerCase()
  const metricKey = `${doctorFormFieldSortKey.value || 'relatedCount'}`.trim()
  return (doctorFormUsage.value.fieldBreakdown || [])
    .filter(item => selectedScene === 'all' || `${item?.sceneType || ''}`.trim().toLowerCase() === selectedScene)
    .slice()
    .sort((left, right) => {
      const compare = Number(doctorFormFieldMetricValue(right, metricKey) || 0) - Number(doctorFormFieldMetricValue(left, metricKey) || 0)
      if (compare !== 0) return compare
      const relatedCompare = Number(right?.relatedCount || 0) - Number(left?.relatedCount || 0)
      if (relatedCompare !== 0) return relatedCompare
      return `${left?.fieldLabel || ''}`.localeCompare(`${right?.fieldLabel || ''}`, 'zh-CN')
    })
})
const doctorFormTemplateFieldOptions = computed(() => {
  const selectedScene = `${doctorFormFieldSceneFilter.value || 'all'}`.trim().toLowerCase()
  const fieldMap = new Map()
  ;(doctorFormUsage.value.templateBreakdown || []).forEach(item => {
    const sceneType = `${item?.sceneType || ''}`.trim().toLowerCase()
    if (selectedScene !== 'all' && sceneType !== selectedScene) return
    const fieldKey = `${item?.fieldKey || ''}`.trim().toLowerCase()
    if (!fieldKey || fieldMap.has(fieldKey)) return
    fieldMap.set(fieldKey, item?.fieldLabel || doctorFormRegenerateFieldLabel(fieldKey) || fieldKey)
  })
  return [
    { label: '全部字段', value: 'all' },
    ...Array.from(fieldMap.entries()).map(([value, label]) => ({ label, value }))
  ]
})
const displayDoctorFormTemplateBreakdown = computed(() => {
  const selectedScene = `${doctorFormFieldSceneFilter.value || 'all'}`.trim().toLowerCase()
  const selectedField = `${doctorFormTemplateFieldFilter.value || 'all'}`.trim().toLowerCase()
  const metricKey = `${doctorFormTemplateSortKey.value || 'composeCount'}`.trim()
  return (doctorFormUsage.value.templateBreakdown || [])
    .filter(item => selectedScene === 'all' || `${item?.sceneType || ''}`.trim().toLowerCase() === selectedScene)
    .filter(item => selectedField === 'all' || `${item?.fieldKey || ''}`.trim().toLowerCase() === selectedField)
    .slice()
    .sort((left, right) => {
      const compare = Number(doctorFormTemplateMetricValue(right, metricKey) || 0) - Number(doctorFormTemplateMetricValue(left, metricKey) || 0)
      if (compare !== 0) return compare
      const savedCompare = Number(right?.savedCount || 0) - Number(left?.savedCount || 0)
      if (savedCompare !== 0) return savedCompare
      const composeCompare = Number(right?.composeCount || 0) - Number(left?.composeCount || 0)
      if (composeCompare !== 0) return composeCompare
      return `${left?.templateLabel || ''}`.localeCompare(`${right?.templateLabel || ''}`, 'zh-CN')
    })
})
const auditEmptyDescription = computed(() => {
  if (auditLoading.value) return '正在加载最近 AI 导诊输出。'
  if (auditKeyword.value.trim()) return '当前关键词和筛选条件下还没有最近 AI 导诊输出。'
  if (highRiskOnly.value) return '当前抽样范围内还没有命中高风险提示的 AI 输出。'
  return '当前筛选条件下还没有最近 AI 导诊输出。'
})
const queueEmptyDescription = computed(() => {
  if (queueLoading.value) return '正在加载高风险待复核队列。'
  if (reviewQueueKeyword.value.trim()) return '当前关键词下还没有命中的高风险待复核样本。'
  return '当前最近高风险 AI 输出已基本完成医生复核，暂无额外待处理样本。'
})

const runtimeModeSummary = computed(() => {
  if (overview.value.runtimeAvailable) {
    return '当前环境已具备 AI 导诊运行条件，患者提交问诊后可生成 AI 补充建议，并支持继续追问。'
  }
  if (!overview.value.triageEnabled) {
    return '当前总开关已关闭，系统会继续使用规则分诊和现有问诊链路。'
  }
  return '当前环境仍处于规则兜底模式，建议优先排查告警项。'
})

watch(doctorFormFieldSceneFilter, () => {
  doctorFormTemplateFieldFilter.value = 'all'
})

function createEmptyOverview() {
  return {
    providerName: 'DeepSeek',
    providerBaseUrl: '',
    modelName: '',
    temperature: null,
    maxTokens: null,
    promptVersion: '',
    doctorCandidateLimit: 0,
    triageEnabled: false,
    providerEnabled: false,
    apiKeyConfigured: false,
    modelBeanReady: false,
    runtimeAvailable: false,
    runtimeStatus: '',
    warnings: [],
    consultationCount: 0,
    triageSessionCount: 0,
    openSessionCount: 0,
    triageResultCount: 0,
    aiSummaryMessageCount: 0,
    aiFollowupQuestionCount: 0,
    aiChatReplyCount: 0,
    userFollowupMessageCount: 0,
    latestAiMessageTime: null
  }
}

function createEmptyAiConfigForm() {
  return {
    enabled: 1,
    promptVersion: 'deepseek-triage-v1',
    doctorCandidateLimit: 3,
    updateTime: null
  }
}

function applyAiConfigForm(data) {
  const next = {
    ...createEmptyAiConfigForm(),
    ...(data || {})
  }
  aiConfigForm.enabled = Number(next.enabled ?? 1) === 0 ? 0 : 1
  aiConfigForm.promptVersion = `${next.promptVersion || ''}`.trim()
  aiConfigForm.doctorCandidateLimit = Number(next.doctorCandidateLimit || 3)
  aiConfigForm.updateTime = next.updateTime || null
}

function syncAiConfigSnapshot(data) {
  const next = {
    ...createEmptyAiConfigForm(),
    ...(data || {})
  }
  aiConfigSnapshot.enabled = Number(next.enabled ?? 1) === 0 ? 0 : 1
  aiConfigSnapshot.promptVersion = `${next.promptVersion || ''}`.trim()
  aiConfigSnapshot.doctorCandidateLimit = Number(next.doctorCandidateLimit || 3)
  aiConfigSnapshot.updateTime = next.updateTime || null
}

function createEmptyDoctorMessageUsageOverview() {
  return {
    generatedCount: 0,
    appliedCount: 0,
    sentCount: 0,
    templateUsedCount: 0,
    deepseekCount: 0,
    fallbackCount: 0,
    applyRate: 0,
    sendAdoptionRate: 0,
    sceneBreakdown: [],
    recentItems: []
  }
}

function createEmptyDoctorFormUsageOverview() {
  return {
    generatedCount: 0,
    fullGeneratedCount: 0,
    fieldRegenerateCount: 0,
    contextRewriteCount: 0,
    contextRewriteSavedCount: 0,
    appliedCount: 0,
    pureAiAppliedCount: 0,
    templateUsedCount: 0,
    savedCount: 0,
    deepseekCount: 0,
    fallbackCount: 0,
    applyRate: 0,
    saveAdoptionRate: 0,
    contextRewriteSaveRate: 0,
    sceneBreakdown: [],
    promptBreakdown: [],
    fieldBreakdown: [],
    templateBreakdown: [],
    recentItems: []
  }
}

function loadAiConfig() {
  configLoading.value = true
  get('/api/admin/consultation-ai/config', data => {
    applyAiConfigForm(data)
    syncAiConfigSnapshot(data)
    configLoading.value = false
  }, message => {
    configLoading.value = false
    ElMessage.warning(message || 'AI 配置加载失败')
  })
}

function resetAiConfigForm() {
  applyAiConfigForm(aiConfigSnapshot)
}

function loadAiConfigHistory() {
  configHistoryLoading.value = true
  get('/api/admin/consultation-ai/config-history?limit=8', data => {
    configHistoryItems.value = Array.isArray(data) ? data : []
    configHistoryLoading.value = false
  }, message => {
    configHistoryLoading.value = false
    ElMessage.warning(message || 'AI 配置变更历史加载失败')
  })
}

function saveAiConfig() {
  const payload = {
    enabled: Number(aiConfigForm.enabled || 0) === 0 ? 0 : 1,
    promptVersion: `${aiConfigForm.promptVersion || ''}`.trim(),
    doctorCandidateLimit: Number(aiConfigForm.doctorCandidateLimit || 0)
  }
  if (!payload.promptVersion) {
    ElMessage.warning('请输入 Prompt 版本')
    return
  }
  configSubmitting.value = true
  post('/api/admin/consultation-ai/config', payload, () => {
    configSubmitting.value = false
    ElMessage.success('AI 配置已保存')
    loadAiConfig()
    loadAiConfigHistory()
    loadOverview()
  }, message => {
    configSubmitting.value = false
    ElMessage.warning(message || 'AI 配置保存失败')
  })
}

function configEnabledLabel(value) {
  return Number(value ?? 0) === 0 ? '关闭' : '开启'
}

function loadOverview() {
  loading.value = true
  get('/api/admin/consultation-ai/overview', data => {
    overview.value = {
      ...createEmptyOverview(),
      ...(data || {})
    }
    loading.value = false
  }, message => {
    loading.value = false
    ElMessage.warning(message || 'AI 导诊运行概览加载失败')
  })
}

function loadDoctorMessageUsage() {
  doctorMessageUsageLoading.value = true
  get('/api/admin/consultation-ai/doctor-message-usage', data => {
    doctorMessageUsage.value = {
      ...createEmptyDoctorMessageUsageOverview(),
      ...(data || {})
    }
    doctorMessageUsageLoading.value = false
  }, message => {
    doctorMessageUsageLoading.value = false
    ElMessage.warning(message || '医生端 AI 沟通草稿使用概况加载失败')
  })
}

function loadDoctorFormUsage() {
  doctorFormUsageLoading.value = true
  get('/api/admin/consultation-ai/doctor-form-usage', data => {
    doctorFormUsage.value = {
      ...createEmptyDoctorFormUsageOverview(),
      ...(data || {})
    }
    doctorFormUsageLoading.value = false
  }, message => {
    doctorFormUsageLoading.value = false
    ElMessage.warning(message || '医生端 AI 处理/随访草稿使用概况加载失败')
  })
}

function normalizeAuditItem(item) {
  const insight = resolveTriageMessageAuditInsight(item)
  return {
    ...item,
    insight,
    doctorReview: resolveDoctorReview(item, insight)
  }
}

function buildAuditQuery(includeLimit = true) {
  const query = [
    `messageType=${encodeURIComponent(auditMessageType.value)}`,
    `highRiskOnly=${highRiskOnly.value ? 'true' : 'false'}`
  ]
  if (auditKeyword.value.trim()) {
    query.push(`keyword=${encodeURIComponent(auditKeyword.value.trim())}`)
  }
  if (includeLimit) {
    query.push(`limit=${AUDIT_LIMIT}`)
  }
  return query.join('&')
}

function buildReviewQueueQuery(includeLimit = true) {
  const query = []
  if (reviewQueueKeyword.value.trim()) {
    query.push(`keyword=${encodeURIComponent(reviewQueueKeyword.value.trim())}`)
  }
  if (includeLimit) {
    query.push(`limit=${REVIEW_QUEUE_LIMIT}`)
  }
  return query.join('&')
}

function loadReviewQueue() {
  queueLoading.value = true
  get(`/api/admin/consultation-ai/high-risk-review-queue?${buildReviewQueueQuery(true)}`, data => {
    reviewQueueItems.value = (data || []).map(item => normalizeAuditItem(item))
    queueLoading.value = false
  }, message => {
    queueLoading.value = false
    ElMessage.warning(message || '高风险待复核队列加载失败')
  })
}

function loadAuditList() {
  auditLoading.value = true
  get(`/api/admin/consultation-ai/audit-list?${buildAuditQuery(true)}`, data => {
    auditItems.value = (data || []).map(item => normalizeAuditItem(item))
    auditLoading.value = false
  }, message => {
    auditLoading.value = false
    ElMessage.warning(message || '最近 AI 导诊输出加载失败')
  })
}

function resetAuditFilters() {
  auditKeyword.value = ''
  auditMessageType.value = 'all'
  highRiskOnly.value = false
  loadAuditList()
}

function resetReviewQueueFilters() {
  reviewQueueKeyword.value = ''
  loadReviewQueue()
}

function exportAuditList() {
  auditExporting.value = true
  download(`/api/admin/consultation-ai/audit-list/export?${buildAuditQuery(true)}`, 'consultation-ai-audit.csv', () => {
    auditExporting.value = false
    ElMessage.success('CSV download started')
  }, message => {
    auditExporting.value = false
    ElMessage.warning(message || 'AI audit export failed')
  }, error => {
    auditExporting.value = false
    console.error(error)
    ElMessage.error('AI audit export failed')
  })
}

function exportReviewQueue() {
  queueExporting.value = true
  download(`/api/admin/consultation-ai/high-risk-review-queue/export?${buildReviewQueueQuery(true)}`, 'consultation-ai-high-risk-review.csv', () => {
    queueExporting.value = false
    ElMessage.success('CSV download started')
  }, message => {
    queueExporting.value = false
    ElMessage.warning(message || 'High-risk queue export failed')
  }, error => {
    queueExporting.value = false
    console.error(error)
    ElMessage.error('High-risk queue export failed')
  })
}

function availabilityLabel(value, yesText = '已就绪', noText = '未就绪') {
  return value ? yesText : noText
}

function statusTagType(value) {
  return value ? 'success' : 'warning'
}

function statusTagLabel(value) {
  return value ? '正常' : '待处理'
}

function statusToneClass(value) {
  return value ? 'is-success' : 'is-warning'
}

function auditTypeLabel(value) {
  return ({
    ai_triage_summary: 'AI 导诊总结',
    ai_followup_questions: 'AI 追问建议',
    ai_chat_reply: 'AI 对话回复'
  })[value] || value || 'AI 输出'
}

function consultationStatusLabel(value) {
  return ({
    submitted: '已提交',
    triaged: '已分诊',
    processing: '处理中',
    completed: '已完成'
  })[value] || ''
}

function triageActionLabel(value) {
  return ({
    emergency: '立即急诊',
    offline: '尽快线下就医',
    followup: '复诊随访',
    online: '线上继续沟通'
  })[`${value || ''}`.toLowerCase()] || ''
}

function sourceLabel(value) {
  return ({
    deepseek: 'DeepSeek',
    fallback: '规则兜底'
  })[`${value || ''}`.toLowerCase()] || (value || '')
}

function rateText(value) {
  const number = Number(value)
  return Number.isFinite(number) && number > 0 ? `${number.toFixed(2)}%` : '0.00%'
}

function doctorMessageSceneLabel(value) {
  return ({
    opening: '首次接诊',
    clarify: '补充追问',
    check_result: '结果解读',
    follow_up: '复诊随访'
  })[`${value || ''}`.toLowerCase()] || value || '沟通草稿'
}

function doctorMessageSourceLabel(item) {
  if (item?.fallback === 1) return '规则兜底'
  return sourceLabel(item?.source || 'deepseek') || 'AI 草稿'
}

function doctorMessageApplyModeLabel(value) {
  return ({
    replace: '覆盖带入',
    append: '追加带入',
    compose: 'AI+模板合成'
  })[`${value || ''}`.toLowerCase()] || '已带入'
}

function doctorMessageUsageStatusLabel(item) {
  if (item?.sentStatus === 1) return '已发送'
  if (Number(item?.applyCount || 0) > 0) return '已带入待发送'
  return '仅生成未带入'
}

function doctorFormSceneLabel(value) {
  return ({
    handle: '医生处理',
    follow_up: '随访记录'
  })[`${value || ''}`.toLowerCase()] || value || '表单草稿'
}

function doctorFormRegenerateFieldLabel(value) {
  return ({
    doctor_summary: '判断摘要',
    medical_advice: '处理建议',
    follow_up_plan: '随访安排',
    patient_instruction: '患者指导',
    followup_summary: '随访摘要',
    followup_advice: '随访建议',
    followup_next_step: '下一步安排'
  })[`${value || ''}`.toLowerCase()] || ''
}

function doctorFormGenerationLabel(item) {
  const fieldLabel = doctorFormRegenerateFieldLabel(item?.regenerateField)
  return fieldLabel ? `字段重写：${fieldLabel}` : '整份草稿'
}

function doctorFormSourceLabel(item) {
  if (item?.fallback === 1) return '规则兜底'
  return sourceLabel(item?.source || 'deepseek') || 'AI 草稿'
}

function doctorFormApplyTargetLabel(value) {
  return ({
    handle_form: '带入处理表单',
    conclusion_form: '带入结构化结论',
    follow_up_form: '带入随访表单'
  })[`${value || ''}`.toLowerCase()] || '已带入'
}

function doctorFormApplyModeLabel(value) {
  return ({
    replace: '直接带入',
    append: '追加带入',
    compose: 'AI+模板合成'
  })[`${value || ''}`.toLowerCase()] || '已带入'
}

function doctorFormUsageStatusLabel(item) {
  if (item?.savedStatus === 1) return '已保存'
  if (Number(item?.applyCount || 0) > 0) return '已带入待保存'
  return '仅生成未带入'
}

function doctorFormPromptVersionLabel(value) {
  const text = `${value || ''}`.trim()
  if (!text) return '未标记版本'
  const shortText = text.split('/').filter(Boolean).slice(-1)[0] || text
  return shortText || text
}

function doctorFormPromptSortLabel(value) {
  return ({
    generatedCount: '生成次数',
    contextRewriteCount: '上下文续写数',
    fieldRegenerateCount: '字段重写数',
    appliedCount: '带入次数',
    savedCount: '保存次数',
    applyRate: '带入率',
    saveRate: '保存率'
  })[`${value || ''}`] || '生成次数'
}

function doctorFormPromptMetricValue(item, metricKey = doctorFormPromptSortKey.value) {
  const value = item?.[metricKey]
  return Number.isFinite(Number(value)) ? Number(value) : 0
}

function doctorFormPromptPrimaryText(item) {
  const metricKey = `${doctorFormPromptSortKey.value || 'generatedCount'}`
  if (metricKey === 'applyRate' || metricKey === 'saveRate') {
    return rateText(doctorFormPromptMetricValue(item, metricKey))
  }
  return `${doctorFormPromptMetricValue(item, metricKey)}`
}

function doctorFormFieldSortLabel(value) {
  return ({
    relatedCount: '相关日志数',
    regenerateCount: '字段重写数',
    templateComposeCount: '模板拼装数',
    contextRewriteCount: '上下文续写数',
    appliedCount: '带入次数',
    savedCount: '保存次数',
    applyRate: '带入率',
    saveRate: '保存率',
    contextRewriteSaveRate: '续写保存率'
  })[`${value || ''}`] || '相关日志数'
}

function doctorFormFieldMetricValue(item, metricKey = doctorFormFieldSortKey.value) {
  const value = item?.[metricKey]
  return Number.isFinite(Number(value)) ? Number(value) : 0
}

function doctorFormFieldPrimaryText(item) {
  const metricKey = `${doctorFormFieldSortKey.value || 'relatedCount'}`
  if (metricKey === 'applyRate' || metricKey === 'saveRate' || metricKey === 'contextRewriteSaveRate') {
    return rateText(doctorFormFieldMetricValue(item, metricKey))
  }
  return `${doctorFormFieldMetricValue(item, metricKey)}`
}

function doctorFormTemplateSortLabel(value) {
  return ({
    composeCount: '拼装次数',
    contextRewriteCount: '上下文续写数',
    savedCount: '保存次数',
    saveRate: '保存率',
    appliedCount: '带入次数',
    contextRewriteSaveRate: '续写保存率'
  })[`${value || ''}`] || '拼装次数'
}

function doctorFormTemplateMetricValue(item, metricKey = doctorFormTemplateSortKey.value) {
  const value = item?.[metricKey]
  return Number.isFinite(Number(value)) ? Number(value) : 0
}

function doctorFormTemplatePrimaryText(item) {
  const metricKey = `${doctorFormTemplateSortKey.value || 'composeCount'}`
  if (metricKey === 'saveRate' || metricKey === 'contextRewriteSaveRate') {
    return rateText(doctorFormTemplateMetricValue(item, metricKey))
  }
  return `${doctorFormTemplateMetricValue(item, metricKey)}`
}

function parseJsonArray(value) {
  if (!value) return []
  if (Array.isArray(value)) return value
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function doctorHandleStatusLabel(value) {
  return ({
    completed: '处理完成',
    processing: '处理中',
    received: '已接诊'
  })[`${value || ''}`] || (value ? '已处理' : '待医生处理')
}

function conditionLevelLabel(value) {
  return ({
    low: '轻度',
    medium: '中度',
    high: '较高风险',
    critical: '危急'
  })[`${value || ''}`] || ''
}

function dispositionLabel(value) {
  return ({
    observe: '继续观察',
    online_followup: '线上随访',
    offline_visit: '线下就医',
    emergency: '立即急诊'
  })[`${value || ''}`] || ''
}

function aiConsistencyLabel(value) {
  return value === 1 ? '医生标记与 AI 一致' : value === 0 ? '医生标记与 AI 不一致' : ''
}

function doctorFollowUpText(conclusion) {
  if (!conclusion) return ''
  return conclusion.needFollowUp === 1
    ? (conclusion.followUpWithinDays ? `${conclusion.followUpWithinDays} 天内随访` : '需要随访')
    : conclusion.needFollowUp === 0
      ? '暂不需要随访'
      : ''
}

function resolveAuditAiConditionLevel(insight, item) {
  const text = `${item?.triageLevelName || ''}`.trim()
  if (!text) return ''
  const upper = text.toUpperCase()
  if (upper.includes('EMERGENCY') || upper.includes('CRITICAL') || upper.includes('RED') || text.includes('危')) return 'critical'
  if (upper.includes('OFFLINE') || upper.includes('HIGH') || upper.includes('ORANGE') || text.includes('高')) return 'high'
  if (upper.includes('FOLLOWUP') || upper.includes('MEDIUM') || upper.includes('YELLOW') || text.includes('中')) return 'medium'
  if (upper.includes('ONLINE') || upper.includes('LOW') || upper.includes('GREEN') || text.includes('低') || text.includes('轻')) return 'low'
  return ''
}

function resolveAuditAiDisposition(insight, item) {
  const code = `${insight?.recommendedVisitTypeCode || item?.triageActionType || ''}`.toLowerCase()
  return ({
    emergency: 'emergency',
    offline: 'offline_visit',
    followup: 'online_followup',
    online: 'online_followup'
  })[code] || ''
}

function resolveAuditAiFollowUpText(insight, item) {
  const code = `${insight?.recommendedVisitTypeCode || item?.triageActionType || ''}`.toLowerCase()
  if (!code) return ''
  return code === 'followup' ? '3 天内随访' : '暂不需要随访'
}

function compareAuditField(aiValue, doctorValue) {
  if (!aiValue && !doctorValue) return 'pending'
  if (!aiValue || !doctorValue) return 'pending'
  return aiValue === doctorValue ? 'match' : 'mismatch'
}

function resolveAuditOverallStatus(conditionLevelStatus, dispositionStatus, followUpStatus) {
  const statuses = [conditionLevelStatus, dispositionStatus, followUpStatus]
  if (statuses.every(item => item === 'pending')) return 'pending'
  if (statuses.some(item => item === 'mismatch')) return 'mismatch'
  if (statuses.some(item => item === 'pending')) return 'partial'
  return 'match'
}

function resolveDoctorReview(item, insight) {
  const handle = item?.doctorHandle || null
  const conclusion = item?.doctorConclusion || null
  if (!handle && !conclusion) return null

  const aiConditionLevel = resolveAuditAiConditionLevel(insight, item)
  const aiDisposition = resolveAuditAiDisposition(insight, item)
  const aiFollowUp = resolveAuditAiFollowUpText(insight, item)
  const doctorConditionLevel = `${conclusion?.conditionLevel || ''}`.trim()
  const doctorDisposition = `${conclusion?.disposition || ''}`.trim()
  const doctorFollowUp = doctorFollowUpText(conclusion)
  const compareConditionLevelStatus = conclusion ? compareAuditField(aiConditionLevel, doctorConditionLevel) : ''
  const compareDispositionStatus = conclusion ? compareAuditField(aiDisposition, doctorDisposition) : ''
  const compareFollowUpStatus = conclusion ? compareAuditField(aiFollowUp, doctorFollowUp) : ''
  const compareOverallStatus = conclusion
    ? resolveAuditOverallStatus(compareConditionLevelStatus, compareDispositionStatus, compareFollowUpStatus)
    : ''
  const mismatchReasonLabels = parseJsonArray(conclusion?.aiMismatchReasonsJson)
    .map(item => aiMismatchReasonLabel(item))
    .filter(Boolean)

  const progressLabel = conclusion
    ? '已形成结论'
    : doctorHandleStatusLabel(handle?.status)
  const progressText = conclusion
    ? `${conclusion.doctorName || handle?.doctorName || '医生'}已提交最终结构化结论${formatDate(conclusion.updateTime || conclusion.createTime, true) !== '-' ? `，更新时间 ${formatDate(conclusion.updateTime || conclusion.createTime, true)}` : ''}。`
    : `${handle?.doctorName || '医生'}已接手当前问诊，当前状态为${doctorHandleStatusLabel(handle?.status)}${formatDate(handle?.updateTime || handle?.receiveTime, true) !== '-' ? `，最近时间 ${formatDate(handle?.updateTime || handle?.receiveTime, true)}` : ''}。`

  const doctorConclusionText = conclusion
    ? [
        conditionLevelLabel(conclusion.conditionLevel) ? `病情等级 ${conditionLevelLabel(conclusion.conditionLevel)}` : '',
        dispositionLabel(conclusion.disposition) ? `处理去向 ${dispositionLabel(conclusion.disposition)}` : '',
        doctorFollowUp ? `随访安排 ${doctorFollowUp}` : '',
        conclusion.diagnosisDirection ? `诊断方向 ${conclusion.diagnosisDirection}` : ''
      ].filter(Boolean).join('；')
    : ''

  const compareText = conclusion
    ? [
        `病情等级 ${comparisonStatusLabel(compareConditionLevelStatus)}`,
        `处理去向 ${comparisonStatusLabel(compareDispositionStatus)}`,
        `随访安排 ${comparisonStatusLabel(compareFollowUpStatus)}`
      ].join(' · ')
    : ''

  return {
    hasConclusion: !!conclusion,
    handleDoctorName: handle?.doctorName || conclusion?.doctorName || '',
    progressLabel,
    progressText,
    aiConsistencyLabel: aiConsistencyLabel(conclusion?.isConsistentWithAi),
    doctorConclusionText,
    compareText,
    compareOverallStatus,
    mismatchReasonLabels,
    mismatchRemark: `${conclusion?.aiMismatchRemark || ''}`.trim(),
    isMismatch: conclusion?.isConsistentWithAi === 0 || compareOverallStatus === 'mismatch'
  }
}

function isHighRiskAuditItem(item) {
  const insight = item?.insight
  if (!insight) return false
  return !!(
    insight.shouldEscalateToHuman === 1
    || insight.suggestOfflineImmediately === 1
    || insight.recommendedVisitTypeCode === 'emergency'
    || insight.recommendedVisitTypeCode === 'offline'
    || insight.riskFlags?.length
  )
}

function reviewQueueStatusLabel(item) {
  if (!item?.doctorHandle && !item?.doctorConclusion) return '待医生接手'
  if (!!item?.doctorHandle && !item?.doctorConclusion) return '待形成结论'
  if (item?.doctorReview?.isMismatch) return '医生判定有差异'
  return '待人工复核'
}

function reviewQueueTags(item) {
  const tags = []
  if (item?.doctorReview?.compareOverallStatus) tags.push(`对比：${comparisonStatusLabel(item.doctorReview.compareOverallStatus)}`)
  if (item?.doctorReview?.aiConsistencyLabel) tags.push(item.doctorReview.aiConsistencyLabel)
  if (item?.insight?.shouldEscalateToHuman === 1) tags.push('建议医生接管')
  if (item?.insight?.suggestOfflineImmediately === 1) tags.push('建议尽快线下')
  if (item?.insight?.recommendedVisitType) tags.push(`AI 建议：${item.insight.recommendedVisitType}`)
  return [...tags, ...(item?.doctorReview?.mismatchReasonLabels || [])].slice(0, 6)
}

function reviewQueueReasonText(item) {
  const reasons = []
  if (item?.insight?.riskFlags?.length) reasons.push(`风险标签 ${item.insight.riskFlags.join('、')}`)
  if (item?.insight?.shouldEscalateToHuman === 1) reasons.push('AI 建议尽快由医生接管')
  if (item?.insight?.suggestOfflineImmediately === 1) reasons.push('AI 提醒尽快线下就医')
  if (item?.insight?.recommendedVisitType) reasons.push(`建议方式 ${item.insight.recommendedVisitType}`)
  return reasons.length ? reasons.join('；') : '当前样本已命中高风险判定，请优先人工复核。'
}

function queueAiSummaryText(item) {
  const segments = []
  if (item?.insight?.summary) segments.push(item.insight.summary)
  if (item?.insight?.reply) segments.push(item.insight.reply)
  if (item?.insight?.recommendedDepartmentName) segments.push(`建议科室 ${item.insight.recommendedDepartmentName}`)
  if (item?.insight?.doctorRecommendationReason) segments.push(`推荐依据 ${item.insight.doctorRecommendationReason}`)
  if (item?.insight?.nextQuestions?.length) segments.push(`补充建议 ${item.insight.nextQuestions.join('；')}`)
  if (!segments.length && item?.content) segments.push(item.content)
  return abbreviateText(segments.join('；'), 220) || '当前高风险样本暂无更多 AI 说明。'
}

function reviewQueueProgressText(item) {
  if (!item?.doctorHandle && !item?.doctorConclusion) {
    return '当前还没有医生接手，建议优先进入问诊详情确认是否需要立即分派。'
  }
  if (!!item?.doctorHandle && !item?.doctorConclusion) {
    return item.doctorReview?.progressText || '医生已接手，但还没有形成最终结构化结论。'
  }
  if (item?.doctorReview?.isMismatch) {
    return item.doctorReview?.progressText || '医生已完成处理，并明确指出与 AI 建议存在差异。'
  }
  return item?.doctorReview?.progressText || '当前样本仍建议继续人工复核。'
}

function auditContextText(item) {
  const segments = []
  const status = consultationStatusLabel(item.consultationStatus)
  const triageAction = triageActionLabel(item.triageActionType)
  if (item.departmentName) segments.push(`科室 ${item.departmentName}`)
  if (item.triageLevelName) segments.push(`分诊等级 ${item.triageLevelName}`)
  if (status) segments.push(`问诊状态 ${status}`)
  if (triageAction) segments.push(`建议去向 ${triageAction}`)
  return segments.length ? segments.join(' · ') : '当前消息已写入导诊留痕，可继续到导诊记录中心查看完整上下文。'
}

function auditMetaTags(item) {
  const tags = []
  if (item.insight?.recommendedVisitType) tags.push(`建议方式：${item.insight.recommendedVisitType}`)
  if (item.insight?.recommendedDepartmentName) tags.push(`建议科室：${item.insight.recommendedDepartmentName}`)
  if (item.insight?.confidenceText) tags.push(`置信度：${item.insight.confidenceText}`)
  if (item.insight?.promptVersion) tags.push(`Prompt：${item.insight.promptVersion}`)
  if (item.insight?.source) tags.push(`来源：${sourceLabel(item.insight.source)}`)
  if (item.insight?.shouldEscalateToHuman === 1) tags.push('建议医生接管')
  if (item.insight?.suggestOfflineImmediately === 1) tags.push('建议尽快线下')
  return tags.slice(0, 6)
}

function showAuditReply(item) {
  return !!item?.insight?.reply
}

function showAuditContent(item) {
  return !item?.insight?.summary && !item?.insight?.reply && !(item?.insight?.nextQuestions?.length) && !!item?.content
}

function goToConsultationDetail(item) {
  if (!item?.consultationId) {
    ElMessage.warning('当前消息还没有关联问诊记录')
    return
  }
  router.push({
    name: 'admin-consultation-record',
    query: {
      detailId: `${item.consultationId}`,
      source: 'ai-audit'
    }
  })
}

function goToConsultationCenter(item) {
  const query = { source: 'ai-audit' }
  if (item?.consultationId) query.detailId = `${item.consultationId}`
  router.push({
    name: 'admin-consultation-record',
    query
  })
}

function abbreviateText(value, limit = 180) {
  const text = `${value || ''}`.trim()
  if (!text || text.length <= limit) return text
  return `${text.slice(0, limit)}...`
}

function formatDate(value, withTime = false) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', withTime
    ? { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
    : { year: 'numeric', month: '2-digit', day: '2-digit' }
  ).format(new Date(value))
}

onMounted(() => {
  loadAiConfig()
  loadAiConfigHistory()
  loadOverview()
  loadDoctorMessageUsage()
  loadDoctorFormUsage()
  loadReviewQueue()
  loadAuditList()
})
</script>

<style scoped>
.ai-config-page { display: flex; flex-direction: column; gap: 18px; }
.hero-card, .panel-card, .status-card, .metric-card, .hero-metric {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}
.hero-card {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.9fr);
  gap: 18px;
  padding: 26px;
}
.section-tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #27646d;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.hero-copy h2 { margin: 12px 0 8px; font-size: 30px; }
.hero-copy p, .panel-head p, .guideline-item p, .hero-metric p { margin: 0; color: var(--app-muted); line-height: 1.8; }
.chip-row, .hero-actions, .status-grid, .metric-grid, .content-grid { display: flex; gap: 12px; flex-wrap: wrap; }
.chip-row { margin-top: 16px; }
.chip-row span {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #27646d;
}
.hero-actions { margin-top: 18px; }
.hero-side {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.hero-metric { padding: 20px; }
.hero-metric span, .status-card span, .metric-card span { color: var(--app-muted); }
.hero-metric strong, .status-card strong, .metric-card strong {
  display: block;
  margin-top: 14px;
  font-size: 30px;
}
.hero-metric strong.is-success { color: #0f6665; }
.hero-metric strong.is-warning { color: #c77d25; }
.hero-metric-accent {
  border-color: rgba(15, 102, 101, 0.22);
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.08), rgba(255, 255, 255, 0.96));
}
.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}
.status-card { padding: 20px 22px; }
.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
}
.panel-card { padding: 24px; }
.panel-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 18px; }
.panel-head h3 { margin: 0; font-size: 22px; }
.detail-descriptions { margin-bottom: 18px; }
.config-grid {
  display: grid;
  grid-template-columns: minmax(240px, 0.85fr) minmax(0, 1.2fr);
  gap: 18px;
}
.config-note {
  padding: 18px;
  border-radius: 22px;
  background: rgba(15, 102, 101, 0.04);
  border: 1px solid rgba(15, 102, 101, 0.1);
}
.config-note strong {
  display: block;
  margin-bottom: 10px;
  color: #31474d;
}
.config-note p {
  margin: 0;
  line-height: 1.8;
  color: #41575d;
}
.config-note p + p {
  margin-top: 8px;
}
.config-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 18px;
}
.config-form-grid :deep(.el-form-item) {
  margin-bottom: 0;
}
.config-form-item-full {
  grid-column: 1 / -1;
}
.config-history-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.config-history-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(15, 102, 101, 0.04);
  border: 1px solid rgba(15, 102, 101, 0.1);
}
.env-board {
  padding: 16px 18px;
  border-radius: 22px;
  background: rgba(15, 102, 101, 0.04);
}
.env-board strong { display: block; margin-bottom: 10px; }
.env-board pre {
  margin: 0;
  font-family: Consolas, "Courier New", monospace;
  white-space: pre-wrap;
  line-height: 1.8;
  color: #31474d;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}
.metric-card { padding: 18px 20px; }
.metric-card-accent {
  border-color: rgba(15, 102, 101, 0.22);
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.07), rgba(255, 255, 255, 0.98));
}
.copy {
  margin: 0;
  line-height: 1.8;
  color: #41575d;
}
.copy + .copy { margin-top: 6px; }
.warning-list { display: flex; flex-direction: column; gap: 12px; }
.guideline-list { display: flex; flex-direction: column; gap: 14px; }
.guideline-item {
  padding: 18px;
  border-radius: 22px;
  background: rgba(15, 102, 101, 0.04);
  border: 1px solid rgba(15, 102, 101, 0.1);
}
.guideline-item strong { display: block; margin-bottom: 8px; color: #31474d; }
.audit-toolbar,
.audit-tag-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.audit-summary-bar,
.audit-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.field-breakdown-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 8px;
}
.field-breakdown-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}
.field-breakdown-head h4 {
  margin: 0 0 6px;
  font-size: 18px;
  color: #31474d;
}
.field-breakdown-head p {
  margin: 0;
  color: var(--app-muted);
  line-height: 1.8;
}
.field-breakdown-toolbar {
  justify-content: flex-end;
}
.template-breakdown-list {
  margin-top: 0;
}
.queue-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.usage-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}
.audit-review-head,
.audit-review-chips {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.audit-summary-bar {
  margin-bottom: 16px;
  color: var(--app-muted);
}
.audit-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.queue-item {
  min-height: 100%;
}
.audit-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(15, 102, 101, 0.04);
  border: 1px solid rgba(15, 102, 101, 0.1);
}
.audit-head-chips { margin: 0 0 2px; }
.audit-tag-row span {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.1);
  color: #27646d;
  font-size: 12px;
}
.audit-tag-row.danger span {
  background: rgba(214, 95, 80, 0.12);
  color: #9f4336;
}
.audit-review-card {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(19, 73, 80, 0.1);
  background: rgba(255, 255, 255, 0.68);
}
.audit-review-head {
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}
.audit-review-head strong {
  color: #31474d;
}
.audit-review-chips span {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.08);
  color: #27646d;
  font-size: 12px;
}
.compare-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 10px;
  border-radius: 999px;
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
.chip-danger {
  background: rgba(214, 95, 80, 0.12) !important;
  color: #9f4336 !important;
}
.chip-success {
  background: rgba(77, 168, 132, 0.16) !important;
  color: #1f6f4f !important;
}
.chip-warning {
  background: rgba(210, 155, 47, 0.14) !important;
  color: #8f6514 !important;
}
.audit-actions {
  justify-content: flex-end;
}
@media (max-width: 1100px) {
  .hero-card,
  .content-grid,
  .config-grid { grid-template-columns: 1fr; }
  .config-history-list,
  .queue-list,
  .usage-list { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .hero-side { grid-template-columns: 1fr; }
  .panel-head { flex-direction: column; align-items: flex-start; }
  .field-breakdown-head { flex-direction: column; }
  .config-form-grid { grid-template-columns: 1fr; }
  .audit-toolbar,
  .audit-actions { width: 100%; }
  .audit-review-head { flex-direction: column; }
}
</style>
