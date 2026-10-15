package com.skafferi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.skafferi.domain.Item;
import com.skafferi.domain.PendingCookedMeal;
import com.skafferi.domain.PendingMealItem;
import com.skafferi.repository.ExternalMappingRepository;
import com.skafferi.repository.InventoryBatchRepository;
import com.skafferi.repository.ItemRepository;
import com.skafferi.repository.PendingCookedMealRepository;
import com.skafferi.repository.PendingMealItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PendingMealServiceTest {

    private PendingCookedMealRepository mealRepository;
    private PendingMealItemRepository mealItemRepository;
    private ItemRepository itemRepository;
    private InventoryBatchRepository batchRepository;
    private ExternalMappingRepository mappingRepository;
    private InventoryService inventoryService;
    private SettingsService settingsService;
    private PendingMealService pendingMealService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mealRepository = mock(PendingCookedMealRepository.class);
        mealItemRepository = mock(PendingMealItemRepository.class);
        itemRepository = mock(ItemRepository.class);
        batchRepository = mock(InventoryBatchRepository.class);
        mappingRepository = mock(ExternalMappingRepository.class);
        inventoryService = mock(InventoryService.class);
        settingsService = mock(SettingsService.class);
        objectMapper = new ObjectMapper();

        when(settingsService.isMealieAutoDeduct()).thenReturn(false);

        when(mealRepository.save(any(PendingCookedMeal.class))).thenAnswer(invocation -> {
            PendingCookedMeal meal = invocation.getArgument(0);
            if (meal.getId() == null) {
                meal.setId("meal-uuid-1");
            }
            return meal;
        });

        pendingMealService = new PendingMealService(
                mealRepository,
                mealItemRepository,
                itemRepository,
                batchRepository,
                mappingRepository,
                inventoryService,
                settingsService
        );
    }

    @Test
    void testQueueMealFromMealieRecipeJson_FullStructure() throws Exception {
        Item chickenItem = new Item();
        chickenItem.setId("item-chicken");
        chickenItem.setName("Chicken Breast");
        chickenItem.setDefaultUnit("lbs");

        Item creamItem = new Item();
        creamItem.setId("item-cream");
        creamItem.setName("Heavy Whipping Cream");
        creamItem.setDefaultUnit("cup");

        when(itemRepository.findAll()).thenReturn(List.of(chickenItem, creamItem));

        String json = """
        {
            "id": "recipe-456",
            "name": "Creamy Tuscan Garlic Chicken",
            "slug": "creamy-tuscan-garlic-chicken",
            "recipeYield": "4 servings",
            "recipeIngredient": [
                {
                    "food": { "name": "Chicken Breast", "id": "food-1" },
                    "unit": { "name": "lbs", "id": "unit-1" },
                    "quantity": 1.5,
                    "display": "1.5 lbs Chicken Breast, boneless",
                    "isFood": true
                },
                {
                    "food": { "name": "Heavy Cream", "id": "food-2" },
                    "unit": { "name": "cup", "id": "unit-2" },
                    "quantity": 1.0,
                    "display": "1 cup Heavy Cream",
                    "isFood": true
                }
            ]
        }
        """;

        JsonNode root = objectMapper.readTree(json);
        PendingCookedMeal meal = pendingMealService.queueMealFromRecipeJson(root);

        assertNotNull(meal);
        assertEquals("Creamy Tuscan Garlic Chicken", meal.getRecipeName());
        assertEquals("recipe-456", meal.getRecipeId());
        assertEquals(4.0, meal.getServings());
        assertEquals("PENDING", meal.getStatus());
        assertEquals(2, meal.getItems().size());

        PendingMealItem item1 = meal.getItems().get(0);
        assertEquals("Chicken Breast", item1.getFoodName());
        assertEquals(1.5, item1.getOriginalQuantity());
        assertEquals("lbs", item1.getOriginalUnit());
        assertEquals("item-chicken", item1.getMatchedItem().getId());
        assertTrue(item1.isSelected());

        PendingMealItem item2 = meal.getItems().get(1);
        assertEquals("Heavy Cream", item2.getFoodName());
        assertEquals(1.0, item2.getOriginalQuantity());
        assertEquals("cup", item2.getOriginalUnit());
        assertEquals("item-cream", item2.getMatchedItem().getId());
        assertTrue(item2.isSelected());
    }

    @Test
    void testQueueMealFromMealieRecipeJson_SkipsSectionHeaders() throws Exception {
        Item pasta = new Item();
        pasta.setId("item-pasta");
        pasta.setName("Penne Pasta");
        pasta.setDefaultUnit("boxes");

        when(itemRepository.findAll()).thenReturn(List.of(pasta));

        String json = """
        {
            "name": "Baked Penne",
            "recipeYield": "6",
            "recipeIngredient": [
                {
                    "title": "For the Pasta",
                    "isFood": false,
                    "display": "For the Pasta"
                },
                {
                    "food": "Penne Pasta",
                    "quantity": 1,
                    "unit": "box",
                    "display": "1 box Penne Pasta",
                    "isFood": true
                }
            ]
        }
        """;

        JsonNode root = objectMapper.readTree(json);
        PendingCookedMeal meal = pendingMealService.queueMealFromRecipeJson(root);

        assertEquals("Baked Penne", meal.getRecipeName());
        assertEquals(6.0, meal.getServings());
        // Section header should be excluded
        assertEquals(1, meal.getItems().size());
        assertEquals("Penne Pasta", meal.getItems().get(0).getFoodName());
    }

    @Test
    void testQueueMealFromMealieRecipeJson_TextualIngredients() throws Exception {
        Item flour = new Item();
        flour.setId("item-flour");
        flour.setName("All-Purpose Flour");
        flour.setDefaultUnit("cups");

        when(itemRepository.findAll()).thenReturn(List.of(flour));

        String json = """
        {
            "name": "Pancakes",
            "yield": "2 servings",
            "recipeIngredient": [
                "2 cups All-Purpose Flour",
                "1 pinch salt"
            ]
        }
        """;

        JsonNode root = objectMapper.readTree(json);
        PendingCookedMeal meal = pendingMealService.queueMealFromRecipeJson(root);

        assertEquals("Pancakes", meal.getRecipeName());
        assertEquals(2.0, meal.getServings());
        assertEquals(2, meal.getItems().size());

        PendingMealItem item1 = meal.getItems().get(0);
        assertEquals(2.0, item1.getOriginalQuantity());
        assertEquals("cups", item1.getOriginalUnit());
        assertEquals("item-flour", item1.getMatchedItem().getId());
    }

    @Test
    void testQueueMealFromMealieRecipeJson_NestedRecipeObject() throws Exception {
        Item egg = new Item();
        egg.setId("item-egg");
        egg.setName("Eggs");
        egg.setDefaultUnit("count");

        when(itemRepository.findAll()).thenReturn(List.of(egg));

        String json = """
        {
            "recipe": {
                "name": "Omelette",
                "servings": 1,
                "recipeIngredient": [
                    {
                        "food": "Eggs",
                        "quantity": 3
                    }
                ]
            }
        }
        """;

        JsonNode root = objectMapper.readTree(json);
        PendingCookedMeal meal = pendingMealService.queueMealFromRecipeJson(root);

        assertEquals("Omelette", meal.getRecipeName());
        assertEquals(1.0, meal.getServings());
        assertEquals(1, meal.getItems().size());
        assertEquals("Eggs", meal.getItems().get(0).getFoodName());
        assertEquals(3.0, meal.getItems().get(0).getOriginalQuantity());
    }

    @Test
    void testQueueMealFromMealieRecipeJson_AutoDeductEnabled() throws Exception {
        when(settingsService.isMealieAutoDeduct()).thenReturn(true);

        Item cheese = new Item();
        cheese.setId("item-cheese");
        cheese.setName("Cheddar Cheese");
        cheese.setDefaultUnit("oz");

        when(itemRepository.findAll()).thenReturn(List.of(cheese));
        when(itemRepository.findById("item-cheese")).thenReturn(Optional.of(cheese));
        when(mealRepository.findById("meal-uuid-1")).thenAnswer(inv -> {
            PendingCookedMeal m = new PendingCookedMeal("Grilled Cheese", null, 1.0);
            m.setId("meal-uuid-1");
            PendingMealItem item = new PendingMealItem();
            item.setId("pi-1");
            item.setFoodName("Cheddar Cheese");
            item.setMatchedItem(cheese);
            item.setDeductQuantity(2.0);
            item.setSelected(true);
            m.addItem(item);
            return Optional.of(m);
        });

        String json = """
        {
            "name": "Grilled Cheese",
            "recipeIngredient": [
                {
                    "food": "Cheddar Cheese",
                    "quantity": 2
                }
            ]
        }
        """;

        JsonNode root = objectMapper.readTree(json);
        PendingCookedMeal meal = pendingMealService.queueMealFromRecipeJson(root);

        // When auto deduct is enabled, reconcileMeal should have been called
        verify(inventoryService).deductFifo(eq(cheese), eq(2.0));
    }

    @Test
    void testQueueMealFromMealieCustomRecipeActionPayload_ContentWrapperAndScale() throws Exception {
        Item chickenItem = new Item();
        chickenItem.setId("item-chicken");
        chickenItem.setName("Chicken Breast");
        chickenItem.setDefaultUnit("lbs");

        Item creamItem = new Item();
        creamItem.setId("item-cream");
        creamItem.setName("Heavy Cream");
        creamItem.setDefaultUnit("cup");

        when(itemRepository.findAll()).thenReturn(List.of(chickenItem, creamItem));

        String json = """
        {
            "action": {
                "id": "action-123",
                "action_type": "post",
                "title": "Cook with Skafferi",
                "url": "http://localhost:8080/api/recipes/action"
            },
            "content": {
                "id": "recipe-uuid-789",
                "name": "Chicken Tikka Masala",
                "slug": "chicken-tikka-masala",
                "recipe_servings": 4.0,
                "recipe_ingredient": [
                    {
                        "title": null,
                        "note": "boneless skinless chicken breasts, cubed",
                        "unit": { "name": "pound", "abbreviation": "lb" },
                        "food": { "name": "chicken breast" },
                        "quantity": 1.5,
                        "display": "1.5 lbs chicken breast",
                        "is_food": true
                    },
                    {
                        "title": "For the sauce",
                        "note": null,
                        "unit": null,
                        "food": null,
                        "is_food": false
                    },
                    {
                        "title": null,
                        "note": "heavy cream",
                        "unit": { "name": "cup" },
                        "food": null,
                        "quantity": 1.0,
                        "display": "1 cup heavy cream",
                        "is_food": true
                    }
                ]
            },
            "recipe_scale": 2.0
        }
        """;

        JsonNode root = objectMapper.readTree(json);
        PendingCookedMeal meal = pendingMealService.queueMealFromRecipeJson(root);

        assertEquals("Chicken Tikka Masala", meal.getRecipeName());
        assertEquals("recipe-uuid-789", meal.getRecipeId());
        // 4 servings scaled 2x = 8
        assertEquals(8.0, meal.getServings());
        // Section header excluded, 2 real ingredients
        assertEquals(2, meal.getItems().size());

        PendingMealItem item1 = meal.getItems().get(0);
        assertEquals("chicken breast", item1.getFoodName());
        // 1.5 scaled 2x = 3.0
        assertEquals(3.0, item1.getOriginalQuantity());
        assertEquals("pound", item1.getOriginalUnit());
        assertNotNull(item1.getMatchedItem());
        assertEquals("item-chicken", item1.getMatchedItem().getId());

        PendingMealItem item2 = meal.getItems().get(1);
        // Extracted from note when food is null
        assertEquals("heavy cream", item2.getFoodName());
        // 1.0 scaled 2x = 2.0
        assertEquals(2.0, item2.getOriginalQuantity());
        assertEquals("cup", item2.getOriginalUnit());
        assertNotNull(item2.getMatchedItem());
        assertEquals("item-cream", item2.getMatchedItem().getId());
    }
}
