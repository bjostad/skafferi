<template>
  <div class="space-y-6 pb-16">
    
    <!-- Header with Threshold Selector -->
    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 bg-slate-800/80 p-5 rounded-3xl border border-slate-700/80 shadow-sm">
      <div>
        <div class="flex items-center gap-2">
          <Clock class="w-5 h-5 text-amber-400" />
          <h2 class="text-xl font-bold text-white">Expiring Items &amp; Freshness Tracker</h2>
        </div>
        <p class="text-xs text-slate-400 mt-1">Review dated foods nearing expiration and perishable produce to reduce food waste.</p>
      </div>

      <!-- Days Filter Pills -->
      <div class="flex items-center gap-1.5 bg-slate-900/90 p-1 rounded-xl border border-slate-700">
        <button 
          v-for="d in [3, 7, 14, 30]" 
          :key="d"
          @click="changeDays(d)"
          class="px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors"
          :class="daysThreshold === d ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30' : 'text-slate-400 hover:text-white'">
          Next {{ d }} Days
        </button>
      </div>
    </div>

    <!-- Fresh Food & Produce Checks Section (Story 3) -->
    <div v-if="freshCheckItems.length > 0" class="bg-amber-950/20 border border-amber-500/30 rounded-3xl p-5 space-y-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2.5">
          <div class="w-9 h-9 rounded-xl bg-amber-500/20 text-amber-300 flex items-center justify-center font-bold">
            <Sparkles class="w-5 h-5 text-amber-400" />
          </div>
          <div>
            <h3 class="font-bold text-base text-white">Fresh Produce &amp; Perishables Check</h3>
            <p class="text-xs text-slate-400">These items have been in storage without an expiration date. Time to check freshness!</p>
          </div>
        </div>
        <span class="text-xs font-bold px-2.5 py-1 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30">
          {{ freshCheckItems.length }} to check
        </span>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 pt-1">
        <div 
          v-for="item in freshCheckItems" 
          :key="item.id"
          class="bg-slate-900/90 border border-slate-700/80 rounded-2xl p-4 flex flex-col justify-between hover:border-amber-500/40 transition-colors">
          <div>
            <div class="flex items-center justify-between gap-2">
              <span class="font-bold text-sm text-white truncate">{{ item.name }}</span>
              <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 whitespace-nowrap">
                {{ item.daysInStorage }}d in storage
              </span>
            </div>
            <p class="text-xs text-slate-400 mt-1 flex items-center gap-2">
              <span>Qty: <strong class="text-slate-200">{{ item.totalQuantity }} {{ item.defaultUnit }}</strong></span>
              <span v-if="item.defaultLocation" class="text-slate-500">&bull; {{ item.defaultLocation.name }}</span>
            </p>
            <p v-if="item.notes" class="text-[11px] text-slate-400 mt-1.5 italic bg-slate-950/60 p-2 rounded-lg border border-slate-800">
              "{{ item.notes }}"
            </p>
          </div>

          <div class="flex items-center gap-2 pt-3 mt-3 border-t border-slate-800">
            <button 
              @click="markStillFresh(item)"
              class="flex-1 px-2.5 py-1.5 bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/30 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors"
              title="Reset freshness timer">
              <RotateCcw class="w-3.5 h-3.5" />
              <span>Still Fresh</span>
            </button>
            <button 
              @click="openAddBatch(item)"
              class="flex-1 px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors"
              title="Set an expiration date">
              <CalendarPlus class="w-3.5 h-3.5 text-brand-400" />
              <span>Set Expiry</span>
            </button>
            <button 
              @click="consumeItem(item)"
              class="p-1.5 rounded-xl bg-slate-800 hover:bg-rose-500/20 text-slate-400 hover:text-rose-300 border border-slate-700 transition-colors"
              title="Mark as consumed">
              <Check class="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Expiring Items Grid -->
    <div class="space-y-3">
      <h3 class="font-bold text-base text-white flex items-center gap-2">
        <span>Dated Expirations</span>
        <span class="text-xs font-normal text-slate-400">({{ items.length }} items)</span>
      </h3>

      <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <div v-for="n in 4" :key="n" class="bg-slate-800/40 rounded-2xl p-4 border border-slate-700/40 h-44 animate-pulse"></div>
      </div>

      <div v-else-if="items.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <ItemCard 
          v-for="item in items" 
          :key="item.id" 
          :item="item" 
          @adjust="handleAdjust"
          @edit="openEdit"
          @add-batch="openAddBatch" />
      </div>

      <!-- Empty State -->
      <div v-else class="text-center py-16 bg-slate-800/30 rounded-3xl border border-slate-800 p-8 max-w-lg mx-auto">
        <div class="w-16 h-16 rounded-2xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center mx-auto mb-4 border border-emerald-500/20">
          <CheckCircle class="w-8 h-8" />
        </div>
        <h3 class="text-lg font-bold text-white mb-1">No items expiring soon!</h3>
        <p class="text-xs text-slate-400">Everything dated in your pantry is fresh within the next {{ daysThreshold }} days.</p>
      </div>
    </div>

    <!-- Modals -->
    <AddItemModal 
      v-if="editItemModalOpen" 
      :initial-data="itemToEdit" 
      :is-edit="true" 
      @close="editItemModalOpen = false" 
      @saved="fetchExpiring" />

    <AddBatchModal 
      v-if="addBatchModalOpen" 
      :item="itemForBatch" 
      @close="addBatchModalOpen = false" 
      @saved="fetchExpiring" />

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Clock, CheckCircle, Sparkles, RotateCcw, CalendarPlus, Check } from 'lucide-vue-next';
import ItemCard from '../components/ItemCard.vue';
import AddItemModal from '../components/AddItemModal.vue';
import AddBatchModal from '../components/AddBatchModal.vue';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const { showToast } = useToast();

