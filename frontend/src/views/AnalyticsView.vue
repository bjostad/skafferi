<template>
  <div class="space-y-6 pb-24 max-w-6xl mx-auto">
    
    <!-- Header -->
    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-slate-800 pb-5">
      <div>
        <h2 class="text-2xl font-bold text-white tracking-tight flex items-center gap-2.5">
          <BarChart3 class="w-6 h-6 text-brand-400" />
          <span>Price Tracker &amp; Spend Analytics</span>
        </h2>
        <p class="text-xs text-slate-400 mt-1">
          Historical unit price trends, store vs. store deal comparisons, and monthly grocery expenditure.
        </p>
      </div>

      <div class="flex items-center gap-2">
        <button 
          @click="refreshData" 
          :disabled="isLoading"
          class="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700 text-xs font-semibold flex items-center gap-1.5 transition-colors">
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': isLoading }" />
          <span>Refresh</span>
        </button>
      </div>
    </div>

    <!-- KPI Summary Cards -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-3.5">
      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-sm">
        <div class="flex items-center justify-between text-slate-400 mb-1">
          <span class="text-xs font-medium">Total Spend</span>
          <DollarSign class="w-4 h-4 text-emerald-400" />
        </div>
        <div class="text-2xl font-extrabold text-white font-mono">
          ${{ summary.totalSpend ? summary.totalSpend.toFixed(2) : '0.00' }}
        </div>
        <span class="text-[10px] text-slate-500">From all logged receipts</span>
      </div>

      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-sm">
        <div class="flex items-center justify-between text-slate-400 mb-1">
          <span class="text-xs font-medium">Transactions</span>
          <Receipt class="w-4 h-4 text-sky-400" />
        </div>
        <div class="text-2xl font-extrabold text-white font-mono">
          {{ summary.totalTransactions || 0 }}
        </div>
        <span class="text-[10px] text-slate-500">Purchase records</span>
      </div>

      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-sm">
        <div class="flex items-center justify-between text-slate-400 mb-1">
          <span class="text-xs font-medium">Stores Tracked</span>
          <Store class="w-4 h-4 text-amber-400" />
        </div>
        <div class="text-2xl font-extrabold text-white font-mono">
          {{ summary.distinctStoresCount || 0 }}
        </div>
        <span class="text-[10px] text-slate-500">Different merchants</span>
      </div>

      <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-sm">
        <div class="flex items-center justify-between text-slate-400 mb-1">
          <span class="text-xs font-medium">Items Tracked</span>
          <Package class="w-4 h-4 text-purple-400" />
        </div>
        <div class="text-2xl font-extrabold text-white font-mono">
          {{ summary.distinctItemsCount || 0 }}
        </div>
        <span class="text-[10px] text-slate-500">Pantry staples</span>
      </div>
    </div>

    <!-- Tab Selector -->
    <div class="flex items-center gap-2 border-b border-slate-800 pb-1">
      <button 
        v-for="tab in tabs" 
        :key="tab.id"
        @click="activeTab = tab.id"
        class="px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all"
        :class="activeTab === tab.id ? 'bg-brand-500/20 text-brand-300 border border-brand-500/40 shadow-sm' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'">
        <component :is="tab.icon" class="w-4 h-4" />
        <span>{{ tab.label }}</span>
      </button>
    </div>

    <!-- ========================================================================= -->
    <!-- TAB 1: STORE VS. STORE DEAL COMPARISON                                    -->
    <!-- ========================================================================= -->
    <div v-if="activeTab === 'stores'" class="space-y-6">
      
      <!-- Cross Store Comparison Matrix -->
      <div class="bg-slate-900/90 border border-slate-800 rounded-3xl p-6 shadow-lg space-y-4">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <h3 class="text-lg font-bold text-white flex items-center gap-2">
              <Sparkles class="w-5 h-5 text-amber-400" />
              <span>Where to Buy What (Store Comparison Matrix)</span>
            </h3>
            <p class="text-xs text-slate-400 mt-0.5">
              Identifies which store historically offers the lowest price for identical pantry staples.
            </p>
          </div>
          <span class="text-xs font-mono font-bold px-2.5 py-1 rounded-full bg-slate-800 text-slate-300 border border-slate-700 w-fit">
            {{ storeComparison.crossStoreComparisons ? storeComparison.crossStoreComparisons.length : 0 }} Cross-Store Items
          </span>
        </div>

        <div v-if="storeComparison.crossStoreComparisons && storeComparison.crossStoreComparisons.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          <div 
            v-for="(item, idx) in storeComparison.crossStoreComparisons" 
            :key="idx"
            class="bg-slate-800/70 border border-slate-700/80 rounded-2xl p-4.5 space-y-3 hover:border-brand-500/40 transition-colors">
            
            <div class="flex items-center justify-between">
              <span class="font-bold text-white text-base">{{ item.itemName }}</span>
              <span class="text-[11px] font-bold px-2.5 py-0.5 rounded-lg bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1">
                <span>🏆 Best: {{ item.cheapestStore }}</span>
                <span class="font-mono">${{ item.cheapestPrice ? item.cheapestPrice.toFixed(2) : '0.00' }}</span>
              </span>
            </div>

            <!-- Stores Breakdown Grid -->
            <div class="grid grid-cols-2 gap-2 pt-1">
              <div 
                v-for="(price, storeName) in item.storePrices" 
                :key="storeName"
                class="p-2.5 rounded-xl border flex flex-col justify-between"
                :class="storeName === item.cheapestStore ? 'bg-emerald-950/30 border-emerald-500/40' : 'bg-slate-900/60 border-slate-700/60'">
                <span class="text-[11px] text-slate-400 font-medium truncate">{{ storeName }}</span>
                <div class="flex items-baseline justify-between mt-1">
                  <span class="text-sm font-mono font-bold" :class="storeName === item.cheapestStore ? 'text-emerald-300' : 'text-white'">
                    ${{ Number(price).toFixed(2) }}
                  </span>
                  <span class="text-[10px] text-slate-500 font-mono">/{{ item.unit || 'ea' }}</span>
                </div>
              </div>
            </div>

          </div>
        </div>

        <div v-else class="text-center py-10 bg-slate-950/40 rounded-2xl border border-slate-800/80 p-6">
          <Store class="w-10 h-10 text-slate-600 mx-auto mb-2" />
          <h4 class="text-sm font-bold text-slate-300">Not Enough Cross-Store Purchases Yet</h4>
          <p class="text-xs text-slate-400 max-w-md mx-auto mt-1">
            As you log receipts from multiple stores (e.g. Costco, Kroger, Fred Meyer), side-by-side deal comparisons will automatically populate here!
          </p>
        </div>

      </div>

      <!-- Store Overview Cards -->
      <div class="bg-slate-900/90 border border-slate-800 rounded-3xl p-6 shadow-lg space-y-4">
        <h3 class="text-lg font-bold text-white flex items-center gap-2">
          <Store class="w-5 h-5 text-sky-400" />
          <span>Stores Summary</span>
        </h3>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
          <div 
            v-for="s in storeComparison.stores" 
            :key="s.storeName"
            class="bg-slate-800/70 border border-slate-700/80 rounded-2xl p-4 flex flex-col justify-between">
            <div>
              <div class="flex items-center justify-between">
                <h4 class="font-bold text-white text-base">{{ s.storeName }}</h4>
                <span class="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full bg-slate-900 text-slate-300 border border-slate-700">
                  {{ s.transactionCount }} items
                </span>
              </div>
              <div class="mt-3 space-y-1">
                <div class="flex justify-between text-xs">
                  <span class="text-slate-400">Total Spent:</span>
                  <span class="font-mono font-bold text-emerald-400">${{ s.totalSpend.toFixed(2) }}</span>
                </div>
                <div class="flex justify-between text-xs">
                  <span class="text-slate-400">Avg Unit Price:</span>
                  <span class="font-mono font-bold text-slate-200">${{ s.averageUnitPrice.toFixed(2) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- ========================================================================= -->
    <!-- TAB 2: MONTHLY GROCERY SPEND                                              -->
    <!-- ========================================================================= -->
    <div v-if="activeTab === 'spend'" class="space-y-6">
      <div class="bg-slate-900/90 border border-slate-800 rounded-3xl p-6 shadow-lg space-y-5">
        <div>
          <h3 class="text-lg font-bold text-white flex items-center gap-2">
            <DollarSign class="w-5 h-5 text-emerald-400" />
            <span>Monthly Expenditure History</span>
          </h3>
          <p class="text-xs text-slate-400 mt-0.5">
            Aggregated spend across all receipts by calendar month.
          </p>
        </div>

        <div v-if="monthlySpend.length > 0" class="space-y-4">
          <!-- Monthly Spend Bar Chart -->
          <div class="bg-slate-950/60 border border-slate-800 rounded-2xl p-4 space-y-3">
            <span class="text-xs font-semibold text-slate-300 block mb-2">Month-over-Month Totals</span>
            <div class="space-y-2.5">
              <div v-for="m in monthlySpend" :key="m.month" class="space-y-1">
                <div class="flex justify-between text-xs font-mono">
                  <span class="font-bold text-slate-300">{{ formatMonthName(m.month) }}</span>
                  <span class="font-bold text-emerald-400">${{ m.totalSpend.toFixed(2) }} ({{ m.transactionCount }} purchases)</span>
                </div>
                <!-- Spend bar -->
                <div class="w-full bg-slate-800 rounded-full h-3 overflow-hidden flex">
                  <div 
                    class="bg-gradient-to-r from-brand-500 to-emerald-400 h-full rounded-full transition-all duration-500"
                    :style="{ width: `${getSpendPercent(m.totalSpend)}%` }"></div>
                </div>
              </div>
            </div>
          </div>

          <!-- Monthly Category Breakdown -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
            <div 
              v-for="m in monthlySpend" 
              :key="'card-' + m.month"
              class="bg-slate-800/70 border border-slate-700/80 rounded-2xl p-4.5 space-y-3">
              <div class="flex items-center justify-between border-b border-slate-700/80 pb-2">
                <span class="font-bold text-white text-sm">{{ formatMonthName(m.month) }}</span>
                <span class="text-sm font-mono font-bold text-emerald-400">${{ m.totalSpend.toFixed(2) }}</span>
              </div>

              <!-- By Category -->
              <div>
                <span class="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block mb-1.5">By Category</span>
                <div class="space-y-1">
                  <div 
                    v-for="(amt, cat) in m.byCategory" 
                    :key="cat"
                    class="flex items-center justify-between text-xs py-0.5">
                    <span class="text-slate-300 truncate">{{ cat }}</span>
                    <span class="font-mono text-slate-200 font-semibold">${{ Number(amt).toFixed(2) }}</span>
                  </div>
                </div>
              </div>

              <!-- By Store -->
              <div class="pt-2 border-t border-slate-700/50">
                <span class="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block mb-1.5">By Store</span>
                <div class="space-y-1">
                  <div 
                    v-for="(amt, store) in m.byStore" 
                    :key="store"
                    class="flex items-center justify-between text-xs py-0.5">
                    <span class="text-slate-400 truncate">{{ store }}</span>
                    <span class="font-mono text-slate-300">${{ Number(amt).toFixed(2) }}</span>
                  </div>
                </div>
              </div>

            </div>
          </div>
        </div>

        <div v-else class="text-center py-10 bg-slate-950/40 rounded-2xl border border-slate-800/80 p-6">
          <DollarSign class="w-10 h-10 text-slate-600 mx-auto mb-2" />
          <h4 class="text-sm font-bold text-slate-300">No Spend Records Logged</h4>
          <p class="text-xs text-slate-400 max-w-md mx-auto mt-1">
            Import digital receipts or add purchase prices when stocking items to see monthly expenditure charts here!
          </p>
        </div>
      </div>
    </div>

    <!-- ========================================================================= -->
    <!-- TAB 3: ITEM PRICE HISTORY & TREND TRACKER                                 -->
    <!-- ========================================================================= -->
    <div v-if="activeTab === 'prices'" class="space-y-6">
      <div class="bg-slate-900/90 border border-slate-800 rounded-3xl p-6 shadow-lg space-y-5">
        
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h3 class="text-lg font-bold text-white flex items-center gap-2">
              <TrendingUp class="w-5 h-5 text-purple-400" />
              <span>Staple Price History Tracker</span>
            </h3>
            <p class="text-xs text-slate-400 mt-0.5">
              Inspect inflation and historical unit prices for specific pantry items.
            </p>
          </div>

          <!-- Search Filter -->
          <div class="relative w-full sm:w-64">
            <input 
              v-model="searchQuery" 
              @input="debounceSearch"
              placeholder="Search staple (e.g. Milk, Eggs)..." 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500 pl-8" />
            <Search class="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-2.5" />
          </div>
        </div>

        <!-- Price Trend Stats Banner -->
        <div v-if="priceHistory.recordCount > 0" class="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div class="p-3 bg-slate-800/80 rounded-2xl border border-slate-700/80">
            <span class="text-[11px] text-slate-400 block mb-0.5">Lowest Price Ever</span>
            <span class="text-lg font-bold font-mono text-emerald-400">${{ priceHistory.lowestPrice.toFixed(2) }}</span>
            <span v-if="priceHistory.cheapestStore" class="text-[10px] text-slate-500 block truncate">at {{ priceHistory.cheapestStore }}</span>
          </div>

          <div class="p-3 bg-slate-800/80 rounded-2xl border border-slate-700/80">
            <span class="text-[11px] text-slate-400 block mb-0.5">Average Price</span>
            <span class="text-lg font-bold font-mono text-white">${{ priceHistory.averagePrice.toFixed(2) }}</span>
            <span class="text-[10px] text-slate-500 block">Across all purchases</span>
          </div>

          <div class="p-3 bg-slate-800/80 rounded-2xl border border-slate-700/80">
            <span class="text-[11px] text-slate-400 block mb-0.5">Highest Price Paid</span>
            <span class="text-lg font-bold font-mono text-rose-400">${{ priceHistory.highestPrice.toFixed(2) }}</span>
            <span class="text-[10px] text-slate-500 block">Peak price</span>
          </div>

          <div class="p-3 bg-slate-800/80 rounded-2xl border border-slate-700/80">
            <span class="text-[11px] text-slate-400 block mb-0.5">Data Points</span>
            <span class="text-lg font-bold font-mono text-sky-400">{{ priceHistory.recordCount }}</span>
            <span class="text-[10px] text-slate-500 block">Transactions recorded</span>
          </div>
        </div>

        <!-- Price Log Table -->
        <div v-if="priceHistory.pricePoints && priceHistory.pricePoints.length > 0" class="overflow-x-auto">
          <table class="w-full text-left text-xs text-slate-300">
            <thead class="bg-slate-950/60 text-slate-400 uppercase text-[10px] tracking-wider border-b border-slate-800">
              <tr>
                <th class="px-4 py-3">Date</th>
                <th class="px-4 py-3">Item</th>
                <th class="px-4 py-3">Store</th>
                <th class="px-4 py-3 text-right">Unit Price</th>
                <th class="px-4 py-3 text-right">Qty</th>
                <th class="px-4 py-3 text-right">Total</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-800/60">
              <tr v-for="(p, idx) in priceHistory.pricePoints" :key="idx" class="hover:bg-slate-800/40 transition-colors">
                <td class="px-4 py-2.5 font-mono text-slate-400">{{ p.date }}</td>
                <td class="px-4 py-2.5 font-semibold text-white">{{ p.itemName }}</td>
                <td class="px-4 py-2.5">
                  <span class="px-2 py-0.5 rounded-lg bg-slate-800 border border-slate-700 text-[11px] text-slate-300 font-medium">
                    {{ p.store }}
                  </span>
                </td>
                <td class="px-4 py-2.5 text-right font-mono font-bold" :class="p.unitPrice === priceHistory.lowestPrice ? 'text-emerald-400' : 'text-white'">
                  ${{ Number(p.unitPrice).toFixed(2) }}
                  <span v-if="p.unitPrice === priceHistory.lowestPrice" class="ml-1 text-[9px] px-1 py-0.2 rounded bg-emerald-500/20 text-emerald-300">BEST</span>
                </td>
                <td class="px-4 py-2.5 text-right font-mono text-slate-400">{{ p.quantity }} {{ p.unit }}</td>
                <td class="px-4 py-2.5 text-right font-mono font-bold text-slate-200">${{ Number(p.totalPrice).toFixed(2) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else class="text-center py-10 bg-slate-950/40 rounded-2xl border border-slate-800/80 p-6">
          <TrendingUp class="w-10 h-10 text-slate-600 mx-auto mb-2" />
          <h4 class="text-sm font-bold text-slate-300">No Historical Records Found</h4>
          <p class="text-xs text-slate-400 max-w-md mx-auto mt-1">
            Try searching for a different item name or import receipts to populate price records.
          </p>
        </div>

      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { 
  BarChart3, DollarSign, Store, Package, Sparkles, TrendingUp, RefreshCw, 
  Receipt, Search 
} from 'lucide-vue-next';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const { showToast } = useToast();

const isLoading = ref(false);
const activeTab = ref('stores');
const searchQuery = ref('');

const tabs = [
  { id: 'stores', label: 'Store vs Store Deals', icon: Store },
  { id: 'spend', label: 'Monthly Spend', icon: DollarSign },
  { id: 'prices', label: 'Item Price Trends', icon: TrendingUp },
];

const summary = reactive({
  totalSpend: 0,
  totalTransactions: 0,
  distinctStoresCount: 0,
  distinctItemsCount: 0,
});

const storeComparison = reactive({
  stores: [],
  crossStoreComparisons: [],
});

const monthlySpend = ref([]);

const priceHistory = reactive({
  pricePoints: [],
  lowestPrice: 0,
  highestPrice: 0,
  averagePrice: 0,
  cheapestStore: '',
  recordCount: 0,
});

function formatMonthName(ym) {
  if (!ym) return '';
  const [year, month] = ym.split('-');
  const date = new Date(year, parseInt(month, 10) - 1, 1);
  return date.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
}

function getSpendPercent(spend) {
  const max = Math.max(...monthlySpend.value.map(m => m.totalSpend), 1);
  return Math.min(100, Math.round((spend / max) * 100));
}

let searchDebounceTimer = null;
function debounceSearch() {
  clearTimeout(searchDebounceTimer);
  searchDebounceTimer = setTimeout(() => {
    loadPriceHistory();
  }, 300);
}

async function loadSummary() {
  try {
    const res = await api.getSpendSummary();
    Object.assign(summary, res.data);
  } catch (err) {
    console.error('Failed to load spend summary:', err);
  }
}

async function loadStoreComparison() {
  try {
    const res = await api.getStoreComparison();
    Object.assign(storeComparison, res.data);
  } catch (err) {
    console.error('Failed to load store comparison:', err);
  }
}

async function loadMonthlySpend() {
  try {
    const res = await api.getMonthlySpend();
    monthlySpend.value = res.data || [];
  } catch (err) {
    console.error('Failed to load monthly spend:', err);
  }
}

async function loadPriceHistory() {
  try {
    const res = await api.getPriceHistory({ query: searchQuery.value });
    Object.assign(priceHistory, res.data);
  } catch (err) {
    console.error('Failed to load price history:', err);
  }
}

async function refreshData() {
  isLoading.value = true;
  try {
    await Promise.all([
      loadSummary(),
      loadStoreComparison(),
      loadMonthlySpend(),
      loadPriceHistory(),
    ]);
  } catch (err) {
    showToast('Failed to refresh analytics data: ' + err.message, 'error');
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => {
  refreshData();
});
</script>
