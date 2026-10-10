<template>
  <div class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
    <div class="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-2xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">
      
      <!-- Header -->
      <div class="p-4 border-b border-slate-800 flex items-center justify-between">
        <div class="flex items-center gap-3 min-w-0">
          <div v-if="item.imageUrl" class="w-10 h-10 rounded-xl overflow-hidden bg-slate-950 flex-shrink-0 border border-slate-700">
            <img :src="item.imageUrl" :alt="item.name" class="w-full h-full object-cover" />
          </div>
          <div v-else class="w-10 h-10 rounded-xl bg-slate-800 flex items-center justify-center flex-shrink-0 text-brand-400">
            <Receipt class="w-5 h-5" />
          </div>
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <span v-if="item.brand" class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ item.brand }}</span>
              <span class="text-[10px] text-brand-400 font-medium">Stock: {{ item.totalQuantity }} {{ item.defaultUnit }}</span>
            </div>
            <h2 class="font-bold text-base text-white truncate">{{ item.name }}</h2>
          </div>
        </div>
        <button @click="$emit('close')" class="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800">
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- Price Insights Cards -->
      <div v-if="purchases.length > 0 && priceStats.hasPrices" class="grid grid-cols-3 gap-2 p-4 bg-slate-950/60 border-b border-slate-800 text-center">
        <div class="bg-slate-900/90 rounded-xl p-2.5 border border-slate-800">
          <span class="text-[10px] text-slate-400 block font-medium">Latest Price</span>
          <span class="text-sm font-bold text-white font-mono">
            {{ priceStats.latest != null ? '$' + priceStats.latest.toFixed(2) : '—' }}
          </span>
          <span v-if="priceStats.latestStore" class="text-[9px] text-brand-300 block truncate mt-0.5">@ {{ priceStats.latestStore }}</span>
        </div>

        <div class="bg-slate-900/90 rounded-xl p-2.5 border border-slate-800">
          <span class="text-[10px] text-slate-400 block font-medium">Lowest Price</span>
          <span class="text-sm font-bold text-emerald-400 font-mono">
            {{ priceStats.lowest != null ? '$' + priceStats.lowest.toFixed(2) : '—' }}
          </span>
          <span class="text-[9px] text-slate-500 block mt-0.5">Best Deal</span>
        </div>

        <div class="bg-slate-900/90 rounded-xl p-2.5 border border-slate-800">
          <span class="text-[10px] text-slate-400 block font-medium">Avg Price</span>
          <span class="text-sm font-bold text-slate-200 font-mono">
            {{ priceStats.avg != null ? '$' + priceStats.avg.toFixed(2) : '—' }}
          </span>
          <span class="text-[9px] text-slate-500 block mt-0.5">{{ purchases.length }} purchase{{ purchases.length === 1 ? '' : 's' }}</span>
        </div>
      </div>

      <!-- Main Body -->
      <div class="p-4 flex-1 overflow-y-auto space-y-4">
        
        <!-- Toggle to Log New Purchase -->
        <div class="flex items-center justify-between">
          <h3 class="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
            <History class="w-4 h-4 text-brand-400" />
            Purchase History ({{ purchases.length }})
          </h3>
          <button 
            @click="showAddForm = !showAddForm"
            class="text-xs font-semibold px-2.5 py-1 rounded-lg border transition-colors flex items-center gap-1"
            :class="showAddForm ? 'bg-slate-800 text-slate-300 border-slate-700' : 'bg-brand-500/10 text-brand-300 border-brand-500/30 hover:bg-brand-500/20'">
            <Plus class="w-3.5 h-3.5" />
            <span>{{ showAddForm ? 'Close Form' : 'Log Purchase' }}</span>
          </button>
        </div>

        <!-- Add Purchase Mini-Form (All fields optional!) -->
        <form v-if="showAddForm" @submit.prevent="savePurchase" class="bg-slate-950/80 border border-brand-500/30 rounded-xl p-3.5 space-y-3">
          <div class="flex items-center justify-between border-b border-slate-800 pb-2">
            <span class="text-xs font-bold text-white flex items-center gap-1">
              <PlusCircle class="w-3.5 h-3.5 text-brand-400" />
              Log Item Purchase
            </span>
            <span class="text-[10px] text-slate-400 italic">All fields optional</span>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            <div>
              <label class="text-[10px] text-slate-400 font-semibold block mb-1">Purchase Date</label>
              <input 
                type="date" 
                v-model="form.purchasedDate" 
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>

            <div>
              <label class="text-[10px] text-slate-400 font-semibold block mb-1">Store / Location</label>
              <input 
                type="text" 
                v-model="form.store" 
                placeholder="e.g. Fred Meyer, Costco" 
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>

            <div>
              <label class="text-[10px] text-slate-400 font-semibold block mb-1">Unit Price ($)</label>
              <input 
                type="number" 
                step="0.01" 
                v-model.number="form.unitPrice" 
                placeholder="0.00" 
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-brand-500" />
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            <div class="flex gap-2 items-center sm:col-span-1">
              <div class="flex-1">
                <label class="text-[10px] text-slate-400 font-semibold block mb-1">Quantity</label>
                <input 
                  type="number" 
                  step="any" 
                  v-model.number="form.quantity" 
                  class="w-full bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-white text-center font-mono focus:outline-none focus:border-brand-500" />
              </div>
              <div class="w-20">
                <label class="text-[10px] text-slate-400 font-semibold block mb-1">Unit</label>
                <input 
                  type="text" 
                  v-model="form.unit" 
                  class="w-full bg-slate-900 border border-slate-700 rounded-lg px-2 py-1.5 text-xs text-slate-300 text-center focus:outline-none focus:border-brand-500" />
              </div>
            </div>

            <div class="sm:col-span-2">
              <label class="text-[10px] text-slate-400 font-semibold block mb-1">Notes</label>
              <input 
                type="text" 
                v-model="form.notes" 
                placeholder="e.g. On sale, bulk discount" 
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>
          </div>

          <div class="flex justify-end gap-2 pt-1">
            <button 
              type="button" 
              @click="showAddForm = false" 
              class="px-3 py-1.5 text-xs text-slate-400 hover:text-white">
              Cancel
            </button>
            <button 
              type="submit" 
              :disabled="isSaving" 
              class="px-4 py-1.5 rounded-lg bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs shadow-sm flex items-center gap-1.5">
              <Check class="w-3.5 h-3.5" />
              <span>{{ isSaving ? 'Saving...' : 'Save Entry' }}</span>
            </button>
          </div>
        </form>

        <!-- Loading State -->
        <div v-if="loading" class="py-8 text-center text-slate-400 text-xs">
          Loading purchase records...
        </div>

        <!-- Empty State -->
        <div v-else-if="purchases.length === 0" class="py-10 text-center bg-slate-800/30 rounded-2xl border border-slate-800 p-6">
          <Receipt class="w-8 h-8 text-slate-600 mx-auto mb-2" />
          <h4 class="text-sm font-bold text-white mb-1">No purchase history yet</h4>
          <p class="text-xs text-slate-400 max-w-sm mx-auto mb-3">
            Import a receipt or tap "Log Purchase" above to track prices, stores, and purchase dates for this item.
          </p>
        </div>

        <!-- Purchases Timeline List -->
        <div v-else class="space-y-2 max-h-[45vh] overflow-y-auto pr-1">
          <div 
            v-for="record in purchases" 
            :key="record.id" 
            class="bg-slate-800/70 border border-slate-700/80 rounded-xl p-3 flex items-center justify-between gap-3 hover:border-slate-600 transition-colors">
            
            <!-- Left Info -->
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2 flex-wrap">
                <!-- Date -->
                <span class="text-xs font-bold text-white">
                  {{ formatDate(record.purchasedDate) }}
                </span>

                <!-- Store badge -->
                <span v-if="record.store" class="text-[10px] font-semibold px-2 py-0.5 rounded-md bg-slate-900 border border-slate-700 text-brand-300">
                  {{ record.store }}
                </span>

                <!-- Source badge -->
                <span 
                  v-if="record.source === 'RECEIPT'" 
                  class="text-[9px] font-bold px-1.5 py-0.5 rounded bg-blue-950 text-blue-300 border border-blue-800">
                  Receipt
                </span>
              </div>

              <!-- Quantity & Notes -->
              <div class="text-[11px] text-slate-400 mt-1 flex items-center gap-3">
                <span v-if="record.quantity != null">
                  Quantity: <strong class="text-slate-200 font-mono">{{ record.quantity }} {{ record.unit || '' }}</strong>
                </span>
                <span v-if="record.notes" class="truncate italic text-slate-400">
                  "{{ record.notes }}"
                </span>
              </div>
            </div>

            <!-- Right Price & Delete -->
            <div class="flex items-center gap-3 flex-shrink-0 text-right">
              <div>
                <div v-if="record.unitPrice != null" class="font-bold text-sm text-emerald-400 font-mono">
                  \${{ record.unitPrice.toFixed(2) }}
                  <span class="text-[10px] text-slate-400 font-normal">ea</span>
                </div>
                <div v-if="record.totalPrice != null && record.quantity && record.quantity > 1" class="text-[10px] text-slate-400 font-mono">
                  Total: \${{ record.totalPrice.toFixed(2) }}
                </div>
                <div v-else-if="record.unitPrice == null && record.totalPrice != null" class="font-bold text-sm text-emerald-400 font-mono">
                  \${{ record.totalPrice.toFixed(2) }}
                </div>
              </div>

              <button 
                @click="removePurchase(record.id)" 
                title="Delete purchase entry"
                class="p-1 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-slate-900 transition-colors">
                <Trash2 class="w-3.5 h-3.5" />
              </button>
            </div>

          </div>
        </div>

      </div>

      <!-- Footer -->
      <div class="p-3 border-t border-slate-800 flex justify-end">
        <button @click="$emit('close')" class="px-4 py-2 rounded-xl text-xs bg-slate-800 hover:bg-slate-700 text-white font-semibold">
          Close
        </button>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { Receipt, X, Plus, PlusCircle, History, Check, Trash2 } from 'lucide-vue-next';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(['close']);
