<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useConductoresStore } from '@/stores/conductores'
import { useToast } from 'primevue/usetoast'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import type { Conductor } from '@/stores/conductores'

const store = useConductoresStore()
const toast = useToast()

// --- Confirmation dialog state ---
const showConfirm = ref(false)
const confirmAction = ref<'aprobar' | 'rechazar'>('aprobar')
const confirmConductor = ref<Conductor | null>(null)

onMounted(async () => {
  await store.fetchPendientesAprobacion()
})

function openConfirm(action: 'aprobar' | 'rechazar', conductor: Conductor) {
  confirmAction.value = action
  confirmConductor.value = conductor
  showConfirm.value = true
}

async function executeConfirm() {
  if (!confirmConductor.value) return
  const id = confirmConductor.value.id
  const nombre = `${confirmConductor.value.nombre} ${confirmConductor.value.apellidos}`

  try {
    if (confirmAction.value === 'aprobar') {
      await store.aprobarConductor(id)
      toast.add({ severity: 'success', summary: 'Conductor aprobado', detail: nombre, life: 3000 })
    } else {
      await store.rechazarConductor(id)
      toast.add({ severity: 'warn', summary: 'Conductor rechazado', detail: nombre, life: 3000 })
    }
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: `No se pudo ${confirmAction.value} al conductor`, life: 5000 })
  } finally {
    showConfirm.value = false
    confirmConductor.value = null
  }
}
</script>

<template>
  <div class="p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">Aprobaci&oacute;n de Conductores</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
          Conductores registrados pendientes de aprobaci&oacute;n
        </p>
      </div>
      <Tag
        v-if="store.pendientesAprobacion.length > 0"
        :value="`${store.pendientesAprobacion.length} pendientes`"
        severity="warn"
        class="text-sm"
      />
    </div>

    <!-- Empty state -->
    <div
      v-if="!store.pendientesLoading && store.pendientesAprobacion.length === 0"
      class="flex flex-col items-center justify-center py-20 text-gray-400 dark:text-gray-500"
    >
      <i class="pi pi-check-circle text-5xl mb-4"></i>
      <p class="text-lg font-medium">No hay conductores pendientes</p>
      <p class="text-sm">Todos los registros han sido procesados</p>
    </div>

    <!-- Table -->
    <DataTable
      v-else
      :value="store.pendientesAprobacion"
      :loading="store.pendientesLoading"
      stripedRows
      class="text-sm"
      :paginator="store.pendientesAprobacion.length > 10"
      :rows="10"
    >
      <Column field="nombre" header="Nombre" sortable>
        <template #body="{ data }">
          <span class="font-medium">{{ data.nombre }} {{ data.apellidos }}</span>
        </template>
      </Column>
      <Column field="email" header="Email" sortable />
      <Column field="telefono" header="Tel&eacute;fono" />
      <Column field="dni" header="DNI" sortable />
      <Column field="ciudadBase" header="Ciudad" sortable />
      <Column field="radioAccionKm" header="Radio (km)">
        <template #body="{ data }">
          {{ data.radioAccionKm || '—' }} km
        </template>
      </Column>
      <Column header="Acciones" :style="{ width: '200px' }">
        <template #body="{ data }">
          <div class="flex gap-2">
            <Button
              label="Aprobar"
              icon="pi pi-check"
              severity="success"
              size="small"
              @click="openConfirm('aprobar', data)"
            />
            <Button
              label="Rechazar"
              icon="pi pi-times"
              severity="danger"
              size="small"
              outlined
              @click="openConfirm('rechazar', data)"
            />
          </div>
        </template>
      </Column>
    </DataTable>

    <!-- Confirmation Dialog -->
    <Dialog
      v-model:visible="showConfirm"
      :header="confirmAction === 'aprobar' ? 'Confirmar aprobaci\u00f3n' : 'Confirmar rechazo'"
      :style="{ width: '28rem' }"
      modal
    >
      <p class="text-sm text-gray-600 dark:text-gray-400">
        <template v-if="confirmAction === 'aprobar'">
          &iquest;Aprobar al conductor
          <strong>{{ confirmConductor?.nombre }} {{ confirmConductor?.apellidos }}</strong>?
          Podr&aacute; acceder a la plataforma.
        </template>
        <template v-else>
          &iquest;Rechazar al conductor
          <strong>{{ confirmConductor?.nombre }} {{ confirmConductor?.apellidos }}</strong>?
          Se eliminar&aacute; su registro permanentemente.
        </template>
      </p>
      <template #footer>
        <Button label="Cancelar" text @click="showConfirm = false" />
        <Button
          :label="confirmAction === 'aprobar' ? 'Aprobar' : 'Rechazar'"
          :severity="confirmAction === 'aprobar' ? 'success' : 'danger'"
          :icon="confirmAction === 'aprobar' ? 'pi pi-check' : 'pi pi-times'"
          :loading="store.saving"
          @click="executeConfirm"
        />
      </template>
    </Dialog>
  </div>
</template>
