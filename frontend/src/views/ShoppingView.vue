<template>
  <div class="space-y-6 pb-16 max-w-5xl mx-auto">
    
    <!-- Top Header -->
    <div class="bg-gradient-to-r from-brand-900/60 to-slate-900 p-6 rounded-3xl border border-brand-800/40 shadow-lg flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
      <div>
        <div class="flex items-center gap-2">
          <ShoppingCart class="w-6 h-6 text-brand-400" />
          <h2 class="text-xl font-bold text-white">Shopping List</h2>
        </div>
        <p class="text-xs text-slate-300 mt-1 max-w-xl">
          Items below their minimum threshold are automatically gathered here for your next grocery trip.
        </p>
      </div>

      <!-- Bring! Sync Button (Only visible if Bring! is configured) -->
      <div v-if="isBringConfigured" class="flex items-center gap-2">
        <button 
          @click="pushAllLowStockToBring" 
          :disabled="isSyncing || lowStockItems.length === 0"
          class="px-4 py-2.5 rounded-xl bg-brand-300 hover:bg-brand-200 disabled:opacity-50 text-slate-950 font-bold text-xs shadow-sm flex items-center gap-2 transition-all">
          <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': isSyncing }" />
          <span>Sync to Bring!</span>
        </button>
      </div>
    </div>

    <!-- Smart Restock & Seasonal Recommendations Tray -->
    <div v-if="soonDepletedItems.length > 0 || seasonalSuggestions.length > 0" class="bg-slate-900/90 border border-brand-500/30 rounded-3xl p-5 shadow-lg space-y-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 rounded-xl bg-brand-500/20 text-brand-300 flex items-center justify-center border border-brand-500/30">
            <Sparkles class="w-4 h-4" />
          </div>
          <div>
            <h3 class="font-bold text-sm text-white">Smart Restock &amp; Seasonal Recommendations</h3>
            <p class="text-[11px] text-slate-400">Based on your household's actual consumption velocity and calendar seasonality.</p>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-3.5">
        <!-- Predicted Run-Outs -->
        <div v-if="soonDepletedItems.length > 0" class="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-4 space-y-2.5">
          <span class="text-xs font-bold text-amber-300 flex items-center gap-1.5">
            <Clock class="w-3.5 h-3.5" />
            <span>Predicted to Run Out Soon (Next 3–5 Days)</span>
          </span>
          <div class="space-y-2">
            <div 
              v-for="p in soonDepletedItems" 
              :key="p.itemId"
              class="flex items-center justify-between gap-2 p-2 rounded-xl bg-slate-900/60 border border-slate-700/50">
              <div class="min-w-0">
                <span class="font-semibold text-xs text-white truncate block">{{ p.itemName }}</span>
                <span class="text-[10px] text-slate-400 font-mono">
                  Current: {{ p.currentQuantity }} {{ p.unit }} &bull; Est. runs out in {{ p.estimatedDaysRemaining !== null ? p.estimatedDaysRemaining + 'd' : 'soon' }}
                </span>
              </div>
              <button 
                type="button" 
                @click="addSuggestedToShopping(p.itemName)"
                class="px-2.5 py-1 rounded-lg bg-brand-500/20 hover:bg-brand-500/30 text-brand-200 border border-brand-500/30 text-[11px] font-bold whitespace-nowrap flex items-center gap-1 transition-colors">
                <Plus class="w-3 h-3" />
                <span>Add</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Seasonal Staples In Season -->
        <div v-if="seasonalSuggestions.length > 0" class="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-4 space-y-2.5">
          <span class="text-xs font-bold text-emerald-300 flex items-center gap-1.5">
            <Calendar class="w-3.5 h-3.5" />
            <span>Seasonal Staples In Season</span>
          </span>
          <div class="space-y-2">
            <div 
              v-for="s in seasonalSuggestions" 
              :key="s.itemId"
              class="flex items-center justify-between gap-2 p-2 rounded-xl bg-slate-900/60 border border-slate-700/50">
              <div class="min-w-0">
                <span class="font-semibold text-xs text-white truncate block">{{ s.itemName }}</span>
                <span class="text-[10px] text-emerald-400/90 font-mono">
                  {{ s.seasonalWindow }} &bull; {{ s.confidencePercent }}% seasonal concentration
                </span>
              </div>
              <button 
                type="button" 
                @click="addSuggestedToShopping(s.itemName)"
                class="px-2.5 py-1 rounded-lg bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-200 border border-emerald-500/30 text-[11px] font-bold whitespace-nowrap flex items-center gap-1 transition-colors">
                <Plus class="w-3 h-3" />
                <span>Add</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Layout: 2 Columns if Bring! is configured; Clean Full Width if Standalone -->
    <div :class="isBringConfigured ? 'grid grid-cols-1 lg:grid-cols-2 gap-6' : 'grid grid-cols-1 gap-6'">
      
      <!-- Primary Column: Needed Items (Low Stock & Depleted) -->
      <div class="bg-slate-800/80 rounded-2xl p-5 border border-slate-700/80 flex flex-col justify-between">
        <div>
          <div class="flex items-center justify-between border-b border-slate-700 pb-3 mb-4">
            <h3 class="font-bold text-white text-base flex items-center gap-2">
              <AlertTriangle class="w-4 h-4 text-amber-400" />
              Items to Buy ({{ lowStockItems.length }})
            </h3>
            
            <span class="text-xs text-slate-400">
              {{ lowStockItems.filter(i => i.isOutOfStock).length }} Out of Stock &bull; {{ lowStockItems.filter(i => !i.isOutOfStock).length }} Low
            </span>
          </div>

          <!-- Items List -->
          <div v-if="lowStockItems.length > 0" class="space-y-2.5 max-h-[60vh] overflow-y-auto pr-1">
            <div 
              v-for="item in lowStockItems" 
              :key="item.id"
              class="bg-slate-900/80 border border-slate-700/60 rounded-xl p-3.5 flex items-center justify-between gap-3 hover:border-brand-500/40 transition-colors">
              
              <div class="flex items-center gap-3 min-w-0">
                <div class="w-10 h-10 rounded-lg bg-slate-800 border border-slate-700 flex items-center justify-center flex-shrink-0 overflow-hidden">
                  <img v-if="item.imageUrl" :src="item.imageUrl" class="w-full h-full object-contain" />
                  <Package v-else class="w-5 h-5 text-slate-500" />
                </div>

                <div class="min-w-0">
                  <div class="flex items-center gap-2">
                    <h4 class="font-bold text-sm text-white truncate">{{ item.name }}</h4>
                    <span v-if="item.isOutOfStock" class="text-[10px] font-bold px-1.5 py-0.2 rounded bg-rose-500/20 text-rose-300 border border-rose-500/30">Out</span>
                    <span v-else class="text-[10px] font-bold px-1.5 py-0.2 rounded bg-amber-500/20 text-amber-300 border border-amber-500/30">Low</span>
                  </div>
                  <p class="text-xs text-slate-400 mt-0.5">
                    Current: <span class="font-mono text-slate-200">{{ item.totalQuantity }}</span> / Need: <span class="font-mono text-brand-300">{{ item.defaultPurchaseAmount || ('+' + item.restockQuantity + ' ' + item.defaultUnit) }}</span>
                  </p>
                </div>
              </div>

              <div class="flex items-center gap-2">
                <!-- 1-Click Restock / Bought Button -->
                <button 
                  @click="restockItem(item)"
                  title="Mark as Bought & Restock"
                  class="px-3 py-1.5 rounded-lg bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/30 text-xs font-semibold flex items-center gap-1.5 transition-colors">
                  <Check class="w-3.5 h-3.5 text-emerald-400" />
                  <span>Bought</span>
                </button>

                <!-- Bring Button (Only if configured) -->
                <button 
                  v-if="isBringConfigured"
                  @click="pushSingleItemToBring(item)"
                  title="Send to Bring!"
                  class="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700">
                  <Plus class="w-4 h-4 text-brand-400" />
                </button>
              </div>

            </div>
          </div>

          <div v-else class="text-center py-12 text-slate-400 text-xs">
            <CheckCircle class="w-10 h-10 text-emerald-400 mx-auto mb-3 opacity-70" />
            <p class="text-sm font-semibold text-white">Your shopping list is clear!</p>
            <p class="text-xs text-slate-400 mt-1">All pantry items are above their minimum thresholds.</p>
          </div>
        </div>

        <!-- Optional Bring Promo (if NOT configured) -->
        <div v-if="!isBringConfigured" class="mt-6 pt-4 border-t border-slate-700/60 flex items-center justify-between text-xs text-slate-400">
          <span>Tip: You can connect your Bring! mobile shopping list in Settings.</span>
          <router-link to="/settings?tab=integrations" class="text-brand-400 font-semibold hover:underline">
            Configure Bring! &rarr;
          </router-link>
        </div>

      </div>

      <!-- Secondary Column: Live Bring! List (Only rendered if Bring! is configured) -->
      <div v-if="isBringConfigured" class="bg-slate-800/80 rounded-2xl p-5 border border-slate-700/80 flex flex-col justify-between">
        <div>
          <div class="flex items-center justify-between border-b border-slate-700 pb-3 mb-4">
            <div class="flex items-center gap-2">
              <div class="w-6 h-6 rounded-md bg-red-500/20 text-red-400 flex items-center justify-center font-bold text-xs border border-red-500/30">
                B!
              </div>
              <h3 class="font-bold text-white text-base">
                Connected Bring! List ({{ bringItems.length }})
              </h3>
            </div>
            
            <button @click="fetchBringList" class="text-xs text-brand-400 hover:underline flex items-center gap-1">
              <RefreshCw class="w-3 h-3" /> Refresh
            </button>
          </div>

          <!-- Quick Add to Bring Input -->
          <div class="flex gap-2 mb-4">
            <input 
              v-model="quickItemName"
              placeholder="Add item directly to Bring! list..." 
              @keyup.enter="quickAddToBring"
              class="flex-1 bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
            <button 
              @click="quickAddToBring"
              :disabled="!quickItemName"
              class="px-4 py-2 bg-brand-300 hover:bg-brand-200 disabled:opacity-50 text-slate-950 text-xs font-bold rounded-xl shadow-sm">
              Add
            </button>
          </div>

          <div v-if="bringItems.length > 0" class="space-y-2 max-h-[50vh] overflow-y-auto pr-1">
            <div 
              v-for="(bItem, idx) in bringItems" 
              :key="idx"
              class="bg-slate-900/60 border border-slate-800 rounded-xl p-3 flex items-center justify-between">
              <div>
                <span class="text-sm font-semibold text-white">{{ bItem.name }}</span>
                <span v-if="bItem.specification" class="text-xs text-slate-400 ml-2 font-mono">({{ bItem.specification }})</span>
              </div>
            </div>
          </div>

          <div v-else class="text-center py-12 text-slate-400 text-xs">
            <p>Your connected Bring! list is currently empty.</p>
          </div>
        </div>
      </div>

    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { 
  ShoppingCart, RefreshCw, AlertTriangle, CheckCircle, Plus, Check, Package,
  Clock, Calendar, Sparkles 
} from 'lucide-vue-next';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const { showToast } = useToast();

