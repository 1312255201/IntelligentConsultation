<template>
  <div class="admin-config-center-page">
    <section class="hero-card">
      <div>
        <h3>全局配置管理</h3>
        <p>统一查看平台展示、智能分配和 AI 导诊核心配置，减少后台配置入口分散带来的维护成本。</p>
      </div>
      <el-button type="primary" @click="loadSummary">刷新配置概览</el-button>
    </section>

    <section class="config-grid">
      <article class="config-card">
        <span class="config-kicker">平台信息</span>
        <strong>{{ homepage.heroTitle || '尚未配置平台主标题' }}</strong>
        <p>{{ homepage.noticeText || '当前未设置首页公告。' }}</p>
        <el-button link type="primary" @click="router.push('/admin/platform')">进入平台信息管理</el-button>
      </article>
      <article class="config-card">
        <span class="config-kicker">智能分配</span>
        <strong>候选医生数 {{ dispatch.recommendDoctorLimit || 0 }}</strong>
        <p>最低推荐分 {{ dispatch.minRecommendationScore || 0 }}，最高推荐分 {{ dispatch.maxRecommendationScore || 0 }}。</p>
        <el-button link type="primary" @click="router.push('/admin/consultation-dispatch')">进入分配策略</el-button>
      </article>
      <article class="config-card">
        <span class="config-kicker">AI 导诊</span>
        <strong>{{ ai.enabled === 1 ? '已启用 AI 导诊' : '已关闭 AI 导诊' }}</strong>
        <p>Prompt 版本：{{ ai.promptVersion || '未配置' }}；候选医生上限：{{ ai.doctorCandidateLimit || 0 }}。</p>
        <el-button link type="primary" @click="router.push('/admin/consultation-ai')">进入 AI 配置</el-button>
      </article>
    </section>

    <section class="quick-card">
      <h3>配置联动提醒</h3>
      <div class="quick-grid">
        <article class="quick-item">
          <strong>首页展示</strong>
          <p>平台信息管理负责首页标题、公告、推荐医生和案例展示。</p>
        </article>
        <article class="quick-item">
          <strong>调度策略</strong>
          <p>智能分配配置会直接影响推荐医生排序和接诊超时判断。</p>
        </article>
        <article class="quick-item">
          <strong>AI 行为</strong>
          <p>AI 配置决定导诊开关、Prompt 版本和候选医生数量。</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { get } from '@/net'

const router = useRouter()

const homepage = reactive({
  heroTitle: '',
  noticeText: ''
})

const dispatch = reactive({
  recommendDoctorLimit: 0,
  minRecommendationScore: 0,
  maxRecommendationScore: 0
})

const ai = reactive({
  enabled: 0,
  promptVersion: '',
  doctorCandidateLimit: 0
})

function loadSummary() {
  get('/api/admin/homepage/config', data => Object.assign(homepage, data || {}))
  get('/api/admin/consultation-dispatch/config', data => Object.assign(dispatch, data || {}))
  get('/api/admin/consultation-ai/config', data => Object.assign(ai, data || {}))
}

onMounted(loadSummary)
</script>

<style scoped>
.admin-config-center-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.config-card,
.quick-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 40px rgba(17, 70, 77, 0.08);
}

.hero-card,
.quick-card {
  padding: 24px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  flex-wrap: wrap;
}

.hero-card h3,
.quick-card h3 {
  margin: 0;
  font-size: 24px;
  color: #17373d;
}

.hero-card p,
.config-card p,
.quick-item p {
  margin: 8px 0 0;
  color: #687c84;
  line-height: 1.7;
}

.config-grid,
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.config-card {
  padding: 22px;
}

.config-kicker {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(15, 102, 101, 0.1);
  color: #0f6665;
  font-size: 12px;
}

.config-card strong {
  display: block;
  margin-top: 14px;
  font-size: 24px;
  color: #17373d;
}

.quick-item {
  padding: 18px 20px;
  border-radius: 20px;
  background: #f6fbfb;
}

.quick-item strong {
  color: #17373d;
}
</style>
