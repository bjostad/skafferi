import { createRouter, createWebHistory } from 'vue-router';
import InventoryView from '../views/InventoryView.vue';
import ExpiringView from '../views/ExpiringView.vue';
import ShoppingView from '../views/ShoppingView.vue';
import RecipesView from '../views/RecipesView.vue';
import AnalyticsView from '../views/AnalyticsView.vue';
import SettingsView from '../views/SettingsView.vue';

const routes = [
  { path: '/', name: 'Inventory', component: InventoryView },
  { path: '/expiring', name: 'Expiring', component: ExpiringView },
  { path: '/shopping', name: 'Shopping', component: ShoppingView },
  { path: '/recipes', name: 'Recipes', component: RecipesView },
  { path: '/analytics', name: 'Analytics', component: AnalyticsView },
  { path: '/settings', name: 'Settings', component: SettingsView },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
