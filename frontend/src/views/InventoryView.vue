<template>
  <div class="space-y-6 pb-16">
    
    <!-- Top Filter Bar -->
    <div class="flex flex-col md:flex-row gap-3 items-stretch md:items-center justify-between">
      
      <!-- Search Bar -->
      <div class="relative flex-1 max-w-md">
        <Search class="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
        <input 
          v-model="searchQuery"
          @input="fetchItems"
          type="text" 
          placeholder="Search items, brands, or barcodes..." 
          class="w-full bg-slate-900 border border-slate-700 rounded-2xl pl-10 pr-4 py-2.5 text-sm text-white placeholder-slate-400 focus:outline-none focus:border-brand-400 transition-colors shadow-sm ring-1 ring-white/5" />
      </div>

      <!-- Category Filter Dropdown & Sort Selector -->
      <div class="flex items-center gap-2 flex-wrap sm:flex-nowrap">
        <select 
          v-model="selectedCategoryId"
          @change="fetchItems"
          class="bg-slate-900 border border-slate-700 rounded-2xl px-3.5 py-2.5 text-xs font-medium text-slate-100 focus:outline-none focus:border-brand-400 transition-colors shadow-sm ring-1 ring-white/5">
          <option value="">All Categories</option>
          <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
        </select>

        <select 
          v-model="selectedSortBy"
          @change="fetchItems"
          class="bg-slate-900 border border-slate-700 rounded-2xl px-3.5 py-2.5 text-xs font-medium text-slate-100 focus:outline-none focus:border-brand-400 transition-colors shadow-sm ring-1 ring-white/5">
          <option value="NAME">Sort: A-Z</option>
          <option value="EXPIRY">Sort: Expiration (Urgent first)</option>
          <option value="QTY_DESC">Sort: Stock (High to Low)</option>
          <option value="QTY_ASC">Sort: Stock (Low to High)</option>
        </select>
      </div>

    </div>

    <!-- Status & Expiration Filter Pills -->
    <div class="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none text-xs">
      <button 
        @click="selectStatus('ALL')"
        class="px-3.5 py-1.5 rounded-xl font-bold whitespace-nowrap transition-all shadow-sm border"
        :class="selectedStatus === 'ALL' ? 'bg-slate-700 text-white border-slate-600' : 'bg-slate-900 text-slate-300 hover:text-white border-slate-800 hover:border-slate-700'">
        All Status
      </button>

      <button 
        @click="selectStatus('EXPIRING_SOON')"
        class="px-3.5 py-1.5 rounded-xl font-bold whitespace-nowrap transition-all flex items-center gap-1.5 shadow-sm border"
        :class="selectedStatus === 'EXPIRING_SOON' ? 'bg-amber-500/25 text-amber-200 border-amber-400' : 'bg-slate-900 text-slate-300 hover:text-white border-slate-800 hover:border-slate-700'">
        <Clock class="w-3.5 h-3.5 text-amber-400" />
        Expiring Soon
      </button>

      <button 
        @click="selectStatus('EXPIRED')"
        class="px-3.5 py-1.5 rounded-xl font-bold whitespace-nowrap transition-all flex items-center gap-1.5 shadow-sm border"
        :class="selectedStatus === 'EXPIRED' ? 'bg-rose-500/25 text-rose-200 border-rose-400' : 'bg-slate-900 text-slate-300 hover:text-white border-slate-800 hover:border-slate-700'">
        <AlertCircle class="w-3.5 h-3.5 text-rose-400" />
        Expired
      </button>

      <button 
        @click="selectStatus('FRESH_CHECK')"
        class="px-3.5 py-1.5 rounded-xl font-bold whitespace-nowrap transition-all flex items-center gap-1.5 shadow-sm border"
        :class="selectedStatus === 'FRESH_CHECK' ? 'bg-emerald-500/25 text-emerald-200 border-emerald-400' : 'bg-slate-900 text-slate-300 hover:text-white border-slate-800 hover:border-slate-700'">
        <Sparkles class="w-3.5 h-3.5 text-emerald-400" />
        Fresh Produce Check
      </button>

      <button 
        @click="selectStatus('LOW_STOCK')"
        class="px-3.5 py-1.5 rounded-xl font-bold whitespace-nowrap transition-all flex items-center gap-1.5 shadow-sm border"
        :class="selectedStatus === 'LOW_STOCK' ? 'bg-orange-500/25 text-orange-200 border-orange-400' : 'bg-slate-900 text-slate-300 hover:text-white border-slate-800 hover:border-slate-700'">
        Low Stock
      </button>

      <button 
        @click="selectStatus('OUT_OF_STOCK')"
        class="px-3.5 py-1.5 rounded-xl font-bold whitespace-nowrap transition-all shadow-sm border"
        :class="selectedStatus === 'OUT_OF_STOCK' ? 'bg-rose-950/60 text-rose-200 border-rose-500' : 'bg-slate-900 text-slate-300 hover:text-white border-slate-800 hover:border-slate-700'">
        Out of Stock
      </button>
    </div>

    <!-- Location Filter Pills (Fridge, Freezer, Pantry, Spice Rack) -->
    <div class="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none">
      <button 
        @click="selectLocation('')"
        class="px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all flex items-center gap-1.5 shadow-sm border"
        :class="selectedLocationId === '' ? 'bg-brand-500/25 text-brand-200 border-brand-400 shadow-md' : 'bg-slate-900 text-slate-300 hover:text-white border-slate-800 hover:border-slate-700'">
        <LayoutGrid class="w-3.5 h-3.5" />
        All Locations
      </button>

      <button 
        v-for="loc in locations" 
        :key="loc.id" 
        @click="selectLocation(loc.id)"
        class="px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all flex items-center gap-1.5 shadow-sm border"
        :class="selectedLocationId === loc.id ? 'bg-brand-500/25 text-brand-200 border-brand-400 shadow-md' : 'bg-slate-900 text-slate-300 hover:text-white border-slate-800 hover:border-slate-700'">
        <component :is="getIcon(loc.icon)" class="w-3.5 h-3.5" />
        {{ loc.name }}
      </button>
    </div>

    <!-- Inventory Cards Grid -->
    <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <div v-for="n in 8" :key="n" class="bg-slate-800/40 rounded-2xl p-4 border border-slate-700/40 h-44 animate-pulse"></div>
    </div>

    <div v-else-if="items.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <ItemCard 
        v-for="item in items" 
        :key="item.id" 
        :item="item" 
        @adjust="handleAdjust"
        @edit="openEdit"
        @add-batch="openAddBatch"
        @move-zone="openMoveZone"
        @consume="handleConsume"
        @history="openPurchaseHistory" />
    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-16 bg-slate-800/30 rounded-3xl border border-slate-800 p-8 max-w-lg mx-auto">
      <div class="w-16 h-16 rounded-2xl bg-brand-500/10 text-brand-400 flex items-center justify-center mx-auto mb-4 border border-brand-500/20">
        <PackageOpen class="w-8 h-8" />
      </div>
      <h3 class="text-lg font-bold text-white mb-1">No items found</h3>
      <p class="text-xs text-slate-400 mb-6">Your pantry is empty in this view. Scan a barcode, import a receipt, or add items manually.</p>
      <div class="flex justify-center gap-3">
        <button @click="$emit('open-scan')" class="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold border border-slate-700 flex items-center gap-2">
          <ScanBarcode class="w-4 h-4 text-brand-400" />
          Scan Barcode
        </button>
        <button @click="$emit('open-add')" class="px-4 py-2 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 text-xs font-bold shadow-sm">
          Add Item Manually
        </button>
      </div>
    </div>

    <!-- Modals -->
    <AddItemModal 
      v-if="editItemModalOpen" 
      :initial-data="itemToEdit" 
      :is-edit="true" 
      @close="editItemModalOpen = false" 
      @saved="fetchItems" />

    <AddBatchModal 
      v-if="addBatchModalOpen" 
      :item="itemForBatch" 
      @close="addBatchModalOpen = false" 
      @saved="fetchItems" />

    <MoveZoneModal 
      v-if="moveZoneModalOpen"
      :item="itemForMove"
      :locations="locations"
      @close="moveZoneModalOpen = false"
      @transferred="fetchItems" />

    <PurchaseHistoryModal 
      v-if="historyModalOpen" 
      :item="itemForHistory" 
      @close="historyModalOpen = false" />

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { 
  Search, LayoutGrid, PackageOpen, ScanBarcode, Refrigerator, Snowflake, 
  Flame, Archive, Package, Clock, AlertCircle, Sparkles 
} from 'lucide-vue-next';
import ItemCard from '../components/ItemCard.vue';
import AddItemModal from '../components/AddItemModal.vue';
import AddBatchModal from '../components/AddBatchModal.vue';
import MoveZoneModal from '../components/MoveZoneModal.vue';
import PurchaseHistoryModal from '../components/PurchaseHistoryModal.vue';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const { showToast } = useToast();

