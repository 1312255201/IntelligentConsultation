<template>
  <div class="admin-operation-log-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>命中记录</span>
        <strong>{{ total }}</strong>
      </article>
      <article class="stat-card">
        <span>本页成功</span>
        <strong>{{ successCount }}</strong>
      </article>
      <article class="stat-card">
        <span>本页异常</span>
        <strong>{{ errorCount }}</strong>
      </article>
      <article class="stat-card">
        <span>本页平均耗时</span>
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
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="methodFilter" clearable placeholder="全部方法" style="width: 140px">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
            <el-option label="PATCH" value="PATCH" />
            <el-option label="OPTIONS" value="OPTIONS" />
          </el-select>
          <el-select v-model="statusFilter" clearable placeholder="全部结果" style="width: 140px">
            <el-option label="成功" :value="200" />
            <el-option label="异常" :value="-1" />
          </el-select>
          <div class="filter-switch">
            <span>显示低价值请求</span>
            <el-switch v-model="includeLowValue" @change="handleSearch" />
          </div>
        </div>
        <div class="toolbar-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <el-table :data="logs" v-loading="loading" border>
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

      <div class="table-footer">
        <span class="table-summary">当前第 {{ pageNo }} 页，共 {{ total }} 条记录</span>
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="pageNo"
          :page-size="pageSize"
          :page-sizes="[20, 50, 100]"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
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
const includeLowValue = ref(false)
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const successCount = computed(() => logs.value.filter(item => Number(item.responseCode || 0) === 200).length)
const errorCount = computed(() => logs.value.filter(item => Number(item.responseCode || 0) !== 200).length)
const avgDurationText = computed(() => {
  if (!logs.value.length) return '0ms'
  const total = logs.value.reduce((sum, item) => sum + Number(item.durationMs || 0), 0)
  return `${Math.round(total / logs.value.length)}ms`
})

function buildQuery() {
  const params = new URLSearchParams()
  params.set('pageNo', `${pageNo.value}`)
  params.set('pageSize', `${pageSize.value}`)
  params.set('includeLowValue', `${includeLowValue.value}`)
  if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
  if (methodFilter.value) params.set('method', methodFilter.value)
  if (statusFilter.value !== null && statusFilter.value !== undefined && statusFilter.value !== '') {
    params.set('status', `${statusFilter.value}`)
  }
  return params.toString()
}

function loadLogs() {
  loading.value = true
  get(`/api/admin/system/operation-log/list?${buildQuery()}`, (data) => {
    logs.value = data?.records || []
    total.value = Number(data?.total || 0)
    pageNo.value = Number(data?.pageNo || pageNo.value)
    pageSize.value = Number(data?.pageSize || pageSize.value)
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function handleSearch() {
  pageNo.value = 1
  loadLogs()
}

function resetFilters() {
  keyword.value = ''
  methodFilter.value = ''
  statusFilter.value = null
  includeLowValue.value = false
  pageNo.value = 1
  pageSize.value = 20
  loadLogs()
}

function handleSizeChange(value) {
  pageSize.value = value
  pageNo.value = 1
  loadLogs()
}

function handleCurrentChange(value) {
  pageNo.value = value
  loadLogs()
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

.filter-switch,
.table-footer {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-switch {
  min-height: 40px;
  color: #4d6368;
  font-size: 13px;
  padding: 0 12px;
  border-radius: 14px;
  background: #f4faf9;
}

.table-footer {
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
  flex-wrap: wrap;
}

.table-summary {
  color: #6c7f86;
  font-size: 13px;
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

@media (max-width: 768px) {
  .table-footer {
    align-items: flex-start;
  }
}
</style>
