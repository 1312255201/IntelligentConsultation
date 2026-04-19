<template>
  <div class="home-page">
    <!-- Navbar -->
    <header class="navbar">
      <div class="nav-content">
        <div class="logo">
          <div class="logo-icon">
            <el-icon><Monitor /></el-icon>
          </div>
          <div class="logo-text">
            <strong>智慧医疗问诊系统</strong>
            <span>Intelligent Consultation</span>
          </div>
        </div>
        <nav class="nav-links">
          <a @click="scrollTo('departments')">重点科室</a>
          <a @click="scrollTo('doctors')">推荐医生</a>
          <a @click="scrollTo('cases')">经典案例</a>
          <a @click="scrollTo('process')">服务流程</a>
        </nav>
        <div class="nav-actions">
          <el-button round plain class="btn-login" @click="goLoginPage">
            {{ unauthorized() ? '登录 / 注册' : '我的服务' }}
          </el-button>
          <el-button round type="primary" class="btn-gradient" @click="goConsultEntry">免费在线咨询</el-button>
        </div>
      </div>
    </header>

    <main class="main-content">
      <!-- Hero Section -->
      <section class="hero-section">
        <div class="hero-bg-shapes">
          <div class="shape shape-1"></div>
          <div class="shape shape-2"></div>
        </div>
        
        <div class="hero-container">
          <div class="hero-text">
            <div class="hero-badge" @click="goConsultEntry">
              <span class="badge-pulse"></span>
              {{ landing.config.noticeText }}
              <el-icon><ArrowRight /></el-icon>
            </div>
            <h1 class="hero-title">{{ landing.config.heroTitle }}</h1>
            <p class="hero-subtitle">{{ landing.config.heroSubtitle }}</p>
            <div class="hero-cta">
              <el-button round size="large" type="primary" class="btn-gradient btn-large" @click="goConsultEntry">
                立即发起咨询
              </el-button>
              <el-button round size="large" class="btn-outlined btn-large" @click="scrollTo('doctors')">
                查看推荐专家
              </el-button>
            </div>
            <div class="hero-stats">
              <div class="stat-item">
                <span class="stat-num">{{ displayDepartments.length }}+</span>
                <span class="stat-label">覆盖科室</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-num">{{ displayRecommendDoctors.length }}+</span>
                <span class="stat-label">权威专家</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-num">{{ displayCases.length }}+</span>
                <span class="stat-label">康复案例</span>
              </div>
            </div>
          </div>
          
          <div class="hero-visual">
            <div class="carousel-wrapper">
              <el-carousel :interval="5000" type="card" height="380px" @change="handleCarouselChange" :autoplay="true" indicator-position="none">
                <el-carousel-item v-for="slide in carouselSlides" :key="slide.id">
                  <div class="carousel-card" @click="handleSlideAction(slide)">
                    <el-image v-if="slide.image" :src="slide.image" fit="cover" class="card-img" />
                    <div v-else class="card-fallback">{{ slide.fallback }}</div>
                    <div class="card-overlay">
                      <span class="card-badge">{{ slide.badge }}</span>
                      <h3>{{ slide.name }}</h3>
                      <p>{{ slide.title }}</p>
                    </div>
                  </div>
                </el-carousel-item>
              </el-carousel>
              
              <!-- Active Slide Info -->
              <div class="carousel-info" v-if="activeSlide">
                <div class="info-kicker">{{ activeSlide.category }}</div>
                <div class="info-title">{{ activeSlide.name }}</div>
                <div class="info-desc">{{ activeSlide.summary }}</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Overview Grid -->
      <section class="overview-section">
        <div class="overview-grid">
          <div class="overview-main glass-card">
            <span class="section-tag">Platform Overview</span>
            <h2>{{ landing.config.introTitle }}</h2>
            <p>{{ landing.config.introContent }}</p>
            <div v-if="landing.config.servicePhone" class="contact-pill">
              <el-icon><Phone /></el-icon> {{ landing.config.servicePhone }}
            </div>
          </div>
          <div v-for="item in introCards" :key="item.title" class="feature-card glass-card hover-lift">
            <div class="feature-icon-wrapper">
              <el-icon class="feature-icon"><component :is="item.icon" /></el-icon>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
            <a class="feature-link" @click="item.action">
              {{ item.actionText }} <el-icon><ArrowRight /></el-icon>
            </a>
          </div>
        </div>
      </section>

      <!-- Departments -->
      <section id="departments" class="content-section">
        <div class="section-header">
          <div class="header-titles">
            <span class="section-tag">Medical Departments</span>
            <h2 class="section-title">重点服务科室</h2>
            <p class="section-subtitle">围绕常见问诊需求与健康管理场景，精准对接各类重点服务。</p>
          </div>
          <el-button round class="view-more-btn" @click="goConsultEntry">
            全部科室咨询 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        
        <div v-if="displayDepartments.length" class="grid-3">
          <div v-for="item in displayDepartments" :key="item.id" class="card dept-card shadow-hover">
            <div class="dept-top">
              <div class="dept-icon-box">
                <el-icon><OfficeBuilding /></el-icon>
              </div>
              <div class="dept-title">
                <h3>{{ item.name }}</h3>
                <span class="dept-code">{{ item.code }}</span>
              </div>
            </div>
            <p class="dept-desc">{{ item.description || '提供在线沟通、就诊建议与后续健康管理支持。' }}</p>
            <div class="dept-meta">
              <span v-if="item.location" class="meta-item"><el-icon><Location /></el-icon>{{ item.location }}</span>
              <span v-if="item.phone" class="meta-item"><el-icon><Phone /></el-icon>{{ item.phone }}</span>
            </div>
            <el-button plain round class="btn-full" @click="goConsultEntry">选择该科室</el-button>
          </div>
        </div>
        <el-empty v-else description="科室信息整理中，您可以先发起在线咨询" />
      </section>

      <!-- Doctors -->
      <section id="doctors" class="content-section bg-subtle">
        <div class="section-header">
          <div class="header-titles">
            <span class="section-tag">Expert Team</span>
            <h2 class="section-title">推荐专家团队</h2>
            <p class="section-subtitle">由三甲医院资深主任领衔，为您提供最专业可靠的诊疗建议。</p>
          </div>
          <el-button round class="view-more-btn" @click="goConsultEntry">
            在线预约专家 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <div v-if="displayRecommendDoctors.length" class="grid-2">
          <div v-for="item in displayRecommendDoctors" :key="item.id" class="card doctor-card shadow-hover">
            <div class="doc-header">
              <el-avatar :size="72" :src="resolveImagePath(item.photo) || undefined" class="doc-avatar">
                {{ item.name?.slice(0, 1) || 'D' }}
              </el-avatar>
              <div class="doc-titles">
                <h3>{{ item.name }}</h3>
                <span class="doc-role">{{ item.title || item.displayTitle || '推荐医生' }}</span>
              </div>
              <div class="doc-tags">
                <span class="tag-primary">{{ item.departmentName }}</span>
                <span class="tag-secondary" v-if="item.displayTitle">{{ item.displayTitle }}</span>
              </div>
            </div>
            <div class="doc-body">
              <div class="doc-field">
                <span class="field-label">擅长：</span>
                <p>{{ item.recommendReason || item.introduction || '擅长多场景诊疗与健康管理服务。' }}</p>
              </div>
              <div class="doc-field">
                <span class="field-label">简介：</span>
                <p>{{ item.expertise || '支持在线问诊、复诊指导与健康建议。' }}</p>
              </div>
            </div>
            <div class="doc-actions">
              <el-button type="primary" round plain @click="goConsultEntry">向他提问</el-button>
              <el-button round plain @click="scrollTo('cases')">查看案例</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="专家团队信息整理中" />
      </section>

      <!-- Cases -->
      <section id="cases" class="content-section">
        <div class="section-header">
          <div class="header-titles">
            <span class="section-tag">Success Cases</span>
            <h2 class="section-title">经典康复案例</h2>
            <p class="section-subtitle">真实的诊疗记录与方案参考，帮助您深入了解治疗思路。</p>
          </div>
          <el-button round class="view-more-btn" @click="goConsultEntry">
            获取专属方案 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <div v-if="displayCases.length" class="grid-cases">
          <div v-for="(item, index) in displayCases" :key="item.id" :class="['card case-card shadow-hover', { 'case-large': index === 0 }]">
            <div class="case-img-wrap">
              <el-image :src="resolveImagePath(item.cover)" fit="cover" class="case-cover" />
              <div class="case-badge">{{ item.departmentName }}</div>
            </div>
            <div class="case-content">
              <h3>{{ item.title }}</h3>
              <p class="case-summary">{{ item.summary }}</p>
              <div class="case-tags-row">
                <span v-for="tag in parseTags(item.tags)" :key="tag" class="tag-outline">{{ tag }}</span>
              </div>
              <div class="case-footer">
                <div class="case-doc" v-if="item.doctorName">
                  <el-icon><UserFilled /></el-icon> {{ item.doctorName }}
                </div>
                <div class="case-actions">
                  <el-button type="primary" link @click="openCaseDetail(item)">了解详情 <el-icon><ArrowRight /></el-icon></el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="案例内容持续更新中" />
      </section>

      <!-- Process -->
      <section id="process" class="process-section">
        <div class="process-header">
          <span class="section-tag text-light">Process</span>
          <h2 class="section-title text-white">智慧就医，化繁为简</h2>
          <p class="section-subtitle text-light-muted" style="margin: 0 auto;">简单四步，即可享受专业、便捷的在线医疗服务体系。</p>
        </div>
        <div class="grid-4 process-grid">
          <div v-for="(item, index) in processSteps" :key="index" class="process-step">
            <div class="step-num">0{{ index + 1 }}</div>
            <div class="step-card">
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
              <div class="step-action" @click="goConsultEntry">{{ item.action }} <el-icon><ArrowRight /></el-icon></div>
            </div>
          </div>
        </div>
      </section>

      <!-- CTA Banner -->
      <section class="cta-banner">
        <div class="cta-banner-bg"></div>
        <div class="cta-content">
          <span class="section-tag text-light">Start Now</span>
          <h2>开启您的专属健康之旅</h2>
          <p>无论日常咨询还是疑难求助，我们时刻为您提供专业支持。</p>
        </div>
        <div class="cta-actions">
          <el-button round size="large" class="btn-inverse" @click="goLoginPage">
            {{ unauthorized() ? '登录 / 注册' : '我的个人中心' }}
          </el-button>
          <el-button round size="large" type="primary" class="btn-gradient-light" @click="goConsultEntry">
            立即发起在线问诊
          </el-button>
        </div>
      </section>
    </main>

    <!-- Floating Action Button -->
    <el-tooltip content="在线客服 / 发起咨询" placement="left">
      <button class="fab-btn" @click="goConsultEntry">
        <el-icon><Service /></el-icon>
      </button>
    </el-tooltip>

    <!-- Case Detail Dialog -->
    <el-dialog v-model="caseDialogVisible" width="760px" :title="activeCase?.title || '经典案例详情'" destroy-on-close custom-class="custom-dialog">
      <div v-if="activeCase" class="dialog-case-detail">
        <el-image class="dialog-img" :src="resolveImagePath(activeCase.cover)" fit="cover" />
        <div class="dialog-meta">
          <span class="tag-primary">{{ activeCase.departmentName }}</span>
          <span class="tag-secondary" v-if="activeCase.doctorName">{{ activeCase.doctorName }}</span>
          <span class="tag-secondary" v-if="activeCase.doctorTitle">{{ activeCase.doctorTitle }}</span>
        </div>
        <p class="dialog-summary">{{ activeCase.summary }}</p>
        <div class="dialog-content">
          <p>{{ activeCase.detail || '如需进一步了解该案例的适用人群、诊疗思路与后续建议，可继续发起在线咨询。' }}</p>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button round @click="caseDialogVisible = false">关闭窗口</el-button>
          <el-button round type="primary" class="btn-gradient" @click="goConsultEntry">向医生咨询同类问题</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {
  ArrowRight,
  DataAnalysis,
  Location,
  OfficeBuilding,
  Phone,
  Service,
  UserFilled,
  Monitor
} from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPublic, resolveHomeRouteByRole, resolveImagePath, takeAccessRole, unauthorized } from '@/net'

