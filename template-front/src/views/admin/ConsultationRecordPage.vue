<template>
  <div class="record-page">
    <section class="stats">
      <article class="card stat"><span>问诊记录</span><strong>{{ records.length }}</strong></article>
      <article class="card stat"><span>已认领</span><strong>{{ assignedCount }}</strong></article>
      <article class="card stat"><span>医生已完成</span><strong>{{ completedCount }}</strong></article>
      <article class="card stat"><span>今日新增</span><strong>{{ todayCount }}</strong></article>
      <article class="card stat"><span>等待首推医生</span><strong>{{ waitingDispatchCount }}</strong></article>
      <article class="card stat"><span>首推已接手</span><strong>{{ suggestedAcceptedCount }}</strong></article>
    </section>

    <section class="card block">
      <div class="head">
        <div>
          <h3>服务评价质控概览</h3>
          <p>汇总患者对在线问诊服务的评分、是否解决以及医生处理状态，方便后台快速定位需要继续跟进的记录。</p>
        </div>
        <el-button @click="applyServiceFeedbackQuickFilter('attention')">只看待关注评价</el-button>
      </div>
      <div class="summary-grid feedback-summary-grid">
        <article class="subcard summary-card">
          <span>已提交评价</span>
          <strong>{{ serviceFeedbackCount }}</strong>
          <small>{{ serviceFeedbackCoverageText }}</small>
        </article>
        <article class="subcard summary-card">
          <span>待关注评价</span>
          <strong>{{ attentionServiceFeedbackCount }}</strong>
          <small>低分或未解决，且医生尚未标记为已处理。</small>
        </article>
        <article class="subcard summary-card">
          <span>低分评价</span>
          <strong>{{ lowScoreServiceFeedbackCount }}</strong>
          <small>2 星及以下的服务评价，建议优先复盘。</small>
        </article>
        <article class="subcard summary-card">
          <span>医生已处理</span>
          <strong>{{ handledServiceFeedbackCount }}</strong>
          <small>{{ serviceFeedbackHandledRateText }}</small>
        </article>
        <article class="subcard summary-card">
          <span>平均处理时长</span>
          <strong>{{ averageServiceFeedbackHandleDurationText }}</strong>
          <small>按已处理服务评价统计。</small>
        </article>
        <article class="subcard summary-card">
          <span>超时未处理</span>
          <strong>{{ overdueUnhandledServiceFeedbackCount }}</strong>
          <small>{{ `当前阈值 ${SERVICE_FEEDBACK_HANDLE_OVERDUE_HOURS}h` }}</small>
        </article>
      </div>
      <div class="breakdown-grid">
        <article class="subcard breakdown-card">
          <div class="breakdown-head">
            <div>
              <strong>最近待关注评价</strong>
              <p>优先查看低分或未解决，且医生还未完成处理登记的服务评价。</p>
            </div>
          </div>
          <div v-if="attentionServiceFeedbackPreview.length" class="dispatch-wait-list">
            <article v-for="item in attentionServiceFeedbackPreview" :key="item.id" class="dispatch-wait-card feedback-focus-card">
              <div class="chips">
                <span>{{ item.consultationNo }}</span>
                <span>{{ item.patientName }}</span>
                <span>{{ item.categoryName }}</span>
                <span>{{ item.departmentName || '未分配科室' }}</span>
                <span>{{ formatDate(serviceFeedbackTime(item)) }}</span>
              </div>
              <div class="feedback-tag-row">
                <el-tag :type="serviceFeedbackScoreTagType(item.serviceFeedback?.serviceScore)" effect="light">
                  {{ item.serviceFeedback?.serviceScore ?? '-' }}/5 分
                </el-tag>
                <el-tag :type="serviceFeedbackResolvedTagType(item.serviceFeedback?.isResolved)" effect="light">
                  {{ serviceFeedbackResolvedLabel(item.serviceFeedback?.isResolved) }}
                </el-tag>
                <el-tag :type="serviceFeedbackHandleTagType(item.serviceFeedback?.doctorHandleStatus)" effect="light">
                  {{ serviceFeedbackHandleLabel(item.serviceFeedback?.doctorHandleStatus) }}
                </el-tag>
              </div>
              <p class="copy"><strong>反馈内容：</strong>{{ item.serviceFeedback?.feedbackText || '患者未填写补充评价内容。' }}</p>
              <p v-if="item.serviceFeedback?.doctorHandleRemark" class="copy"><strong>处理备注：</strong>{{ item.serviceFeedback.doctorHandleRemark }}</p>
              <div class="actions">
                <el-button link type="primary" @click="openDetail(item.id)">查看详情</el-button>
                <el-button link @click="applyServiceFeedbackQuickFilter('attention')">查看同类记录</el-button>
              </div>
            </article>
          </div>
          <el-empty v-else description="当前没有待关注的服务评价" />
        </article>
        <article class="subcard breakdown-card">
          <div class="breakdown-head">
            <div>
              <strong>按科室聚合</strong>
              <p>查看各科室服务评价数量、重点关注量以及医生处理进度，便于做横向比较。</p>
            </div>
          </div>
          <el-table v-if="serviceFeedbackDepartmentBreakdown.length" :data="serviceFeedbackDepartmentBreakdown" size="small" border>
            <el-table-column prop="departmentName" label="科室" min-width="140" />
            <el-table-column prop="feedbackCount" label="评价数" width="86" align="center" />
            <el-table-column prop="attentionCount" label="待关注" width="86" align="center" />
            <el-table-column prop="lowScoreCount" label="低分" width="86" align="center" />
            <el-table-column prop="unresolvedCount" label="未解决" width="86" align="center" />
            <el-table-column prop="handledRateText" label="已处理率" width="100" align="center" />
          </el-table>
          <el-empty v-else description="当前暂无可统计的服务评价数据" />
        </article>
      </div>
      <div class="head summary-subhead">
        <div>
          <h3>医生处理表现</h3>
          <p>继续查看各医生收到的服务评价量、重点关注积压和平均处理时长，帮助后台识别服务反馈处理负载。</p>
        </div>
      </div>
      <div class="breakdown-grid">
        <article class="subcard breakdown-card">
          <div class="breakdown-head">
            <div>
              <strong>按医生聚合</strong>
              <p>聚合每位医生的服务评价数量、待关注积压、超时未处理量和平均处理时长。</p>
            </div>
          </div>
          <el-table v-if="serviceFeedbackDoctorBreakdown.length" :data="serviceFeedbackDoctorBreakdown" size="small" border>
            <el-table-column prop="doctorName" label="医生" min-width="120" show-overflow-tooltip />
            <el-table-column prop="departmentName" label="科室" min-width="120" show-overflow-tooltip />
            <el-table-column prop="feedbackCount" label="评价数" width="80" align="center" />
            <el-table-column prop="attentionCount" label="待关注" width="86" align="center" />
            <el-table-column prop="overdueUnhandledCount" label="超时未处理" width="100" align="center" />
            <el-table-column prop="handledRateText" label="已处理率" width="96" align="center" />
            <el-table-column prop="averageHandleDurationText" label="平均处理时长" min-width="120" align="center" />
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="applyServiceFeedbackDoctorQuickFilter(row.doctorName, 'attention')">查看记录</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="当前暂无可统计的医生服务评价数据" />
        </article>
        <article class="subcard breakdown-card">
          <div class="breakdown-head">
            <div>
              <strong>超时未处理评价</strong>
              <p>优先关注超过处理阈值仍未登记处理结果的服务评价，帮助平台及时干预。</p>
            </div>
          </div>
          <div v-if="overdueUnhandledServiceFeedbackPreview.length" class="dispatch-wait-list">
            <article v-for="item in overdueUnhandledServiceFeedbackPreview" :key="`overdue-${item.id}`" class="dispatch-wait-card feedback-focus-card">
              <div class="chips">
                <span>{{ item.consultationNo }}</span>
                <span>{{ item.patientName }}</span>
                <span>{{ item.serviceFeedback?.doctorName || '未分配医生' }}</span>
                <span>{{ formatDate(serviceFeedbackBaseTime(item)) }}</span>
              </div>
              <div class="feedback-tag-row">
                <el-tag :type="serviceFeedbackScoreTagType(item.serviceFeedback?.serviceScore)" effect="light">
                  {{ item.serviceFeedback?.serviceScore ?? '-' }}/5 分
                </el-tag>
                <el-tag type="danger" effect="light">
                  已等待 {{ serviceFeedbackPendingDurationText(item) }}
                </el-tag>
              </div>
              <p class="copy"><strong>反馈内容：</strong>{{ item.serviceFeedback?.feedbackText || '患者未填写补充评价内容。' }}</p>
              <div class="actions">
                <el-button link type="primary" @click="openDetail(item.id)">查看详情</el-button>
                <el-button link type="danger" @click="applyServiceFeedbackQuickFilter('overdue_unhandled')">查看全部超时</el-button>
              </div>
            </article>
          </div>
          <el-empty v-else description="当前没有超时未处理的服务评价" />
        </article>
      </div>
    </section>

    <section class="card block">
      <div class="head">
        <div>
          <h3>智能分配运营总览</h3>
          <p>查看当前推荐覆盖、首推命中率和待接手积压情况，方便持续优化分配链路。</p>
        </div>
        <el-button :loading="dispatchSummaryLoading" @click="loadSmartDispatchSummary">刷新分配统计</el-button>
      </div>
      <div class="summary-grid dispatch-summary-grid">
        <article class="subcard summary-card">
          <span>推荐覆盖问诊</span>
          <strong>{{ dispatchSummary.suggestedCount }}</strong>
          <small>{{ dispatchSummary.suggestionCoverageText || '-' }}</small>
        </article>
        <article class="subcard summary-card">
          <span>首推命中率</span>
          <strong>{{ dispatchSummary.suggestedHitRateText }}</strong>
          <small>{{ dispatchSummary.claimedBySuggestedCount }} 单由首推医生接手</small>
        </article>
        <article class="subcard summary-card">
          <span>转其他医生率</span>
          <strong>{{ dispatchSummary.claimedByOtherRateText }}</strong>
          <small>{{ dispatchSummary.claimedByOtherCount }} 单由其他医生承接</small>
        </article>
        <article class="subcard summary-card">
          <span>平均接手时长</span>
          <strong>{{ dispatchSummary.averageClaimDurationText }}</strong>
          <small>按已接手推荐单统计</small>
        </article>
        <article class="subcard summary-card">
          <span>超 24h 待接手</span>
          <strong>{{ dispatchSummary.overdueWaitingCount }}</strong>
          <small class="muted-copy">{{ `当前阈值 ${dispatchSummary.overdueThresholdHours || 24}h` }}</small>
          <small>{{ dispatchSummary.waitingAcceptCount }} 单仍在等待首推医生</small>
        </article>
        <article class="subcard summary-card">
          <span>科室分诊队列</span>
          <strong>{{ dispatchSummary.departmentQueueCount }}</strong>
          <small>尚未形成明确首推医生</small>
        </article>
      </div>
      <div class="breakdown-grid">
        <article class="subcard breakdown-card">
          <div class="breakdown-head">
            <div>
              <strong>首推医生承接情况</strong>
              <p>聚焦系统首推给谁、最终是否由首推医生接手，以及当前待接手积压主要落在哪些医生。</p>
            </div>
          </div>
          <el-table v-if="dispatchSummary.doctorBreakdown?.length" :data="dispatchSummary.doctorBreakdown" size="small" border>
            <el-table-column prop="doctorName" label="首推医生" min-width="120" show-overflow-tooltip />
            <el-table-column prop="departmentName" label="科室" min-width="120" show-overflow-tooltip />
            <el-table-column prop="totalSuggestedCount" label="被推荐量" width="90" align="center" />
            <el-table-column prop="waitingAcceptCount" label="待接手" width="90" align="center" />
            <el-table-column prop="acceptedCount" label="已接手" width="90" align="center" />
            <el-table-column prop="lostCount" label="他人承接" width="96" align="center" />
            <el-table-column prop="hitRateText" label="命中率" width="90" align="center" />
            <el-table-column prop="averageClaimDurationText" label="平均接手时长" min-width="120" align="center" />
          </el-table>
          <el-empty v-else description="当前暂无可统计的首推医生承接数据" />
        </article>
        <article class="subcard breakdown-card">
          <div class="breakdown-head">
            <div>
              <strong>长时间待接手问诊</strong>
              <p>优先关注当前仍处于等待首推医生接手状态、且等待时间较长的问诊单。</p>
            </div>
          </div>
          <div v-if="dispatchSummary.recentWaitingRecords?.length" class="dispatch-wait-list">
            <article v-for="item in dispatchSummary.recentWaitingRecords" :key="item.consultationId" class="dispatch-wait-card">
              <div class="chips">
                <span>{{ item.consultationNo }}</span>
                <span>{{ item.patientName }}</span>
                <span>{{ item.categoryName }}</span>
                <span>{{ item.departmentName || '未分配科室' }}</span>
                <span>{{ formatDate(item.createTime) }}</span>
              </div>
              <p class="copy"><strong>优先医生：</strong>{{ item.suggestedDoctorName || '待系统明确' }}</p>
              <p class="copy"><strong>等待时长：</strong>{{ item.waitingDurationText || '-' }}</p>
              <p v-if="item.recommendationReason" class="copy"><strong>推荐依据：</strong>{{ item.recommendationReason }}</p>
              <div class="actions">
                <el-button link type="primary" @click="openDetail(item.consultationId)">查看详情</el-button>
              </div>
            </article>
          </div>
          <el-empty v-else description="当前暂无待接手的首推问诊" />
        </article>
      </div>
    </section>

    <section class="card block">
      <div class="head">
        <div>
          <h3>AI 采纳总览</h3>
          <p>查看当前问诊记录中，AI 建议与医生最终结论的一致情况和最近差异样本。</p>
        </div>
        <el-button :loading="summaryLoading" @click="loadAiSummary">刷新 AI 统计</el-button>
      </div>
      <div class="summary-grid">
        <article class="subcard summary-card">
          <span>可对比结论</span>
          <strong>{{ aiSummary.comparedCount }}</strong>
          <small>{{ aiSummary.coverageText || '-' }}</small>
        </article>
        <article class="subcard summary-card">
          <span>与 AI 一致</span>
          <strong>{{ aiSummary.consistentCount }}</strong>
          <small>{{ aiSummary.consistentRateText || '-' }}</small>
        </article>
        <article class="subcard summary-card">
          <span>与 AI 不一致</span>
          <strong>{{ aiSummary.mismatchCount }}</strong>
          <small>优先复盘差异原因</small>
        </article>
        <article class="subcard summary-card">
          <span>待医生判断</span>
          <strong>{{ aiSummary.pendingCount }}</strong>
          <small>尚未形成可对比结论</small>
        </article>
      </div>
      <div class="breakdown-grid">
        <article class="subcard breakdown-card">
          <div class="breakdown-head">
            <div>
              <strong>按科室拆分</strong>
              <p>快速查看哪些科室的 AI 采纳率更高，哪些科室差异更集中。</p>
            </div>
          </div>
          <el-table v-if="aiSummary.departmentBreakdown?.length" :data="aiSummary.departmentBreakdown" size="small" border>
            <el-table-column prop="groupName" label="科室" min-width="140" />
            <el-table-column prop="totalCount" label="总量" width="80" align="center" />
            <el-table-column prop="comparedCount" label="可对比" width="90" align="center" />
            <el-table-column prop="consistentRateText" label="一致率" width="90" align="center" />
            <el-table-column prop="mismatchCount" label="差异数" width="90" align="center" />
            <el-table-column prop="pendingCount" label="待判断" width="90" align="center" />
          </el-table>
          <el-empty v-else description="当前暂无科室维度的 AI 采纳统计" />
        </article>
        <article class="subcard breakdown-card">
          <div class="breakdown-head">
            <div>
              <strong>按问诊分类拆分</strong>
              <p>定位哪些问诊分类更容易出现 AI 与医生结论偏差，便于继续优化 Prompt 和规则。</p>
            </div>
          </div>
          <el-table v-if="aiSummary.categoryBreakdown?.length" :data="aiSummary.categoryBreakdown" size="small" border>
            <el-table-column prop="groupName" label="问诊分类" min-width="140" />
            <el-table-column prop="totalCount" label="总量" width="80" align="center" />
            <el-table-column prop="comparedCount" label="可对比" width="90" align="center" />
            <el-table-column prop="consistentRateText" label="一致率" width="90" align="center" />
            <el-table-column prop="mismatchCount" label="差异数" width="90" align="center" />
            <el-table-column prop="pendingCount" label="待判断" width="90" align="center" />
          </el-table>
          <el-empty v-else description="当前暂无分类维度的 AI 采纳统计" />
        </article>
        <article class="subcard breakdown-card">
          <div class="breakdown-head">
            <div>
              <strong>按医生拆分</strong>
              <p>基于已提交的结构化结论，查看每位医生对 AI 建议的采纳情况和最常见差异原因。</p>
            </div>
          </div>
          <el-table v-if="aiSummary.doctorBreakdown?.length" :data="aiSummary.doctorBreakdown" size="small" border>
            <el-table-column prop="doctorName" label="医生" min-width="120" show-overflow-tooltip />
            <el-table-column prop="departmentName" label="科室" min-width="120" show-overflow-tooltip />
            <el-table-column prop="totalCount" label="结论数" width="86" align="center" />
            <el-table-column prop="comparedCount" label="已对比" width="86" align="center" />
            <el-table-column prop="consistentRateText" label="一致率" width="86" align="center" />
            <el-table-column prop="mismatchCount" label="差异数" width="86" align="center" />
            <el-table-column label="差异主因" min-width="220">
              <template #default="{ row }">
                <div v-if="row.mismatchReasonBreakdown?.length" class="reason-chip-list">
                  <span
                    v-for="item in row.mismatchReasonBreakdown.slice(0, 3)"
                    :key="`${row.doctorId || row.doctorName}-${item.reasonCode}`"
                    class="reason-chip is-clickable"
                    @click="openMismatchSampleDialog({
                      title: `${row.doctorName || '医生'} · ${item.reasonLabel}`,
                      doctorName: row.doctorName || '',
                      reasonCode: item.reasonCode,
                      sourceCount: item.count
                    })"
                  >
                    {{ item.reasonLabel }} {{ item.count }}次
                  </span>
                </div>
                <span v-else class="muted-copy">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  :disabled="!row.mismatchCount"
                  @click="openMismatchSampleDialog({
                    title: `${row.doctorName || '医生'} 的 AI 偏差样本`,
                    doctorName: row.doctorName || '',
                    sourceCount: row.mismatchCount
                  })"
                >
                  查看样本
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="当前暂无医生维度的 AI 采纳统计" />
        </article>
        <article class="subcard breakdown-card">
          <div class="breakdown-head">
            <div>
              <strong>差异原因汇总</strong>
              <p>汇总医生标记的 AI 偏差原因，便于后续优化 Prompt、规则和接诊流程。</p>
            </div>
          </div>
          <div v-if="aiSummary.mismatchReasonBreakdown?.length" class="reason-list">
            <article v-for="item in aiSummary.mismatchReasonBreakdown" :key="item.reasonCode" class="reason-item">
              <div class="reason-item-main">
                <strong>{{ item.reasonLabel }}</strong>
                <span>{{ item.count }} 次</span>
              </div>
              <el-button
                link
                type="primary"
                @click="openMismatchSampleDialog({
                  title: `${item.reasonLabel} 相关偏差样本`,
                  reasonCode: item.reasonCode,
                  sourceCount: item.count
                })"
              >
                查看样本
              </el-button>
            </article>
          </div>
          <el-empty v-else description="当前暂无已沉淀的 AI 差异原因统计" />
        </article>
      </div>
      <div class="head summary-subhead">
        <div>
          <h3>字段偏差分析</h3>
          <p>聚焦病情等级、处理去向和随访安排三个关键字段，查看 AI 建议与医生最终结论的偏差集中点。</p>
        </div>
      </div>
      <div v-if="aiSummary.fieldBreakdown?.length" class="field-grid">
        <article v-for="item in aiSummary.fieldBreakdown" :key="item.fieldKey" class="subcard field-card">
          <div class="field-card-head">
            <div>
              <strong>{{ item.fieldLabel }}</strong>
              <p>已对比 {{ item.comparedCount }} / {{ item.totalCount }}</p>
            </div>
            <span class="field-rate">{{ item.mismatchRateText === '-' ? '偏差率 -' : `偏差率 ${item.mismatchRateText}` }}</span>
          </div>
          <div class="field-metrics">
            <article>
              <span>一致</span>
              <strong>{{ item.matchCount }}</strong>
            </article>
            <article>
              <span>偏差</span>
              <strong>{{ item.mismatchCount }}</strong>
            </article>
            <article>
              <span>待补充</span>
              <strong>{{ item.pendingCount }}</strong>
            </article>
          </div>
          <div>
            <small class="field-reason-title">偏差关联原因</small>
            <div v-if="item.mismatchReasonBreakdown?.length" class="reason-chip-list">
              <span v-for="reason in item.mismatchReasonBreakdown.slice(0, 4)" :key="`${item.fieldKey}-${reason.reasonCode}`" class="reason-chip">
                {{ reason.reasonLabel }} {{ reason.count }}次
              </span>
            </div>
            <span v-else class="muted-copy">当前暂无可关联的差异原因</span>
          </div>
          <div class="field-card-actions">
            <el-button link type="danger" :disabled="!item.mismatchCount" @click="openFieldSampleDialog(item, 'mismatch')">查看偏差样本</el-button>
            <el-button link type="primary" :disabled="!item.pendingCount" @click="openFieldSampleDialog(item, 'pending')">查看待补充样本</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="当前暂无字段级 AI 偏差分析数据" />
      <div class="head summary-subhead">
        <div>
          <h3>最近差异记录</h3>
          <p>优先查看 AI 与医生结论存在偏差的问诊单，便于继续做人工纠偏和规则优化。</p>
        </div>
      </div>
      <div v-if="aiSummary.recentMismatchRecords?.length" class="mismatch-list">
        <article v-for="item in aiSummary.recentMismatchRecords" :key="item.consultationId" class="subcard mismatch-card">
          <div class="chips">
            <span>{{ item.consultationNo }}</span>
            <span>{{ item.patientName }}</span>
            <span>{{ item.categoryName }}</span>
            <span>{{ item.departmentName || '未分配科室' }}</span>
            <span>{{ formatDate(item.updateTime) }}</span>
          </div>
          <p class="copy"><strong>处理医生：</strong>{{ item.doctorName || '未记录' }}</p>
          <p class="copy"><strong>病情等级：</strong>AI {{ item.aiConditionLevel ? conditionLevelLabel(item.aiConditionLevel) : '未提供' }} / 医生 {{ item.doctorConditionLevel ? conditionLevelLabel(item.doctorConditionLevel) : '未填写' }}</p>
          <p class="copy"><strong>处理去向：</strong>AI {{ item.aiDisposition ? dispositionLabel(item.aiDisposition) : '未提供' }} / 医生 {{ item.doctorDisposition ? dispositionLabel(item.doctorDisposition) : '未填写' }}</p>
          <p class="copy"><strong>随访安排：</strong>AI {{ item.aiFollowUpText || '未提供' }} / 医生 {{ item.doctorFollowUpText || '未填写' }}</p>
          <p v-if="item.aiReasonText" class="copy"><strong>AI 推荐依据：</strong>{{ item.aiReasonText }}</p>
          <div v-if="mismatchReasonLabels(item.mismatchReasonCodes).length" class="chips danger">
            <span v-for="tag in mismatchReasonLabels(item.mismatchReasonCodes)" :key="tag">{{ tag }}</span>
          </div>
          <p v-if="item.mismatchRemark" class="copy"><strong>差异说明：</strong>{{ item.mismatchRemark }}</p>
          <div class="actions">
            <el-button link type="primary" @click="openDetail(item.consultationId)">查看详情</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="当前暂无已标记为与 AI 不一致的问诊记录" />
    </section>

    <section class="card block">
      <div class="head">
        <div class="toolbar">
          <el-input v-model="keyword" clearable placeholder="搜索单号、就诊人、标题或主诉" style="width:260px" />
          <el-select v-model="categoryFilter" clearable placeholder="全部分类" style="width:160px">
            <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="triageFilter" clearable placeholder="全部分诊等级" style="width:160px">
            <el-option v-for="item in triageOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="statusFilter" clearable placeholder="全部状态" style="width:140px">
            <el-option label="已提交" value="submitted" />
            <el-option label="已分诊" value="triaged" />
            <el-option label="处理中" value="processing" />
            <el-option label="已完成" value="completed" />
          </el-select>
          <el-select v-model="assignmentFilter" clearable placeholder="全部认领状态" style="width:160px">
            <el-option label="待认领" value="unclaimed" />
            <el-option label="已认领" value="claimed" />
            <el-option label="已释放" value="released" />
          </el-select>
          <el-select v-model="dispatchFilter" clearable placeholder="全部智能分配" style="width:180px">
            <el-option label="等待首推医生" value="waiting_accept" />
            <el-option label="首推医生已接手" value="claimed_by_suggested" />
            <el-option label="其他医生接手" value="claimed_by_other" />
            <el-option label="科室分诊队列" value="department_queue" />
          </el-select>
          <el-select v-model="feedbackFilter" clearable placeholder="全部服务评价" style="width:180px">
            <el-option label="已提交评价" value="has_feedback" />
            <el-option label="待关注评价" value="attention" />
            <el-option label="低分评价" value="low_score" />
            <el-option label="未解决评价" value="unresolved" />
            <el-option label="医生待处理" value="unhandled" />
            <el-option label="超时未处理" value="overdue_unhandled" />
            <el-option label="医生已处理" value="handled" />
          </el-select>
        </div>
        <el-button @click="refreshAll">刷新</el-button>
      </div>

      <el-table :data="filteredRecords" v-loading="loading" border>
        <el-table-column prop="consultationNo" label="问诊单号" min-width="170" />
        <el-table-column prop="patientName" label="就诊人" min-width="110" />
        <el-table-column prop="categoryName" label="问诊分类" min-width="130" />
        <el-table-column label="初步分诊" min-width="140" align="center">
          <template #default="{ row }">
            <span class="badge" :style="triageBadgeStyle(row.triageLevelColor)">{{ row.triageLevelName || '待评估' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="认领状态" min-width="180">
          <template #default="{ row }">
            <div class="assignment">
              <el-tag :type="assignmentTagType(row.doctorAssignment)" effect="light">{{ assignmentStatusLabel(row.doctorAssignment) }}</el-tag>
              <span v-if="row.doctorAssignment?.doctorName">{{ row.doctorAssignment.doctorName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="智能分配" min-width="210">
          <template #default="{ row }">
            <div class="assignment">
              <el-tag :type="smartDispatchTagType(row.smartDispatch)" effect="light">{{ smartDispatchStatusLabel(row.smartDispatch) }}</el-tag>
              <span>{{ smartDispatchLine(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="服务评价" min-width="280">
          <template #default="{ row }">
            <div v-if="row.serviceFeedback" class="feedback-cell">
              <div class="feedback-tag-row">
                <el-tag :type="serviceFeedbackScoreTagType(row.serviceFeedback.serviceScore)" effect="light">
                  {{ row.serviceFeedback.serviceScore ?? '-' }}/5 分
                </el-tag>
                <el-tag :type="serviceFeedbackResolvedTagType(row.serviceFeedback.isResolved)" effect="light">
                  {{ serviceFeedbackResolvedLabel(row.serviceFeedback.isResolved) }}
                </el-tag>
                <el-tag :type="serviceFeedbackHandleTagType(row.serviceFeedback.doctorHandleStatus)" effect="light">
                  {{ serviceFeedbackHandleLabel(row.serviceFeedback.doctorHandleStatus) }}
                </el-tag>
              </div>
              <span class="feedback-cell-copy">{{ row.serviceFeedback.feedbackText || '患者未填写补充评价内容。' }}</span>
            </div>
            <span v-else class="muted-copy">暂无服务评价</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }"><el-tag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="160">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="openDetail(row.id)">详情</el-button></template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="fieldSampleVisible" :title="fieldSampleDialogTitle" width="920px" destroy-on-close>
      <div class="field-sample-toolbar">
        <div class="chips">
          <span>{{ fieldSampleMeta.fieldLabel || '-' }}</span>
          <span v-if="fieldSampleMeta.mismatchCount !== null">偏差 {{ fieldSampleMeta.mismatchCount }}</span>
          <span v-if="fieldSampleMeta.pendingCount !== null">待补充 {{ fieldSampleMeta.pendingCount }}</span>
        </div>
        <div class="field-sample-status-group">
          <el-button :type="fieldSampleStatus === 'mismatch' ? 'danger' : 'default'" plain @click="switchFieldSampleStatus('mismatch')">偏差样本</el-button>
          <el-button :type="fieldSampleStatus === 'pending' ? 'primary' : 'default'" plain @click="switchFieldSampleStatus('pending')">待补充样本</el-button>
          <el-button plain :disabled="!fieldSampleMeta.fieldKey" :loading="fieldSampleExporting" @click="exportFieldSamples">Export CSV</el-button>
        </div>
      </div>
      <div class="field-sample-filter-bar">
        <el-input
          v-model="fieldSampleFilters.keyword"
          clearable
          placeholder="搜索单号、患者、医生或字段取值"
          style="width: 240px"
          @keyup.enter="loadFieldSamples"
        />
        <el-select v-model="fieldSampleFilters.doctorName" clearable placeholder="全部医生" style="width: 160px">
          <el-option v-for="item in fieldSampleDoctorOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="fieldSampleFilters.categoryName" clearable placeholder="全部分类" style="width: 160px">
          <el-option v-for="item in fieldSampleCategoryOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="fieldSampleFilters.departmentName" clearable placeholder="全部科室" style="width: 160px">
          <el-option v-for="item in fieldSampleDepartmentOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button type="primary" plain @click="loadFieldSamples">筛选</el-button>
        <el-button @click="resetFieldSampleFilters">重置</el-button>
      </div>
      <div v-loading="fieldSampleLoading">
        <template v-if="fieldSamples.length">
          <div class="field-sample-list">
          <article v-for="item in fieldSamples" :key="`${item.fieldKey}-${item.consultationId}-${item.compareStatus}`" class="subcard field-sample-card">
            <div class="chips">
              <span>{{ item.consultationNo }}</span>
              <span>{{ item.patientName }}</span>
              <span>{{ item.categoryName }}</span>
              <span>{{ item.departmentName || '未分配科室' }}</span>
              <span :class="['compare-badge', comparisonStatusClass(item.compareStatus)]">{{ comparisonStatusLabel(item.compareStatus) }}</span>
              <span>{{ formatDate(item.updateTime) }}</span>
            </div>
            <p class="copy"><strong>处理医生：</strong>{{ item.doctorName || '待处理' }}</p>
            <p class="copy"><strong>{{ item.fieldLabel }}：</strong>AI {{ item.aiValueText || '未提供' }} / 医生 {{ item.doctorValueText || '待补充' }}</p>
            <div v-if="mismatchReasonLabels(item.mismatchReasonCodes).length" class="chips danger">
              <span v-for="tag in mismatchReasonLabels(item.mismatchReasonCodes)" :key="`${item.consultationId}-${tag}`">{{ tag }}</span>
            </div>
            <p v-if="item.mismatchRemark" class="copy"><strong>差异说明：</strong>{{ item.mismatchRemark }}</p>
            <div class="field-sample-actions">
              <el-button link type="primary" @click="openDetailFromFieldSample(item.consultationId)">查看详情</el-button>
            </div>
          </article>
        </div>
        </template>
        <el-empty v-else :description="fieldSampleStatus === 'mismatch' ? '当前暂无该字段的偏差样本' : '当前暂无该字段的待补充样本'" />
        <div v-if="fieldSamples.length" class="sample-footer">
          <span class="muted-copy">已加载 {{ fieldSamples.length }} 条样本</span>
          <el-button v-if="fieldSampleHasMore" plain :loading="fieldSampleLoading" @click="loadMoreFieldSamples">加载更多</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="mismatchSampleVisible" :title="mismatchSampleDialogTitle" width="920px" destroy-on-close>
      <div class="field-sample-toolbar">
        <div class="chips">
          <span>AI 偏差样本</span>
          <span v-if="mismatchSampleFilters.doctorName">医生 {{ mismatchSampleFilters.doctorName }}</span>
          <span v-if="mismatchSampleFilters.reasonCode">原因 {{ aiMismatchReasonLabel(mismatchSampleFilters.reasonCode) }}</span>
          <span v-if="mismatchSampleMeta.sourceCount !== null">命中 {{ mismatchSampleMeta.sourceCount }}</span>
        </div>
        <el-button :loading="mismatchSampleLoading" @click="loadMismatchSamples">刷新样本</el-button>
      </div>
      <div class="field-sample-filter-bar">
        <el-input
          v-model="mismatchSampleFilters.keyword"
          clearable
          placeholder="搜索单号、患者、医生或 AI 推荐依据"
          style="width: 240px"
          @keyup.enter="loadMismatchSamples"
        />
        <el-select v-model="mismatchSampleFilters.doctorName" clearable placeholder="全部医生" style="width: 160px">
          <el-option v-for="item in fieldSampleDoctorOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="mismatchSampleFilters.reasonCode" clearable placeholder="全部原因" style="width: 180px">
          <el-option v-for="item in mismatchSampleReasonOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="mismatchSampleFilters.categoryName" clearable placeholder="全部分类" style="width: 160px">
          <el-option v-for="item in fieldSampleCategoryOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="mismatchSampleFilters.departmentName" clearable placeholder="全部科室" style="width: 160px">
          <el-option v-for="item in fieldSampleDepartmentOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button type="primary" plain @click="loadMismatchSamples">筛选</el-button>
        <el-button @click="resetMismatchSampleFilters">重置</el-button>
        <el-button plain :loading="mismatchSampleExporting" @click="exportMismatchSamples">Export CSV</el-button>
      </div>
      <div v-loading="mismatchSampleLoading">
        <div v-if="mismatchSamples.length" class="mismatch-list">
          <article v-for="item in mismatchSamples" :key="`${item.consultationId}-${item.updateTime || ''}`" class="subcard mismatch-card">
            <div class="chips">
              <span>{{ item.consultationNo }}</span>
              <span>{{ item.patientName }}</span>
              <span>{{ item.categoryName }}</span>
              <span>{{ item.departmentName || '未分配科室' }}</span>
              <span>{{ formatDate(item.updateTime) }}</span>
            </div>
            <p class="copy"><strong>处理医生：</strong>{{ item.doctorName || '未记录' }}</p>
            <p class="copy"><strong>病情等级：</strong>AI {{ item.aiConditionLevel ? conditionLevelLabel(item.aiConditionLevel) : '未提供' }} / 医生 {{ item.doctorConditionLevel ? conditionLevelLabel(item.doctorConditionLevel) : '未填写' }}</p>
            <p class="copy"><strong>处理去向：</strong>AI {{ item.aiDisposition ? dispositionLabel(item.aiDisposition) : '未提供' }} / 医生 {{ item.doctorDisposition ? dispositionLabel(item.doctorDisposition) : '未填写' }}</p>
            <p class="copy"><strong>随访安排：</strong>AI {{ item.aiFollowUpText || '未提供' }} / 医生 {{ item.doctorFollowUpText || '未填写' }}</p>
            <p v-if="item.aiReasonText" class="copy"><strong>AI 推荐依据：</strong>{{ item.aiReasonText }}</p>
            <div v-if="mismatchReasonLabels(item.mismatchReasonCodes).length" class="chips danger">
              <span v-for="tag in mismatchReasonLabels(item.mismatchReasonCodes)" :key="`${item.consultationId}-${tag}`">{{ tag }}</span>
            </div>
            <p v-if="item.mismatchRemark" class="copy"><strong>偏差说明：</strong>{{ item.mismatchRemark }}</p>
            <div class="actions">
              <el-button link type="primary" @click="openDetailFromMismatchSample(item.consultationId)">查看详情</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前暂无符合条件的 AI 偏差样本" />
        <div v-if="mismatchSamples.length" class="sample-footer">
          <span class="muted-copy">已加载 {{ mismatchSamples.length }} 条样本</span>
          <el-button v-if="mismatchSampleHasMore" plain :loading="mismatchSampleLoading" @click="loadMoreMismatchSamples">加载更多</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="问诊记录详情" width="1080px" destroy-on-close @closed="handleDetailDialogClosed">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <div class="meta">
            <article><span>问诊单号</span><strong>{{ detail.consultationNo }}</strong></article>
            <article><span>就诊人</span><strong>{{ detail.patientName }}</strong></article>
            <article><span>问诊分类</span><strong>{{ detail.categoryName }}</strong></article>
            <article><span>推荐科室</span><strong>{{ detail.departmentName || '待分配' }}</strong></article>
            <article><span>初步分诊</span><strong><span class="badge" :style="triageBadgeStyle(detail.triageLevelColor)">{{ detail.triageLevelName || '待评估' }}</span></strong></article>
            <article><span>当前状态</span><strong>{{ statusLabel(detail.status) }}</strong></article>
          </div>

          <section class="card panel">
            <h3>问诊摘要</h3>
            <p class="copy"><strong>标题：</strong>{{ detail.title }}</p>
            <p class="copy"><strong>主诉摘要：</strong>{{ detail.chiefComplaint || '未自动提取' }}</p>
            <p class="copy"><strong>健康摘要：</strong>{{ detail.healthSummary || '未关联健康档案摘要' }}</p>
            <p class="copy"><strong>系统建议：</strong>{{ detail.triageSuggestion || '当前暂无额外建议' }}</p>
            <p class="copy"><strong>规则摘要：</strong>{{ detail.triageRuleSummary || '当前未命中额外高风险规则' }}</p>
          </section>

          <section class="card panel">
            <div class="head">
              <div>
                <h3>认领归档</h3>
                <p>查看医生接单锁定情况，便于运营和质控追踪。</p>
              </div>
              <div class="chips" v-if="detail.doctorAssignment">
                <span>{{ assignmentStatusLabel(detail.doctorAssignment) }}</span>
                <span>{{ detail.doctorAssignment.doctorName || '-' }}</span>
                <span v-if="detail.doctorAssignment.claimTime">认领 {{ formatDate(detail.doctorAssignment.claimTime) }}</span>
                <span v-if="detail.doctorAssignment.releaseTime">释放 {{ formatDate(detail.doctorAssignment.releaseTime) }}</span>
              </div>
            </div>
            <div class="subcard">
              <p class="copy"><strong>当前状态：</strong>{{ assignmentStatusLabel(detail.doctorAssignment) }}</p>
              <p class="copy"><strong>认领医生：</strong>{{ detail.doctorAssignment?.doctorName || '暂无认领医生' }}</p>
              <p class="copy"><strong>科室快照：</strong>{{ detail.doctorAssignment?.departmentName || detail.departmentName || '-' }}</p>
            </div>
          </section>

          <section class="card panel">
            <div class="head">
              <div>
                <h3>智能分配归档</h3>
                <p>{{ smartDispatchHintText(detail?.smartDispatch) }}</p>
              </div>
              <div class="chips">
                <span>{{ smartDispatchStatusLabel(detail?.smartDispatch) }}</span>
                <span v-if="getSmartDispatch(detail).suggestedDoctorName">首推 {{ getSmartDispatch(detail).suggestedDoctorName }}</span>
                <span v-if="getSmartDispatch(detail).candidateCount">候选 {{ getSmartDispatch(detail).candidateCount }} 位</span>
              </div>
            </div>
            <div class="subcard">
              <p class="copy"><strong>当前进度：</strong>{{ smartDispatchHintText(detail?.smartDispatch) }}</p>
              <p v-if="getSmartDispatch(detail).suggestedDoctorName" class="copy">
                <strong>优先医生：</strong>{{ getSmartDispatch(detail).suggestedDoctorName }}{{ getSmartDispatch(detail).suggestedDoctorTitle ? ` / ${getSmartDispatch(detail).suggestedDoctorTitle}` : '' }}
              </p>
              <p v-if="getSmartDispatch(detail).recommendationReason" class="copy"><strong>推荐依据：</strong>{{ getSmartDispatch(detail).recommendationReason }}</p>
              <p v-if="getSmartDispatch(detail).suggestedDoctorNextScheduleText" class="copy"><strong>排班参考：</strong>{{ getSmartDispatch(detail).suggestedDoctorNextScheduleText }}</p>
            </div>
          </section>

          <section v-if="detail.doctorHandle" class="card panel">
            <div class="head">
              <div>
                <h3>医生处理归档</h3>
                <p>查看医生是否已接手当前问诊，以及本次处理建议和随访安排。</p>
              </div>
              <div class="chips">
                <span>{{ detail.doctorHandle.doctorName || '未指派医生' }}</span>
                <span>{{ handleStatusLabel(detail.doctorHandle.status) }}</span>
                <span>接手 {{ formatDate(detail.doctorHandle.receiveTime) }}</span>
                <span v-if="detail.doctorHandle.completeTime">完成 {{ formatDate(detail.doctorHandle.completeTime) }}</span>
              </div>
            </div>
            <div class="subcard">
              <p class="copy"><strong>判断摘要：</strong>{{ detail.doctorHandle.summary || '暂无摘要' }}</p>
              <p class="copy"><strong>处理建议：</strong>{{ detail.doctorHandle.medicalAdvice || '暂无处理建议' }}</p>
              <p class="copy"><strong>随访计划：</strong>{{ detail.doctorHandle.followUpPlan || '暂无随访安排' }}</p>
              <p class="copy"><strong>内部备注：</strong>{{ detail.doctorHandle.internalRemark || '暂无内部备注' }}</p>
            </div>
          </section>

          <section v-if="detail.doctorConclusion" class="card panel">
            <div class="head">
              <div>
                <h3>结构化结论</h3>
                <p>查看医生沉淀的标准化结论，用于 AI 复盘和统计分析。</p>
              </div>
              <div class="chips">
                <span>{{ conditionLevelLabel(detail.doctorConclusion.conditionLevel) }}</span>
                <span>{{ dispositionLabel(detail.doctorConclusion.disposition) }}</span>
                <span>{{ aiConsistencyLabel(detail.doctorConclusion.isConsistentWithAi) }}</span>
                <span>{{ detail.doctorConclusion.needFollowUp === 1 ? '需要随访' : '无需随访' }}</span>
                <span v-if="detail.doctorConclusion.followUpWithinDays">{{ detail.doctorConclusion.followUpWithinDays }} 天内随访</span>
              </div>
            </div>
            <div class="subcard">
              <p class="copy"><strong>诊断方向：</strong>{{ detail.doctorConclusion.diagnosisDirection || '未填写' }}</p>
              <p class="copy"><strong>患者指导：</strong>{{ detail.doctorConclusion.patientInstruction || '暂无患者指导' }}</p>
            </div>
            <div v-if="parseJsonArray(detail.doctorConclusion.conclusionTagsJson).length" class="chips">
              <span v-for="item in parseJsonArray(detail.doctorConclusion.conclusionTagsJson)" :key="item">{{ item }}</span>
            </div>
            <div v-if="doctorConclusionMismatchReasonLabels(detail.doctorConclusion).length" class="chips danger">
              <span v-for="item in doctorConclusionMismatchReasonLabels(detail.doctorConclusion)" :key="item">{{ item }}</span>
            </div>
            <p v-if="detail.doctorConclusion.aiMismatchRemark" class="copy"><strong>差异说明：</strong>{{ detail.doctorConclusion.aiMismatchRemark }}</p>
          </section>

          <section v-if="detail.aiComparison" class="card panel compare-panel">
            <div class="head">
              <div>
                <h3>AI 采纳摘要</h3>
                <p>{{ detail.aiComparison.summary }}</p>
              </div>
              <div class="chips">
                <span :class="['compare-badge', comparisonStatusClass(detail.aiComparison.overallStatus)]">{{ comparisonStatusLabel(detail.aiComparison.overallStatus) }}</span>
                <span v-if="detail.doctorConclusion">{{ aiConsistencyLabel(detail.doctorConclusion.isConsistentWithAi) }}</span>
              </div>
            </div>
            <div class="compare-grid">
              <article class="subcard compare-card">
                <strong>AI 建议</strong>
                <p class="copy"><strong>病情等级：</strong>{{ detail.aiComparison.aiConditionLevel ? conditionLevelLabel(detail.aiComparison.aiConditionLevel) : '未提供' }}</p>
                <p class="copy"><strong>处理去向：</strong>{{ detail.aiComparison.aiDisposition ? dispositionLabel(detail.aiComparison.aiDisposition) : '未提供' }}</p>
                <p class="copy"><strong>建议科室：</strong>{{ detail.aiComparison.aiDepartmentName || '未提供' }}</p>
                <p class="copy"><strong>随访建议：</strong>{{ detail.aiComparison.aiFollowUpText || '未提供' }}</p>
                <p class="copy"><strong>置信度：</strong>{{ detail.aiComparison.aiConfidenceText || '未提供' }}</p>
                <p v-if="detail.aiComparison.aiReasonText" class="copy"><strong>推荐依据：</strong>{{ detail.aiComparison.aiReasonText }}</p>
              </article>
              <article class="subcard compare-card">
                <strong>医生最终结论</strong>
                <p class="copy"><strong>病情等级：</strong>{{ detail.doctorConclusion?.conditionLevel ? conditionLevelLabel(detail.doctorConclusion.conditionLevel) : '待医生判断' }}</p>
                <p class="copy"><strong>处理去向：</strong>{{ detail.doctorConclusion?.disposition ? dispositionLabel(detail.doctorConclusion.disposition) : '待医生判断' }}</p>
                <p class="copy"><strong>诊断方向：</strong>{{ detail.doctorConclusion?.diagnosisDirection || '待医生填写' }}</p>
                <p class="copy"><strong>随访建议：</strong>{{ doctorFollowUpText(detail.doctorConclusion) || '待医生判断' }}</p>
                <p class="copy"><strong>AI 一致性：</strong>{{ detail.doctorConclusion ? aiConsistencyLabel(detail.doctorConclusion.isConsistentWithAi) : '待医生判断' }}</p>
              </article>
            </div>
            <div class="compare-list">
              <article class="subcard compare-item">
                <div>
                  <strong>病情等级</strong>
                  <p class="copy">AI：{{ detail.aiComparison.aiConditionLevel ? conditionLevelLabel(detail.aiComparison.aiConditionLevel) : '未提供' }}</p>
                  <p class="copy">医生：{{ detail.doctorConclusion?.conditionLevel ? conditionLevelLabel(detail.doctorConclusion.conditionLevel) : '待医生判断' }}</p>
                </div>
                <span :class="['compare-badge', comparisonStatusClass(detail.aiComparison.conditionLevelStatus)]">{{ comparisonStatusLabel(detail.aiComparison.conditionLevelStatus) }}</span>
              </article>
              <article class="subcard compare-item">
                <div>
                  <strong>处理去向</strong>
                  <p class="copy">AI：{{ detail.aiComparison.aiDisposition ? dispositionLabel(detail.aiComparison.aiDisposition) : '未提供' }}</p>
                  <p class="copy">医生：{{ detail.doctorConclusion?.disposition ? dispositionLabel(detail.doctorConclusion.disposition) : '待医生判断' }}</p>
                </div>
                <span :class="['compare-badge', comparisonStatusClass(detail.aiComparison.dispositionStatus)]">{{ comparisonStatusLabel(detail.aiComparison.dispositionStatus) }}</span>
              </article>
              <article class="subcard compare-item">
                <div>
                  <strong>随访安排</strong>
                  <p class="copy">AI：{{ detail.aiComparison.aiFollowUpText || '未提供' }}</p>
                  <p class="copy">医生：{{ doctorFollowUpText(detail.doctorConclusion) || '待医生判断' }}</p>
                </div>
                <span :class="['compare-badge', comparisonStatusClass(detail.aiComparison.followUpStatus)]">{{ comparisonStatusLabel(detail.aiComparison.followUpStatus) }}</span>
              </article>
            </div>
            <div v-if="detail.aiComparison.aiRecommendedDoctors?.length" class="chips">
              <span v-for="item in detail.aiComparison.aiRecommendedDoctors" :key="item">{{ item }}</span>
            </div>
            <div v-if="detail.aiComparison.aiRiskFlags?.length" class="chips danger">
              <span v-for="item in detail.aiComparison.aiRiskFlags" :key="item">{{ item }}</span>
            </div>
          </section>

          <section class="card panel">
            <div class="head">
              <div>
                <h3>随访记录</h3>
                <p>查看医生对已完成问诊单的后续跟踪记录。</p>
              </div>
              <div class="chips" v-if="detail.doctorFollowUps?.length">
                <span>共 {{ detail.doctorFollowUps.length }} 条</span>
                <span v-if="detail.doctorFollowUps[0]?.createTime">最近随访 {{ formatDate(detail.doctorFollowUps[0].createTime) }}</span>
              </div>
            </div>
            <div v-if="detail.doctorFollowUps?.length" class="answer-grid">
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
          </section>

          <section v-if="detail.triageResult" class="card panel">
            <div class="head">
              <div>
                <h3>分诊结果归档</h3>
                <p>查看最终推荐结果、风险摘要和推荐医生。</p>
              </div>
              <div class="chips">
                <span>{{ detail.triageResult.triageLevelName || '待评估' }}</span>
                <span>{{ detail.triageResult.departmentName || '未匹配科室' }}</span>
                <span>置信度 {{ formatConfidence(detail.triageResult.confidenceScore) }}</span>
                <span v-if="detail.triageResult.doctorName">推荐医生 {{ detail.triageResult.doctorName }}</span>
              </div>
            </div>
            <div class="subcard">
              <p class="copy"><strong>结果说明：</strong>{{ detail.triageResult.reasonText || '暂无结果说明' }}</p>
              <p class="copy"><strong>风险标签：</strong>{{ parseJsonArray(detail.triageResult.riskFlagsJson).join('、') || '暂无风险标签' }}</p>
            </div>
          </section>

          <section v-if="detail.triageFeedback" class="card panel">
            <div class="head">
              <div>
                <h3>用户反馈</h3>
                <p>查看用户对本次导诊的采纳情况和人工纠偏信息。</p>
              </div>
              <div class="chips">
                <span>{{ detail.triageFeedback.isAdopted === 1 ? '已采纳' : '未采纳' }}</span>
                <span>评分 {{ detail.triageFeedback.userScore }}/5</span>
                <span v-if="detail.triageFeedback.manualCorrectDepartmentName">修正科室 {{ detail.triageFeedback.manualCorrectDepartmentName }}</span>
                <span v-if="detail.triageFeedback.manualCorrectDoctorName">修正医生 {{ detail.triageFeedback.manualCorrectDoctorName }}</span>
              </div>
            </div>
            <div class="subcard">
              <p class="copy"><strong>反馈内容：</strong>{{ detail.triageFeedback.feedbackText || '用户未填写额外说明' }}</p>
              <p class="copy"><strong>反馈时间：</strong>{{ formatDate(detail.triageFeedback.updateTime || detail.triageFeedback.createTime) }}</p>
            </div>
          </section>

          <section class="card panel">
            <div class="head">
              <div>
                <h3>问诊服务评价</h3>
                <p>查看患者对本次医生在线问诊服务的评分、问题解决情况和补充反馈。</p>
              </div>
            </div>
            <div v-if="detail.serviceFeedback" class="subcard">
              <div class="chips">
                <span>服务评分：{{ detail.serviceFeedback.serviceScore ?? '-' }}/5</span>
                <span>{{ serviceFeedbackResolvedLabel(detail.serviceFeedback.isResolved) }}</span>
                <span>{{ serviceFeedbackHandleLabel(detail.serviceFeedback.doctorHandleStatus) }}</span>
                <span v-if="detail.serviceFeedback.patientName">评价人：{{ detail.serviceFeedback.patientName }}</span>
                <span>评价时间：{{ formatDate(detail.serviceFeedback.updateTime || detail.serviceFeedback.createTime) }}</span>
                <span v-if="detail.serviceFeedback.doctorHandleTime">处理时间：{{ formatDate(detail.serviceFeedback.doctorHandleTime) }}</span>
              </div>
              <p class="copy">{{ detail.serviceFeedback.feedbackText || '患者未填写补充评价内容。' }}</p>
              <p v-if="detail.serviceFeedback.doctorHandleDoctorName" class="copy"><strong>处理医生：</strong>{{ detail.serviceFeedback.doctorHandleDoctorName }}</p>
              <p v-if="detail.serviceFeedback.doctorHandleRemark" class="copy"><strong>处理备注：</strong>{{ detail.serviceFeedback.doctorHandleRemark }}</p>
            </div>
            <el-empty v-else description="当前暂无问诊服务评价" />
          </section>

          <section class="card panel">
            <h3>规则命中日志</h3>
            <el-table v-if="detail.ruleHits?.length" :data="detail.ruleHits" border>
              <el-table-column label="主规则" width="90" align="center">
                <template #default="{ row }"><el-tag v-if="row.isPrimary === 1" type="danger" effect="light">主规则</el-tag><span v-else>-</span></template>
              </el-table-column>
              <el-table-column prop="ruleName" label="规则名称" min-width="170" />
              <el-table-column prop="ruleCode" label="规则编码" min-width="150" />
              <el-table-column prop="triageLevelName" label="规则等级" min-width="120" />
              <el-table-column prop="matchedSummary" label="命中依据" min-width="220" show-overflow-tooltip />
            </el-table>
            <el-empty v-else description="当前记录未命中额外规则" />
          </section>

          <section class="card panel">
            <h3>问诊答案</h3>
            <div v-if="detail.answers?.length" class="answer-grid">
              <article v-for="answer in detail.answers" :key="answer.id" class="subcard">
                <strong>{{ answer.fieldLabel }}</strong>
                <div class="copy">
                  <template v-if="answer.fieldType === 'upload' && answer.fieldValue"><img :src="resolveImagePath(answer.fieldValue)" :alt="answer.fieldLabel" class="image" /></template>
                  <template v-else-if="answer.fieldType === 'multi_select'"><div class="chips"><span v-for="item in parseJsonArray(answer.fieldValue)" :key="item">{{ item }}</span></div></template>
                  <template v-else>{{ displayAnswer(answer) }}</template>
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无问诊答案" />
          </section>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { download, get, resolveImagePath } from '@/net'
import { aiMismatchReasonLabel, aiMismatchReasonOptions, comparisonStatusClass, comparisonStatusLabel } from '@/triage/comparison'
import { normalizeSmartDispatch, smartDispatchHintText, smartDispatchStatusLabel, smartDispatchTagType } from '@/triage/dispatch'

const SAMPLE_PAGE_SIZE = 20
const SERVICE_FEEDBACK_HANDLE_OVERDUE_HOURS = 24
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const currentDetailId = ref(null)
const summaryLoading = ref(false)
const dispatchSummaryLoading = ref(false)
const mismatchSampleVisible = ref(false)
const mismatchSampleLoading = ref(false)
const mismatchSampleExporting = ref(false)
const mismatchSampleOffset = ref(0)
const mismatchSampleHasMore = ref(false)
const fieldSampleVisible = ref(false)
const fieldSampleLoading = ref(false)
const fieldSampleExporting = ref(false)
const fieldSampleOffset = ref(0)
const fieldSampleHasMore = ref(false)
const records = ref([])
const detail = ref(null)
const aiSummary = ref(createEmptyAiSummary())
const dispatchSummary = ref(createEmptyDispatchSummary())
const mismatchSamples = ref([])
const fieldSamples = ref([])
const keyword = ref('')
const categoryFilter = ref('')
const triageFilter = ref('')
const statusFilter = ref('')
const assignmentFilter = ref('')
const dispatchFilter = ref('')
const feedbackFilter = ref('')
const mismatchSampleMeta = ref(createEmptyMismatchSampleMeta())
const mismatchSampleFilters = reactive(createEmptyMismatchSampleFilters())
const fieldSampleStatus = ref('mismatch')
const fieldSampleMeta = ref(createEmptyFieldSampleMeta())
const fieldSampleFilters = reactive(createEmptyFieldSampleFilters())

function createEmptyAiSummary() {
  return {
    totalRecordCount: 0,
    comparedCount: 0,
    consistentCount: 0,
    mismatchCount: 0,
    pendingCount: 0,
    coverageText: '-',
    consistentRateText: '-',
    departmentBreakdown: [],
    categoryBreakdown: [],
    fieldBreakdown: [],
    doctorBreakdown: [],
    mismatchReasonBreakdown: [],
    recentMismatchRecords: []
  }
}

function createEmptyDispatchSummary() {
  return {
    totalRecordCount: 0,
    suggestedCount: 0,
    waitingAcceptCount: 0,
    claimedBySuggestedCount: 0,
    claimedByOtherCount: 0,
    departmentQueueCount: 0,
    overdueWaitingCount: 0,
    overdueThresholdHours: 24,
    suggestionCoverageText: '-',
    suggestedHitRateText: '-',
    claimedByOtherRateText: '-',
    averageClaimDurationText: '-',
    doctorBreakdown: [],
    recentWaitingRecords: []
  }
}

function createEmptyFieldSampleMeta() {
  return {
    fieldKey: '',
    fieldLabel: '',
    mismatchCount: null,
    pendingCount: null
  }
}

function createEmptyMismatchSampleMeta() {
  return {
    title: 'AI 偏差样本',
    sourceCount: null,
    defaultFilters: createEmptyMismatchSampleFilters()
  }
}

function createEmptyMismatchSampleFilters() {
  return {
    keyword: '',
    doctorName: '',
    reasonCode: '',
    categoryName: '',
    departmentName: ''
  }
}

function createEmptyFieldSampleFilters() {
  return {
    keyword: '',
    doctorName: '',
    categoryName: '',
    departmentName: ''
  }
}

const assignedCount = computed(() => records.value.filter(item => item.doctorAssignment?.status === 'claimed').length)
const completedCount = computed(() => records.value.filter(item => item.status === 'completed').length)
const todayCount = computed(() => {
  const today = new Date().toDateString()
  return records.value.filter(item => item.createTime && new Date(item.createTime).toDateString() === today).length
})
const waitingDispatchCount = computed(() => records.value.filter(item => getSmartDispatch(item).status === 'waiting_accept').length)
const suggestedAcceptedCount = computed(() => records.value.filter(item => getSmartDispatch(item).status === 'claimed_by_suggested').length)
const serviceFeedbackRecords = computed(() => records.value.filter(hasServiceFeedback))
const serviceFeedbackCount = computed(() => serviceFeedbackRecords.value.length)
const attentionServiceFeedbackRecords = computed(() => serviceFeedbackRecords.value
  .filter(isAttentionServiceFeedbackRecord)
  .slice()
  .sort(compareServiceFeedbackRecord))
const attentionServiceFeedbackCount = computed(() => attentionServiceFeedbackRecords.value.length)
const attentionServiceFeedbackPreview = computed(() => attentionServiceFeedbackRecords.value.slice(0, 6))
const lowScoreServiceFeedbackCount = computed(() => serviceFeedbackRecords.value.filter(isLowScoreServiceFeedbackRecord).length)
const unresolvedServiceFeedbackCount = computed(() => serviceFeedbackRecords.value.filter(isUnresolvedServiceFeedbackRecord).length)
const handledServiceFeedbackCount = computed(() => serviceFeedbackRecords.value.filter(isHandledServiceFeedbackRecord).length)
const overdueUnhandledServiceFeedbackRecords = computed(() => serviceFeedbackRecords.value
  .filter(record => isOverdueUnhandledServiceFeedbackRecord(record, SERVICE_FEEDBACK_HANDLE_OVERDUE_HOURS))
  .slice()
  .sort(compareServiceFeedbackRecord))
const overdueUnhandledServiceFeedbackCount = computed(() => overdueUnhandledServiceFeedbackRecords.value.length)
const overdueUnhandledServiceFeedbackPreview = computed(() => overdueUnhandledServiceFeedbackRecords.value.slice(0, 6))
const serviceFeedbackCoverageText = computed(() => `${serviceFeedbackCount.value} / ${records.value.length}`)
const serviceFeedbackHandledRateText = computed(() => formatRateText(handledServiceFeedbackCount.value, serviceFeedbackCount.value))
const averageServiceFeedbackHandleDurationText = computed(() => {
  const durations = serviceFeedbackRecords.value
    .map(serviceFeedbackHandleDurationMs)
    .filter(value => Number.isFinite(value) && value >= 0)
  return formatAverageDurationText(durations)
})
const serviceFeedbackDepartmentBreakdown = computed(() => {
  const groups = new Map()
  serviceFeedbackRecords.value.forEach(record => {
    const departmentName = record.departmentName || record.serviceFeedback?.departmentName || '未分配科室'
    const current = groups.get(departmentName) || {
      departmentName,
      feedbackCount: 0,
      attentionCount: 0,
      lowScoreCount: 0,
      unresolvedCount: 0,
      handledCount: 0,
      handledRateText: '-'
    }
    current.feedbackCount += 1
    if (isAttentionServiceFeedbackRecord(record)) current.attentionCount += 1
    if (isLowScoreServiceFeedbackRecord(record)) current.lowScoreCount += 1
    if (isUnresolvedServiceFeedbackRecord(record)) current.unresolvedCount += 1
    if (isHandledServiceFeedbackRecord(record)) current.handledCount += 1
    groups.set(departmentName, current)
  })
  return [...groups.values()]
    .map(item => ({
      ...item,
      handledRateText: formatRateText(item.handledCount, item.feedbackCount)
    }))
    .sort((left, right) => {
      if (right.attentionCount !== left.attentionCount) return right.attentionCount - left.attentionCount
      if (right.feedbackCount !== left.feedbackCount) return right.feedbackCount - left.feedbackCount
      return `${left.departmentName}`.localeCompare(`${right.departmentName}`, 'zh-CN')
    })
})
const serviceFeedbackDoctorBreakdown = computed(() => {
  const groups = new Map()
  serviceFeedbackRecords.value.forEach(record => {
    const doctorId = record.serviceFeedback?.doctorId ?? null
    const doctorName = record.serviceFeedback?.doctorName || record.serviceFeedback?.doctorHandleDoctorName || '未分配医生'
    const departmentName = record.departmentName || record.serviceFeedback?.departmentName || '未分配科室'
    const key = doctorId != null ? `doctor:${doctorId}` : `doctor-name:${doctorName}`
    const current = groups.get(key) || {
      doctorId,
      doctorName,
      departmentName,
      feedbackCount: 0,
      attentionCount: 0,
      lowScoreCount: 0,
      overdueUnhandledCount: 0,
      handledCount: 0,
      totalHandleDurationMs: 0,
      handleDurationCount: 0,
      handledRateText: '-',
      averageHandleDurationText: '-'
    }
    current.feedbackCount += 1
    if (isAttentionServiceFeedbackRecord(record)) current.attentionCount += 1
    if (isLowScoreServiceFeedbackRecord(record)) current.lowScoreCount += 1
    if (isOverdueUnhandledServiceFeedbackRecord(record, SERVICE_FEEDBACK_HANDLE_OVERDUE_HOURS)) current.overdueUnhandledCount += 1
    if (isHandledServiceFeedbackRecord(record)) current.handledCount += 1
    const duration = serviceFeedbackHandleDurationMs(record)
    if (Number.isFinite(duration) && duration >= 0) {
      current.totalHandleDurationMs += duration
      current.handleDurationCount += 1
    }
    groups.set(key, current)
  })
  return [...groups.values()]
    .map(item => ({
      ...item,
      handledRateText: formatRateText(item.handledCount, item.feedbackCount),
      averageHandleDurationText: item.handleDurationCount > 0
        ? formatDurationText(item.totalHandleDurationMs / item.handleDurationCount)
        : '-'
    }))
    .sort((left, right) => {
      if (right.overdueUnhandledCount !== left.overdueUnhandledCount) return right.overdueUnhandledCount - left.overdueUnhandledCount
      if (right.attentionCount !== left.attentionCount) return right.attentionCount - left.attentionCount
      if (right.feedbackCount !== left.feedbackCount) return right.feedbackCount - left.feedbackCount
      return `${left.doctorName}`.localeCompare(`${right.doctorName}`, 'zh-CN')
    })
})
const categoryOptions = computed(() => [...new Set(records.value.map(item => item.categoryName).filter(Boolean))])
const triageOptions = computed(() => [...new Set(records.value.map(item => item.triageLevelName).filter(Boolean))])
const fieldSampleDoctorOptions = computed(() => [...new Set((aiSummary.value.doctorBreakdown || []).map(item => item.doctorName).filter(Boolean))])
const fieldSampleCategoryOptions = computed(() => [...new Set(records.value.map(item => item.categoryName).filter(Boolean))])
const fieldSampleDepartmentOptions = computed(() => [...new Set(records.value.map(item => item.departmentName).filter(Boolean))])
/* const mismatchSampleReasonOptions = computed(() => aiMismatchReasonOptions.map(option => {
  const count = (aiSummary.value.mismatchReasonBreakdown || []).find(item => item.reasonCode === option.value)?.count
  return {
    ...option,
    label: count ? `${option.label}（${count}）` : option.label
  }
}))
const mismatchSampleDialogTitle = computed(() => mismatchSampleMeta.value.title || 'AI 偏差样本')
*/
const mismatchSampleReasonOptions = computed(() => aiMismatchReasonOptions.map(option => {
  const count = (aiSummary.value.mismatchReasonBreakdown || []).find(item => item.reasonCode === option.value)?.count
  return {
    ...option,
    label: count ? `${option.label} (${count})` : option.label
  }
}))
const mismatchSampleDialogTitle = computed(() => mismatchSampleMeta.value.title || 'AI 偏差样本')
const fieldSampleDialogTitle = computed(() => `${fieldSampleMeta.value.fieldLabel || '字段'}${fieldSampleStatus.value === 'mismatch' ? '偏差样本' : '待补充样本'}`)
const filteredRecords = computed(() => records.value.filter(item => {
  const search = keyword.value.trim().toLowerCase()
  const matchesKeyword = !search || [
    item.consultationNo,
    item.patientName,
    item.title,
    item.chiefComplaint,
    item.serviceFeedback?.doctorName,
    item.serviceFeedback?.doctorHandleDoctorName,
    item.serviceFeedback?.feedbackText,
    item.serviceFeedback?.doctorHandleRemark
  ].filter(Boolean).some(text => `${text}`.toLowerCase().includes(search))
  const matchesCategory = !categoryFilter.value || item.categoryName === categoryFilter.value
  const matchesTriage = !triageFilter.value || item.triageLevelName === triageFilter.value
  const matchesStatus = !statusFilter.value || item.status === statusFilter.value
  const matchesAssignment = !assignmentFilter.value || assignmentKey(item.doctorAssignment) === assignmentFilter.value
  const matchesDispatch = !dispatchFilter.value || getSmartDispatch(item).status === dispatchFilter.value
  const matchesFeedback = !feedbackFilter.value || matchesServiceFeedbackFilter(item, feedbackFilter.value)
  return matchesKeyword && matchesCategory && matchesTriage && matchesStatus && matchesAssignment && matchesDispatch && matchesFeedback
}))

function loadData() {
  loading.value = true
  get('/api/admin/consultation-record/list', data => {
    records.value = (data || []).map(item => ({
      ...item,
      smartDispatch: normalizeSmartDispatch(item?.smartDispatch)
    }))
    loading.value = false
  }, message => {
    loading.value = false
    ElMessage.warning(message || '问诊记录加载失败')
  })
}
function refreshAll() {
  loadData()
  loadAiSummary()
  loadSmartDispatchSummary()
}
function applyServiceFeedbackQuickFilter(filter = 'attention') {
  keyword.value = ''
  categoryFilter.value = ''
  triageFilter.value = ''
  assignmentFilter.value = ''
  dispatchFilter.value = ''
  statusFilter.value = 'completed'
  feedbackFilter.value = filter
}
function applyServiceFeedbackDoctorQuickFilter(doctorName = '', filter = 'attention') {
  applyServiceFeedbackQuickFilter(filter)
  keyword.value = doctorName || ''
}
function loadAiSummary() {
  summaryLoading.value = true
  get('/api/admin/consultation-record/ai-summary', data => {
    aiSummary.value = { ...createEmptyAiSummary(), ...(data || {}) }
    summaryLoading.value = false
  }, message => {
    summaryLoading.value = false
    ElMessage.warning(message || 'AI 采纳统计加载失败')
  })
}
function loadSmartDispatchSummary() {
  dispatchSummaryLoading.value = true
  get('/api/admin/consultation-record/smart-dispatch-summary', data => {
    dispatchSummary.value = { ...createEmptyDispatchSummary(), ...(data || {}) }
    dispatchSummaryLoading.value = false
  }, message => {
    dispatchSummaryLoading.value = false
    ElMessage.warning(message || '智能分配统计加载失败')
  })
}
function openMismatchSampleDialog({
  title = 'AI 偏差样本',
  doctorName = '',
  reasonCode = '',
  categoryName = '',
  departmentName = '',
  sourceCount = null
} = {}) {
  const defaultFilters = {
    ...createEmptyMismatchSampleFilters(),
    doctorName,
    reasonCode,
    categoryName,
    departmentName
  }
  mismatchSampleMeta.value = {
    title,
    sourceCount,
    defaultFilters
  }
  Object.assign(mismatchSampleFilters, createEmptyMismatchSampleFilters(), defaultFilters)
  mismatchSampleOffset.value = 0
  mismatchSampleHasMore.value = false
  mismatchSampleVisible.value = true
  loadMismatchSamples()
}
function buildMismatchSampleQuery({ offset = 0, includePaging = true } = {}) {
  const query = []
  if (includePaging) {
    query.push(`limit=${SAMPLE_PAGE_SIZE}`, `offset=${offset}`)
  }
  if (`${mismatchSampleFilters.keyword || ''}`.trim()) query.push(`keyword=${encodeURIComponent(`${mismatchSampleFilters.keyword}`.trim())}`)
  if (mismatchSampleFilters.doctorName) query.push(`doctorName=${encodeURIComponent(mismatchSampleFilters.doctorName)}`)
  if (mismatchSampleFilters.reasonCode) query.push(`reasonCode=${encodeURIComponent(mismatchSampleFilters.reasonCode)}`)
  if (mismatchSampleFilters.categoryName) query.push(`categoryName=${encodeURIComponent(mismatchSampleFilters.categoryName)}`)
  if (mismatchSampleFilters.departmentName) query.push(`departmentName=${encodeURIComponent(mismatchSampleFilters.departmentName)}`)
  return query
}
function loadMismatchSamples({ append = false } = {}) {
  mismatchSampleLoading.value = true
  const nextOffset = append ? mismatchSampleOffset.value : 0
  if (!append) mismatchSamples.value = []
  const query = buildMismatchSampleQuery({ offset: nextOffset })
  get(`/api/admin/consultation-record/mismatch-samples?${query.join('&')}`, data => {
    const list = data || []
    mismatchSamples.value = append ? [...mismatchSamples.value, ...list] : list
    mismatchSampleOffset.value = nextOffset + list.length
    mismatchSampleHasMore.value = list.length >= SAMPLE_PAGE_SIZE
    mismatchSampleLoading.value = false
  }, message => {
    mismatchSampleLoading.value = false
    ElMessage.warning(message || 'AI 偏差样本加载失败')
  })
}
function resetMismatchSampleFilters() {
  Object.assign(
    mismatchSampleFilters,
    createEmptyMismatchSampleFilters(),
    mismatchSampleMeta.value.defaultFilters || createEmptyMismatchSampleFilters()
  )
  mismatchSampleOffset.value = 0
  mismatchSampleHasMore.value = false
  loadMismatchSamples()
}
function loadMoreMismatchSamples() {
  if (!mismatchSampleHasMore.value || mismatchSampleLoading.value) return
  loadMismatchSamples({ append: true })
}
function exportMismatchSamples() {
  mismatchSampleExporting.value = true
  const query = buildMismatchSampleQuery({ includePaging: false })
  download(`/api/admin/consultation-record/mismatch-samples/export?${query.join('&')}`, 'consultation-mismatch-samples.csv', () => {
    mismatchSampleExporting.value = false
    ElMessage.success('CSV download started')
  }, message => {
    mismatchSampleExporting.value = false
    ElMessage.warning(message || 'Mismatch sample export failed')
  }, error => {
    mismatchSampleExporting.value = false
    console.error(error)
    ElMessage.error('Mismatch sample export failed')
  })
}
function openFieldSampleDialog(item, status = 'mismatch') {
  fieldSampleMeta.value = {
    fieldKey: item?.fieldKey || '',
    fieldLabel: item?.fieldLabel || '',
    mismatchCount: item?.mismatchCount ?? 0,
    pendingCount: item?.pendingCount ?? 0
  }
  Object.assign(fieldSampleFilters, createEmptyFieldSampleFilters())
  fieldSampleOffset.value = 0
  fieldSampleHasMore.value = false
  fieldSampleStatus.value = status === 'pending' ? 'pending' : 'mismatch'
  fieldSampleVisible.value = true
  loadFieldSamples()
}
function switchFieldSampleStatus(status) {
  const nextStatus = status === 'pending' ? 'pending' : 'mismatch'
  if (fieldSampleStatus.value === nextStatus) return
  fieldSampleStatus.value = nextStatus
  fieldSampleOffset.value = 0
  fieldSampleHasMore.value = false
  if (fieldSampleVisible.value) loadFieldSamples()
}
function buildFieldSampleQuery({ offset = 0, includePaging = true } = {}) {
  const query = [
    `fieldKey=${encodeURIComponent(fieldSampleMeta.value.fieldKey)}`,
    `status=${encodeURIComponent(fieldSampleStatus.value)}`
  ]
  if (includePaging) {
    query.push(`limit=${SAMPLE_PAGE_SIZE}`, `offset=${offset}`)
  }
  if (`${fieldSampleFilters.keyword || ''}`.trim()) query.push(`keyword=${encodeURIComponent(`${fieldSampleFilters.keyword}`.trim())}`)
  if (fieldSampleFilters.doctorName) query.push(`doctorName=${encodeURIComponent(fieldSampleFilters.doctorName)}`)
  if (fieldSampleFilters.categoryName) query.push(`categoryName=${encodeURIComponent(fieldSampleFilters.categoryName)}`)
  if (fieldSampleFilters.departmentName) query.push(`departmentName=${encodeURIComponent(fieldSampleFilters.departmentName)}`)
  return query
}
function loadFieldSamples({ append = false } = {}) {
  if (!fieldSampleMeta.value.fieldKey) {
    fieldSamples.value = []
    fieldSampleOffset.value = 0
    fieldSampleHasMore.value = false
    return
  }
  fieldSampleLoading.value = true
  const nextOffset = append ? fieldSampleOffset.value : 0
  if (!append) fieldSamples.value = []
  const query = buildFieldSampleQuery({ offset: nextOffset })
  get(`/api/admin/consultation-record/field-samples?${query.join('&')}`, data => {
    const list = data || []
    fieldSamples.value = append ? [...fieldSamples.value, ...list] : list
    fieldSampleOffset.value = nextOffset + list.length
    fieldSampleHasMore.value = list.length >= SAMPLE_PAGE_SIZE
    fieldSampleLoading.value = false
  }, message => {
    fieldSampleLoading.value = false
    ElMessage.warning(message || '字段样本加载失败')
  })
}
function resetFieldSampleFilters() {
  Object.assign(fieldSampleFilters, createEmptyFieldSampleFilters())
  fieldSampleOffset.value = 0
  fieldSampleHasMore.value = false
  loadFieldSamples()
}
function loadMoreFieldSamples() {
  if (!fieldSampleHasMore.value || fieldSampleLoading.value) return
  loadFieldSamples({ append: true })
}
function exportFieldSamples() {
  if (!fieldSampleMeta.value.fieldKey) return
  fieldSampleExporting.value = true
  const query = buildFieldSampleQuery({ includePaging: false })
  download(`/api/admin/consultation-record/field-samples/export?${query.join('&')}`, 'consultation-field-samples.csv', () => {
    fieldSampleExporting.value = false
    ElMessage.success('CSV download started')
  }, message => {
    fieldSampleExporting.value = false
    ElMessage.warning(message || 'Field sample export failed')
  }, error => {
    fieldSampleExporting.value = false
    console.error(error)
    ElMessage.error('Field sample export failed')
  })
}
function openDetailFromMismatchSample(id) {
  mismatchSampleVisible.value = false
  openDetail(id)
}
function openDetailFromFieldSample(id) {
  fieldSampleVisible.value = false
  openDetail(id)
}
function openDetail(id, options = {}) {
  const { syncRoute = true } = options
  const detailId = normalizePositiveId(id)
  if (!detailId) return
  currentDetailId.value = detailId
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  if (syncRoute) syncDetailRouteQuery(detailId)
  get(`/api/admin/consultation-record/detail?id=${detailId}`, data => {
    detail.value = data ? {
      ...data,
      smartDispatch: normalizeSmartDispatch(data?.smartDispatch)
    } : null
    detailLoading.value = false
  }, message => {
    detailLoading.value = false
    detailVisible.value = false
    detail.value = null
    currentDetailId.value = null
    syncDetailRouteQuery(null)
    ElMessage.warning(message || '问诊记录详情加载失败')
  })
}
function handleDetailDialogClosed() {
  detail.value = null
  currentDetailId.value = null
  syncDetailRouteQuery(null)
}
function syncDetailRouteQuery(id) {
  const nextId = normalizePositiveId(id)
  const currentId = normalizePositiveId(route.query.detailId)
  if (currentId === nextId) return
  const nextQuery = { ...route.query }
  if (nextId) nextQuery.detailId = `${nextId}`
  else delete nextQuery.detailId
  router.replace({
    path: route.path,
    query: nextQuery
  }).catch(() => {})
}
function normalizePositiveId(value) {
  const number = Number(value)
  return Number.isInteger(number) && number > 0 ? number : null
}
function assignmentKey(assignment) {
  if (!assignment) return 'unclaimed'
  return assignment.status === 'claimed' ? 'claimed' : 'released'
}
function assignmentStatusLabel(assignment) {
  if (!assignment) return '待认领'
  return assignment.status === 'claimed' ? '已认领' : '已释放'
}
function assignmentTagType(assignment) {
  if (!assignment) return 'info'
  return assignment.status === 'claimed' ? 'success' : 'warning'
}
function getSmartDispatch(record) {
  return normalizeSmartDispatch(record?.smartDispatch)
}
function smartDispatchLine(record) {
  const summary = getSmartDispatch(record)
  if (summary.suggestedDoctorName) return `首推 ${summary.suggestedDoctorName}`
  if (summary.claimedDoctorName) return `接手 ${summary.claimedDoctorName}`
  return smartDispatchHintText(summary)
}
function statusLabel(value) { return ({ submitted: '已提交', triaged: '已分诊', processing: '处理中', completed: '已完成' })[value] || value || '-' }
function handleStatusLabel(value) { return value === 'completed' ? '处理完成' : '处理中' }
function conditionLevelLabel(value) { return ({ low: '轻度', medium: '中度', high: '较高风险', critical: '危急' })[value] || '未填写' }
function dispositionLabel(value) { return ({ observe: '继续观察', online_followup: '线上随访', offline_visit: '线下就医', emergency: '立即急诊' })[value] || '未填写' }
function aiConsistencyLabel(value) { return value === 1 ? '与 AI 一致' : value === 0 ? '与 AI 不一致' : '未判断' }
function doctorFollowUpText(conclusion) { return !conclusion ? '' : conclusion.needFollowUp === 1 ? (conclusion.followUpWithinDays ? `${conclusion.followUpWithinDays} 天内随访` : '需要随访') : conclusion.needFollowUp === 0 ? '暂不需要随访' : '' }
function followUpTypeLabel(value) { return ({ platform: '平台随访', phone: '电话随访', offline: '线下随访', other: '其他方式' })[value] || '其他方式' }
function patientStatusLabel(value) { return ({ improved: '明显好转', stable: '基本稳定', worsened: '出现加重', other: '其他情况' })[value] || '其他情况' }
function statusTagType(value) { return ({ submitted: 'info', triaged: 'primary', processing: 'warning', completed: 'success' })[value] || 'info' }
function triageBadgeStyle(color) { return color ? { color, borderColor: `${color}33`, backgroundColor: `${color}14` } : {} }
function parseJsonArray(value) { try { const parsed = value ? JSON.parse(value) : []; return Array.isArray(parsed) ? parsed : [] } catch { return [] } }
function mismatchReasonLabels(codes) { return (codes || []).map(item => aiMismatchReasonLabel(item)).filter(Boolean) }
function doctorConclusionMismatchReasonLabels(conclusion) { return mismatchReasonLabels(parseJsonArray(conclusion?.aiMismatchReasonsJson)) }
function hasServiceFeedback(record) { return !!record?.serviceFeedback }
function serviceFeedbackTime(record) { return record?.serviceFeedback?.updateTime || record?.serviceFeedback?.createTime || record?.updateTime || record?.createTime || null }
function serviceFeedbackBaseTime(record) { return serviceFeedbackTime(record) }
function serviceFeedbackHandleDurationMs(record) {
  if (!isHandledServiceFeedbackRecord(record)) return null
  const baseTime = serviceFeedbackBaseTime(record)
  const handleTime = record?.serviceFeedback?.doctorHandleTime
  if (!baseTime || !handleTime) return null
  const diff = new Date(handleTime).getTime() - new Date(baseTime).getTime()
  return Number.isFinite(diff) && diff >= 0 ? diff : null
}
function isOverdueUnhandledServiceFeedbackRecord(record, hours = SERVICE_FEEDBACK_HANDLE_OVERDUE_HOURS) {
  if (!hasServiceFeedback(record) || isHandledServiceFeedbackRecord(record)) return false
  const baseTime = serviceFeedbackBaseTime(record)
  if (!baseTime) return false
  const thresholdMs = Number(hours) * 60 * 60 * 1000
  return Date.now() - new Date(baseTime).getTime() >= thresholdMs
}
function formatDurationText(value) {
  const ms = Number(value)
  if (!Number.isFinite(ms) || ms < 0) return '-'
  const totalMinutes = Math.round(ms / 60000)
  if (totalMinutes < 60) return `${Math.max(totalMinutes, 1)} 分钟`
  const totalHours = ms / (60 * 60 * 1000)
  if (totalHours < 24) return `${totalHours.toFixed(totalHours >= 10 ? 0 : 1)} 小时`
  const totalDays = totalHours / 24
  return `${totalDays.toFixed(totalDays >= 10 ? 0 : 1)} 天`
}
function formatAverageDurationText(values) {
  if (!Array.isArray(values) || !values.length) return '-'
  const total = values.reduce((sum, item) => sum + item, 0)
  return formatDurationText(total / values.length)
}
function serviceFeedbackPendingDurationText(record) {
  const baseTime = serviceFeedbackBaseTime(record)
  if (!baseTime) return '-'
  return formatDurationText(Date.now() - new Date(baseTime).getTime())
}
function serviceFeedbackScoreValue(record) {
  const score = Number(record?.serviceFeedback?.serviceScore)
  return Number.isFinite(score) ? score : null
}
function isLowScoreServiceFeedbackRecord(record) {
  const score = serviceFeedbackScoreValue(record)
  return score !== null && score > 0 && score <= 2
}
function isUnresolvedServiceFeedbackRecord(record) { return hasServiceFeedback(record) && Number(record?.serviceFeedback?.isResolved) !== 1 }
function isHandledServiceFeedbackRecord(record) { return hasServiceFeedback(record) && Number(record?.serviceFeedback?.doctorHandleStatus) === 1 }
function isAttentionServiceFeedbackRecord(record) {
  if (!hasServiceFeedback(record) || isHandledServiceFeedbackRecord(record)) return false
  return isLowScoreServiceFeedbackRecord(record) || isUnresolvedServiceFeedbackRecord(record)
}
function matchesServiceFeedbackFilter(record, filter) {
  if (!filter) return true
  if (filter === 'has_feedback') return hasServiceFeedback(record)
  if (filter === 'attention') return isAttentionServiceFeedbackRecord(record)
  if (filter === 'low_score') return isLowScoreServiceFeedbackRecord(record)
  if (filter === 'unresolved') return isUnresolvedServiceFeedbackRecord(record)
  if (filter === 'unhandled') return hasServiceFeedback(record) && !isHandledServiceFeedbackRecord(record)
  if (filter === 'overdue_unhandled') return isOverdueUnhandledServiceFeedbackRecord(record, SERVICE_FEEDBACK_HANDLE_OVERDUE_HOURS)
  if (filter === 'handled') return isHandledServiceFeedbackRecord(record)
  return true
}
function compareServiceFeedbackRecord(left, right) {
  const leftAttention = isAttentionServiceFeedbackRecord(left) ? 1 : 0
  const rightAttention = isAttentionServiceFeedbackRecord(right) ? 1 : 0
  if (rightAttention !== leftAttention) return rightAttention - leftAttention
  const leftLowScore = isLowScoreServiceFeedbackRecord(left) ? 1 : 0
  const rightLowScore = isLowScoreServiceFeedbackRecord(right) ? 1 : 0
  if (rightLowScore !== leftLowScore) return rightLowScore - leftLowScore
  const leftUnresolved = isUnresolvedServiceFeedbackRecord(left) ? 1 : 0
  const rightUnresolved = isUnresolvedServiceFeedbackRecord(right) ? 1 : 0
  if (rightUnresolved !== leftUnresolved) return rightUnresolved - leftUnresolved
  const leftTime = serviceFeedbackTime(left) ? new Date(serviceFeedbackTime(left)).getTime() : 0
  const rightTime = serviceFeedbackTime(right) ? new Date(serviceFeedbackTime(right)).getTime() : 0
  if (rightTime !== leftTime) return rightTime - leftTime
  return Number(right?.id || 0) - Number(left?.id || 0)
}
function formatRateText(numerator, denominator) {
  const total = Number(denominator)
  const current = Number(numerator)
  if (!Number.isFinite(total) || total <= 0 || !Number.isFinite(current) || current < 0) return '-'
  return `${Math.round((current / total) * 100)}%`
}
function serviceFeedbackResolvedLabel(value) { return value === 1 ? '本次问题已解决' : '仍需继续处理' }
function serviceFeedbackResolvedTagType(value) { return value === 1 ? 'success' : 'warning' }
function serviceFeedbackHandleLabel(value) { return value === 1 ? '医生已处理' : '待医生跟进' }
function serviceFeedbackHandleTagType(value) { return value === 1 ? 'success' : 'info' }
function serviceFeedbackScoreTagType(value) {
  const score = Number(value)
  if (!Number.isFinite(score)) return 'info'
  if (score <= 2) return 'danger'
  if (score === 3) return 'warning'
  return 'success'
}
function formatConfidence(value) {
  const number = Number(value)
  return Number.isNaN(number) || number <= 0 ? '-' : `${Math.round(number * 100)}%`
}
function displayAnswer(answer) { return answer.fieldType === 'switch' ? (answer.fieldValue === '1' ? '是' : '否') : (answer.fieldValue || '-') }
function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}
watch(() => route.query.detailId, value => {
  const detailId = normalizePositiveId(value)
  if (!detailId) {
    if (detailVisible.value) detailVisible.value = false
    return
  }
  if (currentDetailId.value === detailId && detailVisible.value) return
  openDetail(detailId, { syncRoute: false })
}, { immediate: true })
onMounted(() => refreshAll())
</script>

<style scoped>
.record-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stats,
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.breakdown-grid,
.field-grid,
.answer-grid,
.mismatch-list,
.compare-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.stat,
.block,
.panel {
  padding: 22px;
}

.stat span,
.head p,
.assignment span,
.meta span,
.summary-card span,
.summary-card small,
.breakdown-head p {
  color: var(--app-muted);
}

.stat strong,
.summary-card strong {
  display: block;
  margin-top: 14px;
  font-size: 30px;
}

.head,
.toolbar,
.assignment,
.chips,
.actions {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.head {
  justify-content: space-between;
  margin-bottom: 16px;
}

.head h3,
.panel h3 {
  margin: 0;
}

.head p,
.breakdown-head p {
  margin: 6px 0 0;
  line-height: 1.7;
}

.summary-subhead {
  margin-top: 18px;
}

.dispatch-summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 18px;
}

.feedback-summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 18px;
}

.breakdown-grid {
  margin-top: 18px;
}

.field-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.breakdown-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.breakdown-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.breakdown-head strong {
  display: block;
}

.meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.meta article,
.subcard {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(19, 73, 80, 0.05);
}

.meta strong {
  display: block;
  margin-top: 8px;
}

.copy {
  margin: 0;
  line-height: 1.8;
  color: #41575d;
}

.copy + .copy {
  margin-top: 8px;
}

.feedback-cell,
.feedback-focus-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feedback-tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.feedback-cell-copy {
  color: #41575d;
  line-height: 1.7;
}

.chips span {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(19, 73, 80, 0.08);
  color: #27646d;
}

.badge {
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

.image {
  width: 220px;
  height: 150px;
  border-radius: 18px;
  object-fit: cover;
  border: 1px solid rgba(17, 70, 77, 0.08);
}

.compare-panel {
  background: linear-gradient(180deg, rgba(15, 102, 101, 0.08), rgba(255, 255, 255, 0.78));
}

.compare-card strong,
.compare-item strong {
  display: block;
  margin-bottom: 8px;
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

.chips.danger span {
  background: rgba(214, 95, 80, 0.12);
  color: #9f4336;
}

.field-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field-card-actions,
.field-sample-toolbar,
.field-sample-filter-bar,
.field-sample-status-group,
.field-sample-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.field-card-head,
.field-metrics {
  display: flex;
  gap: 12px;
}

.field-card-head {
  justify-content: space-between;
  align-items: flex-start;
}

.field-card-head strong,
.field-metrics strong,
.field-reason-title {
  display: block;
}

.field-card-head p,
.field-metrics span,
.field-reason-title {
  color: var(--app-muted);
}

.field-card-head p,
.field-reason-title {
  margin: 6px 0 0;
  line-height: 1.6;
}

.field-rate {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(214, 95, 80, 0.12);
  color: #9f4336;
  font-size: 12px;
  white-space: nowrap;
}

.field-metrics {
  justify-content: space-between;
}

.field-metrics article {
  flex: 1;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(19, 73, 80, 0.04);
}

.field-metrics strong {
  margin-top: 10px;
  font-size: 24px;
}

.field-card-actions {
  justify-content: flex-end;
}

.field-sample-toolbar {
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.field-sample-filter-bar {
  align-items: center;
  margin-bottom: 16px;
}

.field-sample-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field-sample-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field-sample-actions {
  justify-content: flex-end;
}

.sample-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.reason-chip-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.reason-chip {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(214, 95, 80, 0.12);
  color: #9f4336;
  font-size: 12px;
  line-height: 1.4;
}

.reason-chip.is-clickable {
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.reason-chip.is-clickable:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(159, 67, 54, 0.12);
}

.muted-copy {
  color: var(--app-muted);
}

.reason-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reason-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(19, 73, 80, 0.04);
}

.reason-item-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.reason-item strong {
  display: block;
}

.reason-item span {
  color: var(--app-muted);
  white-space: nowrap;
}

.dispatch-wait-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dispatch-wait-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@media (max-width: 1180px) {
  .stats,
  .summary-grid,
  .breakdown-grid,
  .field-grid,
  .meta,
  .answer-grid,
  .mismatch-list,
  .compare-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .head,
  .toolbar,
  .compare-item,
  .actions,
  .breakdown-head,
  .field-card-head,
  .field-metrics,
  .field-sample-toolbar,
  .field-sample-filter-bar,
  .field-sample-status-group,
  .field-card-actions,
  .field-sample-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
