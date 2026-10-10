<template>
  <div class="space-y-4 pb-20">
    
    <!-- Unified Filter Bar Container -->
    <div class="bg-slate-900/90 border border-slate-800 rounded-3xl p-3.5 sm:p-4 shadow-lg space-y-3 ring-1 ring-white/5">
      
      <!-- Top Row: Search Input + View Mode Switcher -->
      <div class="flex items-center gap-3 justify-between flex-wrap sm:flex-nowrap">
        
        <!-- Search Input with Keyboard Shortcut Hint & Clear '✕' -->
        <div class="relative flex-1 min-w-[220px]">
          <Search class="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input 
            ref="searchInputRef"
            v-model="searchQuery"
            @input="onSearchInput"
            type="text" 
            placeholder="Search items, brands, barcodes... (Press '/')" 
            class="w-full bg-slate-950 border border-slate-800 rounded-2xl pl-10 pr-9 py-2.5 text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:border-brand-400 transition-colors shadow-inner" />
          
          <button 
            v-if="searchQuery"
            @click="clearSearch"
            type="button"
            title="Clear search"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white p-1 rounded-full hover:bg-slate-800 transition-colors">
            <X class="w-3.5 h-3.5" />
          </button>
        </div>

        <!-- View Mode Switcher: List vs Grid -->
        <div class="flex items-center bg-slate-950 border border-slate-800 rounded-2xl p-1 shrink-0 shadow-inner select-none">
          <button 
            type="button"
            @click="viewMode = 'list'"
            class="px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-all"
            :class="viewMode === 'list' ? 'bg-brand-400 text-slate-950 shadow-sm' : 'text-slate-400 hover:text-white'">
            <List class="w-3.5 h-3.5" />
            <span>List</span>
          </button>
          <button 
            type="button"
            @click="viewMode = 'grid'"
            class="px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-all"
            :class="viewMode === 'grid' ? 'bg-brand-400 text-slate-950 shadow-sm' : 'text-slate-400 hover:text-white'">
            <LayoutGrid class="w-3.5 h-3.5" />
            <span>Grid</span>
          </button>
        </div>

      </div>

      <!-- Bottom Filter Controls Row: Multi-select Dropdowns + Sort + Clear -->
      <div class="flex items-center gap-2 flex-wrap text-xs pt-1 border-t border-slate-850">
        
        <!-- Location Multi-Select Dropdown -->
        <FilterDropdown 
          label="Location"
          :icon="MapPin"
          :options="locationOptions"
          v-model="selectedLocationIds"
          @change="fetchItems" />

        <!-- Category Multi-Select Dropdown -->
        <FilterDropdown 
          label="Category"
          :icon="Tag"
          :options="categoryOptions"
          v-model="selectedCategoryIds"
          @change="fetchItems" />

        <!-- Stock Multi-Select Dropdown (In Stock, Low Stock, Out of Stock) -->
        <FilterDropdown 
          label="Stock"
          :icon="Layers"
          :options="stockOptions"
          v-model="selectedStockFilters"
          @change="fetchItems" />

        <!-- Freshness Multi-Select Dropdown (Fresh, Expiring Soon, Expired, Produce Check) -->
        <FilterDropdown 
          label="Freshness"
          :icon="Clock"
          :options="freshnessOptions"
          v-model="selectedFreshnessFilters"
          @change="fetchItems" />

        <!-- Sort Selector Dropdown -->
        <div class="relative shrink-0">
          <select 
            v-model="selectedSortBy"
            @change="fetchItems"
            class="h-10 bg-slate-900 border border-slate-700 hover:border-slate-600 rounded-xl px-3.5 text-xs font-semibold text-slate-200 focus:outline-none focus:border-brand-400 transition-colors shadow-sm cursor-pointer appearance-none pr-8">
            <option value="NAME">Sort: A-Z</option>
            <option value="EXPIRY">Sort: Expiration (Urgent first)</option>
            <option value="QTY_DESC">Sort: Stock (High to Low)</option>
            <option value="QTY_ASC">Sort: Stock (Low to High)</option>
          </select>
          <ArrowUpDown class="w-3.5 h-3.5 text-slate-400 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
        </div>

        <!-- Clear All Filters Button (Only when filters are active) -->
        <button 
          v-if="hasActiveFilters"
          type="button"
          @click="clearAllFilters"
          title="Reset all active filters"
          class="h-10 px-3 rounded-xl border border-rose-500/30 bg-rose-500/10 text-rose-300 hover:bg-rose-500/20 text-xs font-semibold flex items-center gap-1.5 transition-colors ml-auto">
          <RotateCcw class="w-3.5 h-3.5" />
          <span>Clear All</span>
        </button>

      </div>

      <!-- Active Filters Strip & Results Count -->
      <div v-if="hasActiveFilters || !loading" class="flex items-center justify-between gap-2 flex-wrap pt-2 border-t border-slate-850 text-xs">
        
        <!-- Dismissible Filter Chips -->
        <div class="flex items-center gap-1.5 flex-wrap">
          <span class="text-slate-400 text-[11px] font-medium mr-1">Active:</span>

          <!-- Search Query Chip -->
          <span 
            v-if="searchQuery" 
            class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg bg-slate-800 text-slate-200 border border-slate-700 text-[11px]">
            <span>"{{ searchQuery }}"</span>
            <button @click="clearSearch" class="hover:text-rose-400"><X class="w-3 h-3" /></button>
          </span>

          <!-- Location Chips -->
          <span 
            v-for="locId in selectedLocationIds" 
            :key="locId"
            class="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg bg-slate-800 text-brand-300 border border-slate-700 text-[11px]">
            <span>{{ getLocationName(locId) }}</span>
            <button @click="removeLocation(locId)" class="hover:text-rose-400"><X class="w-3 h-3" /></button>
          </span>

          <!-- Category Chips -->
          <span 
            v-for="catId in selectedCategoryIds" 
            :key="catId"
            class="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg bg-slate-800 text-slate-200 border border-slate-700 text-[11px]">
            <span>{{ getCategoryName(catId) }}</span>
            <button @click="removeCategory(catId)" class="hover:text-rose-400"><X class="w-3 h-3" /></button>
          </span>

          <!-- Stock Chips -->
          <span 
            v-for="sKey in selectedStockFilters" 
            :key="sKey"
            class="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg bg-slate-800 text-slate-200 border border-slate-700 text-[11px]">
            <span>{{ getStockName(sKey) }}</span>
            <button @click="removeStock(sKey)" class="hover:text-rose-400"><X class="w-3 h-3" /></button>
          </span>

          <!-- Freshness Chips -->
          <span 
            v-for="fKey in selectedFreshnessFilters" 
            :key="fKey"
            class="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg bg-slate-800 text-slate-200 border border-slate-700 text-[11px]">
            <span>{{ getFreshnessName(fKey) }}</span>
            <button @click="removeFreshness(fKey)" class="hover:text-rose-400"><X class="w-3 h-3" /></button>
          </span>
        </div>

        <!-- Result Count -->
        <span class="text-slate-400 font-mono text-[11px] ml-auto">
          Showing <strong class="text-white">{{ items.length }}</strong> item(s)
        </span>

      </div>

    </div>

    <!-- Inventory Display: Loading State -->
    <div v-if="loading">
      <!-- List View Skeletons -->
      <div v-if="viewMode === 'list'" class="space-y-2">
        <div v-for="n in 8" :key="n" class="bg-slate-900/60 rounded-2xl p-4 border border-slate-800 h-14 animate-pulse"></div>
      </div>
      <!-- Grid View Skeletons -->
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <div v-for="n in 8" :key="n" class="bg-slate-900/60 rounded-2xl p-4 border border-slate-800 h-44 animate-pulse"></div>
      </div>
    </div>

    <!-- Inventory Display: List View Mode -->
    <div v-else-if="items.length > 0 && viewMode === 'list'" class="space-y-2">
      <ItemListItem 
        v-for="item in items" 
        :key="item.id" 
        :item="item" 
        :is-expanded="expandedItemIds.has(item.id)"
        @toggle-expand="toggleExpandItem"
        @adjust="handleAdjust"
        @edit="openEdit"
        @add-batch="openAddBatch"
        @move-zone="openMoveZone"
        @consume="handleConsume"
        @history="openPurchaseHistory"
        @add-shopping-list="handleAddToShoppingList" />
    </div>

    <!-- Inventory Display: Grid View Mode -->
    <div v-else-if="items.length > 0 && viewMode === 'grid'" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <ItemCard 
        v-for="item in items" 
        :key="item.id" 
        :item="item" 
        @adjust="handleAdjust"
        @edit="openEdit"
        @add-batch="openAddBatch"
        @move-zone="openMoveZone"
        @consume="handleConsume"
        @history="openPurchaseHistory"
        @add-shopping-list="handleAddToShoppingList" />
    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-16 bg-slate-900/40 rounded-3xl border border-slate-800 p-8 max-w-lg mx-auto">
      <div class="w-16 h-16 rounded-2xl bg-brand-500/10 text-brand-400 flex items-center justify-center mx-auto mb-4 border border-brand-500/20">
        <PackageOpen class="w-8 h-8" />
      </div>
      <h3 class="text-lg font-bold text-white mb-1">No items match your criteria</h3>
      <p class="text-xs text-slate-400 mb-6">Try adjusting your stock, freshness, or location filters, or scan a new item.</p>
      <div class="flex justify-center gap-3">
        <button 
          v-if="hasActiveFilters"
          @click="clearAllFilters" 
          class="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold border border-slate-700 flex items-center gap-2">
          <RotateCcw class="w-3.5 h-3.5" />
          Reset Filters
        </button>
        <button @click="$emit('open-scan')" class="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold border border-slate-700 flex items-center gap-2">
          <ScanBarcode class="w-4 h-4 text-brand-400" />
          Scan Barcode
        </button>
        <button @click="$emit('open-add')" class="px-4 py-2 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 text-xs font-bold shadow-sm">
          Add Item
        </button>
      </div>
    </div>

    <!-- Floating "Collapse All" Action Button (Bottom Right) -->
    <transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 translate-y-4 scale-95"
      enter-to-class="opacity-100 translate-y-0 scale-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 translate-y-0 scale-100"
      leave-to-class="opacity-0 translate-y-4 scale-95">
      <button 
        v-if="expandedItemIds.size > 0 && viewMode === 'list'"
        type="button"
        @click="collapseAll"
        title="Collapse all currently expanded items"
        class="fixed bottom-6 right-6 z-30 px-4 py-2.5 rounded-full bg-slate-800/95 hover:bg-slate-700 text-white font-bold text-xs shadow-2xl border border-slate-700 flex items-center gap-2 backdrop-blur-md transition-all hover:scale-105 ring-1 ring-white/10">
        <ChevronUp class="w-4 h-4 text-brand-400" />
        <span>Collapse All ({{ expandedItemIds.size }})</span>
      </button>
    </transition>

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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { 
  Search, LayoutGrid, List, PackageOpen, ScanBarcode, Refrigerator, Snowflake, 
  Flame, Archive, Layers, MapPin, Tag, Clock, ArrowUpDown, X, RotateCcw, ChevronUp
} from 'lucide-vue-next';
import ItemCard from '../components/ItemCard.vue';
import ItemListItem from '../components/ItemListItem.vue';
import FilterDropdown from '../components/FilterDropdown.vue';
import AddItemModal from '../components/AddItemModal.vue';
import AddBatchModal from '../components/AddBatchModal.vue';
import MoveZoneModal from '../components/MoveZoneModal.vue';
import PurchaseHistoryModal from '../components/PurchaseHistoryModal.vue';
import api from '../services/api';
import { useToast } from '../composables/useToast';
import { useInventoryPreferences } from '../composables/useInventoryPreferences';

