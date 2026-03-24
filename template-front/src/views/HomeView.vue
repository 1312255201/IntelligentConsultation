<template>
  <div class="home-page">
    <header class="topbar">
      <div class="brand">
        <div class="brand-mark">IC</div>
        <div>
          <strong>智能问诊系统</strong>
          <span>Intelligent Consultation</span>
        </div>
      </div>
      <nav class="nav">
        <button type="button" @click="scrollTo('departments')">科室服务</button>
        <button type="button" @click="scrollTo('doctors')">推荐医生</button>
        <button type="button" @click="scrollTo('cases')">经典案例</button>
        <button type="button" @click="scrollTo('process')">服务流程</button>
      </nav>
      <div class="topbar-actions">
        <el-button plain @click="goLoginPage">
          {{ unauthorized() ? '登录/注册' : '我的服务' }}
        </el-button>
        <el-button type="primary" @click="goConsultEntry">在线咨询</el-button>
      </div>
    </header>

    <main class="page-main">
      <section class="hero">
        <div class="hero-copy">
          <span class="eyebrow">Smart Medical Portal</span>
          <button class="notice" type="button" @click="goConsultEntry">
            <span>{{ landing.config.noticeText }}</span>
            <el-icon><ArrowRight /></el-icon>
          </button>
          <h1>{{ landing.config.heroTitle }}</h1>
          <p>{{ landing.config.heroSubtitle }}</p>
          <div class="hero-actions">
            <el-button type="primary" size="large" @click="goConsultEntry">立即咨询</el-button>
            <el-button size="large" @click="scrollTo('doctors')">查看推荐医生</el-button>
            <el-button size="large" text @click="scrollTo('cases')">浏览经典案例</el-button>
          </div>
          <div class="hero-tags">
            <span>在线咨询</span>
            <span>推荐专家</span>
            <span>经典案例</span>
            <span>后续智能分诊</span>
          </div>
          <div class="hero-metrics">
            <article><strong>{{ displayDepartments.length }}</strong><span>覆盖科室</span></article>
            <article><strong>{{ displayRecommendDoctors.length }}</strong><span>推荐医生</span></article>
            <article><strong>{{ displayCases.length }}</strong><span>精选案例</span></article>
          </div>
        </div>

        <div class="hero-stage">
          <div class="stage-head">
            <div>
              <span class="eyebrow stage-eyebrow">Featured Stories</span>
              <h2>精选内容推荐</h2>
            </div>
            <div class="stage-controls">
              <button type="button" @click="prevCarousel"><el-icon><ArrowLeft /></el-icon></button>
              <button type="button" @click="nextCarousel"><el-icon><ArrowRight /></el-icon></button>
            </div>
          </div>

          <div class="stage-scene" @mouseenter="pauseCarousel" @mouseleave="resumeCarousel">
            <div class="orb orb-a"></div>
            <div class="orb orb-b"></div>
            <div class="ring ring-a"></div>
            <div class="ring ring-b"></div>
            <div class="stage-floor"></div>
            <div class="stage-grid"></div>
            <div class="carousel">
              <article
                v-for="(slide, index) in carouselSlides"
                :key="slide.id"
                :class="['scene-card', slide.type, { active: index === carouselIndex }]"
                :style="slideStyle(index)"
              >
                <div class="scene-card-shell">
                  <div class="scene-media">
                    <img v-if="slide.image" :src="slide.image" :alt="slide.name" />
                    <div v-else class="scene-fallback">{{ slide.fallback }}</div>
                    <span class="scene-badge">{{ slide.badge }}</span>
                  </div>
                  <div class="scene-body">
                    <span class="scene-kicker">{{ slide.category }}</span>
                    <h3>{{ slide.name }}</h3>
                    <strong>{{ slide.title }}</strong>
                    <p>{{ slide.summary }}</p>
                    <div class="scene-tags">
                      <span v-for="tag in slide.tags" :key="tag">{{ tag }}</span>
                    </div>
                    <el-button type="primary" @click="handleSlideAction(slide)">{{ slide.actionText }}</el-button>
                  </div>
                </div>
              </article>
            </div>
          </div>

          <div class="stage-footer">
            <div>
              <span class="eyebrow stage-eyebrow">{{ activeSlide?.category || '精选内容' }}</span>
              <strong>{{ activeSlide?.name || '智能问诊系统' }}</strong>
              <p>{{ activeSlide?.summary || '从医生专长到真实案例，这里会持续呈现值得优先了解的服务内容。' }}</p>
            </div>
            <div class="stage-indicators">
              <div class="dots">
                <button
                  v-for="(slide, index) in carouselSlides"
                  :key="slide.id"
                  type="button"
                  :class="{ active: index === carouselIndex }"
                  @click="goToSlide(index)"
                ></button>
              </div>
              <div class="stage-status">
                <span>{{ String(carouselIndex + 1).padStart(2, '0') }}</span>
                <div class="stage-progress">
                  <i :style="{ width: `${((carouselIndex + 1) / carouselSlides.length) * 100}%` }"></i>
                </div>
                <small>{{ carouselSlides.length }} 项精选</small>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="intro-grid">
        <article class="intro-card intro-main">
          <span class="eyebrow">Overview</span>
          <h2>{{ landing.config.introTitle }}</h2>
          <p>{{ landing.config.introContent }}</p>
          <div v-if="landing.config.servicePhone" class="service-phone">
            <el-icon><Phone /></el-icon>
            <span>{{ landing.config.servicePhone }}</span>
          </div>
        </article>
        <article v-for="item in introCards" :key="item.title" class="intro-card">
          <el-icon class="intro-icon"><component :is="item.icon" /></el-icon>
          <strong>{{ item.title }}</strong>
          <p>{{ item.description }}</p>
          <button type="button" @click="item.action">{{ item.actionText }}</button>
        </article>
      </section>

      <section id="departments" class="section">
        <div class="section-head">
          <div>
            <span class="eyebrow">Departments</span>
            <h2>重点服务科室</h2>
            <p>围绕常见问诊需求与健康管理场景，先看看我们当前覆盖的重点服务方向。</p>
          </div>
          <el-button text @click="goConsultEntry">
            开始咨询
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div v-if="displayDepartments.length" class="card-grid three">
          <article v-for="item in displayDepartments" :key="item.id" class="info-card">
            <div class="card-top">
              <div>
                <strong>{{ item.name }}</strong>
                <span>{{ item.code }}</span>
              </div>
              <el-icon><OfficeBuilding /></el-icon>
            </div>
            <p>{{ item.description || '提供在线沟通、就诊建议与后续健康管理支持。' }}</p>
            <div class="meta-row">
              <span v-if="item.location"><el-icon><Location /></el-icon>{{ item.location }}</span>
              <span v-if="item.phone"><el-icon><Phone /></el-icon>{{ item.phone }}</span>
            </div>
            <el-button plain @click="goConsultEntry">选择该科室咨询</el-button>
          </article>
        </div>
        <el-empty v-else description="科室信息整理中，您可以先发起在线咨询">
          <el-button type="primary" @click="goConsultEntry">立即咨询</el-button>
        </el-empty>
      </section>

      <section class="featured">
        <div id="doctors" class="section">
          <div class="section-head">
            <div>
              <span class="eyebrow">Doctors</span>
              <h2>推荐医生</h2>
            </div>
            <el-button text @click="goConsultEntry">
              预约咨询
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div v-if="displayRecommendDoctors.length" class="card-grid two">
            <article v-for="item in displayRecommendDoctors" :key="item.id" class="info-card doctor-card">
              <div class="doctor-head">
                <el-avatar :size="68" :src="resolveImagePath(item.photo) || undefined">
                  {{ item.name?.slice(0, 1) || 'D' }}
                </el-avatar>
                <div>
                  <strong>{{ item.name }}</strong>
                  <span>{{ item.title || item.displayTitle || '推荐医生' }}</span>
                </div>
              </div>
              <div class="chip-row">
                <span>{{ item.departmentName }}</span>
                <span v-if="item.displayTitle">{{ item.displayTitle }}</span>
              </div>
              <p>{{ item.recommendReason || item.introduction || '擅长多场景诊疗与健康管理服务。' }}</p>
              <p>{{ item.expertise || '支持在线问诊、复诊指导与健康建议。' }}</p>
              <div class="action-row">
                <el-button type="primary" @click="goConsultEntry">预约咨询</el-button>
                <el-button @click="scrollTo('cases')">查看案例</el-button>
              </div>
            </article>
          </div>
          <el-empty v-else description="医生团队信息整理中，欢迎先提交咨询需求">
            <el-button type="primary" @click="goConsultEntry">开始咨询</el-button>
          </el-empty>
        </div>

        <div id="cases" class="section">
          <div class="section-head">
            <div>
              <span class="eyebrow">Cases</span>
              <h2>经典案例</h2>
            </div>
            <el-button text @click="goConsultEntry">
              获取方案
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div v-if="displayCases.length" class="card-grid two">
            <article
              v-for="(item, index) in displayCases"
              :key="item.id"
              :class="['case-card', { large: index === 0 }]"
            >
              <el-image
                class="case-image"
                :src="resolveImagePath(item.cover)"
                fit="cover"
                preview-teleported
                :preview-src-list="[resolveImagePath(item.cover)]"
              />
              <div class="case-body">
                <div class="chip-row">
                  <span>{{ item.departmentName }}</span>
                  <span v-if="item.doctorName">{{ item.doctorName }}</span>
                </div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.summary }}</p>
                <div class="chip-row">
                  <span v-for="tag in parseTags(item.tags)" :key="tag">{{ tag }}</span>
                </div>
                <div class="action-row">
                  <el-button link type="primary" @click="openCaseDetail(item)">查看详情</el-button>
                  <el-button type="primary" @click="goConsultEntry">获取方案</el-button>
                </div>
              </div>
            </article>
          </div>
          <el-empty v-else description="案例内容持续更新中，您也可以直接发起咨询">
            <el-button type="primary" @click="goConsultEntry">发起咨询</el-button>
          </el-empty>
        </div>
      </section>

      <section id="process" class="section process">
        <div class="section-head">
          <div>
            <span class="eyebrow">Process</span>
            <h2>服务流程</h2>
            <p>从了解服务到提交需求，再到获得建议与后续跟进，整个过程清晰直接。</p>
          </div>
        </div>
        <div class="card-grid four">
          <article v-for="(item, index) in processSteps" :key="item.title" class="process-card">
            <span class="process-index">0{{ index + 1 }}</span>
            <strong>{{ item.title }}</strong>
            <p>{{ item.description }}</p>
            <button type="button" @click="goConsultEntry">{{ item.action }}</button>
          </article>
        </div>
      </section>

      <section class="cta-banner">
        <div>
          <span class="eyebrow">Start Now</span>
          <h2>已经找到感兴趣的医生或案例？现在就开始您的咨询</h2>
          <p>您可以继续浏览服务内容，也可以直接提交咨询需求，让合适的医生更快了解您的情况。</p>
        </div>
        <div class="action-row">
          <el-button type="primary" size="large" @click="goConsultEntry">立即咨询</el-button>
          <el-button size="large" @click="goLoginPage">{{ unauthorized() ? '登录/注册' : '我的服务' }}</el-button>
        </div>
      </section>
    </main>

    <button class="floating-consult" type="button" @click="goConsultEntry">立即咨询</button>

    <el-dialog
      v-model="caseDialogVisible"
      width="720px"
      :title="activeCase?.title || '经典案例'"
      destroy-on-close
    >
      <div v-if="activeCase" class="case-detail">
        <el-image class="case-detail-image" :src="resolveImagePath(activeCase.cover)" fit="cover" />
        <div class="chip-row">
          <span>{{ activeCase.departmentName }}</span>
          <span v-if="activeCase.doctorName">{{ activeCase.doctorName }}</span>
          <span v-if="activeCase.doctorTitle">{{ activeCase.doctorTitle }}</span>
        </div>
        <p class="case-detail-summary">{{ activeCase.summary }}</p>
        <p>{{ activeCase.detail || '如需进一步了解适用人群、诊疗思路与后续建议，可继续发起咨询。' }}</p>
        <div class="action-row">
          <el-button @click="caseDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="goConsultEntry">继续咨询</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import {
  ArrowLeft,
  ArrowRight,
  DataAnalysis,
  Location,
  OfficeBuilding,
  Phone,
  Service,
  UserFilled
} from '@element-plus/icons-vue'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getPublic, resolveHomeRouteByRole, resolveImagePath, unauthorized } from '@/net'

