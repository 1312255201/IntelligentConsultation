<template>
  <div class="profile-page">
    <section class="hero-card">
      <div class="avatar-section">
        <el-upload
          :action="uploadAction"
          :headers="uploadHeaders"
          :show-file-list="false"
          :before-upload="beforeAvatarUpload"
          :on-success="handleAvatarSuccess"
          :on-error="handleAvatarError"
          accept="image/*"
          class="avatar-uploader"
        >
          <div class="avatar-shell">
            <el-avatar :size="108" :src="avatarUrl || undefined">
              {{ userInitial }}
            </el-avatar>
            <span class="avatar-tip">点击更换头像</span>
          </div>
        </el-upload>
        <p>支持 JPG、PNG、WEBP，文件大小不超过 200KB。</p>
      </div>

      <div class="hero-info">
        <span class="section-tag">Account Profile</span>
        <h2>{{ profile.username || '当前用户' }}</h2>
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
      </div>
    </section>

    <div class="form-grid">
      <section class="form-card">
        <div class="card-head">
          <div>
            <h3>修改邮箱</h3>
            <p>输入新的联系邮箱，并使用当前密码确认本次操作。</p>
          </div>
        </div>

        <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-position="top">
          <el-form-item label="新邮箱" prop="email">
            <el-input v-model="emailForm.email" placeholder="请输入新的邮箱地址">
              <template #prefix>
                <el-icon><Message /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="当前密码" prop="password">
            <el-input
              v-model="emailForm.password"
              type="password"
              show-password
              placeholder="请输入当前密码以确认"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-button :loading="emailSubmitting" type="primary" @click="submitEmailChange">
            保存邮箱
          </el-button>
        </el-form>
      </section>

      <section class="form-card">
        <div class="card-head">
          <div>
            <h3>修改密码</h3>
            <p>建议设置 6 到 20 位的新密码，并和当前密码保持区分。</p>
          </div>
        </div>

        <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              show-password
              placeholder="请输入当前密码"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              placeholder="请输入新密码"
            >
              <template #prefix>
                <el-icon><Key /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入新密码"
            >
              <template #prefix>
                <el-icon><Key /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-button :loading="passwordSubmitting" type="primary" @click="submitPasswordChange">
            更新密码
          </el-button>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { Key, Lock, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, inject, reactive, ref, watch } from 'vue'
import { authHeader, backendBaseUrl, post } from '@/net'

const accountContext = inject('account-context', null)

const profile = computed(() => accountContext?.profile || {})
const avatarUrl = computed(() => accountContext?.avatarUrl?.value || '')
const userInitial = computed(() => (profile.value.username || 'U').slice(0, 1).toUpperCase())
const registerTimeText = computed(() => formatDate(profile.value.registerTime))

const emailFormRef = ref()
const passwordFormRef = ref()
const emailSubmitting = ref(false)
const passwordSubmitting = ref(false)

const emailForm = reactive({
  email: '',
  password: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const uploadAction = computed(() => `${backendBaseUrl()}/api/image/avatar`)
const uploadHeaders = computed(() => authHeader())

watch(() => profile.value.email, (email) => {
  emailForm.email = email || ''
}, { immediate: true })

const emailRules = {
  email: [
    { required: true, message: '请输入新的邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入合法的邮箱地址', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需要在 6 到 20 位之间', trigger: ['blur', 'change'] }
  ]
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需要在 6 到 20 位之间', trigger: ['blur', 'change'] }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需要在 6 到 20 位之间', trigger: ['blur', 'change'] }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change']
    }
  ]
}

function submitEmailChange() {
  emailFormRef.value.validate((valid) => {
    if (!valid) return
    emailSubmitting.value = true
    post('/api/user/change-email', {
      email: emailForm.email,
      password: emailForm.password
    }, () => {
      emailSubmitting.value = false
      emailForm.password = ''
      accountContext?.patchProfile({ email: emailForm.email })
      ElMessage.success('邮箱更新成功')
    }, () => {
      emailSubmitting.value = false
      ElMessage.warning('邮箱更新失败，请检查输入后重试')
    })
  })
}

function submitPasswordChange() {
  passwordFormRef.value.validate((valid) => {
    if (!valid) return
    passwordSubmitting.value = true
    post('/api/user/change-password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    }, () => {
      passwordSubmitting.value = false
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      ElMessage.success('密码更新成功')
    }, (message) => {
      passwordSubmitting.value = false
      ElMessage.warning(message || '密码更新失败，请稍后再试')
    })
  })
}

function beforeAvatarUpload(file) {
  const isImage = `${file.type || ''}`.startsWith('image/')
  const isLt200K = file.size / 1024 <= 200

  if (!isImage) {
    ElMessage.error('头像文件必须是图片格式')
    return false
  }
  if (!isLt200K) {
    ElMessage.error('头像大小不能超过 200KB')
    return false
  }
  return true
}

function handleAvatarSuccess(response) {
  if (response?.code !== 200) {
    ElMessage.error(response?.message || '头像上传失败')
    return
  }

  accountContext?.patchProfile({ avatar: response.data })
  accountContext?.refreshProfile(false)
  ElMessage.success('头像上传成功')
}

function handleAvatarError() {
  ElMessage.error('头像上传失败，请稍后再试')
}

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
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-card,
.form-card {
  border: 1px solid var(--app-border);
  border-radius: 28px;
  background: var(--app-panel);
  box-shadow: var(--app-shadow);
}

.hero-card {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 24px;
  padding: 28px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(128, 217, 205, 0.2), rgba(255, 214, 164, 0.2));
  text-align: center;
}

.avatar-shell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.avatar-tip {
  display: inline-flex;
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(19, 73, 80, 0.08);
  color: #27646d;
  font-size: 13px;
}

.avatar-section p {
  margin: 18px 0 0;
  color: var(--app-muted);
  line-height: 1.7;
}

.avatar-uploader {
  cursor: pointer;
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

.hero-info h2 {
  margin: 18px 0 20px;
  font-size: 30px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.form-card {
  padding: 26px;
}

.card-head {
  margin-bottom: 22px;
}

.card-head h3 {
  margin: 0;
  font-size: 24px;
}

.card-head p {
  margin: 10px 0 0;
  color: var(--app-muted);
  line-height: 1.7;
}

@media (max-width: 960px) {
  .hero-card,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
