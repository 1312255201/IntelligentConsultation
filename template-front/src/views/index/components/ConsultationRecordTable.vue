<template>
  <section ref="historySectionRef" class="history-card">
    <div class="panel-head">
      <div>
        <span class="panel-kicker">历史记录</span>
        <h3>问诊记录</h3>
      </div>
      <el-button @click="emit('refresh-records')">刷新记录</el-button>
    </div>

    <div class="history-toolbar">
      <el-input v-model="keywordModel" clearable placeholder="搜索标题、分类、就诊人或消息" style="width: 260px" />
      <el-select v-model="statusModel" clearable placeholder="全部状态" style="width: 150px">
        <el-option label="已提交" value="submitted" />
        <el-option label="已分诊" value="triaged" />
        <el-option label="处理中" value="processing" />
        <el-option label="已完成" value="completed" />
      </el-select>
      <el-select v-model="progressModel" style="width: 170px">
        <el-option label="待评价" value="pending_feedback" />
        <el-option label="全部进度" value="all" />
        <el-option label="医生新回复" value="doctor_replied" />
        <el-option label="待医生处理" value="waiting_doctor" />
        <el-option label="医生处理中" value="doctor_processing" />
        <el-option label="已完成" value="completed" />
      </el-select>
      <el-select v-model="followUpModel" style="width: 170px">
        <el-option label="全部随访" value="all" />
        <el-option label="待随访" value="pending" />
        <el-option label="今日到期" value="due_today" />
        <el-option label="已逾期" value="overdue" />
      </el-select>
    </div>

    <el-table :data="records" :row-key="row => row.id" v-loading="historyLoading" border :row-class-name="recordRowClassName">
      <el-table-column label="初步分诊" min-width="140" align="center">
        <template #default="{ row }">
          <span class="triage-badge" :style="helpers.triageBadgeStyle(row.triageLevelColor)">
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
          <el-tag type="warning" effect="light">{{ helpers.statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="收费" min-width="170">
        <template #default="{ row }">
          <div class="record-message-cell">
            <el-tag :type="helpers.paymentStatusTagType(row.payment)" effect="light">{{ helpers.paymentStatusLabel(row.payment) }}</el-tag>
            <span>{{ helpers.formatAmount(row.payment?.amount) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="智能分配" min-width="220">
        <template #default="{ row }">
          <div class="record-message-cell">
            <strong>{{ helpers.smartDispatchStatusLabel(row.smartDispatch) }}</strong>
            <span>{{ helpers.smartDispatchLine(row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="沟通状态" min-width="220">
        <template #default="{ row }">
          <div class="record-message-cell">
            <strong>{{ helpers.recordMessageStatus(row) }}</strong>
            <span>{{ helpers.recordMessagePreview(row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="问诊进度" min-width="180">
        <template #default="{ row }">
          <div class="record-message-cell">
            <el-tag :type="helpers.recordProgressTagType(row)" effect="light">{{ helpers.recordProgressLabel(row) }}</el-tag>
            <span>{{ helpers.recordProgressHint(row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="随访提醒" min-width="180">
        <template #default="{ row }">
          <div class="record-message-cell">
            <el-tag :type="helpers.followUpTagType(row)" effect="light">{{ helpers.followUpTagLabel(row) }}</el-tag>
            <span>{{ helpers.followUpLine(row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="healthSummary" label="健康摘要" min-width="220" show-overflow-tooltip />
      <el-table-column label="提交时间" min-width="170">
        <template #default="{ row }">
          {{ helpers.formatDate(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right" class-name="record-action-column">
        <template #default="{ row }">
          <div class="record-action-cell">
            <el-button link type="primary" @click="emit('open-record-detail', row)">查看详情</el-button>
            <el-button link type="success" @click="emit('open-triage-workspace', row.id)">智能导诊</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  records: {
    type: Array,
    default: () => []
  },
  historyLoading: {
    type: Boolean,
    default: false
  },
  helpers: {
    type: Object,
    required: true
  },
  recordKeyword: {
    type: String,
    default: ''
  },
  recordStatusFilter: {
    type: String,
    default: ''
  },
  recordProgressFilter: {
    type: String,
    default: 'all'
  },
  recordFollowUpFilter: {
    type: String,
    default: 'all'
  }
})

const emit = defineEmits([
  'update:recordKeyword',
  'update:recordStatusFilter',
  'update:recordProgressFilter',
  'update:recordFollowUpFilter',
  'refresh-records',
  'open-record-detail',
  'open-triage-workspace'
])

const historySectionRef = ref(null)

const keywordModel = computed({
  get: () => props.recordKeyword,
  set: (value) => emit('update:recordKeyword', value)
})

const statusModel = computed({
  get: () => props.recordStatusFilter,
  set: (value) => emit('update:recordStatusFilter', value)
})

const progressModel = computed({
  get: () => props.recordProgressFilter,
  set: (value) => emit('update:recordProgressFilter', value)
})

const followUpModel = computed({
  get: () => props.recordFollowUpFilter,
  set: (value) => emit('update:recordFollowUpFilter', value)
})

function recordRowClassName({ row }) {
  if (props.helpers.recordHasUnreadDoctorReply(row)) return 'record-row-unread'
  const state = props.helpers.followUpState(row)
  if (state === 'overdue') return 'record-row-overdue'
  if (state === 'due_today') return 'record-row-due-today'
  return ''
}

function scrollIntoView(options = { behavior: 'smooth', block: 'start' }) {
  historySectionRef.value?.scrollIntoView?.(options)
}

defineExpose({ scrollIntoView })
</script>

<style scoped>
.history-card {
  padding: 22px;
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.panel-head,
.history-toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.panel-head {
  justify-content: space-between;
  align-items: flex-start;
}

.panel-head h3 {
  margin: 6px 0 0;
}

.panel-kicker {
  color: var(--app-muted);
}

.history-toolbar {
  margin: 16px 0;
  align-items: center;
}

.record-message-cell,
.conversation-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
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

.record-message-cell strong {
  color: #31474d;
}

.record-message-cell span {
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.record-action-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  min-height: 40px;
  padding: 4px 0;
}

:deep(.el-table__fixed-right .el-table__fixed-body-wrapper td.el-table__cell),
:deep(.el-table__fixed-right .el-table__header-wrapper th.el-table__cell),
:deep(.el-table__fixed .el-table__fixed-body-wrapper td.el-table__cell),
:deep(.el-table__fixed .el-table__header-wrapper th.el-table__cell) {
  background: #fdfefe;
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

:deep(.el-table__fixed-right .record-row-unread td.el-table__cell),
:deep(.el-table__fixed .record-row-unread td.el-table__cell) {
  background: rgba(77, 168, 132, 0.08);
}

:deep(.el-table__fixed-right .record-row-overdue td.el-table__cell),
:deep(.el-table__fixed .record-row-overdue td.el-table__cell) {
  background: rgba(214, 95, 80, 0.09);
}

:deep(.el-table__fixed-right .record-row-due-today td.el-table__cell),
:deep(.el-table__fixed .record-row-due-today td.el-table__cell) {
  background: rgba(210, 155, 47, 0.09);
}

@media (max-width: 760px) {
  .panel-head,
  .history-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