const router = useRouter()
const caseDialogVisible = ref(false)
const activeCase = ref(null)
const carouselIndex = ref(0)
let carouselTimer = null

const landing = reactive(createDefaultLanding())

const introCards = [
  { title: '立即咨询', description: '在线描述您的问题与困扰，更快获得初步服务建议。', actionText: '开始咨询', action: () => goConsultEntry(), icon: Service },
  { title: '推荐医生', description: '查看医生专长与服务方向，先找到更适合自己的医生。', actionText: '查看医生', action: () => scrollTo('doctors'), icon: UserFilled },
  { title: '案例参考', description: '通过真实案例了解诊疗思路、服务重点与适用场景。', actionText: '浏览案例', action: () => scrollTo('cases'), icon: DataAnalysis }
]

const processSteps = [
  { title: '了解服务方向', description: '先浏览科室、医生与案例，初步判断是否匹配您的需求。', action: '查看内容' },
  { title: '提交咨询需求', description: '描述症状、困扰或想重点了解的问题，帮助我们更快理解您的情况。', action: '发起咨询' },
  { title: '获取专业建议', description: '结合医生专长与服务内容，为您提供更合适的初步方向建议。', action: '获取建议' },
  { title: '持续跟进管理', description: '根据实际需要继续复诊、随访与健康管理服务。', action: '立即开始' }
]

