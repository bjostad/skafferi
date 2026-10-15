package com.skafferi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skafferi.domain.PendingCookedMeal;
import com.skafferi.domain.PendingMealItem;
import com.skafferi.dto.NotificationSummaryDto;
import com.skafferi.dto.PantryItemSummaryDto;
import com.skafferi.dto.SettingsDto;
import com.skafferi.repository.ItemRepository;
import com.skafferi.repository.PendingCookedMealRepository;
import com.skafferi.util.IngredientMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    private final SettingsService settingsService;
    private final InventoryService inventoryService;
    private final BringSyncService bringSyncService;
    private final ItemRepository itemRepository;
    private final PendingCookedMealRepository pendingMealRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final Map<String, CachedMealieRecipe> recipeCache = new ConcurrentHashMap<>();

    private record CachedMealieRecipe(long timestamp, Map<String, Object> recipeData) {}

    public RecipeService(SettingsService settingsService,
                         InventoryService inventoryService,
                         BringSyncService bringSyncService,
                         ItemRepository itemRepository,
                         @org.springframework.beans.factory.annotation.Autowired(required = false) PendingCookedMealRepository pendingMealRepository) {
        this.settingsService = settingsService;
        this.inventoryService = inventoryService;
        this.bringSyncService = bringSyncService;
        this.itemRepository = itemRepository;
        this.pendingMealRepository = pendingMealRepository;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<Map<String, Object>> getMatchingRecipes() {
        return getMatchingRecipes(false);
    }

    public List<Map<String, Object>> getMatchingRecipes(boolean forceRefresh) {
        if (forceRefresh) {
            recipeCache.clear();
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

        List<Map<String, Object>> allRecipes = fetchRecipes(rawSettings);
        if (allRecipes.isEmpty()) {
            return Collections.emptyList();
        }

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

    private List<Map<String, Object>> fetchRecipes(SettingsDto settings) {
        Map<String, Map<String, Object>> recipeMapByName = new LinkedHashMap<>();

        // 1. Fetch from Mealie API if connected
        if (settingsService.hasMealieConnection()) {
            List<Map<String, Object>> mealieApiRecipes = fetchRecipesFromMealieApi(settings);
            for (Map<String, Object> r : mealieApiRecipes) {
                String name = (String) r.get("name");
                if (name != null && !name.isBlank()) {
                    recipeMapByName.put(name.toLowerCase().trim(), r);
                }
            }
        }

        // 2. Also merge recipes from local PendingCookedMeals (recipes submitted via Mealie Recipe Action or Webhooks)
        if (pendingMealRepository != null) {
            try {
                List<PendingCookedMeal> localMeals = pendingMealRepository.findAll();
                for (PendingCookedMeal m : localMeals) {
                    if (m.getRecipeName() == null || m.getRecipeName().isBlank() || "Mealie Recipe".equalsIgnoreCase(m.getRecipeName())) {
                        continue;
                    }
                    String key = m.getRecipeName().toLowerCase().trim();
                    if (!recipeMapByName.containsKey(key)) {
                        List<String> ings = new ArrayList<>();
                        if (m.getItems() != null) {
                            for (PendingMealItem item : m.getItems()) {
                                if (item.getFoodName() != null && !item.getFoodName().isBlank()) {
                                    ings.add(item.getFoodName());
                                } else if (item.getRawIngredientText() != null && !item.getRawIngredientText().isBlank()) {
                                    ings.add(item.getRawIngredientText());
                                }
                            }
                        }
                        if (!ings.isEmpty()) {
                            Map<String, Object> localRecipe = new HashMap<>();
                            localRecipe.put("id", m.getId() != null ? m.getId() : UUID.randomUUID().toString());
                            localRecipe.put("name", m.getRecipeName());
                            localRecipe.put("description", "Recipe from Mealie");
                            localRecipe.put("source", "Mealie Recipe Action");
                            String slug = m.getRecipeId() != null ? m.getRecipeId() : m.getRecipeName();
                            localRecipe.put("slug", slug);
                            localRecipe.put("ingredients", ings);
                            if (settings.mealieBaseUrl() != null && !settings.mealieBaseUrl().isBlank()) {
                                localRecipe.put("recipeUrl", settings.mealieBaseUrl().replaceAll("/+$", "") + "/recipes/" + slug);
                            }
                            recipeMapByName.put(key, localRecipe);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not load recipes from local pending meals: {}", e.getMessage());
            }
        }

        return new ArrayList<>(recipeMapByName.values());
    }

    private List<Map<String, Object>> fetchRecipesFromMealieApi(SettingsDto settings) {
        String url = settings.mealieBaseUrl();
        String token = settings.mealieApiToken();

        if (url == null || url.isBlank() || token == null || token.isBlank()) {
            return Collections.emptyList();
        }

        try {
            String listUrl = url.replaceAll("/+$", "") + "/api/recipes?perPage=50";
            log.info("Fetching recipe summaries from Mealie API: {}", listUrl);
            String json = restClient.get()
                    .uri(listUrl)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(json);
            JsonNode itemsNode = root.has("items") ? root.get("items") : root;

            if (!itemsNode.isArray() || itemsNode.isEmpty()) {
                log.info("No recipes returned from Mealie API list endpoint");
                return Collections.emptyList();
            }

            long now = System.currentTimeMillis();
            long ttlMs = 60 * 60 * 1000L; // 60-minute TTL

            List<JsonNode> summaries = new ArrayList<>();
            itemsNode.forEach(summaries::add);

            // Throttle parallel requests to Mealie to avoid overwhelming single-worker instances
            Semaphore rateLimiter = new Semaphore(3);

            // Fetch details with controlled concurrency and retry
            List<CompletableFuture<Map<String, Object>>> futures = summaries.stream().map(r -> CompletableFuture.<Map<String, Object>>supplyAsync(() -> {
                String id = r.hasNonNull("id") ? r.get("id").asText() : UUID.randomUUID().toString();
                String name = r.hasNonNull("name") ? r.get("name").asText() : "Untitled Recipe";
                String desc = r.hasNonNull("description") ? r.get("description").asText() : "";
                String slug = r.hasNonNull("slug") ? r.get("slug").asText() : id;
                String prepTime = null;
                String cookTime = null;

                // Check if summary already contains ingredients (if Mealie ever populates them)
                List<String> ings = extractIngredientsFromRecipeNode(r);

                if (ings.isEmpty()) {
                    CachedMealieRecipe cached = recipeCache.get(slug);
                    if (cached != null && (now - cached.timestamp() < ttlMs)) {
                        return cached.recipeData();
                    }

                    try {
                        rateLimiter.acquire();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return cached != null ? cached.recipeData() : Collections.emptyMap();
                    }

                    JsonNode detailRoot = null;
                    int maxAttempts = 3;
                    try {
                        String detailUrl = url.replaceAll("/+$", "") + "/api/recipes/" + slug;
                        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                            try {
                                String detailJson = restClient.get()
                                        .uri(detailUrl)
                                        .header("Authorization", "Bearer " + token)
                                        .retrieve()
                                        .body(String.class);

                                if (detailJson != null && !detailJson.isBlank()) {
                                    detailRoot = objectMapper.readTree(detailJson);
                                    break;
                                }
                            } catch (Exception e) {
                                if (attempt == maxAttempts) {
                                    log.warn("Could not fetch recipe details for slug '{}' from Mealie after {} attempts: {}", slug, maxAttempts, e.getMessage());
                                } else {
                                    log.debug("Retry {}/{} for slug '{}' due to: {}", attempt, maxAttempts, slug, e.getMessage());
                                    try {
                                        Thread.sleep(150L * attempt);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        break;
                                    }
                                }
                            }
                        }
                    } finally {
                        rateLimiter.release();
                    }

                    if (detailRoot != null) {
                        ings = extractIngredientsFromRecipeNode(detailRoot);
                        if (detailRoot.hasNonNull("name") && !detailRoot.path("name").asText().isBlank()) {
                            name = detailRoot.get("name").asText();
                        }
                        if (detailRoot.hasNonNull("description")) {
                            desc = detailRoot.get("description").asText();
                        }
                        if (detailRoot.hasNonNull("prepTime")) {
                            prepTime = detailRoot.get("prepTime").asText();
                        } else if (detailRoot.hasNonNull("prep_time")) {
                            prepTime = detailRoot.get("prep_time").asText();
                        }
                        if (detailRoot.hasNonNull("performTime")) {
                            cookTime = detailRoot.get("performTime").asText();
                        } else if (detailRoot.hasNonNull("cookTime")) {
                            cookTime = detailRoot.get("cookTime").asText();
                        } else if (detailRoot.hasNonNull("perform_time")) {
                            cookTime = detailRoot.get("perform_time").asText();
                        } else if (detailRoot.hasNonNull("cook_time")) {
                            cookTime = detailRoot.get("cook_time").asText();
                        }
                    } else if (cached != null && cached.recipeData() != null) {
                        log.info("Mealie failed for slug '{}'; falling back to existing cached recipe details", slug);
                        return cached.recipeData();
                    }
                }

                Map<String, Object> recipeMap = new HashMap<>();
                recipeMap.put("id", id);
                recipeMap.put("name", name);
                recipeMap.put("description", desc);
                recipeMap.put("source", "Mealie");
                recipeMap.put("slug", slug);
                recipeMap.put("ingredients", ings);
                if (url != null && !url.isBlank()) {
                    recipeMap.put("recipeUrl", url.replaceAll("/+$", "") + "/recipes/" + slug);
                }
                if (prepTime != null && !prepTime.isBlank()) {
                    recipeMap.put("prepTime", prepTime);
                }
                if (cookTime != null && !cookTime.isBlank()) {
                    recipeMap.put("cookTime", cookTime);
                }

                if (!ings.isEmpty()) {
                    recipeCache.put(slug, new CachedMealieRecipe(now, recipeMap));
                }

                return recipeMap;
            })).toList();

            List<Map<String, Object>> list = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(rm -> {
                        @SuppressWarnings("unchecked")
                        List<String> ingList = (List<String>) rm.get("ingredients");
                        return ingList != null && !ingList.isEmpty();
                    })
                    .collect(Collectors.toList());

            log.info("Fetched {} complete recipes with ingredients from Mealie API", list.size());
            return list;
        } catch (Exception e) {
            log.warn("Could not fetch recipes from Mealie API: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> extractIngredientsFromRecipeNode(JsonNode recipeNode) {
        List<String> ings = new ArrayList<>();
        JsonNode ingsNode = null;
        if (recipeNode.has("recipe_ingredient") && recipeNode.get("recipe_ingredient").isArray()) {
            ingsNode = recipeNode.get("recipe_ingredient");
        } else if (recipeNode.has("recipeIngredient") && recipeNode.get("recipeIngredient").isArray()) {
            ingsNode = recipeNode.get("recipeIngredient");
        } else if (recipeNode.has("ingredients") && recipeNode.get("ingredients").isArray()) {
            ingsNode = recipeNode.get("ingredients");
        }

        if (ingsNode != null) {
            for (JsonNode ing : ingsNode) {
                boolean isFood = true;
                if (ing.has("is_food")) {
                    isFood = ing.get("is_food").asBoolean(true);
                } else if (ing.has("isFood")) {
                    isFood = ing.get("isFood").asBoolean(true);
                }
                if (!isFood) {
                    boolean hasFood = (ing.hasNonNull("food") && !ing.path("food").asText().isBlank())
                            || (ing.hasNonNull("foodName") && !ing.path("foodName").asText().isBlank())
                            || (ing.hasNonNull("food_name") && !ing.path("food_name").asText().isBlank());
                    if (!hasFood) {
                        continue; // skip section headers like "For the sauce"
                    }
                }

                String foodName = null;
                if (ing.path("food").isObject() && ing.path("food").hasNonNull("name") && !ing.path("food").path("name").asText().isBlank()) {
                    foodName = ing.path("food").path("name").asText().trim();
                } else if (ing.path("food").isTextual() && !ing.path("food").asText().isBlank()) {
                    foodName = ing.path("food").asText().trim();
                } else if (ing.hasNonNull("foodName") && !ing.path("foodName").asText().isBlank()) {
                    foodName = ing.path("foodName").asText().trim();
                } else if (ing.hasNonNull("food_name") && !ing.path("food_name").asText().isBlank()) {
                    foodName = ing.path("food_name").asText().trim();
                } else if (ing.hasNonNull("note") && !ing.path("note").asText().isBlank()) {
                    foodName = ing.path("note").asText().trim();
                } else if (ing.hasNonNull("title") && !ing.path("title").asText().isBlank()) {
                    foodName = ing.path("title").asText().trim();
                } else if (ing.hasNonNull("display") && !ing.path("display").asText().isBlank()) {
                    foodName = ing.path("display").asText().trim();
                } else if (ing.isTextual() && !ing.asText().isBlank()) {
                    foodName = ing.asText().trim();
                }

                if (foodName != null && !foodName.isBlank()) {
                    ings.add(foodName);
                }
            }
        }
        return ings;
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
