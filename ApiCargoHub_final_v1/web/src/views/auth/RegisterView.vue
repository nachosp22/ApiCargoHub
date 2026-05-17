<template>
  <div class="relative min-h-screen flex items-center justify-center px-4 py-8 overflow-hidden">
    <img
      src="/assets/brand/login-bg.png"
      alt=""
      aria-hidden="true"
      class="absolute inset-0 h-full w-full object-cover"
    />
    <div class="absolute inset-0 bg-gradient-to-br from-slate-950/70 via-slate-900/60 to-primary-950/70"></div>

    <div class="relative w-full max-w-2xl bg-white/95 dark:bg-gray-900/90 backdrop-blur-sm rounded-2xl shadow-xl dark:shadow-black/40 p-8 border border-white/20 dark:border-gray-700/50">
      <div class="text-center mb-8">
        <img
          src="/assets/brand/logo.png"
          alt="CargoHub"
          class="w-12 h-12 object-contain mx-auto mb-4"
        />
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">{{ t('auth.register.title') }}</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">{{ t('auth.register.subtitle') }}</p>
      </div>

      <div v-if="errorMessage" class="mb-4 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
        <p class="text-sm text-red-700 dark:text-red-400">{{ errorMessage }}</p>
      </div>

      <div v-if="registrationSuccess" class="mb-4 p-6 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-xl text-center">
        <div class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <i class="pi pi-check-circle text-green-600 text-3xl"></i>
        </div>
        <h3 class="text-lg font-semibold text-green-800 mb-2">{{ t('auth.register.successTitle') }}</h3>
        <p class="text-sm text-green-700">
          {{ t('auth.register.successMessage') }}
        </p>
        <router-link
          to="/login"
          class="inline-flex items-center mt-4 text-sm text-primary hover:underline font-medium"
        >
          <i class="pi pi-arrow-left mr-1"></i>
          {{ t('auth.register.backToLogin') }}
        </router-link>
      </div>

      <div v-if="!selectedRole && !registrationSuccess" class="space-y-4">
        <p class="text-center text-gray-600 mb-6">{{ t('auth.register.accountTypeQuestion') }}</p>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <button
            type="button"
            @click="selectedRole = 'CLIENTE'"
            class="group relative p-6 border-2 border-gray-200 rounded-2xl hover:border-primary hover:shadow-lg transition-all duration-300 text-left"
          >
            <div class="w-14 h-14 bg-blue-100 rounded-2xl flex items-center justify-center mb-4 group-hover:bg-blue-200 transition-colors">
              <i class="pi pi-building text-blue-600 text-2xl"></i>
            </div>
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-1">{{ t('auth.register.company') }}</h3>
            <p class="text-sm text-gray-500">{{ t('auth.register.companyDesc') }}</p>
          </button>

          <button
            type="button"
            @click="selectedRole = 'CONDUCTOR'"
            class="group relative p-6 border-2 border-gray-200 rounded-2xl hover:border-primary hover:shadow-lg transition-all duration-300 text-left"
          >
            <div class="w-14 h-14 bg-emerald-100 rounded-2xl flex items-center justify-center mb-4 group-hover:bg-emerald-200 transition-colors">
              <i class="pi pi-car text-emerald-600 text-2xl"></i>
            </div>
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-1">{{ t('auth.register.driver') }}</h3>
            <p class="text-sm text-gray-500">{{ t('auth.register.driverDesc') }}</p>
          </button>
        </div>
      </div>

      <form v-if="selectedRole && !registrationSuccess" @submit.prevent="handleRegister" class="space-y-6">
        <button
          type="button"
          @click="selectedRole = null"
          class="inline-flex items-center text-sm text-gray-500 hover:text-gray-700 transition-colors"
        >
          <i class="pi pi-arrow-left mr-1"></i>
          {{ t('auth.register.changeAccountType') }}
        </button>

        <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-sm font-medium"
          :class="selectedRole === 'CLIENTE' ? 'bg-blue-100 text-blue-700' : 'bg-emerald-100 text-emerald-700'"
        >
          <i :class="selectedRole === 'CLIENTE' ? 'pi pi-building' : 'pi pi-car'"></i>
          {{ selectedRole === 'CLIENTE' ? t('auth.register.companyRegistration') : t('auth.register.driverRegistration') }}
        </div>

        <fieldset class="space-y-4">
          <legend class="text-lg font-semibold text-gray-800 dark:text-gray-200 border-b border-gray-200 dark:border-gray-700 pb-2 w-full">
            {{ t('auth.register.accessData') }}
          </legend>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="md:col-span-2">
              <label for="email" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.email') }} <span class="text-red-500">*</span>
              </label>
              <InputText
                id="email"
                v-model="form.email"
                type="email"
                :placeholder="t('auth.login.emailPlaceholder')"
                class="w-full"
                :disabled="loading"
                required
              />
            </div>

            <div>
              <label for="password" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.password') }} <span class="text-red-500">*</span>
              </label>
              <InputText
                id="password"
                v-model="form.password"
                type="password"
                :placeholder="t('auth.register.passwordPlaceholder')"
                class="w-full"
                :disabled="loading"
                required
                minlength="6"
              />
            </div>

            <div>
              <label for="confirmPassword" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.confirmPassword') }} <span class="text-red-500">*</span>
              </label>
              <InputText
                id="confirmPassword"
                v-model="confirmPassword"
                type="password"
                :placeholder="t('auth.register.confirmPasswordPlaceholder')"
                class="w-full"
                :disabled="loading"
                required
              />
            </div>
          </div>
        </fieldset>

        <fieldset v-if="selectedRole === 'CLIENTE'" class="space-y-4">
          <legend class="text-lg font-semibold text-gray-800 dark:text-gray-200 border-b border-gray-200 dark:border-gray-700 pb-2 w-full">
            {{ t('auth.register.companyData') }}
          </legend>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label for="nombreEmpresa" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.companyName') }} <span class="text-red-500">*</span>
              </label>
              <InputText
                id="nombreEmpresa"
                v-model="form.nombreEmpresa"
                :placeholder="t('auth.register.companyNamePlaceholder')"
                class="w-full"
                :disabled="loading"
                required
              />
            </div>

            <div>
              <label for="cif" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.cif') }} <span class="text-red-500">*</span>
              </label>
              <InputText
                id="cif"
                v-model="form.cif"
                :placeholder="t('auth.register.cifPlaceholder')"
                class="w-full"
                :disabled="loading"
                required
              />
            </div>

            <div class="md:col-span-2">
              <label for="direccionFiscal" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.fiscalAddress') }}
              </label>
              <InputText
                id="direccionFiscal"
                v-model="form.direccionFiscal"
                :placeholder="t('auth.register.fiscalAddressPlaceholder')"
                class="w-full"
                :disabled="loading"
              />
            </div>

            <div>
              <label for="telefonoCliente" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.phone') }}
              </label>
              <InputText
                id="telefonoCliente"
                v-model="form.telefono"
                :placeholder="t('auth.register.phonePlaceholder')"
                class="w-full"
                :disabled="loading"
              />
            </div>

            <div>
              <label for="emailContacto" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.contactEmail') }}
              </label>
              <InputText
                id="emailContacto"
                v-model="form.emailContacto"
                type="email"
                :placeholder="t('auth.register.contactEmailPlaceholder')"
                class="w-full"
                :disabled="loading"
              />
            </div>

            <div>
              <label for="sector" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.sector') }}
              </label>
              <Dropdown
                id="sector"
                v-model="form.sector"
                :options="sectorOptions"
                :placeholder="t('auth.register.sectorPlaceholder')"
                class="w-full"
                :disabled="loading"
                showClear
              />
            </div>
          </div>
        </fieldset>

        <fieldset v-if="selectedRole === 'CONDUCTOR'" class="space-y-4">
          <legend class="text-lg font-semibold text-gray-800 dark:text-gray-200 border-b border-gray-200 dark:border-gray-700 pb-2 w-full">
            {{ t('auth.register.personalData') }}
          </legend>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label for="nombre" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.name') }} <span class="text-red-500">*</span>
              </label>
              <InputText
                id="nombre"
                v-model="form.nombre"
                :placeholder="t('auth.register.namePlaceholder')"
                class="w-full"
                :disabled="loading"
                required
              />
            </div>

            <div>
              <label for="apellidos" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.surname') }}
              </label>
              <InputText
                id="apellidos"
                v-model="form.apellidos"
                :placeholder="t('auth.register.surnamePlaceholder')"
                class="w-full"
                :disabled="loading"
              />
            </div>

            <div>
              <label for="dni" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.dni') }} <span class="text-red-500">*</span>
              </label>
              <InputText
                id="dni"
                v-model="form.dni"
                :placeholder="t('auth.register.dniPlaceholder')"
                class="w-full"
                :disabled="loading"
                required
              />
            </div>

            <div>
              <label for="telefonoConductor" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.phone') }} <span class="text-red-500">*</span>
              </label>
              <InputText
                id="telefonoConductor"
                v-model="form.telefono"
                :placeholder="t('auth.register.phonePlaceholder')"
                class="w-full"
                :disabled="loading"
                required
              />
            </div>

            <div>
              <label for="carnetConducir" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.driverLicense') }}
              </label>
              <Dropdown
                id="carnetConducir"
                v-model="form.carnetConducir"
                :options="carnetOptions"
                :placeholder="t('auth.register.driverLicensePlaceholder')"
                class="w-full"
                :disabled="loading"
                showClear
              />
            </div>

            <div>
              <label for="experienciaAnios" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.experience') }}
              </label>
              <InputText
                id="experienciaAnios"
                :modelValue="form.experienciaAnios != null ? String(form.experienciaAnios) : ''"
                @update:modelValue="(v: string | undefined) => form.experienciaAnios = v ? Number(v) : undefined"
                type="number"
                min="0"
                :placeholder="t('auth.register.experiencePlaceholder')"
                class="w-full"
                :disabled="loading"
              />
            </div>

            <div class="md:col-span-2">
              <label for="ciudadBase" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {{ t('auth.register.baseCity') }}
              </label>
              <InputText
                id="ciudadBase"
                v-model="form.ciudadBase"
                :placeholder="t('auth.register.baseCityPlaceholder')"
                class="w-full"
                :disabled="loading"
              />
            </div>
          </div>
        </fieldset>

        <div v-if="selectedRole === 'CONDUCTOR'" class="p-4 bg-amber-50 border border-amber-200 rounded-xl">
          <div class="flex items-start gap-3">
            <i class="pi pi-info-circle text-amber-600 mt-0.5"></i>
            <p class="text-sm text-amber-700">
              {{ t('auth.register.driverInfoBox') }}
            </p>
          </div>
        </div>

        <Button
          type="submit"
          :label="selectedRole === 'CONDUCTOR' ? t('auth.register.submitDriver') : t('auth.register.submitCompany')"
          :loading="loading"
          class="w-full"
          severity="primary"
        />
      </form>

      <div v-if="!registrationSuccess" class="mt-6 text-center">
        <router-link to="/login" class="text-sm text-primary hover:underline">
          {{ t('auth.register.hasAccount') }}
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore, AuthLoginError } from '@/stores/auth'
import type { RegisterData } from '@/stores/auth'
import InputText from 'primevue/inputtext'
import Dropdown from 'primevue/dropdown'
import Button from 'primevue/button'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const selectedRole = ref<'CLIENTE' | 'CONDUCTOR' | null>(null)
const registrationSuccess = ref(false)

