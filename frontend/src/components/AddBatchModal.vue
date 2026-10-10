<template>
  <div class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
    <div class="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md overflow-hidden shadow-2xl flex flex-col">
      
      <!-- Header -->
      <div class="p-4 border-b border-slate-800 flex items-center justify-between">
        <div>
          <span class="text-xs text-brand-400 font-bold">Add Batch</span>
          <h2 class="font-bold text-base text-white truncate">{{ item.name }}</h2>
        </div>
        <button @click="$emit('close')" class="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800">
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- Form -->
      <form @submit.prevent="save" class="p-5 space-y-4">
        
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Quantity *</label>
            <input 
              type="number" 
              step="any"
              v-model.number="form.quantity" 
              required
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-brand-500" />
          </div>

          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Unit</label>
            <input 
              v-model="form.unit" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500" />
          </div>
        </div>

        <div>
          <label class="text-xs font-semibold text-slate-300 block mb-1">Storage Location</label>
          <select 
            v-model="form.locationId" 
            class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-brand-500">
            <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
          </select>
        </div>

        <div>
          <label class="text-xs font-semibold text-slate-300 block mb-1">Expiration Date</label>
          <input 
            type="date" 
            v-model="form.expirationDate" 
            class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500" />
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Unit Price ($) (Optional)</label>
            <input 
              type="number" 
              step="0.01" 
              v-model.number="form.unitPrice" 
              placeholder="0.00" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-brand-500" />
          </div>

          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Store (Optional)</label>
            <input 
              v-model="form.store" 
              placeholder="e.g. Fred Meyer, Costco" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500" />
          </div>
        </div>

        <div>
          <label class="text-xs font-semibold text-slate-300 block mb-1">Note (Optional)</label>
          <input 
            v-model="form.note" 
            placeholder="e.g. Unopened box, Top shelf" 
            class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
        </div>

        <!-- Buttons -->
        <div class="flex justify-end gap-2 pt-2">
          <button type="button" @click="$emit('close')" class="px-4 py-2 text-xs text-slate-400 hover:text-white">Cancel</button>
          <button 
            type="submit" 
            class="px-5 py-2 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs shadow-sm">
            Add Batch
          </button>
        </div>

      </form>

    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { X } from 'lucide-vue-next';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const { showToast } = useToast();

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(['close', 'saved']);

const locations = ref([]);

const form = reactive({
  itemId: props.item.id,
  locationId: props.item.defaultLocation?.id || 'loc-pantry',
  quantity: 1,
  unit: props.item.defaultUnit || 'count',
  expirationDate: '',
  note: '',
  unitPrice: null,
  store: '',
});

onMounted(async () => {
  try {
    const res = await api.getLocations();
    locations.value = res.data;
  } catch (err) {
    console.error('Failed to load locations:', err);
  }
});

async function save() {
  try {
    await api.addBatch({
      ...form,
      expirationDate: form.expirationDate ? form.expirationDate : null,
    });
    showToast(`Added +${form.quantity} ${form.unit} to ${props.item.name}!`);
    emit('saved');
    emit('close');
  } catch (err) {
    showToast('Failed to add batch: ' + err.message, 'error');
  }
}
</script>
