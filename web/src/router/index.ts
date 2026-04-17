import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  // --- Public routes (LandingLayout) ---
  {
    path: '/',
    component: () => import('@/layouts/LandingLayout.vue'),
    children: [
      {
        path: '',
        name: 'landing',
        component: () => import('@/views/landing/LandingPage.vue'),
      },
    ],
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { requiresAuth: false },
  },

  // --- Portal routes (authenticated, PortalLayout) ---
  {
    path: '/portal',
    component: () => import('@/layouts/PortalLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/portal/dashboard',
      },
      {
        path: 'dashboard',
        name: 'portal-dashboard',
        component: () => import('@/views/portal/DashboardView.vue'),
      },
      {
        path: 'solicitar-porte',
        name: 'portal-solicitar-porte',
        component: () => import('@/views/portal/SolicitarPorteView.vue'),
      },
      {
        path: 'mis-portes',
        name: 'portal-mis-portes',
        component: () => import('@/views/portal/MisPortesView.vue'),
      },
      {
        path: 'portes/:id/tracking',
        name: 'portal-tracking',
        component: () => import('@/views/portal/TrackingPorteView.vue'),
      },
      {
        path: 'mis-facturas',
        name: 'portal-mis-facturas',
        component: () => import('@/views/portal/MisFacturasView.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition || { top: 0 }
  },
})

// Navigation guard — protect portal routes
router.beforeEach(async (to) => {
  // Check both canonical and legacy token keys
  const isLoggedIn = !!(
    localStorage.getItem('auth_token') || localStorage.getItem('token')
  )

  if (to.matched.some((record) => record.meta.requiresAuth) && !isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if ((to.name === 'login' || to.name === 'register') && isLoggedIn) {
    return { name: 'portal-dashboard' }
  }
})

export default router