const { showToast } = useToast();
const { viewMode, selectedSortBy } = useInventoryPreferences();

defineEmits(['open-scan', 'open-receipt', 'open-add']);

const items = ref([]);
const locations = ref([]);
const categories = ref([]);
const loading = ref(true);

const searchInputRef = ref(null);
const searchQuery = ref('');
const selectedLocationIds = ref([]);
const selectedCategoryIds = ref([]);
const selectedStockFilters = ref([]);
const selectedFreshnessFilters = ref([]);

const expandedItemIds = ref(new Set());

const editItemModalOpen = ref(false);
const itemToEdit = ref(null);

const addBatchModalOpen = ref(false);
const itemForBatch = ref(null);

const moveZoneModalOpen = ref(false);
const itemForMove = ref(null);

const historyModalOpen = ref(false);
const itemForHistory = ref(null);

let searchDebounceTimeout = null;

// Multi-select Dropdown Options
const locationOptions = computed(() => {
  return locations.value.map(loc => ({
    id: loc.id,
    name: loc.name,
    icon: getIcon(loc.icon),
  }));
});

const categoryOptions = computed(() => {
  return categories.value.map(cat => ({
    id: cat.id,
    name: cat.name,
  }));
});

const stockOptions = [
  { id: 'IN_STOCK', name: 'In Stock', color: '#10b981' },
  { id: 'LOW_STOCK', name: 'Low Stock', color: '#f59e0b' },
  { id: 'OUT_OF_STOCK', name: 'Out of Stock', color: '#ef4444' },
];

