<template>
  <div class="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-['Plus_Jakarta_Sans',sans-serif]">
    
    <!-- Top Nav -->
    <Navbar 
      @open-scan="scanModalOpen = true"
      @open-receipt="receiptModalOpen = true"
      @open-add="openAddModal" />

    <!-- Main Content Area -->
    <main class="flex-1 max-w-7xl w-full mx-auto px-4 lg:px-8 pt-6">
      <router-view 
        @open-scan="scanModalOpen = true"
        @open-receipt="receiptModalOpen = true"
        @open-add="openAddModal" />
    </main>

    <!-- Global Modals -->
    <BarcodeScannerModal 
      v-if="scanModalOpen" 
      @close="scanModalOpen = false" 
      @found-item="handleScannedItem"
      @item-updated="handleItemSaved" />

    <ReceiptImportModal 
      v-if="receiptModalOpen" 
      @close="receiptModalOpen = false" 
      @committed="handleReceiptCommitted" />

    <AddItemModal 
      v-if="addModalOpen" 
      :initial-data="scannedItemData"
      :is-edit="false"
      @close="addModalOpen = false" 
      @saved="handleItemSaved" />

    <!-- Global Toast Notification Banner -->
    <ToastBanner />

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import Navbar from './components/Navbar.vue';
import BarcodeScannerModal from './components/BarcodeScannerModal.vue';
import ReceiptImportModal from './components/ReceiptImportModal.vue';
import AddItemModal from './components/AddItemModal.vue';
import ToastBanner from './components/ToastBanner.vue';
import { useToast } from './composables/useToast';
import { appInfo } from './services/appInfo';

const { showToast } = useToast();
const router = useRouter();

onMounted(() => {
  appInfo.loadVersion();
});

const scanModalOpen = ref(false);
const receiptModalOpen = ref(false);
const addModalOpen = ref(false);
const scannedItemData = ref(null);

function openAddModal() {
  scannedItemData.value = null;
  addModalOpen.value = true;
}

function handleScannedItem(lookupResult) {
  scannedItemData.value = {
    name: lookupResult.name || '',
    brand: lookupResult.brand || '',
    packageSize: lookupResult.packageSize || '',
    barcode: lookupResult.barcode || '',
    imageUrl: lookupResult.imageUrl || '',
    categoryId: lookupResult.categorySuggestion || 'cat-canned',
    initialQuantity: 1,
    defaultUnit: lookupResult.defaultUnit || 'count',
  };
  addModalOpen.value = true;
}

function handleReceiptCommitted() {
  showToast('Receipt items added to inventory!');
  router.push('/');
}

function handleItemSaved(savedItem) {
  const name = savedItem?.name || 'Item';
  showToast(`${name} added to inventory!`);
  router.push('/');
}
</script>
