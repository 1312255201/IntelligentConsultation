<template>
  <div class="auth-layout">
    <section class="auth-hero">
      <div class="hero-topbar">
        <div class="brand">
          <div class="brand-mark">IC</div>
          <div>
            <strong>智能问诊系统</strong>
            <span>Intelligent Consultation</span>
          </div>
        </div>
        <el-button plain @click="router.push('/')">返回首页</el-button>
      </div>

      <div class="hero-copy">
        <span class="hero-kicker">Medical Access Portal</span>
        <h1>让用户先看到网站主页，再自然进入登录与咨询流程</h1>
        <p>
          当前登录、注册、找回密码统一放在独立入口中，首页上的“立刻咨询”“预约专家”“查看案例”等按钮都可以继续引导用户进入系统。
        </p>
      </div>

      <div class="hero-grid">
        <article class="hero-card">
          <strong>首页先承接</strong>
          <span>先展示平台价值、科室能力、推荐医生和案例，再引导用户登录。</span>
        </article>
        <article class="hero-card">
          <strong>多入口转化</strong>
          <span>不再依赖右上角单一登录按钮，减少用户找入口的成本。</span>
        </article>
        <article class="hero-card">
          <strong>后续易扩展</strong>
          <span>后面接预约挂号、患者建档、智能问诊时可以直接复用当前入口设计。</span>
        </article>
      </div>
    </section>

    <section class="auth-panel">
      <router-view v-slot="{ Component }">
        <transition name="panel-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) 430px;
  gap: 18px;
  padding: 20px;
}

.auth-hero,
.auth-panel {
  border-radius: 32px;
  overflow: hidden;
  box-shadow: var(--app-shadow);
}

.auth-hero {
  padding: 28px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #f4fbfd;
  background:
    radial-gradient(circle at 18% 22%, rgba(128, 219, 208, 0.24), transparent 28%),
    radial-gradient(circle at 78% 28%, rgba(255, 204, 145, 0.22), transparent 22%),
    linear-gradient(145deg, #0f3f47 0%, #16545d 46%, #0d3339 100%);
}

.hero-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #8be0d2, #ffd09b);
  color: #103d45;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.brand strong,
.brand span {
  display: block;
}

.brand span {
  margin-top: 4px;
  color: rgba(244, 251, 253, 0.72);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy {
  max-width: 640px;
  padding: 32px 0;
}

.hero-kicker {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 18px 0;
  font-size: clamp(34px, 4vw, 54px);
  line-height: 1.06;
}

.hero-copy p {
  max-width: 560px;
  margin: 0;
  color: rgba(244, 251, 253, 0.8);
  font-size: 16px;
  line-height: 1.8;
}

.hero-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.hero-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(18px);
}

.hero-card strong {
  display: block;
  margin-bottom: 10px;
}

.hero-card span {
  color: rgba(244, 251, 253, 0.75);
  font-size: 13px;
  line-height: 1.75;
}

.auth-panel {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(16, 57, 60, 0.08);
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
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .hero-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .auth-layout {
    padding: 12px;
  }

  .auth-hero,
  .auth-panel {
    border-radius: 24px;
  }

  .auth-hero {
    padding: 20px;
  }

  .hero-topbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
