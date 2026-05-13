<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'
import { useFacturasStore } from '@/stores/facturas'
import KpiCard from '@/components/dashboard/KpiCard.vue'

const dashboardStore = useDashboardStore()
const facturasStore = useFacturasStore()
const router = useRouter()

const COMISION_BENEFICIO = 0.1
const OBJETIVO_FACTURACION_ANUAL = 100000

function getNow(): Date {
  return new Date()
}

function isSameMonth(date: Date, base: Date): boolean {
  return date.getMonth() === base.getMonth() && date.getFullYear() === base.getFullYear()
}

function toDate(value: string | undefined): Date | null {
  if (!value || value === '—') return null
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
    maximumFractionDigits: 2,
  }).format(value)
}

function formatPercent(value: number): string {
  const sign = value > 0 ? '+' : ''
  return `${sign}${value.toFixed(1)}%`
}

function formatPercentPlain(value: number): string {
  return `${value.toFixed(1)}%`
}

function formatCurrencyCompact(value: number): string {
  return new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(value)
}

const facturacionMensual = computed(() => {
  const now = getNow()
  const mesActual = now.getMonth()
  const anioActual = now.getFullYear()
  const prevDate = new Date(anioActual, mesActual - 1, 1)

  const acumulado = new Map<string, number>()
  let facturacionMesActual = 0
  let facturacionAnual = 0
  let facturacionPagadaMesActual = 0
  let facturacionPendienteMesActual = 0
  let facturasPagadasMesActual = 0
  let facturasPendientesMesActual = 0
  let cantidadMesActual = 0

  facturasStore.facturas.forEach((factura) => {
    const fecha = toDate(factura.fechaEmision)
    if (!fecha) return

    const key = `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`
    acumulado.set(key, (acumulado.get(key) ?? 0) + factura.importeTotal)

    if (fecha.getFullYear() === anioActual) {
      facturacionAnual += factura.importeTotal
    }

    if (isSameMonth(fecha, now)) {
      cantidadMesActual += 1
      facturacionMesActual += factura.importeTotal

      if (factura.pagada) {
        facturasPagadasMesActual += 1
        facturacionPagadaMesActual += factura.importeTotal
      } else {
        facturasPendientesMesActual += 1
        facturacionPendienteMesActual += factura.importeTotal
      }
    }
  })

  const keyActual = `${anioActual}-${String(mesActual + 1).padStart(2, '0')}`
  const keyAnterior = `${prevDate.getFullYear()}-${String(prevDate.getMonth() + 1).padStart(2, '0')}`

  const actual = facturacionMesActual || (acumulado.get(keyActual) ?? 0)
  const anterior = acumulado.get(keyAnterior) ?? 0
  const variacionAbsoluta = actual - anterior
  const variacionPorcentual = anterior > 0 ? (variacionAbsoluta / anterior) * 100 : 0

  const ratioCobro = actual > 0 ? (facturacionPagadaMesActual / actual) * 100 : 0
  const precioMedioPorteMes = cantidadMesActual > 0 ? actual / cantidadMesActual : 0

  const ytdMensual = Array.from({ length: 12 }, (_, index) => {
    const d = new Date(anioActual, index, 1)
    const key = `${anioActual}-${String(index + 1).padStart(2, '0')}`
    return {
      label: d.toLocaleDateString('es-ES', { month: 'short' }),
      total: acumulado.get(key) ?? 0,
    }
  })

  const ytdMax = Math.max(...ytdMensual.map((m) => m.total), 1)

  const ytdBars = ytdMensual.map((mes, index) => {
    const barAreaWidth = 100
    const gap = 2.2
    const width = Math.max((barAreaWidth - gap * (ytdMensual.length - 1)) / ytdMensual.length, 1.8)
    const x = index * (width + gap)
    const normalized = ytdMax > 0 ? mes.total / ytdMax : 0
    const height = Number.isFinite(normalized) ? Math.max(normalized * 84, mes.total > 0 ? 4 : 0) : 0
    const y = 92 - height

    return {
      ...mes,
      x,
      y,
      width,
      height,
    }
  })

  return {
    actual,
    anterior,
    variacionAbsoluta,
    variacionPorcentual,
    facturacionAnual,
    beneficioAnualEstimado: facturacionAnual * COMISION_BENEFICIO,
    facturacionPagadaMesActual,
    facturacionPendienteMesActual,
    facturasPagadasMesActual,
    facturasPendientesMesActual,
    cantidadMesActual,
    ratioCobro,
    precioMedioPorteMes,
    ytdMensual,
    ytdBars,
  }
})

