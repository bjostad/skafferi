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
            Mealie recipes were cooked. Review matched ingredients and deduction quantities before stock is adjusted.
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
    <div v-if="hasMealieConfigured || hasLlmConfigured || pendingMeals.length > 0" class="flex items-center gap-2 border-b border-slate-800 pb-1 overflow-x-auto">
      <button 
        v-if="hasMealieConfigured"
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
      v-if="!hasMealieConfigured && !hasLlmConfigured && pendingMeals.length === 0" 
      class="text-center py-16 bg-slate-900/60 border border-slate-800 rounded-3xl p-8 max-w-lg mx-auto space-y-4 shadow-lg my-6">
      <div class="w-16 h-16 rounded-3xl bg-brand-500/10 border border-brand-500/30 flex items-center justify-center text-brand-400 mx-auto">
        <ChefHat class="w-8 h-8" />
      </div>
      <div>
        <h3 class="text-lg font-bold text-white">Recipe Features Not Configured</h3>
        <p class="text-xs text-slate-400 mt-1 max-w-sm mx-auto leading-relaxed">
          Connect your Mealie recipe instance to match recipes with in-stock pantry items, or add an LLM API key to generate creative zero-waste meals from expiring ingredients.
        </p>
      </div>
      <router-link 
        to="/settings?section=integrations" 
        class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-brand-300 hover:bg-brand-200 text-slate-950 font-bold text-xs shadow-md transition-all">
        <Settings class="w-4 h-4" />
        <span>Configure in Settings</span>
      </router-link>
    </div>

    <!-- ========================================================================= -->
    <!-- TAB 1: PANTRY RECIPE MATCHES                                              -->
    <!-- ========================================================================= -->
    <div v-if="activeTab === 'matches' && hasMealieConfigured" class="space-y-4">
      
      <div v-if="recipes.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div 
          v-for="recipe in recipes" 
          :key="recipe.id"
          class="bg-slate-900/90 border rounded-3xl p-5 shadow-lg flex flex-col justify-between transition-all"
          :class="recipe.savesExpiringFood ? 'border-amber-500/40 bg-gradient-to-br from-amber-950/10 to-slate-900/90' : 'border-slate-800'">
          
          <div class="space-y-3">
            
            <!-- Badges Header -->
            <div class="flex items-center justify-between gap-2 flex-wrap">
              <span 
                class="text-xs font-bold px-2.5 py-0.5 rounded-full font-mono flex items-center gap-1.5"
                :class="recipe.matchPercentage >= 80 ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' : 'bg-slate-800 text-slate-300 border border-slate-700'">
                <CheckCircle2 v-if="recipe.matchPercentage === 100" class="w-3.5 h-3.5 text-emerald-400" />
                <span>{{ recipe.matchPercentage }}% Pantry Match</span>
              </span>

              <span 
                v-if="recipe.savesExpiringFood" 
                class="text-xs font-bold px-2.5 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1">
                <Sparkles class="w-3.5 h-3.5" />
                <span>Rescues Expiring Food</span>
              </span>
            </div>

            <!-- Title & Description -->
            <div>
              <h3 class="font-bold text-lg text-white">{{ recipe.name }}</h3>
              <p class="text-xs text-slate-400 mt-1 line-clamp-2">{{ recipe.description }}</p>
            </div>

            <!-- Quick Meta -->
            <div class="flex items-center gap-3 text-xs text-slate-400 pt-1">
              <span v-if="recipe.prepTime">Prep: {{ recipe.prepTime }}</span>
              <span v-if="recipe.cookTime">&bull; Cook: {{ recipe.cookTime }}</span>
            </div>

            <!-- Matched Ingredients Chips -->
            <div>
              <label class="text-[11px] font-semibold text-slate-400 block mb-1.5">In-Stock Ingredients:</label>
              <div class="flex flex-wrap gap-1.5">
                <span 
                  v-for="(ing, idx) in recipe.matchedIngredients" 
                  :key="idx"
                  class="text-[11px] px-2 py-0.5 rounded-lg bg-emerald-500/15 text-emerald-300 border border-emerald-500/30 font-medium">
                  ✓ {{ ing }}
                </span>
                <span v-if="recipe.matchedIngredients.length === 0" class="text-xs text-slate-500">
                  None currently in pantry
                </span>
              </div>
            </div>

            <!-- Missing Ingredients Chips -->
            <div v-if="recipe.missingIngredients && recipe.missingIngredients.length > 0">
              <label class="text-[11px] font-semibold text-slate-400 block mb-1.5">Missing Ingredients:</label>
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

          <!-- Card Actions -->
          <div class="flex items-center justify-between pt-4 mt-4 border-t border-slate-800">
            <span class="text-[11px] text-slate-500 font-medium">Source: {{ recipe.source || 'Pantry Chef' }}</span>

            <button 
              v-if="recipe.missingIngredients && recipe.missingIngredients.length > 0"
              type="button" 
              @click="addMissingToShopping(recipe.missingIngredients)"
              class="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-750 text-brand-300 hover:text-brand-200 border border-slate-700 text-xs font-semibold flex items-center gap-1.5 transition-colors">
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

      <div v-else class="text-center py-12 bg-slate-900/60 rounded-3xl border border-slate-800 p-8">
        <UtensilsCrossed class="w-12 h-12 text-slate-600 mx-auto mb-3" />
        <h4 class="text-base font-bold text-white">No Recipe Matches Found</h4>
        <p class="text-xs text-slate-400 max-w-md mx-auto mt-1">
          Stock additional pantry ingredients or configure your Mealie integration in Settings to sync your personal recipe book!
        </p>
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
          No pending meals in queue. When Mealie triggers a &ldquo;Meal Cooked&rdquo; webhook, ingredients will automatically appear here for review before inventory is deducted.
        </p>
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
import { ref, onMounted } from 'vue';
import { 
  ChefHat, UtensilsCrossed, Sparkles, Check, CheckCircle2, ShoppingCart, RefreshCw,
  Clock, ArrowRight, X, Settings 
} from 'lucide-vue-next';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const { showToast } = useToast();

