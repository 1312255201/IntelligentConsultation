<template>
  <div class="workspace-shell">
    <aside class="side-panel">
      <div class="brand-block">
        <div class="brand-mark">{{ brandMark }}</div>
        <div>
          <div class="brand-title">{{ brandTitle }}</div>
          <div class="brand-subtitle">{{ brandSubtitle }}</div>
        </div>
      </div>

      <el-menu
        :default-active="route.path"
        class="side-menu"
        router
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.index"
          :index="item.index"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <div class="menu-copy">
            <span>{{ item.title }}</span>
            <el-tag
              v-if="menuBadgeCount(item) > 0"
              :type="menuBadgeType(item)"
              effect="dark"
              size="small"
              class="menu-count-tag"
            >
              {{ formatMenuCount(menuBadgeCount(item)) }}
            </el-tag>
          </div>
        </el-menu-item>
      </el-menu>

      <div v-if="footerText" class="side-footer">
        {{ footerText }}
      </div>
    </aside>

    <div class="workspace-main">
      <header class="workspace-header">
        <div class="page-meta">
          <div class="page-kicker">{{ pageKicker }}</div>
          <h1>{{ currentItem.title }}</h1>
          <p v-if="currentItem.description">{{ currentItem.description }}</p>
        </div>

        <div class="user-panel">
          <el-avatar :size="46" :src="avatarUrl || undefined">
            {{ userInitial }}
          </el-avatar>
          <div class="user-text">
            <strong>{{ profile.username || 'Loading...' }}</strong>
            <span>{{ profile.email || 'Fetching user profile' }}</span>
          </div>
          <el-tag v-if="profile.role" type="success" effect="light">
            {{ profile.role }}
          </el-tag>
          <el-dropdown @command="handleCommand">
            <el-button class="action-button" plain>
              操作
              <el-icon class="action-icon"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="item in menuItems"
                  :key="item.index"
                  :command="item.index"
                >
                  {{ item.title }}
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="workspace-body">
        <router-view v-slot="{ Component }">
          <transition name="panel-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, provide, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, logout, resolveHomeRouteByRole, resolveImagePath, syncAccessProfile } from '@/net'
import {
  followUpState,
  isPendingFollowUpRecord,
  isReminderRecord,
  normalizeReminderRecords,
  recordHasUnreadDoctorReply,
  recordProgressStage
} from '@/triage/reminder'

const props = defineProps({
  brandMark: {
    type: String,
    default: 'IC'
  },
  brandTitle: {
    type: String,
    required: true
  },
  brandSubtitle: {
    type: String,
    default: ''
  },
  footerText: {
    type: String,
    default: ''
  },
  pageKicker: {
    type: String,
    default: 'Workspace'
  },
  menuItems: {
    type: Array,
    required: true
  }
})

const router = useRouter()
const route = useRoute()

const profileLoading = ref(false)
const profile = reactive(createEmptyProfile())
const workspaceSummaryLoading = ref(false)
const workspaceSummary = reactive(createEmptyWorkspaceSummary())
const doctorWorkspaceSummaryLoading = ref(false)
const doctorWorkspaceSummary = reactive(createEmptyDoctorWorkspaceSummary())

function createEmptyProfile() {
  return {
    id: null,
    username: '',
    email: '',
    role: '',
    avatar: '',
    registerTime: ''
  }
}

function createEmptyWorkspaceSummary() {
  return {
    totalReminderCount: 0,
    unreadDoctorReplyCount: 0,
    waitingDoctorHandleCount: 0,
    pendingFollowUpCount: 0,
    dueTodayFollowUpCount: 0,
    overdueFollowUpCount: 0
  }
}

function createEmptyDoctorWorkspaceSummary() {
  return {
    bound: 0,
    unclaimedConsultationCount: 0,
    highPriorityUnclaimedCount: 0,
    unreadConsultationCount: 0,
    waitingReplyConsultationCount: 0,
    pendingFollowUpCount: 0,
    dueTodayFollowUpCount: 0,
    overdueFollowUpCount: 0,
    recommendedConsultationCount: 0,
    myClaimedConsultationCount: 0,
    actionableConsultationCount: 0
  }
}

function patchProfile(nextProfile = {}) {
  Object.assign(profile, nextProfile)
}

function patchWorkspaceSummary(nextSummary = {}) {
  Object.assign(workspaceSummary, createEmptyWorkspaceSummary(), nextSummary)
}

