<template>
  <div class="admin-operation-log-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>日志条数</span>
        <strong>{{ logs.length }}</strong>
      </article>
      <article class="stat-card">
        <span>成功请求</span>
        <strong>{{ successCount }}</strong>
      </article>
      <article class="stat-card">
        <span>异常请求</span>
        <strong>{{ errorCount }}</strong>
      </article>
      <article class="stat-card">
        <span>平均耗时</span>
        <strong>{{ avgDurationText }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索请求地址、账号或 IP"
            style="max-width: 320px"
          />
          <el-select v-model="methodFilter" clearable placeholder="全部方法" style="width: 140px">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="OPTIONS" value="OPTIONS" />
          </el-select>
          <el-select v-model="statusFilter" clearable placeholder="全部结果" style="width: 140px">
            <el-option label="成功" :value="200" />
            <el-option label="异常" :value="-1" />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadLogs">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredLogs" v-loading="loading" border>
        <el-table-column prop="requestUrl" label="请求地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="方法" width="90" align="center" />
        <el-table-column label="响应码" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="Number(row.responseCode || 0) === 200 ? 'success' : 'danger'" effect="light">
              {{ row.responseCode || '—' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="role" label="角色" min-width="120" show-overflow-tooltip />
        <el-table-column prop="remoteIp" label="IP" min-width="130" />
        <el-table-column label="耗时" width="100" align="center">
          <template #default="{ row }">{{ row.durationMs || 0 }}ms</template>
        </el-table-column>
        <el-table-column label="记录时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="detailVisible" title="日志详情" width="760px" destroy-on-close>
      <div v-if="activeLog" class="detail-grid">
        <div class="detail-item">
          <span>请求地址</span>
          <strong>{{ activeLog.requestUrl }}</strong>
        </div>
        <div class="detail-item">
          <span>请求方法</span>
          <strong>{{ activeLog.requestMethod }}</strong>
        </div>
        <div class="detail-item">
          <span>账号</span>
          <strong>{{ activeLog.username || '未认证' }}</strong>
        </div>
        <div class="detail-item">
          <span>响应码</span>
          <strong>{{ activeLog.responseCode || '—' }}</strong>
        </div>
        <div class="detail-item">
          <span>耗时</span>
          <strong>{{ activeLog.durationMs || 0 }}ms</strong>
        </div>
        <div class="detail-item">
          <span>记录时间</span>
          <strong>{{ formatDate(activeLog.createTime) }}</strong>
        </div>
      </div>

      <el-form label-position="top">
        <el-form-item label="请求参数">
          <el-input :model-value="activeLog?.requestParams || '—'" type="textarea" :rows="6" readonly />
        </el-form-item>
        <el-form-item label="响应摘要">
          <el-input :model-value="activeLog?.responseSummary || '—'" type="textarea" :rows="8" readonly />
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { get } from '@/net'

const loading = ref(false)
const keyword = ref('')
const methodFilter = ref('')
const statusFilter = ref(null)
const logs = ref([])
const activeLog = ref(null)
const detailVisible = ref(false)

const filteredLogs = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return logs.value.filter(item => {
    if (methodFilter.value && item.requestMethod !== methodFilter.value) return false
    if (statusFilter.value === 200 && Number(item.responseCode || 0) !== 200) return false
    if (statusFilter.value === -1 && Number(item.responseCode || 0) === 200) return false
    if (!search) return true
    return [item.requestUrl, item.username, item.remoteIp]
      .filter(Boolean)
      .some(text => `${text}`.toLowerCase().includes(search))
  })
})

const successCount = computed(() => logs.value.filter(item => Number(item.responseCode || 0) === 200).length)
const errorCount = computed(() => logs.value.filter(item => Number(item.responseCode || 0) !== 200).length)
const avgDurationText = computed(() => {
  if (!logs.value.length) return '0ms'
  const total = logs.value.reduce((sum, item) => sum + Number(item.durationMs || 0), 0)
  return `${Math.round(total / logs.value.length)}ms`
})

function loadLogs() {
  loading.value = true
  get('/api/admin/system/operation-log/list', (data) => {
    logs.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function openDetail(row) {
  activeLog.value = row
  detailVisible.value = true
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadLogs)
</script>

<style scoped>
.admin-operation-log-page {
  display: grid;
  gap: 20px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.stat-card,
.table-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 40px rgba(17, 70, 77, 0.08);
}

.stat-card {
  padding: 20px 22px;
}

.stat-card span {
  display: block;
  color: #6c7f86;
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 30px;
  color: #17373d;
}

.table-card {
  padding: 22px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.toolbar-filters,
.toolbar-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.detail-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: #f6fbfb;
}

.detail-item span {
  display: block;
  color: #6c7f86;
  font-size: 12px;
}

.detail-item strong {
  display: block;
  margin-top: 6px;
  color: #17373d;
  word-break: break-all;
}
</style>
