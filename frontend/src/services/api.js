import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

export default {
  // Items & Inventory
  getItems(params = {}) {
    const serializedParams = { ...params };
    ['locationId', 'categoryId', 'stockFilter', 'freshnessFilter'].forEach((key) => {
      if (Array.isArray(serializedParams[key])) {
        serializedParams[key] = serializedParams[key].filter(Boolean).join(',');
      }
    });
    return api.get('/items', { params: serializedParams });
  },
  getItem(id) {
    return api.get(`/items/${id}`);
  },
  getExpiringSoon(days = 7) {
    return api.get('/items/expiring', { params: { days } });
  },
  getNotifications() {
    return api.get('/items/notifications');
  },
  saveItem(item) {
    return api.post('/items', item);
  },
  deleteItem(id) {
    return api.delete(`/items/${id}`);
  },
  resetFreshness(id) {
    return api.post(`/items/${id}/reset-freshness`);
  },
  adjustQuantity(itemId, delta) {
    return api.post('/inventory/adjust', { itemId, delta });
  },
  addBatch(batch) {
    return api.post('/inventory/batch', batch);
  },
  consumeItem(itemId) {
    return api.post('/inventory/consume', { itemId });
  },
  moveLocation({ itemId, batchId, targetLocationId, quantity }) {
    return api.post('/inventory/move-location', { itemId, batchId, targetLocationId, quantity });
  },
  deleteBatch(batchId) {
    return api.delete(`/inventory/batch/${batchId}`);
  },

  // Locations
  getLocations() {
    return api.get('/locations');
  },
  saveLocation(location) {
    return api.post('/locations', location);
  },
  deleteLocation(id) {
    return api.delete(`/locations/${id}`);
  },
  reorderLocations(orderedIds) {
    return api.post('/locations/reorder', orderedIds);
  },

  // Categories
  getCategories() {
    return api.get('/categories');
  },
  saveCategory(category) {
    return api.post('/categories', category);
  },
  updateCategory(id, category) {
    return api.put(`/categories/${id}`, category);
  },

  // Users & Auth
  getUsers() {
    return api.get('/users');
  },
  saveUser(user) {
    return api.post('/users', user);
  },
  deleteUser(id) {
    return api.delete(`/users/${id}`);
  },
  getAuthProviders() {
    return api.get('/auth/providers');
  },

  // Barcode
  lookupBarcode(code) {
    return api.get(`/barcode/${code}`);
  },

  // Purchases
  getItemPurchases(itemId) {
    return api.get(`/items/${itemId}/purchases`);
  },
  addPurchaseRecord(itemId, data) {
    return api.post(`/items/${itemId}/purchases`, data);
  },
  deletePurchaseRecord(itemId, purchaseId) {
    return api.delete(`/items/${itemId}/purchases/${purchaseId}`);
  },

  // Receipts
  getReceiptProviders() {
    return api.get('/receipts/providers');
  },
  parseReceipt(rawText, providerId = 'auto') {
    return api.post('/receipts/parse', { rawText, providerId });
  },
  commitReceipt(storeName, items, transactionDate = null) {
    return api.post('/receipts/commit', { storeName, items, transactionDate });
  },

  // Bring!
  getBringShoppingList() {
    return api.get('/bring/list');
  },
  getBringLists() {
    return api.get('/bring/lists');
  },
  addToBring(name, specification) {
    return api.post('/bring/add', { name, specification });
  },
  testBringAuth(email, password) {
    return api.post('/bring/test', { email, password });
  },

  // Settings
  getSettings() {
    return api.get('/settings');
  },
  saveSettings(settings) {
    return api.post('/settings', settings);
  },
  getVersion() {
    return api.get('/settings/version');
  },
  testEmail() {
    return api.post('/settings/test-email');
  },
  sendDigest(type = 'daily') {
    return api.post('/settings/send-digest', null, { params: { type } });
  },
  testLlm(data) {
    return api.post('/settings/test-llm', data);
  },

  // Analytics
  getSpendSummary() {
    return api.get('/analytics/summary');
  },
  getMonthlySpend() {
    return api.get('/analytics/spend');
  },
  getStoreComparison() {
    return api.get('/analytics/stores');
  },
  getPriceHistory(params = {}) {
    return api.get('/analytics/prices', { params });
  },

  // Predictions & Replenishment
  getDepletionPredictions() {
    return api.get('/inventory/predictions');
  },
  getSeasonalSuggestions() {
    return api.get('/inventory/seasonal-suggestions');
  },

  // Recipes & Meal Planning
  getRecipeMatches() {
    return api.get('/recipes/matches');
  },
  generateZeroWasteMeal(itemIds = []) {
    return api.post('/recipes/ai-zero-waste', { itemIds });
  },
  addMissingToShoppingList(items = []) {
    return api.post('/recipes/missing-to-shopping', { items });
  },
  triggerRecipeAction(recipeJson) {
    return api.post('/recipes/action', recipeJson);
  },

  // Pending Cooked Meals & Reconciliation
  getPendingMeals() {
    return api.get('/meals/pending');
  },
  getPendingMealsCount() {
    return api.get('/meals/pending/count');
  },
  reconcileMeal(id, payload) {
    return api.post(`/meals/pending/${id}/reconcile`, payload);
  },
  reconcileAllMeals() {
    return api.post('/meals/pending/reconcile-all');
  },
  dismissPendingMeal(id) {
    return api.delete(`/meals/pending/${id}`);
  },
};