const router = useRouter()
const caseDialogVisible = ref(false)
const activeCase = ref(null)
const carouselIndex = ref(0)

const landing = reactive(createDefaultLanding())

const introCards = [
  { title: '即时在线咨询', description: '随时随地描述您的健康困扰，快速获取专业医生的初步建议。', actionText: '开始咨询', action: () => goConsultEntry(), icon: Service },
  { title: '权威专家团队', description: '汇聚三甲医院资深专家，多科室覆盖，精准匹配您的医疗需求。', actionText: '查看专家', action: () => scrollTo('doctors'), icon: UserFilled },
  { title: '海量康复案例', description: '真实诊疗记录与权威方案参考，为您提供安心、靠谱的就医指引。', actionText: '浏览案例', action: () => scrollTo('cases'), icon: DataAnalysis }
]

const processSteps = [
  { title: '了解服务与专家', description: '浏览科室、专家团队及真实案例，初步判定服务匹配度。', action: '查看服务' },
  { title: '提交详细症状', description: '在线描述您的具体症状与困惑，提供病历报告以便精确评估。', action: '提交信息' },
  { title: '获取专业方案', description: '专家团队结合病情，给出针对性的诊疗建议与干预方案。', action: '获取方案' },
  { title: '健康随访跟进', description: '个性化复诊计划与持续跟踪，保障您的长期健康。', action: '开始跟进' }
]

