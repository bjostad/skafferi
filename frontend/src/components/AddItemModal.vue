<template>
  <div class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
    <div class="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl flex flex-col">
      
      <!-- Header -->
      <div class="p-4 border-b border-slate-800 flex items-center justify-between">
        <h2 class="font-bold text-lg text-white">{{ isEditingExisting ? 'Edit Item' : 'Add Item to Pantry' }}</h2>
        <button @click="$emit('close')" class="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800">
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- Form -->
      <form @submit.prevent="save" class="p-6 space-y-4 overflow-y-auto max-h-[75vh]">
        
        <!-- Name -->
        <div>
          <label class="text-xs font-semibold text-slate-300 block mb-1">Item Name *</label>
          <input 
            v-model="form.name" 
            required 
            placeholder="e.g. 2% Milk, Honeycrisp Apples, All-Purpose Flour" 
            class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500" />
        </div>

        <!-- Existing Item Match Alert & Quick Add / Adjust -->
        <div v-if="existingMatch" class="p-3.5 bg-amber-950/40 border border-amber-500/40 rounded-2xl space-y-2.5">
          <div class="flex items-center justify-between gap-2">
            <span class="text-xs text-amber-200 font-semibold truncate">
              "{{ existingMatch.name }}" already exists in your inventory
            </span>
            <span class="text-[11px] font-mono font-bold px-2 py-0.5 rounded-lg bg-amber-500/20 text-amber-300 border border-amber-500/30 whitespace-nowrap">
              In Pantry: {{ existingMatch.totalQuantity }} {{ existingMatch.defaultUnit }}
            </span>
          </div>

          <div class="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2 pt-1 border-t border-amber-500/20">
            <button 
              type="button" 
              @click="switchToEditExisting(existingMatch)" 
              class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs font-semibold transition-colors text-center">
              Adjust Inventory / Edit
            </button>

            <div class="flex items-center gap-2 justify-end">
              <div class="flex items-center bg-slate-900 border border-slate-700 rounded-xl p-0.5">
                <button 
                  type="button" 
                  @click="quickAddQty = Math.max(1, quickAddQty - 1)" 
                  class="w-6 h-6 rounded-lg bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs flex items-center justify-center">
                  -
                </button>
                <input 
                  type="number" 
                  min="1" 
                  v-model.number="quickAddQty" 
                  class="w-9 text-center bg-transparent font-mono font-bold text-xs text-white focus:outline-none" />
                <button 
                  type="button" 
                  @click="quickAddQty++" 
                  class="w-6 h-6 rounded-lg bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs flex items-center justify-center">
                  +
                </button>
              </div>

              <button 
                type="button" 
                @click="addStockToExisting(existingMatch)" 
                class="px-3.5 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs shadow-sm transition-colors whitespace-nowrap">
                Add to Inventory (+{{ quickAddQty }})
              </button>
            </div>
          </div>
        </div>

        <!-- Brand & Package Size -->
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Brand</label>
            <input 
              v-model="form.brand" 
              placeholder="e.g. Kroger, Simple Truth, Kirkland" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500" />
          </div>
          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Package Size</label>
            <input 
              v-model="form.packageSize" 
              placeholder="e.g. 16 oz, 1 lb, 500g, 1 gal" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500" />
          </div>
        </div>

        <!-- Category & Location -->
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Category</label>
            <select 
              v-model="form.categoryId" 
              @change="handleCategoryChange"
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-brand-500">
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>

          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Default Storage Location</label>
            <select 
              v-model="form.defaultLocationId" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-brand-500">
              <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
            </select>
          </div>
        </div>

        <!-- Perishable / Fresh Produce Reminder Toggle -->
        <div class="p-3 bg-slate-950/60 rounded-xl border border-slate-800 flex items-center justify-between">
          <div class="pr-2">
            <span class="text-xs font-semibold text-slate-200 block">Perishable / Fresh Produce</span>
            <span class="text-[11px] text-slate-400 block">Triggers freshness check reminders even without an expiration date.</span>
          </div>
          <input 
            type="checkbox" 
            v-model="form.perishable" 
            class="w-4 h-4 rounded text-brand-600 bg-slate-800 border-slate-700 focus:ring-brand-500 flex-shrink-0" />
        </div>

        <!-- Quantity & Unit (if new item) -->
        <div v-if="!isEdit" class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Initial Quantity</label>
            <input 
              type="number" 
              step="any"
              v-model.number="form.initialQuantity" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-brand-500" />
          </div>

          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Unit</label>
            <input 
              v-model="form.defaultUnit" 
              placeholder="count, lbs, oz, gal, g" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500" />
          </div>
        </div>

        <!-- Date Added & Expiration Date (if new item) -->
        <div v-if="!isEdit" class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Date Added / Purchased</label>
            <input 
              type="date" 
              v-model="form.purchasedDate" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500" />
          </div>
          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Expiration Date (Optional)</label>
            <input 
              type="date" 
              v-model="form.expirationDate" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500" />
          </div>
        </div>

        <!-- Low Stock Threshold & Bring Sync -->
        <div class="p-3 bg-slate-950/60 rounded-xl border border-slate-800 space-y-3">
          <div class="flex items-center justify-between">
            <span class="text-xs font-semibold text-slate-300">Bring! Shopping List Auto-Restock</span>
            <input 
              type="checkbox" 
              v-model="form.autoAddToBring" 
              class="w-4 h-4 rounded text-brand-600 bg-slate-800 border-slate-700 focus:ring-brand-500" />
          </div>
          
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="text-[11px] text-slate-400 block mb-0.5">Min Stock Alert</label>
              <input 
                type="number" 
                step="any"
                v-model.number="form.minThreshold" 
                class="w-full bg-slate-800 border border-slate-700 rounded-lg px-2.5 py-1 text-xs text-white font-mono focus:outline-none focus:border-brand-500" />
            </div>
            <div>
              <label class="text-[11px] text-slate-400 block mb-0.5">Restock Amount</label>
              <input 
                type="number" 
                step="any"
                v-model.number="form.restockQuantity" 
                class="w-full bg-slate-800 border border-slate-700 rounded-lg px-2.5 py-1 text-xs text-white font-mono focus:outline-none focus:border-brand-500" />
            </div>
          </div>

          <div>
            <label class="text-[11px] text-slate-400 block mb-0.5">Bring! Purchase Spec (e.g. 1 dozen, 6 eggs, 2 bags)</label>
            <input 
              v-model="form.defaultPurchaseAmount" 
              placeholder="e.g. 1 dozen, 6 eggs, 2 bags, 1 gallon" 
              class="w-full bg-slate-800 border border-slate-700 rounded-lg px-2.5 py-1 text-xs text-white focus:outline-none focus:border-brand-500" />
            <p class="text-[10px] text-slate-400 mt-1">Leave blank to use numeric restock amount & unit.</p>
          </div>
        </div>

        <!-- Barcode & Image URL -->
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Barcode (UPC)</label>
            <input 
              v-model="form.barcode" 
              placeholder="011110416002" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-brand-500" />
          </div>
          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Image URL</label>
            <input 
              v-model="form.imageUrl" 
              placeholder="https://..." 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
          </div>
        </div>

        <!-- Notes & Storage Tips -->
        <div>
          <label class="text-xs font-semibold text-slate-300 block mb-1">Notes &amp; Storage Tips</label>
          <textarea 
            v-model="form.notes" 
            rows="2" 
            placeholder="e.g. Keep in crisper drawer, use within 5 days of opening..." 
            class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500 resize-none"></textarea>
        </div>

        <!-- Buttons -->
        <div class="flex justify-between items-center pt-2">
          <button 
            v-if="isEditingExisting" 
            type="button" 
            @click="deleteItem" 
            class="px-3 py-2 text-xs font-bold text-rose-400 hover:text-rose-300 hover:bg-rose-500/10 rounded-xl">
            Delete Item
          </button>
          <div v-else></div>

          <div class="flex gap-2">
            <button type="button" @click="$emit('close')" class="px-4 py-2 text-xs text-slate-400 hover:text-white">Cancel</button>
            <button 
              type="submit" 
              class="px-5 py-2 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs shadow-sm">
              {{ isEditingExisting ? 'Save Changes' : 'Add to Inventory' }}
            </button>
          </div>
        </div>

      </form>

    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { X } from 'lucide-vue-next';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const { showToast } = useToast();

