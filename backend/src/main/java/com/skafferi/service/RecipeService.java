package com.skafferi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        if (!settingsService.hasMealieConnection()) {
            return Collections.emptyList();
        }

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
        if (!settingsService.hasLlmApiKey()) {
            throw new IllegalStateException("LLM API key is not configured. Please add an API key in Settings > Integrations & APIs.");
        }

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
            candidateItems.addAll(notifs.expiredItems());
            if (candidateItems.isEmpty()) {
                candidateItems = pantryItems.stream().filter(i -> !i.isOutOfStock()).limit(5).collect(Collectors.toList());
            }
        }

        List<String> expiringNames = candidateItems.stream().map(PantryItemSummaryDto::name).distinct().collect(Collectors.toList());

        List<String> inStockStaples = pantryItems.stream()
                .filter(i -> !i.isOutOfStock() && !expiringNames.contains(i.name()))
                .map(PantryItemSummaryDto::name)
                .limit(20)
                .collect(Collectors.toList());

        return callLlmForZeroWasteMeal(expiringNames, inStockStaples);
    }

    private Map<String, Object> callLlmForZeroWasteMeal(List<String> expiringNames, List<String> inStockStaples) {
        String apiKey = settingsService.getLlmApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("LLM API key is not configured.");
        }

        String provider = settingsService.getLlmProvider();
        String model = settingsService.getLlmModel();
        String baseUrl = settingsService.getLlmBaseUrl();

        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = resolveDefaultBaseUrl(provider);
        }
        if (model == null || model.isBlank()) {
            model = resolveDefaultModel(provider);
        }

        String endpoint = baseUrl.replaceAll("/+$", "") + "/chat/completions";

        String expiringListStr = expiringNames.isEmpty()
                ? "Pantry staples & aging produce"
                : String.join(", ", expiringNames);

        String staplesListStr = inStockStaples.isEmpty()
                ? "Standard kitchen staples, spices, cooking oil, salt, pepper"
                : String.join(", ", inStockStaples);

        String systemPrompt = """
                You are a creative, professional Zero-Waste Chef. Your goal is to help home cooks rescue aging ingredients and produce delicious meals while preventing food waste.
                Respond ONLY with a valid JSON object matching this schema:
                {
                  "title": "Creative Recipe Title",
                  "description": "Short 1-2 sentence description highlighting the flavor and how it rescues expiring ingredients",
                  "utilizedItems": ["List of items rescued from the expiring list"],
                  "pantryItemsUsed": ["List of other in-stock pantry items used"],
                  "missingIngredients": ["List of optional missing ingredients not in stock, if any"],
                  "prepTime": "10 mins",
                  "cookTime": "20 mins",
                  "servings": 2,
                  "difficulty": "Easy",
                  "instructions": [
                    "Step 1...",
                    "Step 2...",
                    "Step 3..."
                  ],
                  "chefTips": "A practical zero-waste tip, storage advice, or clever substitution"
                }
                """;

        String userPrompt = String.format("""
                Please generate 1 inspiring zero-waste recipe using my kitchen ingredients.

                EXPIRING INGREDIENTS TO RESCUE (Priority):
                %s

                OTHER IN-STOCK PANTRY INGREDIENTS:
                %s
                """, expiringListStr, staplesListStr);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.7
        );

        try {
            String responseJson = restClient.post()
                    .uri(endpoint)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode != null && messageNode.has("content")) {
                    String rawContent = messageNode.get("content").asText();
                    return parseRecipeJson(rawContent, expiringNames);
                }
            }
            throw new RuntimeException("Unexpected response structure from LLM endpoint.");
        } catch (Exception e) {
            log.error("LLM Zero-Waste generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("LLM recipe generation failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseRecipeJson(String raw, List<String> fallbackExpiring) {
        String clean = raw.trim();
        if (clean.contains("```json")) {
            clean = clean.substring(clean.indexOf("```json") + 7);
            if (clean.contains("```")) {
                clean = clean.substring(0, clean.indexOf("```"));
            }
        } else if (clean.contains("```")) {
            clean = clean.substring(clean.indexOf("```") + 3);
            if (clean.contains("```")) {
                clean = clean.substring(0, clean.indexOf("```"));
            }
        }
        clean = clean.trim();

        try {
            return objectMapper.readValue(clean, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON directly, fallback to wrapper: {}", e.getMessage());
            return buildTailoredZeroWasteSuggestion(fallbackExpiring);
        }
    }

    public Map<String, Object> testLlmConnection(String apiKey, String provider, String model, String baseUrl) {
        String key = (apiKey != null && !apiKey.isBlank() && !apiKey.equals("********"))
                ? apiKey.trim()
                : settingsService.getLlmApiKey();

        if (key == null || key.isBlank()) {
            return Map.of("success", false, "error", "API Key is required.");
        }

        String p = (provider != null && !provider.isBlank()) ? provider : settingsService.getLlmProvider();
        String m = (model != null && !model.isBlank()) ? model.trim() : null;
        if (m == null || m.isBlank()
                || "gpt-4o-mini".equalsIgnoreCase(m)
                || "gemini-1.5-flash".equalsIgnoreCase(m)
                || "gemini-2.5-flash".equalsIgnoreCase(m)
                || "google/gemini-2.5-flash".equalsIgnoreCase(m)) {
            m = resolveDefaultModel(p);
        }

        String u = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.trim() : null;
        if (u == null || u.isBlank()) {
            u = resolveDefaultBaseUrl(p);
        }

        String endpoint = u.replaceAll("/+$", "") + "/chat/completions";

        Map<String, Object> payload = Map.of(
                "model", m,
                "messages", List.of(
                        Map.of("role", "user", "content", "Reply with: Hello from " + settingsService.getAppName() + " Zero-Waste Chef!")
                ),
                "max_tokens", 30
        );

        try {
            String responseJson = restClient.post()
                    .uri(endpoint)
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            String reply = "OK";
            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                reply = root.get("choices").get(0).path("message").path("content").asText(reply);
            }
            return Map.of("success", true, "message", "Connected successfully to " + m + "! Reply: " + reply.trim());
        } catch (Exception e) {
            log.warn("LLM test connection failed: {}", e.getMessage());
            return Map.of("success", false, "error", "Connection error: " + e.getMessage());
        }
    }

    public static String resolveDefaultBaseUrl(String provider) {
        if (provider == null) return "https://api.openai.com/v1";
        return switch (provider.toLowerCase()) {
            case "gemini" -> "https://generativelanguage.googleapis.com/v1beta/openai";
            case "openrouter" -> "https://openrouter.ai/api/v1";
            case "ollama" -> "http://host.docker.internal:11434/v1";
            default -> "https://api.openai.com/v1";
        };
    }

    public static String resolveDefaultModel(String provider) {
        if (provider == null) return "chat-latest";
        return switch (provider.toLowerCase()) {
            case "gemini" -> "gemini-flash-latest";
            case "openrouter" -> "openrouter/auto";
            case "ollama" -> "latest";
            default -> "chat-latest";
        };
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

        if (url != null && !url.isBlank() && token != null && !token.isBlank()) {
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

        return Collections.emptyList();
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