const displayDepartments = computed(() => landing.departments.slice(0, 6))
const displayRecommendDoctors = computed(() => landing.recommendDoctors.slice(0, 4))
const displayCases = computed(() => landing.cases.slice(0, 4))

const carouselSlides = computed(() => {
  const doctors = landing.recommendDoctors.slice(0, 4).map(item => ({
    id: `doctor-${item.id}`,
    sourceId: item.id,
    type: 'doctor',
    category: '✨ 专家推荐',
    badge: item.departmentName || 'Doctor',
    name: item.name,
    title: item.displayTitle || item.title || '权威推荐专家',
    summary: item.recommendReason || item.expertise || '提供专业的在线诊疗与健康指导。',
    image: resolveImagePath(item.photo),
    tags: compactTags([item.departmentName, item.title, item.displayTitle]),
    actionText: '预约该专家',
    fallback: (item.name || 'D').slice(0, 1)
  }))
  const cases = landing.cases.slice(0, 3).map(item => ({
    id: `case-${item.id}`,
    sourceId: item.id,
    type: 'case',
    category: '📖 经典案例',
    badge: item.departmentName || 'Case',
    name: item.title,
    title: item.doctorName ? `${item.doctorName} · ${item.doctorTitle || '方案参考'}` : item.departmentName,
    summary: item.summary || '真实诊疗记录，助您深入了解诊疗思路与对策。',
    image: resolveImagePath(item.cover),
    tags: compactTags([item.departmentName, item.doctorName, ...parseTags(item.tags)]),
    actionText: '了解详情',
    fallback: 'C'
  }))
  const mixed = []
  const length = Math.max(doctors.length, cases.length)
  for (let index = 0; index < length; index++) {
    if (doctors[index]) mixed.push(doctors[index])
    if (cases[index]) mixed.push(cases[index])
  }
  if (mixed.length) return mixed.slice(0, 6)
  return [{
    id: 'placeholder',
    sourceId: 0,
    type: 'doctor',
    category: '⭐ 特色服务',
    badge: 'Medical',
    name: '智慧医疗问诊系统',
    title: '提供全天候在线健康咨询',
    summary: '汇聚优质医疗资源，解决您的健康难题。',
    image: '',
    tags: ['在线问诊', '推荐医生', '案例展示'],
    actionText: '立即体验',
    fallback: 'IC'
  }]
})

