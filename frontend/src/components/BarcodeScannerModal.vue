<template>
  <div class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4 overflow-y-auto">
    <div class="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-lg shadow-2xl flex flex-col my-auto max-h-[92vh] overflow-hidden">
      
      <!-- Modal Header (fixed at top of modal) -->
      <div class="p-3.5 sm:p-4 border-b border-slate-800 flex items-center justify-between flex-shrink-0 bg-slate-900">
        <div class="flex items-center gap-2">
          <ScanBarcode class="w-5 h-5 text-brand-400" />
          <h2 class="font-bold text-base sm:text-lg text-white">Scan Barcode</h2>
        </div>
        <button @click="closeModal" class="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors">
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- Modal Body (scrollable if viewport is small) -->
      <div class="overflow-y-auto flex-1 p-3.5 sm:p-4 space-y-4">

        <!-- Camera Scanner Viewfinder -->
        <div class="flex flex-col items-center">
          <div class="relative w-full max-w-sm rounded-xl overflow-hidden bg-black border-2 border-dashed border-brand-500/60 min-h-[220px] max-h-[260px] flex items-center justify-center">
            <div id="barcode-reader" class="w-full h-full"></div>
            
            <!-- Scan Overlay Badge when detected and locked -->
            <div v-if="isLocked" class="absolute inset-0 bg-slate-950/70 backdrop-blur-xs flex flex-col items-center justify-center p-4 text-center z-10">
              <div class="w-10 h-10 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center mb-2 border border-emerald-500/30">
                <Check class="w-6 h-6" />
              </div>
              <p class="text-xs font-bold text-white">Barcode Captured!</p>
              <p class="text-[11px] text-slate-300 font-mono mt-0.5">{{ lastScannedCode }}</p>
              <button 
                type="button" 
                @click="resumeScanning" 
                class="mt-2.5 px-3 py-1 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-600 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-colors">
                <RefreshCw class="w-3.5 h-3.5" />
                <span>Scan Another</span>
              </button>
            </div>
          </div>
          <p class="text-xs text-slate-400 mt-2 text-center">Point your camera at a grocery barcode (UPC / EAN)</p>
        </div>

        <!-- Manual Barcode Input Fallback -->
        <div class="p-3 rounded-xl border border-slate-800 bg-slate-950/60">
          <label class="text-xs font-semibold text-slate-400 block mb-1.5">Or enter barcode manually:</label>
          <div class="flex gap-2">
            <input 
              v-model="manualCode"
              type="text" 
              placeholder="e.g. 011110416002"
              @keyup.enter="handleManualLookup"
              class="flex-1 bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs sm:text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-brand-500" />
            <button 
              @click="handleManualLookup"
              :disabled="!manualCode || isLookingUp"
              class="px-4 py-2 bg-brand-300 hover:bg-brand-200 disabled:opacity-50 text-slate-950 text-xs font-bold rounded-xl shadow-sm transition-colors flex items-center gap-1">
              <Loader2 v-if="isLookingUp" class="w-3.5 h-3.5 animate-spin" />
              <span>Lookup</span>
            </button>
          </div>
        </div>

        <!-- Scanned Result Card (steady card that does NOT flicker or disappear) -->
        <div v-if="lookupResult" class="p-4 rounded-xl border border-slate-700 bg-slate-800/90 shadow-lg space-y-3">
          <div class="flex items-start gap-3">
            <div class="w-14 h-14 rounded-lg bg-slate-900 border border-slate-700 flex items-center justify-center flex-shrink-0 overflow-hidden">
              <img v-if="lookupResult.imageUrl" :src="lookupResult.imageUrl" class="w-full h-full object-contain" />
              <Package v-else class="w-7 h-7 text-slate-500" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between gap-1">
                <span class="text-[10px] text-brand-400 font-bold uppercase tracking-wider bg-brand-500/10 px-1.5 py-0.5 rounded border border-brand-500/20">{{ lookupResult.source || 'Barcode' }}</span>
                <span class="text-[11px] text-slate-400 font-mono">{{ lookupResult.barcode }}</span>
              </div>
              <h4 class="font-bold text-white text-sm sm:text-base mt-1 line-clamp-2 leading-snug">{{ lookupResult.name || 'Unknown Product' }}</h4>
              <div class="flex items-center gap-2 mt-1 flex-wrap">
                <span v-if="lookupResult.brand" class="text-xs text-slate-400">{{ lookupResult.brand }}</span>
                <span v-if="lookupResult.packageSize" class="text-[11px] text-slate-400 font-mono bg-slate-900 px-1.5 py-0.5 rounded">{{ lookupResult.packageSize }}</span>
                <span v-if="lookupResult.existingItemId" class="text-[11px] font-mono font-semibold px-2 py-0.5 rounded-md bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                  In Pantry: {{ lookupResult.currentQuantity ?? 0 }} {{ lookupResult.defaultUnit }}
                </span>
              </div>
            </div>
          </div>

          <!-- Bottom Action Buttons: Clear, Adjust, and Add to Inventory -->
          <div class="pt-3 border-t border-slate-700/80 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2.5">
            <button 
              type="button" 
              @click="resumeScanning" 
              class="px-2.5 py-1.5 text-xs text-slate-400 hover:text-white rounded-lg text-left transition-colors flex items-center gap-1">
              <RefreshCw class="w-3.5 h-3.5" />
              <span>Scan Different Item</span>
            </button>

            <div class="flex items-center gap-2 flex-wrap justify-end">
              <!-- If item already exists in pantry: show Adjust Inventory AND Quick-Add with stepper -->
              <template v-if="lookupResult.existingItemId">
                <button 
                  type="button"
                  @click="proceedWithResult" 
                  class="px-3.5 py-2 text-xs font-semibold bg-slate-800 hover:bg-slate-750 text-slate-200 border border-slate-600 rounded-xl transition-colors">
                  Adjust Inventory
                </button>

                <!-- Stepper + Quick Add Button -->
                <div class="flex items-center gap-1.5">
                  <div class="flex items-center bg-slate-950 border border-slate-700 rounded-xl p-0.5">
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
                      class="w-9 text-center bg-transparent font-mono font-bold text-xs text-white focus:outline-none" />
                    <button 
                      type="button" 
                      @click="addQuantity++" 
                      class="w-7 h-7 rounded-lg bg-slate-800 hover:bg-slate-700 text-white flex items-center justify-center font-bold text-xs">
                      +
                    </button>
                  </div>

                  <button 
                    type="button"
                    @click="quickAddExistingStock" 
                    class="px-3.5 py-2 text-xs font-bold bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl shadow-md transition-colors flex items-center gap-1.5">
                    <Plus class="w-3.5 h-3.5" />
                    <span>Add (+{{ addQuantity }})</span>
                  </button>
                </div>
              </template>

              <!-- If brand new item: standard Add to Inventory button -->
              <template v-else>
                <button 
                  type="button"
                  @click="proceedWithResult" 
                  class="px-5 py-2 text-xs font-bold bg-brand-300 hover:bg-brand-200 text-slate-950 rounded-xl shadow-md transition-colors flex items-center gap-1.5">
                  <Plus class="w-3.5 h-3.5" />
                  <span>Add to Inventory</span>
                </button>
              </template>
            </div>
          </div>
        </div>

      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { Html5Qrcode } from 'html5-qrcode';