const props = defineProps({
  initialData: Object,
  isEdit: Boolean,
});

const emit = defineEmits(['close', 'saved']);

const categories = ref([]);
const locations = ref([]);
const existingItems = ref([]);
const quickAddQty = ref(1);
const isEditingExisting = ref(props.isEdit);

const form = reactive({
  id: props.initialData?.id || null,
  name: props.initialData?.name || '',
  brand: props.initialData?.brand || '',
  packageSize: props.initialData?.packageSize || '',
  categoryId: props.initialData?.category?.id || props.initialData?.categoryId || 'cat-canned',
  defaultLocationId: props.initialData?.defaultLocation?.id || props.initialData?.defaultLocationId || 'loc-pantry',
  defaultUnit: props.initialData?.defaultUnit || 'count',
  initialQuantity: props.initialData?.initialQuantity ?? 1,
  minThreshold: props.initialData?.minThreshold ?? 0,
  restockQuantity: props.initialData?.restockQuantity ?? 1,
  expirationDate: props.initialData?.expirationDate || '',
  purchasedDate: props.initialData?.purchasedDate || new Date().toISOString().substring(0, 10),
  barcode: props.initialData?.barcode || '',
  imageUrl: props.initialData?.imageUrl || '',
  autoAddToBring: props.initialData?.autoAddToBring ?? true,
  defaultPurchaseAmount: props.initialData?.defaultPurchaseAmount || '',
  perishable: props.initialData?.perishable ?? (props.initialData?.category?.id === 'cat-produce' || props.initialData?.categoryId === 'cat-produce'),
  notes: props.initialData?.notes || '',
});

