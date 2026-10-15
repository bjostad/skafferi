<template>
  <div class="space-y-6 pb-24 max-w-6xl mx-auto">
    
    <!-- Header -->
    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-slate-800 pb-5">
      <div>
        <h2 class="text-2xl font-bold text-white tracking-tight flex items-center gap-2.5">
          <ChefHat class="w-6 h-6 text-brand-400" />
          <span>Recipe Matcher &amp; Zero-Waste Chef</span>
        </h2>
        <p class="text-xs text-slate-400 mt-1">
          Find recipes using in-stock pantry items, reconcile cooked meals, and prioritize expiring foods to eliminate waste.
        </p>
      </div>

      <div class="flex items-center gap-2">
        <button 
          @click="toggleSampleMode" 
          class="px-3.5 py-1.5 rounded-xl border text-xs font-semibold flex items-center gap-1.5 transition-colors"
          :class="isSampleMode ? 'bg-amber-500/20 text-amber-300 border-amber-500/40' : 'bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border-slate-700'">
          <Sparkles class="w-3.5 h-3.5 text-amber-400" />
          <span>{{ isSampleMode ? 'Hide Samples' : 'Show Samples' }}</span>
        </button>

        <button 
          @click="refreshCurrentTab" 
          :disabled="isLoading"
          class="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700 text-xs font-semibold flex items-center gap-1.5 transition-colors">
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': isLoading }" />
          <span>Refresh</span>
        </button>
      </div>
    </div>

    <!-- Pending Meals Banner (visible when meals are queued) -->
    <div 
      v-if="pendingMeals.length > 0 && activeTab !== 'pending'" 
      class="p-4 rounded-2xl bg-gradient-to-r from-amber-500/20 via-amber-500/10 to-brand-500/10 border border-amber-500/40 flex items-center justify-between gap-3 shadow-md animate-in fade-in">
      <div class="flex items-center gap-3">
        <div class="w-9 h-9 rounded-xl bg-amber-500/20 flex items-center justify-center text-amber-400 shrink-0">
          <Clock class="w-5 h-5 animate-pulse" />
        </div>
        <div>
          <p class="text-sm font-bold text-white">
            {{ pendingMeals.length }} Cooked Meal{{ pendingMeals.length > 1 ? 's' : '' }} Pending Review
          </p>
          <p class="text-[11px] text-amber-200/80">
            Mealie recipes were cooked or triggered via Recipe Action. Review matched ingredients and deduction quantities before stock is adjusted.
          </p>
        </div>
      </div>
      <button 
        @click="activeTab = 'pending'" 
        class="px-4 py-2 rounded-xl bg-amber-400 hover:bg-amber-300 text-slate-950 font-bold text-xs shadow-sm flex items-center gap-1.5 shrink-0 transition-colors">
        <span>Review Queue</span>
        <ArrowRight class="w-3.5 h-3.5" />
      </button>
    </div>

    <!-- Mode Selector Tabs -->
    <div v-if="hasMealieConfigured || hasLlmConfigured || pendingMeals.length > 0 || recipes.length > 0" class="flex items-center gap-2 border-b border-slate-800 pb-1 overflow-x-auto">
      <button 
        v-if="hasMealieConfigured || recipes.length > 0 || pendingMeals.length > 0"
        @click="activeTab = 'matches'"
        class="px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 whitespace-nowrap transition-all"
        :class="activeTab === 'matches' ? 'bg-brand-500/20 text-brand-300 border border-brand-500/40 shadow-sm' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'">
        <UtensilsCrossed class="w-4 h-4" />
        <span>What Can I Cook Tonight? (Pantry Matches)</span>
      </button>

      <button 
        v-if="hasMealieConfigured || pendingMeals.length > 0"
        @click="activeTab = 'pending'"
        class="px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 whitespace-nowrap transition-all relative"
        :class="activeTab === 'pending' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40 shadow-sm' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'">
        <Clock class="w-4 h-4 text-amber-400" />
        <span>Cooked Meals Queue</span>
        <span 
          v-if="pendingMeals.length > 0" 
          class="ml-1 px-1.5 py-0.5 text-[10px] font-bold rounded-full bg-amber-500 text-slate-950">
          {{ pendingMeals.length }}
        </span>
      </button>

      <button 
        v-if="hasLlmConfigured"
        @click="activeTab = 'generator'"
        class="px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 whitespace-nowrap transition-all"
        :class="activeTab === 'generator' ? 'bg-brand-500/20 text-brand-300 border border-brand-500/40 shadow-sm' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'">
        <Sparkles class="w-4 h-4 text-amber-400" />
        <span>Zero-Waste Chef</span>
      </button>
    </div>

    <!-- Empty State: Neither Mealie nor LLM Configured -->
    <div 
      v-if="!hasMealieConfigured && !hasLlmConfigured && pendingMeals.length === 0 && recipes.length === 0" 
      class="text-center py-16 bg-slate-900/60 border border-slate-800 rounded-3xl p-8 max-w-lg mx-auto space-y-4 shadow-lg my-6">
      <div class="w-16 h-16 rounded-3xl bg-brand-500/10 border border-brand-500/30 flex items-center justify-center text-brand-400 mx-auto">
        <ChefHat class="w-8 h-8" />
      </div>
      <div>
        <h3 class="text-lg font-bold text-white">Recipe Features Not Configured</h3>
        <p class="text-xs text-slate-400 mt-1 max-w-sm mx-auto leading-relaxed">
          Configure a Mealie Recipe Action to POST cooked meals directly to Skafferi for review, connect your Mealie instance to match in-stock pantry items, or add an LLM API key for the AI Zero-Waste Chef.
        </p>
      </div>
      <router-link 
        to="/settings?section=integrations" 
        class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs shadow-md transition-all">
        <Settings class="w-4 h-4" />
        <span>Configure Mealie Recipe Action</span>
      </router-link>
    </div>

    <!-- ========================================================================= -->
    <!-- TAB 1: PANTRY RECIPE MATCHES                                              -->
    <!-- ========================================================================= -->
    <div v-if="activeTab === 'matches'" class="space-y-4">
      
      <!-- Sample Preview Banner -->
      <div 
        v-if="isSampleMode" 
        class="p-3.5 rounded-2xl bg-gradient-to-r from-emerald-500/15 via-brand-500/10 to-slate-900 border border-emerald-500/30 flex items-center justify-between gap-3 text-xs shadow-sm">
        <div class="flex items-center gap-2.5 text-emerald-300">
          <Sparkles class="w-4 h-4 text-amber-400 shrink-0" />
          <span><strong>Sample Preview Mode:</strong> Showing recommended recipe cards with progressive green headers (20% to 100%), &ldquo;Uses Expiring Food&rdquo; badges, and 10-item pagination.</span>
        </div>
        <button @click="toggleSampleMode" class="text-xs text-slate-400 hover:text-white underline shrink-0">
          Exit Preview
        </button>
      </div>

      <!-- Top Pagination Info & Quick Nav (visible when over 10 recipes) -->
      <div 
        v-if="recipes.length > pageSize" 
        class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 bg-slate-900/80 p-3.5 rounded-2xl border border-slate-800 shadow-sm text-xs">
        <div class="flex items-center gap-2 text-slate-300">
          <UtensilsCrossed class="w-4 h-4 text-emerald-400 shrink-0" />
          <span>
            Showing <strong class="text-white">{{ (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, recipes.length) }}</strong> of <strong class="text-white">{{ recipes.length }}</strong> matched recipes ({{ pageSize }} per page)
          </span>
        </div>

        <div class="flex items-center gap-2 self-end sm:self-auto">
          <span class="text-slate-400 font-medium">Page {{ currentPage }} of {{ totalPages }}</span>
          <div class="flex items-center gap-1">
            <button
              @click="goToPage(currentPage - 1)"
              :disabled="currentPage === 1"
              title="Previous page"
              class="p-1.5 rounded-xl border text-xs transition-colors"
              :class="currentPage === 1 ? 'opacity-30 cursor-not-allowed bg-slate-900 border-slate-800 text-slate-600' : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700 hover:text-white'">
              <ChevronLeft class="w-3.5 h-3.5" />
            </button>
            <button
              @click="goToPage(currentPage + 1)"
              :disabled="currentPage === totalPages"
              title="Next page"
              class="p-1.5 rounded-xl border text-xs transition-colors"
              :class="currentPage === totalPages ? 'opacity-30 cursor-not-allowed bg-slate-900 border-slate-800 text-slate-600' : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700 hover:text-white'">
              <ChevronRight class="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      <div ref="recipeGridRef" v-if="recipes.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div 
          v-for="recipe in paginatedRecipes" 
          :key="recipe.id"
          class="bg-slate-900/90 border rounded-3xl overflow-hidden shadow-lg flex flex-col justify-between transition-all hover:border-slate-700"
          :class="recipe.savesExpiringFood ? 'border-amber-500/40 bg-gradient-to-br from-amber-950/10 to-slate-900/90' : 'border-slate-800'">
          
          <!-- Card Header Section (Title, Match %, dynamic green gradient) -->
          <div 
            class="p-4 sm:p-5 transition-colors"
            :style="getHeaderStyle(recipe.matchPercentage)">
            <div class="flex items-start justify-between gap-3">
              <div class="space-y-1 min-w-0">
                <h3 class="font-bold text-lg text-white leading-snug break-words">{{ recipe.name }}</h3>
                <div v-if="recipe.prepTime || recipe.cookTime" class="flex items-center gap-2 text-xs text-slate-300/80">
                  <span v-if="recipe.prepTime">Prep: {{ formatDuration(recipe.prepTime) }}</span>
                  <span v-if="recipe.cookTime">&bull; Cook: {{ formatDuration(recipe.cookTime) }}</span>
                </div>
              </div>

              <div class="flex flex-col items-end gap-1.5 shrink-0">
                <!-- Match % Badge -->
                <div 
                  class="flex items-center gap-1.5 px-3 py-1 rounded-full font-mono font-bold text-xs shadow-sm backdrop-blur-sm"
                  :class="getMatchBadgeClass(recipe.matchPercentage)">
                  <CheckCircle2 v-if="recipe.matchPercentage === 100" class="w-3.5 h-3.5 text-emerald-300" />
                  <span>{{ recipe.matchPercentage }}% Match</span>
                </div>

                <!-- Uses Expiring Food Badge -->
                <span 
                  v-if="recipe.savesExpiringFood" 
                  class="text-[11px] font-bold px-2.5 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/40 flex items-center gap-1 shrink-0">
                  <Sparkles class="w-3 h-3 text-amber-400" />
                  <span>Uses Expiring Food</span>
                </span>
              </div>
            </div>
          </div>

          <!-- Card Content Body -->
          <div class="p-5 flex-1 flex flex-col justify-between space-y-4">
            
            <div class="space-y-3">
              <!-- Description -->
              <p v-if="recipe.description" class="text-xs text-slate-400 line-clamp-2 leading-relaxed">
                {{ recipe.description }}
              </p>

              <!-- Matched Ingredients Chips -->
              <div>
                <label class="text-[11px] font-semibold text-slate-400 block mb-1.5">
                  In-Stock Ingredients ({{ recipe.matchedIngredients?.length || 0 }}):
                </label>
                <div class="flex flex-wrap gap-1.5">
                  <span 
                    v-for="(ing, idx) in recipe.matchedIngredients" 
                    :key="idx"
                    class="text-[11px] px-2 py-0.5 rounded-lg bg-emerald-500/15 text-emerald-300 border border-emerald-500/30 font-medium">
                    ✓ {{ ing }}
                  </span>
                  <span v-if="!recipe.matchedIngredients || recipe.matchedIngredients.length === 0" class="text-xs text-slate-500 italic">
                    None currently in pantry
                  </span>
                </div>
              </div>

              <!-- Missing Ingredients Chips -->
              <div v-if="recipe.missingIngredients && recipe.missingIngredients.length > 0">
                <label class="text-[11px] font-semibold text-slate-400 block mb-1.5">
                  Missing Ingredients ({{ recipe.missingIngredients.length }}):
                </label>
                <div class="flex flex-wrap gap-1.5">
                  <span 
                    v-for="(ing, idx) in recipe.missingIngredients" 
                    :key="idx"
                    class="text-[11px] px-2 py-0.5 rounded-lg bg-slate-800/80 text-slate-400 border border-slate-700 font-medium">
                    + {{ ing }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Card Bottom Section: Prominent Centered Match / Link Button & Actions -->
            <div class="pt-4 border-t border-slate-800/80 flex flex-col items-center gap-3">
              <!-- Prominent Match Button in Middle Bottom -->
              <a 
                v-if="getMealieRecipeUrl(recipe)"
                :href="getMealieRecipeUrl(recipe)" 
                target="_blank" 
                rel="noopener noreferrer"
                class="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs shadow-md shadow-emerald-950/40 flex items-center justify-center gap-2 transition-all transform hover:-translate-y-0.5 active:translate-y-0 text-center">
                <UtensilsCrossed class="w-4 h-4" />
                <span>View Recipe in Mealie</span>
                <ExternalLink class="w-3.5 h-3.5 opacity-80" />
              </a>
              <div 
                v-else
                class="text-[11px] text-slate-500 italic text-center">
                Configure Mealie URL in Settings to open recipe directly
              </div>

              <!-- Secondary Row -->
              <div class="w-full flex items-center justify-between text-xs pt-1">
                <span class="text-[11px] text-slate-500 font-medium">Source: {{ recipe.source || 'Mealie' }}</span>

                <button 
                  v-if="recipe.missingIngredients && recipe.missingIngredients.length > 0"
                  type="button" 
                  @click="addMissingToShopping(recipe.missingIngredients)"
                  class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-brand-300 hover:text-brand-200 border border-slate-700 text-xs font-semibold flex items-center gap-1.5 transition-colors">
                  <ShoppingCart class="w-3.5 h-3.5" />
                  <span>Add {{ recipe.missingIngredients.length }} Missing to List</span>
                </button>
                <span v-else class="text-xs font-bold text-emerald-400 flex items-center gap-1">
                  <CheckCircle2 class="w-4 h-4" />
                  <span>All in stock!</span>
                </span>
              </div>
            </div>

          </div>
        </div>
      </div>

      <!-- Bottom Pagination Bar (when over pageSize recipes) -->
      <div 
        v-if="recipes.length > pageSize" 
        class="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4 border-t border-slate-800">
        <p class="text-xs text-slate-400">
          Showing <span class="font-bold text-white">{{ (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, recipes.length) }}</span> of <span class="font-bold text-white">{{ recipes.length }}</span> matches ({{ pageSize }} per page)
        </p>

        <div class="flex items-center gap-1.5 flex-wrap justify-center">
          <button
            @click="goToPage(currentPage - 1)"
            :disabled="currentPage === 1"
            class="px-3 py-1.5 rounded-xl border text-xs font-semibold flex items-center gap-1 transition-all"
            :class="currentPage === 1 ? 'opacity-40 cursor-not-allowed bg-slate-900 border-slate-800 text-slate-500' : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700 hover:text-white'">
            <ChevronLeft class="w-3.5 h-3.5" />
            <span>Prev</span>
          </button>

          <template v-for="(p, idx) in visiblePages" :key="idx">
            <span v-if="p === '...'" class="px-2 text-xs text-slate-500 font-mono">...</span>
            <button
              v-else
              @click="goToPage(p)"
              class="w-8 h-8 rounded-xl text-xs font-bold transition-all flex items-center justify-center border"
              :class="currentPage === p 
                ? 'bg-emerald-500 text-slate-950 border-emerald-400 shadow-md shadow-emerald-950/40' 
                : 'bg-slate-800/80 hover:bg-slate-700 text-slate-300 border-slate-700 hover:text-white'">
              {{ p }}
            </button>
          </template>

          <button
            @click="goToPage(currentPage + 1)"
            :disabled="currentPage === totalPages"
            class="px-3 py-1.5 rounded-xl border text-xs font-semibold flex items-center gap-1 transition-all"
            :class="currentPage === totalPages ? 'opacity-40 cursor-not-allowed bg-slate-900 border-slate-800 text-slate-500' : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700 hover:text-white'">
            <span>Next</span>
            <ChevronRight class="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      <div v-else class="text-center py-12 bg-slate-900/60 rounded-3xl border border-slate-800 p-8 space-y-3">
        <UtensilsCrossed class="w-12 h-12 text-slate-600 mx-auto mb-2" />
        <h4 class="text-base font-bold text-white">No Recipe Matches Found</h4>
        <p class="text-xs text-slate-400 max-w-md mx-auto">
          Stock additional pantry ingredients or configure your Mealie integration in Settings to sync your personal recipe book!
        </p>
        <div class="pt-2">
          <button 
            @click="toggleSampleMode" 
            class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-brand-500/20 hover:bg-brand-500/30 text-brand-300 border border-brand-500/40 text-xs font-bold transition-all">
            <Sparkles class="w-4 h-4 text-amber-400" />
            <span>Load Recommended Recipe Samples</span>
          </button>
        </div>
      </div>

    </div>

    <!-- ========================================================================= -->
    <!-- TAB 2: COOKED MEALS RECONCILIATION QUEUE                                  -->
    <!-- ========================================================================= -->
    <div v-if="activeTab === 'pending' && (hasMealieConfigured || pendingMeals.length > 0)" class="space-y-5 animate-in fade-in duration-200">
      
      <!-- Queue Header Toolbar -->
      <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 bg-slate-900/60 p-4 rounded-2xl border border-slate-800">
        <div>
          <h3 class="text-sm font-bold text-white flex items-center gap-2">
            <Clock class="w-4 h-4 text-amber-400" />
            <span>Cooked Meals Awaiting Review</span>
            <span class="px-2 py-0.5 text-xs font-bold rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30">
              {{ pendingMeals.length }}
            </span>
          </h3>
          <p class="text-xs text-slate-400 mt-0.5">
            Review ingredient matches and quantities before committing inventory deductions via FIFO.
          </p>
        </div>

        <div v-if="pendingMeals.length > 0" class="flex items-center gap-2 w-full sm:w-auto">
          <button 
            @click="reconcileAll"
            :disabled="isReconciling"
            class="w-full sm:w-auto px-4 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs shadow-sm flex items-center justify-center gap-1.5 transition-all">
            <CheckCircle2 class="w-4 h-4" />
            <span>Reconcile All Meals ({{ pendingMeals.length }})</span>
          </button>
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="pendingMeals.length === 0" class="text-center py-16 bg-slate-900/40 rounded-3xl border border-slate-800 p-8 space-y-3">
        <CheckCircle2 class="w-12 h-12 text-emerald-500/80 mx-auto" />
        <h4 class="text-base font-bold text-white">All Cooked Meals Reconciled!</h4>
        <p class="text-xs text-slate-400 max-w-md mx-auto">
          No pending meals in queue. When you trigger your &ldquo;Cook with Skafferi&rdquo; Recipe Action in Mealie, ingredients will automatically appear here for review before inventory is deducted.
        </p>
        <div class="pt-2">
          <router-link 
            to="/settings?section=integrations" 
            class="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700 text-xs font-semibold transition-colors">
            <Settings class="w-3.5 h-3.5 text-brand-300" />
            <span>Configure Mealie Recipe Action</span>
          </router-link>
        </div>
      </div>

      <!-- Meal Cards List -->
      <div v-else class="space-y-4">
        <div 
          v-for="meal in pendingMeals" 
          :key="meal.id"
          class="bg-slate-900/90 border border-slate-800 rounded-3xl overflow-hidden shadow-lg transition-all">
          
          <!-- Card Header -->
          <div class="p-5 bg-slate-950/60 border-b border-slate-800/80 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <div>
              <div class="flex items-center gap-2.5">
                <span class="w-2.5 h-2.5 rounded-full bg-amber-400 animate-pulse"></span>
                <h4 class="text-base font-bold text-white">{{ meal.recipeName }}</h4>
                <span v-if="meal.servings" class="text-xs px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                  {{ meal.servings }} {{ meal.servings === 1 ? 'serving' : 'servings' }}
                </span>
              </div>
              <p class="text-xs text-slate-400 mt-1 flex items-center gap-2">
                <Clock class="w-3.5 h-3.5 text-slate-500" />
                <span>Cooked {{ formatCookedTime(meal.cookedAt) }}</span>
                <span>&bull;</span>
                <span>{{ meal.items.length }} ingredients</span>
              </p>
            </div>

            <div class="flex items-center gap-2 w-full sm:w-auto justify-end pt-2 sm:pt-0 border-t sm:border-0 border-slate-800">
              <button 
                type="button"
                @click="dismissMeal(meal)"
                title="Dismiss meal without deducting from inventory"
                class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-rose-950/60 text-slate-400 hover:text-rose-300 border border-slate-700 text-xs font-semibold flex items-center gap-1.5 transition-colors">
                <X class="w-3.5 h-3.5" />
                <span>Dismiss</span>
              </button>

              <button 
                type="button"
                @click="reconcileSingleMeal(meal)"
                :disabled="isReconciling"
                class="px-4 py-1.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 text-xs font-bold shadow-sm flex items-center gap-1.5 transition-colors">
                <Check class="w-3.5 h-3.5" />
                <span>Confirm &amp; Deduct</span>
              </button>
            </div>
          </div>

          <!-- Ingredients Table / Rows -->
          <div class="p-5 space-y-3">
            <div class="hidden md:grid grid-cols-12 gap-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider px-3 pb-1 border-b border-slate-800/60">
              <div class="col-span-1">Deduct</div>
              <div class="col-span-4">Recipe Ingredient (Mealie)</div>
              <div class="col-span-4">Matched Pantry Item</div>
              <div class="col-span-2">Deduct Qty</div>
              <div class="col-span-1 text-right">Remember</div>
            </div>

            <div class="space-y-2.5">
              <div 
                v-for="item in meal.items" 
                :key="item.id"
                class="p-3 sm:px-4 rounded-2xl border transition-all flex flex-col md:grid md:grid-cols-12 gap-3 items-start md:items-center"
                :class="item.selected ? 'bg-slate-850/80 bg-slate-900/60 border-slate-800 hover:border-slate-700' : 'bg-slate-950/40 border-slate-900 opacity-60'">
                
                <!-- Checkbox -->
                <div class="flex items-center gap-2 md:col-span-1">
                  <input 
                    type="checkbox" 
                    v-model="item.selected" 
                    class="w-4 h-4 rounded text-emerald-500 bg-slate-900 border-slate-700 focus:ring-emerald-500" />
                  <span class="text-xs text-slate-400 md:hidden font-semibold">Deduct from inventory</span>
                </div>

                <!-- Recipe Ingredient Raw Text & Confidence -->
                <div class="md:col-span-4 min-w-0">
                  <span class="font-bold text-xs text-white block truncate">{{ item.rawIngredientText }}</span>
                  <div class="flex items-center gap-1.5 mt-1 flex-wrap">
                    <span 
                      class="text-[10px] font-bold px-2 py-0.5 rounded-full border inline-block"
                      :class="getConfidenceBadgeClass(item.matchConfidence)">
                      {{ formatConfidence(item.matchConfidence) }}
                    </span>
                    <span v-if="item.originalQuantity" class="text-[10px] text-slate-500">
                      Recipe: {{ item.originalQuantity }} {{ item.originalUnit || '' }}
                    </span>
                  </div>
                </div>

                <!-- Matched Pantry Item Selector -->
                <div class="md:col-span-4 w-full">
                  <select 
                    v-model="item.matchedItemId" 
                    @change="onPantryItemChanged(item)"
                    class="w-full bg-slate-950 border border-slate-700 rounded-xl px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-brand-500">
                    <option :value="null">-- Skip / Unmatched --</option>
                    <option v-for="p in pantryItems" :key="p.id" :value="p.id">
                      {{ p.name }} ({{ p.totalQuantity || 0 }} {{ p.defaultUnit || 'count' }} in stock)
                    </option>
                  </select>
                </div>

                <!-- Deduct Quantity Input -->
                <div class="md:col-span-2 flex items-center gap-1.5 w-full">
                  <div class="flex items-center gap-1 bg-slate-950 border border-slate-700 rounded-xl px-2 py-1 w-full">
                    <input 
                      type="number" 
                      step="any"
                      min="0.01" 
                      v-model.number="item.deductQuantity" 
                      class="w-16 bg-transparent text-xs text-white font-mono focus:outline-none text-right" />
                    <span class="text-[11px] text-slate-400 truncate">{{ item.deductUnit || 'count' }}</span>
                  </div>
                </div>

                <!-- Remember Mapping Checkbox -->
                <div class="md:col-span-1 flex md:justify-end items-center gap-1.5 w-full">
                  <label class="text-[11px] text-slate-400 md:hidden">Remember alias:</label>
                  <input 
                    type="checkbox" 
                    v-model="item.rememberMapping" 
                    title="Remember this pantry match for future Mealie recipes"
                    class="w-4 h-4 rounded text-brand-500 bg-slate-900 border-slate-700 focus:ring-brand-500" />
                </div>

              </div>
            </div>

          </div>

        </div>
      </div>

    </div>

    <!-- ========================================================================= -->
    <!-- TAB 3: ZERO-WASTE CHEF GENERATOR                                          -->
    <!-- ========================================================================= -->
    <div v-if="activeTab === 'generator' && hasLlmConfigured" class="space-y-6">
      
      <div class="bg-gradient-to-br from-brand-950/40 via-slate-900 to-slate-900 border border-brand-500/30 rounded-3xl p-6 shadow-xl space-y-4">
        <div>
          <h3 class="text-lg font-bold text-white flex items-center gap-2">
            <Sparkles class="w-5 h-5 text-amber-400" />
            <span>AI Zero-Waste Chef</span>
          </h3>
          <p class="text-xs text-slate-300 mt-1">
            Transform expiring leftovers and miscellaneous pantry staples into creative meals.
          </p>
        </div>

        <button 
          @click="generateMeal" 
          :disabled="isGenerating"
          class="px-5 py-2.5 rounded-xl bg-brand-400 hover:bg-brand-300 text-slate-950 font-bold text-xs shadow-md flex items-center gap-2 transition-all">
          <Sparkles class="w-4 h-4" :class="{ 'animate-spin': isGenerating }" />
          <span>{{ isGenerating ? 'Composing Recipe...' : 'Generate Meal Idea from Expiring Items' }}</span>
        </button>
      </div>

      <!-- Generated Recipe Display -->
      <div v-if="generatedRecipe" class="bg-slate-900/90 border border-slate-800 rounded-3xl p-6 shadow-lg space-y-5">
        <div>
          <span class="text-xs font-bold uppercase tracking-wider text-amber-400">Chef Suggestion</span>
          <h3 class="text-xl font-bold text-white mt-1">{{ generatedRecipe.title }}</h3>
          <p class="text-xs text-slate-300 mt-1">{{ generatedRecipe.description }}</p>
        </div>

        <!-- Utilized items -->
        <div v-if="generatedRecipe.utilizedItems && generatedRecipe.utilizedItems.length > 0">
          <label class="text-xs font-semibold text-slate-400 block mb-1">Rescued Items:</label>
          <div class="flex flex-wrap gap-2">
            <span 
              v-for="(item, idx) in generatedRecipe.utilizedItems" 
              :key="idx"
              class="px-2.5 py-1 rounded-xl bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 text-xs font-semibold">
              {{ item }}
            </span>
          </div>
        </div>

        <!-- Step-by-Step Instructions -->
        <div class="space-y-2 pt-2 border-t border-slate-800">
          <span class="text-xs font-bold uppercase tracking-wider text-white">Cooking Steps:</span>
          <ol class="space-y-2 text-xs text-slate-300">
            <li 
              v-for="(step, idx) in generatedRecipe.instructions" 
              :key="idx"
              class="flex items-start gap-3 p-2.5 rounded-xl bg-slate-900/60 border border-slate-800/80">
              <span class="w-5 h-5 rounded-lg bg-brand-500/20 text-brand-300 font-bold flex items-center justify-center text-[11px] flex-shrink-0 border border-brand-500/30">
                {{ idx + 1 }}
              </span>
              <span class="leading-relaxed">{{ step }}</span>
            </li>
          </ol>
        </div>

        <!-- Chef Tips -->
        <div v-if="generatedRecipe.chefTips" class="p-3 bg-brand-950/40 border border-brand-500/30 rounded-xl text-xs text-brand-200">
          💡 {{ generatedRecipe.chefTips }}
        </div>
      </div>

      <div v-else class="text-center py-12 bg-slate-950/40 rounded-2xl border border-slate-800 p-8">
        <ChefHat class="w-12 h-12 text-slate-600 mx-auto mb-3" />
        <h4 class="text-sm font-bold text-slate-300">Ready to Create</h4>
        <p class="text-xs text-slate-400 max-w-sm mx-auto mt-1">
          Click <strong>Generate Meal Idea</strong> above to compose a dish tailored to your inventory.
        </p>
      </div>

    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { 
  ChefHat, UtensilsCrossed, Sparkles, Check, CheckCircle2, ShoppingCart, RefreshCw,
  Clock, ArrowRight, X, Settings, ExternalLink, ChevronLeft, ChevronRight
} from 'lucide-vue-next';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const { showToast } = useToast();

const activeTab = ref('matches');
const isLoading = ref(false);
const isGenerating = ref(false);
const isReconciling = ref(false);

const mealieBaseUrl = ref('');
const mealieFrontendUrl = ref('');
const hasMealieConfigured = ref(false);
const hasLlmConfigured = ref(false);
const isSampleMode = ref(false);

const recipeGridRef = ref(null);
const currentPage = ref(1);
const pageSize = 10;

const SAMPLE_RECIPES = [
  {
    id: 'sample-tuscan-chicken',
    name: 'Creamy Tuscan Garlic Chicken',
    slug: 'creamy-tuscan-garlic-chicken',
    description: 'Tender chicken cutlets simmered in a luscious garlic, sun-dried tomato, and spinach cream sauce.',
    prepTime: 'PT15M',
    cookTime: 'PT20M',
    matchPercentage: 100,
    savesExpiringFood: true,
    matchedIngredients: ['Chicken Breast', 'Heavy Cream', 'Baby Spinach', 'Garlic', 'Parmesan Cheese'],
    missingIngredients: [],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/creamy-tuscan-garlic-chicken'
  },
  {
    id: 'sample-salmon-dill',
    name: 'Pan-Seared Salmon with Lemon Dill Butter',
    slug: 'pan-seared-salmon-with-lemon-dill-butter',
    description: 'Crispy skin salmon fillets draped in a velvety golden lemon herb butter sauce.',
    prepTime: 'PT10M',
    cookTime: 'PT12M',
    matchPercentage: 95,
    savesExpiringFood: true,
    matchedIngredients: ['Salmon Fillets', 'Fresh Dill', 'Lemon', 'Butter'],
    missingIngredients: ['Capers'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/pan-seared-salmon-with-lemon-dill-butter'
  },
  {
    id: 'sample-sheet-fajitas',
    name: 'Sizzling Sheet-Pan Chicken Fajitas',
    slug: 'sizzling-sheet-pan-chicken-fajitas',
    description: 'Tender marinated chicken strips roasted with bell peppers, sweet red onions, and warm Mexican spices.',
    prepTime: 'PT15M',
    cookTime: 'PT25M',
    matchPercentage: 90,
    savesExpiringFood: true,
    matchedIngredients: ['Chicken Breast', 'Bell Peppers', 'Red Onion', 'Lime', 'Cumin'],
    missingIngredients: ['Flour Tortillas'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/sizzling-sheet-pan-chicken-fajitas'
  },
  {
    id: 'sample-pesto-pasta',
    name: 'Fresh Garden Basil Pesto Penne',
    slug: 'fresh-garden-basil-pesto-penne',
    description: 'Al dente penne tossed with freshly crushed sweet basil, extra virgin olive oil, and aged parmesan.',
    prepTime: 'PT10M',
    cookTime: 'PT15M',
    matchPercentage: 80,
    savesExpiringFood: true,
    matchedIngredients: ['Penne Pasta', 'Fresh Basil', 'Garlic Cloves', 'Extra Virgin Olive Oil'],
    missingIngredients: ['Pine Nuts'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/fresh-garden-basil-pesto-penne'
  },
  {
    id: 'sample-stuffed-shells',
    name: 'Spinach & Ricotta Stuffed Jumbo Shells',
    slug: 'spinach-and-ricotta-stuffed-jumbo-shells',
    description: 'Jumbo pasta shells loaded with herb ricotta, baby spinach, and bubbling mozzarella in marinara.',
    prepTime: 'PT20M',
    cookTime: 'PT30M',
    matchPercentage: 78,
    savesExpiringFood: true,
    matchedIngredients: ['Jumbo Shells', 'Ricotta Cheese', 'Baby Spinach', 'Mozzarella'],
    missingIngredients: ['Marinara Sauce'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/spinach-and-ricotta-stuffed-jumbo-shells'
  },
  {
    id: 'sample-potato-chowder',
    name: 'Smoky Bacon & Cheddar Potato Chowder',
    slug: 'smoky-bacon-and-cheddar-potato-chowder',
    description: 'Creamy rustic potato chowder garnished with crisp applewood bacon crumbles and chives.',
    prepTime: 'PT15M',
    cookTime: 'PT35M',
    matchPercentage: 75,
    savesExpiringFood: true,
    matchedIngredients: ['Yukon Gold Potatoes', 'Bacon', 'Sharp Cheddar', 'Milk'],
    missingIngredients: ['Green Onions'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/smoky-bacon-and-cheddar-potato-chowder'
  },
  {
    id: 'sample-beef-tacos',
    name: 'Classic Skillet Beef Tacos',
    slug: 'classic-skillet-beef-tacos',
    description: 'Crispy warm taco shells filled with seasoned ground beef, melted sharp cheddar, and fresh pico de gallo.',
    prepTime: 'PT10M',
    cookTime: 'PT20M',
    matchPercentage: 60,
    savesExpiringFood: false,
    matchedIngredients: ['Ground Beef', 'Sharp Cheddar', 'Canned Black Beans'],
    missingIngredients: ['Taco Shells', 'Sour Cream'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/classic-skillet-beef-tacos'
  },
  {
    id: 'sample-quinoa-bowl',
    name: 'Mediterranean Quinoa & Chickpea Power Bowl',
    slug: 'mediterranean-quinoa-and-chickpea-power-bowl',
    description: 'Nutrient-rich fluffy quinoa bowl loaded with spiced chickpeas, cucumbers, kalamata olives, and tahini drizzle.',
    prepTime: 'PT15M',
    cookTime: 'PT15M',
    matchPercentage: 58,
    savesExpiringFood: false,
    matchedIngredients: ['Quinoa', 'Canned Chickpeas', 'Cucumber', 'Olive Oil'],
    missingIngredients: ['Feta Cheese', 'Tahini'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/mediterranean-quinoa-and-chickpea-power-bowl'
  },
  {
    id: 'sample-tofu-bowl',
    name: 'Crispy Sesame Garlic Tofu Rice Bowl',
    slug: 'crispy-sesame-garlic-tofu-rice-bowl',
    description: 'Golden pan-crisped tofu cubes glazed in ginger soy glaze over warm jasmine rice.',
    prepTime: 'PT15M',
    cookTime: 'PT20M',
    matchPercentage: 55,
    savesExpiringFood: false,
    matchedIngredients: ['Extra Firm Tofu', 'Jasmine Rice', 'Soy Sauce', 'Sesame Oil'],
    missingIngredients: ['Green Onions', 'Sesame Seeds'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/crispy-sesame-garlic-tofu-rice-bowl'
  },
  {
    id: 'sample-fried-rice',
    name: 'Classic Vegetable & Egg Fried Rice',
    slug: 'classic-vegetable-and-egg-fried-rice',
    description: 'Wok-tossed chilled rice with scrambled eggs, sweet peas, diced carrots, and savory toasted sesame oil.',
    prepTime: 'PT10M',
    cookTime: 'PT10M',
    matchPercentage: 52,
    savesExpiringFood: false,
    matchedIngredients: ['Cooked Rice', 'Eggs', 'Frozen Peas', 'Soy Sauce'],
    missingIngredients: ['Sesame Seeds'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/classic-vegetable-and-egg-fried-rice'
  },
  {
    id: 'sample-beef-chili',
    name: 'Slow-Cooked Three-Bean Beef Chili',
    slug: 'slow-cooked-three-bean-beef-chili',
    description: 'Thick, hearty chili simmered with ground chuck, kidney beans, crushed tomatoes, and smoked paprika.',
    prepTime: 'PT20M',
    cookTime: 'PT90M',
    matchPercentage: 50,
    savesExpiringFood: false,
    matchedIngredients: ['Ground Chuck', 'Kidney Beans', 'Crushed Tomatoes', 'Onions'],
    missingIngredients: ['Chili Powder', 'Cheddar'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/slow-cooked-three-bean-beef-chili'
  },
  {
    id: 'sample-chicken-noodle',
    name: 'Homestyle Chicken Noodle Soup',
    slug: 'homestyle-chicken-noodle-soup',
    description: 'Soothing golden chicken broth packed with wide egg noodles, tender chicken chunks, and aromatic celery.',
    prepTime: 'PT15M',
    cookTime: 'PT30M',
    matchPercentage: 48,
    savesExpiringFood: false,
    matchedIngredients: ['Chicken Thighs', 'Egg Noodles', 'Carrots', 'Garlic'],
    missingIngredients: ['Celery Stalks', 'Chicken Broth'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/homestyle-chicken-noodle-soup'
  },
  {
    id: 'sample-tomato-bisque',
    name: 'Silky Roasted Tomato & Basil Bisque',
    slug: 'silky-roasted-tomato-and-basil-bisque',
    description: 'Smooth, roasted Roma tomato bisque finished with sweet basil cream and cracked black pepper.',
    prepTime: 'PT15M',
    cookTime: 'PT30M',
    matchPercentage: 45,
    savesExpiringFood: false,
    matchedIngredients: ['Canned Tomatoes', 'Garlic', 'Heavy Cream', 'Olive Oil'],
    missingIngredients: ['Fresh Basil Leaves', 'Vegetable Stock'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/silky-roasted-tomato-and-basil-bisque'
  },
  {
    id: 'sample-butternut-risotto',
    name: 'Creamy Roasted Butternut Squash Risotto',
    slug: 'creamy-roasted-butternut-squash-risotto',
    description: 'Arborio rice slowly infused with rich stock, roasted caramelized squash purée, and nutty parmesan.',
    prepTime: 'PT20M',
    cookTime: 'PT35M',
    matchPercentage: 42,
    savesExpiringFood: false,
    matchedIngredients: ['Arborio Rice', 'Onion', 'Parmesan', 'Butter'],
    missingIngredients: ['Butternut Squash', 'White Wine'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/creamy-roasted-butternut-squash-risotto'
  },
  {
    id: 'sample-garlic-shrimp',
    name: 'Garlic Butter Shrimp Spaghetti',
    slug: 'garlic-butter-shrimp-spaghetti',
    description: 'Juicy shrimp sautéed in white wine garlic butter tossed with al dente spaghetti and parsley.',
    prepTime: 'PT10M',
    cookTime: 'PT15M',
    matchPercentage: 40,
    savesExpiringFood: false,
    matchedIngredients: ['Spaghetti', 'Butter', 'Garlic', 'Olive Oil'],
    missingIngredients: ['Raw Shrimp', 'Dry White Wine'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/garlic-butter-shrimp-spaghetti'
  },
  {
    id: 'sample-minestrone',
    name: 'Rustic Tuscan Vegetable Minestrone',
    slug: 'rustic-tuscan-vegetable-minestrone',
    description: 'Wholesome simmered soup packed with hearty root vegetables, cannellini beans, and Italian herbs.',
    prepTime: 'PT20M',
    cookTime: 'PT40M',
    matchPercentage: 35,
    savesExpiringFood: false,
    matchedIngredients: ['Carrots', 'Diced Tomatoes'],
    missingIngredients: ['Cannellini Beans', 'Celery', 'Zucchini', 'Vegetable Broth'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/rustic-tuscan-vegetable-minestrone'
  },
  {
    id: 'sample-bbq-pork',
    name: 'Slow-Cooked BBQ Pulled Pork Bowls',
    slug: 'slow-cooked-bbq-pulled-pork-bowls',
    description: 'Tender shredded pork shoulder coated in tangy smoky barbecue sauce served over brown rice.',
    prepTime: 'PT15M',
    cookTime: 'PT120M',
    matchPercentage: 30,
    savesExpiringFood: false,
    matchedIngredients: ['Brown Rice', 'Brown Sugar'],
    missingIngredients: ['Pork Shoulder', 'BBQ Sauce', 'Apple Cider Vinegar'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/slow-cooked-bbq-pulled-pork-bowls'
  },
  {
    id: 'sample-thai-curry',
    name: 'Aromatic Thai Red Curry with Sweet Potato',
    slug: 'aromatic-thai-red-curry-with-sweet-potato',
    description: 'Creamy coconut red curry sauce brimming with sweet potato cubes, bell peppers, and fresh cilantro.',
    prepTime: 'PT15M',
    cookTime: 'PT25M',
    matchPercentage: 28,
    savesExpiringFood: false,
    matchedIngredients: ['Sweet Potato', 'Coconut Milk'],
    missingIngredients: ['Red Curry Paste', 'Bamboo Shoots', 'Cilantro'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/aromatic-thai-red-curry-with-sweet-potato'
  },
  {
    id: 'sample-french-onion',
    name: 'Caramelized French Onion Soup',
    slug: 'caramelized-french-onion-soup',
    description: 'Deeply caramelized yellow onions in rich beef thyme broth topped with toasted baguette and Gruyère.',
    prepTime: 'PT25M',
    cookTime: 'PT50M',
    matchPercentage: 25,
    savesExpiringFood: false,
    matchedIngredients: ['Yellow Onions', 'Butter', 'Thyme'],
    missingIngredients: ['Beef Broth', 'Gruyere Cheese', 'French Baguette'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/caramelized-french-onion-soup'
  },
  {
    id: 'sample-lo-mein',
    name: 'Sesame Ginger Vegetable Lo Mein',
    slug: 'sesame-ginger-vegetable-lo-mein',
    description: 'Stir-fried egg noodles with crisp shredded cabbage, carrots, and sweet bell peppers in a savory dark soy sauce.',
    prepTime: 'PT15M',
    cookTime: 'PT10M',
    matchPercentage: 20,
    savesExpiringFood: false,
    matchedIngredients: ['Carrots', 'Soy Sauce'],
    missingIngredients: ['Lo Mein Noodles', 'Napa Cabbage', 'Oyster Sauce'],
    source: 'Mealie',
    recipeUrl: 'http://localhost:9000/recipes/sesame-ginger-vegetable-lo-mein'
  }
];

const recipes = ref([]);
const pendingMeals = ref([]);
const pantryItems = ref([]);
const generatedRecipe = ref(null);

const totalPages = computed(() => Math.ceil(recipes.value.length / pageSize) || 1);

const paginatedRecipes = computed(() => {
  if (recipes.value.length <= pageSize) {
    return recipes.value;
  }
  const start = (currentPage.value - 1) * pageSize;
  return recipes.value.slice(start, start + pageSize);
});

const visiblePages = computed(() => {
  const total = totalPages.value;
  const current = currentPage.value;
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1);
  }
  if (current <= 4) {
    return [1, 2, 3, 4, 5, '...', total];
  } else if (current >= total - 3) {
    return [1, '...', total - 4, total - 3, total - 2, total - 1, total];
  } else {
    return [1, '...', current - 1, current, current + 1, '...', total];
  }
});

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
  if (recipeGridRef.value) {
    recipeGridRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

async function loadSettings() {
  try {
    const res = await api.getSettings();
    const data = res.data || {};
    mealieBaseUrl.value = data.mealieBaseUrl || '';
    mealieFrontendUrl.value = data.mealieFrontendUrl || '';
    hasMealieConfigured.value = !!(data.hasMealieConnection || (data.mealieBaseUrl && data.mealieApiToken));
    hasLlmConfigured.value = !!(data.hasLlmApiKey || (data.llmApiKey && data.llmApiKey.length > 0));

    // Choose default active tab based on what's configured or if sample recipes exist
    if (pendingMeals.value.length > 0) {
      activeTab.value = 'pending';
    } else {
      activeTab.value = 'matches';
    }
  } catch (err) {
    console.error('Failed to load settings in RecipesView:', err);
  }
}

async function loadRecipes(force = false) {
  isLoading.value = true;
  currentPage.value = 1;
  try {
    const res = await api.getRecipeMatches(force ? { refresh: true } : {});
    const data = res.data || [];
    if (data.length > 0) {
      recipes.value = data;
      isSampleMode.value = false;
    } else {
      // Backend returned 0 matches; display sample preview recipes
      recipes.value = SAMPLE_RECIPES;
      isSampleMode.value = true;
    }
  } catch (err) {
    console.warn('Recipe matches returned error, falling back to samples:', err);
    recipes.value = SAMPLE_RECIPES;
    isSampleMode.value = true;
  } finally {
    isLoading.value = false;
  }
}

function toggleSampleMode() {
  currentPage.value = 1;
  if (isSampleMode.value) {
    isSampleMode.value = false;
    recipes.value = [];
  } else {
    isSampleMode.value = true;
    recipes.value = SAMPLE_RECIPES;
  }
}

async function loadPendingMeals() {
  try {
    const res = await api.getPendingMeals();
    pendingMeals.value = (res.data || []).map(meal => ({
      ...meal,
      items: (meal.items || []).map(i => ({
        ...i,
        rememberMapping: i.matchConfidence !== 'EXACT' && i.matchConfidence !== 'MANUAL'
      }))
    }));
    window.dispatchEvent(new CustomEvent('pending-meals-changed', { detail: { count: pendingMeals.value.length } }));
  } catch (err) {
    console.error('Failed to load pending meals:', err);
  }
}

async function loadPantryItems() {
  try {
    const res = await api.getItems();
    pantryItems.value = res.data || [];
  } catch (err) {
    console.error('Failed to load pantry items:', err);
  }
}

function refreshCurrentTab() {
  if (activeTab.value === 'matches') {
    loadRecipes(true);
  } else if (activeTab.value === 'pending') {
    loadPendingMeals();
  }
}

function onPantryItemChanged(item) {
  if (!item.matchedItemId) {
    item.selected = false;
    item.matchConfidence = 'UNMATCHED';
    return;
  }
  const found = pantryItems.value.find(p => p.id === item.matchedItemId);
  if (found) {
    item.selected = true;
    item.deductUnit = found.defaultUnit || 'count';
    item.matchConfidence = 'MANUAL';
    item.rememberMapping = true;
  }
}

async function reconcileSingleMeal(meal) {
  isReconciling.value = true;
  try {
    const payload = {
      items: meal.items.map(i => ({
        pendingItemId: i.id,
        itemId: i.matchedItemId,
        deductQuantity: i.deductQuantity,
        selected: i.selected,
        rememberMapping: !!i.rememberMapping
      }))
    };
    await api.reconcileMeal(meal.id, payload);
    showToast(`Reconciled and deducted ingredients for "${meal.recipeName}"!`);
    await loadPendingMeals();
  } catch (err) {
    showToast('Failed to reconcile meal: ' + err.message, 'error');
  } finally {
    isReconciling.value = false;
  }
}

async function reconcileAll() {
  if (!confirm(`Reconcile all ${pendingMeals.value.length} pending cooked meals? Selected ingredients will be deducted from inventory.`)) {
    return;
  }
  isReconciling.value = true;
  try {
    await api.reconcileAllMeals();
    showToast('All pending meals reconciled and deducted!');
    await loadPendingMeals();
  } catch (err) {
    showToast('Failed to reconcile all meals: ' + err.message, 'error');
  } finally {
    isReconciling.value = false;
  }
}

async function dismissMeal(meal) {
  if (!confirm(`Dismiss "${meal.recipeName}" without deducting anything from your pantry?`)) {
    return;
  }
  try {
    await api.dismissPendingMeal(meal.id);
    showToast(`Dismissed "${meal.recipeName}".`);
    await loadPendingMeals();
  } catch (err) {
    showToast('Failed to dismiss meal: ' + err.message, 'error');
  }
}

function formatConfidence(conf) {
  switch (conf) {
    case 'EXACT': return 'Exact Match';
    case 'SUBSTRING': return 'Substring Match';
    case 'TOKEN': return 'Token Match';
    case 'FUZZY': return 'Fuzzy Match';
    case 'MANUAL': return 'Saved Alias';
    default: return 'Unmatched';
  }
}

function getConfidenceBadgeClass(conf) {
  switch (conf) {
    case 'EXACT':
    case 'SUBSTRING':
      return 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30';
    case 'TOKEN':
      return 'bg-blue-500/15 text-blue-300 border-blue-500/30';
    case 'FUZZY':
      return 'bg-amber-500/15 text-amber-300 border-amber-500/30';
    case 'MANUAL':
      return 'bg-purple-500/15 text-purple-300 border-purple-500/30';
    default:
      return 'bg-rose-500/15 text-rose-300 border-rose-500/30';
  }
}

function formatCookedTime(dateStr) {
  if (!dateStr) return 'recently';
  try {
    const date = new Date(dateStr);
    return date.toLocaleString(undefined, {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit'
    });
  } catch (e) {
    return dateStr;
  }
}

async function generateMeal() {
  isGenerating.value = true;
  try {
    const res = await api.generateZeroWasteMeal([]);
    generatedRecipe.value = res.data;
    showToast('Chef generated a zero-waste recipe!');
  } catch (err) {
    const errorMsg = err.response?.data?.message || err.response?.data?.error || err.message;
    showToast('Failed to generate recipe: ' + errorMsg, 'error');
  } finally {
    isGenerating.value = false;
  }
}

async function addMissingToShopping(items) {
  try {
    const res = await api.addMissingToShoppingList(items);
    showToast(`Added ${res.data.addedCount || items.length} missing items to shopping list!`);
  } catch (err) {
    showToast('Failed to add missing ingredients: ' + err.message, 'error');
  }
}

function getMealieRecipeUrl(recipe) {
  if (!recipe) return '';
  const publicBase = (mealieFrontendUrl.value || mealieBaseUrl.value || '').replace(/\/+$/, '');
  const identifier = recipe.slug || recipe.id;
  if (publicBase && identifier) {
    return `${publicBase}/recipes/${identifier}`;
  }
  if (recipe.recipeUrl && typeof recipe.recipeUrl === 'string' && recipe.recipeUrl.startsWith('http')) {
    return recipe.recipeUrl;
  }
  return publicBase;
}

function getHeaderStyle(matchPercentage = 0) {
  const pct = Math.max(0, Math.min(100, Number(matchPercentage) || 0));
  const factor = pct / 100;
  
  // Interpolate emerald tint:
  // At 0%: subtle slate overlay (alpha ~0.04)
  // At 100%: rich, glowing emerald green (alpha ~0.42)
  const startAlpha = (0.04 + factor * 0.38).toFixed(3);
  const endAlpha = (0.02 + factor * 0.26).toFixed(3);
  const borderAlpha = (0.12 + factor * 0.48).toFixed(3);

  return {
    background: `linear-gradient(135deg, rgba(16, 185, 129, ${startAlpha}) 0%, rgba(5, 150, 105, ${endAlpha}) 100%)`,
    borderBottom: `1px solid rgba(16, 185, 129, ${borderAlpha})`
  };
}

function getMatchBadgeClass(matchPercentage = 0) {
  const pct = Number(matchPercentage) || 0;
  if (pct >= 90) {
    return 'bg-emerald-500/35 text-emerald-100 border border-emerald-400/60 shadow-[0_0_12px_rgba(16,185,129,0.3)]';
  } else if (pct >= 70) {
    return 'bg-emerald-500/25 text-emerald-200 border border-emerald-500/50';
  } else if (pct >= 40) {
    return 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/35';
  } else {
    return 'bg-slate-800/80 text-slate-300 border border-slate-700/80';
  }
}

function formatDuration(val) {
  if (!val) return '';
  if (typeof val !== 'string') return String(val);
  const match = val.match(/PT(?:(\d+)H)?(?:(\d+)M)?/i);
  if (match) {
    const hours = match[1] ? `${match[1]} hr` : '';
    const mins = match[2] ? `${match[2]} min` : '';
    return [hours, mins].filter(Boolean).join(' ') || val;
  }
  return val;
}

onMounted(async () => {
  await Promise.all([loadPendingMeals(), loadPantryItems()]);
  await loadSettings();
  loadRecipes();
});
</script>