defineEmits(['open-scan', 'open-receipt', 'open-add']);

const items = ref([]);
const locations = ref([]);
const categories = ref([]);
const loading = ref(true);

const searchQuery = ref('');
const selectedLocationId = ref('');
const selectedCategoryId = ref('');
const selectedStatus = ref('ALL');
const selectedSortBy = ref('NAME');

const editItemModalOpen = ref(false);
const itemToEdit = ref(null);

const addBatchModalOpen = ref(false);
const itemForBatch = ref(null);

const moveZoneModalOpen = ref(false);
const itemForMove = ref(null);

const historyModalOpen = ref(false);
const itemForHistory = ref(null);

onMounted(async () => {
  await Promise.all([loadMetadata(), fetchItems()]);
});

async function loadMetadata() {
  try {
    const [lRes, cRes] = await Promise.all([
      api.getLocations(),
      api.getCategories(),
    ]);
    locations.value = lRes.data;
    categories.value = cRes.data;
  } catch (err) {
    console.error('Failed to load metadata:', err);
  }
}

async function fetchItems() {
  loading.value = true;
  try {
    const res = await api.getItems({
      locationId: selectedLocationId.value,
      categoryId: selectedCategoryId.value,
      search: searchQuery.value,
      status: selectedStatus.value,
      sortBy: selectedSortBy.value,
    });
    items.value = res.data;
  } catch (err) {
    console.error('Failed to fetch items:', err);
  } finally {
    loading.value = false;
  }
}