const displayDepartments = computed(() => landing.departments.slice(0, 6))
const displayRecommendDoctors = computed(() => landing.recommendDoctors.slice(0, 4))
const displayCases = computed(() => landing.cases.slice(0, 4))

const carouselSlides = computed(() => {
  const doctors = landing.recommendDoctors.slice(0, 4).map(item => ({
    id: `doctor-${item.id}`,
    sourceId: item.id,
    type: 'doctor',
    category: '推荐医生',
    badge: item.departmentName || 'Doctor',
    name: item.name,
    title: item.displayTitle || item.title || '推荐医生',
    summary: item.recommendReason || item.expertise || item.introduction || '擅长多场景诊疗与健康管理服务。',
    image: resolveImagePath(item.photo),
    tags: compactTags([item.departmentName, item.title, item.displayTitle]),
    actionText: '预约咨询',
    fallback: (item.name || 'D').slice(0, 1)
  }))
  const cases = landing.cases.slice(0, 3).map(item => ({
    id: `case-${item.id}`,
    sourceId: item.id,
    type: 'case',
    category: '经典案例',
    badge: item.departmentName || 'Case',
    name: item.title,
    title: item.doctorName ? `${item.doctorName} · ${item.doctorTitle || '方案参考'}` : item.departmentName,
    summary: item.summary || '可进一步了解诊疗思路、服务重点与后续建议。',
    image: resolveImagePath(item.cover),
    tags: compactTags([item.departmentName, item.doctorName, ...parseTags(item.tags)]),
    actionText: '查看详情',
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
    category: '服务亮点',
    badge: 'Medical',
    name: '智能问诊系统',
    title: '在线健康咨询',
    summary: '聚焦常见咨询需求、医生推荐与案例参考，帮助您更快找到合适方向。',
    image: '',
    tags: ['在线问诊', '推荐医生', '案例展示'],
    actionText: '立即咨询',
    fallback: 'IC'
  }]
})

const activeSlide = computed(() => carouselSlides.value[carouselIndex.value] || null)

function createDefaultLanding() {
  return {
    config: {
      heroTitle: '智能问诊系统',
      heroSubtitle: '提供专业、可靠、便捷的在线问诊与健康管理服务。',
      noticeText: '精选医生与真实案例持续更新，欢迎先了解服务再发起咨询。',
      introTitle: '平台简介',
      introContent: '围绕在线咨询、推荐医生、经典案例与健康管理服务，提供更清晰、更便捷的健康咨询体验。',
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
  unauthorized() ? router.push('/login') : router.push(resolveHomeRouteByRole())
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

function normalizeOffset(index) {
  const total = carouselSlides.value.length
  if (!total) return 0
  let offset = index - carouselIndex.value
  const half = Math.floor(total / 2)
  if (offset > half) offset -= total
  if (offset < -half) offset += total
  return offset
}

function slideStyle(index) {
  const offset = normalizeOffset(index)
  const distance = Math.abs(offset)
  return {
    transform: `translate(-50%, -50%) translateX(${offset * 28}%) translateZ(${Math.max(0, 250 - distance * 72)}px) rotateY(${offset * -34}deg) scale(${Math.max(0.72, 1 - distance * 0.11)})`,
    zIndex: String(100 - distance),
    opacity: distance > 3 ? 0 : 1 - distance * 0.18
  }
}

function nextCarousel() {
  if (carouselSlides.value.length <= 1) return
  carouselIndex.value = (carouselIndex.value + 1) % carouselSlides.value.length
}

function prevCarousel() {
  if (carouselSlides.value.length <= 1) return
  carouselIndex.value = (carouselIndex.value - 1 + carouselSlides.value.length) % carouselSlides.value.length
}

function goToSlide(index) {
  carouselIndex.value = index
}

function pauseCarousel() {
  if (carouselTimer) {
    window.clearInterval(carouselTimer)
    carouselTimer = null
  }
}

function resumeCarousel() {
  pauseCarousel()
  if (carouselSlides.value.length <= 1) return
  carouselTimer = window.setInterval(() => nextCarousel(), 4200)
}

watch(() => carouselSlides.value.length, (length) => {
  if (!length) {
    carouselIndex.value = 0
    pauseCarousel()
    return
  }
  if (carouselIndex.value >= length) carouselIndex.value = 0
  resumeCarousel()
}, { immediate: true })

onMounted(() => loadLanding())
onBeforeUnmount(() => pauseCarousel())
</script>

<style scoped>
.home-page { min-height: 100vh; padding: 20px; color: #173740; }
.page-main { display: flex; flex-direction: column; gap: 20px; }
.topbar, .hero, .intro-grid, .section, .featured, .cta-banner { border: 1px solid rgba(23, 55, 64, 0.08); border-radius: 32px; box-shadow: 0 24px 70px rgba(12, 52, 60, 0.12); }
.topbar { position: sticky; top: 20px; z-index: 30; display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 18px 24px; margin-bottom: 20px; background: rgba(255, 255, 255, 0.82); backdrop-filter: blur(20px); }
.brand, .doctor-head, .card-top { display: flex; align-items: center; gap: 14px; }
.brand-mark { width: 54px; height: 54px; display: grid; place-items: center; border-radius: 20px; background: linear-gradient(135deg, #78dcc9, #ffc77e); color: #0c4148; font-weight: 800; letter-spacing: 0.08em; box-shadow: 0 18px 34px rgba(15, 102, 101, 0.2); }
.brand strong, .brand span { display: block; }
.brand span { margin-top: 4px; font-size: 12px; letter-spacing: 0.12em; text-transform: uppercase; color: #708890; }
.nav { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.nav button, .intro-card button, .process-card button { border: none; background: transparent; cursor: pointer; }
.nav button { padding: 10px 14px; border-radius: 999px; color: #23454d; transition: background-color 0.25s ease; }
.nav button:hover { background: rgba(15, 102, 101, 0.08); }
.topbar-actions, .hero-actions, .action-row, .stage-controls { display: flex; gap: 12px; flex-wrap: wrap; }
.eyebrow { display: inline-flex; font-size: 12px; letter-spacing: 0.2em; text-transform: uppercase; }

.hero { position: relative; overflow: hidden; padding: 30px; display: grid; grid-template-columns: minmax(0, 0.95fr) minmax(360px, 1.05fr); gap: 28px; align-items: center; background: linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(245, 251, 251, 0.76)); }
.hero::before, .hero::after { content: ''; position: absolute; border-radius: 999px; filter: blur(16px); opacity: 0.75; }
.hero::before { width: 260px; height: 260px; left: -40px; top: -70px; background: radial-gradient(circle, rgba(120, 220, 201, 0.46), transparent 65%); animation: float-orb 12s ease-in-out infinite; }
.hero::after { width: 340px; height: 340px; right: -110px; bottom: -100px; background: radial-gradient(circle, rgba(255, 194, 124, 0.34), transparent 70%); animation: float-orb 15s ease-in-out infinite reverse; }
.hero-copy { position: relative; z-index: 1; }
.hero-copy .eyebrow { padding: 8px 14px; border-radius: 999px; background: rgba(15, 102, 101, 0.08); color: #0f6665; }
.notice { width: fit-content; margin-top: 18px; display: inline-flex; align-items: center; gap: 8px; padding: 10px 16px; border: none; border-radius: 999px; background: rgba(255, 255, 255, 0.74); color: #245158; box-shadow: inset 0 0 0 1px rgba(15, 102, 101, 0.08); cursor: pointer; }
.hero-copy h1 { margin: 24px 0 18px; font-size: clamp(48px, 5vw, 80px); line-height: 0.94; letter-spacing: -0.04em; }
.hero-copy p { max-width: 620px; margin: 0; color: #617c85; font-size: 17px; line-height: 1.82; }
.hero-tags, .chip-row, .meta-row, .scene-tags, .dots { display: flex; flex-wrap: wrap; gap: 10px; }
.hero-tags { margin-top: 24px; }
.hero-tags span, .chip-row span { padding: 7px 12px; border-radius: 999px; font-size: 12px; background: rgba(255, 255, 255, 0.72); color: #55717b; border: 1px solid rgba(23, 55, 64, 0.06); }
.hero-metrics { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; margin-top: 28px; }
.hero-metrics article { padding: 18px 20px; border-radius: 24px; background: rgba(255, 255, 255, 0.68); border: 1px solid rgba(23, 55, 64, 0.05); }
.hero-metrics strong { display: block; font-size: 32px; line-height: 1; }
.hero-metrics span { display: block; margin-top: 8px; color: #6b858e; }

.hero-stage { position: relative; padding: 24px; border-radius: 30px; background: linear-gradient(160deg, rgba(12, 53, 61, 0.98), rgba(18, 75, 84, 0.95)); color: #f2fbfc; overflow: hidden; }
.stage-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.stage-head h2 { margin: 10px 0 0; font-size: 34px; }
.stage-eyebrow { color: rgba(242, 251, 252, 0.68); }
.stage-controls button { width: 44px; height: 44px; display: grid; place-items: center; border: none; border-radius: 16px; background: rgba(255, 255, 255, 0.08); color: #f2fbfc; cursor: pointer; transition: transform 0.25s ease, background-color 0.25s ease; }
.stage-controls button:hover { transform: translateY(-2px); background: rgba(255, 255, 255, 0.16); }
.stage-scene { position: relative; height: 470px; margin-top: 18px; perspective: 1800px; transform-style: preserve-3d; isolation: isolate; }
.orb, .ring { position: absolute; inset: 0; margin: auto; pointer-events: none; }
.orb { border-radius: 999px; filter: blur(24px); }
.orb-a { width: 220px; height: 220px; left: -8%; top: 15%; background: radial-gradient(circle, rgba(118, 214, 198, 0.34), transparent 68%); }
.orb-b { width: 240px; height: 240px; right: -10%; bottom: 2%; background: radial-gradient(circle, rgba(255, 189, 112, 0.34), transparent 70%); }
.ring { border-radius: 999px; border: 1px solid rgba(255, 255, 255, 0.08); }
.ring-a { width: 82%; height: 70%; animation: spin-ring 18s linear infinite; }
.ring-b { width: 58%; height: 46%; animation: spin-ring 12s linear infinite reverse; }
.stage-floor { position: absolute; left: 12%; right: 12%; bottom: 38px; height: 110px; border-radius: 999px; background: radial-gradient(circle, rgba(81, 219, 193, 0.34), rgba(81, 219, 193, 0.08) 35%, transparent 72%); transform: rotateX(82deg) translateZ(-80px); filter: blur(10px); opacity: 0.82; animation: pulse-floor 6s ease-in-out infinite; pointer-events: none; }
.stage-grid { position: absolute; inset: 12% 10% 12%; border-radius: 38px; background-image:
  linear-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
  linear-gradient(90deg, rgba(255, 255, 255, 0.08) 1px, transparent 1px);
  background-size: 46px 46px;
  -webkit-mask-image: linear-gradient(to bottom, transparent, rgba(0, 0, 0, 0.85) 26%, rgba(0, 0, 0, 0.12));
  mask-image: linear-gradient(to bottom, transparent, rgba(0, 0, 0, 0.85) 26%, rgba(0, 0, 0, 0.12));
  transform: rotateX(72deg) translateZ(-140px);
  opacity: 0.28;
  pointer-events: none;
}
.carousel { position: relative; width: 100%; height: 100%; transform-style: preserve-3d; }
.scene-card { position: absolute; top: 50%; left: 50%; width: min(360px, 78%); overflow: hidden; border-radius: 30px; background: rgba(255, 255, 255, 0.14); box-shadow: 0 26px 60px rgba(0, 0, 0, 0.2); backdrop-filter: blur(20px); transition: transform 0.85s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.85s ease, filter 0.45s ease; pointer-events: none; filter: saturate(0.82); }
.scene-card.active { pointer-events: auto; filter: saturate(1.08); }
.scene-card.doctor { background: linear-gradient(180deg, rgba(12, 70, 80, 0.72), rgba(13, 40, 47, 0.88)); }
.scene-card.case { background: linear-gradient(180deg, rgba(94, 50, 16, 0.46), rgba(18, 44, 49, 0.9)); }
.scene-card-shell { position: relative; height: 100%; border-radius: inherit; overflow: hidden; animation: float-card 7s ease-in-out infinite; }
.scene-card-shell::before { content: ''; position: absolute; inset: 0; background: linear-gradient(135deg, rgba(255, 255, 255, 0.22), transparent 26%, transparent 68%, rgba(255, 255, 255, 0.12)); opacity: 0.72; pointer-events: none; z-index: 1; }
.scene-card.active .scene-card-shell { animation-duration: 5.2s; box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12), 0 0 48px rgba(76, 210, 190, 0.16); }
.scene-media { position: relative; height: 220px; overflow: hidden; }
.scene-media::after { content: ''; position: absolute; inset: auto -16% -10% -16%; height: 58%; background: linear-gradient(to top, rgba(5, 19, 22, 0.48), transparent); pointer-events: none; }
.scene-media img, .case-image, .case-detail-image { width: 100%; height: 100%; object-fit: cover; }
.scene-media img { transition: transform 0.85s ease; }
.scene-card.active .scene-media img { transform: scale(1.06); }
.scene-fallback { width: 100%; height: 100%; display: grid; place-items: center; background: linear-gradient(145deg, rgba(18, 87, 99, 0.94), rgba(10, 39, 45, 0.98)); font-size: 52px; font-weight: 800; letter-spacing: 0.06em; }
.scene-badge { position: absolute; top: 16px; left: 16px; padding: 8px 12px; border-radius: 999px; background: rgba(6, 24, 28, 0.48); color: rgba(242, 251, 252, 0.86); font-size: 12px; letter-spacing: 0.08em; text-transform: uppercase; }
.scene-body { position: relative; z-index: 2; padding: 20px 20px 22px; }
.scene-kicker { color: rgba(242, 251, 252, 0.66); font-size: 12px; letter-spacing: 0.18em; text-transform: uppercase; }
.scene-body h3 { margin: 14px 0 8px; font-size: 30px; line-height: 1.02; }
.scene-body strong { display: block; color: rgba(242, 251, 252, 0.88); font-size: 15px; }
.scene-body p { margin: 12px 0 0; color: rgba(242, 251, 252, 0.76); line-height: 1.72; }
.scene-tags { margin: 16px 0 18px; }
.scene-tags span { padding: 7px 12px; border-radius: 999px; background: rgba(255, 255, 255, 0.1); color: rgba(242, 251, 252, 0.82); font-size: 12px; }
.stage-footer { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-top: 18px; }
.stage-footer strong { display: block; margin-top: 10px; font-size: 22px; }
.stage-footer p { max-width: 420px; margin: 10px 0 0; color: rgba(242, 251, 252, 0.72); line-height: 1.72; }
.stage-indicators { display: flex; flex-direction: column; align-items: flex-end; gap: 14px; min-width: 190px; }
.dots button { width: 10px; height: 10px; border: none; border-radius: 999px; background: rgba(255, 255, 255, 0.24); cursor: pointer; }
.dots button.active { transform: scale(1.35); background: #ffd18a; }
.stage-status { display: flex; align-items: center; gap: 12px; color: rgba(242, 251, 252, 0.68); text-transform: uppercase; letter-spacing: 0.16em; }
.stage-status span { font-size: 18px; font-weight: 700; color: #f7fcfd; letter-spacing: 0.12em; }
.stage-progress { width: 124px; height: 4px; border-radius: 999px; background: rgba(255, 255, 255, 0.16); overflow: hidden; }
.stage-progress i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #72dcc8, #ffd18a); box-shadow: 0 0 18px rgba(255, 209, 138, 0.4); transition: width 0.55s ease; }
.stage-status small { font-size: 10px; color: rgba(242, 251, 252, 0.58); letter-spacing: 0.16em; }
.intro-grid { display: grid; grid-template-columns: minmax(0, 1.2fr) repeat(3, minmax(0, 0.8fr)); gap: 16px; padding: 18px; background: rgba(255, 255, 255, 0.78); }
.intro-card { min-height: 220px; padding: 22px; border-radius: 28px; background: rgba(255, 255, 255, 0.9); border: 1px solid rgba(23, 55, 64, 0.06); }
.intro-main { background: linear-gradient(135deg, #113f47, #185861); color: #f2fbfb; }
.intro-main h2 { margin: 14px 0; font-size: 32px; }
.intro-main p { margin: 0; color: rgba(242, 251, 252, 0.78); line-height: 1.82; }
.service-phone { width: fit-content; margin-top: 18px; display: inline-flex; align-items: center; gap: 10px; padding: 10px 14px; border-radius: 999px; background: rgba(255, 255, 255, 0.1); }
.intro-icon { font-size: 24px; color: #0f6665; }
.intro-card strong { display: block; margin: 16px 0 10px; font-size: 20px; }
.intro-card p { margin: 0; color: #678089; line-height: 1.75; }
.intro-card button, .process-card button { margin-top: 22px; padding: 0; color: #0f6665; font-weight: 700; }
.section { padding: 26px; background: rgba(255, 255, 255, 0.82); }
.section-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.section-head h2 { margin: 10px 0; font-size: 36px; }
.section-head p { margin: 0; color: #6a828b; line-height: 1.75; }
.featured { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; }
.card-grid { display: grid; gap: 18px; }
.card-grid.three { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.card-grid.two { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.card-grid.four { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.info-card, .case-card, .process-card { border-radius: 28px; overflow: hidden; background: rgba(255, 255, 255, 0.9); border: 1px solid rgba(23, 55, 64, 0.06); transition: transform 0.28s ease, box-shadow 0.28s ease; }
.info-card:hover, .case-card:hover, .process-card:hover { transform: translateY(-4px); box-shadow: 0 24px 40px rgba(18, 59, 69, 0.1); }
.info-card { padding: 22px; }
.card-top { justify-content: space-between; }
.meta-row { margin: 18px 0; }
.meta-row span { display: inline-flex; align-items: center; gap: 6px; color: #738c94; font-size: 13px; }
.doctor-head { margin-bottom: 18px; }
.doctor-card p, .info-card p, .case-card p, .process-card p, .case-detail p { color: #657d86; line-height: 1.75; }
.doctor-head span, .card-top span { color: #738c94; font-size: 13px; }
.doctor-card strong, .case-card strong, .info-card strong, .process-card strong { display: block; }
.case-card.large { grid-column: span 2; }
.case-image { height: 220px; }
.case-card.large .case-image { height: 280px; }
.case-body { padding: 20px 22px 22px; }
.case-body strong { margin-top: 8px; font-size: 24px; }
.process { background: linear-gradient(160deg, rgba(17, 78, 87, 0.04), rgba(255, 255, 255, 0.88)); }
.process-card { padding: 24px; }
.process-index { display: inline-flex; margin-bottom: 18px; font-size: 34px; font-weight: 800; color: rgba(15, 102, 101, 0.22); }
.cta-banner { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 28px; background: linear-gradient(135deg, #103f47, #195a64); color: #f3fbfc; }
.cta-banner h2 { max-width: 720px; margin: 12px 0; font-size: 38px; }
.cta-banner p { max-width: 720px; margin: 0; color: rgba(243, 251, 252, 0.78); line-height: 1.8; }
.floating-consult { position: fixed; right: 24px; bottom: 24px; z-index: 40; border: none; border-radius: 999px; padding: 15px 24px; background: linear-gradient(135deg, #0f6665, #28a78f); color: #ffffff; font-weight: 700; box-shadow: 0 18px 42px rgba(15, 102, 101, 0.32); cursor: pointer; }
.case-detail-image { width: 100%; height: 260px; border-radius: 24px; overflow: hidden; margin-bottom: 18px; }
.case-detail-summary { font-size: 16px; color: #18363d; }
@keyframes spin-ring { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes float-orb { 0%,100% { transform: translate3d(0,0,0); } 50% { transform: translate3d(0,18px,0); } }
@keyframes float-card { 0%, 100% { transform: translateY(0px); } 50% { transform: translateY(-10px); } }
@keyframes pulse-floor { 0%, 100% { opacity: 0.74; transform: rotateX(82deg) translateZ(-80px) scale(1); } 50% { opacity: 0.92; transform: rotateX(82deg) translateZ(-80px) scale(1.06); } }
@media (max-width: 1360px) { .hero, .featured, .intro-grid { grid-template-columns: 1fr; } }
@media (max-width: 1180px) { .card-grid.three, .card-grid.four { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 980px) { .home-page { padding: 12px; } .topbar, .hero, .intro-grid, .section, .featured, .cta-banner { border-radius: 24px; } .topbar, .section-head, .cta-banner { flex-direction: column; align-items: flex-start; } .hero, .card-grid.two, .card-grid.three, .card-grid.four { grid-template-columns: 1fr; } .stage-footer, .stage-indicators { flex-direction: column; align-items: flex-start; } .stage-status { flex-wrap: wrap; } .case-card.large { grid-column: auto; } }
@media (max-width: 720px) { .nav { display: none; } .hero-copy h1 { font-size: 42px; } .stage-scene { height: 430px; } .stage-grid { inset: 16% 4% 14%; } .scene-card { width: min(292px, 88%); } .scene-body h3 { font-size: 24px; } .section-head h2, .cta-banner h2 { font-size: 30px; } .floating-consult { right: 16px; bottom: 16px; } }
</style>
