<template>
  <div class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
    <div class="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-4xl overflow-hidden shadow-2xl flex flex-col max-h-[92vh]">
      
      <!-- Header -->
      <div class="p-4 border-b border-slate-800 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <Receipt class="w-5 h-5 text-brand-400" />
          <h2 class="font-bold text-lg text-white">
            {{ step === 'summary' ? 'Import Summary' : 'Import Digital Receipt' }}
          </h2>
        </div>
        <button @click="handleClose" class="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800">
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- Step 1: Input / Paste Tab -->
      <div v-if="step === 'input'" class="p-6 flex flex-col gap-4 overflow-y-auto">
        <div class="flex items-center justify-between gap-4 flex-wrap">
          <div>
            <h3 class="font-bold text-white text-base">Paste or Upload Receipt</h3>
            <p class="text-xs text-slate-400 mt-0.5">Paste text from Kroger "My Purchases", e-receipts, or upload a receipt export</p>
          </div>

          <!-- Store Selector (Pluggable Strategy) -->
          <div class="flex items-center gap-2">
            <label class="text-xs text-slate-400 font-medium">Store Strategy:</label>
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
          placeholder="Paste Kroger purchase history, receipt lines, or HTML here...&#10;&#10;Example:&#10;KROGER 2% MILK 1GAL   3.49&#10;UPC: 0001111041600&#10;SIMPLE TRUTH ORGANIC EGGS 12CT   4.29&#10;BANANAS 2.3 LB   1.35&#10;TOTAL   9.13"
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
            <span>{{ isParsing ? 'Parsing & Enriching UPCs...' : 'Parse Receipt' }}</span>
          </button>
        </div>
      </div>

      <!-- Step 2: Interactive Review & Staging Checklist -->
      <div v-else-if="step === 'review'" class="p-6 flex flex-col gap-4 overflow-y-auto">
        
        <!-- Header Info Bar -->
        <div class="p-3.5 bg-slate-950/70 border border-slate-800 rounded-xl flex items-center justify-between gap-4 flex-wrap">
          <div class="flex items-center gap-3 flex-wrap">
            <div>
              <label class="text-[10px] text-slate-400 font-semibold block mb-0.5">Store / Merchant</label>
              <input 
                v-model="editStoreName" 
                class="bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1 text-xs text-brand-300 font-bold focus:outline-none focus:border-brand-500" />
            </div>

            <div>
              <label class="text-[10px] text-slate-400 font-semibold block mb-0.5">Purchase Date</label>
              <input 
                type="date"
                v-model="editPurchaseDate" 
                class="bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1 text-xs text-slate-200 focus:outline-none focus:border-brand-500" />
            </div>
          </div>

          <div class="flex items-center gap-4 text-right">
            <div>
              <span class="text-[10px] text-slate-400 block font-medium">Selected Items</span>
              <span class="font-bold text-xs text-slate-200 font-mono">
                {{ selectedCount }} of {{ parseResult.items.length }}
              </span>
            </div>
            <div>
              <span class="text-[10px] text-slate-400 block font-medium">Calculated Cost</span>
              <span class="font-bold text-sm text-emerald-400 font-mono">
                \${{ calculatedTotal.toFixed(2) }}
              </span>
            </div>
          </div>
        </div>

        <!-- Items Review List -->
        <div class="space-y-2.5 max-h-[50vh] overflow-y-auto pr-1">
          <div 
            v-for="(item, idx) in parseResult.items" 
            :key="idx" 
            class="bg-slate-800/80 border border-slate-700/80 rounded-xl p-3.5 space-y-2.5 transition-colors"
            :class="{ 'opacity-50': !item.selected }">
            
            <!-- Row 1: Checkbox, Name, Brand, Price, Quantity -->
            <div class="flex items-center gap-3">
              <input 
                type="checkbox" 
                v-model="item.selected" 
                class="w-4 h-4 rounded text-brand-600 bg-slate-900 border-slate-700 focus:ring-brand-500 flex-shrink-0" />

              <!-- Thumbnail if enriched -->
              <div v-if="item.imageUrl" class="w-8 h-8 rounded-lg overflow-hidden bg-slate-950 border border-slate-700 flex-shrink-0 hidden sm:block">
                <img :src="item.imageUrl" :alt="item.cleanName" class="w-full h-full object-cover" />
              </div>

              <!-- Item Name & UPC Match Badge -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-1">
                  <input 
                    v-model="item.cleanName" 
                    placeholder="Item name"
                    class="bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1 text-xs text-white font-semibold flex-1 focus:outline-none focus:border-brand-500" />
                  
                  <!-- Enrichment Badge -->
                  <span 
                    v-if="item.matchedItemId" 
                    class="text-[9px] font-bold px-1.5 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-700 whitespace-nowrap">
                    Pantry Match
                  </span>
                  <span 
                    v-else-if="item.barcode" 
                    class="text-[9px] font-bold px-1.5 py-0.5 rounded bg-blue-950 text-blue-300 border border-blue-800 whitespace-nowrap">
                    UPC Enriched
                  </span>
                </div>
              </div>

              <!-- Quantity & Unit -->
              <div class="flex items-center gap-1 flex-shrink-0">
                <input 
                  type="number" 
                  step="any"
                  v-model.number="item.quantity" 
                  title="Quantity"
                  class="w-14 bg-slate-900 border border-slate-700 rounded-lg px-1.5 py-1 text-xs text-white text-center font-mono focus:outline-none focus:border-brand-500" />
                <input 
                  v-model="item.unit" 
                  title="Unit"
                  class="w-12 bg-slate-900 border border-slate-700 rounded-lg px-1 py-1 text-xs text-slate-300 text-center focus:outline-none focus:border-brand-500" />
              </div>

              <!-- Price Per Item -->
              <div class="w-20 flex-shrink-0 text-right">
                <div class="relative">
                  <span class="absolute left-1.5 top-1/2 -translate-y-1/2 text-slate-500 text-xs">$</span>
                  <input 
                    type="number"
                    step="0.01"
                    v-model.number="item.unitPrice" 
                    placeholder="0.00"
                    title="Price per unit"
                    class="w-full bg-slate-900 border border-slate-700 rounded-lg pl-4 pr-1 py-1 text-xs text-emerald-400 font-mono text-right focus:outline-none focus:border-brand-500" />
                </div>
              </div>
            </div>

            <!-- Row 2: Location Selector & Expiration Date with Quick Adjusters -->
            <div class="pl-7 grid grid-cols-1 sm:grid-cols-2 gap-2.5 items-center pt-1 border-t border-slate-700/50">
              <!-- Destination Storage Location -->
              <div class="flex items-center gap-2">
                <label class="text-[10px] text-slate-400 font-semibold whitespace-nowrap">Location:</label>
                <select 
                  v-model="item.suggestedLocationId"
                  class="bg-slate-900 border border-slate-700 rounded-lg px-2 py-1 text-xs text-brand-300 flex-1 focus:outline-none focus:border-brand-500">
                  <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
                </select>
              </div>

              <!-- Expiration Date & Presets -->
              <div class="flex items-center gap-1.5 flex-wrap">
                <label class="text-[10px] text-slate-400 font-semibold whitespace-nowrap">Expires:</label>
                <input 
                  type="date"
                  v-model="item.estimatedExpirationDate" 
                  class="bg-slate-900 border border-slate-700 rounded-lg px-2 py-0.5 text-xs text-slate-200 focus:outline-none focus:border-brand-500 font-mono" />
                
                <!-- Quick Date Chips -->
                <div class="flex items-center gap-1 text-[9px] font-semibold">
                  <button 
                    type="button" 
                    @click="setExpiryDays(item, 4)" 
                    class="px-1.5 py-0.5 rounded bg-slate-900 hover:bg-slate-700 text-slate-300 border border-slate-700" 
                    title="Expires in 4 days">+4d</button>
                  <button 
                    type="button" 
                    @click="setExpiryDays(item, 7)" 
                    class="px-1.5 py-0.5 rounded bg-slate-900 hover:bg-slate-700 text-slate-300 border border-slate-700" 
                    title="Expires in 1 week">+1w</button>
                  <button 
                    type="button" 
                    @click="setExpiryDays(item, 14)" 
                    class="px-1.5 py-0.5 rounded bg-slate-900 hover:bg-slate-700 text-slate-300 border border-slate-700" 
                    title="Expires in 2 weeks">+2w</button>
                  <button 
                    type="button" 
                    @click="setExpiryMonths(item, 1)" 
                    class="px-1.5 py-0.5 rounded bg-slate-900 hover:bg-slate-700 text-slate-300 border border-slate-700" 
                    title="Expires in 1 month">+1m</button>
                  <button 
                    type="button" 
                    @click="item.estimatedExpirationDate = null" 
                    class="px-1.5 py-0.5 rounded bg-slate-900 hover:bg-rose-900/40 text-slate-400 hover:text-rose-300 border border-slate-700" 
                    title="No expiration">Clear</button>
                </div>
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
            :disabled="isCommitting || selectedCount === 0"
            class="px-5 py-2.5 rounded-xl bg-brand-300 hover:bg-brand-200 disabled:opacity-50 text-slate-950 font-bold text-xs shadow-sm flex items-center gap-2">
            <Check class="w-4 h-4" />
            <span>{{ isCommitting ? 'Committing...' : `Commit ${selectedCount} Items to Pantry` }}</span>
          </button>
        </div>
      </div>

      <!-- Step 3: Import Summary -->
      <div v-else-if="step === 'summary'" class="p-6 flex flex-col gap-5 overflow-y-auto">
        <!-- Success Banner -->
        <div class="flex items-center gap-3 p-4 bg-emerald-950/40 border border-emerald-500/30 rounded-2xl text-emerald-200">
          <div class="w-10 h-10 rounded-xl bg-emerald-500/20 flex items-center justify-center flex-shrink-0">
            <CheckCircle2 class="w-6 h-6 text-emerald-400" />
          </div>
          <div>
            <h3 class="font-bold text-base text-white">Receipt Successfully Imported!</h3>
            <p class="text-xs text-emerald-300/80">
              {{ committedSummary.items.length }} items have been added to your pantry and logged to your purchase history ledger.
            </p>
          </div>
        </div>

        <!-- Metric Cards -->
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div class="bg-slate-950/80 border border-slate-800 rounded-xl p-3.5 text-center">
            <span class="text-[10px] text-slate-400 uppercase font-bold tracking-wider block">Items Added</span>
            <span class="text-2xl font-black text-white font-mono mt-1 block">
              {{ committedSummary.items.length }}
            </span>
            <span class="text-[10px] text-brand-300 mt-0.5 block">batches stocked</span>
          </div>

          <div class="bg-slate-950/80 border border-slate-800 rounded-xl p-3.5 text-center">
            <span class="text-[10px] text-slate-400 uppercase font-bold tracking-wider block">Total Spent</span>
            <span class="text-2xl font-black text-emerald-400 font-mono mt-1 block">
              \${{ committedSummary.totalCost.toFixed(2) }}
            </span>
            <span class="text-[10px] text-slate-400 mt-0.5 block">logged to purchase history</span>
          </div>

          <div class="bg-slate-950/80 border border-slate-800 rounded-xl p-3.5 text-center">
            <span class="text-[10px] text-slate-400 uppercase font-bold tracking-wider block">Store & Date</span>
            <span class="text-base font-bold text-slate-200 truncate mt-1.5 block">
              {{ committedSummary.storeName }}
            </span>
            <span class="text-[10px] text-slate-400 mt-0.5 block">
              {{ committedSummary.date || 'Today' }}
            </span>
          </div>
        </div>

        <!-- Detailed Breakdown -->
        <div class="space-y-2">
          <h4 class="text-xs font-bold text-slate-300 uppercase tracking-wider">Imported Items Breakdown</h4>
          <div class="bg-slate-950/60 border border-slate-800 rounded-xl overflow-hidden max-h-52 overflow-y-auto">
            <div 
              v-for="(item, idx) in committedSummary.items" 
              :key="idx" 
              class="p-2.5 border-b border-slate-800/80 last:border-b-0 flex items-center justify-between text-xs gap-3">
              <div class="min-w-0 flex-1">
                <span class="font-bold text-white block truncate">{{ item.name }}</span>
                <span class="text-[10px] text-slate-400">
                  {{ item.quantity }} {{ item.unit }} • Stored in: <span class="text-brand-300">{{ getLocationName(item.locationId) }}</span>
                </span>
              </div>
              <div class="text-right flex-shrink-0">
                <span v-if="item.unitPrice != null" class="font-mono font-bold text-emerald-400 block">
                  \${{ (item.unitPrice * (item.quantity || 1)).toFixed(2) }}
                </span>
                <span v-if="item.expirationDate" class="text-[10px] text-slate-400 font-mono block">
                  Exp: {{ item.expirationDate }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex justify-between items-center pt-2 border-t border-slate-800">
          <button 
            @click="resetForAnother" 
            class="px-4 py-2 rounded-xl text-xs text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 font-semibold flex items-center gap-1.5">
            <Plus class="w-3.5 h-3.5" />
            <span>Import Another Receipt</span>
          </button>
          
          <button 
            @click="finishAndClose" 
            class="px-6 py-2.5 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs shadow-sm flex items-center gap-1.5">
            <Check class="w-4 h-4" />
            <span>View in Inventory</span>
          </button>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { Receipt, X, Upload, Sparkles, Check, CheckCircle2, Plus } from 'lucide-vue-next';
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

const editStoreName = ref('');
const editPurchaseDate = ref(new Date().toISOString().substring(0, 10));

const committedSummary = ref({
  storeName: '',
  date: '',
  totalCost: 0,
  items: [],
});

const selectedCount = computed(() => {
  if (!parseResult.value || !parseResult.value.items) return 0;
  return parseResult.value.items.filter(i => i.selected).length;
});

const calculatedTotal = computed(() => {
  if (!parseResult.value || !parseResult.value.items) return 0;
  return parseResult.value.items
    .filter(i => i.selected && i.unitPrice != null)
    .reduce((sum, i) => sum + (i.unitPrice * (i.quantity || 1)), 0);
});

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
    editStoreName.value = res.data.storeName || 'Grocery Store';
    if (res.data.transactionDate) {
      editPurchaseDate.value = String(res.data.transactionDate).substring(0, 10);
    }
    step.value = 'review';
  } catch (err) {
    showToast('Failed to parse receipt: ' + (err.response?.data?.message || err.message), 'error');
  } finally {
    isParsing.value = false;
  }
}

