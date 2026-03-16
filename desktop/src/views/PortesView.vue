<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { usePortesStore } from '@/stores/portes'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import PorteTable from '@/components/portes/PorteTable.vue'
import PorteDialog from '@/components/portes/PorteDialog.vue'
import PorteStatusBadge from '@/components/portes/PorteStatusBadge.vue'
import type { Porte, CreatePorteRequest, EstadoPorte } from '@/stores/portes'

const portesStore = usePortesStore()
const authStore = useAuthStore()
const toast = useToast()

// --- Dialog state ---
const showDialog = ref(false)
const editingPorte = ref<Porte | null>(null)

// --- Detail panel state ---
const showDetail = ref(false)
const detailPorte = ref<Porte | null>(null)

// --- Delete confirmation ---
const showDeleteConfirm = ref(false)
const deletingPorte = ref<Porte | null>(null)

// --- Lifecycle ---
onMounted(async () => {
  await Promise.all([portesStore.fetchPortes(), portesStore.fetchReferenceData()])
})

// --- Handlers ---

function onNewPorte(): void {
  editingPorte.value = null
  showDialog.value = true
}

function onEditPorte(porte: Porte): void {
  editingPorte.value = porte
  showDialog.value = true
}

function onViewPorte(porte: Porte): void {
  detailPorte.value = porte
  showDetail.value = true
}

function onConfirmDelete(porte: Porte): void {
  deletingPorte.value = porte
  showDeleteConfirm.value = true
}

async function onSavePorte(data: CreatePorteRequest & { estado?: EstadoPorte }): Promise<void> {
  try {
    if (editingPorte.value) {
      await portesStore.updatePorte(editingPorte.value.id, {
        origen: data.origen,
        destino: data.destino,
        descripcionCliente: data.descripcionCliente,
        fechaRecogida: data.fechaRecogida,
        fechaEntrega: data.fechaEntrega,
        estado: data.estado,
      })
      toast.add({
        severity: 'success',
        summary: 'Porte actualizado',
        detail: `El porte #${editingPorte.value.id} se ha actualizado correctamente.`,
        life: 3000,
      })
    } else {
      const created = await portesStore.createPorte(data)
      toast.add({
        severity: 'success',
        summary: 'Porte creado',
        detail: `El porte #${created.id} se ha creado correctamente.`,
        life: 3000,
      })
    }
    showDialog.value = false
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'No se pudo guardar el porte. Inténtalo de nuevo.',
      life: 5000,
    })
  }
}

async function onDeletePorte(): Promise<void> {
  if (!deletingPorte.value) return
  try {
    const id = deletingPorte.value.id
    await portesStore.deletePorte(id)
    toast.add({
      severity: 'success',
      summary: 'Porte eliminado',
      detail: `El porte #${id} se ha eliminado correctamente.`,
      life: 3000,
    })
    showDeleteConfirm.value = false
    deletingPorte.value = null
    // Also close detail if viewing the deleted porte
    if (detailPorte.value?.id === id) {
      showDetail.value = false
      detailPorte.value = null
    }
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'No se pudo eliminar el porte. Inténtalo de nuevo.',
      life: 5000,
    })
  }
}