const items = ref([]);
const bringItems = ref([]);
const settings = ref({});
const quickItemName = ref('');
const isSyncing = ref(false);
const predictions = ref([]);
const seasonalSuggestions = ref([]);

const isBringConfigured = computed(() => {
  return Boolean(settings.value?.bringEmail && settings.value.bringEmail.trim().length > 0);
});

const lowStockItems = computed(() => {
  return items.value.filter(i => i.isLowStock || i.isOutOfStock);
});

const soonDepletedItems = computed(() => {
  return predictions.value.filter(p => p.runsOutSoon && p.currentQuantity > 0);
});

onMounted(() => {
  fetchAll();
});

async function fetchAll() {
  await Promise.all([fetchPantryItems(), fetchSettings(), fetchPredictions()]);
  if (isBringConfigured.value) {
    await fetchBringList();
  }
}

async function fetchPredictions() {
  try {
    const [pRes, sRes] = await Promise.all([
      api.getDepletionPredictions(),
      api.getSeasonalSuggestions(),
    ]);
    predictions.value = pRes.data || [];
    seasonalSuggestions.value = sRes.data || [];
  } catch (err) {
    console.error('Failed to load predictions:', err);
  }
}

async function addSuggestedToShopping(name) {
  if (isBringConfigured.value) {
    await pushSingleItemToBring({ name, defaultPurchaseAmount: '1' });
  } else {
    showToast(`"${name}" flagged for restock!`);
  }
}

