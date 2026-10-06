<template>
  <div class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
    <div class="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md overflow-hidden shadow-2xl flex flex-col">
      
      <!-- Header -->
      <div class="p-4 border-b border-slate-800 flex items-center justify-between">
        <div>
          <span class="text-xs text-brand-400 font-bold">Transfer Storage Zone</span>
          <h2 class="font-bold text-base text-white truncate">{{ item.name }}</h2>
        </div>
        <button @click="$emit('close')" class="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800">
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- Form -->
      <form @submit.prevent="saveTransfer" class="p-5 space-y-4">
        
        <!-- Current Zone Status -->
        <div class="p-3 bg-slate-950/60 rounded-xl border border-slate-800 flex items-center justify-between">
          <div>
            <span class="text-[11px] text-slate-400 block">Current Zone</span>
            <span class="text-xs font-bold text-slate-200">{{ currentZoneName }}</span>
          </div>
          <div class="text-right">
            <span class="text-[11px] text-slate-400 block">Total In Stock</span>
            <span class="text-xs font-mono font-bold text-brand-300">{{ item.totalQuantity }} {{ item.defaultUnit }}</span>
          </div>
        </div>

        <!-- Batch Selection (if item has multiple batches) -->
        <div v-if="item.batches && item.batches.length > 1">
          <label class="text-xs font-semibold text-slate-300 block mb-1">Select Batch to Move</label>
          <select 
            v-model="selectedBatchId" 
            class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-brand-500">
            <option value="">Move All Batches (Entire Stock)</option>
            <option v-for="b in item.batches" :key="b.id" :value="b.id">
              {{ b.locationName || 'Zone' }} &bull; {{ b.quantity }} {{ b.unit }} {{ b.expirationDate ? `(Exp: ${b.expirationDate})` : '' }}
            </option>
          </select>
        </div>

        <!-- Target Storage Zone -->
        <div>
          <label class="text-xs font-semibold text-slate-300 block mb-1">Destination Storage Zone *</label>
          <select 
            v-model="targetLocationId" 
            required
            class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500">
            <option 
              v-for="loc in availableLocations" 
              :key="loc.id" 
              :value="loc.id">
              {{ loc.name }}
            </option>
          </select>
        </div>

        <!-- Quantity to move (if moving a specific batch) -->
        <div v-if="selectedBatch">
          <label class="text-xs font-semibold text-slate-300 block mb-1">Quantity to Transfer</label>
          <div class="flex items-center gap-2">
            <input 
              type="number" 
              step="any" 
              min="0.1" 
              :max="selectedBatch.quantity" 
              v-model.number="moveQty"
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-brand-500" />
            <span class="text-xs font-mono text-slate-400 whitespace-nowrap">/ {{ selectedBatch.quantity }} {{ selectedBatch.unit }}</span>
          </div>
        </div>

        <!-- Buttons -->
        <div class="flex justify-end gap-2 pt-2 border-t border-slate-800">
          <button type="button" @click="$emit('close')" class="px-4 py-2 text-xs text-slate-400 hover:text-white">
            Cancel
          </button>
          <button 
            type="submit" 
            :disabled="!targetLocationId || targetLocationId === currentZoneId"
            class="px-5 py-2 rounded-xl bg-brand-300 hover:bg-brand-200 disabled:opacity-40 text-slate-950 font-bold text-xs shadow-sm transition-colors flex items-center gap-1.5">
            <ArrowRightLeft class="w-3.5 h-3.5" />
            <span>Transfer to Zone</span>
          </button>
        </div>

      </form>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { X, ArrowRightLeft } from 'lucide-vue-next';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const { showToast } = useToast();

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
  locations: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(['close', 'transferred']);

const selectedBatchId = ref('');
const targetLocationId = ref('');
const moveQty = ref(null);

const currentZoneId = computed(() => {
  return props.item.defaultLocation?.id || (props.item.batches?.[0]?.locationId) || '';
});

const currentZoneName = computed(() => {
  return props.item.defaultLocation?.name || (props.item.batches?.[0]?.locationName) || 'Unassigned';
});

const availableLocations = computed(() => {
  return props.locations;
});

const selectedBatch = computed(() => {
  if (!selectedBatchId.value) return null;
  return props.item.batches?.find(b => b.id === selectedBatchId.value) || null;
});

onMounted(() => {
  // Select first available location different from current
  const other = props.locations.find(l => l.id !== currentZoneId.value);
  if (other) {
    targetLocationId.value = other.id;
  }
});

async function saveTransfer() {
  if (!targetLocationId.value) return;

  try {
    const res = await api.moveLocation({
      itemId: props.item.id,
      batchId: selectedBatchId.value || null,
      targetLocationId: targetLocationId.value,
      quantity: selectedBatch.value ? moveQty.value : null,
    });

    const targetLoc = props.locations.find(l => l.id === targetLocationId.value);
    showToast(`Moved "${props.item.name}" to ${targetLoc?.name || 'new zone'}!`);
    emit('transferred', res.data);
    emit('close');
  } catch (err) {
    showToast('Failed to transfer zone: ' + err.message, 'error');
  }
}
</script>
