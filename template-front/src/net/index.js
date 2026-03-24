import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const authItemName = 'authorize'

const backendBaseUrl = () => `${axios.defaults.baseURL || ''}`.replace(/\/$/, '')

const defaultError = (error) => {
  console.error(error)
  const status = error?.response?.status
  const message = error?.response?.data?.message

  if (status === 429 && message) {
    ElMessage.error(message)
  } else if (message) {
    ElMessage.error(message)
  } else {
    ElMessage.error('发生了一些错误，请联系管理员')
  }
}

const defaultFailure = (message, status, url) => {
  console.warn(`请求地址: ${url}, 状态码: ${status}, 错误信息: ${message}`)
  ElMessage.warning(message)
}

function currentAccessStorage() {
  if (localStorage.getItem(authItemName)) return localStorage
  if (sessionStorage.getItem(authItemName)) return sessionStorage
  return null
}

function readAccessObject() {
  const storage = currentAccessStorage()
  if (!storage) return null
  const str = storage.getItem(authItemName)
  return str ? JSON.parse(str) : null
}

function writeAccessObject(remember, authObj) {
  const targetStorage = remember ? localStorage : sessionStorage
  const otherStorage = remember ? sessionStorage : localStorage
  otherStorage.removeItem(authItemName)
  targetStorage.setItem(authItemName, JSON.stringify(authObj))
}

function takeAccessObject() {
  const authObj = readAccessObject()
  if (!authObj) return null

  if (new Date(authObj.expire) <= new Date()) {
    deleteAccessToken()
    ElMessage.warning('登录状态已过期，请重新登录')
    return null
  }
  return authObj
}

function takeAccessToken() {
  return takeAccessObject()?.token || null
}

function takeAccessRole() {
  return takeAccessObject()?.role || null
}

function authHeader() {
  const token = takeAccessToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

function storeAccessToken(remember, token, expire, role, username) {
  writeAccessObject(remember, {
    token,
    expire,
    role,
    username
  })
}

function syncAccessProfile(profile = {}) {
  const storage = currentAccessStorage()
  const current = takeAccessObject()
  if (!storage || !current) return

  storage.setItem(authItemName, JSON.stringify({
    ...current,
    role: profile.role || current.role,
    username: profile.username || current.username
  }))
}

function deleteAccessToken(redirect = false) {
  localStorage.removeItem(authItemName)
  sessionStorage.removeItem(authItemName)
  if (redirect) {
    router.push({ name: 'welcome-login' })
  }
}

function handleResponse(url, data, success, failure) {
  if (data.code === 200) {
    success(data.data)
  } else if (data.code === 401) {
    failure('登录状态已过期，请重新登录', data.code, url)
    deleteAccessToken(true)
  } else {
    failure(data.message, data.code, url)
  }
}

function internalPost(url, data, headers, success, failure, error = defaultError) {
  axios.post(url, data, { headers }).then(({ data }) => {
    handleResponse(url, data, success, failure)
  }).catch(err => error(err))
}

function internalGet(url, headers, success, failure, error = defaultError) {
  axios.get(url, { headers }).then(({ data }) => {
    handleResponse(url, data, success, failure)
  }).catch(err => error(err))
}

function login(username, password, remember, success, failure = defaultFailure) {
  internalPost('/api/auth/login', new URLSearchParams({
    username,
    password
  }), {
    'Content-Type': 'application/x-www-form-urlencoded'
  }, (data) => {
    storeAccessToken(remember, data.token, data.expire, data.role, data.username)
    ElMessage.success(`登录成功，欢迎 ${data.username}`)
    success(data)
  }, failure)
}

function post(url, data, success, failure = defaultFailure) {
  internalPost(url, data, authHeader(), success, failure)
}

function get(url, success, failure = defaultFailure) {
  internalGet(url, authHeader(), success, failure)
}

function getPublic(url, success, failure = defaultFailure) {
  internalGet(url, {}, success, failure)
}

function upload(url, data, success, failure = defaultFailure, error = defaultError) {
  internalPost(url, data, authHeader(), success, failure, error)
}

function logout(success, failure = defaultFailure) {
  get('/api/auth/logout', () => {
    deleteAccessToken()
    ElMessage.success('退出登录成功')
    success()
  }, failure)
}

function unauthorized() {
  return !takeAccessToken()
}

function resolveHomeRouteByRole(role = takeAccessRole()) {
  return role === 'admin' ? '/admin/homepage' : '/index/profile'
}

function resolveImagePath(path) {
  if (!path) return ''
  if (/^https?:\/\//.test(path)) return path
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${backendBaseUrl()}/images${normalizedPath}`
}

export {
  authHeader,
  backendBaseUrl,
  get,
  getPublic,
  login,
  logout,
  post,
  resolveHomeRouteByRole,
  resolveImagePath,
  syncAccessProfile,
  takeAccessRole,
  takeAccessToken,
  unauthorized,
  upload
}
