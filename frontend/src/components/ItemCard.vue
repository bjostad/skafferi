<template>
  <div class="bg-slate-900 border border-slate-700/80 hover:border-brand-400/70 rounded-2xl overflow-hidden transition-all duration-200 shadow-lg hover:shadow-2xl flex flex-col justify-between group ring-1 ring-white/5">
    
    <!-- Top Section: Item Photo as Background with Gradient Overlay -->
    <div class="relative h-40 w-full overflow-hidden bg-slate-950 flex flex-col justify-between p-3.5">
      
      <!-- Background Image -->
      <div 
        v-if="item.imageUrl" 
        class="absolute inset-0 bg-cover bg-center transition-transform duration-300 group-hover:scale-105"
        :style="{ backgroundImage: `url(${item.imageUrl})` }">
      </div>
      <!-- Fallback Background Pattern if no image -->
      <div 
        v-else 
        class="absolute inset-0 bg-gradient-to-tr from-slate-900 via-slate-850 to-slate-800 flex items-center justify-center">
        <Package class="w-16 h-16 text-slate-700/80 -rotate-12 transition-transform group-hover:scale-110" />
      </div>

      <!-- High-contrast Dark Gradient Overlay for Crisp Text Readability -->
      <div class="absolute inset-0 bg-gradient-to-t from-slate-900 via-slate-900/70 to-black/60"></div>

      <!-- Top Row Badges (Floating on Photo) -->
      <div class="relative z-10 flex items-start justify-between gap-1">
        <span v-if="item.brand" class="text-[11px] font-bold text-white px-2 py-0.5 rounded-md bg-black/75 backdrop-blur-md border border-white/20 truncate max-w-[140px] shadow-sm">
          {{ item.brand }}
        </span>
        <span v-else></span>

        <!-- Expiration / Freshness Status Badge -->
        <span 
          v-if="item.freshCheckNeeded"
          title="Freshness check needed"
          class="inline-flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded-full bg-amber-500 text-slate-950 shadow-md backdrop-blur-md border border-amber-300">
          <Clock class="w-3 h-3 text-slate-950" />
          Check ({{ item.daysInStorage }}d)
        </span>
        <span 
          v-else-if="item.expiryStatus === 'EXPIRED'"
          class="inline-flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded-full bg-rose-600 text-white shadow-md backdrop-blur-md border border-rose-400">
          <AlertCircle class="w-3 h-3 text-white" />
          Expired
        </span>
        <span 
          v-else-if="item.expiryStatus === 'EXPIRING_SOON'"
          class="inline-flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded-full bg-amber-500 text-slate-950 shadow-md backdrop-blur-md border border-amber-300">
          <Clock class="w-3 h-3 text-slate-950" />
          {{ item.daysUntilEarliestExpiry }}d left
        </span>
        <span 
          v-else-if="item.expiryStatus === 'FRESH'"
          class="inline-flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded-full bg-emerald-600 text-white shadow-md backdrop-blur-md border border-emerald-400">
          <CheckCircle class="w-3 h-3 text-white" />
          Fresh
        </span>
      </div>

      <!-- Bottom of Top Section: Title & Location/Category Chips -->
      <div class="relative z-10">
        <h3 class="font-bold text-white text-base leading-snug drop-shadow-md truncate tracking-tight" :title="item.name">
          {{ item.name }}
        </h3>

        <div class="flex items-center gap-1.5 mt-2 flex-wrap">
          <!-- Category Badge -->
          <span v-if="item.category" class="text-[10px] font-semibold px-2 py-0.5 rounded-md bg-black/75 backdrop-blur-md text-slate-200 border border-white/20 shadow-sm">
            {{ item.category.name }}
          </span>
          <!-- Location Badge -->
          <span v-if="item.defaultLocation" class="text-[10px] font-semibold px-2 py-0.5 rounded-md bg-slate-800/90 backdrop-blur-md text-brand-300 border border-brand-400/30 shadow-sm">
            {{ item.defaultLocation.name }}
          </span>
          <!-- Package Size Badge -->
          <span v-if="item.packageSize" class="text-[10px] font-mono font-medium px-2 py-0.5 rounded-md bg-black/75 backdrop-blur-md text-slate-200 border border-white/20 shadow-sm">
            {{ item.packageSize }}
          </span>
          <!-- Perishable Badge -->
          <span v-if="item.perishable" class="text-[10px] font-semibold px-2 py-0.5 rounded-md bg-emerald-950/90 backdrop-blur-md text-emerald-300 border border-emerald-500/40 shadow-sm">
            Fresh Produce
          </span>
        </div>
      </div>

    </div>

    <!-- Middle & Bottom Section: Batches + Stepper Controls -->
    <div class="p-3.5 bg-slate-850/80 flex flex-col justify-between flex-1">
      
      <!-- Batches Breakdown (if multiple) -->
      <div v-if="item.batches && item.batches.length > 0" class="text-xs text-slate-300 space-y-1 mb-3">
        <div v-for="batch in item.batches" :key="batch.id" class="flex items-center justify-between text-[11px] bg-slate-800/80 px-2.5 py-1 rounded-lg border border-slate-700/70">
          <span class="truncate font-medium text-slate-200">{{ batch.locationName }} ({{ batch.quantity }} {{ batch.unit }})</span>
          <span v-if="batch.expirationDate" class="text-slate-300 font-mono text-[10px] font-semibold">Exp: {{ batch.expirationDate }}</span>
        </div>
      </div>
      <div v-else class="mb-2"></div>

      <!-- Bottom Controls: Stepper & Quick Actions -->
      <div class="pt-2.5 border-t border-slate-700/60 flex items-center justify-between gap-2">
        <!-- Fast +/- Stepper -->
        <div class="flex items-center gap-1 bg-slate-900 rounded-xl p-1 border border-slate-700 shadow-inner">
          <button 
            @click="adjust(-1)"
            :disabled="item.totalQuantity <= 0"
            title="Deduct 1"
            class="w-7 h-7 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-25 text-white font-bold flex items-center justify-center transition-colors">
            <Minus class="w-3.5 h-3.5" />
          </button>
          
          <span class="px-2 font-mono font-bold text-sm min-w-[2.75rem] text-center" :class="item.isOutOfStock ? 'text-rose-400' : (item.isLowStock ? 'text-amber-400' : 'text-white')">
            {{ formatQty(item.totalQuantity) }}
          </span>

          <button 
            @click="adjust(1)"
            title="Add 1"
            class="w-7 h-7 rounded-lg bg-slate-800 hover:bg-slate-700 text-white font-bold flex items-center justify-center transition-colors">
            <Plus class="w-3.5 h-3.5" />
          </button>
        </div>

        <span class="text-xs text-slate-300 font-semibold truncate max-w-[4rem]">{{ item.defaultUnit }}</span>

        <!-- Action Buttons -->
        <div class="flex items-center gap-1">
          <button 
            @click="$emit('move-zone', item)"
            title="Move / Transfer Storage Zone"
            class="p-1.5 rounded-lg text-slate-300 hover:text-brand-300 hover:bg-slate-800 border border-transparent hover:border-slate-700 transition-colors">
            <ArrowRightLeft class="w-3.5 h-3.5" />
          </button>
          <button 
            @click="$emit('add-batch', item)"
            title="Add Dated Batch"
            class="p-1.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 border border-transparent hover:border-slate-700 transition-colors">
            <CalendarPlus class="w-3.5 h-3.5 text-brand-400" />
          </button>
          <button 
            @click="$emit('history', item)"
            title="Purchase & Price History"
            class="p-1.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 border border-transparent hover:border-slate-700 transition-colors">
            <Receipt class="w-3.5 h-3.5 text-emerald-400" />
          </button>
          <button 
            @click="$emit('edit', item)"
            title="Edit Item"
            class="p-1.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 border border-transparent hover:border-slate-700 transition-colors">
            <Edit2 class="w-3.5 h-3.5" />
          </button>
          <button 
            v-if="item.totalQuantity > 0"
            @click="$emit('consume', item)"
            title="Mark All as Consumed / Used Up"
            class="p-1.5 rounded-lg text-slate-300 hover:text-rose-400 hover:bg-rose-500/10 border border-transparent hover:border-rose-500/30 transition-colors">
            <CheckCheck class="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

    </div>

  </div>
</template>

<script setup>
import { 
  Package, AlertCircle, Clock, CheckCircle, Plus, Minus, 
  CalendarPlus, Edit2, ArrowRightLeft, CheckCheck, Receipt
} from 'lucide-vue-next';

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(['adjust', 'edit', 'add-batch', 'move-zone', 'consume', 'history']);

function adjust(delta) {
  emit('adjust', { itemId: props.item.id, delta });
}

function formatQty(qty) {
  return Number.isInteger(qty) ? qty : qty.toFixed(1);
}
</script>
