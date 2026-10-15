package com.skafferi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skafferi.domain.CachedRecipe;
import com.skafferi.domain.PendingCookedMeal;
import com.skafferi.domain.PendingMealItem;
import com.skafferi.dto.NotificationSummaryDto;
import com.skafferi.dto.PantryItemSummaryDto;
import com.skafferi.dto.SettingsDto;
import com.skafferi.repository.CachedRecipeRepository;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    private final SettingsService settingsService;
    private final InventoryService inventoryService;
    private final BringSyncService bringSyncService;
    private final ItemRepository itemRepository;
    private final PendingCookedMealRepository pendingMealRepository;
    private final CachedRecipeRepository cachedRecipeRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final Map<String, CachedMealieRecipe> recipeCache = new ConcurrentHashMap<>();
    private final AtomicBoolean isBackgroundSyncRunning = new AtomicBoolean(false);
    private volatile long lastMealieScanTime = 0;
    private static final long SCAN_COOLDOWN_MS = 5 * 60 * 1000L; // 5 minutes between full scans unless forced

    private record CachedMealieRecipe(long timestamp, String dateUpdated, Map<String, Object> recipeData) {}

    public RecipeService(SettingsService settingsService,
                         InventoryService inventoryService,
                         BringSyncService bringSyncService,
                         ItemRepository itemRepository,
                         @org.springframework.beans.factory.annotation.Autowired(required = false) PendingCookedMealRepository pendingMealRepository,
                         @org.springframework.beans.factory.annotation.Autowired(required = false) CachedRecipeRepository cachedRecipeRepository) {
        this.settingsService = settingsService;
        this.inventoryService = inventoryService;
        this.bringSyncService = bringSyncService;
        this.itemRepository = itemRepository;
        this.pendingMealRepository = pendingMealRepository;
        this.cachedRecipeRepository = cachedRecipeRepository;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = new ObjectMapper();

        if (this.cachedRecipeRepository != null) {
            try {
                List<CachedRecipe> allCached = this.cachedRecipeRepository.findAll();
                long now = System.currentTimeMillis();
                for (CachedRecipe cr : allCached) {
                    Map<String, Object> rMap = toRecipeMap(cr);
                    if (rMap != null && rMap.get("ingredients") != null && !((List<?>) rMap.get("ingredients")).isEmpty()) {
                        this.recipeCache.put(cr.getSlug(), new CachedMealieRecipe(cr.getLastSyncedAt() > 0 ? cr.getLastSyncedAt() : now, cr.getDateUpdated(), rMap));
                    }
                }
                log.info("Initialized in-memory recipe cache with {} recipes from SQLite database", recipeCache.size());
            } catch (Exception e) {
                log.warn("Could not pre-load cached recipes from SQLite: {}", e.getMessage());
            }
        }
    }

    public List<Map<String, Object>> getMatchingRecipes() {
        return getMatchingRecipes(false);
    }

    public List<Map<String, Object>> getMatchingRecipes(boolean forceRefresh) {
        if (forceRefresh) {
            lastMealieScanTime = 0;
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

        List<Map<String, Object>> allRecipes = fetchRecipes(rawSettings, forceRefresh);
        if (allRecipes.isEmpty()) {
            return Collections.emptyList();
        }

        SettingsDto settings = null;
        String publicBaseUrl = null;
        try {
            settings = settingsService.getRawSettings();
            if (settings != null) {
                publicBaseUrl = (settings.mealieFrontendUrl() != null && !settings.mealieFrontendUrl().isBlank())
                        ? settings.mealieFrontendUrl().trim().replaceAll("/+$", "")
                        : (settings.mealieBaseUrl() != null && !settings.mealieBaseUrl().isBlank() ? settings.mealieBaseUrl().trim().replaceAll("/+$", "") : null);
            }
        } catch (Exception e) {
            // ignore
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
            if (publicBaseUrl != null) {
                String slug = (String) r.get("slug");
                if (slug != null && !slug.isBlank()) {
                    result.put("recipeUrl", publicBaseUrl + "/recipes/" + slug);
                }
            }
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

        // Scalability protection for large catalogs (e.g. 15 to 15,000 recipes):
        // Return all recipes that save expiring food, plus top matches capped at 100 total
        // to avoid overwhelming browser memory and DOM rendering.
        int maxResults = 100;
        if (matchedRecipes.size() > maxResults) {
            List<Map<String, Object>> capped = new ArrayList<>();
            for (Map<String, Object> r : matchedRecipes) {
                if (Boolean.TRUE.equals(r.get("savesExpiringFood"))) {
                    capped.add(r);
                } else if (capped.size() < maxResults) {
                    capped.add(r);
                }
            }
            return capped;
        }

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

    private List<Map<String, Object>> fetchRecipes(SettingsDto settings, boolean forceRefresh) {
        Map<String, Map<String, Object>> recipeMapByName = new LinkedHashMap<>();

        // 1. Fetch from Mealie API if connected, or serve from local SQLite cache
        if (settingsService.hasMealieConnection()) {
            List<Map<String, Object>> mealieApiRecipes = fetchRecipesFromMealieApi(settings, forceRefresh);
            for (Map<String, Object> r : mealieApiRecipes) {
                String name = (String) r.get("name");
                if (name != null && !name.isBlank()) {
                    recipeMapByName.put(name.toLowerCase().trim(), r);
                }
            }
        } else {
            List<Map<String, Object>> fallback = getFallbackCachedRecipes();
            for (Map<String, Object> r : fallback) {
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
                            String publicBaseUrl = (settings.mealieFrontendUrl() != null && !settings.mealieFrontendUrl().isBlank())
                                    ? settings.mealieFrontendUrl().trim()
                                    : settings.mealieBaseUrl();
                            if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
                                localRecipe.put("recipeUrl", publicBaseUrl.replaceAll("/+$", "") + "/recipes/" + slug);
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

    private List<Map<String, Object>> fetchRecipesFromMealieApi(SettingsDto settings, boolean forceRefresh) {
        String url = settings.mealieBaseUrl();
        String token = settings.mealieApiToken();
        String publicBaseUrl = (settings.mealieFrontendUrl() != null && !settings.mealieFrontendUrl().isBlank())
                ? settings.mealieFrontendUrl().trim()
                : url;

        if (url == null || url.isBlank() || token == null || token.isBlank()) {
            return Collections.emptyList();
        }

        long now = System.currentTimeMillis();

        // 1. Check cooldown: if scanned recently and cache is non-empty, serve immediately
        if (!forceRefresh && (now - lastMealieScanTime < SCAN_COOLDOWN_MS) && !recipeCache.isEmpty()) {
            log.debug("Serving {} recipes from memory/SQLite cache (Mealie scan cooldown active)", recipeCache.size());
            return recipeCache.values().stream()
                    .map(CachedMealieRecipe::recipeData)
                    .filter(r -> r.get("ingredients") != null && !((List<?>) r.get("ingredients")).isEmpty())
                    .collect(Collectors.toList());
        }

        try {
            int page = 1;
            int perPage = 100;
            int totalPages = 1;
            List<JsonNode> allSummaries = new ArrayList<>();
            Set<String> activeMealieSlugs = new HashSet<>();

            while (page <= totalPages && page <= 200) { // Safety cap at 200 pages (20,000 recipes)
                String listUrl = url.replaceAll("/+$", "") + "/api/recipes?page=" + page + "&perPage=" + perPage;
                log.debug("Fetching recipe summaries page {}: {}", page, listUrl);

                String json;
                try {
                    json = restClient.get()
                            .uri(listUrl)
                            .header("Authorization", "Bearer " + token)
                            .retrieve()
                            .body(String.class);
                } catch (Exception e) {
                    log.warn("Failed to fetch Mealie recipe summaries on page {}: {}", page, e.getMessage());
                    break;
                }

                if (json == null || json.isBlank()) break;
                JsonNode root = objectMapper.readTree(json);
                JsonNode itemsNode = root.has("items") ? root.get("items") : root;

                if (!itemsNode.isArray() || itemsNode.isEmpty()) {
                    break;
                }

                for (JsonNode item : itemsNode) {
                    allSummaries.add(item);
                    if (item.hasNonNull("slug")) {
                        activeMealieSlugs.add(item.get("slug").asText());
                    } else if (item.hasNonNull("id")) {
                        activeMealieSlugs.add(item.get("id").asText());
                    }
                }

                if (root.has("totalPages")) {
                    totalPages = root.get("totalPages").asInt(1);
                } else if (root.has("total_pages")) {
                    totalPages = root.get("total_pages").asInt(1);
                } else if (root.has("total")) {
                    int total = root.get("total").asInt(0);
                    totalPages = (int) Math.ceil((double) total / perPage);
                } else {
                    if (itemsNode.size() < perPage) {
                        break;
                    }
                    totalPages = page + 1;
                }

                page++;
            }

            if (allSummaries.isEmpty()) {
                log.info("No recipes returned from Mealie API list endpoint");
                return getFallbackCachedRecipes();
            }

            // Clean up any deleted Mealie recipes from cache and DB
            if (!activeMealieSlugs.isEmpty()) {
                recipeCache.keySet().removeIf(slug -> {
                    CachedMealieRecipe r = recipeCache.get(slug);
                    if (r != null && "Mealie".equals(r.recipeData().get("source"))) {
                        if (!activeMealieSlugs.contains(slug)) {
                            if (cachedRecipeRepository != null) {
                                cachedRecipeRepository.findBySlug(slug).ifPresent(cachedRecipeRepository::delete);
                            }
                            return true;
                        }
                    }
                    return false;
                });
            }

            // Identify which recipes need detail fetching
            List<JsonNode> needsFetch = new ArrayList<>();
            List<Map<String, Object>> readyRecipes = new ArrayList<>();

            for (JsonNode summary : allSummaries) {
                String slug = summary.hasNonNull("slug") ? summary.get("slug").asText() : summary.path("id").asText();
                String dateUpdated = extractDateUpdated(summary);

                // Check if summary node itself contains ingredients
                List<String> summaryIngs = extractIngredientsFromRecipeNode(summary);
                if (!summaryIngs.isEmpty()) {
                    Map<String, Object> rMap = buildRecipeMapFromNode(summary, summaryIngs, publicBaseUrl);
                    recipeCache.put(slug, new CachedMealieRecipe(now, dateUpdated, rMap));
                    persistToDb(rMap, dateUpdated);
                    readyRecipes.add(rMap);
                    continue;
                }

                CachedMealieRecipe cached = recipeCache.get(slug);
                if (cached != null
                        && cached.recipeData() != null
                        && cached.recipeData().get("ingredients") != null
                        && !((List<?>) cached.recipeData().get("ingredients")).isEmpty()) {

                    if (dateUpdated != null && dateUpdated.equals(cached.dateUpdated())) {
                        readyRecipes.add(cached.recipeData());
                        continue;
                    } else if (dateUpdated == null && !forceRefresh && (now - cached.timestamp() < (60 * 60 * 1000L))) {
                        readyRecipes.add(cached.recipeData());
                        continue;
                    }
                }

                needsFetch.add(summary);
            }

            log.info("Mealie recipe sync: {} total summaries, {} already cached, {} need detail fetch",
                    allSummaries.size(), readyRecipes.size(), needsFetch.size());

            // Synchronously fetch up to 50 recipes so user gets immediate response
            int syncLimit = 50;
            List<JsonNode> syncBatch = needsFetch.size() <= syncLimit
                    ? needsFetch
                    : needsFetch.subList(0, syncLimit);
            List<JsonNode> backgroundBatch = needsFetch.size() > syncLimit
                    ? needsFetch.subList(syncLimit, needsFetch.size())
                    : Collections.emptyList();

            if (!syncBatch.isEmpty()) {
                Semaphore rateLimiter = new Semaphore(3);
                List<CompletableFuture<Map<String, Object>>> futures = syncBatch.stream().map(summary -> CompletableFuture.supplyAsync(() -> {
                    try {
                        rateLimiter.acquire();
                        try {
                            String dateUpdated = extractDateUpdated(summary);
                            Map<String, Object> detail = fetchRecipeDetailWithRetry(url, token, summary, dateUpdated, publicBaseUrl);
                            if (detail != null && detail.get("ingredients") != null && !((List<?>) detail.get("ingredients")).isEmpty()) {
                                String slug = (String) detail.get("slug");
                                recipeCache.put(slug, new CachedMealieRecipe(now, dateUpdated, detail));
                                persistToDb(detail, dateUpdated);
                                return detail;
                            }
                        } finally {
                            rateLimiter.release();
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                })).toList();

                for (CompletableFuture<Map<String, Object>> f : futures) {
                    try {
                        Map<String, Object> r = f.join();
                        if (r != null) {
                            readyRecipes.add(r);
                        }
                    } catch (Exception e) {
                        log.warn("Error fetching recipe detail in sync batch: {}", e.getMessage());
                    }
                }
            }

            // If large catalog (e.g. 15,000 recipes), queue background worker for remaining items
            if (!backgroundBatch.isEmpty() && isBackgroundSyncRunning.compareAndSet(false, true)) {
                final List<JsonNode> toSync = new ArrayList<>(backgroundBatch);
                log.info("Queued {} recipes for background synchronization into SQLite cache", toSync.size());
                CompletableFuture.runAsync(() -> {
                    try {
                        Semaphore bgLimiter = new Semaphore(2);
                        int synced = 0;
                        for (JsonNode summary : toSync) {
                            if (Thread.currentThread().isInterrupted()) break;
                            String slug = summary.hasNonNull("slug") ? summary.get("slug").asText() : summary.path("id").asText();
                            String dateUpdated = extractDateUpdated(summary);

                            CachedMealieRecipe existing = recipeCache.get(slug);
                            if (existing != null && dateUpdated != null && dateUpdated.equals(existing.dateUpdated())
                                    && existing.recipeData().get("ingredients") != null
                                    && !((List<?>) existing.recipeData().get("ingredients")).isEmpty()) {
                                continue;
                            }

                            try {
                                bgLimiter.acquire();
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                            try {
                                Map<String, Object> detail = fetchRecipeDetailWithRetry(url, token, summary, dateUpdated, publicBaseUrl);
                                if (detail != null && detail.get("ingredients") != null && !((List<?>) detail.get("ingredients")).isEmpty()) {
                                    recipeCache.put(slug, new CachedMealieRecipe(System.currentTimeMillis(), dateUpdated, detail));
                                    persistToDb(detail, dateUpdated);
                                    synced++;
                                    if (synced % 100 == 0) {
                                        log.info("Background recipe sync progress: {}/{} ingested into SQLite", synced, toSync.size());
                                    }
                                }
                                Thread.sleep(60);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            } catch (Exception ex) {
                                log.debug("Background recipe sync error for slug '{}': {}", slug, ex.getMessage());
                            } finally {
                                bgLimiter.release();
                            }
                        }
                        log.info("Completed background recipe sync: {} new/updated recipes saved to SQLite", synced);
                    } finally {
                        isBackgroundSyncRunning.set(false);
                    }
                });
            }

            lastMealieScanTime = now;
            log.info("Serving {} complete recipes with ingredients from Mealie (API + Cache)", readyRecipes.size());
            return readyRecipes;

        } catch (Exception e) {
            log.warn("Could not fetch recipes from Mealie API: {}. Falling back to cached recipes.", e.getMessage());
            return getFallbackCachedRecipes();
        }
    }

    private List<Map<String, Object>> getFallbackCachedRecipes() {
        if (!recipeCache.isEmpty()) {
            return recipeCache.values().stream()
                    .map(CachedMealieRecipe::recipeData)
                    .filter(r -> r.get("ingredients") != null && !((List<?>) r.get("ingredients")).isEmpty())
                    .collect(Collectors.toList());
        }
        if (cachedRecipeRepository != null) {
            try {
                return cachedRecipeRepository.findAll().stream()
                        .map(this::toRecipeMap)
                        .filter(Objects::nonNull)
                        .filter(r -> r.get("ingredients") != null && !((List<?>) r.get("ingredients")).isEmpty())
                        .collect(Collectors.toList());
            } catch (Exception ex) {
                log.warn("Could not load fallback recipes from repository: {}", ex.getMessage());
            }
        }
        return Collections.emptyList();
    }

    private String extractDateUpdated(JsonNode node) {
        if (node == null) return null;
        if (node.hasNonNull("dateUpdated") && !node.get("dateUpdated").asText().isBlank()) {
            return node.get("dateUpdated").asText().trim();
        }
        if (node.hasNonNull("date_updated") && !node.get("date_updated").asText().isBlank()) {
            return node.get("date_updated").asText().trim();
        }
        if (node.hasNonNull("updatedAt") && !node.get("updatedAt").asText().isBlank()) {
            return node.get("updatedAt").asText().trim();
        }
        if (node.hasNonNull("update_at") && !node.get("update_at").asText().isBlank()) {
            return node.get("update_at").asText().trim();
        }
        if (node.hasNonNull("lastModified") && !node.get("lastModified").asText().isBlank()) {
            return node.get("lastModified").asText().trim();
        }
        return null;
    }

    private Map<String, Object> fetchRecipeDetailWithRetry(String url, String token, JsonNode summary, String dateUpdated, String publicBaseUrl) {
        String id = summary.hasNonNull("id") ? summary.get("id").asText() : UUID.randomUUID().toString();
        String name = summary.hasNonNull("name") ? summary.get("name").asText() : "Untitled Recipe";
        String desc = summary.hasNonNull("description") ? summary.get("description").asText() : "";
        String slug = summary.hasNonNull("slug") ? summary.get("slug").asText() : id;
        String prepTime = null;
        String cookTime = null;

        JsonNode detailRoot = null;
        int maxAttempts = 3;
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

        List<String> ings = Collections.emptyList();
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
        } else {
            CachedMealieRecipe cached = recipeCache.get(slug);
            if (cached != null && cached.recipeData() != null) {
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
        String linkUrl = (publicBaseUrl != null && !publicBaseUrl.isBlank()) ? publicBaseUrl : url;
        if (linkUrl != null && !linkUrl.isBlank()) {
            recipeMap.put("recipeUrl", linkUrl.replaceAll("/+$", "") + "/recipes/" + slug);
        }
        if (prepTime != null && !prepTime.isBlank()) {
            recipeMap.put("prepTime", prepTime);
        }
        if (cookTime != null && !cookTime.isBlank()) {
            recipeMap.put("cookTime", cookTime);
        }

        return recipeMap;
    }

    private Map<String, Object> buildRecipeMapFromNode(JsonNode node, List<String> ings, String url) {
        String id = node.hasNonNull("id") ? node.get("id").asText() : UUID.randomUUID().toString();
        String name = node.hasNonNull("name") ? node.get("name").asText() : "Untitled Recipe";
        String desc = node.hasNonNull("description") ? node.get("description").asText() : "";
        String slug = node.hasNonNull("slug") ? node.get("slug").asText() : id;
        String prepTime = node.hasNonNull("prepTime") ? node.get("prepTime").asText() : (node.hasNonNull("prep_time") ? node.get("prep_time").asText() : null);
        String cookTime = node.hasNonNull("performTime") ? node.get("performTime").asText() : (node.hasNonNull("cookTime") ? node.get("cookTime").asText() : null);

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
        return recipeMap;
    }

    private Map<String, Object> toRecipeMap(CachedRecipe cr) {
        if (cr == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", cr.getId());
        map.put("name", cr.getName());
        map.put("description", cr.getDescription() != null ? cr.getDescription() : "");
        map.put("source", cr.getSource() != null ? cr.getSource() : "Mealie");
        map.put("slug", cr.getSlug());
        
        String recipeUrl = cr.getRecipeUrl();
        try {
            SettingsDto settings = settingsService.getRawSettings();
            if (settings != null) {
                String publicBaseUrl = (settings.mealieFrontendUrl() != null && !settings.mealieFrontendUrl().isBlank())
                        ? settings.mealieFrontendUrl().trim().replaceAll("/+$", "")
                        : (settings.mealieBaseUrl() != null && !settings.mealieBaseUrl().isBlank() ? settings.mealieBaseUrl().trim().replaceAll("/+$", "") : null);
                if (publicBaseUrl != null && cr.getSlug() != null && !cr.getSlug().isBlank()) {
                    recipeUrl = publicBaseUrl + "/recipes/" + cr.getSlug();
                }
            }
        } catch (Exception e) {
            // Ignore if settings service is unavailable
        }
        map.put("recipeUrl", recipeUrl);
        if (cr.getPrepTime() != null) map.put("prepTime", cr.getPrepTime());
        if (cr.getCookTime() != null) map.put("cookTime", cr.getCookTime());

        List<String> ings = Collections.emptyList();
        if (cr.getIngredientsJson() != null && !cr.getIngredientsJson().isBlank()) {
            try {
                ings = objectMapper.readValue(cr.getIngredientsJson(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse cached ingredients for slug '{}': {}", cr.getSlug(), e.getMessage());
            }
        }
        map.put("ingredients", ings);
        return map;
    }

    private void persistToDb(Map<String, Object> recipeMap, String dateUpdated) {
        if (cachedRecipeRepository == null || recipeMap == null) return;
        try {
            String slug = (String) recipeMap.get("slug");
            String id = (String) recipeMap.get("id");
            if (id == null || id.isBlank()) {
                id = (slug != null && !slug.isBlank()) ? slug : UUID.randomUUID().toString();
            }

            if (slug != null) {
                Optional<CachedRecipe> existing = cachedRecipeRepository.findBySlug(slug);
                if (existing.isPresent()) {
                    id = existing.get().getId();
                }
            }

            String name = (String) recipeMap.get("name");
            String desc = (String) recipeMap.get("description");
            String source = (String) recipeMap.get("source");
            String recipeUrl = (String) recipeMap.get("recipeUrl");
            String prepTime = (String) recipeMap.get("prepTime");
            String cookTime = (String) recipeMap.get("cookTime");

            @SuppressWarnings("unchecked")
            List<String> ings = (List<String>) recipeMap.get("ingredients");
            String ingsJson = (ings != null && !ings.isEmpty()) ? objectMapper.writeValueAsString(ings) : "[]";

            CachedRecipe cr = new CachedRecipe(
                    id, slug, name, desc, source, recipeUrl, prepTime, cookTime,
                    dateUpdated, ingsJson, System.currentTimeMillis()
            );
            cachedRecipeRepository.save(cr);
        } catch (Exception e) {
            log.warn("Could not persist recipe '{}' to SQLite: {}", recipeMap.get("name"), e.getMessage());
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
