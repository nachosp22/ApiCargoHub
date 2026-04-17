<template>
  <div class="min-h-screen flex items-center justify-center bg-canvas px-4 py-8">
    <div class="w-full max-w-2xl bg-white rounded-2xl shadow-lg p-8">
      <div class="text-center mb-8">
        <div
          class="w-12 h-12 bg-gradient-to-br from-primary-500 to-primary-700 rounded-xl flex items-center justify-center mx-auto mb-4"
        >
          <i class="pi pi-truck text-white text-xl"></i>
        </div>
        <h1 class="text-2xl font-bold text-gray-900">Crear Cuenta</h1>
        <p class="text-sm text-gray-500 mt-1">Únete a CargoHub</p>
      </div>

      <!-- Error message -->
      <div v-if="errorMessage" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-sm text-red-700">{{ errorMessage }}</p>
      </div>

      <!-- Success message (conductor pending approval) -->
      <div v-if="registrationSuccess" class="mb-4 p-6 bg-green-50 border border-green-200 rounded-xl text-center">
        <div class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <i class="pi pi-check-circle text-green-600 text-3xl"></i>
        </div>
        <h3 class="text-lg font-semibold text-green-800 mb-2">Registro completado</h3>
        <p class="text-sm text-green-700">
          Tu cuenta será revisada por nuestro equipo. Te notificaremos cuando esté aprobada.
        </p>
        <router-link
          to="/login"
          class="inline-flex items-center mt-4 text-sm text-primary hover:underline font-medium"
        >
          <i class="pi pi-arrow-left mr-1"></i>
          Volver al inicio de sesión
        </router-link>
      </div>

      <!-- Step 1: Account type selection -->
      <div v-if="!selectedRole && !registrationSuccess" class="space-y-4">
        <p class="text-center text-gray-600 mb-6">¿Qué tipo de cuenta quieres crear?</p>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <button
            type="button"
            @click="selectedRole = 'CLIENTE'"
            class="group relative p-6 border-2 border-gray-200 rounded-2xl hover:border-primary hover:shadow-lg transition-all duration-300 text-left"
          >
            <div class="w-14 h-14 bg-blue-100 rounded-2xl flex items-center justify-center mb-4 group-hover:bg-blue-200 transition-colors">
              <i class="pi pi-building text-blue-600 text-2xl"></i>
            </div>
            <h3 class="text-lg font-semibold text-gray-900 mb-1">Empresa</h3>
            <p class="text-sm text-gray-500">Necesito enviar mercancía</p>
          </button>

          <button
            type="button"
            @click="selectedRole = 'CONDUCTOR'"
            class="group relative p-6 border-2 border-gray-200 rounded-2xl hover:border-primary hover:shadow-lg transition-all duration-300 text-left"
          >
            <div class="w-14 h-14 bg-emerald-100 rounded-2xl flex items-center justify-center mb-4 group-hover:bg-emerald-200 transition-colors">
              <i class="pi pi-car text-emerald-600 text-2xl"></i>
            </div>
            <h3 class="text-lg font-semibold text-gray-900 mb-1">Transportista</h3>
            <p class="text-sm text-gray-500">Quiero transportar mercancía</p>
          </button>
        </div>
      </div>

      <!-- Step 2: Registration form -->
      <form v-if="selectedRole && !registrationSuccess" @submit.prevent="handleRegister" class="space-y-6">
        <!-- Back button -->
        <button
          type="button"
          @click="selectedRole = null"
          class="inline-flex items-center text-sm text-gray-500 hover:text-gray-700 transition-colors"
        >
          <i class="pi pi-arrow-left mr-1"></i>
          Cambiar tipo de cuenta
        </button>

        <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-sm font-medium"
          :class="selectedRole === 'CLIENTE' ? 'bg-blue-100 text-blue-700' : 'bg-emerald-100 text-emerald-700'"
        >
          <i :class="selectedRole === 'CLIENTE' ? 'pi pi-building' : 'pi pi-car'"></i>
          {{ selectedRole === 'CLIENTE' ? 'Registro de Empresa' : 'Registro de Transportista' }}
        </div>

        <!-- Account Section (shared) -->
        <fieldset class="space-y-4">
          <legend class="text-lg font-semibold text-gray-800 border-b border-gray-200 pb-2 w-full">
            Datos de acceso
          </legend>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="md:col-span-2">
              <label for="email" class="block text-sm font-medium text-gray-700 mb-1">
                Email <span class="text-red-500">*</span>
              </label>
              <InputText
                id="email"
                v-model="form.email"
                type="email"
                placeholder="tu@email.com"
                class="w-full"
                :disabled="loading"
                required
              />
            </div>

            <div>
              <label for="password" class="block text-sm font-medium text-gray-700 mb-1">
                Contraseña <span class="text-red-500">*</span>
              </label>
              <InputText
                id="password"
                v-model="form.password"
                type="password"
                placeholder="Mínimo 6 caracteres"
                class="w-full"
                :disabled="loading"
                required
                minlength="6"
              />
            </div>

            <div>
              <label for="confirmPassword" class="block text-sm font-medium text-gray-700 mb-1">
                Confirmar Contraseña <span class="text-red-500">*</span>
              </label>
              <InputText
                id="confirmPassword"
                v-model="confirmPassword"
                type="password"
                placeholder="Repite tu contraseña"
                class="w-full"
                :disabled="loading"
                required
              />
            </div>
          </div>
        </fieldset>

        <!-- CLIENTE: Company Section -->
        <fieldset v-if="selectedRole === 'CLIENTE'" class="space-y-4">
          <legend class="text-lg font-semibold text-gray-800 border-b border-gray-200 pb-2 w-full">
            Datos de la empresa
          </legend>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label for="nombreEmpresa" class="block text-sm font-medium text-gray-700 mb-1">
                Nombre de la empresa <span class="text-red-500">*</span>
              </label>
              <InputText
                id="nombreEmpresa"
                v-model="form.nombreEmpresa"
                placeholder="Ej: Transportes García S.L."
                class="w-full"
                :disabled="loading"
                required
              />
            </div>

            <div>
              <label for="cif" class="block text-sm font-medium text-gray-700 mb-1">
                CIF / NIF <span class="text-red-500">*</span>
              </label>
              <InputText
                id="cif"
                v-model="form.cif"
                placeholder="Ej: B12345678"
                class="w-full"
                :disabled="loading"
                required
              />
            </div>

            <div class="md:col-span-2">
              <label for="direccionFiscal" class="block text-sm font-medium text-gray-700 mb-1">
                Dirección fiscal
              </label>
              <InputText
                id="direccionFiscal"
                v-model="form.direccionFiscal"
                placeholder="Calle, número, CP, Ciudad"
                class="w-full"
                :disabled="loading"
              />
            </div>

            <div>
              <label for="telefonoCliente" class="block text-sm font-medium text-gray-700 mb-1">
                Teléfono
              </label>
              <InputText
                id="telefonoCliente"
                v-model="form.telefono"
                placeholder="Ej: +34 612 345 678"
                class="w-full"
                :disabled="loading"
              />
            </div>

            <div>
              <label for="emailContacto" class="block text-sm font-medium text-gray-700 mb-1">
                Email de contacto
              </label>
              <InputText
                id="emailContacto"
                v-model="form.emailContacto"
                type="email"
                placeholder="contacto@tuempresa.com"
                class="w-full"
                :disabled="loading"
              />
            </div>

            <div>
              <label for="sector" class="block text-sm font-medium text-gray-700 mb-1">
                Sector / Industria
              </label>
              <Dropdown
                id="sector"
                v-model="form.sector"
                :options="sectorOptions"
                placeholder="Selecciona un sector"
                class="w-full"
                :disabled="loading"
                showClear
              />
            </div>
          </div>
        </fieldset>

        <!-- CONDUCTOR: Driver Section -->
        <fieldset v-if="selectedRole === 'CONDUCTOR'" class="space-y-4">
          <legend class="text-lg font-semibold text-gray-800 border-b border-gray-200 pb-2 w-full">
            Datos personales
          </legend>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label for="nombre" class="block text-sm font-medium text-gray-700 mb-1">
                Nombre <span class="text-red-500">*</span>
              </label>
              <InputText
                id="nombre"
                v-model="form.nombre"
                placeholder="Ej: Juan"
                class="w-full"
                :disabled="loading"
                required
              />
            </div>

            <div>
              <label for="apellidos" class="block text-sm font-medium text-gray-700 mb-1">
                Apellidos
              </label>
              <InputText
                id="apellidos"
                v-model="form.apellidos"
                placeholder="Ej: García López"
                class="w-full"
                :disabled="loading"
              />
            </div>

            <div>
              <label for="dni" class="block text-sm font-medium text-gray-700 mb-1">
                DNI / NIE <span class="text-red-500">*</span>
              </label>
              <InputText
                id="dni"
                v-model="form.dni"
                placeholder="Ej: 12345678A"
                class="w-full"
                :disabled="loading"
                required
              />
            </div>

            <div>
              <label for="telefonoConductor" class="block text-sm font-medium text-gray-700 mb-1">
                Teléfono <span class="text-red-500">*</span>
              </label>
              <InputText
                id="telefonoConductor"
                v-model="form.telefono"
                placeholder="Ej: +34 612 345 678"
                class="w-full"
                :disabled="loading"
                required
              />
            </div>

            <div>
              <label for="carnetConducir" class="block text-sm font-medium text-gray-700 mb-1">
                Carnet de conducir
              </label>
              <Dropdown
                id="carnetConducir"
                v-model="form.carnetConducir"
                :options="carnetOptions"
                placeholder="Tipo de carnet"
                class="w-full"
                :disabled="loading"
                showClear
              />
            </div>

            <div>
              <label for="experienciaAnios" class="block text-sm font-medium text-gray-700 mb-1">
                Años de experiencia
              </label>
              <InputText
                id="experienciaAnios"
                :modelValue="form.experienciaAnios != null ? String(form.experienciaAnios) : ''"
                @update:modelValue="(v: string | undefined) => form.experienciaAnios = v ? Number(v) : undefined"
                type="number"
                min="0"
                placeholder="Ej: 5"
                class="w-full"
                :disabled="loading"
              />
            </div>

            <div class="md:col-span-2">
              <label for="ciudadBase" class="block text-sm font-medium text-gray-700 mb-1">
                Ciudad base
              </label>
              <InputText
                id="ciudadBase"
                v-model="form.ciudadBase"
                placeholder="Ej: Madrid"
                class="w-full"
                :disabled="loading"
              />
            </div>
          </div>
        </fieldset>

        <!-- Info box for conductors -->
        <div v-if="selectedRole === 'CONDUCTOR'" class="p-4 bg-amber-50 border border-amber-200 rounded-xl">
          <div class="flex items-start gap-3">
            <i class="pi pi-info-circle text-amber-600 mt-0.5"></i>
            <p class="text-sm text-amber-700">
              Tras el registro, un administrador revisará tu perfil antes de activar tu cuenta.
              Te notificaremos por email cuando esté aprobada.
            </p>
          </div>
        </div>

        <Button
          type="submit"
          :label="selectedRole === 'CONDUCTOR' ? 'Enviar Solicitud' : 'Crear Cuenta'"
          :loading="loading"
          class="w-full"
          severity="primary"
        />
      </form>

      <div v-if="!registrationSuccess" class="mt-6 text-center">
        <router-link to="/login" class="text-sm text-primary hover:underline">
          ¿Ya tienes cuenta? Inicia sesión
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore, AuthLoginError } from '@/stores/auth'
import type { RegisterData } from '@/stores/auth'
import InputText from 'primevue/inputtext'
import Dropdown from 'primevue/dropdown'
import Button from 'primevue/button'