const sectorOptions = computed(() => [
  t('auth.register.sectors.food'),
  t('auth.register.sectors.automotive'),
  t('auth.register.sectors.construction'),
  t('auth.register.sectors.electronics'),
  t('auth.register.sectors.pharmaceutical'),
  t('auth.register.sectors.logistics'),
  t('auth.register.sectors.manufacturing'),
  t('auth.register.sectors.chemical'),
  t('auth.register.sectors.retail'),
  t('auth.register.sectors.textile'),
  t('auth.register.sectors.other'),
])

const carnetOptions = ['B', 'C', 'C+E', 'D', 'D+E']

const form = reactive<RegisterData>({
  email: '',
  password: '',
  rol: undefined,
  nombreEmpresa: '',
  cif: '',
  direccionFiscal: '',
  telefono: '',
  emailContacto: '',
  sector: '',
  nombre: '',
  apellidos: '',
  dni: '',
  ciudadBase: '',
  carnetConducir: '',
  experienciaAnios: undefined,
})

const confirmPassword = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function handleRegister() {
  errorMessage.value = ''

  if (form.password !== confirmPassword.value) {
    errorMessage.value = t('auth.register.errorPasswordMismatch')
    return
  }

  if (form.password.length < 6) {
    errorMessage.value = t('auth.register.errorPasswordLength')
    return
  }

  if (selectedRole.value === 'CLIENTE') {
    if (!form.nombreEmpresa?.trim()) {
      errorMessage.value = t('auth.register.errorCompanyNameRequired')
      return
    }
    if (!form.cif?.trim()) {
      errorMessage.value = t('auth.register.errorCifRequired')
      return
    }
  }

  if (selectedRole.value === 'CONDUCTOR') {
    if (!form.nombre?.trim()) {
      errorMessage.value = t('auth.register.errorNameRequired')
      return
    }
    if (!form.dni?.trim()) {
      errorMessage.value = t('auth.register.errorDniRequired')
      return
    }
    if (!form.telefono?.trim()) {
      errorMessage.value = t('auth.register.errorPhoneRequired')
      return
    }
  }

  form.rol = selectedRole.value ?? 'CLIENTE'
  loading.value = true

  try {
    const result = await authStore.register(form)
    if (result.pendingApproval) {
      registrationSuccess.value = true
    } else {
      await router.push('/portal/dashboard')
    }
  } catch (err) {
    if (err instanceof AuthLoginError) {
      errorMessage.value = err.message
    } else {
      errorMessage.value = t('auth.register.unexpectedError')
    }
  } finally {
    loading.value = false
  }
}
</script>