import { ScanBarcode, X, Package, Plus, Check, RefreshCw, Loader2 } from 'lucide-vue-next';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const { showToast } = useToast();

const emit = defineEmits(['close', 'found-item', 'item-updated']);

let html5QrCode = null;
const manualCode = ref('');
const lookupResult = ref(null);
const isScanning = ref(false);
const isLocked = ref(false);
const isLookingUp = ref(false);
const lastScannedCode = ref('');
const addQuantity = ref(1);

onMounted(async () => {
  await initScanner();
});

onBeforeUnmount(() => {
  stopScanner();
});

async function initScanner() {
  try {
    if (!html5QrCode) {
      html5QrCode = new Html5Qrcode('barcode-reader');
    }
    await html5QrCode.start(
      { facingMode: 'environment' },
      { 
        fps: 10, 
        qrbox: (viewfinderWidth, viewfinderHeight) => {
          const width = Math.floor(viewfinderWidth * 0.85);
          const height = Math.floor(viewfinderHeight * 0.7);
          return { width, height };
        },
        aspectRatio: 1.333
      },
      (decodedText) => {
        onBarcodeDetected(decodedText);
      },
      () => {
        // Ignore frame errors
      }
    );
    isScanning.value = true;
  } catch (err) {
    console.warn('Camera barcode scanner error:', err);
  }
}

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

async function onBarcodeDetected(code) {
  if (!code || isLocked.value || isLookingUp.value) return;
  const cleanCode = code.trim();
  if (cleanCode === lastScannedCode.value && lookupResult.value) {
    return;
  }

  isLocked.value = true;
  lastScannedCode.value = cleanCode;

  // Pause scanner video feed so it does not keep flickering or re-scanning
  if (html5QrCode && isScanning.value) {
    try {
      html5QrCode.pause(true);
    } catch (e) {
      console.warn('Could not pause camera feed:', e);
    }
  }

  await performLookup(cleanCode);
}

async function handleManualLookup() {
  if (!manualCode.value) return;
  const code = manualCode.value.trim();
  isLocked.value = true;
  lastScannedCode.value = code;

  if (html5QrCode && isScanning.value) {
    try {
      html5QrCode.pause(true);
    } catch (e) {
      // ignore
    }
  }

  await performLookup(code);
}

async function performLookup(code) {
  isLookingUp.value = true;
  try {
    const res = await api.lookupBarcode(code);
    lookupResult.value = res.data;
    addQuantity.value = 1;
  } catch (err) {
    console.error('Barcode lookup failed:', err);
    showToast('Failed to lookup barcode: ' + (err.message || 'Unknown error'), 'error');
  } finally {
    isLookingUp.value = false;
  }
}

function resumeScanning() {
  lookupResult.value = null;
  manualCode.value = '';
  addQuantity.value = 1;
  isLocked.value = false;
  lastScannedCode.value = '';

  if (html5QrCode) {
    try {
      html5QrCode.resume();
    } catch (e) {
      // If resume fails (e.g. state was not paused), restart cleanly
      initScanner();
    }
  }
}

function proceedWithResult() {
  if (!lookupResult.value) return;
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

