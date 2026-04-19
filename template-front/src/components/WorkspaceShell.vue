<template>
  <div class="workspace-shell">
    <aside class="side-panel">
      <div class="brand-block">
        <div class="brand-mark">{{ brandMark }}</div>
        <div class="brand-text">
          <div class="brand-title">{{ brandTitle }}</div>
          <div class="brand-subtitle">{{ brandSubtitle }}</div>
        </div>
      </div>

      <el-scrollbar class="side-menu-container" wrap-class="scrollbar-wrap" view-class="scrollbar-view">
        <el-menu
          :default-active="route.path"
          class="side-menu"
          router
          unique-opened
        >
          <template v-for="item in menuItems" :key="item.index">
            <!-- 包含子菜单的项 -->
            <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.index">
              <template #title>
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item
                v-for="sub in item.children"
                :key="sub.index"
                :index="sub.index"
              >
                <el-icon><component :is="sub.icon" /></el-icon>
                <div class="menu-copy">
                  <span>{{ sub.title }}</span>
                  <el-tag
                    v-if="menuBadgeCount(sub) > 0"
                    :type="menuBadgeType(sub)"
                    effect="dark"
                    size="small"
                    class="menu-count-tag"
                  >
                    {{ formatMenuCount(menuBadgeCount(sub)) }}
                  </el-tag>
                </div>
              </el-menu-item>
            </el-sub-menu>

            <!-- 无子菜单且直接路由的项 -->
            <el-menu-item v-else :index="item.index">
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
          </template>
        </el-menu>
      </el-scrollbar>

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
          <el-avatar :size="48" :src="avatarUrl || undefined" class="user-avatar">
            {{ userInitial }}
          </el-avatar>
          <div class="user-text">
            <strong>{{ profile.username || 'Loading...' }}</strong>
            <span>{{ profile.email || 'Fetching user profile' }}</span>
          </div>
          <el-tag v-if="profile.role" type="success" effect="light" round class="role-tag">
            {{ profile.role }}
          </el-tag>
          <el-dropdown @command="handleCommand" trigger="click">
            <el-button class="action-button" plain round>
              选项
              <el-icon class="action-icon el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu class="custom-dropdown">
                <el-dropdown-item
                  v-for="item in flatMenuItems"
                  :key="item.index"
                  :command="item.index"
                >
                  <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
                  {{ item.title }}
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided class="text-danger">
                  <el-icon><Close /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="workspace-body">
        <router-view v-slot="{ Component }">
          <transition name="panel-fade" mode="out-in">
            <div class="body-wrapper">
              <component :is="Component" />
            </div>
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ArrowDown, Close } from '@element-plus/icons-vue'
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
    serviceFeedbackCount: 0,
    unresolvedServiceFeedbackCount: 0,
    lowScoreServiceFeedbackCount: 0,
    attentionServiceFeedbackCount: 0,
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
    Number(summary.overdueFollowUpCount || 0),
    Number(summary.attentionServiceFeedbackCount || 0)
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

const flatMenuItems = computed(() => {
  const result = []
  props.menuItems.forEach(item => {
    if (item.children && item.children.length > 0) {
      result.push(...item.children)
    } else {
      result.push(item)
    }
  })
  return result
})

const currentItem = computed(() => {
  return flatMenuItems.value.find(item => route.path.startsWith(item.index)) || flatMenuItems.value[0] || {}
})

const avatarUrl = computed(() => resolveImagePath(profile.avatar))
const userInitial = computed(() => (profile.username || 'U').slice(0, 1).toUpperCase())