const activeTab = ref('matches');
const isLoading = ref(false);
const isGenerating = ref(false);
const isReconciling = ref(false);

const hasMealieConfigured = ref(false);
const hasLlmConfigured = ref(false);

const recipes = ref([]);
const pendingMeals = ref([]);
const pantryItems = ref([]);
const generatedRecipe = ref(null);

async function loadSettings() {
  try {
    const res = await api.getSettings();
    const data = res.data || {};
    hasMealieConfigured.value = !!(data.hasMealieConnection || (data.mealieBaseUrl && data.mealieApiToken));
    hasLlmConfigured.value = !!(data.hasLlmApiKey || (data.llmApiKey && data.llmApiKey.length > 0));

    // Choose default active tab based on what's configured
    if (pendingMeals.value.length > 0) {
      activeTab.value = 'pending';
    } else if (hasMealieConfigured.value) {
      activeTab.value = 'matches';
    } else if (hasLlmConfigured.value) {
      activeTab.value = 'generator';
    }
  } catch (err) {
    console.error('Failed to load settings in RecipesView:', err);
  }
}

async function loadRecipes() {
  if (!hasMealieConfigured.value) {
    recipes.value = [];
    return;
  }
  isLoading.value = true;
  try {
    const res = await api.getRecipeMatches();
    recipes.value = res.data || [];
  } catch (err) {
    showToast('Failed to load matching recipes: ' + err.message, 'error');
  } finally {
    isLoading.value = false;
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
  if (activeTab.value === 'matches' && hasMealieConfigured.value) {
    loadRecipes();
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

onMounted(async () => {
  await Promise.all([loadPendingMeals(), loadPantryItems()]);
  await loadSettings();
  if (hasMealieConfigured.value) {
    loadRecipes();
  }
});
</script>
