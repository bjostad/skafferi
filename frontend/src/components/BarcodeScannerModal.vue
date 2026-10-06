<template>
  <div class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
    <div class="bg-slate-850 bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl flex flex-col">
      
      <!-- Modal Header -->
      <div class="p-4 border-b border-slate-800 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <ScanBarcode class="w-5 h-5 text-brand-400" />
          <h2 class="font-bold text-lg text-white">Scan Barcode</h2>
        </div>
        <button @click="closeModal" class="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800">
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- Camera Scanner Viewfinder -->
      <div class="p-4 flex flex-col items-center">
        <div id="barcode-reader" class="w-full max-w-sm rounded-xl overflow-hidden bg-black border-2 border-dashed border-brand-500/60 min-h-[260px]"></div>
        <p class="text-xs text-slate-400 mt-3 text-center">Point your camera at the grocery barcode (UPC / EAN)</p>
      </div>

      <!-- Manual Barcode Input Fallback -->
      <div class="p-4 border-t border-slate-800 bg-slate-950/50">
        <label class="text-xs font-semibold text-slate-400 block mb-1.5">Or enter barcode manually:</label>
        <div class="flex gap-2">
          <input 
            v-model="manualCode"
            type="text" 
            placeholder="e.g. 011110416002"
            @keyup.enter="handleLookup(manualCode)"
            class="flex-1 bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-brand-500" />
          <button 
            @click="handleLookup(manualCode)"
            :disabled="!manualCode"
            class="px-4 py-2 bg-brand-300 hover:bg-brand-200 disabled:opacity-50 text-slate-950 text-xs font-bold rounded-xl shadow-sm transition-colors">
            Lookup
          </button>
        </div>
      </div>

      <!-- Scanned Result Card (if detected) -->
      <div v-if="lookupResult" class="p-4 border-t border-slate-800 bg-slate-800/80">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-lg bg-slate-900 border border-slate-700 flex items-center justify-center flex-shrink-0 overflow-hidden">
            <img v-if="lookupResult.imageUrl" :src="lookupResult.imageUrl" class="w-full h-full object-contain" />
            <Package v-else class="w-6 h-6 text-slate-500" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between">
              <span class="text-xs text-brand-400 font-bold uppercase tracking-wider">{{ lookupResult.source }}</span>
              <span class="text-xs text-slate-400 font-mono">{{ lookupResult.barcode }}</span>
            </div>
            <h4 class="font-bold text-white text-sm truncate">{{ lookupResult.name || 'Unknown Product' }}</h4>
            <div class="flex items-center gap-2 mt-0.5">
              <span v-if="lookupResult.brand" class="text-xs text-slate-400 truncate">{{ lookupResult.brand }}</span>
              <span v-if="lookupResult.existingItemId" class="text-[11px] font-mono font-semibold px-2 py-0.2 rounded-md bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                In Pantry: {{ lookupResult.currentQuantity ?? 0 }} {{ lookupResult.defaultUnit }}
              </span>
            </div>
          </div>
        </div>

        <div class="mt-4 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2.5 pt-3 border-t border-slate-700/80">
          <button @click="resetScan" class="px-3 py-1.5 text-xs text-slate-400 hover:text-white rounded-lg text-left">
            &larr; Scan Another
          </button>

          <div class="flex items-center gap-2 flex-wrap justify-end">
            <!-- If item already exists: show Adjust Inventory AND choose quantity to Add -->
            <template v-if="lookupResult.existingItemId">
              <button 
                @click="proceedWithResult" 
                class="px-3.5 py-2 text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-xl transition-colors">
                Adjust Inventory
              </button>

              <!-- Quantity Stepper & Add to Inventory Button -->
              <div class="flex items-center gap-1.5">
                <div class="flex items-center bg-slate-900 border border-slate-700 rounded-xl p-0.5">
                  <button 
                    type="button" 
                    @click="addQuantity = Math.max(1, addQuantity - 1)" 
                    class="w-7 h-7 rounded-lg bg-slate-800 hover:bg-slate-700 text-white flex items-center justify-center font-bold text-xs">
                    -
                  </button>
                  <input 
                    type="number" 
                    min="1" 
                    v-model.number="addQuantity" 
                    class="w-10 text-center bg-transparent font-mono font-bold text-xs text-white focus:outline-none" />
                  <button 
                    type="button" 
                    @click="addQuantity++" 
                    class="w-7 h-7 rounded-lg bg-slate-800 hover:bg-slate-700 text-white flex items-center justify-center font-bold text-xs">
                    +
                  </button>
                </div>

                <button 
                  @click="quickAddExistingStock" 
                  class="px-4 py-2 text-xs font-bold bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl shadow-sm transition-colors flex items-center gap-1.5">
                  <Plus class="w-3.5 h-3.5" />
                  <span>Add to Inventory (+{{ addQuantity }})</span>
                </button>
              </div>
            </template>

            <!-- If brand new item: standard Add to Inventory modal button -->
            <template v-else>
              <button 
                @click="proceedWithResult" 
                class="px-5 py-2 text-xs font-bold bg-brand-300 hover:bg-brand-200 text-slate-950 rounded-xl shadow-sm transition-colors flex items-center gap-1.5">
                <Plus class="w-3.5 h-3.5" />
                <span>Add to Inventory</span>
              </button>
            </template>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { Html5Qrcode } from 'html5-qrcode';
import { ScanBarcode, X, Package, Plus } from 'lucide-vue-next';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const { showToast } = useToast();

const emit = defineEmits(['close', 'found-item', 'item-updated']);

let html5QrCode = null;
const manualCode = ref('');
const lookupResult = ref(null);
const isScanning = ref(false);
const addQuantity = ref(1);

onMounted(async () => {
  try {
    html5QrCode = new Html5Qrcode('barcode-reader');
    await html5QrCode.start(
      { facingMode: 'environment' },
      { fps: 10, qrbox: { width: 250, height: 180 } },
      (decodedText) => {
        handleLookup(decodedText);
      },
      (error) => {
        // ignore scan frame errors
      }
    );
    isScanning.value = true;
  } catch (err) {
    console.warn('Camera barcode scanner error:', err);
  }
});

onBeforeUnmount(() => {
  stopScanner();
});

async function stopScanner() {
  if (html5QrCode && isScanning.value) {
    try {
      await html5QrCode.stop();
      isScanning.value = false;
    } catch (e) {
      console.warn('Error stopping scanner:', e);
    }
  }
}

async function handleLookup(code) {
  if (!code) return;
  try {
    const res = await api.lookupBarcode(code.trim());
    lookupResult.value = res.data;
    addQuantity.value = 1;
  } catch (err) {
    console.error('Barcode lookup failed:', err);
  }
}

function resetScan() {
  lookupResult.value = null;
  manualCode.value = '';
  addQuantity.value = 1;
}

function proceedWithResult() {
  emit('found-item', lookupResult.value);
  closeModal();
}

async function quickAddExistingStock() {
  if (!lookupResult.value?.existingItemId) return;
  try {
    await api.adjustQuantity(lookupResult.value.existingItemId, addQuantity.value);
    const unitStr = lookupResult.value.defaultUnit ? ` ${lookupResult.value.defaultUnit}` : '';
    showToast(`Added ${addQuantity.value}${unitStr} of "${lookupResult.value.name}" to inventory!`);
    emit('item-updated');
    closeModal();
  } catch (err) {
    showToast('Failed to add stock: ' + err.message, 'error');
  }
}

function closeModal() {
  stopScanner();
  emit('close');
}
</script>
