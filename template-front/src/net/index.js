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

function handleSuccessProcessingError(url, err, failure, error = defaultError) {
  console.error(`Error while processing successful response from ${url}`, err)
  if (typeof failure === 'function') {
    failure('页面处理响应数据时发生异常，请刷新后重试', 500, url)
    return
  }
  error(err)
}

function handleResponse(url, data, success, failure, error = defaultError) {
  if (data.code === 200) {
    try {
      success(data.data)
    } catch (err) {
      handleSuccessProcessingError(url, err, failure, error)
    }
  } else if (data.code === 401) {
    failure('登录状态已过期，请重新登录', data.code, url)
    deleteAccessToken(true)
  } else {
    failure(data.message, data.code, url)
  }
}

function handleRequestError(url, err, failure, error = defaultError) {
  const status = err?.response?.status
  const message = err?.response?.data?.message

  if (status === 401) {
    failure(message || '登录状态已过期，请重新登录', status, url)
    deleteAccessToken(true)
    return
  }

  if (status || message) {
    failure(message || '请求失败，请稍后重试', status, url)
    return
  }

  error(err)
}

function internalPost(url, data, headers, success, failure, error = defaultError) {
  axios.post(url, data, { headers }).then(({ data }) => {
    handleResponse(url, data, success, failure, error)
  }).catch(err => handleRequestError(url, err, failure, error))
}

function internalGet(url, headers, success, failure, error = defaultError) {
  axios.get(url, { headers }).then(({ data }) => {
    handleResponse(url, data, success, failure, error)
  }).catch(err => handleRequestError(url, err, failure, error))
}

async function parseBlobPayload(blob) {
  if (!(typeof Blob !== 'undefined' && blob instanceof Blob)) return null
  const text = await blob.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return { message: text }
  }
}

function resolveDownloadFilename(headers = {}, fallbackFilename = 'download.csv') {
  const contentDisposition = headers['content-disposition'] || headers['Content-Disposition']
  if (!contentDisposition) return fallbackFilename

  const encodedMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (encodedMatch?.[1]) {
    try {
      return decodeURIComponent(encodedMatch[1])
    } catch {
      return encodedMatch[1]
    }
  }

  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return plainMatch?.[1] || fallbackFilename
}

function triggerDownload(blob, filename) {
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = filename
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(objectUrl)
}

async function internalDownload(url, headers, fallbackFilename, success, failure, error = defaultError) {
  try {
    const response = await axios.get(url, {
      headers,
      responseType: 'blob'
    })
    const filename = resolveDownloadFilename(response.headers, fallbackFilename)
    triggerDownload(response.data, filename)
    success(filename)
  } catch (err) {
    const payload = await parseBlobPayload(err?.response?.data)
    const status = payload?.code || err?.response?.status
    const message = payload?.message

    if (status === 401) {
      failure(message || '登录状态已过期，请重新登录', status, url)
      deleteAccessToken(true)
      return
    }
    if (message) {
      failure(message, status, url)
      return
    }
    error(err)
  }
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

function download(url, fallbackFilename, success = () => {}, failure = defaultFailure, error = defaultError) {
  internalDownload(url, authHeader(), fallbackFilename, success, failure, error)
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
  if (role === 'admin') return '/admin/account'
  if (role === 'doctor') return '/doctor/workbench'
  return '/index/overview'
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
  download,
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