function resolveDoctorActionableCount(summary = {}) {
  const counts = [
    Number(summary.unclaimedConsultationCount || 0),
    Number(summary.unreadConsultationCount || 0),
    Number(summary.waitingReplyConsultationCount || 0),
    Number(summary.overdueFollowUpCount || 0)
  ]
  return counts.reduce((total, value) => total + (Number.isFinite(value) ? value : 0), 0)
}

function buildDoctorWorkspaceSummary(summary = {}) {
  const nextSummary = {
    ...createEmptyDoctorWorkspaceSummary(),
    ...(summary || {})
  }
  nextSummary.actionableConsultationCount = resolveDoctorActionableCount(nextSummary)
  return nextSummary
}

function patchDoctorWorkspaceSummary(nextSummary = {}) {
  Object.assign(doctorWorkspaceSummary, buildDoctorWorkspaceSummary(nextSummary))
}

function routeRoleByPath(path = '') {
  if (path.startsWith('/admin')) return 'admin'
  if (path.startsWith('/doctor')) return 'doctor'
  if (path.startsWith('/index')) return 'user'
  return null
}

function refreshProfile(showLoading = true) {
  if (showLoading) {
    profileLoading.value = true
  }

  get('/api/user/me', (data) => {
    Object.assign(profile, createEmptyProfile(), data)
    syncAccessProfile(data)
    profileLoading.value = false
    redirectWhenRoleMismatch(data.role)
  }, (message) => {
    profileLoading.value = false
    if (message) {
      ElMessage.warning(message)
    }
  })
}

function redirectWhenRoleMismatch(role) {
  const requiredRole = routeRoleByPath(route.path)
  if (requiredRole && requiredRole !== role) {
    router.replace(resolveHomeRouteByRole(role))
  }
}

function buildUserWorkspaceSummary(records = []) {
  const followUpRecords = records.filter(isPendingFollowUpRecord)
  return {
    totalReminderCount: records.filter(isReminderRecord).length,
    unreadDoctorReplyCount: records.filter(recordHasUnreadDoctorReply).length,
    waitingDoctorHandleCount: records.filter(item => recordProgressStage(item) === 'waiting_doctor').length,
    pendingFollowUpCount: followUpRecords.length,
    dueTodayFollowUpCount: followUpRecords.filter(item => followUpState(item) === 'due_today').length,
    overdueFollowUpCount: followUpRecords.filter(item => followUpState(item) === 'overdue').length
  }
}

function refreshWorkspaceSummary(showLoading = false) {
  if (routeRoleByPath(route.path) !== 'user') {
    patchWorkspaceSummary()
    workspaceSummaryLoading.value = false
    return
  }

  if (showLoading) {
    workspaceSummaryLoading.value = true
  }

  get('/api/user/consultation/record/list', (data) => {
    patchWorkspaceSummary(buildUserWorkspaceSummary(normalizeReminderRecords(data || [])))
    workspaceSummaryLoading.value = false
  }, (message) => {
    workspaceSummaryLoading.value = false
    if (showLoading && message) {
      ElMessage.warning(message)
    }
  })
}

function refreshDoctorWorkspaceSummary(showLoading = false) {
  if (routeRoleByPath(route.path) !== 'doctor') {
    patchDoctorWorkspaceSummary()
    doctorWorkspaceSummaryLoading.value = false
    return
  }

  if (showLoading) {
    doctorWorkspaceSummaryLoading.value = true
  }

  get('/api/doctor/workbench/summary', (data) => {
    patchDoctorWorkspaceSummary(data || {})
    doctorWorkspaceSummaryLoading.value = false
  }, (message) => {
    doctorWorkspaceSummaryLoading.value = false
    if (showLoading && message) {
      ElMessage.warning(message)
    }
  })
}

const currentItem = computed(() => {
  return props.menuItems.find(item => route.path === item.index) || props.menuItems[0]
})

const avatarUrl = computed(() => resolveImagePath(profile.avatar))
const userInitial = computed(() => (profile.username || 'U').slice(0, 1).toUpperCase())

function menuBadgeCount(item) {
  if ((item?.badgeKey || item?.index) === 'patient-reminder' || item?.index === '/index/reminder') {
    return workspaceSummary.totalReminderCount
  }
  if ((item?.badgeKey || item?.index) === 'doctor-consultation' || item?.index === '/doctor/consultation') {
    return doctorWorkspaceSummary.actionableConsultationCount
  }
  return 0
}

