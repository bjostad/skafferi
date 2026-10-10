<template>
  <div 
    class="bg-slate-900 border rounded-2xl transition-all duration-200 overflow-hidden shadow-sm hover:shadow-md"
    :class="[
      isExpanded ? 'border-brand-400/80 ring-1 ring-brand-400/20' : 'border-slate-800 hover:border-slate-700',
      accentBorderClass
    ]">
    
    <!-- Collapsed Row (Main Header Strip) -->
    <div 
      @click="toggleExpand"
      class="p-3 sm:px-4 sm:py-3 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 cursor-pointer select-none group">
      
      <!-- Left: Thumbnail + Title + Chips -->
      <div class="flex items-center gap-3 min-w-0 flex-1">
        <!-- Thumbnail or Fallback Icon -->
        <div class="w-10 h-10 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-center shrink-0 overflow-hidden relative">
          <img 
            v-if="item.imageUrl" 
            :src="item.imageUrl" 
            :alt="item.name"
            class="w-full h-full object-cover transition-transform group-hover:scale-105" />
          <Package v-else class="w-5 h-5 text-slate-600" />
        </div>

        <!-- Name, Brand, Package Size, Category & Location -->
        <div class="min-w-0 flex-1">
          <div class="flex items-center gap-2 flex-wrap">
            <h3 class="font-bold text-sm text-white truncate max-w-xs group-hover:text-brand-300 transition-colors" :title="item.name">
              {{ item.name }}
            </h3>

            <!-- Brand Badge -->
            <span v-if="item.brand" class="text-[10px] font-bold text-slate-300 px-2 py-0.5 rounded-md bg-slate-800 border border-slate-700/80 truncate max-w-[120px]">
              {{ item.brand }}
            </span>

            <!-- Package Size Badge -->
            <span v-if="item.packageSize" class="text-[10px] font-mono text-slate-400 px-1.5 py-0.5 rounded bg-slate-950/70 border border-slate-800/80">
              {{ item.packageSize }}
            </span>
          </div>

          <!-- Subtext Badges (Category, Location, Perishable) -->
          <div class="flex items-center gap-2 mt-1 text-[11px] text-slate-400 flex-wrap">
            <span v-if="item.defaultLocation" class="text-brand-300/90 font-medium flex items-center gap-1">
              {{ item.defaultLocation.name }}
            </span>

            <span v-if="item.defaultLocation && item.category" class="text-slate-600">&bull;</span>

            <span v-if="item.category" class="text-slate-300">
              {{ item.category.name }}
            </span>

            <!-- Perishable Fresh Food Flag -->
            <span v-if="item.perishable" class="px-1.5 py-0.2 rounded text-[10px] font-semibold bg-emerald-950/80 text-emerald-300 border border-emerald-500/30">
              Produce
            </span>
          </div>
        </div>
      </div>

      <!-- Right: Freshness Status + Fast Stepper + Expand Trigger -->
      <div class="flex items-center justify-between sm:justify-end gap-3 shrink-0" @click.stop>
        
        <!-- Freshness Status Pill -->
        <div class="shrink-0">
          <span 
            v-if="item.freshCheckNeeded"
            title="Freshness check needed"
            class="inline-flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-400/50">
            <Clock class="w-3 h-3 text-amber-400" />
            Check ({{ item.daysInStorage }}d)
          </span>
          <span 
            v-else-if="item.expiryStatus === 'EXPIRED'"
            class="inline-flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded-full bg-rose-600/20 text-rose-300 border border-rose-500/50">
            <AlertCircle class="w-3 h-3 text-rose-400" />
            Expired
          </span>
          <span 
            v-else-if="item.expiryStatus === 'EXPIRING_SOON'"
            class="inline-flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-400/50">
            <Clock class="w-3 h-3 text-amber-400" />
            {{ item.daysUntilEarliestExpiry }}d left
          </span>
          <span 
            v-else-if="item.expiryStatus === 'FRESH'"
            class="inline-flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded-full bg-emerald-600/20 text-emerald-300 border border-emerald-500/50">
            <CheckCircle class="w-3 h-3 text-emerald-400" />
            Fresh
          </span>
          <span 
            v-else
            class="text-[10px] text-slate-500 font-medium px-2 py-0.5">
            No expiry
          </span>
        </div>

        <!-- Fast Stepper Controls -->
        <div class="flex items-center gap-1 bg-slate-950 rounded-xl p-0.5 border border-slate-800 shadow-inner">
          <button 
            type="button"
            @click="adjust(-1)"
            :disabled="item.totalQuantity <= 0"
            title="Deduct 1"
            class="w-7 h-7 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-20 text-white font-bold flex items-center justify-center transition-colors">
            <Minus class="w-3.5 h-3.5" />
          </button>
          
          <span 
            class="px-2 font-mono font-bold text-xs min-w-[2.5rem] text-center" 
            :class="item.isOutOfStock ? 'text-rose-400' : (item.isLowStock ? 'text-amber-400' : 'text-white')">
            {{ formatQty(item.totalQuantity) }}
          </span>

          <button 
            type="button"
            @click="adjust(1)"
            title="Add 1"
            class="w-7 h-7 rounded-lg bg-slate-800 hover:bg-slate-700 text-white font-bold flex items-center justify-center transition-colors">
            <Plus class="w-3.5 h-3.5" />
          </button>
        </div>

        <span class="text-xs text-slate-400 font-medium hidden md:inline-block w-12 truncate">
          {{ item.defaultUnit }}
        </span>

        <!-- Chevron Expand Button -->
        <button 
          type="button"
          @click="toggleExpand"
          title="Toggle Details"
          class="p-1.5 rounded-xl hover:bg-slate-800 text-slate-400 hover:text-white transition-transform duration-200">
          <ChevronDown class="w-4 h-4 transition-transform duration-200" :class="{ 'rotate-180': isExpanded }" />
        </button>

      </div>

    </div>

    <!-- Expanded Accordion Content -->
    <div 
      v-if="isExpanded" 
      class="border-t border-slate-800 bg-slate-950/60 p-4 sm:p-5 space-y-4">
      
      <!-- Top Row: Batches Breakdown & Metadata Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        
        <!-- Batches Breakdown -->
        <div class="bg-slate-900/80 border border-slate-800 rounded-xl p-3.5">
          <div class="flex items-center justify-between mb-2.5 pb-2 border-b border-slate-800">
            <span class="text-xs font-bold text-slate-300">Storage Batches</span>
            <span class="text-[11px] font-mono text-slate-400">{{ (item.batches && item.batches.length) || 0 }} batch(es)</span>
          </div>

          <div v-if="item.batches && item.batches.length > 0" class="space-y-1.5 max-h-40 overflow-y-auto pr-1 scrollbar-thin">
            <div 
              v-for="batch in item.batches" 
              :key="batch.id" 
              class="flex items-center justify-between text-xs bg-slate-950/80 px-3 py-1.5 rounded-lg border border-slate-800">
              <div>
                <span class="font-medium text-slate-200">{{ batch.locationName }}</span>
                <span class="text-slate-400 font-mono text-[11px] ml-1.5">({{ batch.quantity }} {{ batch.unit }})</span>
              </div>
              <div class="flex items-center gap-2">
                <span v-if="batch.expirationDate" class="text-amber-400/90 font-mono text-[11px]">
                  Exp: {{ batch.expirationDate }}
                </span>
                <span v-else class="text-slate-500 text-[11px]">No exp</span>
              </div>
            </div>
          </div>
          <div v-else class="text-xs text-slate-500 py-3 text-center">
            No active dated batches recorded.
          </div>
        </div>

        <!-- Detailed Metadata Attributes -->
        <div class="bg-slate-900/80 border border-slate-800 rounded-xl p-3.5 flex flex-col justify-between">
          <div class="space-y-2 text-xs">
            <div class="flex items-center justify-between pb-2 border-b border-slate-800">
              <span class="font-bold text-slate-300">Item Details</span>
              <span v-if="item.barcode" class="font-mono text-[11px] text-slate-400">Barcode: {{ item.barcode }}</span>
            </div>

            <div class="grid grid-cols-2 gap-2 text-slate-300 pt-1">
              <div>
                <span class="text-slate-500 block text-[11px]">Min Threshold:</span>
                <span class="font-semibold">{{ item.minThreshold }} {{ item.defaultUnit }}</span>
              </div>
              <div>
                <span class="text-slate-500 block text-[11px]">Restock Amount:</span>
                <span class="font-semibold">{{ item.restockQuantity || 1 }} {{ item.defaultUnit }}</span>
              </div>
              <div>
                <span class="text-slate-500 block text-[11px]">Default Location:</span>
                <span class="font-semibold">{{ item.defaultLocation?.name || 'None' }}</span>
              </div>
              <div>
                <span class="text-slate-500 block text-[11px]">Days in Storage:</span>
                <span class="font-semibold">{{ item.daysInStorage !== null ? `${item.daysInStorage} days` : 'N/A' }}</span>
              </div>
            </div>

            <p v-if="item.notes" class="text-[11px] text-slate-400 italic bg-slate-950/70 p-2 rounded-lg border border-slate-800/80 mt-2">
              "{{ item.notes }}"
            </p>
          </div>
        </div>

      </div>

      <!-- Action Toolbar (With Explicit Text Buttons) -->
      <div class="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-slate-800">
        
        <!-- Primary Shopping Integration Action -->
        <button 
          type="button"
          @click="$emit('add-shopping-list', item)"
          title="Add this item to Bring! or Shopping List"
          class="px-3.5 py-2 rounded-xl bg-brand-400 hover:bg-brand-300 text-slate-950 text-xs font-bold shadow-sm flex items-center gap-2 transition-all">
          <ShoppingCart class="w-3.5 h-3.5" />
          <span>Add to Shopping List</span>
        </button>

        <!-- Secondary Action Buttons -->
        <div class="flex items-center gap-1.5 flex-wrap">
          
          <!-- Move Storage Zone -->
          <button 
            type="button"
            @click="$emit('move-zone', item)"
            title="Move / Transfer Storage Zone"
            class="px-3 py-1.5 rounded-xl text-xs font-medium text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 border border-slate-700 flex items-center gap-1.5 transition-colors">
            <ArrowRightLeft class="w-3.5 h-3.5 text-brand-400" />
            <span>Move Zone</span>
          </button>

          <!-- Add Dated Batch -->
          <button 
            type="button"
            @click="$emit('add-batch', item)"
            title="Add Dated Batch"
            class="px-3 py-1.5 rounded-xl text-xs font-medium text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 border border-slate-700 flex items-center gap-1.5 transition-colors">
            <CalendarPlus class="w-3.5 h-3.5 text-brand-400" />
            <span>+ Batch</span>
          </button>

          <!-- Purchase History -->
          <button 
            type="button"
            @click="$emit('history', item)"
            title="Purchase & Price History"
            class="px-3 py-1.5 rounded-xl text-xs font-medium text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 border border-slate-700 flex items-center gap-1.5 transition-colors">
            <Receipt class="w-3.5 h-3.5 text-emerald-400" />
            <span>History</span>
          </button>

          <!-- Edit Item (Modal Trigger as specified) -->
          <button 
            type="button"
            @click="$emit('edit', item)"
            title="Edit Item Details"
            class="px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-200 hover:text-white bg-slate-800 hover:bg-slate-700 border border-slate-700 flex items-center gap-1.5 transition-colors">
            <Edit2 class="w-3.5 h-3.5 text-amber-400" />
            <span>Edit Item</span>
          </button>

          <!-- Consume / Used Up -->
          <button 
            v-if="item.totalQuantity > 0"
            type="button"
            @click="$emit('consume', item)"
            title="Mark All as Consumed / Used Up"
            class="px-3 py-1.5 rounded-xl text-xs font-medium text-rose-300 hover:text-rose-200 bg-rose-950/40 hover:bg-rose-900/50 border border-rose-800/50 flex items-center gap-1.5 transition-colors">
            <CheckCheck class="w-3.5 h-3.5 text-rose-400" />
            <span>Consume</span>
          </button>

        </div>

      </div>

    </div>

  </div>
