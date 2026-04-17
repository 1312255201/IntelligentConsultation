<template>
  <div class="admin-order-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>订单总数</span>
        <strong>{{ orders.length }}</strong>
      </article>
      <article class="stat-card">
        <span>待支付</span>
        <strong>{{ pendingCount }}</strong>
      </article>
      <article class="stat-card">
        <span>已支付</span>
        <strong>{{ paidCount }}</strong>
      </article>
      <article class="stat-card">
        <span>累计金额</span>
        <strong>{{ totalAmountText }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索订单号、问诊号、患者或用户"
            style="max-width: 320px"
          />
          <el-select v-model="statusFilter" clearable placeholder="全部状态" style="width: 160px">
            <el-option label="待支付" value="pending" />
            <el-option label="已支付" value="paid" />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadOrders">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredOrders" v-loading="loading" border>
        <el-table-column prop="paymentNo" label="订单号" min-width="200" />
        <el-table-column prop="consultationNo" label="问诊号" min-width="180" />
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="patientName" label="患者" min-width="120" />
        <el-table-column prop="doctorName" label="接诊医生" min-width="120" />
        <el-table-column prop="categoryName" label="问诊分类" min-width="120" />
        <el-table-column label="金额" width="100" align="center">
          <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'paid' ? 'success' : 'warning'" effect="light">
              {{ row.status === 'paid' ? '已支付' : '待支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付方式" width="100" align="center">
          <template #default="{ row }">{{ row.paymentChannel || '—' }}</template>
        </el-table-column>
        <el-table-column label="支付时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.paidTime) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openConsultation(row)">查看问诊</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { get } from '@/net'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const orders = ref([])

const filteredOrders = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return orders.value.filter(item => {
    if (statusFilter.value && item.status !== statusFilter.value) return false
    if (!search) return true
    return [item.paymentNo, item.consultationNo, item.patientName, item.username]
      .filter(Boolean)
      .some(text => `${text}`.toLowerCase().includes(search))
  })
})

const pendingCount = computed(() => orders.value.filter(item => item.status === 'pending').length)
const paidCount = computed(() => orders.value.filter(item => item.status === 'paid').length)
const totalAmountText = computed(() => `¥${orders.value.reduce((total, item) => total + Number(item.amount || 0), 0).toFixed(2)}`)

function loadOrders() {
  loading.value = true
  get('/api/admin/system/order/list', (data) => {
    orders.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function openConsultation(row) {
  router.push({
    path: '/admin/consultation-record',
    query: {
      detailId: row.consultationId
    }
  })
}

function formatAmount(value) {
  return Number(value || 0).toFixed(2)
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadOrders)
</script>

<style scoped>
.admin-order-page {
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
</style>
