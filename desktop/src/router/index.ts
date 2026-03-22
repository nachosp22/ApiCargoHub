import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const fleetRealtimeEnabled = import.meta.env.VITE_FEATURE_FLEET_REALTIME === 'true'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    // Authenticated layout wrapper — all child routes use MainLayout
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
      },
      {
        path: 'portes',
        name: 'portes',
        component: () => import('@/views/PortesView.vue'),
      },
      {
        path: 'conductores',
        name: 'conductores',
        component: () => import('@/views/ConductoresView.vue'),
      },
      {
        path: 'vehiculos',
        name: 'vehiculos',
        component: () => import('@/views/VehiculosView.vue'),
      },
      {
        path: 'incidencias',
        name: 'incidencias',
        component: () => import('@/views/IncidenciasView.vue'),
      },
      {
        path: 'facturas',
        name: 'facturas',
        component: () => import('@/views/FacturasView.vue'),
      },
      {
        path: 'clientes',
        name: 'clientes',
        component: () => import('@/views/ClientesView.vue'),
      },
      ...(fleetRealtimeEnabled
        ? [
            {
              path: 'fleet-map',
              name: 'fleet-map',
              component: () => import('@/views/FleetMapView.vue'),
            },
          ]
        : []),
    ],
  },
]

const router = createRouter({
  // Use hash history for Electron compatibility (file:// protocol)
  history: createWebHashHistory(),
  routes,
})

// --- Navigation Guard: protect authenticated routes ---
router.beforeEach((to) => {
  const authStore = useAuthStore()
  const isLoggedIn = !!authStore.token

  // If route (or parent) requires auth and user is not authenticated, redirect to login
  if (to.matched.some((record) => record.meta.requiresAuth) && !isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // If user is authenticated and tries to visit login, redirect to dashboard
  if (to.name === 'login' && isLoggedIn) {
    return { name: 'dashboard' }
  }
})

export default router
