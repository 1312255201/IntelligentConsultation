<template>
  <div class="admin-account-page">
    <section class="stat-grid">
      <article class="stat-card">
        <span>系统账号总数</span>
        <strong>{{ accounts.length }}</strong>
      </article>
      <article class="stat-card">
        <span>管理员</span>
        <strong>{{ adminCount }}</strong>
      </article>
      <article class="stat-card">
        <span>医生账号</span>
        <strong>{{ doctorCount }}</strong>
      </article>
      <article class="stat-card">
        <span>普通用户</span>
        <strong>{{ userCount }}</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索用户名、邮箱或绑定医生"
            style="max-width: 320px"
          />
          <el-select v-model="roleFilter" clearable placeholder="全部角色" style="width: 160px">
            <el-option label="管理员" value="admin" />
            <el-option label="医生" value="doctor" />
            <el-option label="用户" value="user" />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadAccounts">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredAccounts" v-loading="loading" border>
        <el-table-column prop="username" label="用户名" min-width="150" />
        <el-table-column prop="email" label="邮箱" min-width="220" show-overflow-tooltip />
        <el-table-column label="角色" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" effect="light">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="绑定医生" min-width="140">
          <template #default="{ row }">{{ row.doctorName || '未绑定' }}</template>
        </el-table-column>
        <el-table-column label="就诊人档案" width="100" align="center">
          <template #default="{ row }">{{ row.patientCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="问诊数" width="100" align="center">
          <template #default="{ row }">{{ row.consultationCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="订单数" width="100" align="center">
          <template #default="{ row }">{{ row.paymentCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.registerTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRoleDialog(row)">调整角色</el-button>
            <el-button link type="warning" @click="openPasswordDialog(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="roleDialogVisible" title="调整账号角色" width="420px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input :model-value="`${activeAccount?.username || ''} / ${activeAccount?.email || ''}`" disabled />
        </el-form-item>
        <el-form-item label="新角色">
          <el-select v-model="roleForm.role" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="医生" value="doctor" />
            <el-option label="用户" value="user" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="submitRole">确认修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="重置账号密码" width="420px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input :model-value="`${activeAccount?.username || ''} / ${activeAccount?.email || ''}`" disabled />
        </el-form-item>
        <el-form-item label="临时密码">
          <el-input v-model="passwordForm.password" show-password maxlength="32" placeholder="请输入 6-32 位临时密码" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="submitPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { get, post } from '@/net'

const loading = ref(false)
const roleSubmitting = ref(false)
const passwordSubmitting = ref(false)
const keyword = ref('')
const roleFilter = ref('')
const accounts = ref([])
const activeAccount = ref(null)
const roleDialogVisible = ref(false)
const passwordDialogVisible = ref(false)

const roleForm = reactive({
  role: 'user'
})

const passwordForm = reactive({
  password: ''
})

const filteredAccounts = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return accounts.value.filter(item => {
    if (roleFilter.value && item.role !== roleFilter.value) return false
    if (!search) return true
    return [item.username, item.email, item.doctorName]
      .filter(Boolean)
      .some(text => `${text}`.toLowerCase().includes(search))
  })
})

const adminCount = computed(() => accounts.value.filter(item => item.role === 'admin').length)
const doctorCount = computed(() => accounts.value.filter(item => item.role === 'doctor').length)
const userCount = computed(() => accounts.value.filter(item => item.role === 'user').length)

function loadAccounts() {
  loading.value = true
  get('/api/admin/system/account/list', (data) => {
    accounts.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function openRoleDialog(row) {
  activeAccount.value = row
  roleForm.role = row.role || 'user'
  roleDialogVisible.value = true
}

function openPasswordDialog(row) {
  activeAccount.value = row
  passwordForm.password = ''
  passwordDialogVisible.value = true
}

function submitRole() {
  if (!activeAccount.value?.id) return
  roleSubmitting.value = true
  post('/api/admin/system/account/role', {
    id: activeAccount.value.id,
    role: roleForm.role
  }, () => {
    roleSubmitting.value = false
    roleDialogVisible.value = false
    ElMessage.success('账号角色已更新')
    loadAccounts()
  }, (message) => {
    roleSubmitting.value = false
    ElMessage.warning(message || '账号角色更新失败')
  })
}

function submitPassword() {
  if (!activeAccount.value?.id) return
  const password = `${passwordForm.password || ''}`.trim()
  if (password.length < 6) {
    ElMessage.warning('临时密码至少 6 位')
    return
  }
  passwordSubmitting.value = true
  post('/api/admin/system/account/password/reset', {
    id: activeAccount.value.id,
    password
  }, () => {
    passwordSubmitting.value = false
    passwordDialogVisible.value = false
    ElMessage.success('账号密码已重置')
  }, (message) => {
    passwordSubmitting.value = false
    ElMessage.warning(message || '账号密码重置失败')
  })
}

function roleLabel(role) {
  if (role === 'admin') return '管理员'
  if (role === 'doctor') return '医生'
  return '用户'
}

function roleTagType(role) {
  if (role === 'admin') return 'danger'
  if (role === 'doctor') return 'success'
  return 'info'
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadAccounts)
</script>

<style scoped>
.admin-account-page {
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