const { showToast } = useToast();

const purchases = ref([]);
const loading = ref(true);
const isSaving = ref(false);
const showAddForm = ref(false);

const form = reactive({
  purchasedDate: new Date().toISOString().substring(0, 10),
  store: '',
  quantity: 1,
  unit: props.item.defaultUnit || 'count',
  unitPrice: null,
  notes: '',
});

const priceStats = computed(() => {
  const withPrice = purchases.value.filter(p => p.unitPrice != null || p.totalPrice != null);
  if (withPrice.length === 0) {
    return { hasPrices: false, latest: null, latestStore: null, lowest: null, avg: null };
  }

  const prices = withPrice.map(p => p.unitPrice != null ? p.unitPrice : (p.totalPrice / (p.quantity || 1)));
  const latestRecord = withPrice[0];
  const latest = latestRecord.unitPrice != null ? latestRecord.unitPrice : latestRecord.totalPrice;
  const lowest = Math.min(...prices);
  const avg = prices.reduce((a, b) => a + b, 0) / prices.length;

  return {
    hasPrices: true,
    latest,
    latestStore: latestRecord.store,
    lowest,
    avg,
  };
});

onMounted(async () => {
  await fetchPurchases();
});

async function fetchPurchases() {
  loading.value = true;
  try {
    const res = await api.getItemPurchases(props.item.id);
    purchases.value = res.data;
  } catch (err) {
    console.error('Failed to load purchases:', err);
  } finally {
    loading.value = false;
  }
}