const items = ref([]);
const freshCheckItems = ref([]);
const loading = ref(true);
const daysThreshold = ref(7);

const editItemModalOpen = ref(false);
const itemToEdit = ref(null);

const addBatchModalOpen = ref(false);
const itemForBatch = ref(null);

onMounted(() => {
  fetchExpiring();
});

async function fetchExpiring() {
  loading.value = true;
  try {
    const [expRes, allRes] = await Promise.all([
      api.getExpiringSoon(daysThreshold.value),
      api.getItems(),
    ]);
    items.value = expRes.data;
    freshCheckItems.value = allRes.data.filter(i => i.freshCheckNeeded && i.totalQuantity > 0);
  } catch (err) {
    console.error('Failed to fetch expiring items:', err);
  } finally {
    loading.value = false;
  }
}

function changeDays(days) {
  daysThreshold.value = days;
  fetchExpiring();
}

async function handleAdjust({ itemId, delta }) {
  try {
    const res = await api.adjustQuantity(itemId, delta);
    const index = items.value.findIndex(i => i.id === itemId);
    if (index !== -1) {
      items.value[index] = res.data;
    }
    const freshIdx = freshCheckItems.value.findIndex(i => i.id === itemId);
    if (freshIdx !== -1) {
      freshCheckItems.value[freshIdx] = res.data;
    }
  } catch (err) {
    console.error('Failed to adjust quantity:', err);
  }
}

async function markStillFresh(item) {
  try {
    await api.resetFreshness(item.id);
    showToast(`"${item.name}" marked as still fresh! Timer reset.`);
    await fetchExpiring();
  } catch (err) {
    showToast('Failed to reset freshness timer: ' + err.message, 'error');
  }
}

async function consumeItem(item) {
  try {
    await api.adjustQuantity(item.id, -item.totalQuantity);
    showToast(`Marked ${item.name} as consumed!`);
    await fetchExpiring();
  } catch (err) {
    showToast('Failed to consume item: ' + err.message, 'error');
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
</script>
