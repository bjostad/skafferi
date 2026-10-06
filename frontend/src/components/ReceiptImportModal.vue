<template>
  <div class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
    <div class="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-3xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">
      
      <!-- Header -->
      <div class="p-4 border-b border-slate-800 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <Receipt class="w-5 h-5 text-brand-400" />
          <h2 class="font-bold text-lg text-white">Import Digital Receipt</h2>
        </div>
        <button @click="$emit('close')" class="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800">
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- Step 1: Input / Paste Tab -->
      <div v-if="step === 'input'" class="p-6 flex flex-col gap-4 overflow-y-auto">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="font-bold text-white text-base">Paste or Upload Receipt</h3>
            <p class="text-xs text-slate-400 mt-0.5">Copy text from Kroger "My Purchases" or drop a digital receipt export</p>
          </div>

          <!-- Store Selector (Pluggable Strategy) -->
          <div class="flex items-center gap-2">
            <label class="text-xs text-slate-400 font-medium">Store:</label>
            <select 
              v-model="selectedProvider"
              class="bg-slate-800 border border-slate-700 rounded-xl px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-brand-500">
              <option value="auto">Auto-Detect</option>
              <option v-for="prov in providers" :key="prov.id" :value="prov.id">{{ prov.name }}</option>
            </select>
          </div>
        </div>

        <!-- Textarea for Pasting Receipt -->
        <textarea 
          v-model="rawReceiptText"
          rows="10"
          placeholder="Paste Kroger purchase history, receipt lines, or HTML here...&#10;&#10;Example:&#10;KROGER 2% MILK 1GAL   3.49&#10;SIMPLE TRUTH ORGANIC EGGS 12CT   4.29&#10;BANANAS 2.3 LB   1.35&#10;TOTAL   9.13"
          class="w-full bg-slate-950/80 border border-slate-800 rounded-xl p-3.5 text-xs text-slate-200 font-mono placeholder-slate-600 focus:outline-none focus:border-brand-500"></textarea>

        <!-- File Drop Upload -->
        <div class="border-2 border-dashed border-slate-700 rounded-xl p-4 text-center hover:border-brand-500/50 transition-colors">
          <input type="file" ref="fileInput" @change="handleFileUpload" class="hidden" accept=".txt,.html,.json,.csv" />
          <button @click="$refs.fileInput.click()" class="text-xs text-brand-400 font-semibold hover:underline flex items-center gap-1.5 mx-auto">
            <Upload class="w-4 h-4" />
            Or click to upload a receipt file (.txt, .html, .json)
          </button>
          <span v-if="fileName" class="text-xs text-slate-300 block mt-1">Loaded: {{ fileName }}</span>
        </div>

        <div class="flex justify-end gap-2 mt-2">
          <button @click="$emit('close')" class="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white">Cancel</button>
          <button 
            @click="parseReceipt"
            :disabled="!rawReceiptText || isParsing"
            class="px-5 py-2 rounded-xl bg-brand-300 hover:bg-brand-200 disabled:opacity-50 text-slate-950 font-bold text-xs shadow-sm flex items-center gap-2">
            <Sparkles class="w-4 h-4" />
            <span>{{ isParsing ? 'Parsing...' : 'Parse Receipt' }}</span>
          </button>
        </div>
      </div>

      <!-- Step 2: Interactive Review & Staging Checklist -->
      <div v-else-if="step === 'review'" class="p-6 flex flex-col gap-4 overflow-y-auto">
        <div class="flex items-center justify-between border-b border-slate-800 pb-3">
          <div>
            <span class="text-xs text-brand-400 font-bold uppercase tracking-wider">{{ parseResult.storeName }}</span>
            <h3 class="font-bold text-white text-base">Review & Stock Pantry ({{ parseResult.items.filter(i => i.selected).length }} of {{ parseResult.items.length }} selected)</h3>
          </div>
          <div v-if="parseResult.totalAmount" class="text-right">
            <span class="text-xs text-slate-400 block">Total</span>
            <span class="font-bold text-slate-200 font-mono">\${{ parseResult.totalAmount.toFixed(2) }}</span>
          </div>
        </div>

        <!-- Items Review List -->
        <div class="space-y-2.5 max-h-[50vh] overflow-y-auto pr-1">
          <div 
            v-for="(item, idx) in parseResult.items" 
            :key="idx" 
            class="bg-slate-800/80 border border-slate-700/80 rounded-xl p-3 flex items-center gap-3 transition-colors"
            :class="{ 'opacity-50': !item.selected }">
            
            <input 
              type="checkbox" 
              v-model="item.selected" 
              class="w-4 h-4 rounded text-brand-600 bg-slate-900 border-slate-700 focus:ring-brand-500" />

            <!-- Item Details & Input Fields -->
            <div class="flex-1 min-w-0 grid grid-cols-1 sm:grid-cols-4 gap-2 items-center">
              <!-- Item Name -->
              <div class="sm:col-span-2">
                <input 
                  v-model="item.cleanName" 
                  class="bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1 text-xs text-white font-semibold w-full focus:outline-none focus:border-brand-500" />
                <span v-if="item.unitPrice" class="text-[10px] text-slate-400 font-mono mt-0.5 block">\${{ item.unitPrice.toFixed(2) }}</span>
              </div>

              <!-- Quantity & Unit -->
              <div class="flex items-center gap-1.5">
                <input 
                  type="number" 
                  step="any"
                  v-model.number="item.quantity" 
                  class="w-16 bg-slate-900 border border-slate-700 rounded-lg px-2 py-1 text-xs text-white text-center font-mono focus:outline-none focus:border-brand-500" />
                <input 
                  v-model="item.unit" 
                  class="w-14 bg-slate-900 border border-slate-700 rounded-lg px-1.5 py-1 text-xs text-slate-300 text-center focus:outline-none focus:border-brand-500" />
              </div>

              <!-- Destination Location -->
              <div>
                <select 
                  v-model="item.suggestedLocationId"
                  class="bg-slate-900 border border-slate-700 rounded-lg px-2 py-1 text-xs text-brand-300 w-full focus:outline-none focus:border-brand-500">
                  <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
                </select>
              </div>
            </div>

          </div>
        </div>

        <!-- Footer Actions -->
        <div class="flex justify-between items-center pt-3 border-t border-slate-800">
          <button @click="step = 'input'" class="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white">
            Back to Input
          </button>
          <button 
            @click="commitReceipt"
            :disabled="isCommitting || parseResult.items.filter(i => i.selected).length === 0"
            class="px-5 py-2.5 rounded-xl bg-brand-300 hover:bg-brand-200 disabled:opacity-50 text-slate-950 font-bold text-xs shadow-sm flex items-center gap-2">
            <Check class="w-4 h-4" />
            <span>{{ isCommitting ? 'Committing...' : 'Commit to Pantry' }}</span>
          </button>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Receipt, X, Upload, Sparkles, Check } from 'lucide-vue-next';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const { showToast } = useToast();