function setExpiryDays(item, days) {
  const d = new Date();
  d.setDate(d.getDate() + days);
  item.estimatedExpirationDate = d.toISOString().substring(0, 10);
}

function setExpiryMonths(item, months) {
  const d = new Date();
  d.setMonth(d.getMonth() + months);
  item.estimatedExpirationDate = d.toISOString().substring(0, 10);
}

function getLocationName(locId) {
  const match = locations.value.find(l => l.id === locId);
  return match ? match.name : 'Pantry';
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
    expirationDate: i.estimatedExpirationDate || null,
    purchasedDate: editPurchaseDate.value || null,
    unitPrice: i.unitPrice != null ? i.unitPrice : null,
    totalPrice: i.unitPrice != null && i.quantity ? i.unitPrice * i.quantity : null,
    store: editStoreName.value || null,
    barcode: i.barcode,
    imageUrl: i.imageUrl,
  }));

  if (selectedItems.length === 0) return;

  isCommitting.value = true;
  try {
    await api.commitReceipt(editStoreName.value, selectedItems, editPurchaseDate.value);
    
    // Save to summary state
    committedSummary.value = {
      storeName: editStoreName.value,
      date: editPurchaseDate.value,
      totalCost: selectedItems.reduce((acc, i) => acc + (i.totalPrice || 0), 0),
      items: selectedItems,
    };

    showToast(`Successfully imported ${selectedItems.length} items!`);
    emit('committed');
    step.value = 'summary';
  } catch (err) {
    showToast('Failed to commit receipt: ' + err.message, 'error');
  } finally {
    isCommitting.value = false;
  }
}

function resetForAnother() {
  rawReceiptText.value = '';
  fileName.value = '';
  parseResult.value = null;
  step.value = 'input';
}

function finishAndClose() {
  emit('committed');
  emit('close');
}

function handleClose() {
  if (step.value === 'summary') {
    emit('committed');
  }
  emit('close');
}
</script>
