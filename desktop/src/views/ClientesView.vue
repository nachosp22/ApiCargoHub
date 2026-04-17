<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useClientesStore } from '@/stores/clientes'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import ClienteTable from '@/components/clientes/ClienteTable.vue'
import ClienteDialog from '@/components/clientes/ClienteDialog.vue'
import ClientePortesTab from '@/components/clientes/ClientePortesTab.vue'
import type { Cliente, CreateClienteRequest } from '@/stores/clientes'

const clientesStore = useClientesStore()
const toast = useToast()

// --- Dialog state ---
const showDialog = ref(false)
const editingCliente = ref<Cliente | null>(null)

// --- Portes panel state ---
const showPortes = ref(false)
const portesCliente = ref<Cliente | null>(null)

// --- Delete confirmation ---
const showDeleteConfirm = ref(false)
const deletingCliente = ref<Cliente | null>(null)

// --- Lifecycle ---
onMounted(async () => {
  await clientesStore.fetchClientes()
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

async function onViewPortes(cliente: Cliente): Promise<void> {
  portesCliente.value = cliente
  showPortes.value = true
  await clientesStore.fetchClientePortes(cliente.id)
}

function onConfirmDelete(cliente: Cliente): void {
  deletingCliente.value = cliente
  showDeleteConfirm.value = true
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

async function onDeleteCliente(): Promise<void> {
  if (!deletingCliente.value) return
  try {
    const cliente = deletingCliente.value
    await clientesStore.deleteCliente(cliente.id)
    toast.add({
      severity: 'success',
      summary: 'Cliente eliminado',
      detail: `${cliente.nombreEmpresa} ha sido eliminado.`,
      life: 3000,
    })
    showDeleteConfirm.value = false
    deletingCliente.value = null
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'No se pudo eliminar el cliente. Inténtalo de nuevo.',
      life: 5000,
    })
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Page Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl bg-teal-50 text-teal-600 flex items-center justify-center">
          <i class="pi pi-building text-2xl"></i>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800">Clientes</h1>
          <p class="text-sm text-gray-500 mt-0.5">Gestión de clientes y sus portes</p>
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
      class="flex items-center gap-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-3 gap-4">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-teal-50 text-teal-600 flex items-center justify-center">
            <i class="pi pi-building text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800">{{ clientesStore.totalClientes }}</p>
            <p class="text-xs text-gray-500">Total Clientes</p>
          </div>
        </div>
      </div>
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
            <i class="pi pi-envelope text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800">{{ clientesStore.clientes.filter(c => c.emailContacto).length }}</p>
            <p class="text-xs text-gray-500">Con Email</p>
          </div>
        </div>
      </div>
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <i class="pi pi-phone text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800">{{ clientesStore.clientes.filter(c => c.telefono).length }}</p>
            <p class="text-xs text-gray-500">Con Teléfono</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Data Table -->
    <ClienteTable
      :clientes="clientesStore.clientes"
      :loading="clientesStore.loading"
      @edit="onEditCliente"
      @view-portes="onViewPortes"
      @delete="onConfirmDelete"
    />

    <!-- Create/Edit Dialog -->
    <ClienteDialog
      v-model:visible="showDialog"
      :cliente="editingCliente"
      :saving="clientesStore.saving"
      @save="onSaveCliente"
    />

    <!-- Portes Dialog -->
    <Dialog
      v-model:visible="showPortes"
      :header="`Portes de ${portesCliente?.nombreEmpresa ?? ''}`"
      :modal="true"
      :closable="true"
      :style="{ width: '800px' }"
    >
      <div class="pt-2">
        <ClientePortesTab
          :portes="clientesStore.clientePortes"
          :loading="clientesStore.loadingPortes"
        />
      </div>
    </Dialog>

    <!-- Delete Confirmation Dialog -->
    <Dialog
      v-model:visible="showDeleteConfirm"
      header="Eliminar Cliente"
      :modal="true"
      :closable="true"
      :style="{ width: '450px' }"
    >
      <div class="flex items-start gap-4 py-2">
        <div class="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 bg-red-50">
          <i class="pi pi-trash text-red-500"></i>
        </div>
        <div>
          <p class="text-gray-800 font-medium">
            ¿Eliminar a {{ deletingCliente?.nombreEmpresa }}?
          </p>
          <p class="text-sm text-gray-500 mt-1">
            Esta acción no se puede deshacer. Se eliminará el cliente y toda su información asociada.
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
            :loading="clientesStore.saving"
            @click="onDeleteCliente"
          />
        </div>
      </template>
    </Dialog>
  </div>
</template>