async function fetchSettings() {
  try {
    const res = await api.getSettings();
    settings.value = res.data;
  } catch (err) {
    console.error('Failed to load settings:', err);
  }
}

async function fetchPantryItems() {
  try {
    const res = await api.getItems();
    items.value = res.data;
  } catch (err) {
    console.error('Failed to load pantry items:', err);
  }
}

async function fetchBringList() {
  try {
    const res = await api.getBringShoppingList();
    bringItems.value = res.data;
  } catch (err) {
    console.error('Failed to fetch Bring! list:', err);
  }
}

async function restockItem(item) {
  try {
    const restockQty = item.restockQuantity > 0 ? item.restockQuantity : 1;
    await api.adjustQuantity(item.id, restockQty);
    await fetchPantryItems();
    showToast(`Restocked ${item.name} (+${restockQty})!`);
  } catch (err) {
    showToast('Failed to restock item: ' + err.message, 'error');
  }
}

function getItemBringSpec(item) {
  if (item.defaultPurchaseAmount && item.defaultPurchaseAmount.trim().length > 0) {
    return item.defaultPurchaseAmount.trim();
  }
  return item.restockQuantity > 0 ? `${item.restockQuantity} ${item.defaultUnit}` : '';
}

async function pushSingleItemToBring(item) {
  try {
    const spec = getItemBringSpec(item);
    await api.addToBring(item.name, spec);
    await fetchBringList();
    showToast(`Added "${item.name}" (${spec || '1'}) to Bring!`);
  } catch (err) {
    const msg = err.response?.data?.message || err.message;
    showToast('Failed to send to Bring: ' + msg, 'error');
  }
}

async function pushAllLowStockToBring() {
  isSyncing.value = true;
  try {
    const count = lowStockItems.value.length;
    for (const item of lowStockItems.value) {
      const spec = getItemBringSpec(item);
      await api.addToBring(item.name, spec);
    }
    await fetchBringList();
    showToast(`Successfully synced ${count} items to Bring!`);
  } catch (err) {
    const msg = err.response?.data?.message || err.message;
    showToast('Sync error: ' + msg, 'error');
  } finally {
    isSyncing.value = false;
  }
}

async function quickAddToBring() {
  if (!quickItemName.value) return;
  const name = quickItemName.value;
  try {
    await api.addToBring(name, '');
    quickItemName.value = '';
    await fetchBringList();
    showToast(`Added "${name}" to Bring!`);
  } catch (err) {
    const msg = err.response?.data?.message || err.message;
    showToast('Failed to add to Bring: ' + msg, 'error');
  }
}
</script>
