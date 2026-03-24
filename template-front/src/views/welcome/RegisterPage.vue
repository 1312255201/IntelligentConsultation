<template>
  <div class="auth-page">
    <div class="page-head">
      <h2>注册账号</h2>
      <p>创建账号后即可进入智能问诊系统继续咨询与资料管理。</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @validate="onValidate">
      <el-form-item prop="username" label="用户名">
        <el-input v-model="form.username" maxlength="20" placeholder="请输入用户名">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="password" label="密码">
        <el-input v-model="form.password" maxlength="20" type="password" show-password placeholder="请输入密码">
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="passwordRepeat" label="确认密码">
        <el-input v-model="form.passwordRepeat" maxlength="20" type="password" show-password placeholder="请再次输入密码">
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="email" label="邮箱">
        <el-input v-model="form.email" type="email" placeholder="请输入邮箱地址">
          <template #prefix>
            <el-icon><Message /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="code" label="验证码">
        <div class="code-row">
          <el-input v-model="form.code" maxlength="6" placeholder="请输入验证码">
            <template #prefix>
              <el-icon><EditPen /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" plain :disabled="!isEmailValid || coldTime > 0" @click="validateEmail">
            {{ coldTime > 0 ? `${coldTime}s` : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>
    </el-form>

    <div class="action-stack">
      <el-button type="primary" size="large" @click="register">立即注册</el-button>
      <el-button size="large" @click="router.push('/login')">返回登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { EditPen, Lock, Message, User } from '@element-plus/icons-vue'
import router from '@/router'
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { get, post } from '@/net'

const formRef = ref()
const isEmailValid = ref(false)
const coldTime = ref(0)

const form = reactive({
  username: '',
  password: '',
  passwordRepeat: '',
  email: '',
  code: ''
})

const validateUsername = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入用户名'))
  } else if (!/^[a-zA-Z0-9\u4e00-\u9fa5]+$/.test(value)) {
    callback(new Error('用户名仅支持中文、英文和数字'))
  } else {
    callback()
  }
}

const validatePasswordRepeat = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { validator: validateUsername, trigger: ['blur', 'change'] },
    { min: 2, max: 20, message: '用户名长度需在 2 到 20 个字符之间', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 个字符之间', trigger: ['blur', 'change'] }
  ],
  passwordRepeat: [
    { validator: validatePasswordRepeat, trigger: ['blur', 'change'] }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入合法的邮箱地址', trigger: ['blur', 'change'] }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

function onValidate(prop, isValid) {
  if (prop === 'email') {
    isEmailValid.value = isValid
  }
}

function register() {
  formRef.value.validate((isValid) => {
    if (!isValid) {
      ElMessage.warning('请先完整填写注册信息')
      return
    }

    post('/api/auth/register', {
      username: form.username,
      password: form.password,
      email: form.email,
      code: form.code
    }, () => {
      ElMessage.success('注册成功，请登录系统')
      router.push('/login')
    })
  })
}

function validateEmail() {
  coldTime.value = 60
  get(`/api/auth/ask-code?email=${form.email}&type=register`, () => {
    ElMessage.success(`验证码已发送到邮箱：${form.email}`)
    const handle = setInterval(() => {
      coldTime.value--
      if (coldTime.value === 0) {
        clearInterval(handle)
      }
    }, 1000)
  }, (message) => {
    ElMessage.warning(message)
    coldTime.value = 0
  })
}
</script>

<style scoped>
.auth-page {
  padding: 40px 32px 36px;
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

.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 12px;
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
</style>
