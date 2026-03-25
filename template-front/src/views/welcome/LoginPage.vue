<template>
  <div class="auth-page">
    <div class="page-head">
      <h2>登录系统</h2>
      <p>输入账号信息后进入智能问诊系统。</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item prop="username" label="用户名或邮箱">
        <el-input v-model="form.username" maxlength="20" placeholder="请输入用户名或邮箱">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="password" label="密码">
        <el-input v-model="form.password" type="password" maxlength="20" show-password placeholder="请输入密码">
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <div class="helper-row">
        <el-checkbox v-model="form.remember" label="记住我" />
        <el-link @click="router.push('/forget')">忘记密码</el-link>
      </div>
    </el-form>

    <div class="action-stack">
      <el-button type="primary" size="large" @click="userLogin">立即登录</el-button>
      <el-button size="large" @click="router.push('/')">先返回首页看看</el-button>
    </div>

    <div class="page-foot">
      <span>还没有账号？</span>
      <el-link type="primary" @click="router.push('/register')">去注册</el-link>
    </div>
  </div>
</template>

<script setup>
import { Lock, User } from '@element-plus/icons-vue'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, resolveHomeRouteByRole } from '@/net'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const form = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

function userLogin() {
  formRef.value.validate((isValid) => {
    if (!isValid) return
    login(form.username, form.password, form.remember, (data) => {
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
      router.push(redirect.startsWith('/') ? redirect : resolveHomeRouteByRole(data.role))
    })
  })
}
</script>

<style scoped>
.auth-page {
  padding: 48px 32px 36px;
}

.page-head h2 {
  margin: 0;
  font-size: 30px;
}

.page-head p {
  margin: 10px 0 0;
  color: var(--app-muted);
  line-height: 1.7;
}

.helper-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}

.action-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 28px;
}

.action-stack :deep(.el-button) {
  width: 100%;
}

.page-foot {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  color: var(--app-muted);
}
</style>