const activeSlide = computed(() => carouselSlides.value[carouselIndex.value] || null)

function handleCarouselChange(index) {
  carouselIndex.value = index
}

function createDefaultLanding() {
  return {
    config: {
      heroTitle: '智慧医疗，护航您的健康',
      heroSubtitle: '提供专业、可靠、快速响应的在线问诊与一站式健康管理服务体系。',
      noticeText: '优质医疗资源持续更新中，为您带来最佳就医体验。',
      introTitle: '为什么选择我们？',
      introContent: '依托前沿互联网医疗技术与三甲权威专家团队，打破时空限制，将优质医疗服务直接送到您身边。',
      servicePhone: ''
    },
    departments: [],
    recommendDoctors: [],
    cases: []
  }
}

function loadLanding() {
  getPublic('/api/homepage/landing', (data) => {
    Object.assign(landing, createDefaultLanding(), data || {})
  }, () => {})
}

function goLoginPage() {
  unauthorized() ? router.push('/login') : router.push(resolveHomeRouteByRole())
}

function goConsultEntry() {
  if (unauthorized()) {
    router.push({
      path: '/login',
      query: {
        redirect: '/index/consultation'
      }
    })
    return
  }
  const role = takeAccessRole()
  router.push(role === 'user' ? '/index/consultation' : resolveHomeRouteByRole(role))
}

function scrollTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openCaseDetail(item) {
  activeCase.value = item
  caseDialogVisible.value = true
}

function handleSlideAction(slide) {
  if (slide.type === 'case') {
    const item = landing.cases.find(current => current.id === slide.sourceId)
    if (item) {
      openCaseDetail(item)
      return
    }
  }
  goConsultEntry()
}

function parseTags(value) {
  if (!value) return []
  return String(value).split(/[，,]/).map(item => item.trim()).filter(Boolean).slice(0, 4)
}

function compactTags(list) {
  return list.filter(Boolean).slice(0, 3)
}

onMounted(() => loadLanding())
</script>

