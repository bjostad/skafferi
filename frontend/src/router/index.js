import { createRouter, createWebHistory } from 'vue-router';
import InventoryView from '../views/InventoryView.vue';
import ExpiringView from '../views/ExpiringView.vue';
import ShoppingView from '../views/ShoppingView.vue';
import SettingsView from '../views/SettingsView.vue';

const routes = [
  { path: '/', name: 'Inventory', component: InventoryView },
  { path: '/expiring', name: 'Expiring', component: ExpiringView },
  { path: '/shopping', name: 'Shopping', component: ShoppingView },
  { path: '/settings', name: 'Settings', component: SettingsView },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
