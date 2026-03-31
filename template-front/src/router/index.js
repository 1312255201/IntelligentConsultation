import { createRouter, createWebHistory } from 'vue-router'
import { resolveHomeRouteByRole, takeAccessRole, unauthorized } from '@/net'

function workspaceRoleByPath(path = '') {
  if (path.startsWith('/admin')) return 'admin'
  if (path.startsWith('/doctor')) return 'doctor'
  if (path.startsWith('/index')) return 'user'
  return null
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue')
    },
    {
      path: '/login',
      name: 'welcome-login',
      component: () => import('@/views/WelcomeView.vue'),
      children: [
        {
          path: '',
          component: () => import('@/views/welcome/LoginPage.vue')
        }
      ]
    },
    {
      path: '/register',
      name: 'welcome-register',
      component: () => import('@/views/WelcomeView.vue'),
      children: [
        {
          path: '',
          component: () => import('@/views/welcome/RegisterPage.vue')
        }
      ]
    },
    {
      path: '/forget',
      name: 'welcome-forget',
      component: () => import('@/views/WelcomeView.vue'),
      children: [
        {
          path: '',
          component: () => import('@/views/welcome/ForgetPage.vue')
        }
      ]
    },
    {
      path: '/index',
      component: () => import('@/views/IndexView.vue'),
      redirect: '/index/profile',
      children: [
        {
          path: 'profile',
          name: 'index-profile',
          component: () => import('@/views/index/ProfilePage.vue')
        },
        {
          path: 'patient',
          name: 'index-patient',
          component: () => import('@/views/index/PatientPage.vue')
        },
        {
          path: 'health',
          name: 'index-health',
          component: () => import('@/views/index/HealthPage.vue')
        },
        {
          path: 'consultation',
          name: 'index-consultation',
          component: () => import('@/views/index/ConsultationPage.vue')
        },
        {
          path: 'overview',
          name: 'index-overview',
          component: () => import('@/views/index/OverviewPage.vue')
        }
      ]
    },
    {
      path: '/doctor',
      component: () => import('@/views/DoctorView.vue'),
      redirect: '/doctor/workbench',
      children: [
        {
          path: 'workbench',
          name: 'doctor-workbench',
          component: () => import('@/views/doctor/DoctorWorkbenchPage.vue')
        },
        {
          path: 'consultation',
          name: 'doctor-consultation',
          component: () => import('@/views/doctor/DoctorConsultationPage.vue')
        },
        {
          path: 'schedule',
          name: 'doctor-schedule',
          component: () => import('@/views/doctor/DoctorSchedulePage.vue')
        },
        {
          path: 'reply-template',
          name: 'doctor-reply-template',
          component: () => import('@/views/doctor/DoctorReplyTemplatePage.vue')
        },
        {
          path: 'profile',
          name: 'doctor-profile',
          component: () => import('@/views/index/ProfilePage.vue')
        }
      ]
    },
    {
      path: '/admin',
      component: () => import('@/views/AdminView.vue'),
      redirect: '/admin/homepage',
      children: [
        {
          path: 'homepage',
          name: 'admin-homepage',
          component: () => import('@/views/admin/HomepageSettingPage.vue')
        },
        {
          path: 'department',
          name: 'admin-department',
          component: () => import('@/views/admin/DepartmentPage.vue')
        },
        {
          path: 'doctor',
          name: 'admin-doctor',
          component: () => import('@/views/admin/DoctorPage.vue')
        },
        {
          path: 'doctor-service',
          name: 'admin-doctor-service',
          component: () => import('@/views/admin/DoctorServiceTagPage.vue')
        },
        {
          path: 'doctor-schedule',
          name: 'admin-doctor-schedule',
          component: () => import('@/views/admin/DoctorSchedulePage.vue')
        },
        {
          path: 'consultation-category',
          name: 'admin-consultation-category',
          component: () => import('@/views/admin/ConsultationCategoryPage.vue')
        },
        {
          path: 'consultation-template',
          name: 'admin-consultation-template',
          component: () => import('@/views/admin/ConsultationTemplatePage.vue')
        },
        {
          path: 'body-part',
          name: 'admin-body-part',
          component: () => import('@/views/admin/BodyPartDictPage.vue')
        },
        {
          path: 'symptom',
          name: 'admin-symptom',
          component: () => import('@/views/admin/SymptomDictPage.vue')
        },
        {
          path: 'triage-level',
          name: 'admin-triage-level',
          component: () => import('@/views/admin/TriageLevelDictPage.vue')
        },
        {
          path: 'red-flag',
          name: 'admin-red-flag',
          component: () => import('@/views/admin/RedFlagRulePage.vue')
        },
        {
          path: 'triage-knowledge',
          name: 'admin-triage-knowledge',
          component: () => import('@/views/admin/TriageKnowledgePage.vue')
        },
        {
          path: 'triage-case',
          name: 'admin-triage-case',
          component: () => import('@/views/admin/TriageCasePage.vue')
        },
        {
          path: 'consultation-record',
          name: 'admin-consultation-record',
          component: () => import('@/views/admin/ConsultationRecordPage.vue')
        },
        {
          path: 'profile',
          name: 'admin-profile',
          component: () => import('@/views/index/ProfilePage.vue')
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const isUnauthorized = unauthorized()
  const role = takeAccessRole()
  const routeName = String(to.name || '')
  const requiredRole = workspaceRoleByPath(to.path)

  if (routeName.startsWith('welcome') && !isUnauthorized) {
    next(resolveHomeRouteByRole(role))
    return
  }

  if (requiredRole) {
    if (isUnauthorized) {
      next('/login')
      return
    }
    if (role !== requiredRole) {
      next(resolveHomeRouteByRole(role))
      return
    }
  }

  next()
})

export default router
