import { ref, watch } from 'vue';

const VIEW_MODE_KEY = 'skafferi_inventory_view_mode';
const SORT_BY_KEY = 'skafferi_inventory_sort_by';

export function useInventoryPreferences() {
  const savedViewMode = localStorage.getItem(VIEW_MODE_KEY);
  const viewMode = ref(savedViewMode === 'grid' ? 'grid' : 'list');

  const savedSortBy = localStorage.getItem(SORT_BY_KEY);
  const selectedSortBy = ref(savedSortBy || 'NAME');

  watch(viewMode, (newVal) => {
    localStorage.setItem(VIEW_MODE_KEY, newVal);
  });

  watch(selectedSortBy, (newVal) => {
    localStorage.setItem(SORT_BY_KEY, newVal);
  });

  return {
    viewMode,
    selectedSortBy,
  };
}
