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
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>

      <div class="side-footer">
        {{ footerText }}
      </div>
    </aside>

    <div class="workspace-main">
      <header class="workspace-header">
        <div class="page-meta">
          <div class="page-kicker">{{ pageKicker }}</div>
          <h1>{{ currentItem.title }}</h1>
          <p>{{ currentItem.description }}</p>
        </div>

        <div class="user-panel">
          <el-avatar :size="46" :src="avatarUrl || undefined">
            {{ userInitial }}
          </el-avatar>
          <div class="user-text">
            <strong>{{ profile.username || '加载中...' }}</strong>
            <span>{{ profile.email || '正在获取用户资料' }}</span>
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
import { computed, onMounted, provide, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, logout, resolveHomeRouteByRole, resolveImagePath, syncAccessProfile } from '@/net'

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

function patchProfile(nextProfile = {}) {
  Object.assign(profile, nextProfile)
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
  if (route.path.startsWith('/admin') && role !== 'admin') {
    router.replace(resolveHomeRouteByRole(role))
  } else if (route.path.startsWith('/index') && role === 'admin') {
    router.replace(resolveHomeRouteByRole(role))
  }
}

const currentItem = computed(() => {
  return props.menuItems.find(item => route.path === item.index) || props.menuItems[0]
})

const avatarUrl = computed(() => resolveImagePath(profile.avatar))
const userInitial = computed(() => (profile.username || 'U').slice(0, 1).toUpperCase())

function handleCommand(command) {
  if (command === 'logout') {
    logout(() => router.push('/'))
  } else {
    router.push(command)
  }
}

provide('account-context', {
  avatarUrl,
  patchProfile,
  profile,
  profileLoading,
  refreshProfile
})

onMounted(() => refreshProfile())
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
