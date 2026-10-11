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
          Find recipes using in-stock pantry items and prioritize expiring foods to eliminate waste.
        </p>
      </div>

      <div class="flex items-center gap-2">
        <button 
          @click="loadRecipes" 
          :disabled="isLoading"
          class="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700 text-xs font-semibold flex items-center gap-1.5 transition-colors">
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': isLoading }" />
          <span>Refresh Matches</span>
        </button>
      </div>
    </div>

    <!-- Mode Selector Tabs -->
    <div class="flex items-center gap-2 border-b border-slate-800 pb-1">
      <button 
        @click="activeTab = 'matches'"
        class="px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all"
        :class="activeTab === 'matches' ? 'bg-brand-500/20 text-brand-300 border border-brand-500/40 shadow-sm' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'">
        <UtensilsCrossed class="w-4 h-4" />
        <span>What Can I Cook Tonight? (Pantry Matches)</span>
      </button>

      <button 
        @click="activeTab = 'generator'"
        class="px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all"
        :class="activeTab === 'generator' ? 'bg-brand-500/20 text-brand-300 border border-brand-500/40 shadow-sm' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'">
        <Sparkles class="w-4 h-4 text-amber-400" />
        <span>Zero-Waste Chef (Leftover Dish Generator)</span>
      </button>
    </div>

    <!-- ========================================================================= -->
    <!-- TAB 1: PANTRY RECIPE MATCHES                                              -->
    <!-- ========================================================================= -->
    <div v-if="activeTab === 'matches'" class="space-y-4">
      
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
                class="text-[11px] font-bold px-2.5 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1">
                <Sparkles class="w-3 h-3 text-amber-400" />
                <span>Rescues Expiring Food</span>
              </span>
            </div>

            <!-- Title & Description -->
            <div>
              <h3 class="font-bold text-white text-lg">{{ recipe.name }}</h3>
              <p class="text-xs text-slate-400 mt-1 line-clamp-2">{{ recipe.description }}</p>
            </div>

            <!-- Expiring Ingredients Highlight -->
            <div v-if="recipe.expiringIngredientsUsed && recipe.expiringIngredientsUsed.length > 0" class="p-2.5 bg-amber-950/30 border border-amber-500/20 rounded-xl space-y-1">
              <span class="text-[10px] font-bold uppercase tracking-wider text-amber-300 block">Uses Expiring Items:</span>
              <div class="flex flex-wrap gap-1.5">
                <span 
                  v-for="(ing, idx) in recipe.expiringIngredientsUsed" 
                  :key="idx"
                  class="px-2 py-0.5 rounded-md bg-amber-500/20 text-amber-200 text-[11px] font-semibold border border-amber-500/30">
                  {{ ing }}
                </span>
              </div>
            </div>

            <!-- Ingredients Status List -->
            <div class="space-y-1.5 pt-1">
              <span class="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">Ingredients</span>
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-1 text-xs">
                <!-- Matched -->
                <div 
                  v-for="(ing, idx) in recipe.matchedIngredients" 
                  :key="'m-' + idx"
                  class="flex items-center gap-1.5 text-emerald-300 bg-emerald-950/20 px-2 py-1 rounded-lg border border-emerald-500/20 truncate">
                  <Check class="w-3 h-3 flex-shrink-0 text-emerald-400" />
                  <span class="truncate">{{ ing }}</span>
                </div>
                <!-- Missing -->
                <div 
                  v-for="(ing, idx) in recipe.missingIngredients" 
                  :key="'miss-' + idx"
                  class="flex items-center gap-1.5 text-slate-400 bg-slate-800/40 px-2 py-1 rounded-lg border border-slate-700/50 truncate">
                  <span class="w-1.5 h-1.5 rounded-full bg-amber-400 flex-shrink-0"></span>
                  <span class="truncate">{{ ing }}</span>
                </div>
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
    <!-- TAB 2: ZERO-WASTE CHEF GENERATOR                                          -->
    <!-- ========================================================================= -->
    <div v-if="activeTab === 'generator'" class="space-y-6">
      <div class="bg-slate-900/90 border border-slate-800 rounded-3xl p-6 shadow-lg space-y-5">
        
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h3 class="text-lg font-bold text-white flex items-center gap-2">
              <Sparkles class="w-5 h-5 text-amber-400" />
              <span>Zero-Waste Meal Generator</span>
            </h3>
            <p class="text-xs text-slate-400 mt-0.5">
              Select expiring items or let the chef craft a culinary creation from your current pantry inventory.
            </p>
          </div>

          <button 
            type="button" 
            @click="generateMeal" 
            :disabled="isGenerating"
            class="px-5 py-2.5 rounded-2xl bg-brand-300 hover:bg-brand-200 disabled:opacity-50 text-slate-950 font-bold text-xs shadow-md flex items-center gap-2 transition-all">
            <Sparkles class="w-4 h-4 text-slate-950" :class="{ 'animate-spin': isGenerating }" />
            <span>{{ isGenerating ? 'Cooking up idea...' : 'Generate Meal Idea' }}</span>
          </button>
        </div>

        <!-- Generated Recipe Output -->
        <div v-if="generatedRecipe" class="p-6 bg-slate-850 bg-slate-950/70 border border-brand-500/40 rounded-2xl space-y-4 animate-in fade-in duration-200">
          <div class="flex items-start justify-between gap-4">
            <div>
              <span class="text-[10px] font-mono font-bold uppercase tracking-wider text-amber-400 px-2 py-0.5 rounded-full bg-amber-500/10 border border-amber-500/30">
                ZERO-WASTE CREATION
              </span>
              <h4 class="text-xl font-bold text-white mt-1.5">{{ generatedRecipe.title }}</h4>
              <p class="text-xs text-slate-300 mt-1">{{ generatedRecipe.description }}</p>
            </div>
            
            <div class="flex items-center gap-2 text-xs font-mono text-slate-400">
              <span v-if="generatedRecipe.prepTime">Prep: {{ generatedRecipe.prepTime }}</span>
              <span>&bull;</span>
              <span v-if="generatedRecipe.cookTime">Cook: {{ generatedRecipe.cookTime }}</span>
            </div>
          </div>

          <!-- Items Utilized -->
          <div v-if="generatedRecipe.utilizedItems && generatedRecipe.utilizedItems.length > 0" class="space-y-1.5 pt-1">
            <span class="text-[11px] font-bold uppercase tracking-wider text-slate-400">Pantry Items Rescued:</span>
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

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { 
  ChefHat, UtensilsCrossed, Sparkles, Check, CheckCircle2, ShoppingCart, RefreshCw 
} from 'lucide-vue-next';
import api from '../services/api';
import { useToast } from '../composables/useToast';

const { showToast } = useToast();

const activeTab = ref('matches');
const isLoading = ref(false);
const isGenerating = ref(false);
const recipes = ref([]);
const generatedRecipe = ref(null);

async function loadRecipes() {
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

async function generateMeal() {
  isGenerating.value = true;
  try {
    const res = await api.generateZeroWasteMeal([]);
    generatedRecipe.value = res.data;
    showToast('Generated zero-waste recipe!');
  } catch (err) {
    showToast('Failed to generate recipe: ' + err.message, 'error');
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

onMounted(() => {
  loadRecipes();
});
</script>
