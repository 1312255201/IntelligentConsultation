import { createRouter, createWebHistory } from 'vue-router'
import { resolveHomeRouteByRole, takeAccessRole, unauthorized } from '@/net'

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

  if (routeName.startsWith('welcome') && !isUnauthorized) {
    next(resolveHomeRouteByRole(role))
    return
  }

  if (to.path.startsWith('/admin')) {
    if (isUnauthorized) {
      next('/login')
      return
    }
    if (role && role !== 'admin') {
      next(resolveHomeRouteByRole(role))
      return
    }
  }

  if (to.path.startsWith('/index')) {
    if (isUnauthorized) {
      next('/login')
      return
    }
    if (role === 'admin') {
      next(resolveHomeRouteByRole(role))
      return
    }
  }

  next()
})

export default router