function menuBadgeType(item) {
  if ((item?.badgeKey || item?.index) === 'patient-reminder' || item?.index === '/index/reminder') {
    if (workspaceSummary.overdueFollowUpCount > 0) {
      return 'danger'
    }
    return 'primary'
  }
  if ((item?.badgeKey || item?.index) === 'doctor-consultation' || item?.index === '/doctor/consultation') {
    if (doctorWorkspaceSummary.highPriorityUnclaimedCount > 0 || doctorWorkspaceSummary.overdueFollowUpCount > 0) {
      return 'danger'
    }
    if (doctorWorkspaceSummary.unreadConsultationCount > 0 || doctorWorkspaceSummary.waitingReplyConsultationCount > 0) {
      return 'warning'
    }
    return 'primary'
  }
  return 'info'
}

function formatMenuCount(value) {
  return value > 99 ? '99+' : value
}

function handleCommand(command) {
  if (command === 'logout') {
    logout(() => router.push('/'))
  } else {
    router.push(command)
  }
}

provide('account-context', {
  avatarUrl,
  doctorWorkspaceSummary,
  doctorWorkspaceSummaryLoading,
  patchProfile,
  patchDoctorWorkspaceSummary,
  profile,
  profileLoading,
  refreshProfile,
  refreshDoctorWorkspaceSummary,
  patchWorkspaceSummary,
  refreshWorkspaceSummary,
  workspaceSummary,
  workspaceSummaryLoading
})

watch(() => route.path, (path) => {
  const role = routeRoleByPath(path)
  if (role === 'user') {
    refreshWorkspaceSummary()
    patchDoctorWorkspaceSummary()
  } else if (role === 'doctor') {
    patchWorkspaceSummary()
    refreshDoctorWorkspaceSummary()
  } else {
    patchWorkspaceSummary()
    patchDoctorWorkspaceSummary()
  }
})

onMounted(() => {
  refreshProfile()
  if (routeRoleByPath(route.path) === 'doctor') {
    refreshDoctorWorkspaceSummary(true)
  } else {
    refreshWorkspaceSummary(true)
  }
})
</script>

<style scoped>
.workspace-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 272px minmax(0, 1fr);
  padding: 20px;
  gap: 18px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px 18px;
  border: 1px solid rgba(16, 57, 60, 0.08);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(19, 73, 80, 0.92), rgba(19, 73, 80, 0.78));
  color: #f3fbfb;
  box-shadow: 0 20px 55px rgba(12, 45, 49, 0.2);
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 6px 2px;
}

.brand-mark {
  width: 50px;
  height: 50px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: linear-gradient(135deg, #88e0d1, #f6d39a);
  color: #0d4047;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.brand-title {
  font-size: 20px;
  font-weight: 700;
}

.brand-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(243, 251, 251, 0.72);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.side-menu {
  flex: 1;
  border: none;
  background: transparent;
}

.side-menu :deep(.el-menu-item) {
  margin-bottom: 8px;
  border-radius: 16px;
  color: rgba(243, 251, 251, 0.82);
}

.menu-copy {
  min-width: 0;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.menu-count-tag {
  flex-shrink: 0;
}

.side-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.18);
  color: #ffffff;
  font-weight: 600;
}

.side-footer {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(243, 251, 251, 0.75);
  line-height: 1.7;
  font-size: 13px;
}

.workspace-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.workspace-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 24px 28px;
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
  backdrop-filter: blur(18px);
}

.page-kicker {
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #5e8f92;
}

.page-meta h1 {
  margin: 8px 0 10px;
  font-size: 30px;
  line-height: 1.1;
}

.page-meta p {
  margin: 0;
  color: var(--app-muted);
}

.user-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.user-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-text strong,
.user-text span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-text span {
  margin-top: 4px;
  color: var(--app-muted);
  font-size: 13px;
}

.action-button {
  border-radius: 14px;
  padding-inline: 16px;
}

.action-icon {
  margin-left: 6px;
}

.workspace-body {
  min-height: 0;
}

.panel-fade-enter-active,
.panel-fade-leave-active {
  transition: all 0.24s ease;
}

.panel-fade-enter-from,
.panel-fade-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

@media (max-width: 1100px) {
  .workspace-shell {
    grid-template-columns: 1fr;
  }

  .side-panel {
    gap: 14px;
  }
}

@media (max-width: 760px) {
  .workspace-shell {
    padding: 12px;
  }

  .workspace-header {
    flex-direction: column;
    align-items: flex-start;
    padding: 20px;
  }

  .user-panel {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
