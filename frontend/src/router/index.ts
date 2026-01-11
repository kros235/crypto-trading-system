import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      redirect: '/dashboard'
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { requiresGuest: true }
    },
    {
      path: '/signup',
      name: 'signup',
      component: () => import('../views/SignupView.vue'),
      meta: { requiresGuest: true }
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/trading-settings',
      name: 'trading-settings',
      component: () => import('../views/TradingSettingsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/transactions',
      name: 'transactions',
      component: () => import('../views/TransactionHistoryView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/holdings',
      name: 'holdings',
      component: () => import('../views/HoldingsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/bot-monitor',
      name: 'bot-monitor',
      component: () => import('../views/BotMonitorView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/daily-report',
      name: 'daily-report',
      component: () => import('../views/DailyReportView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/backtest',
      name: 'Backtest',
      component: () => import('@/views/BacktestView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('@/views/AdminDashboardView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/news',
      name: 'News',
      component: () => import('@/views/NewsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/coins',
      name: 'coins',
      component: () => import('../views/CoinListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/release-notes',
      name: 'release-notes',
      component: () => import('../views/ReleaseNotesView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/account-security',
      name: 'account-security',
      component: () => import('../views/AccountSecurityView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/help',
      name: 'help',
      component: () => import('../views/HelpView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

// 라우터 가드
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 인증이 필요한 페이지
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
    return
  }

  // 로그인한 사용자는 접근 불가 (로그인, 회원가입 페이지)
  if (to.meta.requiresGuest && authStore.isAuthenticated) {
    next('/dashboard')
    return
  }

  next()
})

export default router