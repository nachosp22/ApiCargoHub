import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { AUTH_TOKEN_STORAGE_KEY, AUTH_USER_STORAGE_KEY, LEGACY_TOKEN_STORAGE_KEYS } from '@/constants/auth'

interface StoredUser {
  role?: string
  rol?: string
  clienteId?: number | string
}

function toPositiveClienteId(value: unknown): number | null {
  if (typeof value === 'number' && Number.isInteger(value) && value > 0) return value

  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (!/^\d+$/.test(trimmed)) return null

    const parsed = Number(trimmed)
    if (!Number.isSafeInteger(parsed) || parsed <= 0) return null
    return parsed
  }

  return null
}

function resolveStoredToken(): string | null {
  const current = localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
  if (current) return current

  for (const key of LEGACY_TOKEN_STORAGE_KEYS) {
    const legacy = localStorage.getItem(key)
    if (legacy) {
      localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, legacy)
      localStorage.removeItem(key)
      return legacy
    }
  }

  return null
}

function resolveStoredUser(): StoredUser | null {
  const raw = localStorage.getItem(AUTH_USER_STORAGE_KEY)
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw) as unknown
    if (!parsed || typeof parsed !== 'object') return null
    return parsed as StoredUser
  } catch {
    return null
  }
}

function isCustomerPortalEligible(user: StoredUser | null): boolean {
  if (!user) return false

  const roleRaw = typeof user.role === 'string' ? user.role : user.rol
  const role = typeof roleRaw === 'string' ? roleRaw.trim().toUpperCase() : ''

  if (role === 'CONDUCTOR') return false
  return toPositiveClienteId(user.clienteId) !== null
}

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
      {
        path: 'perfil',
        name: 'portal-perfil',
        component: () => import('@/views/portal/PerfilView.vue'),
      },
      {
        path: 'acceso-no-disponible',
        name: 'portal-acceso-no-disponible',
        component: () => import('@/views/portal/PortalAccessUnavailableView.vue'),
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
  const token = resolveStoredToken()
  const user = resolveStoredUser()
  const isLoggedIn = !!token
  const needsPortalEligibility = to.path.startsWith('/portal') && to.name !== 'portal-acceso-no-disponible'

  if (to.matched.some((record) => record.meta.requiresAuth) && !isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (isLoggedIn && needsPortalEligibility && !isCustomerPortalEligible(user)) {
    return { name: 'portal-acceso-no-disponible' }
  }

  if ((to.name === 'login' || to.name === 'register') && isLoggedIn) {
    return isCustomerPortalEligible(user)
      ? { name: 'portal-dashboard' }
      : { name: 'portal-acceso-no-disponible' }
  }
})

export default router
