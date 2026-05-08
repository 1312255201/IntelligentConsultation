<template>
  <section class="reminder-grid">
    <article class="reminder-card">
      <div class="reminder-head">
        <div>
          <span class="panel-kicker">评价提醒</span>
          <h3>待评价</h3>
          <p>医生完成处理后，可在这里快速补充服务评分、问题是否解决和本次问诊体验。</p>
        </div>
        <el-tag type="info" effect="light">{{ panelState.pendingServiceFeedbackCount }}</el-tag>
      </div>
      <div v-if="panelState.pendingServiceFeedbackRecords.length" class="reminder-list">
        <button
          v-for="item in panelState.pendingServiceFeedbackRecords"
          :key="`feedback-${item.id}`"
          type="button"
          class="reminder-item"
          @click="openRecord(item, 'feedback')"
        >
          <div class="reminder-item-head">
            <strong>{{ item.patientName || '未命名就诊人' }}</strong>
            <span>{{ helpers.serviceFeedbackTimeText(item) }}</span>
          </div>
          <p>{{ helpers.serviceFeedbackReminderText(item) }}</p>
          <div class="reminder-item-meta">
            <span>{{ item.categoryName || '未分类' }}</span>
            <span>待提交服务评价</span>
          </div>
        </button>
      </div>
      <el-empty v-else description="当前没有待评价的问诊" />
      <div class="reminder-foot">
        <el-button text @click="applyFilter({ progress: 'pending_feedback' })">只看待评价</el-button>
      </div>
    </article>

    <article class="reminder-card">
      <div class="reminder-head">
        <div>
          <span class="panel-kicker">消息提醒</span>
          <h3>医生新回复</h3>
          <p>优先查看医生刚回复的问诊，避免错过进一步处理建议。</p>
        </div>
        <el-tag type="success" effect="light">{{ panelState.unreadDoctorReplyCount }}</el-tag>
      </div>
      <div v-if="panelState.unreadReplyRecords.length" class="reminder-list">
        <button
          v-for="item in panelState.unreadReplyRecords"
          :key="`reply-${item.id}`"
          type="button"
          class="reminder-item"
          @click="openRecord(item, 'conversation')"
        >
          <div class="reminder-item-head">
            <strong>{{ item.patientName || '未命名就诊人' }}</strong>
            <span>{{ helpers.formatDate(helpers.getMessageSummary(item).latestTime) }}</span>
          </div>
          <p>{{ helpers.recordMessagePreview(item) }}</p>
          <div class="reminder-item-meta">
            <span>{{ item.categoryName || '未分类' }}</span>
            <span>未读 {{ helpers.getMessageSummary(item).unreadCount }} 条</span>
          </div>
        </button>
      </div>
      <el-empty v-else description="当前没有新的医生回复" />
      <div class="reminder-foot">
        <el-button text @click="applyFilter({ progress: 'doctor_replied' })">只看医生新回复</el-button>
      </div>
    </article>

    <article class="reminder-card">
      <div class="reminder-head">
        <div>
          <span class="panel-kicker">待处理</span>
          <h3>待医生处理</h3>
          <p>这些问诊还在等待医生接手或继续处理，可随时补充更多症状与资料。</p>
        </div>
        <el-tag type="warning" effect="light">{{ panelState.waitingDoctorHandleCount }}</el-tag>
      </div>
      <div v-if="panelState.waitingDoctorRecords.length" class="reminder-list">
        <button
          v-for="item in panelState.waitingDoctorRecords"
          :key="`waiting-${item.id}`"
          type="button"
          class="reminder-item"
          @click="openRecord(item, 'conversation')"
        >
          <div class="reminder-item-head">
            <strong>{{ item.patientName || '未命名就诊人' }}</strong>
            <span>{{ helpers.formatDate(item.createTime) }}</span>
          </div>
          <p>{{ helpers.recordProgressHint(item) }}</p>
          <div class="reminder-item-meta">
            <span>{{ item.categoryName || '未分类' }}</span>
            <span>{{ helpers.smartDispatchStatusLabel(item.smartDispatch) }}</span>
          </div>
        </button>
      </div>
      <el-empty v-else description="当前没有待医生处理的问诊" />
      <div class="reminder-foot">
        <el-button text @click="applyFilter({ progress: 'waiting_doctor' })">只看待处理问诊</el-button>
      </div>
    </article>

    <article class="reminder-card reminder-card-followup">
      <div class="reminder-head">
        <div>
          <span class="panel-kicker">随访提醒</span>
          <h3>待随访提醒</h3>
          <p>及时查看需要继续跟进的问诊，尤其是今日到期和已逾期的随访。</p>
        </div>
        <div class="reminder-tags">
          <el-tag type="primary" effect="light">{{ panelState.pendingFollowUpCount }}</el-tag>
          <el-tag v-if="panelState.dueTodayFollowUpCount" type="warning" effect="light">今日 {{ panelState.dueTodayFollowUpCount }}</el-tag>
          <el-tag v-if="panelState.overdueFollowUpCount" type="danger" effect="light">逾期 {{ panelState.overdueFollowUpCount }}</el-tag>
        </div>
      </div>
      <div v-if="panelState.followUpReminderRecords.length" class="reminder-list">
        <button
          v-for="item in panelState.followUpReminderRecords"
          :key="`followup-${item.id}`"
          type="button"
          :class="['reminder-item', helpers.followUpReminderItemClass(item)]"
          @click="openRecord(item, 'followup')"
        >
          <div class="reminder-item-head">
            <strong>{{ item.patientName || '未命名就诊人' }}</strong>
            <span>{{ helpers.followUpLine(item) }}</span>
          </div>
          <p>{{ item?.doctorConclusion?.patientInstruction || item?.doctorHandle?.followUpPlan || '医生已建议继续关注恢复情况。' }}</p>
          <div class="reminder-item-meta">
            <span>{{ item.categoryName || '未分类' }}</span>
            <span>{{ helpers.followUpTagLabel(item) }}</span>
          </div>
        </button>
      </div>
      <el-empty v-else description="当前没有待随访问诊" />
      <div class="reminder-foot">
        <el-button text @click="applyFilter({ followUp: 'pending' })">只看待随访</el-button>
        <el-button v-if="panelState.overdueFollowUpCount" text type="danger" @click="applyFilter({ followUp: 'overdue' })">优先看逾期</el-button>
      </div>
    </article>
  </section>
</template>

<script setup>
const props = defineProps({
  panelState: {
    type: Object,
    required: true
  },
  helpers: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['open-record-detail', 'apply-record-quick-filter'])

function openRecord(record, action) {
  emit('open-record-detail', record, { action })
}

function applyFilter(payload) {
  emit('apply-record-quick-filter', payload)
}
</script>

<style scoped>
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

.panel-kicker {
  color: var(--app-muted);
}

@media (max-width: 1180px) {
  .reminder-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .reminder-head,
  .reminder-item-head,
  .reminder-item-meta,
  .reminder-foot,
  .reminder-tags {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