const objetivoFacturacionAnual = computed(() => {
  const actual = facturacionMensual.value.facturacionAnual
  const porcentaje = OBJETIVO_FACTURACION_ANUAL > 0 ? (actual / OBJETIVO_FACTURACION_ANUAL) * 100 : 0
  const porcentajeVisual = Math.min(Math.max(porcentaje, 0), 100)
  const restante = Math.max(OBJETIVO_FACTURACION_ANUAL - actual, 0)

  return {
    target: OBJETIVO_FACTURACION_ANUAL,
    actual,
    porcentaje,
    porcentajeVisual,
    restante,
  }
})

const tendenciaMensual = computed(() => {
  if (facturacionMensual.value.variacionAbsoluta > 0) {
    return {
      icon: 'pi pi-arrow-up',
      label: 'Sube vs mes anterior',
      positive: true,
    }
  }

  if (facturacionMensual.value.variacionAbsoluta < 0) {
    return {
      icon: 'pi pi-arrow-down',
      label: 'Baja vs mes anterior',
      positive: false,
    }
  }

  return {
    icon: 'pi pi-minus',
    label: 'Sin variación vs mes anterior',
    positive: true,
  }
})

const metricaPortesProgramados = computed(() => {
  if (dashboardStore.resumen) {
    return {
      title: 'Portes Mañana',
      value: dashboardStore.resumen.portesManana ?? 0,
    }
  }

  const proximos = dashboardStore.allPortes.filter((porte) => {
    if (!['PENDIENTE', 'ASIGNADO', 'PROGRAMADO'].includes(porte.estado)) return false
    const fecha = toDate(porte.fechaRecogida)
    return !!fecha && fecha >= getNow()
  })

  return {
    title: 'Próximos Portes',
    value: proximos.length,
  }
})

function goTo(path: string) {
  return router.push(path)
}

onMounted(() => {
  void dashboardStore.fetchDashboardData()
  void dashboardStore.fetchResumen()
  void dashboardStore.fetchIncidenciasPendientes()
  void facturasStore.fetchFacturas()
})
</script>

