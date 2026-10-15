<template>
  <div class="space-y-6 pb-24 max-w-5xl mx-auto">
    
    <!-- Settings Header -->
    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-slate-800 pb-5">
      <div>
        <h2 class="text-2xl font-bold text-white tracking-tight">Settings & Management</h2>
        <p class="text-xs text-slate-400 mt-1">Manage custom storage locations, household users, single sign-on, and external integrations.</p>
      </div>

      <!-- Quick Actions: Expand/Collapse All + Jump Links -->
      <div class="flex items-center gap-2">
        <button 
          @click="toggleAllSections"
          class="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700/80 text-xs font-semibold transition-colors flex items-center gap-1.5">
          <ChevronsUpDown class="w-3.5 h-3.5 text-brand-400" />
          <span>{{ areAllOpen ? 'Collapse All' : 'Expand All' }}</span>
        </button>
      </div>
    </div>

    <!-- Quick Jump Filter Pills -->
    <div class="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none">
      <button 
        v-for="sec in sections" 
        :key="sec.id"
        @click="jumpToSection(sec.id)"
        class="px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all flex items-center gap-1.5"
        :class="openSections[sec.id] ? 'bg-slate-800 text-brand-300 border border-brand-500/40 shadow-sm' : 'bg-slate-900 text-slate-400 border border-slate-800 hover:text-white hover:bg-slate-800'">
        <component :is="sec.icon" class="w-3.5 h-3.5" />
        {{ sec.title }}
        <span class="w-1.5 h-1.5 rounded-full" :class="openSections[sec.id] ? 'bg-brand-400' : 'bg-slate-600'"></span>
      </button>
    </div>

    <!-- ========================================================================= -->
    <!-- SECTION 0: GENERAL & BRANDING (Collapsible)                               -->
    <!-- ========================================================================= -->
    <div id="section-general" class="bg-slate-850 bg-slate-900/90 border border-slate-700/80 rounded-3xl overflow-hidden shadow-lg transition-all duration-200">
      
      <!-- Collapsible Header -->
      <div 
        @click="toggleSection('general')"
        class="p-5 flex items-center justify-between cursor-pointer select-none hover:bg-slate-800/40 transition-colors">
        <div class="flex items-center gap-3.5">
          <div class="w-10 h-10 rounded-2xl bg-brand-500/10 border border-brand-500/30 flex items-center justify-center text-brand-400">
            <Sliders class="w-5 h-5" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h3 class="font-bold text-lg text-white">General &amp; Branding</h3>
              <span class="text-xs font-bold px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                {{ currentAppName }}
              </span>
            </div>
            <p class="text-xs text-slate-400 mt-0.5">Customize your app name, header branding, and browser page titles.</p>
          </div>
        </div>

        <div class="w-8 h-8 rounded-xl bg-slate-800 flex items-center justify-center text-slate-400 hover:text-white transition-transform">
          <ChevronDown class="w-4 h-4 transition-transform duration-200" :class="{ 'rotate-180': openSections.general }" />
        </div>
      </div>

      <!-- Collapsible Content -->
      <div v-show="openSections.general" class="p-6 pt-0 border-t border-slate-800/80 mt-2 space-y-4 animate-in fade-in duration-150">
        
        <div class="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-5 space-y-4">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-700/80 pb-3">
            <div>
              <h4 class="font-bold text-white text-sm">Application Name</h4>
              <p class="text-xs text-slate-400 mt-0.5">Set a custom name for this application. It replaces &ldquo;Skafferi&rdquo; in the header navigation, browser page titles, and throughout the app.</p>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 items-start">
            <div class="space-y-2">
              <label class="text-xs font-semibold text-slate-300 block">App Name</label>
              <div class="relative">
                <input 
                  v-model="settings.appName"
                  type="text"
                  placeholder="e.g. My Pantry, The Jostad Pantry (default: Skafferi)"
                  maxlength="50"
                  class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-brand-500 transition-colors shadow-inner pr-16" />
                <button 
                  v-if="settings.appName"
                  @click="settings.appName = ''"
                  type="button"
                  title="Reset to default name (Skafferi)"
                  class="absolute right-2 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-white bg-slate-800 px-2 py-1 rounded-lg border border-slate-700 transition-colors">
                  Reset
                </button>
              </div>
              <p class="text-[11px] text-slate-400">
                Leave blank to use the default name (<span class="font-semibold text-slate-300">Skafferi</span>).
              </p>
            </div>

            <!-- Live Preview Card -->
            <div class="p-4 bg-slate-950/70 border border-slate-800 rounded-xl space-y-2">
              <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">Live Preview</span>
              <div class="flex items-center gap-2.5">
                <div class="w-8 h-8 rounded-xl bg-[#68458c] border border-brand-400/30 flex items-center justify-center text-white text-xs font-bold shadow-md">
                  {{ currentAppName.charAt(0).toUpperCase() }}
                </div>
                <div>
                  <span class="font-extrabold text-base text-white tracking-tight">
                    {{ currentAppName }}
                  </span>
                </div>
              </div>
              <p class="text-[11px] text-slate-400 pt-1 border-t border-slate-800/80">
                Browser Title: <span class="text-brand-300 font-mono text-[11px]">Inventory | {{ currentAppName }}</span>
              </p>
            </div>
          </div>
        </div>

        <!-- Section Save Button -->
        <div class="flex justify-end pt-1">
          <button 
            @click="saveAllSettings" 
            :disabled="isSaving"
            class="px-5 py-2.5 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs shadow-md transition-all flex items-center gap-2">
            <Save class="w-4 h-4" />
            <span>{{ isSaving ? 'Saving...' : 'Save General Settings' }}</span>
          </button>
        </div>

      </div>
    </div>

    <!-- ========================================================================= -->
    <!-- SECTION 1: STORAGE LOCATIONS (Collapsible)                                -->
    <!-- ========================================================================= -->
    <div id="section-locations" class="bg-slate-850 bg-slate-900/90 border border-slate-700/80 rounded-3xl overflow-hidden shadow-lg transition-all duration-200">
      
      <!-- Collapsible Header -->
      <div 
        @click="toggleSection('locations')"
        class="p-5 flex items-center justify-between cursor-pointer select-none hover:bg-slate-800/40 transition-colors">
        <div class="flex items-center gap-3.5">
          <div class="w-10 h-10 rounded-2xl bg-brand-500/10 border border-brand-500/30 flex items-center justify-center text-brand-400">
            <MapPin class="w-5 h-5" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h3 class="font-bold text-lg text-white">Storage Locations</h3>
              <span class="text-xs font-bold px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                {{ locations.length }}
              </span>
            </div>
            <p class="text-xs text-slate-400 mt-0.5">Custom storage areas (e.g. Upstairs Freezer, Downstairs Deep Freezer, Garage Pantry, Spice Rack).</p>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <button 
            @click.stop="openAddLocationModal"
            class="hidden sm:flex px-3.5 py-1.5 bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs rounded-xl shadow-sm items-center gap-1.5">
            <Plus class="w-3.5 h-3.5" />
            <span>Add Location</span>
          </button>
          <div class="w-8 h-8 rounded-xl bg-slate-800 flex items-center justify-center text-slate-400 hover:text-white transition-transform">
            <ChevronDown class="w-4 h-4 transition-transform duration-200" :class="{ 'rotate-180': openSections.locations }" />
          </div>
        </div>
      </div>

      <!-- Collapsible Content -->
      <div v-show="openSections.locations" class="p-6 pt-0 border-t border-slate-800/80 mt-2 space-y-4 animate-in fade-in duration-150">
        <div class="flex sm:hidden justify-end pt-2">
          <button 
            @click="openAddLocationModal"
            class="px-3.5 py-1.5 bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs rounded-xl shadow-sm flex items-center gap-1.5">
            <Plus class="w-3.5 h-3.5" />
            <span>Add Location</span>
          </button>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 pt-1">
          <div 
            v-for="loc in locations" 
            :key="loc.id"
            class="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-4 flex flex-col justify-between hover:border-brand-500/40 transition-all group">
            
            <div>
              <div class="flex items-start justify-between">
                <div class="w-10 h-10 rounded-xl bg-brand-950/80 border border-brand-800/50 flex items-center justify-center text-brand-300">
                  <component :is="getIconComponent(loc.icon)" class="w-5 h-5" />
                </div>
                
                <span class="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-slate-900 border border-slate-700 text-slate-300">
                  {{ loc.type || 'PANTRY' }}
                </span>
              </div>

              <h4 class="font-bold text-white text-base mt-3">{{ loc.name }}</h4>
              <p class="text-xs text-slate-400 mt-1 min-h-[1.5rem]">{{ loc.description || 'No description provided' }}</p>
            </div>

            <div class="flex items-center justify-end gap-1 mt-4 pt-3 border-t border-slate-700/60">
              <button 
                @click="openEditLocationModal(loc)"
                title="Edit location"
                class="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-700 transition-colors">
                <Edit2 class="w-3.5 h-3.5" />
              </button>
              <button 
                @click="deleteLocation(loc)"
                :disabled="locations.length <= 1"
                title="Delete location"
                class="p-2 rounded-lg text-rose-400 hover:text-rose-300 hover:bg-rose-500/10 disabled:opacity-30 transition-colors">
                <Trash2 class="w-3.5 h-3.5" />
              </button>
            </div>

          </div>
        </div>
      </div>

    </div>

    <!-- ========================================================================= -->
    <!-- SECTION: FRESHNESS & EXPIRATION ALERTS                                   -->
    <!-- ========================================================================= -->
    <div id="section-notifications" class="bg-slate-850 bg-slate-900/90 border border-slate-700/80 rounded-3xl overflow-hidden shadow-lg transition-all duration-200">
      
      <!-- Collapsible Header -->
      <div 
        @click="toggleSection('notifications')"
        class="p-5 flex items-center justify-between cursor-pointer select-none hover:bg-slate-800/40 transition-colors">
        <div class="flex items-center gap-3.5">
          <div class="w-10 h-10 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400">
            <Bell class="w-5 h-5" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h3 class="font-bold text-lg text-white">Freshness &amp; Expiration Alerts</h3>
            </div>
            <p class="text-xs text-slate-400 mt-0.5">Configure reminder thresholds for fresh produce and foods approaching expiration.</p>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-xl bg-slate-800 flex items-center justify-center text-slate-400 hover:text-white transition-transform">
            <ChevronDown class="w-4 h-4 transition-transform duration-200" :class="{ 'rotate-180': openSections.notifications }" />
          </div>
        </div>
      </div>

      <!-- Collapsible Content -->
      <div v-show="openSections.notifications" class="p-6 pt-0 border-t border-slate-800/80 mt-2 space-y-5 animate-in fade-in duration-150">
        
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 pt-1">
          
          <!-- Fresh Produce & Perishables Rule -->
          <div class="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-5 space-y-4">
            <div class="flex items-center justify-between border-b border-slate-700/80 pb-3">
              <div class="flex items-center gap-2.5">
                <div class="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold text-sm border border-emerald-500/30">
                  <Sparkles class="w-4 h-4" />
                </div>
                <div>
                  <h4 class="font-bold text-white text-sm">Fresh Produce Reminders</h4>
                  <p class="text-[11px] text-slate-400">Remind about perishable food even without an expiration date.</p>
                </div>
              </div>
              <input 
                type="checkbox" 
                v-model="settings.freshFoodReminderEnabled" 
                class="w-4 h-4 rounded text-brand-600 bg-slate-900 border-slate-700 focus:ring-brand-500" />
            </div>

            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Check Freshness After (Days)</label>
              <div class="flex items-center gap-2">
                <input 
                  type="number" 
                  min="1" 
                  max="60" 
                  v-model.number="settings.freshFoodReminderDays" 
                  class="w-24 bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-brand-500" />
                <span class="text-xs text-slate-400">days in refrigerator or storage</span>
              </div>
              <p class="text-[11px] text-slate-400 mt-2">
                Items marked as &ldquo;Perishable / Fresh Produce&rdquo; trigger a reminder once stored for this many days.
              </p>
            </div>
          </div>

          <!-- Expiration Date Advance Rule -->
          <div class="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-5 space-y-4">
            <div class="flex items-center justify-between border-b border-slate-700/80 pb-3">
              <div class="flex items-center gap-2.5">
                <div class="w-8 h-8 rounded-lg bg-amber-500/20 text-amber-400 flex items-center justify-center font-bold text-sm border border-amber-500/30">
                  <Clock class="w-4 h-4" />
                </div>
                <div>
                  <h4 class="font-bold text-white text-sm">Expiration Date Alerts</h4>
                  <p class="text-[11px] text-slate-400">Advance notice before dated foods expire.</p>
                </div>
              </div>
              <input 
                type="checkbox" 
                v-model="settings.expirationReminderEnabled" 
                class="w-4 h-4 rounded text-brand-600 bg-slate-900 border-slate-700 focus:ring-brand-500" />
            </div>

            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Advance Warning Lead Time (Days)</label>
              <div class="flex items-center gap-2">
                <input 
                  type="number" 
                  min="1" 
                  max="30" 
                  v-model.number="settings.expirationReminderDays" 
                  class="w-24 bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-brand-500" />
                <span class="text-xs text-slate-400">days before expiration date</span>
              </div>
              <p class="text-[11px] text-slate-400 mt-2">
                Batches with expiration dates within this window are flagged as &ldquo;Expiring Soon&rdquo;.
              </p>
            </div>
          </div>

        </div>

        <div class="flex justify-end pt-2">
          <button 
            type="button" 
            @click="saveAllSettings" 
            :disabled="isSaving" 
            class="px-5 py-2 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs shadow-sm flex items-center gap-2">
            <Save class="w-4 h-4" />
            <span>Save Reminder Settings</span>
          </button>
        </div>

      </div>

    </div>

    <!-- ========================================================================= -->
    <!-- SECTION: CATEGORIES & DEFAULT SHELF-LIFE PRESETS (Collapsible)           -->
    <!-- ========================================================================= -->
    <div id="section-categories" class="bg-slate-850 bg-slate-900/90 border border-slate-700/80 rounded-3xl overflow-hidden shadow-lg transition-all duration-200">
      
      <!-- Collapsible Header -->
      <div 
        @click="toggleSection('categories')"
        class="p-5 flex items-center justify-between cursor-pointer select-none hover:bg-slate-800/40 transition-colors">
        <div class="flex items-center gap-3.5">
          <div class="w-10 h-10 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400">
            <FolderTree class="w-5 h-5" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h3 class="font-bold text-lg text-white">Categories &amp; Shelf-Life Presets</h3>
              <span class="text-xs font-bold px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                {{ categories.length }}
              </span>
            </div>
            <p class="text-xs text-slate-400 mt-0.5">Customize automatic expiration date offsets applied when adding new items or batches.</p>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-xl bg-slate-800 flex items-center justify-center text-slate-400 hover:text-white transition-transform">
            <ChevronDown class="w-4 h-4 transition-transform duration-200" :class="{ 'rotate-180': openSections.categories }" />
          </div>
        </div>
      </div>

      <!-- Collapsible Content -->
      <div v-show="openSections.categories" class="p-6 pt-0 border-t border-slate-800/80 mt-2 space-y-4 animate-in fade-in duration-150">
        
        <!-- Category Presets List -->
        <div class="bg-slate-900/60 border border-slate-700/80 rounded-2xl overflow-hidden divide-y divide-slate-800/80">
          <div 
            v-for="cat in categories" 
            :key="cat.id"
            class="p-3.5 sm:px-5 sm:py-3.5 flex flex-col md:flex-row md:items-center justify-between gap-3.5 hover:bg-slate-800/40 transition-colors group">
            
            <!-- Category Info & Icon -->
            <div class="flex items-center gap-3.5 min-w-[220px]">
              <div class="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/25 flex items-center justify-center text-amber-400 shrink-0">
                <component :is="getCategoryIconComponent(cat.icon)" class="w-5 h-5" />
              </div>
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <span class="font-bold text-sm text-white truncate">{{ cat.name }}</span>
                  <span v-if="cat.defaultShelfLifeDays" class="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full bg-amber-500/15 text-amber-300 border border-amber-500/30 shrink-0">
                    +{{ cat.defaultShelfLifeDays }}d
                  </span>
                  <span v-else class="text-[10px] text-slate-500 px-2 py-0.5 rounded-full bg-slate-800/80 border border-slate-700/60 shrink-0">
                    No default
                  </span>
                </div>
                <p class="text-[11px] text-slate-400 mt-0.5 truncate">
                  {{ formatShelfLifeDescription(cat.defaultShelfLifeDays) }}
                </p>
              </div>
            </div>

            <!-- Controls: Quick presets, input, and save -->
            <div class="flex items-center gap-2 sm:gap-3 flex-wrap md:flex-nowrap justify-between md:justify-end">
              <!-- Quick Chips -->
              <div class="flex items-center gap-1">
                <button 
                  type="button" 
                  @click="cat.defaultShelfLifeDays = 7" 
                  title="Set to 1 week (7 days)"
                  class="px-2 py-1 text-[11px] font-medium rounded-lg bg-slate-800 hover:bg-slate-700 hover:text-white text-slate-300 border border-slate-700/70 transition-colors">
                  1w
                </button>
                <button 
                  type="button" 
                  @click="cat.defaultShelfLifeDays = 14" 
                  title="Set to 2 weeks (14 days)"
                  class="px-2 py-1 text-[11px] font-medium rounded-lg bg-slate-800 hover:bg-slate-700 hover:text-white text-slate-300 border border-slate-700/70 transition-colors">
                  2w
                </button>
                <button 
                  type="button" 
                  @click="cat.defaultShelfLifeDays = 30" 
                  title="Set to 1 month (30 days)"
                  class="px-2 py-1 text-[11px] font-medium rounded-lg bg-slate-800 hover:bg-slate-700 hover:text-white text-slate-300 border border-slate-700/70 transition-colors">
                  1m
                </button>
                <button 
                  type="button" 
                  @click="cat.defaultShelfLifeDays = 180" 
                  title="Set to 6 months (180 days)"
                  class="px-2 py-1 text-[11px] font-medium rounded-lg bg-slate-800 hover:bg-slate-700 hover:text-white text-slate-300 border border-slate-700/70 transition-colors hidden sm:inline-block">
                  6m
                </button>
                <button 
                  type="button" 
                  @click="cat.defaultShelfLifeDays = 365" 
                  title="Set to 1 year (365 days)"
                  class="px-2 py-1 text-[11px] font-medium rounded-lg bg-slate-800 hover:bg-slate-700 hover:text-white text-slate-300 border border-slate-700/70 transition-colors hidden sm:inline-block">
                  1y
                </button>
                <button 
                  type="button" 
                  @click="cat.defaultShelfLifeDays = null" 
                  title="Clear preset (manual date entry only)"
                  class="px-2 py-1 text-[11px] font-medium rounded-lg bg-slate-800/80 hover:bg-rose-950/60 text-slate-400 hover:text-rose-300 border border-slate-700/60 hover:border-rose-800/40 transition-colors">
                  Clear
                </button>
              </div>

              <!-- Number input -->
              <div class="flex items-center gap-1.5 bg-slate-950 border border-slate-700 rounded-xl px-2.5 py-1 focus-within:border-amber-400/80 transition-colors">
                <input 
                  type="number" 
                  min="1"
                  max="3650"
                  placeholder="None" 
                  v-model.number="cat.defaultShelfLifeDays" 
                  class="w-16 bg-transparent text-xs text-white font-mono text-right focus:outline-none" />
                <span class="text-[11px] text-slate-500 font-medium">days</span>
              </div>

              <!-- Save button for this row -->
              <button 
                type="button" 
                @click="saveCategoryShelfLife(cat)" 
                title="Save shelf-life preset for this category"
                class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 hover:border-slate-600 text-white text-xs font-semibold transition-colors flex items-center gap-1.5 shrink-0">
                <Save class="w-3.5 h-3.5 text-slate-400" />
                <span>Save</span>
              </button>
            </div>

          </div>
        </div>

        <div class="flex flex-col sm:flex-row items-center justify-between gap-3 pt-2">
          <p class="text-xs text-slate-400">
            Changes take effect immediately when adding new inventory items or batches.
          </p>
          <button 
            type="button" 
            @click="saveAllCategories" 
            class="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs shadow-sm flex items-center justify-center gap-2 transition-all">
            <Save class="w-4 h-4" />
            <span>Save All Category Presets</span>
          </button>
        </div>
      </div>

    </div>

    <!-- ========================================================================= -->
    <!-- SECTION 2: HOUSEHOLD USERS (Collapsible)                                  -->
    <!-- ========================================================================= -->
    <div id="section-users" class="bg-slate-850 bg-slate-900/90 border border-slate-700/80 rounded-3xl overflow-hidden shadow-lg transition-all duration-200">
      
      <!-- Collapsible Header -->
      <div 
        @click="toggleSection('users')"
        class="p-5 flex items-center justify-between cursor-pointer select-none hover:bg-slate-800/40 transition-colors">
        <div class="flex items-center gap-3.5">
          <div class="w-10 h-10 rounded-2xl bg-brand-500/10 border border-brand-500/30 flex items-center justify-center text-brand-400">
            <Users class="w-5 h-5" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h3 class="font-bold text-lg text-white">Household Users</h3>
              <span class="text-xs font-bold px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                {{ users.length }}
              </span>
            </div>
            <p class="text-xs text-slate-400 mt-0.5">Manage household accounts, roles, and member profiles.</p>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <button 
            @click.stop="openAddUserModal"
            class="hidden sm:flex px-3.5 py-1.5 bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs rounded-xl shadow-sm items-center gap-1.5">
            <UserPlus class="w-3.5 h-3.5" />
            <span>Add Member</span>
          </button>
          <div class="w-8 h-8 rounded-xl bg-slate-800 flex items-center justify-center text-slate-400 hover:text-white transition-transform">
            <ChevronDown class="w-4 h-4 transition-transform duration-200" :class="{ 'rotate-180': openSections.users }" />
          </div>
        </div>
      </div>

      <!-- Collapsible Content -->
      <div v-show="openSections.users" class="p-6 pt-0 border-t border-slate-800/80 mt-2 space-y-4 animate-in fade-in duration-150">
        <div class="flex sm:hidden justify-end pt-2">
          <button 
            @click="openAddUserModal"
            class="px-3.5 py-1.5 bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs rounded-xl shadow-sm flex items-center gap-1.5">
            <UserPlus class="w-3.5 h-3.5" />
            <span>Add Member</span>
          </button>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 pt-1">
          <div 
            v-for="user in users" 
            :key="user.id"
            class="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-4 flex items-center justify-between hover:border-brand-500/40 transition-all">
            
            <div class="flex items-center gap-3">
              <div 
                class="w-11 h-11 rounded-xl flex items-center justify-center font-bold text-white shadow-md text-sm flex-shrink-0"
                :style="{ backgroundColor: user.avatarColor || '#8B5CF6' }">
                {{ (user.displayName || user.username).charAt(0).toUpperCase() }}
              </div>
              <div class="min-w-0">
                <h4 class="font-bold text-white text-sm truncate">{{ user.displayName || user.username }}</h4>
                <p class="text-[11px] text-slate-400">@{{ user.username }} &bull; {{ user.role }}</p>
                <p v-if="user.email" class="text-[10px] text-slate-500 truncate">{{ user.email }}</p>
              </div>
            </div>

            <div class="flex items-center gap-1">
              <button 
                @click="openEditUserModal(user)"
                title="Edit member"
                class="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-700">
                <Edit2 class="w-3.5 h-3.5" />
              </button>
              <button 
                @click="deleteUser(user)"
                :disabled="users.length <= 1"
                title="Delete member"
                class="p-2 rounded-lg text-rose-400 hover:text-rose-300 hover:bg-rose-500/10 disabled:opacity-30">
                <Trash2 class="w-3.5 h-3.5" />
              </button>
            </div>

          </div>
        </div>
      </div>

    </div>

    <!-- ========================================================================= -->
    <!-- SECTION 3: SSO & AUTHENTICATION (Authentik & Google) (Collapsible)        -->
    <!-- ========================================================================= -->
    <div id="section-auth" class="bg-slate-850 bg-slate-900/90 border border-slate-700/80 rounded-3xl overflow-hidden shadow-lg transition-all duration-200">
      
      <!-- Collapsible Header -->
      <div 
        @click="toggleSection('auth')"
        class="p-5 flex items-center justify-between cursor-pointer select-none hover:bg-slate-800/40 transition-colors">
        <div class="flex items-center gap-3.5">
          <div class="w-10 h-10 rounded-2xl bg-brand-500/10 border border-brand-500/30 flex items-center justify-center text-brand-400">
            <ShieldCheck class="w-5 h-5" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h3 class="font-bold text-lg text-white">Single Sign-On &amp; Auth</h3>
              <span v-if="settings.authentikEnabled || settings.googleAuthEnabled" class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                Active
              </span>
              <span v-else class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-800 text-slate-400 border border-slate-700">
                Disabled
              </span>
            </div>
            <p class="text-xs text-slate-400 mt-0.5">Integrate Authentik (OpenID Connect) and Google Authentication for users.</p>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-xl bg-slate-800 flex items-center justify-center text-slate-400 hover:text-white transition-transform">
            <ChevronDown class="w-4 h-4 transition-transform duration-200" :class="{ 'rotate-180': openSections.auth }" />
          </div>
        </div>
      </div>

      <!-- Collapsible Content -->
      <div v-show="openSections.auth" class="p-6 pt-0 border-t border-slate-800/80 mt-2 space-y-6 animate-in fade-in duration-150">
        
        <!-- Authentik (OpenID Connect) -->
        <div class="bg-slate-800/80 rounded-2xl p-5 border border-slate-700/80 shadow-sm space-y-4">
          <div class="flex items-center justify-between border-b border-slate-700/80 pb-3">
            <div class="flex items-center gap-2.5">
              <div class="w-8 h-8 rounded-lg bg-orange-500/20 text-orange-400 flex items-center justify-center font-bold text-sm border border-orange-500/30">
                A
              </div>
              <div>
                <h4 class="font-bold text-white text-sm">Authentik / Generic OpenID Connect (OIDC)</h4>
                <p class="text-[11px] text-slate-400">Single Sign-On using Authentik, Authelia, or Keycloak.</p>
              </div>
            </div>

            <label class="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" v-model="settings.authentikEnabled" class="sr-only peer">
              <div class="w-11 h-6 bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand-600"></div>
            </label>
          </div>

          <div v-if="settings.authentikEnabled" class="space-y-4 pt-1">
            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Authentik Issuer / Application URL *</label>
              <input 
                v-model="settings.authentikIssuerUrl" 
                placeholder="https://auth.yourdomain.com/application/o/skafferi/" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="text-xs font-semibold text-slate-300 block mb-1">Client ID</label>
                <input 
                  v-model="settings.authentikClientId" 
                  placeholder="Client ID from Authentik Provider" 
                  class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
              </div>

              <div>
                <label class="text-xs font-semibold text-slate-300 block mb-1">Client Secret</label>
                <input 
                  v-model="settings.authentikClientSecret" 
                  type="password" 
                  placeholder="••••••••" 
                  class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
              </div>
            </div>

            <!-- Redirect URI Info Box -->
            <div class="p-3 bg-slate-950/70 rounded-xl border border-slate-800 text-xs text-slate-300 space-y-1">
              <span class="font-semibold text-slate-200">Authentik Provider Redirect URI:</span>
              <p class="text-[11px] text-slate-400">Copy this callback URI into your Authentik Provider setup:</p>
              <div class="bg-slate-900 px-3 py-1.5 rounded-lg font-mono text-brand-400 border border-slate-800 select-all">
                {{ currentOrigin }}/api/auth/callback/oidc
              </div>
            </div>
          </div>
        </div>

        <!-- Google OAuth2 -->
        <div class="bg-slate-800/80 rounded-2xl p-5 border border-slate-700/80 shadow-sm space-y-4">
          <div class="flex items-center justify-between border-b border-slate-700/80 pb-3">
            <div class="flex items-center gap-2.5">
              <div class="w-8 h-8 rounded-lg bg-blue-500/20 text-blue-400 flex items-center justify-center font-bold text-sm border border-blue-500/30">
                G
              </div>
              <div>
                <h4 class="font-bold text-white text-sm">Google Authentication</h4>
                <p class="text-[11px] text-slate-400">Allow signing into {{ currentAppName }} with Google accounts.</p>
              </div>
            </div>

            <label class="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" v-model="settings.googleAuthEnabled" class="sr-only peer">
              <div class="w-11 h-6 bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand-600"></div>
            </label>
          </div>

          <div v-if="settings.googleAuthEnabled" class="space-y-4 pt-1">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="text-xs font-semibold text-slate-300 block mb-1">Google Client ID</label>
                <input 
                  v-model="settings.googleClientId" 
                  placeholder="xxxxxx.apps.googleusercontent.com" 
                  class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
              </div>

              <div>
                <label class="text-xs font-semibold text-slate-300 block mb-1">Google Client Secret</label>
                <input 
                  v-model="settings.googleClientSecret" 
                  type="password" 
                  placeholder="••••••••" 
                  class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
              </div>
            </div>

            <!-- Redirect URI Info Box -->
            <div class="p-3 bg-slate-950/70 rounded-xl border border-slate-800 text-xs text-slate-300 space-y-1">
              <span class="font-semibold text-slate-200">Google Authorized Redirect URI:</span>
              <p class="text-[11px] text-slate-400">Add this URI in Google Cloud Console > Credentials > Authorized redirect URIs:</p>
              <div class="bg-slate-900 px-3 py-1.5 rounded-lg font-mono text-brand-400 border border-slate-800 select-all">
                {{ currentOrigin }}/api/auth/callback/google
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end pt-2">
          <button 
            type="button" 
            @click="saveAllSettings"
            :disabled="isSaving"
            class="px-5 py-2 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs shadow-sm flex items-center gap-2">
            <Save class="w-4 h-4" />
            <span>{{ isSaving ? 'Saving...' : 'Save SSO Settings' }}</span>
          </button>
        </div>

      </div>

    </div>

    <!-- ========================================================================= -->
    <!-- SECTION 4: INTEGRATIONS & APIS (Bring, Mealie, Kroger) (Collapsible)       -->
    <!-- ========================================================================= -->
    <div id="section-integrations" class="bg-slate-850 bg-slate-900/90 border border-slate-700/80 rounded-3xl overflow-hidden shadow-lg transition-all duration-200">
      
      <!-- Collapsible Header -->
      <div 
        @click="toggleSection('integrations')"
        class="p-5 flex items-center justify-between cursor-pointer select-none hover:bg-slate-800/40 transition-colors">
        <div class="flex items-center gap-3.5">
          <div class="w-10 h-10 rounded-2xl bg-brand-500/10 border border-brand-500/30 flex items-center justify-center text-brand-400">
            <Link2 class="w-5 h-5" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h3 class="font-bold text-lg text-white">Integrations &amp; APIs</h3>
              <span class="text-xs font-bold px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                Bring! &bull; Mealie &bull; Kroger &bull; AI Chef
              </span>
            </div>
            <p class="text-xs text-slate-400 mt-0.5">Connect external mobile lists, meal planning webhooks, and grocery catalogs.</p>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-xl bg-slate-800 flex items-center justify-center text-slate-400 hover:text-white transition-transform">
            <ChevronDown class="w-4 h-4 transition-transform duration-200" :class="{ 'rotate-180': openSections.integrations }" />
          </div>
        </div>
      </div>

      <!-- Collapsible Content -->
      <div v-show="openSections.integrations" class="p-6 pt-0 border-t border-slate-800/80 mt-2 space-y-6 animate-in fade-in duration-150">
        
        <!-- 1. Bring! -->
        <div class="bg-slate-800/80 rounded-2xl p-5 border border-slate-700/80 shadow-sm space-y-4">
          <div class="flex items-center justify-between border-b border-slate-700/80 pb-3">
            <div class="flex items-center gap-2.5">
              <div class="w-8 h-8 rounded-lg bg-red-500/20 text-red-400 flex items-center justify-center font-bold text-sm border border-red-500/30">
                B!
              </div>
              <div>
                <div class="flex items-center gap-1.5">
                  <h4 class="font-bold text-white text-sm">Bring! Shopping List</h4>
                  <button 
                    type="button" 
                    @click="bringHelpModalOpen = true"
                    title="Instructions for Google or Apple Sign-In"
                    class="p-0.5 rounded-md text-slate-400 hover:text-brand-300 hover:bg-slate-750 transition-colors">
                    <Info class="w-3.5 h-3.5" />
                  </button>
                </div>
                <p class="text-[11px] text-slate-400">Automatically sync low-stock pantry items to mobile phones.</p>
              </div>
            </div>
            <button 
              type="button" 
              @click="testBringConnection" 
              :disabled="isTestingBring || !settings.bringEmail"
              class="px-3 py-1.5 rounded-xl bg-slate-700 hover:bg-slate-600 disabled:opacity-50 text-white text-xs font-semibold transition-colors">
              {{ isTestingBring ? 'Testing...' : 'Test Connection' }}
            </button>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Bring! Account Email</label>
              <input 
                v-model="settings.bringEmail" 
                type="email" 
                placeholder="user@example.com" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>

            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Bring! Password</label>
              <input 
                v-model="settings.bringPassword" 
                type="password" 
                placeholder="••••••••" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>
          </div>

          <!-- Target Shopping List Selector -->
          <div v-if="bringLists.length > 0" class="pt-1">
            <label class="text-xs font-semibold text-slate-300 block mb-1">Target Shopping List in Bring!</label>
            <select 
              v-model="settings.bringListUuid"
              class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500">
              <option value="">Default Primary List (Auto-selected by Bring!)</option>
              <option v-for="l in bringLists" :key="l.listUuid" :value="l.listUuid">
                {{ l.name }}
              </option>
            </select>
            <p class="text-[11px] text-slate-400 mt-1">Select which shared list in your Bring! account {{ currentAppName }} should sync pantry items to.</p>
          </div>

          <!-- Note for Google/Apple Sign-in users -->
          <div class="p-2.5 rounded-xl bg-slate-900/60 border border-slate-750 text-[11px] text-slate-400 flex items-start gap-2">
            <span class="text-amber-400 font-bold">💡 Note:</span>
            <span>If you log into Bring! using <strong>Google or Apple Sign-In</strong>, simply set a password in the Bring! mobile app (<strong>Profile &gt; Settings &gt; Change Password</strong>) or use &ldquo;Forgot Password&rdquo; on <a href="https://web.getbring.com" target="_blank" class="text-brand-300 underline hover:text-white">web.getbring.com</a>. Then enter your Gmail address and that password above.</span>
          </div>

          <div class="flex items-center justify-between pt-2">
            <span class="text-xs text-slate-300">Enable automatic background sync on low stock</span>
            <input 
              type="checkbox" 
              v-model="settings.bringAutoSync" 
              class="w-4 h-4 rounded text-brand-600 bg-slate-900 border-slate-700 focus:ring-brand-500" />
          </div>
        </div>

        <!-- 2. Mealie -->
        <div class="bg-slate-800/80 rounded-2xl p-5 border border-slate-700/80 shadow-sm space-y-4">
          <div class="flex items-center justify-between border-b border-slate-700/80 pb-3 flex-wrap gap-2">
            <div class="flex items-center gap-2.5">
              <div class="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold text-sm border border-emerald-500/30">
                M
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <h4 class="font-bold text-white text-sm">Mealie Recipe Actions &amp; Sync</h4>
                  <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                    Recipe Action Ready
                  </span>
                </div>
                <p class="text-[11px] text-slate-400">Trigger custom recipe actions in Mealie to send recipes and reconcile pantry deductions in {{ currentAppName }}.</p>
              </div>
            </div>

            <div class="flex items-center gap-2">
              <router-link
                to="/recipes"
                class="px-3 py-1.5 rounded-xl bg-slate-700 hover:bg-slate-600 text-white text-xs font-semibold transition-colors flex items-center gap-1.5">
                <ChefHat class="w-3.5 h-3.5 text-amber-400" />
                <span>Cooked Meals Queue</span>
              </router-link>

              <button 
                type="button" 
                @click="sendSampleRecipeAction"
                :disabled="isSendingSample"
                title="Send a sample Mealie recipe JSON to test the workflow"
                class="px-3 py-1.5 rounded-xl bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 border border-emerald-500/30 text-xs font-semibold transition-colors flex items-center gap-1.5">
                <Sparkles class="w-3.5 h-3.5" :class="{ 'animate-spin': isSendingSample }" />
                <span>{{ isSendingSample ? 'Sending...' : 'Simulate Recipe Action' }}</span>
              </button>
            </div>
          </div>

          <!-- Step-by-Step Recipe Action Setup Guide -->
          <div class="p-4 bg-slate-950/70 rounded-2xl border border-slate-800 text-xs text-slate-300 space-y-3">
            <div class="flex items-center justify-between">
              <span class="font-bold text-white text-xs flex items-center gap-1.5">
                <span>Configure Mealie Custom Recipe Action</span>
              </span>
              <span class="text-[11px] text-slate-400">Settings &gt; Data Management &gt; Custom Recipe Actions</span>
            </div>

            <ol class="list-decimal list-inside space-y-1.5 text-slate-300 text-xs leading-relaxed">
              <li>In Mealie, open <strong>Settings &gt; Data Management &gt; Custom Recipe Actions</strong> (or Household &gt; Recipe Actions).</li>
              <li>Click <strong>Create Action</strong> (or <strong>+</strong>).</li>
              <li>Set <strong>Action Type</strong> to <span class="px-1.5 py-0.5 rounded bg-slate-800 text-amber-300 font-mono text-[11px] font-bold">POST</span> (sends recipe JSON).</li>
              <li>Set <strong>Action Title</strong> to <span class="font-semibold text-white">Cook with {{ currentAppName }}</span> (or <em>Send to {{ currentAppName }}</em>).</li>
              <li>Set the <strong>Action URL</strong> to the endpoint below:</li>
            </ol>

            <div class="flex items-center gap-2">
              <div class="flex-1 bg-slate-900 px-3.5 py-2 rounded-xl font-mono text-brand-300 border border-slate-800 text-xs select-all truncate">
                {{ currentOrigin }}/api/recipes/action
              </div>
              <button 
                type="button"
                @click="copyRecipeActionUrl"
                class="px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 hover:text-white border border-slate-700 text-xs font-semibold flex items-center gap-1.5 transition-colors shrink-0">
                <Check v-if="hasCopiedActionUrl" class="w-3.5 h-3.5 text-emerald-400" />
                <Copy v-else class="w-3.5 h-3.5" />
                <span>{{ hasCopiedActionUrl ? 'Copied!' : 'Copy URL' }}</span>
              </button>
            </div>

            <p class="text-[11px] text-slate-400 pt-1 border-t border-slate-800/80 leading-normal">
              When triggered from a recipe page in Mealie, the full recipe JSON is sent to {{ currentAppName }} and queued under <strong>Recipes &gt; Cooked Meals Queue</strong> for you to review matches, adjust quantities, and commit FIFO stock deductions.
            </p>
          </div>

          <!-- Optional direct API connection -->
          <div class="space-y-1 pt-1">
            <span class="text-xs font-semibold text-slate-300 block">Mealie Instance Connection (Optional)</span>
            <p class="text-[11px] text-slate-400">Configure these credentials if you also want {{ currentAppName }} to browse and suggest recipes based on your current pantry inventory.</p>
          </div>

          <div class="space-y-4">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="text-xs font-semibold text-slate-300 block mb-1">Mealie API Base URL</label>
                <input 
                  v-model="settings.mealieBaseUrl" 
                  placeholder="http://mealie:9000" 
                  class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
                <p class="text-[10px] text-slate-500 mt-1">Internal Docker/LAN URL used by Skafferi server to sync recipes.</p>
              </div>

              <div>
                <label class="text-xs font-semibold text-slate-300 block mb-1">Mealie API Token</label>
                <input 
                  v-model="settings.mealieApiToken" 
                  type="password" 
                  placeholder="Bearer Token..." 
                  class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
                <p class="text-[10px] text-slate-500 mt-1">Long-lived API token from Mealie User Profile &gt; API Tokens.</p>
              </div>
            </div>

            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Mealie Frontend URL (Optional)</label>
              <input 
                v-model="settings.mealieFrontendUrl" 
                placeholder="https://mealie.ballardplex.com" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
              <p class="text-[10px] text-slate-500 mt-1">Public browser URL used when clicking links to view recipes in Mealie (defaults to API Base URL if left blank).</p>
            </div>
          </div>

          <!-- Meal Deduction Mode Setting -->
          <div class="pt-3 border-t border-slate-700/80 space-y-2">
            <label class="text-xs font-semibold text-slate-300 block">Cooked Meal Inventory Deduction</label>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <label 
                class="flex items-start gap-3 p-3 rounded-xl border cursor-pointer transition-colors"
                :class="!settings.mealieAutoDeduct ? 'bg-brand-500/10 border-brand-500/50 text-white' : 'bg-slate-900 border-slate-700 text-slate-400 hover:border-slate-600'">
                <input 
                  type="radio" 
                  :value="false" 
                  v-model="settings.mealieAutoDeduct" 
                  class="mt-0.5 text-brand-500 focus:ring-brand-500" />
                <div>
                  <span class="text-xs font-bold block text-white">Review Queue (Recommended)</span>
                  <span class="text-[11px] text-slate-400 block mt-0.5">Hold cooked meals in Recipes &gt; Cooked Meals Queue to verify ingredients and adjust deduction amounts before committing.</span>
                </div>
              </label>

              <label 
                class="flex items-start gap-3 p-3 rounded-xl border cursor-pointer transition-colors"
                :class="settings.mealieAutoDeduct ? 'bg-brand-500/10 border-brand-500/50 text-white' : 'bg-slate-900 border-slate-700 text-slate-400 hover:border-slate-600'">
                <input 
                  type="radio" 
                  :value="true" 
                  v-model="settings.mealieAutoDeduct" 
                  class="mt-0.5 text-brand-500 focus:ring-brand-500" />
                <div>
                  <span class="text-xs font-bold block text-white">Immediate Auto-Deduct</span>
                  <span class="text-[11px] text-slate-400 block mt-0.5">Automatically deduct pantry items via FIFO in the background as soon as Mealie triggers a recipe action.</span>
                </div>
              </label>
            </div>
          </div>
        </div>

        <!-- 3. Kroger Developer API -->
        <div class="bg-slate-800/80 rounded-2xl p-5 border border-slate-700/80 shadow-sm space-y-4">
          <div class="flex items-center gap-2.5 border-b border-slate-700/80 pb-3">
            <div class="w-8 h-8 rounded-lg bg-blue-500/20 text-blue-400 flex items-center justify-center font-bold text-sm border border-blue-500/30">
              K
            </div>
            <div>
              <h4 class="font-bold text-white text-sm">Kroger Catalog API (Optional)</h4>
              <p class="text-[11px] text-slate-400">Enriches barcodes and receipts with exact Kroger store titles and images.</p>
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Client ID</label>
              <input 
                v-model="settings.krogerClientId" 
                placeholder="developer.kroger.com Client ID" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>

            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Client Secret</label>
              <input 
                v-model="settings.krogerClientSecret" 
                type="password" 
                placeholder="Client Secret" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>

            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Store Location ID</label>
              <input 
                v-model="settings.krogerLocationId" 
                placeholder="e.g. 01400923" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>
          </div>
        </div>

        <!-- 4. AI Zero-Waste Chef (LLM Integration) -->
        <div class="bg-slate-800/80 rounded-2xl p-5 border border-slate-700/80 shadow-sm space-y-4">
          <div class="flex items-center justify-between border-b border-slate-700/80 pb-3 flex-wrap gap-2">
            <div class="flex items-center gap-2.5">
              <div class="w-8 h-8 rounded-lg bg-amber-500/20 text-amber-400 flex items-center justify-center font-bold text-sm border border-amber-500/30">
                <Sparkles class="w-4 h-4" />
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <h4 class="font-bold text-white text-sm">AI Zero-Waste Chef (LLM Integration)</h4>
                  <span 
                    v-if="settings.hasLlmApiKey || (settings.llmApiKey && settings.llmApiKey !== '')" 
                    class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                    Configured
                  </span>
                  <span 
                    v-else 
                    class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-800 text-slate-400 border border-slate-700">
                    Key Required
                  </span>
                </div>
                <p class="text-[11px] text-slate-400">Generate creative, custom meal ideas from expiring ingredients using your chosen AI model.</p>
              </div>
            </div>

            <button 
              type="button" 
              @click="testLlmConnection" 
              :disabled="isTestingLlm || (!settings.llmApiKey && !settings.hasLlmApiKey)"
              class="px-3 py-1.5 rounded-xl bg-slate-700 hover:bg-slate-600 disabled:opacity-50 text-white text-xs font-semibold transition-colors flex items-center gap-1.5">
              <Sparkles class="w-3.5 h-3.5 text-amber-400" :class="{ 'animate-spin': isTestingLlm }" />
              <span>{{ isTestingLlm ? 'Testing...' : 'Test Connection' }}</span>
            </button>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">AI Provider</label>
              <select 
                v-model="settings.llmProvider"
                @change="onLlmProviderChange"
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500">
                <option value="gemini">Google Gemini</option>
                <option value="openai">OpenAI</option>
                <option value="openrouter">OpenRouter</option>
                <option value="ollama">Local Ollama</option>
                <option value="custom">Custom (OpenAI-compatible)</option>
              </select>
            </div>

            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">API Key *</label>
              <input 
                v-model="settings.llmApiKey" 
                type="password" 
                placeholder="sk-... or API Key" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500 font-mono" />
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <div class="flex items-center justify-between mb-1">
                <label class="text-xs font-semibold text-slate-300">Model Name</label>
                <span class="text-[10px] text-brand-300 font-medium">Defaults to latest version</span>
              </div>
              <input 
                v-model="settings.llmModel" 
                :placeholder="defaultModelPlaceholder" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500 font-mono text-[11px]" />
              <p class="text-[10px] text-slate-500 mt-1">Leave blank to use the latest model, or enter a specific custom model.</p>
            </div>

            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">API Base URL (Optional)</label>
              <input 
                v-model="settings.llmBaseUrl" 
                :placeholder="defaultBaseUrlPlaceholder" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500 font-mono text-[11px]" />
              <p class="text-[10px] text-slate-500 mt-1">Defaults to the official provider endpoint if left blank.</p>
            </div>
          </div>

          <div class="p-2.5 rounded-xl bg-slate-900/60 border border-slate-750 text-[11px] text-slate-400 flex items-start gap-2">
            <span class="text-amber-400 font-bold">💡 Tip:</span>
            <span>Works with Google Gemini, OpenAI, OpenRouter, or self-hosted Ollama (<code>http://host.docker.internal:11434/v1</code>). Once configured, the <strong>Zero-Waste Chef</strong> tab appears in the Recipes view.</span>
          </div>
        </div>

        <div class="flex justify-end pt-2">
          <button 
            type="button" 
            @click="saveAllSettings"
            :disabled="isSaving"
            class="px-5 py-2 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs shadow-sm flex items-center gap-2">
            <Save class="w-4 h-4" />
            <span>{{ isSaving ? 'Saving...' : 'Save Integrations' }}</span>
          </button>
        </div>

      </div>

    </div>

    <!-- ========================================================================= -->
    <!-- SECTION: EMAIL ALERTS & SMTP DIGESTS (Collapsible)                       -->
    <!-- ========================================================================= -->
    <div id="section-email" class="bg-slate-850 bg-slate-900/90 border border-slate-700/80 rounded-3xl overflow-hidden shadow-lg transition-all duration-200">
      
      <!-- Collapsible Header -->
      <div 
        @click="toggleSection('email')"
        class="p-5 flex items-center justify-between cursor-pointer select-none hover:bg-slate-800/40 transition-colors">
        <div class="flex items-center gap-3.5">
          <div class="w-10 h-10 rounded-2xl bg-sky-500/10 border border-sky-500/30 flex items-center justify-center text-sky-400">
            <Mail class="w-5 h-5" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h3 class="font-bold text-lg text-white">Email Notifications (SMTP)</h3>
              <span v-if="settings.smtpEnabled" class="text-xs font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                ACTIVE
              </span>
              <span v-else class="text-xs font-bold px-2 py-0.5 rounded-full bg-slate-800 text-slate-400 border border-slate-700">
                DISABLED
              </span>
            </div>
            <p class="text-xs text-slate-400 mt-0.5">Send daily morning expiration digests and weekend grocery shopping prep emails.</p>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-xl bg-slate-800 flex items-center justify-center text-slate-400 hover:text-white transition-transform">
            <ChevronDown class="w-4 h-4 transition-transform duration-200" :class="{ 'rotate-180': openSections.email }" />
          </div>
        </div>
      </div>

      <!-- Collapsible Content -->
      <div v-show="openSections.email" class="p-6 pt-0 border-t border-slate-800/80 mt-2 space-y-6 animate-in fade-in duration-150">
        
        <!-- Enable Switch -->
        <div class="p-4 bg-slate-800/80 border border-slate-700/80 rounded-2xl flex items-center justify-between">
          <div>
            <h4 class="font-bold text-white text-sm">Enable SMTP Outgoing Email</h4>
            <p class="text-xs text-slate-400 mt-0.5">Connect to your local mail server, Gmail app password, or Mailgun/SendGrid.</p>
          </div>
          <input 
            type="checkbox" 
            v-model="settings.smtpEnabled" 
            class="w-5 h-5 rounded text-brand-600 bg-slate-900 border-slate-700 focus:ring-brand-500" />
        </div>

        <!-- SMTP Server Credentials -->
        <div class="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-5 space-y-4">
          <h4 class="font-bold text-white text-sm border-b border-slate-700/80 pb-2">SMTP Server Configuration</h4>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div class="sm:col-span-2">
              <label class="text-xs font-semibold text-slate-300 block mb-1">SMTP Host / Server</label>
              <input 
                v-model="settings.smtpHost" 
                placeholder="e.g. smtp.gmail.com, mail.example.com" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>
            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Port</label>
              <input 
                type="number"
                v-model.number="settings.smtpPort" 
                placeholder="587" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-brand-500" />
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Encryption</label>
              <select 
                v-model="settings.smtpEncryption" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500">
                <option value="TLS">STARTTLS (587)</option>
                <option value="SSL">SSL / TLS (465)</option>
                <option value="NONE">Plain / None (25)</option>
              </select>
            </div>
            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">SMTP Username</label>
              <input 
                v-model="settings.smtpUsername" 
                placeholder="e.g. user@gmail.com" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>
            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">SMTP Password</label>
              <input 
                type="password"
                v-model="settings.smtpPassword" 
                placeholder="App password or secret" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">From Email Address</label>
              <input 
                v-model="settings.smtpFromAddress" 
                placeholder="e.g. skafferi@home.lan" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>
            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Recipient Addresses (comma-separated)</label>
              <input 
                v-model="settings.smtpRecipients" 
                placeholder="e.g. brian@example.com, family@example.com" 
                class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
            </div>
          </div>
        </div>

        <!-- Digest Schedules -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-5 space-y-3">
            <div class="flex items-center justify-between">
              <div>
                <h5 class="font-bold text-white text-xs">Daily Expiration Digest</h5>
                <p class="text-[11px] text-slate-400">Dispatched every morning at 08:00 AM</p>
              </div>
              <input 
                type="checkbox" 
                v-model="settings.smtpDailyDigestEnabled" 
                class="w-4 h-4 rounded text-brand-600 bg-slate-900 border-slate-700 focus:ring-brand-500" />
            </div>
            <p class="text-[11px] text-slate-400">
              Summarizes foods expired, foods expiring in the next 48 hours, and produce check alerts.
            </p>
            <div class="pt-2">
              <button 
                type="button" 
                @click="triggerManualDigest('daily')"
                :disabled="isSendingDigest"
                class="px-3 py-1.5 rounded-xl bg-slate-700 hover:bg-slate-600 text-white text-xs font-semibold transition-colors">
                Send Daily Digest Now
              </button>
            </div>
          </div>

          <div class="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-5 space-y-3">
            <div class="flex items-center justify-between">
              <div>
                <h5 class="font-bold text-white text-xs">Weekly Grocery Prep Digest</h5>
                <p class="text-[11px] text-slate-400">Dispatched every Friday at 05:00 PM</p>
              </div>
              <input 
                type="checkbox" 
                v-model="settings.smtpWeeklyDigestEnabled" 
                class="w-4 h-4 rounded text-brand-600 bg-slate-900 border-slate-700 focus:ring-brand-500" />
            </div>
            <p class="text-[11px] text-slate-400">
              Summarizes depleted items, low-stock staples below threshold, and predicted run-outs.
            </p>
            <div class="pt-2">
              <button 
                type="button" 
                @click="triggerManualDigest('weekly')"
                :disabled="isSendingDigest"
                class="px-3 py-1.5 rounded-xl bg-slate-700 hover:bg-slate-600 text-white text-xs font-semibold transition-colors">
                Send Weekly Digest Now
              </button>
            </div>
          </div>
        </div>

        <!-- Email Action Footer -->
        <div class="flex items-center justify-between pt-2 border-t border-slate-800/80">
          <button 
            type="button" 
            @click="testSmtpEmail"
            :disabled="isTestingEmail || !settings.smtpHost"
            class="px-4 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 disabled:opacity-40 text-white font-bold text-xs shadow-sm flex items-center gap-2 transition-all">
            <Send class="w-3.5 h-3.5" />
            <span>{{ isTestingEmail ? 'Sending Test Email...' : 'Send Test Email' }}</span>
          </button>

          <button 
            type="button" 
            @click="saveAllSettings" 
            :disabled="isSaving" 
            class="px-5 py-2 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs shadow-sm flex items-center gap-2">
            <Save class="w-4 h-4" />
            <span>Save Email Settings</span>
          </button>
        </div>

      </div>

    </div>

    <!-- Settings Page Footer -->
    <div class="pt-6 pb-2 border-t border-slate-800/80 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-slate-400">
      <div class="flex items-center gap-2">
        <span class="font-semibold text-slate-300">{{ currentAppName }}</span>
        <span>&bull;</span>
        <span>Self-hosted Pantry &amp; Inventory Management</span>
      </div>
      <div class="flex items-center gap-1.5">
        <span v-if="appVersionDisplay" class="font-mono px-2 py-0.5 rounded-md bg-slate-800 text-slate-300 border border-slate-700 text-[11px]">{{ appVersionDisplay }}</span>
      </div>
    </div>

    <!-- Floating Global Save Footer Button -->
    <div class="sticky bottom-4 z-30 flex justify-end">
      <button 
        @click="saveAllSettings"
        :disabled="isSaving"
        class="px-7 py-3 rounded-2xl bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs shadow-xl shadow-black/40 flex items-center gap-2 transition-all">
        <Save class="w-4 h-4" />
        <span>{{ isSaving ? 'Saving Changes...' : 'Save All Settings' }}</span>
      </button>
    </div>

    <!-- ======================================================== -->
    <!-- MODAL: ADD / EDIT LOCATION                               -->
    <!-- ======================================================== -->
    <div v-if="locationModalOpen" class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div class="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md overflow-hidden shadow-2xl flex flex-col">
        <div class="p-4 border-b border-slate-800 flex items-center justify-between">
          <h3 class="font-bold text-base text-white">{{ locationForm.id ? 'Edit Location' : 'Add Storage Location' }}</h3>
          <button @click="locationModalOpen = false" class="p-1 text-slate-400 hover:text-white"><X class="w-5 h-5" /></button>
        </div>

        <form @submit.prevent="saveLocation" class="p-5 space-y-4">
          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Location Name *</label>
            <input 
              v-model="locationForm.name" 
              required 
              placeholder="e.g. Upstairs Freezer, Garage Deep Freezer, Spice Rack" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500" />
          </div>

          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Description</label>
            <input 
              v-model="locationForm.description" 
              placeholder="e.g. Second floor kitchen, Chest freezer in basement" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Type</label>
              <select 
                v-model="locationForm.type" 
                class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500">
                <option value="PANTRY">Pantry / Shelf</option>
                <option value="FRIDGE">Refrigerator</option>
                <option value="FREEZER">Freezer / Deep Freeze</option>
                <option value="SPICE">Spice Rack</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Icon</label>
              <select 
                v-model="locationForm.icon" 
                class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500">
                <option value="Archive">Pantry / Archive</option>
                <option value="Refrigerator">Refrigerator</option>
                <option value="Snowflake">Snowflake / Freezer</option>
                <option value="Flame">Spice / Flame</option>
                <option value="LayoutGrid">Counter / Grid</option>
                <option value="Box">Box / Container</option>
                <option value="Home">Home / Room</option>
                <option value="Warehouse">Warehouse / Garage</option>
              </select>
            </div>
          </div>

          <div class="flex justify-end gap-2 pt-2">
            <button type="button" @click="locationModalOpen = false" class="px-4 py-2 text-xs text-slate-400 hover:text-white">Cancel</button>
            <button type="submit" class="px-5 py-2 bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs rounded-xl shadow-sm">
              Save Location
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- ======================================================== -->
    <!-- MODAL: ADD / EDIT USER                                   -->
    <!-- ======================================================== -->
    <div v-if="userModalOpen" class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div class="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md overflow-hidden shadow-2xl flex flex-col">
        <div class="p-4 border-b border-slate-800 flex items-center justify-between">
          <h3 class="font-bold text-base text-white">{{ userForm.id ? 'Edit Member' : 'Add Household Member' }}</h3>
          <button @click="userModalOpen = false" class="p-1 text-slate-400 hover:text-white"><X class="w-5 h-5" /></button>
        </div>

        <form @submit.prevent="saveUser" class="p-5 space-y-4">
          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Username *</label>
            <input 
              v-model="userForm.username" 
              required 
              placeholder="e.g. john, alex" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-brand-500" />
          </div>

          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Display Name</label>
            <input 
              v-model="userForm.displayName" 
              placeholder="e.g. John Doe" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
          </div>

          <div>
            <label class="text-xs font-semibold text-slate-300 block mb-1">Email (Optional)</label>
            <input 
              v-model="userForm.email" 
              type="email" 
              placeholder="john@example.com" 
              class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500" />
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Role</label>
              <select 
                v-model="userForm.role" 
                class="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-brand-500">
                <option value="ADMIN">Administrator</option>
                <option value="MEMBER">Family Member</option>
              </select>
            </div>

            <div>
              <label class="text-xs font-semibold text-slate-300 block mb-1">Avatar Color</label>
              <div class="flex items-center gap-2">
                <input 
                  type="color" 
                  v-model="userForm.avatarColor" 
                  class="w-9 h-9 rounded-lg bg-transparent border-0 cursor-pointer" />
                <span class="text-xs font-mono text-slate-400">{{ userForm.avatarColor }}</span>
              </div>
            </div>
          </div>

          <div class="flex justify-end gap-2 pt-2">
            <button type="button" @click="userModalOpen = false" class="px-4 py-2 text-xs text-slate-400 hover:text-white">Cancel</button>
            <button type="submit" class="px-5 py-2 bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs rounded-xl shadow-sm">
              Save Member
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- ======================================================== -->
    <!-- MODAL: BRING GOOGLE / APPLE AUTH INSTRUCTIONS            -->
    <!-- ======================================================== -->
    <div v-if="bringHelpModalOpen" class="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div class="bg-slate-900 border border-slate-700/80 rounded-3xl w-full max-w-lg overflow-hidden shadow-2xl flex flex-col animate-in fade-in zoom-in-95 duration-150">
        
        <!-- Header -->
        <div class="p-4 bg-slate-850 bg-slate-950/60 border-b border-slate-800 flex items-center justify-between">
          <div class="flex items-center gap-2.5">
            <div class="w-8 h-8 rounded-xl bg-red-500/20 text-red-400 flex items-center justify-center font-bold text-xs border border-red-500/30">
              B!
            </div>
            <div>
              <h3 class="font-bold text-sm text-white">Using Bring! with Google or Apple ID</h3>
              <p class="text-[11px] text-slate-400">Step-by-step password setup for API integration</p>
            </div>
          </div>
          <button @click="bringHelpModalOpen = false" class="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition-colors">
            <X class="w-4 h-4" />
          </button>
        </div>

        <!-- Body -->
        <div class="p-5 space-y-4 text-xs text-slate-300">
          <p class="leading-relaxed">
            Bring! does not provide an external OAuth authorization screen for third-party apps like {{ currentAppName }}. To sync your shopping list, your Bring! account simply needs a dedicated password:
          </p>

          <div class="space-y-2.5">
            <div class="flex items-start gap-3 p-3 rounded-2xl bg-slate-800/80 border border-slate-700/80">
              <div class="w-6 h-6 rounded-lg bg-brand-500/20 text-brand-300 font-bold flex items-center justify-center flex-shrink-0 text-xs mt-0.5 border border-brand-500/30">
                1
              </div>
              <div class="space-y-1">
                <h5 class="font-bold text-white text-xs">Set a password in Bring!</h5>
                <p class="text-[11px] text-slate-400 leading-normal">
                  In the Bring! mobile app: go to <strong>Profile &gt; Settings &gt; Change Password</strong> (or Set Password).
                </p>
                <p class="text-[11px] text-slate-400 leading-normal">
                  Or on your browser: go to <a href="https://web.getbring.com" target="_blank" rel="noopener noreferrer" class="text-brand-300 underline hover:text-white inline-flex items-center gap-0.5 font-medium">web.getbring.com <ExternalLink class="w-3 h-3 inline" /></a> and click <strong>&ldquo;Forgot Password?&rdquo;</strong> with your Google email.
                </p>
              </div>
            </div>

            <div class="flex items-start gap-3 p-3 rounded-2xl bg-slate-800/80 border border-slate-700/80">
              <div class="w-6 h-6 rounded-lg bg-brand-500/20 text-brand-300 font-bold flex items-center justify-center flex-shrink-0 text-xs mt-0.5 border border-brand-500/30">
                2
              </div>
              <div class="space-y-0.5">
                <h5 class="font-bold text-white text-xs">Enter credentials here</h5>
                <p class="text-[11px] text-slate-400 leading-normal">
                  In {{ currentAppName }} Settings, enter your Google account email (<span class="font-mono text-slate-300">you@gmail.com</span>) and the Bring! password you just created.
                </p>
              </div>
            </div>

            <div class="flex items-start gap-3 p-3 rounded-2xl bg-slate-800/80 border border-slate-700/80">
              <div class="w-6 h-6 rounded-lg bg-brand-500/20 text-brand-300 font-bold flex items-center justify-center flex-shrink-0 text-xs mt-0.5 border border-brand-500/30">
                3
              </div>
              <div class="space-y-0.5">
                <h5 class="font-bold text-white text-xs">Test Connection &amp; Save</h5>
                <p class="text-[11px] text-slate-400 leading-normal">
                  Click <strong>Test Connection</strong> to verify your account, then click <strong>Save Integrations</strong>.
                </p>
              </div>
            </div>
          </div>

          <div class="p-3 rounded-2xl bg-emerald-950/40 border border-emerald-800/40 text-[11px] text-emerald-200 flex items-center gap-2">
            <span class="font-bold text-emerald-400">✓ Safe:</span>
            <span>This will <strong>not</strong> log you out or break your one-click Google Sign-in on your phone.</span>
          </div>
        </div>

        <!-- Footer -->
        <div class="p-3.5 bg-slate-850 bg-slate-950/60 border-t border-slate-800 flex justify-end">
          <button 
            type="button" 
            @click="bringHelpModalOpen = false" 
            class="px-4 py-2 bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs rounded-xl shadow-sm transition-all">
            Got it, thanks
          </button>
        </div>

      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { 
  MapPin, Users, Link2, ShieldCheck, Plus, Edit2, Trash2, UserPlus, Save, X, Info, ExternalLink,
  ChevronDown, ChevronsUpDown, Bell, Clock, Sparkles, FolderTree, Mail, Send,
  Refrigerator, Snowflake, Flame, Archive, LayoutGrid, Box, Home, Warehouse,
  Apple, Milk, Beef, Croissant, Package, Wheat, CupSoda, Cookie, IceCream,
  Sliders, Copy, Check, ChefHat
} from 'lucide-vue-next';
import api from '../services/api';
import { useToast } from '../composables/useToast';
import { appInfo } from '../services/appInfo';

const { showToast } = useToast();
const appVersionDisplay = computed(() => appInfo.getVersionDisplay());

const route = useRoute();
const bringHelpModalOpen = ref(false);
const bringLists = ref([]);

// Collapsible state for each section
const openSections = reactive({
  general: true,
  locations: true,
  notifications: true,
  categories: true,
  email: true,
  users: true,
  auth: true,
  integrations: true,
});

const areAllOpen = computed(() => {
  return Object.values(openSections).every(Boolean);
});

function toggleAllSections() {
  const target = !areAllOpen.value;
  openSections.general = target;
  openSections.locations = target;
  openSections.notifications = target;
  openSections.categories = target;
  openSections.email = target;
  openSections.users = target;
  openSections.auth = target;
  openSections.integrations = target;
}

function toggleSection(secId) {
  openSections[secId] = !openSections[secId];
}

function jumpToSection(secId) {
  openSections[secId] = true;
  nextTick(() => {
    const el = document.getElementById(`section-${secId}`);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  });
}

const sections = [
  { id: 'general', title: 'General & Branding', icon: Sliders },
  { id: 'locations', title: 'Storage Locations', icon: MapPin },
  { id: 'notifications', title: 'Freshness & Expiry', icon: Bell },
  { id: 'categories', title: 'Categories & Shelf-Life', icon: FolderTree },
  { id: 'email', title: 'Email Alerts (SMTP)', icon: Mail },
  { id: 'users', title: 'Household Users', icon: Users },
  { id: 'auth', title: 'SSO & Auth', icon: ShieldCheck },
  { id: 'integrations', title: 'Integrations & APIs', icon: Link2 },
];

const currentOrigin = computed(() => window.location.origin);

watch(() => route.query.section || route.query.tab, (sec) => {
  if (sec && openSections.hasOwnProperty(sec)) {
    jumpToSection(sec);
  }
}, { immediate: true });

const locations = ref([]);
const categories = ref([]);
const users = ref([]);
const isSaving = ref(false);
const isTestingBring = ref(false);
const isTestingEmail = ref(false);
const isSendingDigest = ref(false);
const isTestingLlm = ref(false);

const settings = reactive({
  appName: '',
  bringEmail: '',
  bringPassword: '',
  bringListUuid: '',
  bringAutoSync: false,
  freshFoodReminderDays: 5,
  freshFoodReminderEnabled: true,
  expirationReminderDays: 3,
  expirationReminderEnabled: true,
  mealieBaseUrl: '',
  mealieFrontendUrl: '',
  mealieApiToken: '',
  mealieAutoDeduct: false,
  krogerClientId: '',
  krogerClientSecret: '',
  krogerLocationId: '',
  authentikEnabled: false,
  authentikIssuerUrl: '',
  authentikClientId: '',
  authentikClientSecret: '',
  googleAuthEnabled: false,
  googleClientId: '',
  googleClientSecret: '',
  smtpHost: '',
  smtpPort: 587,
  smtpUsername: '',
  smtpPassword: '',
  smtpEncryption: 'TLS',
  smtpFromAddress: '',
  smtpRecipients: '',
  smtpEnabled: false,
  smtpDailyDigestEnabled: false,
  smtpWeeklyDigestEnabled: false,
  llmApiKey: '',
  llmProvider: 'openai',
  llmModel: '',
  llmBaseUrl: '',
  hasLlmApiKey: false,
  hasMealieConnection: false,
});

const currentAppName = computed(() => {
  return (settings.appName && settings.appName.trim()) ? settings.appName.trim() : 'Skafferi';
});

const defaultModelPlaceholder = computed(() => {
  switch (settings.llmProvider) {
    case 'gemini': return 'Default: gemini-flash-latest (leave blank for latest)';
    case 'openrouter': return 'Default: openrouter/auto (leave blank for latest)';
    case 'ollama': return 'Default: latest (leave blank for latest)';
    case 'openai':
    default: return 'Default: chat-latest (leave blank for latest)';
  }
});

const defaultBaseUrlPlaceholder = computed(() => {
  switch (settings.llmProvider) {
    case 'gemini': return 'https://generativelanguage.googleapis.com/v1beta/openai';
    case 'openrouter': return 'https://openrouter.ai/api/v1';
    case 'ollama': return 'http://host.docker.internal:11434/v1';
    case 'openai':
    default: return 'https://api.openai.com/v1';
  }
});

function formatShelfLifeDescription(days) {
  if (!days || days <= 0) return 'No automatic expiration offset';
  if (days < 7) return `≈ ${days} day${days > 1 ? 's' : ''}`;
  if (days < 30) {
    const weeks = Math.round(days / 7);
    return `≈ ${weeks} week${weeks > 1 ? 's' : ''} (${days} days)`;
  }
  if (days < 365) {
    const months = Math.round(days / 30);
    return `≈ ${months} month${months > 1 ? 's' : ''} (${days} days)`;
  }
  const years = Math.round((days / 365) * 10) / 10;
  return `≈ ${years} year${years > 1 ? 's' : ''} (${days} days)`;
}

async function loadCategories() {
  try {
    const res = await api.getCategories();
    categories.value = res.data;
  } catch (err) {
    console.error('Failed to load categories:', err);
  }
}

async function saveCategoryShelfLife(cat) {
  try {
    await api.updateCategory(cat.id, cat);
    showToast(`Saved default shelf-life for ${cat.name}!`);
  } catch (err) {
    showToast('Failed to save category: ' + err.message, 'error');
  }
}

async function saveAllCategories() {
  try {
    await Promise.all(categories.value.map(c => api.updateCategory(c.id, c)));
    showToast('Saved all category shelf-life presets!');
  } catch (err) {
    showToast('Failed to save categories: ' + err.message, 'error');
  }
}

async function testSmtpEmail() {
  isTestingEmail.value = true;
  try {
    const res = await api.testEmail();
    if (res.data.success) {
      showToast('Test email sent successfully! Check your inbox.');
    } else {
      showToast('Test email failed: ' + (res.data.error || 'Unknown error'), 'error');
    }
  } catch (err) {
    showToast('Failed to send test email: ' + (err.response?.data?.error || err.message), 'error');
  } finally {
    isTestingEmail.value = false;
  }
}

async function triggerManualDigest(type) {
  isSendingDigest.value = true;
  try {
    const res = await api.sendDigest(type);
    showToast(res.data.message || 'Digest sent successfully!');
  } catch (err) {
    showToast('Failed to dispatch digest: ' + (err.response?.data?.error || err.message), 'error');
  } finally {
    isSendingDigest.value = false;
  }
}

const isSendingSample = ref(false);
const hasCopiedActionUrl = ref(false);

function copyRecipeActionUrl() {
  const url = `${currentOrigin.value}/api/recipes/action`;
  navigator.clipboard.writeText(url);
  hasCopiedActionUrl.value = true;
  showToast('Recipe Action URL copied to clipboard!');
  setTimeout(() => {
    hasCopiedActionUrl.value = false;
  }, 2500);
}

async function sendSampleRecipeAction() {
  isSendingSample.value = true;
  try {
    const sampleRecipe = {
      id: 'sample-' + Date.now(),
      name: 'Creamy Tuscan Garlic Chicken',
      slug: 'creamy-tuscan-garlic-chicken',
      recipe_servings: 4,
      recipe_yield: '4 servings',
      recipe_ingredient: [
        {
          food: { name: 'Chicken Breast' },
          unit: { name: 'lbs' },
          quantity: 1.5,
          display: '1.5 lbs Chicken Breast, boneless',
          is_food: true
        },
        {
          food: { name: 'Heavy Cream' },
          unit: { name: 'cup' },
          quantity: 1.0,
          display: '1 cup Heavy Cream',
          is_food: true
        },
        {
          food: { name: 'Garlic' },
          unit: { name: 'cloves' },
          quantity: 3.0,
          display: '3 cloves Garlic, minced',
          is_food: true
        },
        {
          food: { name: 'Baby Spinach' },
          unit: { name: 'cups' },
          quantity: 2.0,
          display: '2 cups Baby Spinach',
          is_food: true
        }
      ]
    };

    const mealieActionPayload = {
      action: {
        id: 'action-sample',
        action_type: 'post',
        title: `Cook with ${currentAppName.value}`,
        url: `${currentOrigin.value}/api/recipes/action`
      },
      content: sampleRecipe,
      recipe_scale: 1.0
    };

    await api.triggerRecipeAction(mealieActionPayload);
    showToast(`Sample recipe "${sampleRecipe.name}" queued! Check Recipes > Cooked Meals Queue.`);
    window.dispatchEvent(new CustomEvent('pending-meals-changed'));
  } catch (err) {
    showToast('Failed to send sample recipe: ' + (err.response?.data?.message || err.message), 'error');
  } finally {
    isSendingSample.value = false;
  }
}

// Location Modal
const locationModalOpen = ref(false);
const locationForm = reactive({
  id: null,
  name: '',
  description: '',
  icon: 'Archive',
  type: 'PANTRY',
});

// User Modal
const userModalOpen = ref(false);
const userForm = reactive({
  id: null,
  username: '',
  displayName: '',
  email: '',
  role: 'ADMIN',
  avatarColor: '#8B5CF6',
});

onMounted(() => {
  loadAll();
});

async function loadAll() {
  await Promise.all([loadLocations(), loadUsers(), loadSettings(), loadCategories()]);
}

async function loadLocations() {
  try {
    const res = await api.getLocations();
    locations.value = res.data;
  } catch (err) {
    console.error('Failed to load locations:', err);
  }
}

async function loadUsers() {
  try {
    const res = await api.getUsers();
    users.value = res.data;
  } catch (err) {
    console.error('Failed to load users:', err);
  }
}

async function loadSettings() {
  try {
    const res = await api.getSettings();
    Object.assign(settings, res.data);
    if (res.data.appName !== undefined) {
      settings.appName = res.data.appName || '';
      appInfo.setAppName(res.data.appName);
    }
    if (['gpt-4o-mini', 'gemini-1.5-flash', 'gemini-2.5-flash', 'google/gemini-2.5-flash', 'llama3.2:latest'].includes(settings.llmModel)) {
      settings.llmModel = '';
    }
    if (settings.bringEmail) {
      fetchBringLists();
    }
  } catch (err) {
    console.error('Failed to load settings:', err);
  }
}

function openAddLocationModal() {
  Object.assign(locationForm, {
    id: null,
    name: '',
    description: '',
    icon: 'Archive',
    type: 'PANTRY',
  });
  locationModalOpen.value = true;
}

function openEditLocationModal(loc) {
  Object.assign(locationForm, {
    id: loc.id,
    name: loc.name,
    description: loc.description || '',
    icon: loc.icon || 'Archive',
    type: loc.type || 'PANTRY',
  });
  locationModalOpen.value = true;
}

async function saveLocation() {
  try {
    const locName = locationForm.name;
    await api.saveLocation(locationForm);
    locationModalOpen.value = false;
    await loadLocations();
    showToast(`Location "${locName}" saved!`);
  } catch (err) {
    showToast('Failed to save location: ' + err.message, 'error');
  }
}

async function deleteLocation(loc) {
  if (confirm(`Are you sure you want to delete "${loc.name}"? Items stored here will be moved to another location.`)) {
    try {
      await api.deleteLocation(loc.id);
      await loadLocations();
      showToast(`Location "${loc.name}" deleted`, 'info');
    } catch (err) {
      showToast('Failed to delete location: ' + err.message, 'error');
    }
  }
}

function openAddUserModal() {
  Object.assign(userForm, {
    id: null,
    username: '',
    displayName: '',
    email: '',
    role: 'ADMIN',
    avatarColor: '#8B5CF6',
  });
  userModalOpen.value = true;
}

function openEditUserModal(u) {
  Object.assign(userForm, {
    id: u.id,
    username: u.username,
    displayName: u.displayName || '',
    email: u.email || '',
    role: u.role || 'ADMIN',
    avatarColor: u.avatarColor || '#8B5CF6',
  });
  userModalOpen.value = true;
}

async function saveUser() {
  try {
    const name = userForm.displayName || userForm.username;
    await api.saveUser(userForm);
    userModalOpen.value = false;
    await loadUsers();
    showToast(`User "${name}" saved!`);
  } catch (err) {
    showToast('Failed to save user: ' + err.message, 'error');
  }
}

async function deleteUser(u) {
  const name = u.displayName || u.username;
  if (confirm(`Are you sure you want to remove ${name}?`)) {
    try {
      await api.deleteUser(u.id);
      await loadUsers();
      showToast(`User "${name}" removed`, 'info');
    } catch (err) {
      showToast('Failed to remove user: ' + err.message, 'error');
    }
  }
}

function onLlmProviderChange() {
  if (!settings.llmModel || ['gpt-4o-mini', 'gemini-1.5-flash', 'gemini-2.5-flash', 'google/gemini-2.5-flash', 'llama3.2:latest'].includes(settings.llmModel)) {
    settings.llmModel = '';
  }

  switch (settings.llmProvider) {
    case 'gemini':
      settings.llmBaseUrl = 'https://generativelanguage.googleapis.com/v1beta/openai';
      break;
    case 'openrouter':
      settings.llmBaseUrl = 'https://openrouter.ai/api/v1';
      break;
    case 'ollama':
      settings.llmBaseUrl = 'http://host.docker.internal:11434/v1';
      break;
    case 'openai':
      settings.llmBaseUrl = 'https://api.openai.com/v1';
      break;
    case 'custom':
    default:
      if (!settings.llmBaseUrl) {
        settings.llmBaseUrl = 'https://api.openai.com/v1';
      }
      break;
  }
}

async function testLlmConnection() {
  isTestingLlm.value = true;
  try {
    const res = await api.testLlm({
      apiKey: settings.llmApiKey,
      provider: settings.llmProvider,
      model: settings.llmModel,
      baseUrl: settings.llmBaseUrl
    });
    if (res.data.success) {
      showToast(res.data.message || 'LLM connected successfully!');
    } else {
      showToast('LLM test failed: ' + (res.data.error || 'Connection error'), 'error');
    }
  } catch (err) {
    const errorMsg = err.response?.data?.message || err.response?.data?.error || err.message;
    showToast('Failed to connect to LLM: ' + errorMsg, 'error');
  } finally {
    isTestingLlm.value = false;
  }
}

async function saveAllSettings() {
  isSaving.value = true;
  try {
    const res = await api.saveSettings(settings);
    Object.assign(settings, res.data);
    if (res.data.appName !== undefined) {
      appInfo.setAppName(res.data.appName);
    }
    window.dispatchEvent(new CustomEvent('settings-updated'));
    showToast('Settings saved successfully!');
  } catch (err) {
    showToast('Failed to save settings: ' + err.message, 'error');
  } finally {
    isSaving.value = false;
  }
}

async function fetchBringLists() {
  try {
    const res = await api.getBringLists();
    if (res.data && res.data.length > 0) {
      bringLists.value = res.data;
    }
  } catch (err) {
    console.warn('Could not load Bring lists:', err);
  }
}

async function testBringConnection() {
  isTestingBring.value = true;
  try {
    const res = await api.testBringAuth(settings.bringEmail, settings.bringPassword);
    if (res.data.success) {
      showToast('Bring! Connection successful!');
      await fetchBringLists();
    } else {
      showToast('Bring! Authentication failed. Check your email and password.', 'error');
    }
  } catch (err) {
    showToast('Error testing Bring: ' + err.message, 'error');
  } finally {
    isTestingBring.value = false;
  }
}

function getIconComponent(iconName) {
  switch (iconName) {
    case 'Refrigerator': return Refrigerator;
    case 'Snowflake': return Snowflake;
    case 'Flame': return Flame;
    case 'Archive': return Archive;
    case 'Box': return Box;
    case 'Home': return Home;
    case 'Warehouse': return Warehouse;
    default: return LayoutGrid;
  }
}

const categoryIconMap = {
  Apple, Milk, Beef, Croissant, Package, Wheat, Sparkles, CupSoda, Cookie, IceCream, Home
};

function getCategoryIconComponent(iconName) {
  return categoryIconMap[iconName] || FolderTree;
}
</script>