const emit = defineEmits(['close', 'committed']);

const step = ref('input');
const rawReceiptText = ref('');
const fileName = ref('');
const selectedProvider = ref('auto');
const providers = ref([]);
const locations = ref([]);
const isParsing = ref(false);
const isCommitting = ref(false);
const parseResult = ref(null);

onMounted(async () => {
  try {
    const [pRes, lRes] = await Promise.all([
      api.getReceiptProviders(),
      api.getLocations(),
    ]);
    providers.value = pRes.data;
    locations.value = lRes.data;
  } catch (err) {
    console.error('Failed to load receipt providers/locations:', err);
  }
});

function handleFileUpload(e) {
  const file = e.target.files[0];
  if (!file) return;
  fileName.value = file.name;
  const reader = new FileReader();
  reader.onload = (ev) => {
    rawReceiptText.value = ev.target.result;
  };
  reader.readAsText(file);
}

async function parseReceipt() {
  if (!rawReceiptText.value) return;
  isParsing.value = true;
  try {
    const res = await api.parseReceipt(rawReceiptText.value, selectedProvider.value);
    parseResult.value = res.data;
    step.value = 'review';
  } catch (err) {
    showToast('Failed to parse receipt: ' + (err.response?.data?.message || err.message), 'error');
  } finally {
    isParsing.value = false;
  }
}

async function commitReceipt() {
  const selectedItems = parseResult.value.items.filter(i => i.selected).map(i => ({
    itemId: i.matchedItemId,
    name: i.cleanName,
    brand: i.brand,
    categoryId: i.suggestedCategoryId,
    locationId: i.suggestedLocationId,
    quantity: i.quantity,
    unit: i.unit,
    expirationDate: i.estimatedExpirationDate,
    unitPrice: i.unitPrice,
    barcode: i.barcode,
    imageUrl: i.imageUrl,
  }));

  if (selectedItems.length === 0) return;

  isCommitting.value = true;
  try {
    await api.commitReceipt(parseResult.value.storeName, selectedItems);
    showToast(`Imported ${selectedItems.length} items from ${parseResult.value.storeName || 'receipt'}!`);
    emit('committed');
    emit('close');
  } catch (err) {
    showToast('Failed to commit receipt: ' + err.message, 'error');
  } finally {
    isCommitting.value = false;
  }
}
</script>
