<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useClientesStore } from '@/stores/clientes'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import ClienteTable from '@/components/clientes/ClienteTable.vue'
import ClienteDialog from '@/components/clientes/ClienteDialog.vue'
import type { Cliente, CreateClienteRequest } from '@/stores/clientes'

const clientesStore = useClientesStore()
const toast = useToast()
const route = useRoute()
const router = useRouter()

// --- Dialog state ---
const showDialog = ref(false)
const editingCliente = ref<Cliente | null>(null)

// --- Detail panel state ---
const showDetail = ref(false)
const detailCliente = ref<Cliente | null>(null)

// --- Lifecycle ---
onMounted(async () => {
  await clientesStore.fetchClientes()
  await openClienteFromQuery()
})

watch(() => route.query.clienteId, () => {
  void openClienteFromQuery()
})

// --- Handlers ---

function onNewCliente(): void {
  editingCliente.value = null
  showDialog.value = true
}

function onEditCliente(cliente: Cliente): void {
  editingCliente.value = cliente
  showDialog.value = true
}

function onViewCliente(cliente: Cliente): void {
  detailCliente.value = cliente
  showDetail.value = true
}

async function openClienteFromQuery(): Promise<void> {
  const rawId = route.query.clienteId
  const clienteId = typeof rawId === 'string' ? Number.parseInt(rawId, 10) : NaN
  if (Number.isNaN(clienteId)) return

  const cliente = await clientesStore.fetchCliente(clienteId)
  if (cliente) {
    onViewCliente(cliente)
    await clearQueryParam('clienteId')
  }
}

async function clearQueryParam(param: string): Promise<void> {
  const nextQuery = { ...route.query }
  delete nextQuery[param]
  await router.replace({ query: nextQuery })
}

async function onViewPortes(cliente: Cliente): Promise<void> {
  await router.push({
    path: '/portes',
    query: { clienteId: String(cliente.id) },
  })
}

async function onSaveCliente(data: CreateClienteRequest): Promise<void> {
  try {
    if (editingCliente.value) {
      await clientesStore.updateCliente(editingCliente.value.id, {
        nombreEmpresa: data.nombreEmpresa,
        cif: data.cif,
        emailContacto: data.emailContacto,
        telefono: data.telefono,
        direccion: data.direccion,
        ciudad: data.ciudad,
        codigoPostal: data.codigoPostal,
        pais: data.pais,
      })
      toast.add({
        severity: 'success',
        summary: 'Cliente actualizado',
        detail: `El cliente #${editingCliente.value.id} se ha actualizado correctamente.`,
        life: 3000,
      })
    } else {
      const created = await clientesStore.createCliente(data)
      toast.add({
        severity: 'success',
        summary: 'Cliente creado',
        detail: `El cliente #${created.id} se ha creado correctamente.`,
        life: 3000,
      })
    }
    showDialog.value = false
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'No se pudo guardar el cliente. Inténtalo de nuevo.',
      life: 5000,
    })
  }
}

</script>

<template>
  <div class="h-full min-h-0 flex flex-col gap-6 overflow-hidden">
    <!-- Page Header -->
    <div class="shrink-0 flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl bg-teal-50 text-teal-600 flex items-center justify-center">
          <i class="pi pi-building text-2xl"></i>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">Clientes</h1>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">Gestión de clientes y sus portes</p>
        </div>
      </div>
      <Button
        label="Nuevo Cliente"
        icon="pi pi-plus"
        @click="onNewCliente"
      />
    </div>

    <!-- Mock Data Banner -->
    <div
      v-if="clientesStore.usingMockData"
      class="shrink-0 flex items-center gap-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <!-- Data Table -->
    <div class="flex-1 min-h-0 overflow-auto">
      <ClienteTable
        :clientes="clientesStore.clientes"
        :loading="clientesStore.loading"
        @view="onViewCliente"
        @edit="onEditCliente"
        @view-portes="onViewPortes"
      />
    </div>

    <!-- Create/Edit Dialog -->
    <ClienteDialog
      v-model:visible="showDialog"
      :cliente="editingCliente"
      :saving="clientesStore.saving"
      @save="onSaveCliente"
    />

    <!-- Detail Dialog -->
    <Dialog
      v-model:visible="showDetail"
      :header="`Cliente #${detailCliente?.id ?? ''}`"
      :modal="true"
      :closable="true"
      :style="{ width: '650px' }"
    >
      <div class="pt-2 grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <p class="text-xs uppercase tracking-wide text-gray-400 mb-1">Empresa</p>
          <p class="text-sm text-gray-800 dark:text-gray-100">{{ detailCliente?.nombreEmpresa || '—' }}</p>
        </div>
        <div>
          <p class="text-xs uppercase tracking-wide text-gray-400 mb-1">CIF/NIF</p>
          <p class="text-sm text-gray-700 dark:text-gray-300">{{ detailCliente?.cif || '—' }}</p>
        </div>
        <div>
          <p class="text-xs uppercase tracking-wide text-gray-400 mb-1">Email</p>
          <p class="text-sm text-gray-700 dark:text-gray-300">{{ detailCliente?.emailContacto || '—' }}</p>
        </div>
        <div>
          <p class="text-xs uppercase tracking-wide text-gray-400 mb-1">Teléfono</p>
          <p class="text-sm text-gray-700 dark:text-gray-300">{{ detailCliente?.telefono || '—' }}</p>
        </div>
        <div class="md:col-span-2">
          <p class="text-xs uppercase tracking-wide text-gray-400 mb-1">Dirección</p>
          <p class="text-sm text-gray-700 dark:text-gray-300">{{ detailCliente?.direccion || '—' }}</p>
        </div>
        <div>
          <p class="text-xs uppercase tracking-wide text-gray-400 mb-1">Ciudad</p>
          <p class="text-sm text-gray-700 dark:text-gray-300">{{ detailCliente?.ciudad || '—' }}</p>
        </div>
        <div>
          <p class="text-xs uppercase tracking-wide text-gray-400 mb-1">Código Postal</p>
          <p class="text-sm text-gray-700 dark:text-gray-300">{{ detailCliente?.codigoPostal || '—' }}</p>
        </div>
        <div>
          <p class="text-xs uppercase tracking-wide text-gray-400 mb-1">País</p>
          <p class="text-sm text-gray-700 dark:text-gray-300">{{ detailCliente?.pais || '—' }}</p>
        </div>
      </div>
    </Dialog>
  </div>
</template>
