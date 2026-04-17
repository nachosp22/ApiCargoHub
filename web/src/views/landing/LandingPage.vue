<template>
  <div>
    <!-- Hero Section -->
    <section class="relative overflow-hidden bg-gradient-to-br from-gray-900 via-primary-900 to-gray-900">
      <div class="absolute inset-0 opacity-10" style="background-image: radial-gradient(circle at 1px 1px, rgba(255,255,255,0.3) 1px, transparent 0); background-size: 40px 40px;"></div>

      <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 lg:py-36">
        <div class="grid lg:grid-cols-2 gap-12 items-center">
          <div>
            <div class="inline-flex items-center gap-2 px-3 py-1.5 bg-primary-500/20 rounded-full mb-6">
              <span class="w-2 h-2 bg-green-400 rounded-full animate-pulse"></span>
              <span class="text-sm font-medium text-primary-200">Plataforma activa 24/7</span>
            </div>

            <h1 class="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-white leading-tight">
              Tu plataforma de
              <span class="bg-gradient-to-r from-primary-400 to-blue-400 bg-clip-text text-transparent">transporte</span>
              de mercancías
            </h1>

            <p class="mt-6 text-lg text-gray-300 max-w-lg leading-relaxed">
              Sin costes fijos. Solo pagas una pequeña comisión por cada porte completado.
              Conectamos empresas con transportistas profesionales verificados.
            </p>

            <div class="mt-8 flex flex-col sm:flex-row gap-4">
              <router-link
                to="/register"
                class="inline-flex items-center justify-center px-6 py-3.5 bg-primary text-white font-semibold rounded-xl hover:bg-primary-700 transition-all shadow-lg shadow-primary/25 hover:shadow-xl hover:shadow-primary/30 hover:-translate-y-0.5"
              >
                <i class="pi pi-building mr-2"></i>
                Necesito enviar mercancía
              </router-link>
              <router-link
                to="/register"
                class="inline-flex items-center justify-center px-6 py-3.5 bg-white/10 text-white font-semibold rounded-xl hover:bg-white/20 transition-all backdrop-blur-sm border border-white/10 hover:-translate-y-0.5"
              >
                <i class="pi pi-car mr-2"></i>
                Quiero transportar mercancía
              </router-link>
            </div>

            <!-- Trust indicators -->
            <div class="mt-12 flex items-center gap-8 text-gray-400">
              <div class="text-center">
                <p class="text-2xl font-bold text-white">0€</p>
                <p class="text-xs mt-1">Coste de alta</p>
              </div>
              <div class="w-px h-10 bg-gray-700"></div>
              <div class="text-center">
                <p class="text-2xl font-bold text-white">98%</p>
                <p class="text-xs mt-1">Satisfacción</p>
              </div>
              <div class="w-px h-10 bg-gray-700"></div>
              <div class="text-center">
                <p class="text-2xl font-bold text-white">150+</p>
                <p class="text-xs mt-1">Conductores</p>
              </div>
            </div>
          </div>

          <!-- Hero visual -->
          <div class="hidden lg:flex justify-center">
            <div class="relative w-full max-w-md">
              <div class="absolute -top-4 -left-4 bg-white rounded-2xl shadow-2xl p-5 z-10 animate-float">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-green-100 rounded-xl flex items-center justify-center">
                    <i class="pi pi-check-circle text-green-600"></i>
                  </div>
                  <div>
                    <p class="text-sm font-semibold text-gray-900">Porte Completado</p>
                    <p class="text-xs text-gray-500">Madrid → Barcelona</p>
                  </div>
                </div>
              </div>

              <div class="bg-white/5 backdrop-blur-sm border border-white/10 rounded-3xl p-8 space-y-4">
                <div class="bg-white/10 rounded-xl p-4">
                  <div class="flex items-center gap-3 mb-3">
                    <i class="pi pi-map-marker text-primary-400"></i>
                    <span class="text-white text-sm">Seguimiento en tiempo real</span>
                  </div>
                  <div class="h-32 bg-white/5 rounded-lg flex items-center justify-center">
                    <i class="pi pi-map text-4xl text-primary-300/50"></i>
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-3">
                  <div class="bg-white/10 rounded-xl p-3 text-center">
                    <i class="pi pi-truck text-primary-400 text-xl mb-1"></i>
                    <p class="text-xs text-gray-300">En tránsito</p>
                    <p class="text-lg font-bold text-white">12</p>
                  </div>
                  <div class="bg-white/10 rounded-xl p-3 text-center">
                    <i class="pi pi-euro text-yellow-400 text-xl mb-1"></i>
                    <p class="text-xs text-gray-300">Solo comisión</p>
                    <p class="text-lg font-bold text-white">0€/mes</p>
                  </div>
                </div>
              </div>

              <div class="absolute -bottom-4 -right-4 bg-white rounded-2xl shadow-2xl p-5 z-10 animate-float-delayed">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-primary-100 rounded-xl flex items-center justify-center">
                    <i class="pi pi-bolt text-primary-600"></i>
                  </div>
                  <div>
                    <p class="text-sm font-semibold text-gray-900">IA Cargo Analysis</p>
                    <p class="text-xs text-gray-500">Carga analizada en 3s</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- How it works — DUAL AUDIENCE -->
    <section id="como-funciona" class="py-20 lg:py-28 bg-white">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center max-w-2xl mx-auto mb-16">
          <p class="text-sm font-semibold text-primary uppercase tracking-wider mb-3">¿Cómo funciona?</p>
          <h2 class="text-3xl sm:text-4xl font-bold text-gray-900">
            Sencillo para todos
          </h2>
        </div>

        <div class="grid lg:grid-cols-2 gap-12">
          <!-- Para Empresas -->
          <div class="bg-gradient-to-br from-blue-50 to-primary-50 rounded-3xl p-8 border border-blue-100">
            <div class="flex items-center gap-3 mb-8">
              <div class="w-12 h-12 bg-blue-600 rounded-2xl flex items-center justify-center">
                <i class="pi pi-building text-white text-xl"></i>
              </div>
              <h3 class="text-2xl font-bold text-gray-900">Para Empresas</h3>
            </div>

            <div class="space-y-6">
              <div v-for="(step, index) in stepsEmpresas" :key="step.title" class="flex gap-4">
                <div class="flex-shrink-0 w-10 h-10 bg-blue-600 text-white rounded-xl flex items-center justify-center font-bold text-sm">
                  {{ index + 1 }}
                </div>
                <div>
                  <h4 class="font-semibold text-gray-900">{{ step.title }}</h4>
                  <p class="text-sm text-gray-500 mt-1">{{ step.description }}</p>
                </div>
              </div>
            </div>

            <router-link
              to="/register"
              class="mt-8 inline-flex items-center justify-center w-full px-6 py-3 bg-blue-600 text-white font-semibold rounded-xl hover:bg-blue-700 transition-all shadow-lg shadow-blue-600/25 hover:-translate-y-0.5"
            >
              Registrarme como Empresa
              <i class="pi pi-arrow-right ml-2"></i>
            </router-link>
          </div>

          <!-- Para Camioneros -->
          <div class="bg-gradient-to-br from-emerald-50 to-green-50 rounded-3xl p-8 border border-emerald-100">
            <div class="flex items-center gap-3 mb-8">
              <div class="w-12 h-12 bg-emerald-600 rounded-2xl flex items-center justify-center">
                <i class="pi pi-car text-white text-xl"></i>
              </div>
              <h3 class="text-2xl font-bold text-gray-900">Para Transportistas</h3>
            </div>

            <div class="space-y-6">
              <div v-for="(step, index) in stepsCamioneros" :key="step.title" class="flex gap-4">
                <div class="flex-shrink-0 w-10 h-10 bg-emerald-600 text-white rounded-xl flex items-center justify-center font-bold text-sm">
                  {{ index + 1 }}
                </div>
                <div>
                  <h4 class="font-semibold text-gray-900">{{ step.title }}</h4>
                  <p class="text-sm text-gray-500 mt-1">{{ step.description }}</p>
                </div>
              </div>
            </div>

            <router-link
              to="/register"
              class="mt-8 inline-flex items-center justify-center w-full px-6 py-3 bg-emerald-600 text-white font-semibold rounded-xl hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-600/25 hover:-translate-y-0.5"
            >
              Registrarme como Transportista
              <i class="pi pi-arrow-right ml-2"></i>
            </router-link>
          </div>
        </div>
      </div>
    </section>

    <!-- Benefits — DUAL AUDIENCE -->
    <section id="beneficios" class="py-20 lg:py-28 bg-canvas">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center max-w-2xl mx-auto mb-16">
          <p class="text-sm font-semibold text-primary uppercase tracking-wider mb-3">Ventajas</p>
          <h2 class="text-3xl sm:text-4xl font-bold text-gray-900">
            Beneficios para cada parte
          </h2>
        </div>

        <div class="grid lg:grid-cols-2 gap-12">
          <!-- Empresas benefits -->
          <div>
            <h3 class="text-xl font-bold text-gray-900 mb-6 flex items-center gap-2">
              <i class="pi pi-building text-blue-600"></i>
              Para Empresas
            </h3>
            <div class="grid gap-4">
              <div
                v-for="benefit in benefitsEmpresas"
                :key="benefit.title"
                class="bg-white rounded-2xl p-6 border border-gray-100 hover:shadow-lg hover:border-blue-100 transition-all duration-300"
              >
                <div class="flex items-start gap-4">
                  <div class="w-12 h-12 rounded-2xl flex items-center justify-center flex-shrink-0" :class="benefit.bgColor">
                    <i :class="[benefit.icon, benefit.iconColor]" class="text-xl"></i>
                  </div>
                  <div>
                    <h4 class="font-semibold text-gray-900">{{ benefit.title }}</h4>
                    <p class="text-sm text-gray-500 mt-1">{{ benefit.description }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Camioneros benefits -->
          <div>
            <h3 class="text-xl font-bold text-gray-900 mb-6 flex items-center gap-2">
              <i class="pi pi-car text-emerald-600"></i>
              Para Transportistas
            </h3>
            <div class="grid gap-4">
              <div
                v-for="benefit in benefitsCamioneros"
                :key="benefit.title"
                class="bg-white rounded-2xl p-6 border border-gray-100 hover:shadow-lg hover:border-emerald-100 transition-all duration-300"
              >
                <div class="flex items-start gap-4">
                  <div class="w-12 h-12 rounded-2xl flex items-center justify-center flex-shrink-0" :class="benefit.bgColor">
                    <i :class="[benefit.icon, benefit.iconColor]" class="text-xl"></i>
                  </div>
                  <div>
                    <h4 class="font-semibold text-gray-900">{{ benefit.title }}</h4>
                    <p class="text-sm text-gray-500 mt-1">{{ benefit.description }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Pricing — Commission model -->
    <section id="precios" class="py-20 lg:py-28 bg-white">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center max-w-3xl mx-auto">
          <p class="text-sm font-semibold text-primary uppercase tracking-wider mb-3">Precios</p>
          <h2 class="text-3xl sm:text-4xl font-bold text-gray-900 mb-4">
            Sin cuotas. Sin sorpresas.
          </h2>
          <p class="text-lg text-gray-500 mb-12">
            Solo cobramos una pequeña comisión por cada porte completado con éxito.
          </p>
        </div>

        <div class="max-w-2xl mx-auto">
          <div class="relative bg-gradient-to-br from-primary-50 to-blue-50 rounded-3xl p-10 border border-primary-100 shadow-xl shadow-primary/10">
            <div class="absolute -top-3 left-1/2 -translate-x-1/2">
              <span class="px-4 py-1.5 bg-primary text-white text-sm font-semibold rounded-full">
                Modelo único
              </span>
            </div>

            <div class="text-center mb-8">
              <div class="text-6xl font-extrabold text-gray-900 mb-2">0€</div>
              <p class="text-lg text-gray-500">de alta y sin cuota mensual</p>
            </div>

            <div class="grid sm:grid-cols-2 gap-4 mb-8">
              <div class="flex items-start gap-3">
                <i class="pi pi-check-circle text-green-500 mt-0.5 flex-shrink-0"></i>
                <span class="text-sm text-gray-700">Registro gratuito</span>
              </div>
              <div class="flex items-start gap-3">
                <i class="pi pi-check-circle text-green-500 mt-0.5 flex-shrink-0"></i>
                <span class="text-sm text-gray-700">Análisis IA de carga incluido</span>
              </div>
              <div class="flex items-start gap-3">
                <i class="pi pi-check-circle text-green-500 mt-0.5 flex-shrink-0"></i>
                <span class="text-sm text-gray-700">Seguimiento GPS en tiempo real</span>
              </div>
              <div class="flex items-start gap-3">
                <i class="pi pi-check-circle text-green-500 mt-0.5 flex-shrink-0"></i>
                <span class="text-sm text-gray-700">Facturación automática</span>
              </div>
              <div class="flex items-start gap-3">
                <i class="pi pi-check-circle text-green-500 mt-0.5 flex-shrink-0"></i>
                <span class="text-sm text-gray-700">Seguro de mercancía incluido</span>
              </div>
              <div class="flex items-start gap-3">
                <i class="pi pi-check-circle text-green-500 mt-0.5 flex-shrink-0"></i>
                <span class="text-sm text-gray-700">Panel de control completo</span>
              </div>
            </div>

            <div class="bg-white/80 rounded-2xl p-6 text-center border border-primary-100">
              <p class="text-sm text-gray-500 mb-1">Modelo de comisión</p>
              <p class="text-gray-700 font-medium">
                Solo cobramos un pequeño porcentaje por cada porte completado con éxito.
                Si no envías, no pagas. Así de simple.
              </p>
            </div>

            <router-link
              to="/register"
              class="mt-8 inline-flex items-center justify-center w-full px-6 py-4 bg-primary text-white font-bold text-lg rounded-xl hover:bg-primary-700 transition-all shadow-lg shadow-primary/25 hover:-translate-y-0.5"
            >
              Empezar Gratis
              <i class="pi pi-arrow-right ml-2"></i>
            </router-link>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="py-20 lg:py-28 bg-gradient-to-r from-primary-600 to-primary-800 relative overflow-hidden">
      <div class="absolute inset-0 opacity-10" style="background-image: radial-gradient(circle at 1px 1px, rgba(255,255,255,0.4) 1px, transparent 0); background-size: 32px 32px;"></div>
      <div class="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-3xl sm:text-4xl font-bold text-white mb-6">
          ¿Listo para empezar?
        </h2>
        <p class="text-lg text-primary-100 mb-8 max-w-2xl mx-auto">
          Tanto si necesitas enviar mercancía como si quieres transportarla,
          CargoHub te conecta sin costes fijos. Solo pagas por resultados.
        </p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <router-link
            to="/register"
            class="inline-flex items-center justify-center px-8 py-4 bg-white text-primary-700 font-bold text-lg rounded-xl hover:bg-gray-50 transition-all shadow-xl hover:-translate-y-0.5"
          >
            <i class="pi pi-building mr-2"></i>
            Soy Empresa
          </router-link>
          <router-link
            to="/register"
            class="inline-flex items-center justify-center px-8 py-4 bg-white/10 text-white font-bold text-lg rounded-xl hover:bg-white/20 transition-all border border-white/20 hover:-translate-y-0.5"
          >
            <i class="pi pi-car mr-2"></i>
            Soy Transportista
          </router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
const stepsEmpresas = [
  {
    title: 'Regístrate gratis',
    description: 'Crea tu cuenta en menos de 2 minutos. Sin coste de alta ni compromiso.',
  },
  {
    title: 'Describe tu carga',
    description: 'Escribe qué necesitas enviar. Nuestra IA calcula peso, volumen y vehículo óptimo automáticamente.',
  },
  {
    title: 'Te asignamos el mejor transportista',
    description: 'Nuestro algoritmo encuentra al conductor ideal por cercanía, disponibilidad y tipo de vehículo.',
  },
  {
    title: 'Sigue tu envío en tiempo real',
    description: 'Rastrea tu porte con GPS desde la recogida hasta la entrega. Notificaciones en cada paso.',
  },
]

const stepsCamioneros = [
  {
    title: 'Regístrate gratis',
    description: 'Completa tu perfil con tus datos y experiencia. Sin coste de alta.',
  },
  {
    title: 'Un admin verifica tu perfil',
    description: 'Nuestro equipo revisa tu solicitud para garantizar la calidad del servicio.',
  },
  {
    title: 'Recibe ofertas de porte',
    description: 'Te llegan portes que encajan con tu zona, vehículo y disponibilidad.',
  },
  {
    title: 'Gestiona todo desde la app',
    description: 'Acepta portes, navega a destino y confirma entregas desde tu móvil.',
  },
]

const benefitsEmpresas = [
  {
    icon: 'pi pi-truck',
    iconColor: 'text-blue-600',
    bgColor: 'bg-blue-100',
    title: 'Sin flota propia',
    description: 'No necesitas invertir en camiones ni conductores. Accede a una red de transportistas verificados.',
  },
  {
    icon: 'pi pi-euro',
    iconColor: 'text-emerald-600',
    bgColor: 'bg-emerald-100',
    title: 'Precios competitivos',
    description: 'Sin cuotas fijas. Solo pagas una comisión por porte completado. Transparente y predecible.',
  },
  {
    icon: 'pi pi-map-marker',
    iconColor: 'text-orange-600',
    bgColor: 'bg-orange-100',
    title: 'Tracking GPS en vivo',
    description: 'Sigue cada envío en tiempo real. Sabe exactamente dónde está tu mercancía en todo momento.',
  },
  {
    icon: 'pi pi-file',
    iconColor: 'text-violet-600',
    bgColor: 'bg-violet-100',
    title: 'Facturación automática',
    description: 'Facturas generadas automáticamente al completar cada porte. Descarga y gestiona desde tu portal.',
  },
]

const benefitsCamioneros = [
  {
    icon: 'pi pi-users',
    iconColor: 'text-emerald-600',
    bgColor: 'bg-emerald-100',
    title: 'Sin buscar clientes',
    description: 'Los portes llegan a ti. No pierdas tiempo buscando carga: nosotros te la asignamos.',
  },
  {
    icon: 'pi pi-calendar',
    iconColor: 'text-blue-600',
    bgColor: 'bg-blue-100',
    title: 'Agenda flexible',
    description: 'Tú decides cuándo y dónde trabajar. Configura tu disponibilidad y radio de acción.',
  },
  {
    icon: 'pi pi-shield',
    iconColor: 'text-rose-600',
    bgColor: 'bg-rose-100',
    title: 'Cobra seguro',
    description: 'Pagos garantizados por cada porte completado. Sin impagos ni perseguir facturas.',
  },
  {
    icon: 'pi pi-mobile',
    iconColor: 'text-cyan-600',
    bgColor: 'bg-cyan-100',
    title: 'App móvil completa',
    description: 'Acepta portes, navega, confirma entregas y gestiona tu agenda desde una sola aplicación.',
  },
]
</script>

<style scoped>
@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

@keyframes float-delayed {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-8px); }
}

.animate-float {
  animation: float 4s ease-in-out infinite;
}

.animate-float-delayed {
  animation: float-delayed 4s ease-in-out infinite 1s;
}
</style>
