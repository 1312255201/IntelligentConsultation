<template>
  <div class="admin-resource-overview-page">
    <section class="hero-card">
      <div>
        <h3>医疗资源管理总览</h3>
        <p>把科室、医生、排班、药品、知识库和案例库统一纳入后台维护，方便管理员从资源视角检查系统底座是否完整。</p>
      </div>
      <el-button type="primary" @click="loadSummary">刷新资源概览</el-button>
    </section>

    <section class="stat-grid">
      <article v-for="item in statCards" :key="item.title" class="stat-card">
        <span>{{ item.title }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.description }}</p>
        <el-button text type="primary" @click="router.push(item.path)">进入管理</el-button>
      </article>
    </section>

    <section class="quick-card">
      <div class="quick-head">
        <div>
          <h3>推荐维护顺序</h3>
          <p>建议先维护基础资源，再维护问诊规则和知识资产。</p>
        </div>
      </div>
      <div class="quick-grid">
        <article v-for="item in quickSteps" :key="item.title" class="quick-item">
          <strong>{{ item.title }}</strong>
          <p>{{ item.description }}</p>
          <el-button link type="primary" @click="router.push(item.path)">立即前往</el-button>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { get } from '@/net'

const router = useRouter()

const summary = reactive({
  departmentCount: 0,
  doctorCount: 0,
  scheduleCount: 0,
  medicineCount: 0,
  knowledgeCount: 0,
  caseCount: 0
})

const statCards = computed(() => [
  { title: '科室资源', value: summary.departmentCount, description: '维护科室基础信息和门诊入口。', path: '/admin/department' },
  { title: '医生资源', value: summary.doctorCount, description: '维护医生档案、账号绑定和服务能力。', path: '/admin/doctor' },
  { title: '排班资源', value: summary.scheduleCount, description: '维护接诊时间、容量和服务方式。', path: '/admin/doctor-schedule' },
  { title: '药品资源', value: summary.medicineCount, description: '维护药品、禁忌和联用冲突规则。', path: '/admin/medicine' },
  { title: '知识资产', value: summary.knowledgeCount, description: '维护导诊知识和医生知识沉淀。', path: '/admin/triage-knowledge' },
  { title: '案例资产', value: summary.caseCount, description: '维护典型案例、复盘样本和展示素材。', path: '/admin/triage-case' }
])

const quickSteps = [
  { title: '1. 先建科室', description: '没有科室就无法绑定医生和问诊分类。', path: '/admin/department' },
  { title: '2. 再建医生与排班', description: '医生资源和排班能力是问诊接诊的前提。', path: '/admin/doctor' },
  { title: '3. 配问诊分类与模板', description: '让用户能正常提交结构化问诊资料。', path: '/admin/consultation-category' },
  { title: '4. 配药品与规则', description: '支撑处方开具、禁忌提示和用药反馈。', path: '/admin/medicine' },
  { title: '5. 沉淀知识与案例', description: '补强 AI 导诊解释、人工复盘和答辩展示。', path: '/admin/triage-knowledge' }
]

function loadSummary() {
  get('/api/admin/department/list', data => {
    summary.departmentCount = (data || []).length
  })
  get('/api/admin/doctor/list', data => {
    summary.doctorCount = (data || []).length
  })
  get('/api/admin/doctor-schedule/list', data => {
    summary.scheduleCount = (data || []).length
  })
  get('/api/admin/medicine/list', data => {
    summary.medicineCount = (data || []).length
  })
  get('/api/admin/triage-knowledge/list', data => {
    summary.knowledgeCount = (data || []).length
  })
  get('/api/admin/triage-case/list', data => {
    summary.caseCount = (data || []).length
  })
}

onMounted(loadSummary)
</script>

<style scoped>
.admin-resource-overview-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.quick-card,
.stat-card {
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
.quick-head h3 {
  margin: 0;
  font-size: 24px;
  color: #17373d;
}

.hero-card p,
.quick-head p,
.stat-card p,
.quick-item p {
  margin: 8px 0 0;
  color: #687c84;
  line-height: 1.7;
}

.stat-grid,
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
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
  font-size: 32px;
  color: #17373d;
}

.quick-grid {
  margin-top: 18px;
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
