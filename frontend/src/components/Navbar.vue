<template>
  <nav class="bg-slate-900/95 backdrop-blur-md border-b border-slate-800 sticky top-0 z-40 px-4 lg:px-8 py-3">
    <div class="max-w-7xl mx-auto flex items-center justify-between gap-4">
      
      <!-- Brand Logo -->
      <router-link to="/" class="flex items-center gap-2.5 group">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-600 to-brand-400 flex items-center justify-center shadow-lg shadow-brand-500/25 group-hover:scale-105 transition-transform">
          <Package class="w-5 h-5 text-white" />
        </div>
        <div>
          <span class="font-extrabold text-xl tracking-tight text-white">
            Skafferi
          </span>
        </div>
      </router-link>

      <!-- Main Navigation Links -->
      <div class="hidden md:flex items-center gap-1 bg-slate-950/80 p-1 rounded-xl border border-slate-800">
        <router-link 
          to="/" 
          class="px-3.5 py-1.5 rounded-lg text-sm transition-colors flex items-center gap-2"
          :class="$route.path === '/' ? 'bg-brand-500/25 text-brand-200 border border-brand-400/50 font-bold shadow-sm' : 'text-slate-300 hover:text-white hover:bg-slate-800/80'">
          <Boxes class="w-4 h-4" />
          Inventory
        </router-link>
        
        <router-link 
          to="/expiring" 
          class="px-3.5 py-1.5 rounded-lg text-sm transition-colors flex items-center gap-2"
          :class="$route.path === '/expiring' ? 'bg-brand-500/25 text-brand-200 border border-brand-400/50 font-bold shadow-sm' : 'text-slate-300 hover:text-white hover:bg-slate-800/80'">
          <Clock class="w-4 h-4" />
          Expiring Soon
        </router-link>

        <router-link 
          to="/shopping" 
          class="px-3.5 py-1.5 rounded-lg text-sm transition-colors flex items-center gap-2"
          :class="$route.path === '/shopping' ? 'bg-brand-500/25 text-brand-200 border border-brand-400/50 font-bold shadow-sm' : 'text-slate-300 hover:text-white hover:bg-slate-800/80'">
          <ShoppingCart class="w-4 h-4" />
          Shopping List
        </router-link>
      </div>

      <!-- Quick Action Buttons & User Menu -->
      <div class="flex items-center gap-2">
        <button 
          @click="$emit('open-scan')"
          title="Scan Barcode"
          class="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-750 text-white border border-slate-700 transition-all hover:border-brand-400/50 flex items-center gap-2 shadow-sm">
          <ScanBarcode class="w-4 h-4 text-brand-300" />
          <span class="hidden sm:inline text-xs font-bold">Scan Barcode</span>
        </button>

        <button 
          @click="$emit('open-receipt')"
          title="Import Receipt"
          class="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-750 text-white border border-slate-700 transition-all hover:border-brand-400/50 flex items-center gap-2 shadow-sm">
          <Receipt class="w-4 h-4 text-brand-300" />
          <span class="hidden sm:inline text-xs font-bold">Import Receipt</span>
        </button>

        <button 
          @click="$emit('open-add')"
          class="px-3.5 py-2 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 font-extrabold text-xs transition-all shadow-md flex items-center gap-1.5">
          <Plus class="w-4 h-4" />
          <span>Add Item</span>
        </button>

        <!-- Notification Bell & Popover Trigger -->
        <div class="relative ml-1" ref="notifMenuRef">
          <button 
            @click="toggleNotifMenu"
            title="Notifications & Alerts"
            class="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-750 text-slate-200 border border-slate-700/80 transition-all hover:border-brand-500/40 relative flex items-center justify-center">
            <Bell class="w-4 h-4 text-slate-300" />
            <span 
              v-if="notifSummary && notifSummary.totalAlertCount > 0" 
              class="absolute -top-1 -right-1 px-1.5 py-0.2 min-w-[1.1rem] h-[1.1rem] text-[10px] font-extrabold rounded-full bg-rose-500 text-white flex items-center justify-center shadow-lg border-2 border-slate-900 animate-pulse">
              {{ notifSummary.totalAlertCount }}
            </span>
          </button>

          <!-- Notification Dropdown Popover -->
          <div 
            v-if="notifMenuOpen" 
            class="absolute right-0 mt-2 w-80 sm:w-96 bg-slate-900 border border-slate-700/80 rounded-2xl shadow-2xl overflow-hidden z-50 animate-in fade-in slide-in-from-top-2 duration-150">
            
            <div class="p-3.5 bg-slate-950/70 border-b border-slate-800 flex items-center justify-between">
              <div class="flex items-center gap-2">
                <Bell class="w-4 h-4 text-brand-400" />
                <h4 class="font-bold text-white text-xs">Freshness &amp; Inventory Alerts</h4>
              </div>
              <span 
                v-if="notifSummary && notifSummary.totalAlertCount > 0"
                class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30">
                {{ notifSummary.totalAlertCount }} active
              </span>
              <span v-else class="text-[10px] font-medium text-emerald-400">All clear</span>
            </div>

            <div class="max-h-80 overflow-y-auto divide-y divide-slate-800/60 p-2 space-y-2">
              
              <!-- Expired Items -->
              <div v-if="notifSummary?.expiredItems?.length > 0" class="space-y-1">
                <span class="text-[10px] font-bold uppercase tracking-wider text-rose-400 px-2 flex items-center gap-1">
                  <AlertTriangle class="w-3 h-3" />
                  Expired ({{ notifSummary.expiredItems.length }})
                </span>
                <div 
                  v-for="item in notifSummary.expiredItems.slice(0, 3)" 
                  :key="'exp-' + item.id"
                  @click="goToItem(item)"
                  class="p-2 rounded-xl bg-rose-950/20 hover:bg-rose-950/40 border border-rose-500/20 flex items-center justify-between cursor-pointer transition-colors">
                  <div class="min-w-0 pr-2">
                    <p class="text-xs font-semibold text-white truncate">{{ item.name }}</p>
                    <p class="text-[11px] text-rose-300/80">{{ item.totalQuantity }} {{ item.defaultUnit }} &bull; {{ item.defaultLocation?.name || 'Pantry' }}</p>
                  </div>
                  <span class="text-[10px] font-bold px-2 py-0.5 rounded-lg bg-rose-500/20 text-rose-300 border border-rose-500/30 whitespace-nowrap">
                    Expired
                  </span>
                </div>
              </div>

              <!-- Expiring Soon Items -->
              <div v-if="notifSummary?.expiringSoonItems?.length > 0" class="space-y-1 pt-2">
                <span class="text-[10px] font-bold uppercase tracking-wider text-amber-400 px-2 flex items-center gap-1">
                  <Clock class="w-3 h-3" />
                  Expiring Soon ({{ notifSummary.expiringSoonItems.length }})
                </span>
                <div 
                  v-for="item in notifSummary.expiringSoonItems.slice(0, 4)" 
                  :key="'soon-' + item.id"
                  @click="goToItem(item)"
                  class="p-2 rounded-xl bg-amber-950/20 hover:bg-amber-950/40 border border-amber-500/20 flex items-center justify-between cursor-pointer transition-colors">
                  <div class="min-w-0 pr-2">
                    <p class="text-xs font-semibold text-white truncate">{{ item.name }}</p>
                    <p class="text-[11px] text-amber-300/80">{{ item.totalQuantity }} {{ item.defaultUnit }} &bull; {{ item.defaultLocation?.name || 'Pantry' }}</p>
                  </div>
                  <span class="text-[10px] font-bold px-2 py-0.5 rounded-lg bg-amber-500/20 text-amber-300 border border-amber-500/30 whitespace-nowrap">
                    {{ item.daysUntilEarliestExpiry }}d left
                  </span>
                </div>
              </div>

              <!-- Fresh Produce Checks -->
              <div v-if="notifSummary?.freshCheckItems?.length > 0" class="space-y-1 pt-2">
                <span class="text-[10px] font-bold uppercase tracking-wider text-emerald-400 px-2 flex items-center gap-1">
                  <Sparkles class="w-3 h-3" />
                  Check Freshness ({{ notifSummary.freshCheckItems.length }})
                </span>
                <div 
                  v-for="item in notifSummary.freshCheckItems.slice(0, 3)" 
                  :key="'fresh-' + item.id"
                  @click="goToItem(item)"
                  class="p-2 rounded-xl bg-emerald-950/20 hover:bg-emerald-950/40 border border-emerald-500/20 flex items-center justify-between cursor-pointer transition-colors">
                  <div class="min-w-0 pr-2">
                    <p class="text-xs font-semibold text-white truncate">{{ item.name }}</p>
                    <p class="text-[11px] text-emerald-300/80">{{ item.totalQuantity }} {{ item.defaultUnit }} &bull; {{ item.daysInStorage }}d in storage</p>
                  </div>
                  <span class="text-[10px] font-bold px-2 py-0.5 rounded-lg bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 whitespace-nowrap">
                    Check
                  </span>
                </div>
              </div>

              <!-- No alerts empty state -->
              <div v-if="!notifSummary || notifSummary.totalAlertCount === 0" class="p-6 text-center">
                <CheckCircle2 class="w-8 h-8 text-emerald-400 mx-auto mb-2 opacity-80" />
                <p class="text-xs font-semibold text-white">No expiring items or alerts!</p>
                <p class="text-[11px] text-slate-400 mt-0.5">All tracked food is fresh and in stock.</p>
              </div>

            </div>

            <!-- Footer link to Expiring & Freshness tracker -->
            <div class="p-2.5 bg-slate-950/60 border-t border-slate-800 text-center">
              <router-link 
                to="/expiring" 
                @click="notifMenuOpen = false"
                class="text-xs font-bold text-brand-300 hover:text-brand-200 transition-colors inline-flex items-center gap-1">
                <span>View Full Freshness Tracker &rarr;</span>
              </router-link>
            </div>

          </div>
        </div>

        <!-- User Menu Dropdown Trigger -->
        <div class="relative ml-1 pl-2 border-l border-slate-800" ref="userMenuRef">
          <button 
            @click="userMenuOpen = !userMenuOpen"
            class="flex items-center gap-2 p-1 rounded-xl hover:bg-slate-800 transition-colors group">
            <div 
              class="w-8 h-8 rounded-xl flex items-center justify-center font-bold text-xs text-white shadow-md transition-transform group-hover:scale-105"
              :style="{ backgroundColor: currentUser.avatarColor || '#9685ab' }">
              {{ (currentUser.displayName || currentUser.username || 'A').charAt(0).toUpperCase() }}
            </div>
            <ChevronDown class="w-3.5 h-3.5 text-slate-400 group-hover:text-white transition-transform" :class="{ 'rotate-180': userMenuOpen }" />
          </button>

          <!-- User Menu Dropdown Card -->
          <div 
            v-if="userMenuOpen" 
            class="absolute right-0 mt-2 w-64 bg-slate-900 border border-slate-700/80 rounded-2xl shadow-2xl overflow-hidden z-50 animate-in fade-in slide-in-from-top-2 duration-150">
            
            <!-- User Profile Header -->
            <div class="p-3.5 bg-slate-850 bg-slate-950/60 border-b border-slate-800">
              <div class="flex items-center gap-2.5">
                <div 
                  class="w-9 h-9 rounded-xl flex items-center justify-center font-bold text-xs text-white shadow-md flex-shrink-0"
                  :style="{ backgroundColor: currentUser.avatarColor || '#9685ab' }">
                  {{ (currentUser.displayName || currentUser.username || 'A').charAt(0).toUpperCase() }}
                </div>
                <div class="min-w-0 flex-1">
                  <h4 class="font-bold text-white text-xs truncate">{{ currentUser.displayName || currentUser.username }}</h4>
                  <p class="text-[11px] text-slate-400 truncate">@{{ currentUser.username }}</p>
                </div>
                <span class="text-[10px] font-bold px-1.5 py-0.5 rounded bg-brand-500/20 text-brand-300 border border-brand-500/30">
                  {{ currentUser.role || 'ADMIN' }}
                </span>
              </div>
            </div>

            <!-- Household User Switcher (if multiple users) -->
            <div v-if="allUsers.length > 1" class="p-2 border-b border-slate-800 bg-slate-900">
              <span class="text-[10px] font-bold text-slate-400 uppercase tracking-wider px-2 py-1 block">Switch Member</span>
              <div class="space-y-0.5 mt-0.5">
                <button 
                  v-for="u in allUsers" 
                  :key="u.id"
                  @click="switchUser(u)"
                  class="w-full flex items-center justify-between px-2 py-1.5 rounded-lg text-xs hover:bg-slate-800 transition-colors"
                  :class="u.id === currentUser.id ? 'text-brand-300 font-bold bg-brand-950/50' : 'text-slate-300'">
                  <div class="flex items-center gap-2 min-w-0">
                    <div class="w-4 h-4 rounded-full flex-shrink-0" :style="{ backgroundColor: u.avatarColor || '#9685ab' }"></div>
                    <span class="truncate">{{ u.displayName || u.username }}</span>
                  </div>
                  <Check v-if="u.id === currentUser.id" class="w-3.5 h-3.5 text-brand-400" />
                </button>
              </div>
            </div>

            <!-- SSO Providers (if Authentik or Google enabled) -->
            <div v-if="providers.authentik || providers.google" class="p-2 border-b border-slate-800 bg-slate-950/40">
              <span class="text-[10px] font-bold text-slate-400 uppercase tracking-wider px-2 py-0.5 block">Single Sign-On</span>
              <div class="space-y-1 mt-1">
                <a 
                  v-if="providers.authentik"
                  :href="providers.authentik.loginUrl"
                  class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-orange-500/10 hover:bg-orange-500/20 text-orange-300 border border-orange-500/30 text-xs font-semibold transition-colors">
                  <ShieldCheck class="w-3.5 h-3.5 text-orange-400" />
                  Sign in with Authentik
                </a>
                <a 
                  v-if="providers.google"
                  :href="providers.google.loginUrl"
                  class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-blue-500/10 hover:bg-blue-500/20 text-blue-300 border border-blue-500/30 text-xs font-semibold transition-colors">
                  <ShieldCheck class="w-3.5 h-3.5 text-blue-400" />
                  Sign in with Google
                </a>
              </div>
            </div>

            <!-- Settings Links -->
            <div class="p-1.5 space-y-0.5">
              <router-link 
                to="/settings?section=locations" 
                @click="userMenuOpen = false"
                class="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-slate-300 hover:text-white hover:bg-slate-800 transition-colors">
                <MapPin class="w-4 h-4 text-brand-400" />
                Storage Locations
              </router-link>

              <router-link 
                to="/settings?section=auth" 
                @click="userMenuOpen = false"
                class="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-slate-300 hover:text-white hover:bg-slate-800 transition-colors">
                <ShieldCheck class="w-4 h-4 text-brand-400" />
                SSO &amp; Authentication
              </router-link>

              <router-link 
                to="/settings?section=integrations" 
                @click="userMenuOpen = false"
                class="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-slate-300 hover:text-white hover:bg-slate-800 transition-colors">
                <Link2 class="w-4 h-4 text-brand-400" />
                Integrations &amp; APIs
              </router-link>

              <router-link 
                to="/settings?section=users" 
                @click="userMenuOpen = false"
                class="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-slate-300 hover:text-white hover:bg-slate-800 transition-colors">
                <Users class="w-4 h-4 text-brand-400" />
                Household Users
              </router-link>

              <div class="border-t border-slate-800 my-1"></div>

              <router-link 
                to="/settings" 
                @click="userMenuOpen = false"
                class="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-semibold text-slate-200 hover:text-brand-200 hover:bg-slate-800 transition-colors">
                <Settings class="w-4 h-4 text-brand-400" />
                All Settings
              </router-link>
            </div>

            <!-- Settings Menu Footer -->
            <div class="px-3.5 py-2.5 bg-slate-950/80 border-t border-slate-800 flex items-center justify-between text-[11px] text-slate-400">
              <span class="font-medium text-slate-400">Skafferi</span>
              <span class="font-mono text-slate-400 bg-slate-800/80 px-2 py-0.5 rounded-md border border-slate-700/60 text-[10px]">v1.0</span>
            </div>

          </div>
        </div>

      </div>

    </div>

    <!-- Mobile Navigation Sub-bar -->
    <div class="flex md:hidden items-center justify-around gap-1 mt-2.5 pt-2 border-t border-slate-800 text-xs">
      <router-link to="/" class="flex flex-col items-center gap-1 py-1 px-3 rounded-lg" :class="$route.path === '/' ? 'text-brand-400 font-bold' : 'text-slate-400'">
        <Boxes class="w-4 h-4" />
        Inventory
      </router-link>
      <router-link to="/expiring" class="flex flex-col items-center gap-1 py-1 px-3 rounded-lg" :class="$route.path === '/expiring' ? 'text-brand-400 font-bold' : 'text-slate-400'">
        <Clock class="w-4 h-4" />
        Expiring
      </router-link>
      <router-link to="/shopping" class="flex flex-col items-center gap-1 py-1 px-3 rounded-lg" :class="$route.path === '/shopping' ? 'text-brand-400 font-bold' : 'text-slate-400'">
        <ShoppingCart class="w-4 h-4" />
        Shopping
      </router-link>
      <router-link to="/settings" class="flex flex-col items-center gap-1 py-1 px-3 rounded-lg" :class="$route.path === '/settings' ? 'text-brand-400 font-bold' : 'text-slate-400'">
        <Settings class="w-4 h-4" />
        Settings
      </router-link>
    </div>
  </nav>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { 
  Package, Boxes, Clock, ShoppingCart, Settings, ScanBarcode, Receipt, Plus, 
  ChevronDown, MapPin, Link2, Users, Check, ShieldCheck,
  Bell, AlertTriangle, Sparkles, CheckCircle2 
} from 'lucide-vue-next';
import { auth } from '../services/auth';
import api from '../services/api';

