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