function formatDateTime(dateStr: string | undefined): string {
  if (!dateStr || dateStr === '—') return '—'
  try {
    const date = new Date(dateStr)
    return date.toLocaleString('es-ES', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return dateStr
  }
}

function getConductorFullName(porte: Porte): string {
  if (!porte.conductor) return 'Sin asignar'
  const c = porte.conductor
  return `${c.nombre}${c.apellidos ? ' ' + c.apellidos : ''}`
}

// Suppress unused variable warnings — authStore is used in template
void authStore
</script>

<template>
  <div class="space-y-6">
    <!-- Page Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
          <i class="pi pi-truck text-2xl"></i>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800">Portes</h1>
          <p class="text-sm text-gray-500 mt-0.5">Gestión de portes y envíos</p>
        </div>
      </div>
      <Button
        label="Nuevo Porte"
        icon="pi pi-plus"
        @click="onNewPorte"
      />
    </div>

    <!-- Mock Data Banner -->
    <div
      v-if="portesStore.usingMockData"
      class="flex items-center gap-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <!-- Data Table -->
    <PorteTable
      :portes="portesStore.portes"
      :loading="portesStore.loading"
      @view="onViewPorte"
      @edit="onEditPorte"
      @delete="onConfirmDelete"
    />

    <!-- Create/Edit Dialog -->
    <PorteDialog
      v-model:visible="showDialog"
      :porte="editingPorte"
      :conductores="portesStore.conductores"
      :vehiculos="portesStore.vehiculos"
      :clientes="portesStore.clientes"
      :saving="portesStore.saving"
      @save="onSavePorte"
    />

    <!-- Detail Slide Panel -->
    <Dialog
      v-model:visible="showDetail"
      :header="`Detalle del Porte #${detailPorte?.id ?? ''}`"
      :modal="true"
      :closable="true"
      :style="{ width: '700px' }"
    >
      <div v-if="detailPorte" class="space-y-6 pt-2">
        <!-- Status & ID Header -->
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <span class="text-lg font-bold text-gray-800">#{{ detailPorte.id }}</span>
            <PorteStatusBadge :estado="detailPorte.estado" />
          </div>
          <div class="flex items-center gap-2">
            <Button
              icon="pi pi-pencil"
              severity="secondary"
              text
              rounded
              size="small"
              v-tooltip.top="'Editar'"
              @click="showDetail = false; onEditPorte(detailPorte!)"
            />
          </div>
        </div>

        <!-- Route Info -->
        <div class="bg-gray-50 rounded-xl p-5">
          <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">Ruta</h4>
          <div class="flex items-center gap-4">
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-1">
                <i class="pi pi-map-marker text-blue-500"></i>
                <span class="text-sm text-gray-500">Origen</span>
              </div>
              <p class="text-gray-800 font-medium">{{ detailPorte.origen }}</p>
            </div>
            <div class="flex-shrink-0">
              <i class="pi pi-arrow-right text-gray-300 text-xl"></i>
            </div>
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-1">
                <i class="pi pi-flag text-emerald-500"></i>
                <span class="text-sm text-gray-500">Destino</span>
              </div>
              <p class="text-gray-800 font-medium">{{ detailPorte.destino }}</p>
            </div>
          </div>
          <div v-if="detailPorte.distanciaKm" class="mt-3 pt-3 border-t border-gray-200">
            <span class="text-sm text-gray-500">Distancia:</span>
            <span class="text-sm font-medium text-gray-700 ml-1">{{ detailPorte.distanciaKm }} km</span>
          </div>
        </div>

        <!-- Details Grid -->
        <div class="grid grid-cols-2 gap-5">
          <!-- Conductor -->
          <div class="bg-gray-50 rounded-xl p-4">
            <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2">Conductor</h4>
            <div class="flex items-center gap-3">
              <div
                class="w-10 h-10 rounded-full flex items-center justify-center"
                :class="detailPorte.conductor ? 'bg-primary/10' : 'bg-gray-200'"
              >
                <span
                  class="text-sm font-semibold"
                  :class="detailPorte.conductor ? 'text-primary' : 'text-gray-400'"
                >
                  {{ detailPorte.conductor ? detailPorte.conductor.nombre.charAt(0).toUpperCase() : '?' }}
                </span>
              </div>
              <div>
                <p class="text-gray-800 font-medium">{{ getConductorFullName(detailPorte) }}</p>
                <p v-if="detailPorte.conductor?.telefono" class="text-sm text-gray-500">
                  {{ detailPorte.conductor.telefono }}
                </p>
              </div>
            </div>
          </div>

          <!-- Cliente -->
          <div class="bg-gray-50 rounded-xl p-4">
            <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2">Cliente</h4>
            <p class="text-gray-800 font-medium">{{ detailPorte.cliente?.nombreEmpresa ?? 'Sin cliente' }}</p>
            <p v-if="detailPorte.cliente?.emailContacto" class="text-sm text-gray-500 mt-0.5">
              {{ detailPorte.cliente.emailContacto }}
            </p>
          </div>
        </div>

        <!-- Dates -->
        <div class="grid grid-cols-3 gap-4">
          <div>
            <span class="text-sm text-gray-500">Creación</span>
            <p class="text-sm font-medium text-gray-700">{{ formatDateTime(detailPorte.fechaCreacion) }}</p>
          </div>
          <div>
            <span class="text-sm text-gray-500">Recogida</span>
            <p class="text-sm font-medium text-gray-700">{{ formatDateTime(detailPorte.fechaRecogida) }}</p>
          </div>
          <div>
            <span class="text-sm text-gray-500">Entrega</span>
            <p class="text-sm font-medium text-gray-700">{{ formatDateTime(detailPorte.fechaEntrega) }}</p>
          </div>
        </div>

        <!-- Pricing -->
        <div v-if="detailPorte.precio" class="bg-gray-50 rounded-xl p-4">
          <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2">Económico</h4>
          <div class="grid grid-cols-3 gap-4">
            <div>
              <span class="text-sm text-gray-500">Precio Base</span>
              <p class="text-gray-800 font-medium">{{ detailPorte.precio?.toFixed(2) }} €</p>
            </div>
            <div v-if="detailPorte.ajustePrecio">
              <span class="text-sm text-gray-500">Ajuste</span>
              <p class="text-gray-800 font-medium">{{ detailPorte.ajustePrecio.toFixed(2) }} €</p>
            </div>
            <div>
              <span class="text-sm text-gray-500">Total</span>
              <p class="text-lg font-bold text-primary">
                {{ ((detailPorte.precio ?? 0) + (detailPorte.ajustePrecio ?? 0)).toFixed(2) }} €
              </p>
            </div>
          </div>
        </div>

        <!-- Description -->
        <div v-if="detailPorte.descripcionCliente">
          <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2">Descripción</h4>
          <p class="text-gray-700 text-sm bg-gray-50 rounded-xl p-4">{{ detailPorte.descripcionCliente }}</p>
        </div>
      </div>
    </Dialog>

    <!-- Delete Confirmation Dialog -->
    <Dialog
      v-model:visible="showDeleteConfirm"
      header="Confirmar eliminación"
      :modal="true"
      :closable="true"
      :style="{ width: '450px' }"
    >
      <div class="flex items-start gap-4 py-2">
        <div class="w-10 h-10 rounded-full bg-red-50 flex items-center justify-center flex-shrink-0">
          <i class="pi pi-exclamation-triangle text-red-500"></i>
        </div>
        <div>
          <p class="text-gray-800 font-medium">
            ¿Eliminar el porte #{{ deletingPorte?.id }}?
          </p>
          <p class="text-sm text-gray-500 mt-1">
            Esta acción no se puede deshacer. Se eliminará permanentemente el porte
            de {{ deletingPorte?.origen }} a {{ deletingPorte?.destino }}.
          </p>
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3">
          <Button
            label="Cancelar"
            severity="secondary"
            text
            @click="showDeleteConfirm = false"
          />
          <Button
            label="Eliminar"
            severity="danger"
            icon="pi pi-trash"
            :loading="portesStore.saving"
            @click="onDeletePorte"
          />
        </div>
      </template>
    </Dialog>
  </div>
</template>