<style scoped>
/* Base Variables & Reset */
:global(:root) {
  --primary: #0265DC;
  --primary-light: #4A90E2;
  --primary-gradient: linear-gradient(135deg, #0265DC 0%, #4A90E2 100%);
  --secondary: #0A1930;
  --text-main: #1C274C;
  --text-muted: #64748B;
  --bg-page: #F8FAFC;
  --bg-surface: #FFFFFF;
  --border-light: #E2E8F0;
  --radius-sm: 8px;
  --radius-md: 16px;
  --radius-lg: 24px;
  --shadow-sm: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 10px 15px -3px rgba(0, 0, 0, 0.04);
  --shadow-hover: 0 20px 25px -5px rgba(2, 101, 220, 0.08);
}

.home-page * { box-sizing: border-box; }
.home-page p, .home-page h1, .home-page h2, .home-page h3, .home-page h4, .home-page h5 { margin: 0; font-family: 'Inter', -apple-system, sans-serif; }

.home-page { min-height: 100vh; background-color: var(--bg-page); color: var(--text-main); font-family: 'Inter', sans-serif; overflow-x: hidden; }

/* Components */
.tag-primary { background: rgba(2, 101, 220, 0.1); color: var(--primary); padding: 4px 10px; border-radius: var(--radius-sm); font-size: 13px; font-weight: 600; }
.tag-secondary { background: #F1F5F9; color: var(--text-muted); padding: 4px 10px; border-radius: var(--radius-sm); font-size: 13px; font-weight: 500; }
.tag-outline { border: 1px solid var(--border-light); color: var(--text-muted); padding: 4px 10px; border-radius: var(--radius-sm); font-size: 13px; background: transparent; }

.btn-gradient { background: var(--primary-gradient); border: none; color: white; transition: all 0.3s ease; }
.btn-gradient:hover { box-shadow: 0 8px 16px rgba(2, 101, 220, 0.3); transform: translateY(-1px); color: white; }
.btn-gradient-light { background: white; color: var(--primary); border: none; font-weight: 600; transition: all 0.3s ease; }
.btn-gradient-light:hover { background: #F8FAFC; transform: translateY(-1px); }
.btn-inverse { background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); color: white; backdrop-filter: blur(4px); transition: all 0.3s ease; }
.btn-inverse:hover { background: rgba(255,255,255,0.2); border-color: rgba(255,255,255,0.4); color: white; }

.card { background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light); padding: 24px; transition: all 0.3s ease; display: flex; flex-direction: column; }
.shadow-hover:hover { transform: translateY(-4px); box-shadow: var(--shadow-hover); border-color: transparent; }

.section-tag { font-size: 13px; text-transform: uppercase; letter-spacing: 1.5px; color: var(--primary); font-weight: 700; margin-bottom: 8px; display: inline-block; }
.section-title { font-size: 36px; font-weight: 800; color: var(--text-main); margin-bottom: 12px; line-height: 1.2; }
.section-subtitle { font-size: 16px; color: var(--text-muted); max-width: 600px; line-height: 1.6; }

/* Navbar */
.navbar { position: sticky; top: 0; z-index: 100; background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(12px); border-bottom: 1px solid var(--border-light); }
.nav-content { max-width: 1400px; margin: 0 auto; padding: 16px 32px; display: flex; justify-content: space-between; align-items: center; }
.logo { display: flex; align-items: center; gap: 12px; }
.logo-icon { width: 44px; height: 44px; background: var(--primary-gradient); border-radius: 12px; color: white; display: flex; align-items: center; justify-content: center; font-size: 24px; box-shadow: 0 4px 10px rgba(2, 101, 220, 0.3); }
.logo-text strong { display: block; font-size: 18px; font-weight: 800; color: var(--secondary); letter-spacing: 0.5px; }
.logo-text span { font-size: 12px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px; }
.nav-links { display: flex; gap: 32px; }
.nav-links a { color: var(--text-main); font-weight: 600; cursor: pointer; transition: color 0.2s; font-size: 15px; }
.nav-links a:hover { color: var(--primary); }
.nav-actions { display: flex; gap: 12px; }

/* Main Content Wrapper */
.main-content { max-width: 1400px; margin: 0 auto; padding: 32px; display: flex; flex-direction: column; gap: 40px; }

/* Hero Section */
.hero-section { position: relative; background: var(--bg-surface); border-radius: 32px; padding: 60px; overflow: hidden; box-shadow: var(--shadow-sm); border: 1px solid var(--border-light); }
.hero-bg-shapes { position: absolute; inset: 0; pointer-events: none; overflow: hidden; border-radius: 32px; }
.shape-1 { position: absolute; top: -10%; left: -5%; width: 400px; height: 400px; background: radial-gradient(circle, rgba(74, 144, 226, 0.1) 0%, transparent 70%); border-radius: 50%; }
.shape-2 { position: absolute; bottom: -20%; right: -10%; width: 600px; height: 600px; background: radial-gradient(circle, rgba(2, 101, 220, 0.05) 0%, transparent 70%); border-radius: 50%; }

.hero-container { display: grid; grid-template-columns: 1fr 1fr; gap: 60px; align-items: center; position: relative; z-index: 2; }
.hero-badge { display: inline-flex; align-items: center; gap: 10px; background: #F1F5F9; padding: 8px 16px; border-radius: 100px; font-size: 14px; font-weight: 600; color: var(--primary); margin-bottom: 24px; cursor: pointer; transition: background 0.2s; }
.hero-badge:hover { background: #E2E8F0; }
.badge-pulse { width: 8px; height: 8px; background: #10B981; border-radius: 50%; box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); animation: pulse 2s infinite; }
.hero-title { font-size: 56px; font-weight: 800; line-height: 1.1; margin-bottom: 24px; color: var(--text-main); letter-spacing: -1px; }
.hero-subtitle { font-size: 18px; color: var(--text-muted); line-height: 1.7; margin-bottom: 32px; max-width: 85%; }
.hero-cta { display: flex; gap: 16px; margin-bottom: 48px; }
.btn-large { padding: 0 32px; height: 52px; font-size: 16px; font-weight: 600; }
.btn-outlined { border: 2px solid var(--border-light); color: var(--text-main); background: transparent; transition: all 0.3s ease; }
.btn-outlined:hover { border-color: var(--primary); color: var(--primary); background: rgba(2, 101, 220, 0.05); }

.hero-stats { display: flex; align-items: center; gap: 32px; background: white; padding: 24px 32px; border-radius: 20px; box-shadow: var(--shadow-md); border: 1px solid var(--border-light); display: inline-flex; }
.stat-item { display: flex; flex-direction: column; gap: 4px; }
.stat-num { font-size: 28px; font-weight: 800; color: var(--primary); line-height: 1; }
.stat-label { font-size: 13px; color: var(--text-muted); font-weight: 500; }
.stat-divider { width: 1px; height: 40px; background: var(--border-light); }

/* Hero Carousel */
.carousel-wrapper { position: relative; width: 100%; }
.carousel-card { border-radius: var(--radius-md); overflow: hidden; position: relative; height: 100%; cursor: pointer; display: flex; box-shadow: var(--shadow-sm); transition: transform 0.3s ease; }
.carousel-card:hover { transform: scale(1.02); box-shadow: var(--shadow-hover); }
.card-img { width: 100%; height: 100%; }
.card-fallback { width: 100%; height: 100%; background: var(--primary-gradient); display: flex; align-items: center; justify-content: center; font-size: 64px; color: white; font-weight: 800; opacity: 0.8; }
.card-overlay { position: absolute; inset: 0; background: linear-gradient(to top, rgba(10, 25, 48, 0.9) 0%, rgba(10, 25, 48, 0.2) 50%, transparent 100%); padding: 24px; display: flex; flex-direction: column; justify-content: flex-end; color: white; }
.card-badge { align-self: flex-start; background: rgba(255, 255, 255, 0.2); backdrop-filter: blur(4px); padding: 4px 12px; border-radius: 100px; font-size: 12px; font-weight: 600; margin-bottom: auto; }
.card-overlay h3 { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
.card-overlay p { font-size: 14px; color: rgba(255, 255, 255, 0.8); }

.carousel-info { margin-top: 24px; padding: 20px; background: rgba(2, 101, 220, 0.03); border-radius: var(--radius-md); border: 1px solid rgba(2, 101, 220, 0.1); }
.info-kicker { font-size: 12px; color: var(--primary); font-weight: 700; letter-spacing: 1px; margin-bottom: 4px; }
.info-title { font-size: 18px; font-weight: 800; color: var(--text-main); margin-bottom: 8px; }
.info-desc { font-size: 14px; color: var(--text-muted); line-height: 1.5; }

/* Overview Grid */
.overview-grid { display: grid; grid-template-columns: 1.4fr repeat(3, 1fr); gap: 24px; }
.glass-card { background: var(--bg-surface); border-radius: var(--radius-lg); padding: 32px; border: 1px solid var(--border-light); margin: 0; }
.hover-lift { transition: transform 0.3s ease, box-shadow 0.3s ease; }
.hover-lift:hover { transform: translateY(-8px); box-shadow: var(--shadow-hover); }
.overview-main { display: flex; flex-direction: column; justify-content: center; background: linear-gradient(145deg, #0A1930, #1C274C); color: white; border: none; }
.overview-main .section-tag { color: var(--accent-color); }
.overview-main h2 { color: white; margin-bottom: 16px; font-size: 32px; }
.overview-main p { color: rgba(255, 255, 255, 0.7); font-size: 16px; margin-bottom: 24px; line-height: 1.6; }
.contact-pill { display: inline-flex; align-items: center; gap: 8px; background: rgba(255, 255, 255, 0.1); padding: 10px 20px; border-radius: 100px; font-weight: 600; font-size: 15px; width: fit-content; }
.feature-card { display: flex; flex-direction: column; }
.feature-icon-wrapper { width: 56px; height: 56px; background: rgba(2, 101, 220, 0.08); border-radius: 16px; display: flex; align-items: center; justify-content: center; margin-bottom: 20px; }
.feature-icon { font-size: 28px; color: var(--primary); }
.feature-card h3 { font-size: 18px; font-weight: 700; margin-bottom: 12px; color: var(--text-main); }
.feature-card p { font-size: 14px; color: var(--text-muted); line-height: 1.6; margin-bottom: 24px; flex-grow: 1; }
.feature-link { color: var(--primary); font-weight: 600; font-size: 14px; display: flex; align-items: center; gap: 4px; cursor: pointer; margin-top: auto; transition: gap 0.2s; }
.feature-link:hover { gap: 8px; }

/* Shared Content Section */
.content-section { padding: 80px 40px; background: var(--bg-surface); border-radius: 32px; border: 1px solid var(--border-light); }
.bg-subtle { background: var(--bg-page); border: none; }
.section-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 40px; gap: 24px; }
.view-more-btn { font-weight: 600; }
.grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; }
.grid-2 { display: grid; grid-template-columns: repeat(2, 1fr); gap: 24px; }

/* Departments */
.dept-top { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.dept-icon-box { width: 48px; height: 48px; background: var(--bg-page); border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: var(--primary); }
.dept-title h3 { font-size: 18px; font-weight: 700; color: var(--text-main); margin-bottom: 4px; }
.dept-code { font-size: 12px; color: var(--text-muted); background: #F1F5F9; padding: 2px 8px; border-radius: 4px; }
.dept-desc { font-size: 14px; color: var(--text-muted); line-height: 1.6; margin-bottom: 20px; flex-grow: 1; }
.dept-meta { display: flex; flex-direction: column; gap: 8px; margin-bottom: 24px; padding-top: 16px; border-top: 1px dashed var(--border-light); }
.meta-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--text-muted); }
.btn-full { width: 100%; font-weight: 600; }

/* Doctors */
.doc-header { display: flex; align-items: flex-start; gap: 16px; margin-bottom: 20px; }
.doc-avatar { border: 2px solid white; box-shadow: var(--shadow-sm); background: #fff; }
.doc-titles { flex-grow: 1; display: flex; flex-direction: column; justify-content: center; min-height: 72px; }
.doc-titles h3 { font-size: 20px; font-weight: 800; margin-bottom: 6px; color: var(--text-main); }
.doc-role { font-size: 14px; color: var(--text-muted); font-weight: 500; }
.doc-tags { display: flex; flex-direction: column; gap: 8px; align-items: flex-end; }
.doc-body { flex-grow: 1; background: var(--bg-page); padding: 16px; border-radius: var(--radius-md); margin-bottom: 24px; display: flex; flex-direction: column; gap: 12px; }
.doc-field { display: flex; gap: 8px; }
.field-label { font-size: 13px; font-weight: 700; color: var(--text-main); min-width: 45px; }
.doc-field p { font-size: 13px; color: var(--text-muted); line-height: 1.5; margin: 0; }
.doc-actions { display: flex; gap: 12px; }

/* Cases */
.grid-cases { display: grid; grid-template-columns: repeat(2, 1fr); gap: 24px; }
.case-card { padding: 0; overflow: hidden; display: flex; flex-direction: column; }
.case-large { grid-column: span 2; flex-direction: row; }
.case-img-wrap { position: relative; height: 220px; }
.case-large .case-img-wrap { width: 50%; height: auto; }
.case-cover { width: 100%; height: 100%; }
.case-badge { position: absolute; top: 16px; left: 16px; background: rgba(0, 0, 0, 0.6); backdrop-filter: blur(4px); color: white; padding: 6px 12px; border-radius: 100px; font-size: 12px; font-weight: 600; }
.case-content { padding: 24px; flex-grow: 1; display: flex; flex-direction: column; }
.case-large .case-content { width: 50%; padding: 40px; justify-content: center; }
.case-content h3 { font-size: 20px; font-weight: 700; margin-bottom: 12px; color: var(--text-main); line-height: 1.4; }
.case-large .case-content h3 { font-size: 28px; margin-bottom: 16px; }
.case-summary { font-size: 14px; color: var(--text-muted); line-height: 1.6; margin-bottom: 20px; }
.case-large .case-summary { font-size: 16px; margin-bottom: 24px; }
.case-tags-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 24px; flex-grow: 1; align-items: flex-start; }
.case-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 20px; border-top: 1px solid var(--border-light); }
.case-doc { font-size: 14px; font-weight: 600; color: var(--text-main); display: flex; align-items: center; gap: 6px; }

/* Process */
.process-section { padding: 80px 40px; background: linear-gradient(145deg, #0A1930, #1C274C); border-radius: 32px; text-align: center; color: white; }
.process-header { margin-bottom: 60px; }
.text-light { color: var(--accent-color); }
.text-white { color: white; }
.text-light-muted { color: rgba(255, 255, 255, 0.7); }
.grid-4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; }
.process-step { display: flex; flex-direction: column; align-items: center; text-align: center; position: relative; }
.process-step:not(:last-child)::after { content: ''; position: absolute; top: 32px; right: -20%; width: 40%; height: 2px; background: dashed rgba(255, 255, 255, 0.2); }
.step-num { font-size: 48px; font-weight: 900; color: rgba(255, 255, 255, 0.1); margin-bottom: -20px; z-index: 1; }
.step-card { background: rgba(255, 255, 255, 0.05); backdrop-filter: blur(10px); border: 1px solid rgba(255, 255, 255, 0.1); padding: 32px 24px; border-radius: var(--radius-lg); z-index: 2; height: 100%; display: flex; flex-direction: column; transition: transform 0.3s; width: 100%; }
.step-card:hover { transform: translateY(-5px); background: rgba(255, 255, 255, 0.08); }
.step-card h3 { font-size: 18px; font-weight: 700; margin-bottom: 12px; color: white; }
.step-card p { font-size: 14px; color: rgba(255, 255, 255, 0.7); line-height: 1.6; margin-bottom: 24px; flex-grow: 1; }
.step-action { font-size: 14px; font-weight: 600; color: var(--accent-color); display: flex; align-items: center; justify-content: center; gap: 4px; cursor: pointer; }

/* CTA Banner */
.cta-banner { position: relative; padding: 60px 40px; border-radius: 32px; display: flex; justify-content: space-between; align-items: center; overflow: hidden; }
.cta-banner-bg { position: absolute; inset: 0; background: var(--primary-gradient); z-index: 1; }
.cta-banner-bg::after { content: ''; position: absolute; width: 400px; height: 400px; background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, transparent 60%); top: -200px; right: 100px; border-radius: 50%; }
.cta-content { position: relative; z-index: 2; color: white; max-width: 600px; }
.cta-content h2 { font-size: 32px; font-weight: 800; margin-bottom: 16px; margin-top: 8px; }
.cta-content p { font-size: 16px; color: rgba(255, 255, 255, 0.9); line-height: 1.6; }
.cta-actions { position: relative; z-index: 2; display: flex; gap: 16px; }

/* Floating Button */
.fab-btn { position: fixed; right: 32px; bottom: 32px; width: 64px; height: 64px; border-radius: 32px; background: var(--primary-gradient); color: white; border: none; font-size: 28px; display: flex; align-items: center; justify-content: center; cursor: pointer; box-shadow: 0 10px 25px rgba(2, 101, 220, 0.4); z-index: 999; transition: transform 0.3s; }
.fab-btn:hover { transform: scale(1.05) translateY(-5px); }

/* Dialog */
.dialog-case-detail { display: flex; flex-direction: column; gap: 20px; }
.dialog-img { width: 100%; height: 300px; border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.dialog-meta { display: flex; gap: 12px; }
.dialog-summary { font-size: 18px; font-weight: 600; color: var(--text-main); line-height: 1.5; }
.dialog-content { background: var(--bg-page); padding: 20px; border-radius: var(--radius-md); font-size: 14px; color: var(--text-muted); line-height: 1.6; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; }

/* Animations */
@keyframes pulse { 0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); } 70% { box-shadow: 0 0 0 10px rgba(16, 185, 129, 0); } 100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); } }

/* Responsive */
@media (max-width: 1200px) {
  .hero-container { grid-template-columns: 1fr; text-align: center; gap: 40px; }
  .hero-stats, .hero-cta { justify-content: center; }
  .hero-badge { margin: 0 auto 24px; }
  .overview-grid { grid-template-columns: 1fr 1fr; }
  .overview-main { grid-column: span 2; text-align: center; align-items: center; }
  .grid-3, .grid-4 { grid-template-columns: repeat(2, 1fr); }
  .process-step:not(:last-child)::after { display: none; }
  .case-large { grid-column: auto; flex-direction: column; }
  .case-large .case-img-wrap, .case-large .case-content { width: 100%; }
  .cta-banner { flex-direction: column; text-align: center; gap: 32px; }
}

@media (max-width: 768px) {
  .nav-links { display: none; }
  .main-content { padding: 16px; gap: 32px; }
  .hero-section, .content-section, .process-section { padding: 40px 24px; border-radius: 24px; }
  .hero-title { font-size: 40px; }
  .hero-stats { flex-direction: column; gap: 16px; width: 100%; }
  .stat-divider { width: 100%; height: 1px; }
  .grid-2, .grid-3, .grid-4, .grid-cases { grid-template-columns: 1fr; }
  .overview-grid { grid-template-columns: 1fr; }
  .overview-main { grid-column: auto; }
  .section-header { flex-direction: column; align-items: flex-start; gap: 16px; }
}
</style>