defineEmits(['open-scan', 'open-receipt', 'open-add']);

const router = useRouter();

const userMenuOpen = ref(false);
const userMenuRef = ref(null);

const notifMenuOpen = ref(false);
const notifMenuRef = ref(null);
const notifSummary = ref(null);

const currentUser = computed(() => auth.getCurrentUser());
const allUsers = computed(() => auth.state.allUsers);
const providers = computed(() => auth.state.providers || {});

async function loadNotifications() {
  try {
    const res = await api.getNotifications();
    notifSummary.value = res.data;
  } catch (err) {
    console.error('Failed to load notifications:', err);
  }
}

function toggleNotifMenu() {
  notifMenuOpen.value = !notifMenuOpen.value;
  if (notifMenuOpen.value) {
    userMenuOpen.value = false;
    loadNotifications();
  }
}

function goToItem(item) {
  notifMenuOpen.value = false;
  // If item is expiring or fresh check, route to expiring tracker
  router.push('/expiring');
}

onMounted(async () => {
  await Promise.all([auth.loadUsers(), loadNotifications()]);
  document.addEventListener('click', handleClickOutside);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside);
});

function switchUser(user) {
  auth.switchUser(user);
  userMenuOpen.value = false;
}

function handleClickOutside(event) {
  if (userMenuRef.value && !userMenuRef.value.contains(event.target)) {
    userMenuOpen.value = false;
  }
  if (notifMenuRef.value && !notifMenuRef.value.contains(event.target)) {
    notifMenuOpen.value = false;
  }
}
</script>
