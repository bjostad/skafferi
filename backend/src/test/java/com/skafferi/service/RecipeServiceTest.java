package com.skafferi.service;

import com.skafferi.domain.CachedRecipe;
import com.skafferi.dto.NotificationSummaryDto;
import com.skafferi.dto.PantryItemSummaryDto;
import com.skafferi.dto.SettingsDto;
import com.skafferi.repository.CachedRecipeRepository;
import com.skafferi.repository.ItemRepository;
import com.skafferi.repository.PendingCookedMealRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RecipeServiceTest {

    private SettingsService settingsService;
    private InventoryService inventoryService;
    private BringSyncService bringSyncService;
    private ItemRepository itemRepository;
    private PendingCookedMealRepository pendingMealRepository;
    private CachedRecipeRepository cachedRecipeRepository;

    @BeforeEach
    void setUp() {
        settingsService = Mockito.mock(SettingsService.class);
        inventoryService = Mockito.mock(InventoryService.class);
        bringSyncService = Mockito.mock(BringSyncService.class);
        itemRepository = Mockito.mock(ItemRepository.class);
        pendingMealRepository = Mockito.mock(PendingCookedMealRepository.class);
        cachedRecipeRepository = Mockito.mock(CachedRecipeRepository.class);
    }

    private PantryItemSummaryDto createMockPantryItem(String name, double qty) {
        PantryItemSummaryDto item = Mockito.mock(PantryItemSummaryDto.class);
        when(item.name()).thenReturn(name);
        when(item.totalQuantity()).thenReturn(qty);
        when(item.isOutOfStock()).thenReturn(qty <= 0);
        return item;
    }

    @Test
    void testLoadsPreCachedRecipesAndMatchesPantry() {
        // Prepare cached recipe in SQLite
        CachedRecipe cr = new CachedRecipe(
                "rec-1", "chicken-soup", "Chicken Soup", "Hearty soup",
                "Mealie", "https://mealie.local/recipes/chicken-soup", "10m", "30m",
                "2026-10-15T10:00:00Z", "[\"Chicken Breast\", \"Carrots\", \"Celery\"]", System.currentTimeMillis()
        );
        when(cachedRecipeRepository.findAll()).thenReturn(List.of(cr));

        RecipeService recipeService = new RecipeService(
                settingsService, inventoryService, bringSyncService, itemRepository, pendingMealRepository, cachedRecipeRepository
        );

        // Pantry setup
        SettingsDto settingsDto = Mockito.mock(SettingsDto.class);
        when(settingsService.getRawSettings()).thenReturn(settingsDto);
        when(settingsService.hasMealieConnection()).thenReturn(false);

        PantryItemSummaryDto chicken = createMockPantryItem("Chicken Breast", 2.0);
        PantryItemSummaryDto carrots = createMockPantryItem("Carrots", 1.0);

        when(inventoryService.getAllPantrySummaries(null, null, null, null, null, null, "NAME"))
                .thenReturn(List.of(chicken, carrots));

        // Chicken is expiring soon
        NotificationSummaryDto notifs = new NotificationSummaryDto(
                1, 0, 1, 0, 0,
                Collections.emptyList(),
                List.of(chicken),
                Collections.emptyList(),
                Collections.emptyList()
        );
        when(inventoryService.getNotificationSummary()).thenReturn(notifs);

        List<Map<String, Object>> matches = recipeService.getMatchingRecipes(false);

        assertNotNull(matches);
        assertEquals(1, matches.size());
        Map<String, Object> match = matches.get(0);
        assertEquals("Chicken Soup", match.get("name"));
        assertEquals(true, match.get("savesExpiringFood"));
        assertEquals(67, match.get("matchPercentage")); // 2 of 3 ingredients (Chicken, Carrots)

        @SuppressWarnings("unchecked")
        List<String> matchedIngs = (List<String>) match.get("matchedIngredients");
        assertTrue(matchedIngs.contains("Chicken Breast"));
        assertTrue(matchedIngs.contains("Carrots"));

        @SuppressWarnings("unchecked")
        List<String> missingIngs = (List<String>) match.get("missingIngredients");
        assertTrue(missingIngs.contains("Celery"));
    }

    @Test
    void testCapsMatchingRecipesAt100WhilePreservingAllExpiringRecipes() {
        List<CachedRecipe> largeList = new ArrayList<>();
        // Create 150 recipes: 20 save expiring food, 130 do not
        for (int i = 1; i <= 150; i++) {
            String ingJson = (i <= 20)
                    ? "[\"Expiring Milk\", \"Flour\"]"
                    : "[\"Flour\", \"Sugar\"]";
            largeList.add(new CachedRecipe(
                    "rec-" + i, "recipe-" + i, "Recipe #" + i, "Description " + i,
                    "Mealie", null, null, null,
                    "2026-10-15T10:00:00Z", ingJson, System.currentTimeMillis()
            ));
        }
        when(cachedRecipeRepository.findAll()).thenReturn(largeList);

        RecipeService recipeService = new RecipeService(
                settingsService, inventoryService, bringSyncService, itemRepository, pendingMealRepository, cachedRecipeRepository
        );

        SettingsDto settingsDto = Mockito.mock(SettingsDto.class);
        when(settingsService.getRawSettings()).thenReturn(settingsDto);
        when(settingsService.hasMealieConnection()).thenReturn(false);

        PantryItemSummaryDto milk = createMockPantryItem("Expiring Milk", 1.0);
        PantryItemSummaryDto flour = createMockPantryItem("Flour", 5.0);

        when(inventoryService.getAllPantrySummaries(null, null, null, null, null, null, "NAME"))
                .thenReturn(List.of(milk, flour));

        NotificationSummaryDto notifs = new NotificationSummaryDto(
                1, 0, 1, 0, 0,
                Collections.emptyList(),
                List.of(milk),
                Collections.emptyList(),
                Collections.emptyList()
        );
        when(inventoryService.getNotificationSummary()).thenReturn(notifs);

        List<Map<String, Object>> matches = recipeService.getMatchingRecipes(false);

        assertNotNull(matches);
        // Verify capped at 100
        assertEquals(100, matches.size());

        // Verify ALL 20 recipes saving expiring food are preserved
        long expiringCount = matches.stream().filter(r -> Boolean.TRUE.equals(r.get("savesExpiringFood"))).count();
        assertEquals(20, expiringCount);
    }

    @Test
    void testRecipeUrlPrefersMealieFrontendUrlOverBaseUrl() {
        CachedRecipe cr = new CachedRecipe(
                "rec-public", "tacos-de-asada", "Carne Asada Tacos", "Authentic tacos",
                "Mealie", "http://mealie:9000/recipes/tacos-de-asada", "15m", "15m",
                "2026-10-15T10:00:00Z", "[\"Flank Steak\", \"Tortillas\"]", System.currentTimeMillis()
        );
        when(cachedRecipeRepository.findAll()).thenReturn(List.of(cr));

        RecipeService recipeService = new RecipeService(
                settingsService, inventoryService, bringSyncService, itemRepository, pendingMealRepository, cachedRecipeRepository
        );

        SettingsDto settingsDto = Mockito.mock(SettingsDto.class);
        when(settingsDto.mealieBaseUrl()).thenReturn("http://mealie:9000");
        when(settingsDto.mealieFrontendUrl()).thenReturn("https://mealie.ballardplex.com");
        when(settingsService.getRawSettings()).thenReturn(settingsDto);
        when(settingsService.hasMealieConnection()).thenReturn(false);

        PantryItemSummaryDto steak = createMockPantryItem("Flank Steak", 1.0);
        when(inventoryService.getAllPantrySummaries(null, null, null, null, null, null, "NAME"))
                .thenReturn(List.of(steak));

        NotificationSummaryDto notifs = new NotificationSummaryDto(
                0, 0, 0, 0, 0,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        when(inventoryService.getNotificationSummary()).thenReturn(notifs);

        List<Map<String, Object>> matches = recipeService.getMatchingRecipes(false);

        assertNotNull(matches);
        assertEquals(1, matches.size());
        Map<String, Object> match = matches.get(0);
        // Ensure the public frontend URL is used for recipeUrl instead of the internal docker URL
        assertEquals("https://mealie.ballardplex.com/recipes/tacos-de-asada", match.get("recipeUrl"));
    }
}