function selectLocation(locId) {
  selectedLocationId.value = locId;
  fetchItems();
}

function selectStatus(status) {
  selectedStatus.value = status;
  fetchItems();
}

async function handleAdjust({ itemId, delta }) {
  try {
    const res = await api.adjustQuantity(itemId, delta);
    const index = items.value.findIndex(i => i.id === itemId);
    if (index !== -1) {
      items.value[index] = res.data;
    }
  } catch (err) {
    console.error('Failed to adjust quantity:', err);
  }
}

function openEdit(item) {
  itemToEdit.value = item;
  editItemModalOpen.value = true;
}

function openAddBatch(item) {
  itemForBatch.value = item;
  addBatchModalOpen.value = true;
}

function openMoveZone(item) {
  itemForMove.value = item;
  moveZoneModalOpen.value = true;
}

function openPurchaseHistory(item) {
  itemForHistory.value = item;
  historyModalOpen.value = true;
}

async function handleConsume(item) {
  if (confirm(`Mark all "${item.name}" as consumed/used up?`)) {
    try {
      const res = await api.consumeItem(item.id);
      showToast(`Marked "${item.name}" as consumed!`);
      const index = items.value.findIndex(i => i.id === item.id);
      if (index !== -1) {
        items.value[index] = res.data;
      }
    } catch (err) {
      showToast('Failed to consume item: ' + err.message, 'error');
    }
  }
}

function getIcon(iconName) {
  switch (iconName) {
    case 'Refrigerator': return Refrigerator;
    case 'Snowflake': return Snowflake;
    case 'Flame': return Flame;
    case 'Archive': return Archive;
    default: return LayoutGrid;
  }
}
</script>