<template>
  <div class="h-full min-h-0 flex flex-col gap-4 overflow-hidden">
    <div class="shrink-0 flex items-center gap-4">
      <div class="w-12 h-12 rounded-xl bg-sky-50 dark:bg-sky-900/30 text-sky-600 dark:text-sky-400 flex items-center justify-center">
        <i class="pi pi-chart-bar text-2xl"></i>
      </div>
      <div>
        <h1 class="text-2xl font-bold text-gray-800 dark:text-white">Dashboard</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">Resumen general de la plataforma de transporte</p>
      </div>
    </div>

    <div
      v-if="dashboardStore.usingMockData"
      class="flex items-center gap-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
      <button type="button" class="kpi-button" aria-label="Ir a portes del mes" @click="goTo('/portes')">
        <KpiCard
          title="Portes Este Mes"
          :value="dashboardStore.resumen?.portesMes ?? 0"
          icon="pi-truck"
          icon-bg-color="bg-blue-50"
          icon-text-color="text-blue-600"
          :trend="dashboardStore.trends['portesMes']?.value"
          :trend-positive="dashboardStore.trends['portesMes']?.positive"
        />
      </button>
      <button type="button" class="kpi-button" aria-label="Ir a revisiones pendientes" @click="goTo('/revision-portes')">
        <KpiCard
          title="Revisiones Pendientes"
          :value="dashboardStore.revisionesPendientes"
          icon="pi-eye"
          icon-bg-color="bg-orange-50"
          icon-text-color="text-orange-600"
        />
      </button>
      <button type="button" class="kpi-button" aria-label="Ir a portes de mañana" @click="goTo('/portes')">
        <KpiCard
          :title="metricaPortesProgramados.title"
          :value="metricaPortesProgramados.value"
          icon="pi-calendar"
          icon-bg-color="bg-indigo-50"
          icon-text-color="text-indigo-600"
        />
      </button>
      <button type="button" class="kpi-button" aria-label="Ir a incidencias pendientes" @click="goTo('/incidencias')">
        <KpiCard
          title="Incidencias Pendientes"
          :value="dashboardStore.incidenciasPendientes"
          icon="pi-exclamation-triangle"
          icon-bg-color="bg-amber-50"
          icon-text-color="text-amber-600"
        />
      </button>
    </div>

    <section class="finance-panel flex-1 min-h-0 overflow-hidden p-4 lg:p-5">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100">Finanzas</h3>
          <p class="mt-0.5 text-sm text-gray-600 dark:text-gray-400">
            Resumen operativo del ejercicio y rendimiento mensual basado en fecha de emisión.
          </p>
        </div>
        <span
          class="inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium"
          :class="tendenciaMensual.positive ? 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-800' : 'bg-rose-50 text-rose-700 ring-1 ring-rose-200 dark:bg-rose-900/30 dark:text-rose-300 dark:ring-rose-800'"
        >
          <i :class="tendenciaMensual.icon"></i>
          {{ tendenciaMensual.label }}
        </span>
      </div>

      <div class="mt-3 grid grid-cols-1 gap-2.5 md:grid-cols-2 xl:grid-cols-4">
        <article class="finance-card">
          <p class="finance-label">Facturación del mes</p>
          <p class="finance-value">{{ formatCurrency(facturacionMensual.actual) }}</p>
          <p class="finance-subtext">Comparativa mensual: {{ formatCurrency(facturacionMensual.anterior) }} en el periodo anterior</p>
        </article>

        <article class="finance-card">
          <p class="finance-label">Beneficio anual</p>
          <p class="finance-value">{{ formatCurrency(facturacionMensual.beneficioAnualEstimado) }}</p>
          <p class="finance-subtext">Estimación acumulada del ejercicio (10% sobre facturación anual)</p>
        </article>

        <article class="finance-card">
          <p class="finance-label">Precio medio por porte</p>
          <p class="finance-value">{{ formatCurrency(facturacionMensual.precioMedioPorteMes) }}</p>
          <p class="finance-subtext">Promedio sobre facturas emitidas del mes actual</p>
        </article>

        <article class="finance-card">
          <p class="finance-label">Pendiente de cobro</p>
          <p class="finance-value text-rose-700 dark:text-rose-300">{{ formatCurrency(facturacionMensual.facturacionPendienteMesActual) }}</p>
          <p class="finance-subtext">{{ facturacionMensual.facturasPendientesMesActual }} facturas pendientes · {{ formatPercent(facturacionMensual.ratioCobro) }} cobrado</p>
        </article>

        <article class="finance-card md:col-span-2 xl:col-span-4">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <p class="finance-label">Objetivo anual de facturación: {{ formatCurrency(objetivoFacturacionAnual.target) }}</p>
            <p class="text-xs text-gray-500 dark:text-gray-400">Facturación acumulada del ejercicio actual (YTD)</p>
          </div>
          <div class="mt-2.5 h-2 overflow-hidden rounded-full bg-gray-100 dark:bg-gray-700">
            <div
              class="h-full rounded-full bg-blue-600 transition-all duration-500"
              :style="{ width: `${Math.max(objetivoFacturacionAnual.porcentajeVisual, objetivoFacturacionAnual.actual > 0 ? 3 : 0)}%` }"
            ></div>
          </div>
          <div class="mt-2 grid grid-cols-1 gap-2 text-xs text-gray-600 dark:text-gray-300 sm:grid-cols-2">
            <p>
              Facturado YTD: <span class="font-semibold text-blue-700 dark:text-blue-300">{{ formatCurrency(objetivoFacturacionAnual.actual) }}</span>
            </p>
            <p>
              Cumplimiento: <span class="font-semibold text-gray-900 dark:text-white">{{ formatPercentPlain(objetivoFacturacionAnual.porcentaje) }}</span>
            </p>
            <p>
              Restante para objetivo:
              <span class="font-semibold text-rose-700 dark:text-rose-300">{{ formatCurrency(objetivoFacturacionAnual.restante) }}</span>
            </p>
            <p>
              Objetivo anual:
              <span class="font-semibold text-emerald-700 dark:text-emerald-300">{{ formatCurrency(objetivoFacturacionAnual.target) }}</span>
            </p>
          </div>
        </article>
      </div>

      <div class="mt-3 flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-black/15 bg-white p-3.5 dark:border-white/15 dark:bg-black/40">
        <div class="mb-3 flex items-center justify-between gap-2">
          <h4 class="text-sm font-semibold uppercase tracking-wide text-black dark:text-white">Facturación anual por mes</h4>
          <span class="text-xs text-gray-600 dark:text-gray-300">Enero a diciembre · ejercicio actual</span>
        </div>

        <div class="finance-ytd-chart">
          <svg viewBox="0 0 100 100" preserveAspectRatio="none" aria-label="Evolución de facturación del año actual">
            <line x1="0" y1="92" x2="100" y2="92" class="ytd-baseline" />
            <g v-for="mes in facturacionMensual.ytdBars" :key="`bar-${mes.label}`">
              <rect
                v-if="mes.total > 0"
                class="ytd-bar"
                :x="mes.x"
                :y="mes.y"
                :width="mes.width"
                :height="mes.height"
                rx="1"
              >
                <title>{{ `${mes.label}: ${formatCurrency(mes.total)}` }}</title>
              </rect>
              <circle v-else class="ytd-zero-dot" :cx="mes.x + mes.width / 2" cy="92" r="0.9">
                <title>{{ `${mes.label}: ${formatCurrency(0)}` }}</title>
              </circle>
            </g>
          </svg>
          <div class="finance-ytd-axis">
            <span v-for="mes in facturacionMensual.ytdMensual" :key="`axis-${mes.label}`">{{ mes.label }}</span>
          </div>
          <div class="finance-ytd-values" aria-label="Valores mensuales de facturación">
            <span v-for="mes in facturacionMensual.ytdMensual" :key="`value-${mes.label}`">{{ formatCurrencyCompact(mes.total) }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.kpi-button {
  all: unset;
  display: block;
  height: 100%;
  width: 100%;
  cursor: pointer;
  border-radius: 0.75rem;
}

.kpi-button:focus-visible {
  outline: 2px solid rgb(59 130 246 / 1);
  outline-offset: 2px;
}

.finance-panel {
  display: flex;
  flex-direction: column;
  border-radius: 0.875rem;
  border: 1px solid rgb(229 231 235 / 1);
  background: rgb(255 255 255 / 0.92);
  box-shadow: 0 8px 20px rgb(17 24 39 / 0.05);
}

.dark .finance-panel {
  border-color: rgb(55 65 81 / 1);
  background: rgb(17 24 39 / 0.5);
}

.finance-card {
  border: 1px solid rgb(229 231 235 / 1);
  border-radius: 0.75rem;
  padding: 0.75rem;
  background: rgb(255 255 255 / 0.85);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.6);
}

