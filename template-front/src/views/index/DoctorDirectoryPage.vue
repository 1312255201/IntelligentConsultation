<template>
  <div class="doctor-directory-page">
    <section class="hero-card">
      <div>
        <h3>医生信息查询</h3>
        <p>查看平台医生的科室归属、擅长方向和近期排班，帮助患者在发起问诊前先完成医生筛选。</p>
      </div>
      <el-button type="primary" @click="router.push('/index/consultation')">去在线问诊</el-button>
    </section>

    <section class="toolbar-card">
      <div class="toolbar-filters">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索医生姓名、职称、科室或擅长方向"
          style="max-width: 360px"
        />
        <el-select v-model="departmentFilter" clearable filterable placeholder="全部科室" style="width: 180px">
          <el-option v-for="item in departmentOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </div>
    </section>

    <section class="doctor-grid" v-loading="loading">
      <article v-for="item in filteredDoctors" :key="item.id" class="doctor-card">
        <div class="doctor-top">
          <el-avatar :size="58" :src="resolveImagePath(item.photo) || undefined">{{ item.name?.slice(0, 1) || '医' }}</el-avatar>
          <div class="doctor-text">
            <strong>{{ item.name }}</strong>
            <span>{{ item.title || '在线接诊医生' }}</span>
            <em>{{ item.departmentName || '未设置科室' }}</em>
          </div>
        </div>
        <p class="doctor-desc">{{ item.introduction || item.expertise || '暂无简介' }}</p>
        <div class="tag-list">
          <el-tag v-for="tag in item.serviceTags || []" :key="tag" effect="light">{{ tag }}</el-tag>
        </div>
        <div class="doctor-meta">
          <span>近期排班：{{ item.nextScheduleText || '暂未配置' }}</span>
          <span>可用排班：{{ item.availableScheduleCount || 0 }}</span>
        </div>
        <div class="doctor-actions">
          <el-button plain @click="router.push('/index/triage')">先 AI 导诊</el-button>
          <el-button type="primary" @click="router.push('/index/consultation')">发起问诊</el-button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { get, resolveImagePath } from '@/net'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const departmentFilter = ref('')
const doctors = ref([])

const departmentOptions = computed(() => [...new Set(doctors.value.map(item => item.departmentName).filter(Boolean))])

const filteredDoctors = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return doctors.value.filter(item => {
    if (departmentFilter.value && item.departmentName !== departmentFilter.value) return false
    if (!search) return true
    return [
      item.name,
      item.title,
      item.departmentName,
      item.expertise,
      item.introduction,
      ...(item.serviceTags || [])
    ].filter(Boolean).some(text => `${text}`.toLowerCase().includes(search))
  })
})

function loadDoctors() {
  loading.value = true
  get('/api/user/doctor/list', (data) => {
    doctors.value = data || []
    loading.value = false
  }, () => {
    loading.value = false
  })
}

onMounted(loadDoctors)
</script>

<style scoped>
.doctor-directory-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.toolbar-card,
.doctor-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 40px rgba(17, 70, 77, 0.08);
}

.hero-card,
.toolbar-card {
  padding: 24px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  flex-wrap: wrap;
}

.hero-card h3 {
  margin: 0;
  font-size: 24px;
  color: #17373d;
}

.hero-card p {
  margin: 8px 0 0;
  color: #687c84;
  line-height: 1.7;
}

.toolbar-filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.doctor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.doctor-card {
  padding: 22px;
}

.doctor-top {
  display: flex;
  gap: 14px;
  align-items: center;
}

.doctor-text strong {
  display: block;
  color: #17373d;
  font-size: 18px;
}

.doctor-text span,
.doctor-text em,
.doctor-meta span {
  display: block;
  color: #6c7f86;
  font-style: normal;
  margin-top: 4px;
}

.doctor-desc {
  margin: 16px 0;
  color: #4f666d;
  line-height: 1.8;
  min-height: 56px;
}

.tag-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  min-height: 32px;
}

.doctor-meta {
  display: grid;
  gap: 6px;
  margin-top: 14px;
}

.doctor-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
  flex-wrap: wrap;
}
</style>
