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
              <span class="text-sm font-medium text-primary-200">{{ t('landing.hero.badge') }}</span>
            </div>

            <h1 class="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-white leading-tight">
              {{ t('landing.hero.titlePre') }}
              <span class="bg-gradient-to-r from-primary-400 to-blue-400 bg-clip-text text-transparent">{{ t('landing.hero.titleHighlight') }}</span>
              {{ t('landing.hero.titlePost') }}
            </h1>

            <p class="mt-6 text-lg text-gray-300 max-w-lg leading-relaxed">
              {{ t('landing.hero.subtitle') }}
            </p>

            <div class="mt-8 flex flex-col sm:flex-row gap-4">
              <router-link
                to="/register"
                class="inline-flex items-center justify-center px-6 py-3.5 bg-primary text-white font-semibold rounded-xl hover:bg-primary-700 transition-all shadow-lg shadow-primary/25 hover:shadow-xl hover:shadow-primary/30 hover:-translate-y-0.5"
              >
                <i class="pi pi-building mr-2"></i>
                {{ t('landing.hero.ctaShip') }}
              </router-link>
              <router-link
                to="/register"
                class="inline-flex items-center justify-center px-6 py-3.5 bg-white/10 text-white font-semibold rounded-xl hover:bg-white/20 transition-all backdrop-blur-sm border border-white/10 hover:-translate-y-0.5"
              >
                <i class="pi pi-car mr-2"></i>
                {{ t('landing.hero.ctaTransport') }}
              </router-link>
            </div>

            <!-- Trust indicators -->
            <div class="mt-12 flex items-center gap-8 text-gray-400">
              <div class="text-center">
                <p class="text-2xl font-bold text-white">0€</p>
                <p class="text-xs mt-1">{{ t('landing.hero.trustCost') }}</p>
              </div>
              <div class="w-px h-10 bg-gray-700"></div>
              <div class="text-center">
                <p class="text-2xl font-bold text-white">98%</p>
                <p class="text-xs mt-1">{{ t('landing.hero.trustSatisfaction') }}</p>
              </div>
              <div class="w-px h-10 bg-gray-700"></div>
              <div class="text-center">
                <p class="text-2xl font-bold text-white">150+</p>
                <p class="text-xs mt-1">{{ t('landing.hero.trustDrivers') }}</p>
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
                    <p class="text-sm font-semibold text-gray-900">{{ t('landing.hero.cardCompleted') }}</p>
                    <p class="text-xs text-gray-500">{{ t('landing.hero.cardRoute') }}</p>
                  </div>
                </div>
              </div>

              <div class="bg-white/5 backdrop-blur-sm border border-white/10 rounded-3xl p-8 space-y-4">
                <div class="bg-white/10 rounded-xl p-4">
                  <div class="flex items-center gap-3 mb-3">
                    <i class="pi pi-map-marker text-primary-400"></i>
                    <span class="text-white text-sm">{{ t('landing.hero.cardRealtime') }}</span>
                  </div>
                  <div class="h-32 bg-white/5 rounded-lg flex items-center justify-center">
                    <i class="pi pi-map text-4xl text-primary-300/50"></i>
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-3">
                  <div class="bg-white/10 rounded-xl p-3 text-center">
                    <i class="pi pi-truck text-primary-400 text-xl mb-1"></i>
                    <p class="text-xs text-gray-300">{{ t('landing.hero.cardInTransit') }}</p>
                    <p class="text-lg font-bold text-white">12</p>
                  </div>
                  <div class="bg-white/10 rounded-xl p-3 text-center">
                    <i class="pi pi-euro text-yellow-400 text-xl mb-1"></i>
                    <p class="text-xs text-gray-300">{{ t('landing.hero.cardCommission') }}</p>
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
                    <p class="text-sm font-semibold text-gray-900">{{ t('landing.hero.cardAI') }}</p>
                    <p class="text-xs text-gray-500">{{ t('landing.hero.cardAITime') }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- How it works — DUAL AUDIENCE -->
    <section id="como-funciona" class="py-20 lg:py-28 bg-white dark:bg-gray-900">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center max-w-2xl mx-auto mb-16">
          <p class="text-sm font-semibold text-primary uppercase tracking-wider mb-3">{{ t('landing.howItWorks.label') }}</p>
          <h2 class="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white">
            {{ t('landing.howItWorks.title') }}
          </h2>
        </div>

        <div class="grid lg:grid-cols-2 gap-12">
          <!-- Para Empresas -->
          <div class="bg-gradient-to-br from-blue-50 to-primary-50 dark:from-blue-950/30 dark:to-primary-950/30 rounded-3xl p-8 border border-blue-100 dark:border-blue-900">
            <div class="flex items-center gap-3 mb-8">
              <div class="w-12 h-12 bg-blue-600 rounded-2xl flex items-center justify-center">
                <i class="pi pi-building text-white text-xl"></i>
              </div>
              <h3 class="text-2xl font-bold text-gray-900 dark:text-white">{{ t('landing.howItWorks.forCompanies') }}</h3>
            </div>

            <div class="space-y-6">
              <div v-for="(step, index) in stepsEmpresas" :key="step.title" class="flex gap-4">
                <div class="flex-shrink-0 w-10 h-10 bg-blue-600 text-white rounded-xl flex items-center justify-center font-bold text-sm">
                  {{ index + 1 }}
                </div>
                <div>
                  <h4 class="font-semibold text-gray-900 dark:text-white">{{ step.title }}</h4>
                  <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">{{ step.description }}</p>
                </div>
              </div>
            </div>

            <router-link
              to="/register"
              class="mt-8 inline-flex items-center justify-center w-full px-6 py-3 bg-blue-600 text-white font-semibold rounded-xl hover:bg-blue-700 transition-all shadow-lg shadow-blue-600/25 hover:-translate-y-0.5"
            >
              {{ t('landing.howItWorks.registerAsCompany') }}
              <i class="pi pi-arrow-right ml-2"></i>
            </router-link>
          </div>

          <!-- Para Camioneros -->
          <div class="bg-gradient-to-br from-emerald-50 to-green-50 dark:from-emerald-950/30 dark:to-green-950/30 rounded-3xl p-8 border border-emerald-100 dark:border-emerald-900">
            <div class="flex items-center gap-3 mb-8">
              <div class="w-12 h-12 bg-emerald-600 rounded-2xl flex items-center justify-center">
                <i class="pi pi-car text-white text-xl"></i>
              </div>
              <h3 class="text-2xl font-bold text-gray-900 dark:text-white">{{ t('landing.howItWorks.forDrivers') }}</h3>
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
              {{ t('landing.howItWorks.registerAsDriver') }}
              <i class="pi pi-arrow-right ml-2"></i>
            </router-link>
          </div>
        </div>
      </div>
    </section>

    <!-- Benefits — DUAL AUDIENCE -->
    <section id="beneficios" class="py-20 lg:py-28 bg-canvas dark:bg-gray-800">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center max-w-2xl mx-auto mb-16">
          <p class="text-sm font-semibold text-primary uppercase tracking-wider mb-3">{{ t('landing.benefits.label') }}</p>
          <h2 class="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white">
            {{ t('landing.benefits.title') }}
          </h2>
        </div>

        <div class="grid lg:grid-cols-2 gap-12">
          <!-- Empresas benefits -->
          <div>
            <h3 class="text-xl font-bold text-gray-900 dark:text-white mb-6 flex items-center gap-2">
              <i class="pi pi-building text-blue-600"></i>
              {{ t('landing.benefits.forCompanies') }}
            </h3>
            <div class="grid gap-4">
              <div
                v-for="benefit in benefitsEmpresas"
                :key="benefit.title"
                class="bg-white dark:bg-gray-700 rounded-2xl p-6 border border-gray-100 dark:border-gray-600 hover:shadow-lg hover:border-blue-100 dark:hover:border-blue-800 transition-all duration-300"
              >
                <div class="flex items-start gap-4">
                  <div class="w-12 h-12 rounded-2xl flex items-center justify-center flex-shrink-0" :class="benefit.bgColor">
                    <i :class="[benefit.icon, benefit.iconColor]" class="text-xl"></i>
                  </div>
                  <div>
                    <h4 class="font-semibold text-gray-900 dark:text-white">{{ benefit.title }}</h4>
                    <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">{{ benefit.description }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Camioneros benefits -->
          <div>
            <h3 class="text-xl font-bold text-gray-900 dark:text-white mb-6 flex items-center gap-2">
              <i class="pi pi-car text-emerald-600"></i>
              {{ t('landing.benefits.forDrivers') }}
            </h3>
            <div class="grid gap-4">
              <div
                v-for="benefit in benefitsCamioneros"
                :key="benefit.title"
                class="bg-white dark:bg-gray-700 rounded-2xl p-6 border border-gray-100 dark:border-gray-600 hover:shadow-lg hover:border-emerald-100 dark:hover:border-emerald-800 transition-all duration-300"
              >
                <div class="flex items-start gap-4">
                  <div class="w-12 h-12 rounded-2xl flex items-center justify-center flex-shrink-0" :class="benefit.bgColor">
                    <i :class="[benefit.icon, benefit.iconColor]" class="text-xl"></i>
                  </div>
                  <div>
                    <h4 class="font-semibold text-gray-900 dark:text-white">{{ benefit.title }}</h4>
                    <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">{{ benefit.description }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Pricing — Commission model -->
    <section id="precios" class="py-20 lg:py-28 bg-white dark:bg-gray-900">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center max-w-3xl mx-auto">
          <p class="text-sm font-semibold text-primary uppercase tracking-wider mb-3">{{ t('landing.pricing.label') }}</p>
          <h2 class="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white mb-4">
            {{ t('landing.pricing.title') }}
          </h2>
          <p class="text-lg text-gray-500 mb-12">
            {{ t('landing.pricing.subtitle') }}
          </p>
        </div>

        <div class="max-w-2xl mx-auto">
          <div class="relative bg-gradient-to-br from-primary-50 to-blue-50 rounded-3xl p-10 border border-primary-100 shadow-xl shadow-primary/10">
            <div class="absolute -top-3 left-1/2 -translate-x-1/2">
              <span class="px-4 py-1.5 bg-primary text-white text-sm font-semibold rounded-full">
                {{ t('landing.pricing.badge') }}
              </span>
            </div>

            <div class="text-center mb-8">
              <div class="text-6xl font-extrabold text-gray-900 mb-2">0€</div>
              <p class="text-lg text-gray-500">{{ t('landing.pricing.priceSubtitle') }}</p>
            </div>

            <div class="grid sm:grid-cols-2 gap-4 mb-8">
              <div class="flex items-start gap-3">
                <i class="pi pi-check-circle text-green-500 mt-0.5 flex-shrink-0"></i>
                <span class="text-sm text-gray-700">{{ t('landing.pricing.freeRegistration') }}</span>
              </div>
              <div class="flex items-start gap-3">
                <i class="pi pi-check-circle text-green-500 mt-0.5 flex-shrink-0"></i>
                <span class="text-sm text-gray-700">{{ t('landing.pricing.aiAnalysis') }}</span>
              </div>
              <div class="flex items-start gap-3">
                <i class="pi pi-check-circle text-green-500 mt-0.5 flex-shrink-0"></i>
                <span class="text-sm text-gray-700">{{ t('landing.pricing.gpsTracking') }}</span>
              </div>
              <div class="flex items-start gap-3">
                <i class="pi pi-check-circle text-green-500 mt-0.5 flex-shrink-0"></i>
                <span class="text-sm text-gray-700">{{ t('landing.pricing.autoInvoicing') }}</span>
              </div>
              <div class="flex items-start gap-3">
                <i class="pi pi-check-circle text-green-500 mt-0.5 flex-shrink-0"></i>
                <span class="text-sm text-gray-700">{{ t('landing.pricing.insurance') }}</span>
              </div>
              <div class="flex items-start gap-3">
                <i class="pi pi-check-circle text-green-500 mt-0.5 flex-shrink-0"></i>
                <span class="text-sm text-gray-700">{{ t('landing.pricing.dashboard') }}</span>
              </div>
            </div>

            <div class="bg-white/80 rounded-2xl p-6 text-center border border-primary-100">
              <p class="text-sm text-gray-500 mb-1">{{ t('landing.pricing.commissionModel') }}</p>
              <p class="text-gray-700 font-medium">
                {{ t('landing.pricing.commissionDesc') }}
              </p>
            </div>

            <router-link
              to="/register"
              class="mt-8 inline-flex items-center justify-center w-full px-6 py-4 bg-primary text-white font-bold text-lg rounded-xl hover:bg-primary-700 transition-all shadow-lg shadow-primary/25 hover:-translate-y-0.5"
            >
              {{ t('landing.pricing.cta') }}
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
          {{ t('landing.cta.title') }}
        </h2>
        <p class="text-lg text-primary-100 mb-8 max-w-2xl mx-auto">
          {{ t('landing.cta.subtitle') }}
        </p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <router-link
            to="/register"
            class="inline-flex items-center justify-center px-8 py-4 bg-white text-primary-700 font-bold text-lg rounded-xl hover:bg-gray-50 transition-all shadow-xl hover:-translate-y-0.5"
          >
            <i class="pi pi-building mr-2"></i>
            {{ t('landing.cta.company') }}
          </router-link>
          <router-link
            to="/register"
            class="inline-flex items-center justify-center px-8 py-4 bg-white/10 text-white font-bold text-lg rounded-xl hover:bg-white/20 transition-all border border-white/20 hover:-translate-y-0.5"
          >
            <i class="pi pi-car mr-2"></i>
            {{ t('landing.cta.driver') }}
          </router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const stepsEmpresas = computed(() => [
  { title: t('landing.stepsCompanies.step1Title'), description: t('landing.stepsCompanies.step1Desc') },
  { title: t('landing.stepsCompanies.step2Title'), description: t('landing.stepsCompanies.step2Desc') },
  { title: t('landing.stepsCompanies.step3Title'), description: t('landing.stepsCompanies.step3Desc') },
  { title: t('landing.stepsCompanies.step4Title'), description: t('landing.stepsCompanies.step4Desc') },
])

const stepsCamioneros = computed(() => [
  { title: t('landing.stepsDrivers.step1Title'), description: t('landing.stepsDrivers.step1Desc') },
  { title: t('landing.stepsDrivers.step2Title'), description: t('landing.stepsDrivers.step2Desc') },
  { title: t('landing.stepsDrivers.step3Title'), description: t('landing.stepsDrivers.step3Desc') },
  { title: t('landing.stepsDrivers.step4Title'), description: t('landing.stepsDrivers.step4Desc') },
])

const benefitsEmpresas = computed(() => [
  {
    icon: 'pi pi-truck', iconColor: 'text-blue-600', bgColor: 'bg-blue-100',
    title: t('landing.benefitsCompanies.noFleetTitle'), description: t('landing.benefitsCompanies.noFleetDesc'),
  },
  {
    icon: 'pi pi-euro', iconColor: 'text-emerald-600', bgColor: 'bg-emerald-100',
    title: t('landing.benefitsCompanies.pricingTitle'), description: t('landing.benefitsCompanies.pricingDesc'),
  },
  {
    icon: 'pi pi-map-marker', iconColor: 'text-orange-600', bgColor: 'bg-orange-100',
    title: t('landing.benefitsCompanies.trackingTitle'), description: t('landing.benefitsCompanies.trackingDesc'),
  },
  {
    icon: 'pi pi-file', iconColor: 'text-violet-600', bgColor: 'bg-violet-100',
    title: t('landing.benefitsCompanies.invoicingTitle'), description: t('landing.benefitsCompanies.invoicingDesc'),
  },
])

const benefitsCamioneros = computed(() => [
  {
    icon: 'pi pi-users', iconColor: 'text-emerald-600', bgColor: 'bg-emerald-100',
    title: t('landing.benefitsDrivers.noSearchTitle'), description: t('landing.benefitsDrivers.noSearchDesc'),
  },
  {
    icon: 'pi pi-calendar', iconColor: 'text-blue-600', bgColor: 'bg-blue-100',
    title: t('landing.benefitsDrivers.flexTitle'), description: t('landing.benefitsDrivers.flexDesc'),
  },
  {
    icon: 'pi pi-shield', iconColor: 'text-rose-600', bgColor: 'bg-rose-100',
    title: t('landing.benefitsDrivers.payTitle'), description: t('landing.benefitsDrivers.payDesc'),
  },
  {
    icon: 'pi pi-mobile', iconColor: 'text-cyan-600', bgColor: 'bg-cyan-100',
    title: t('landing.benefitsDrivers.appTitle'), description: t('landing.benefitsDrivers.appDesc'),
  },
])
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