async function savePurchase() {
  isSaving.value = true;
  try {
    await api.addPurchaseRecord(props.item.id, {
      purchasedDate: form.purchasedDate || null,
      store: form.store ? form.store.trim() : null,
      quantity: form.quantity != null ? form.quantity : null,
      unit: form.unit ? form.unit.trim() : null,
      unitPrice: form.unitPrice != null ? form.unitPrice : null,
      notes: form.notes ? form.notes.trim() : null,
      source: 'MANUAL',
    });
    showToast('Logged purchase record!');
    showAddForm.value = false;
    form.unitPrice = null;
    form.notes = '';
    await fetchPurchases();
  } catch (err) {
    showToast('Failed to save purchase: ' + err.message, 'error');
  } finally {
    isSaving.value = false;
  }
}

async function removePurchase(purchaseId) {
  if (!confirm('Delete this purchase record?')) return;
  try {
    await api.deletePurchaseRecord(props.item.id, purchaseId);
    purchases.value = purchases.value.filter(p => p.id !== purchaseId);
    showToast('Purchase record deleted');
  } catch (err) {
    showToast('Failed to delete purchase: ' + err.message, 'error');
  }
}

function formatDate(dateStr) {
  if (!dateStr) return 'Date not recorded';
  try {
    const [y, m, d] = dateStr.split('-');
    const dt = new Date(y, m - 1, d);
    return dt.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
  } catch {
    return dateStr;
  }
}
</script>