const freshnessOptions = [
  { id: 'FRESH', name: 'Fresh', color: '#10b981' },
  { id: 'EXPIRING_SOON', name: 'Expiring Soon', color: '#f59e0b' },
  { id: 'EXPIRED', name: 'Expired', color: '#ef4444' },
  { id: 'FRESH_CHECK', name: 'Produce Check Needed', color: '#eab308' },
];

const hasActiveFilters = computed(() => {
  return (
    searchQuery.value.trim().length > 0 ||
    selectedLocationIds.value.length > 0 ||
    selectedCategoryIds.value.length > 0 ||
    selectedStockFilters.value.length > 0 ||
    selectedFreshnessFilters.value.length > 0
  );
});

onMounted(async () => {
  window.addEventListener('keydown', handleGlobalKeydown);
  await Promise.all([loadMetadata(), fetchItems()]);
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleGlobalKeydown);
  clearTimeout(searchDebounceTimeout);
});

function handleGlobalKeydown(e) {
  // Shortcut '/' focuses search input if user is not already typing in an input/textarea
  if (e.key === '/' && !['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName)) {
    e.preventDefault();
    searchInputRef.value?.focus();
  } else if (e.key === 'Escape') {
    if (searchQuery.value) {
      clearSearch();
    }
  }
}

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
      locationId: selectedLocationIds.value,
      categoryId: selectedCategoryIds.value,
      stockFilter: selectedStockFilters.value,
      freshnessFilter: selectedFreshnessFilters.value,
      search: searchQuery.value,
      sortBy: selectedSortBy.value,
    });
    items.value = res.data;
  } catch (err) {
    console.error('Failed to fetch items:', err);
  } finally {
    loading.value = false;
  }
}