const existingMatch = computed(() => {
  if (isEditingExisting.value) return null;
  const n = form.name?.trim().toLowerCase();
  const b = form.barcode?.trim();
  if (!n && !b) return null;
  return existingItems.value.find(item => {
    const matchName = n && item.name?.trim().toLowerCase() === n;
    const matchBarcode = b && item.barcode?.trim() === b;
    return matchName || matchBarcode;
  });
});

function switchToEditExisting(item) {
  isEditingExisting.value = true;
  form.id = item.id;
  form.name = item.name || '';
  form.brand = item.brand || '';
  form.packageSize = item.packageSize || '';
  form.categoryId = item.category?.id || form.categoryId;
  form.defaultLocationId = item.defaultLocation?.id || form.defaultLocationId;
  form.defaultUnit = item.defaultUnit || form.defaultUnit;
  form.minThreshold = item.minThreshold ?? form.minThreshold;
  form.restockQuantity = item.restockQuantity ?? form.restockQuantity;
  form.defaultPurchaseAmount = item.defaultPurchaseAmount || '';
  form.autoAddToBring = item.autoAddToBring ?? true;
  form.barcode = item.barcode || form.barcode;
  form.imageUrl = item.imageUrl || form.imageUrl;
  form.perishable = item.perishable ?? false;
  form.notes = item.notes || '';
}

async function addStockToExisting(item) {
  try {
    await api.adjustQuantity(item.id, quickAddQty.value);
    showToast(`Added ${quickAddQty.value} ${item.defaultUnit || 'count'} of "${item.name}" to inventory!`);
    emit('saved', item);
    emit('close');
  } catch (err) {
    showToast('Failed to adjust inventory: ' + err.message, 'error');
  }
}

function handleCategoryChange() {
  if (form.categoryId === 'cat-produce') {
    form.perishable = true;
    if (form.defaultLocationId === 'loc-pantry') {
      form.defaultLocationId = 'loc-fridge';
    }
  }
}

onMounted(async () => {
  try {
    const [cRes, lRes, itemsRes] = await Promise.all([
      api.getCategories(),
      api.getLocations(),
      api.getItems(),
    ]);
    categories.value = cRes.data;
    locations.value = lRes.data;
    existingItems.value = itemsRes.data || [];
  } catch (err) {
    console.error('Failed to load categories/locations/items:', err);
  }
});

async function save() {
  try {
    const savedItemRes = await api.saveItem(form);
    const savedItem = savedItemRes.data;

    // If new item and has initial quantity, create batch
    if (!isEditingExisting.value && form.initialQuantity > 0) {
      await api.addBatch({
        itemId: savedItem.id,
        locationId: form.defaultLocationId,
        quantity: form.initialQuantity,
        unit: form.defaultUnit,
        expirationDate: form.expirationDate ? form.expirationDate : null,
        purchasedDate: form.purchasedDate ? form.purchasedDate : null,
        note: form.notes,
        barcode: form.barcode,
      });
    }

    emit('saved', savedItem);
    emit('close');
  } catch (err) {
    showToast('Failed to save item: ' + err.message, 'error');
  }
}

async function deleteItem() {
  if (confirm(`Are you sure you want to delete "${form.name}"?`)) {
    try {
      await api.deleteItem(form.id);
      showToast(`"${form.name}" deleted`, 'info');
      emit('saved');
      emit('close');
    } catch (err) {
      showToast('Failed to delete item: ' + err.message, 'error');
    }
  }
}
</script>
