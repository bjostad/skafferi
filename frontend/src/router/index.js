import { createRouter, createWebHistory } from 'vue-router';
import InventoryView from '../views/InventoryView.vue';
import ExpiringView from '../views/ExpiringView.vue';
import ShoppingView from '../views/ShoppingView.vue';
import RecipesView from '../views/RecipesView.vue';
import AnalyticsView from '../views/AnalyticsView.vue';
import SettingsView from '../views/SettingsView.vue';
import { appInfo } from '../services/appInfo';

const routes = [
  { path: '/', name: 'Inventory', component: InventoryView, meta: { title: 'Smart Pantry & Inventory' } },
  { path: '/expiring', name: 'Expiring', component: ExpiringView, meta: { title: 'Expiring Soon' } },
  { path: '/shopping', name: 'Shopping', component: ShoppingView, meta: { title: 'Shopping List' } },
  { path: '/recipes', name: 'Recipes', component: RecipesView, meta: { title: 'Recipes' } },
  { path: '/analytics', name: 'Analytics', component: AnalyticsView, meta: { title: 'Analytics' } },
  { path: '/settings', name: 'Settings', component: SettingsView, meta: { title: 'Settings' } },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export function updatePageTitle(to) {
  const currentRoute = to || router.currentRoute.value;
  const app = appInfo.getAppName();
  if (currentRoute?.path === '/') {
    document.title = `${app} | Smart Pantry & Inventory`;
  } else if (currentRoute?.meta?.title) {
    document.title = `${currentRoute.meta.title} | ${app}`;
  } else {
    document.title = `${app} | Smart Pantry & Inventory`;
  }
}

router.afterEach((to) => {
  updatePageTitle(to);
});

if (typeof window !== 'undefined') {
  window.addEventListener('app-name-changed', () => {
    updatePageTitle();
  });
}

export default router;