function menuBadgeCount(item) {
  if ((item?.badgeKey || item?.index) === 'patient-reminder' || item?.index === '/index/reminder') {
    return workspaceSummary.totalReminderCount
  }
  if (item?.badgeKey === 'doctor-reminder' || item?.index === '/doctor/reminder') {
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
  if (item?.badgeKey === 'doctor-reminder' || item?.index === '/doctor/reminder') {
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
:global(:root) {
  --ws-bg: #F0F4F8;
  --ws-panel: #FFFFFF;
  --ws-border: #E2E8F0;
  --ws-text: #1C274C;
  --ws-muted: #64748B;
  --ws-primary: #0265DC;
  --ws-sidebar-bg: #0A1930;
  --ws-sidebar-text: #8FA5C8;
  --ws-sidebar-hover: rgba(255, 255, 255, 0.08);
  --ws-sidebar-active-bg: #0265DC;
  --ws-sidebar-active-text: #FFFFFF;
  --ws-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

.workspace-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  background-color: var(--ws-bg);
  padding: 16px;
  gap: 16px;
  font-family: 'Inter', -apple-system, sans-serif;
  box-sizing: border-box;
}

/* Sidebar Elements */
.side-panel {
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #0A1930 0%, #112F5C 100%);
  border-radius: 24px;
  color: #fff;
  box-shadow: 0 10px 40px rgba(10, 25, 48, 0.15);
  overflow: hidden;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 28px 24px 20px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #0265DC, #4A90E2);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
  box-shadow: 0 8px 16px rgba(2, 101, 220, 0.3);
}

.brand-text { display: flex; flex-direction: column; }
.brand-title { font-size: 18px; font-weight: 700; color: #FFFFFF; letter-spacing: 0.5px; }
.brand-subtitle { font-size: 11px; margin-top: 4px; color: var(--ws-sidebar-text); text-transform: uppercase; letter-spacing: 1px; font-weight: 600; }

.side-menu-container {
  flex: 1;
  padding: 0 12px;
}

.side-menu {
  border-right: none;
  background: transparent;
}

.side-menu :deep(.el-menu) {
  background-color: transparent !important;
}

.side-menu :deep(.el-sub-menu__title) {
  border-radius: 12px;
  color: var(--ws-sidebar-text);
  margin-bottom: 4px;
  height: 48px;
  line-height: 48px;
  transition: all 0.2s;
}

.side-menu :deep(.el-menu-item) {
  border-radius: 12px;
  color: var(--ws-sidebar-text);
  margin-bottom: 4px;
  height: 48px;
  line-height: 48px;
  transition: all 0.2s;
}

.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-sub-menu__title:hover) {
  background: var(--ws-sidebar-hover) !important;
  color: #ffffff;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: var(--ws-sidebar-active-bg) !important;
  color: var(--ws-sidebar-active-text);
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(2, 101, 220, 0.4);
}

.side-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #ffffff;
}

.side-menu :deep(.el-menu-item .el-icon),
.side-menu :deep(.el-sub-menu__title .el-icon) {
  font-size: 18px;
  margin-right: 12px;
}

.menu-copy {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.side-footer {
  padding: 24px;
  font-size: 12px;
  color: var(--ws-sidebar-text);
  text-align: center;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

/* Main Workspace Elements */
.workspace-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.workspace-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--ws-panel);
  border-radius: 24px;
  padding: 24px 32px;
  box-shadow: var(--ws-shadow);
  border: 1px solid var(--ws-border);
}

.page-kicker {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--ws-primary);
  margin-bottom: 8px;
}

.page-meta h1 {
  font-size: 26px;
  font-weight: 800;
  color: var(--ws-text);
  margin: 0 0 6px 0;
}

.page-meta p {
  font-size: 14px;
  color: var(--ws-muted);
  margin: 0;
}

.user-panel {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-avatar {
  border: 2px solid white;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

.user-text {
  display: flex;
  flex-direction: column;
}

.user-text strong {
  font-size: 15px;
  color: var(--ws-text);
}

.user-text span {
  font-size: 13px;
  color: var(--ws-muted);
}

.role-tag {
  font-weight: 600;
}

.action-button {
  border-color: var(--ws-border);
}

.workspace-body {
  flex: 1;
  background: var(--ws-panel);
  border-radius: 24px;
  box-shadow: var(--ws-shadow);
  border: 1px solid var(--ws-border);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.body-wrapper {
  padding: 32px;
  flex: 1;
  overflow-y: auto;
}

/* Transitions */
.panel-fade-enter-active,
.panel-fade-leave-active {
  transition: all 0.2s ease-out;
}

.panel-fade-enter-from,
.panel-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.text-danger { color: #F56C6C; }
.custom-dropdown .el-icon { margin-right: 8px; }

@media (max-width: 1024px) {
  .workspace-shell { grid-template-columns: 240px minmax(0, 1fr); }
  .workspace-header { padding: 20px; }
  .body-wrapper { padding: 20px; }
}
@media (max-width: 768px) {
  .workspace-shell { grid-template-columns: 1fr; }
  .side-panel { display: none; /* simple mobile fallback, ideally add a drawer */ }
  .user-text, .role-tag { display: none; }
}
</style>
