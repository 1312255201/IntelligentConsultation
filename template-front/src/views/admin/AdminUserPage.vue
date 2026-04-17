<template>
  <div class="admin-user-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>用户总数</span>
        <strong>{{ users.length }}</strong>
      </article>
      <article class="stat-card">
        <span>已建档用户</span>
        <strong>{{ archivedUserCount }}</strong>
      </article>
      <article class="stat-card">
        <span>有问诊记录</span>
        <strong>{{ consultedUserCount }}</strong>
      </article>
      <article class="stat-card">
        <span>有处方记录</span>
        <strong>{{ prescriptionUserCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索用户名或邮箱"
            style="max-width: 320px"
          />
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadUsers">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredUsers" v-loading="loading" border>
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="220" show-overflow-tooltip />
        <el-table-column label="就诊人档案" width="110" align="center">
          <template #default="{ row }">{{ row.patientCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="病史档案" width="110" align="center">
          <template #default="{ row }">{{ row.medicalHistoryCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="问诊总数" width="110" align="center">
          <template #default="{ row }">{{ row.consultationCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="完成问诊" width="110" align="center">
          <template #default="{ row }">{{ row.completedConsultationCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="已支付订单" width="120" align="center">
          <template #default="{ row }">{{ row.paidOrderCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="处方条目" width="110" align="center">
          <template #default="{ row }">{{ row.prescriptionCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="最近问诊时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.latestConsultationTime) }}</template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.registerTime) }}</template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { get } from '@/net'

const loading = ref(false)
const keyword = ref('')
const users = ref([])

const filteredUsers = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  if (!search) return users.value
  return users.value.filter(item =>
    [item.username, item.email]
      .filter(Boolean)
      .some(text => `${text}`.toLowerCase().includes(search))
  )
})

const archivedUserCount = computed(() => users.value.filter(item => Number(item.patientCount || 0) > 0).length)
const consultedUserCount = computed(() => users.value.filter(item => Number(item.consultationCount || 0) > 0).length)
const prescriptionUserCount = computed(() => users.value.filter(item => Number(item.prescriptionCount || 0) > 0).length)

function loadUsers() {
  loading.value = true
  get('/api/admin/system/user/list', (data) => {
    users.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadUsers)
</script>

<style scoped>
.admin-user-page {
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