const router = useRouter()
const authStore = useAuthStore()

const selectedRole = ref<'CLIENTE' | 'CONDUCTOR' | null>(null)
const registrationSuccess = ref(false)

const sectorOptions = [
  'Alimentación',
  'Automoción',
  'Construcción',
  'Electrónica',
  'Farmacéutico',
  'Logística',
  'Manufactura',
  'Químico',
  'Retail',
  'Textil',
  'Otro',
]

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
    errorMessage.value = 'Las contraseñas no coinciden.'
    return
  }

  if (form.password.length < 6) {
    errorMessage.value = 'La contraseña debe tener al menos 6 caracteres.'
    return
  }

  if (selectedRole.value === 'CLIENTE') {
    if (!form.nombreEmpresa?.trim()) {
      errorMessage.value = 'El nombre de la empresa es obligatorio.'
      return
    }
    if (!form.cif?.trim()) {
      errorMessage.value = 'El CIF/NIF es obligatorio.'
      return
    }
  }

  if (selectedRole.value === 'CONDUCTOR') {
    if (!form.nombre?.trim()) {
      errorMessage.value = 'El nombre es obligatorio.'
      return
    }
    if (!form.dni?.trim()) {
      errorMessage.value = 'El DNI/NIE es obligatorio.'
      return
    }
    if (!form.telefono?.trim()) {
      errorMessage.value = 'El teléfono es obligatorio.'
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
      errorMessage.value = 'Error inesperado al crear la cuenta.'
    }
  } finally {
    loading.value = false
  }
}
</script>