.dark .finance-card {
  border-color: rgb(55 65 81 / 1);
  background: rgb(17 24 39 / 0.45);
}

.finance-label {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: rgb(75 85 99 / 1);
}

.dark .finance-label {
  color: rgb(156 163 175 / 1);
}

.finance-value {
  margin-top: 0.35rem;
  font-size: 1.05rem;
  line-height: 1.3rem;
  font-weight: 700;
  color: rgb(17 24 39 / 1);
}

.dark .finance-value {
  color: rgb(243 244 246 / 1);
}

.finance-subtext {
  margin-top: 0.25rem;
  font-size: 0.75rem;
  color: rgb(107 114 128 / 1);
}

.dark .finance-subtext {
  color: rgb(156 163 175 / 1);
}

.finance-ytd-chart {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  border-radius: 0.75rem;
  border: 1px solid rgb(17 24 39 / 0.18);
  background: rgb(255 255 255 / 1);
  padding: 0.6rem;
}

.dark .finance-ytd-chart {
  border-color: rgb(255 255 255 / 0.2);
  background: rgb(15 23 42 / 0.75);
}

.finance-ytd-chart svg {
  flex: 1;
  min-height: 0;
  width: 100%;
  height: clamp(150px, 24vh, 280px);
}

.ytd-baseline {
  stroke: rgb(17 24 39 / 0.28);
  stroke-width: 1;
}

.dark .ytd-baseline {
  stroke: rgb(255 255 255 / 0.32);
}

.ytd-bar {
  fill: rgb(37 99 235 / 1);
}

.finance-ytd-axis {
  margin-top: 0.4rem;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(2.5rem, 1fr));
  gap: 0.35rem;
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgb(107 114 128 / 1);
}

.dark .finance-ytd-axis {
  color: rgb(156 163 175 / 1);
}

.finance-ytd-values {
  margin-top: 0.25rem;
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 0.25rem;
  font-size: 0.58rem;
  text-align: center;
  color: rgb(75 85 99 / 1);
}

.dark .finance-ytd-values {
  color: rgb(156 163 175 / 1);
}

.ytd-zero-dot {
  fill: rgb(37 99 235 / 0.7);
}
</style>