</template>

<script setup>
import { computed } from 'vue';
import { 
  Package, AlertCircle, Clock, CheckCircle, Plus, Minus, 
  CalendarPlus, Edit2, ArrowRightLeft, CheckCheck, Receipt, 
  ShoppingCart, ChevronDown 
} from 'lucide-vue-next';

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
  isExpanded: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits([
  'toggle-expand', 
  'adjust', 
  'edit', 
  'add-batch', 
  'move-zone', 
  'consume', 
  'history', 
  'add-shopping-list'
]);

function toggleExpand() {
  emit('toggle-expand', props.item.id);
}

function adjust(delta) {
  emit('adjust', { itemId: props.item.id, delta });
}

function formatQty(qty) {
  if (qty === null || qty === undefined) return '0';
  return Number.isInteger(qty) ? qty : qty.toFixed(1);
}

const accentBorderClass = computed(() => {
  if (props.item.expiryStatus === 'EXPIRED') {
    return 'border-l-4 border-l-rose-500';
  }
  if (props.item.expiryStatus === 'EXPIRING_SOON' || props.item.freshCheckNeeded) {
    return 'border-l-4 border-l-amber-500';
  }
  if (props.item.expiryStatus === 'FRESH') {
    return 'border-l-4 border-l-emerald-500';
  }
  if (props.item.isOutOfStock) {
    return 'border-l-4 border-l-slate-700 opacity-80';
  }
  return 'border-l-4 border-l-slate-700';
});
</script>
