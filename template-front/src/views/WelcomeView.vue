<template>
  <div class="auth-wrapper">
    <div class="auth-bg"></div>
    <div class="auth-container">
      
      <!-- Left side: Introduction & Welcome Text -->
      <div class="auth-intro">
        <div class="brand">
          <div class="brand-mark"><el-icon><Monitor /></el-icon></div>
          <div class="brand-text">
            <strong>智慧医疗问诊系统</strong>
            <span>Intelligent Consultation</span>
          </div>
        </div>
        
        <div class="intro-content">
          <span class="intro-kicker">Welcome to the Platform</span>
          <h1>开启您的专属<br/><span class="text-highlight">智慧医疗服务</span></h1>
          <p class="intro-desc">
            无论您是需要日常的健康咨询、寻找权威的三甲专家解答，还是希望通过前沿的大模型进行AI辅助分诊，我们都将全天候为您提供最专业、可靠的就医保障。
          </p>
          
          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-icon"><el-icon><Service /></el-icon></div>
              <div class="feature-text">
                <strong>名家在线问诊</strong>
                <span>打破时间与地域限制，随时随地向顶尖专家提问。</span>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon"><el-icon><Cpu /></el-icon></div>
              <div class="feature-text">
                <strong>AI 智能导诊辅助</strong>
                <span>基于医疗大模型的精准排查，秒级推荐初诊科室。</span>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon"><el-icon><DocumentChecked /></el-icon></div>
              <div class="feature-text">
                <strong>全生命周期健康档案</strong>
                <span>全程留存电子病历与处方记录，复诊随访无忧跟踪。</span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="intro-footer">
          <el-button class="btn-back" round @click="router.push('/')">
            <el-icon class="el-icon--left"><ArrowLeft /></el-icon> 先返回平台首页逛逛
          </el-button>
        </div>
      </div>

      <!-- Right side: Auth Form (Router View for Login/Register) -->
      <div class="auth-panel">
        <router-view v-slot="{ Component }">
          <transition name="panel-fade" mode="out-in">
            <div class="panel-wrapper">
              <component :is="Component" />
            </div>
          </transition>
        </router-view>
      </div>

    </div>
  </div>
</template>

<script setup>
import { Monitor, Service, Cpu, DocumentChecked, ArrowLeft } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
:global(:root) {
  --auth-primary: #3DA19F;
  --auth-text-main: #2C3E50;
  --auth-text-muted: #7E8C99;
  --auth-bg-color: #F4F7FB;
}

* {
  box-sizing: border-box;
}

.auth-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  position: relative;
  background: linear-gradient(135deg, #1C2833 0%, #293846 100%);
  overflow: hidden;
  font-family: 'Inter', -apple-system, sans-serif;
}

/* Background Image Overlay */
.auth-bg {
  position: absolute;
  inset: 0;
  background-image: url('https://images.unsplash.com/photo-1551076805-e18690c5e561?ixlib=rb-1.2.1&auto=format&fit=crop&w=2000&q=80');
  background-size: cover;
  background-position: center;
  opacity: 0.15;
  mix-blend-mode: overlay;
  z-index: 1;
}

/* Abstract Orbs for depth */
.auth-wrapper::before,
.auth-wrapper::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  z-index: 2;
  filter: blur(80px);
}

.auth-wrapper::before {
  top: -15%;
  left: -10%;
  width: 50vw;
  height: 50vw;
  background: radial-gradient(circle, rgba(61, 161, 159, 0.4), transparent 70%);
  animation: pulse-orb 15s infinite alternate;
}

.auth-wrapper::after {
  bottom: -20%;
  right: -10%;
  width: 60vw;
  height: 60vw;
  background: radial-gradient(circle, rgba(108, 204, 202, 0.25), transparent 70%);
  animation: pulse-orb 12s infinite alternate-reverse;
}

@keyframes pulse-orb {
  0% { transform: scale(1) translate(0, 0); }
  100% { transform: scale(1.1) translate(30px, 30px); }
}

/* Main Centered Container */
.auth-container {
  position: relative;
  z-index: 10;
  display: flex;
  width: 100%;
  max-width: 1100px;
  min-height: 680px;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(28px);
  -webkit-backdrop-filter: blur(28px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 32px;
  box-shadow: 0 40px 80px rgba(0, 0, 0, 0.35), inset 0 0 0 1px rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

/* Left Introduction Section */
.auth-intro {
  flex: 1.2;
  padding: 56px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #fff;
  background: linear-gradient(145deg, rgba(28, 40, 51, 0.85), rgba(41, 56, 70, 0.6));
  position: relative;
}

/* Brand Logo */
.brand {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-mark {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, #3DA19F, #6CCCCA);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  box-shadow: 0 6px 16px rgba(61, 161, 159, 0.35);
}

.brand-text strong {
  display: block;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.brand-text span {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  text-transform: uppercase;
  letter-spacing: 1.5px;
  margin-top: 4px;
  display: block;
}

/* Main Text Content */
.intro-content {
  margin-top: 40px;
  margin-bottom: 20px;
}

.intro-kicker {
  display: inline-block;
  padding: 6px 14px;
  background: rgba(61, 161, 159, 0.15);
  border: 1px solid rgba(61, 161, 159, 0.3);
  color: #5BC0BE;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  margin-bottom: 24px;
}

.intro-content h1 {
  font-size: 42px;
  font-weight: 800;
  line-height: 1.25;
  margin: 0 0 20px 0;
  letter-spacing: -0.5px;
}

.text-highlight {
  color: transparent;
  background: linear-gradient(90deg, #5BC0BE, #8BE0D2);
  -webkit-background-clip: text;
  background-clip: text;
}

.intro-desc {
  font-size: 16px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 48px;
  max-width: 95%;
}

/* Feature List Base */
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #8be0d2;
  flex-shrink: 0;
}

.feature-text strong {
  display: block;
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 4px;
  color: rgba(255, 255, 255, 0.95);
}

.feature-text span {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.5;
}

/* Footer Section */
.intro-footer {
  margin-top: 40px;
}

.btn-back {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  transition: all 0.3s;
}

.btn-back:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.4);
  color: white;
}

/* Right Panel form area */
.auth-panel {
  flex: 1;
  width: 460px;
  max-width: 460px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 5;
}

.panel-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  overflow-y: auto;
}

/* Router Transitions */
.panel-fade-enter-active,
.panel-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.panel-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.panel-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* Responsive */
@media (max-width: 1024px) {
  .auth-container {
    flex-direction: column;
    max-width: 520px;
    min-height: auto;
  }
  
  .auth-intro {
    flex: none;
    padding: 40px 32px;
  }
  
  .intro-content h1 {
    font-size: 32px;
  }
  
  .feature-list {
    display: none; /* Hide feature list on small screens to save vertical space */
  }

  .auth-panel {
    flex: none;
    width: 100%;
    max-width: 100%;
    min-height: 500px;
  }
}
@media (max-width: 480px) {
  .auth-wrapper {
    padding: 16px;
  }
  .auth-intro {
    padding: 24px;
  }
  .intro-content h1 {
    font-size: 28px;
  }
}
</style>
