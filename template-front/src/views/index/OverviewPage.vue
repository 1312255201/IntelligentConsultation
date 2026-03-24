<template>
  <div class="overview-page">
    <section class="hero-card">
      <div class="hero-copy">
        <span class="section-tag">System Progress</span>
        <h2>基础账户能力已经就绪，智能问诊模块可以继续往上叠加。</h2>
        <p>
          当前模板已经具备登录鉴权、邮箱找回、个人资料维护和 MinIO 头像上传能力，
          接下来可以在这个壳子里逐步接入问诊档案、问诊记录和 AI 问答流程。
        </p>
      </div>
      <el-button type="primary" size="large" round @click="router.push('/index/profile')">
        前往基本信息管理
      </el-button>
    </section>

    <section class="status-grid">
      <article v-for="item in statusCards" :key="item.title" class="status-card">
        <div class="status-top">
          <span class="status-title">{{ item.title }}</span>
          <el-tag :type="item.tagType" effect="light">{{ item.tag }}</el-tag>
        </div>
        <strong>{{ item.value }}</strong>
        <p>{{ item.description }}</p>
      </article>
    </section>

    <section class="summary-card">
      <div class="summary-head">
        <h3>当前账号摘要</h3>
        <span>便于后续扩展问诊身份档案</span>
      </div>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">
          {{ profile.username || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">
          {{ profile.email || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          {{ profile.role || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">
          {{ registerTimeText }}
        </el-descriptions-item>
      </el-descriptions>
    </section>
  </div>
</template>

<script setup>
import { computed, inject } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const accountContext = inject('account-context', null)

const profile = computed(() => accountContext?.profile || {})
const registerTimeText = computed(() => formatDate(profile.value.registerTime))

const statusCards = [
  {
    title: '登录认证',
    value: 'JWT 已接入',
    tag: '已完成',
    tagType: 'success',
    description: '登录、鉴权与退出登录能力可以直接复用。'
  },
  {
    title: '资料维护',
    value: '密码 / 邮箱',
    tag: '已完成',
    tagType: 'success',
    description: '用户登录后可以独立维护基础资料。'
  },
  {
    title: '资源存储',
    value: 'MinIO 头像',
    tag: '已连通',
    tagType: 'warning',
    description: '头像上传已经和对象存储打通。'
  }
]

function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}
</script>

<style scoped>
.overview-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-card,
.summary-card,
.status-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 28px;
  background:
      linear-gradient(135deg, rgba(134, 225, 211, 0.22), rgba(255, 210, 153, 0.22)),
      var(--app-panel);
}

.section-tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(19, 73, 80, 0.08);
  color: #27646d;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h2 {
  margin: 18px 0 12px;
  font-size: 34px;
  line-height: 1.2;
}

.hero-copy p {
  max-width: 780px;
  margin: 0;
  color: var(--app-muted);
  line-height: 1.8;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.status-card {
  padding: 24px;
}

.status-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.status-title {
  color: var(--app-muted);
}

.status-card strong {
  display: block;
  margin-top: 18px;
  font-size: 28px;
}

.status-card p {
  margin: 12px 0 0;
  color: var(--app-muted);
  line-height: 1.7;
}

.summary-card {
  padding: 26px;
}

.summary-head {
  margin-bottom: 18px;
}

.summary-head h3 {
  margin: 0;
  font-size: 24px;
}

.summary-head span {
  display: block;
  margin-top: 8px;
  color: var(--app-muted);
}

@media (max-width: 960px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
