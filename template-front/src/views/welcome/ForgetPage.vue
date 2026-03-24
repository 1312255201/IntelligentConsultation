<template>
  <div class="auth-page">
    <div class="page-head">
      <h2>找回密码</h2>
      <p>先完成邮箱验证，再设置新的登录密码。</p>
    </div>

    <el-steps :active="active" finish-status="success" align-center class="steps">
      <el-step title="验证邮箱" />
      <el-step title="设置新密码" />
    </el-steps>

    <div v-if="active === 0">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @validate="onValidate">
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

      <div class="helper-row">
        <el-link @click="router.push('/register')">没有账号？去注册</el-link>
        <el-link @click="router.push('/login')">返回登录</el-link>
      </div>

      <div class="action-stack">
        <el-button type="primary" size="large" @click="confirmReset">下一步</el-button>
      </div>
    </div>

    <div v-else>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item prop="password" label="新密码">
          <el-input v-model="form.password" maxlength="20" type="password" show-password placeholder="请输入新密码">
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="passwordRepeat" label="确认新密码">
          <el-input v-model="form.passwordRepeat" maxlength="20" type="password" show-password placeholder="请再次输入新密码">
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
      </el-form>

      <div class="action-stack">
        <el-button type="primary" size="large" @click="doReset">立即重置密码</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { EditPen, Lock, Message } from '@element-plus/icons-vue'
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { get, post } from '@/net'

const active = ref(0)
const formRef = ref()
const isEmailValid = ref(false)
const coldTime = ref(0)

const form = reactive({
  email: '',
  code: '',
  password: '',
  passwordRepeat: ''
})

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
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入合法的邮箱地址', trigger: ['blur', 'change'] }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度需在 6 到 16 个字符之间', trigger: ['blur', 'change'] }
  ],
  passwordRepeat: [
    { validator: validatePasswordRepeat, trigger: ['blur', 'change'] }
  ]
}

function onValidate(prop, isValid) {
  if (prop === 'email') {
    isEmailValid.value = isValid
  }
}

function validateEmail() {
  coldTime.value = 60
  get(`/api/auth/ask-code?email=${form.email}&type=reset`, () => {
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

function confirmReset() {
  formRef.value.validate((isValid) => {
    if (!isValid) return
    post('/api/auth/reset-confirm', {
      email: form.email,
      code: form.code
    }, () => {
      active.value = 1
    })
  })
}

function doReset() {
  formRef.value.validate((isValid) => {
    if (!isValid) return
    post('/api/auth/reset-password', {
      email: form.email,
      code: form.code,
      password: form.password
    }, () => {
      ElMessage.success('密码已重置，请重新登录')
      router.push('/login')
    })
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

.steps {
  margin: 24px 0 28px;
}

.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 12px;
}

.helper-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.action-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 24px;
}

.action-stack :deep(.el-button) {
  width: 100%;
}
</style>
