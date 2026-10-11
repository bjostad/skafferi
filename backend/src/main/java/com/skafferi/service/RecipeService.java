package com.skafferi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skafferi.domain.Item;
import com.skafferi.dto.NotificationSummaryDto;
import com.skafferi.dto.PantryItemSummaryDto;
import com.skafferi.dto.SettingsDto;
import com.skafferi.repository.ItemRepository;
import com.skafferi.util.IngredientMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    private final SettingsService settingsService;
    private final InventoryService inventoryService;
    private final BringSyncService bringSyncService;
    private final ItemRepository itemRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RecipeService(SettingsService settingsService,
                         InventoryService inventoryService,
                         BringSyncService bringSyncService,
                         ItemRepository itemRepository) {
        this.settingsService = settingsService;
        this.inventoryService = inventoryService;
        this.bringSyncService = bringSyncService;
        this.itemRepository = itemRepository;
        this.restClient = RestClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public List<Map<String, Object>> getMatchingRecipes() {
        SettingsDto rawSettings = settingsService.getRawSettings();
        List<PantryItemSummaryDto> pantryItems = inventoryService.getAllPantrySummaries(null, null, null, null, null, null, "NAME");

        // Filter items that have positive stock
        List<PantryItemSummaryDto> inStockItems = pantryItems.stream()
                .filter(i -> !i.isOutOfStock() && i.totalQuantity() > 0)
                .collect(Collectors.toList());

        Set<String> inStockNames = inStockItems.stream()
                .map(PantryItemSummaryDto::name)
                .collect(Collectors.toSet());

        NotificationSummaryDto notifs = inventoryService.getNotificationSummary();
        Set<String> expiringNames = new HashSet<>();
        notifs.expiringSoonItems().forEach(i -> expiringNames.add(i.name()));
        notifs.expiredItems().forEach(i -> expiringNames.add(i.name()));
        notifs.freshCheckItems().forEach(i -> expiringNames.add(i.name()));

        List<Map<String, Object>> allRecipes = fetchRecipesFromMealieOrPresets(rawSettings);

        List<Map<String, Object>> matchedRecipes = new ArrayList<>();

        for (Map<String, Object> r : allRecipes) {
            @SuppressWarnings("unchecked")
            List<String> ingredients = (List<String>) r.get("ingredients");
            if (ingredients == null || ingredients.isEmpty()) continue;

            List<String> matched = new ArrayList<>();
            List<String> missing = new ArrayList<>();
            List<String> expiringUsed = new ArrayList<>();

            for (String ing : ingredients) {
                boolean foundInStock = false;
                for (String stockName : inStockNames) {
                    if (IngredientMatcher.matches(ing, stockName)) {
                        foundInStock = true;
                        matched.add(ing);
                        break;
                    }
                }

                if (!foundInStock) {
                    missing.add(ing);
                } else {
                    for (String expName : expiringNames) {
                        if (IngredientMatcher.matches(ing, expName)) {
                            expiringUsed.add(ing);
                            break;
                        }
                    }
                }
            }

            int matchPercent = (int) Math.round(((double) matched.size() / (double) ingredients.size()) * 100);

            Map<String, Object> result = new HashMap<>(r);
            result.put("matchPercentage", matchPercent);
            result.put("matchedIngredients", matched);
            result.put("missingIngredients", missing);
            result.put("expiringIngredientsUsed", expiringUsed);
            result.put("savesExpiringFood", !expiringUsed.isEmpty());

            matchedRecipes.add(result);
        }

        // Sort: recipes using expiring food first, then highest match %
        matchedRecipes.sort((a, b) -> {
            boolean aExp = (boolean) a.get("savesExpiringFood");
            boolean bExp = (boolean) b.get("savesExpiringFood");
            if (aExp != bExp) {
                return aExp ? -1 : 1;
            }
            int aMatch = (int) a.get("matchPercentage");
            int bMatch = (int) b.get("matchPercentage");
            return Integer.compare(bMatch, aMatch);
        });

        return matchedRecipes;
    }

    public Map<String, Object> generateZeroWasteMeal(List<String> selectedItemIds) {
        List<PantryItemSummaryDto> pantryItems = inventoryService.getAllPantrySummaries(null, null, null, null, null, null, "NAME");

        List<PantryItemSummaryDto> candidateItems;
        if (selectedItemIds != null && !selectedItemIds.isEmpty()) {
            candidateItems = pantryItems.stream()
                    .filter(i -> selectedItemIds.contains(i.id()))
                    .collect(Collectors.toList());
        } else {
            NotificationSummaryDto notifs = inventoryService.getNotificationSummary();
            candidateItems = new ArrayList<>(notifs.expiringSoonItems());
            candidateItems.addAll(notifs.freshCheckItems());
            if (candidateItems.isEmpty()) {
                candidateItems = pantryItems.stream().limit(5).collect(Collectors.toList());
            }
        }

        List<String> names = candidateItems.stream().map(PantryItemSummaryDto::name).collect(Collectors.toList());

        // Generate tailored dish suggestions
        return buildTailoredZeroWasteSuggestion(names);
    }

    public Map<String, Object> addMissingToShoppingList(List<String> items) {
        int added = 0;
        for (String item : items) {
            try {
                bringSyncService.addItem(item, "1");
                added++;
            } catch (Exception e) {
                log.warn("Could not add '{}' to Bring: {}", item, e.getMessage());
            }
        }
        return Map.of("success", true, "addedCount", added);
    }

    private List<Map<String, Object>> fetchRecipesFromMealieOrPresets(SettingsDto settings) {
        String url = settings.mealieBaseUrl();
        String token = settings.mealieApiToken();

        if (url != null && !url.isBlank()) {
            try {
                String fullUrl = url.replaceAll("/+$", "") + "/api/recipes";
                String json = restClient.get()
                        .uri(fullUrl)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .body(String.class);

                JsonNode root = objectMapper.readTree(json);
                JsonNode itemsNode = root.has("items") ? root.get("items") : root;

                if (itemsNode.isArray() && itemsNode.size() > 0) {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (JsonNode r : itemsNode) {
                        String id = r.has("id") ? r.get("id").asText() : UUID.randomUUID().toString();
                        String name = r.has("name") ? r.get("name").asText() : "Untitled Recipe";
                        String desc = r.has("description") ? r.get("description").asText() : "";
                        String slug = r.has("slug") ? r.get("slug").asText() : id;

                        List<String> ings = new ArrayList<>();
                        if (r.has("recipeIngredient")) {
                            for (JsonNode ing : r.get("recipeIngredient")) {
                                if (ing.has("food") && ing.get("food").has("name") && !ing.get("food").get("name").asText().isBlank()) {
                                    ings.add(ing.get("food").get("name").asText());
                                } else if (ing.has("note") && !ing.get("note").asText().isBlank()) {
                                    ings.add(ing.get("note").asText());
                                } else if (ing.has("title") && !ing.get("title").asText().isBlank()) {
                                    ings.add(ing.get("title").asText());
                                } else if (ing.isTextual()) {
                                    ings.add(ing.asText());
                                }
                            }
                        }

                        list.add(Map.of(
                                "id", id,
                                "name", name,
                                "description", desc,
                                "source", "Mealie",
                                "slug", slug,
                                "ingredients", ings
                        ));
                    }
                    if (!list.isEmpty()) {
                        return list;
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch recipes from Mealie: {}", e.getMessage());
            }
        }

        // Built-in starter zero-waste pantry recipes
        return getPresetPantryRecipes();
    }

    private List<Map<String, Object>> getPresetPantryRecipes() {
        return List.of(
                Map.of(
                        "id", "preset-frittata",
                        "name", "Farmer's Skillet Frittata",
                        "description", "A classic, forgiving egg dish designed to turn any leftover vegetables, cheese, or cooked meats into a gourmet meal.",
                        "source", "Pantry Chef",
                        "prepTime", "10 mins",
                        "cookTime", "15 mins",
                        "ingredients", List.of("Eggs", "Milk", "Cheese", "Onion", "Bell Pepper", "Spinach", "Butter")
                ),
                Map.of(
                        "id", "preset-stirfry",
                        "name", "Pantry-Rescue Stir Fry",
                        "description", "High-heat wok or skillet toss that crisps up aging vegetables and pairs with rice or noodles.",
                        "source", "Pantry Chef",
                        "prepTime", "10 mins",
                        "cookTime", "10 mins",
                        "ingredients", List.of("Rice", "Soy Sauce", "Garlic", "Broccoli", "Carrots", "Chicken", "Sesame Oil")
                ),
                Map.of(
                        "id", "preset-soup",
                        "name", "Hearty Harvest Vegetable & Bean Soup",
                        "description", "A comforting simmer that transforms aging root vegetables and canned beans into rich broth.",
                        "source", "Pantry Chef",
                        "prepTime", "15 mins",
                        "cookTime", "30 mins",
                        "ingredients", List.of("Canned Beans", "Carrots", "Celery", "Onion", "Garlic", "Olive Oil", "Canned Tomatoes")
                ),
                Map.of(
                        "id", "preset-pasta",
                        "name", "One-Pan Creamy Garlic Herb Pasta",
                        "description", "Silky pasta that utilizes dairy staples and leftover herbs before they wilt.",
                        "source", "Pantry Chef",
                        "prepTime", "5 mins",
                        "cookTime", "15 mins",
                        "ingredients", List.of("Pasta", "Garlic", "Butter", "Parmesan", "Heavy Cream", "Black Pepper")
                ),
                Map.of(
                        "id", "preset-grainbowl",
                        "name", "Roasted Veggie & Grain Power Bowl",
                        "description", "Caramelized sheet-pan vegetables served over seasoned quinoa or rice with vinaigrette.",
                        "source", "Pantry Chef",
                        "prepTime", "10 mins",
                        "cookTime", "25 mins",
                        "ingredients", List.of("Quinoa", "Sweet Potatoes", "Olive Oil", "Lemon", "Chickpeas", "Mixed Greens")
                )
        );
    }

    private Map<String, Object> buildTailoredZeroWasteSuggestion(List<String> expiringIngredients) {
        String mainItems = String.join(", ", expiringIngredients);
        if (expiringIngredients.isEmpty()) {
            mainItems = "Pantry staples";
        }

        List<String> instructions = List.of(
                "Prep: Wash and chop your expiring ingredients (" + mainItems + ") into uniform bite-sized pieces.",
                "Heat: Warm 1-2 tbsp olive oil or butter in a wide skillet over medium-high heat.",
                "Sauté: Add aromatics (garlic/onion if on hand), then cook denser vegetables for 4-5 minutes until tender-crisp.",
                "Combine: Fold in greens, proteins, or cooked grains. Season generously with salt, pepper, and your favorite pantry herbs.",
                "Finish: Plate immediately and garnish with a squeeze of citrus or shredded cheese for maximum freshness."
        );

        return Map.of(
                "title", "Zero-Waste Chef's Creation with " + (!expiringIngredients.isEmpty() ? expiringIngredients.get(0) : "Fresh Produce"),
                "description", "Specially composed to maximize flavor and rescue your expiring items before they go to waste.",
                "utilizedItems", expiringIngredients,
                "prepTime", "10 mins",
                "cookTime", "15 mins",
                "instructions", instructions,
                "chefTips", "Pro tip: Keep vegetable trims and herb stems in a freezer bag to simmer rich homemade vegetable stock later!"
        );
    }
}