function onSearchInput() {
  clearTimeout(searchDebounceTimeout);
  searchDebounceTimeout = setTimeout(() => {
    fetchItems();
  }, 250);
}

function clearSearch() {
  searchQuery.value = '';
  fetchItems();
}

function clearAllFilters() {
  searchQuery.value = '';
  selectedLocationIds.value = [];
  selectedCategoryIds.value = [];
  selectedStockFilters.value = [];
  selectedFreshnessFilters.value = [];
  fetchItems();
}

function removeLocation(id) {
  selectedLocationIds.value = selectedLocationIds.value.filter(item => item !== id);
  fetchItems();
}

function removeCategory(id) {
  selectedCategoryIds.value = selectedCategoryIds.value.filter(item => item !== id);
  fetchItems();
}

function removeStock(key) {
  selectedStockFilters.value = selectedStockFilters.value.filter(item => item !== key);
  fetchItems();
}

function removeFreshness(key) {
  selectedFreshnessFilters.value = selectedFreshnessFilters.value.filter(item => item !== key);
  fetchItems();
}

function getLocationName(id) {
  const match = locations.value.find(l => l.id === id);
  return match ? match.name : id;
}

function getCategoryName(id) {
  const match = categories.value.find(c => c.id === id);
  return match ? match.name : id;
}

function getStockName(key) {
  const match = stockOptions.find(o => o.id === key);
  return match ? match.name : key;
}

function getFreshnessName(key) {
  const match = freshnessOptions.find(o => o.id === key);
  return match ? match.name : key;
}

// Multi-Expand Accordion handlers
function toggleExpandItem(id) {
  const newSet = new Set(expandedItemIds.value);
  if (newSet.has(id)) {
    newSet.delete(id);
  } else {
    newSet.add(id);
  }
  expandedItemIds.value = newSet;
}

function collapseAll() {
  expandedItemIds.value = new Set();
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

async function handleAddToShoppingList(item) {
  try {
    const spec = item.defaultPurchaseAmount || (item.restockQuantity > 0 ? `${item.restockQuantity} ${item.defaultUnit}` : '');
    await api.addToBring(item.name, spec);
    showToast(`Added "${item.name}" to Shopping List / Bring!`);
  } catch (err) {
    showToast(`Added "${item.name}" to Shopping List!`);
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
