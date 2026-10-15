package com.skafferi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.skafferi.domain.ExternalMapping;
import com.skafferi.domain.Item;
import com.skafferi.domain.PendingCookedMeal;
import com.skafferi.domain.PendingMealItem;
import com.skafferi.dto.MealieWebhookPayload;
import com.skafferi.dto.PendingMealDto;
import com.skafferi.dto.ReconcileMealRequestDto;
import com.skafferi.repository.ExternalMappingRepository;
import com.skafferi.repository.InventoryBatchRepository;
import com.skafferi.repository.ItemRepository;
import com.skafferi.repository.PendingCookedMealRepository;
import com.skafferi.repository.PendingMealItemRepository;
import com.skafferi.util.IngredientMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PendingMealService {

    private static final Logger log = LoggerFactory.getLogger(PendingMealService.class);
    private static final Pattern TEXT_ING_PATTERN = Pattern.compile("^([0-9]+(?:/[0-9]+|\\.[0-9]+)?)\\s*([a-zA-Z]+)?\\s*(.*)$");

    private final PendingCookedMealRepository mealRepository;
    private final PendingMealItemRepository mealItemRepository;
    private final ItemRepository itemRepository;
    private final InventoryBatchRepository batchRepository;
    private final ExternalMappingRepository mappingRepository;
    private final InventoryService inventoryService;
    private final SettingsService settingsService;

    public PendingMealService(PendingCookedMealRepository mealRepository,
                              PendingMealItemRepository mealItemRepository,
                              ItemRepository itemRepository,
                              InventoryBatchRepository batchRepository,
                              ExternalMappingRepository mappingRepository,
                              InventoryService inventoryService,
                              @org.springframework.beans.factory.annotation.Autowired(required = false) SettingsService settingsService) {
        this.mealRepository = mealRepository;
        this.mealItemRepository = mealItemRepository;
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.mappingRepository = mappingRepository;
        this.inventoryService = inventoryService;
        this.settingsService = settingsService;
    }

    @Transactional
    public PendingCookedMeal queueMealFromRecipeJson(JsonNode root) {
        if (root == null || root.isNull() || root.isEmpty()) {
            throw new IllegalArgumentException("Recipe payload cannot be empty");
        }

        // Unwrap if nested (e.g. { "recipe": { ... } } or { "data": { ... } })
        if (root.hasNonNull("recipe") && root.get("recipe").isObject()) {
            root = root.get("recipe");
        } else if (root.hasNonNull("data") && root.get("data").isObject()) {
            root = root.get("data");
        }

        String recipeName = parseRecipeName(root);
        String recipeId = parseRecipeId(root);
        Double servings = parseServings(root);

        log.info("Processing recipe action from Mealie: '{}' (ID: {}, Servings: {})",
                recipeName, recipeId, servings);

        PendingCookedMeal meal = new PendingCookedMeal(recipeName, recipeId, servings);
        meal.setCookedAt(LocalDateTime.now());
        meal.setStatus("PENDING");

        List<Item> allItems = itemRepository.findAll();

        JsonNode ingredientsNode = null;
        if (root.has("recipeIngredient") && root.get("recipeIngredient").isArray()) {
            ingredientsNode = root.get("recipeIngredient");
        } else if (root.has("ingredients") && root.get("ingredients").isArray()) {
            ingredientsNode = root.get("ingredients");
        } else if (root.has("recipe_ingredient") && root.get("recipe_ingredient").isArray()) {
            ingredientsNode = root.get("recipe_ingredient");
        }

        if (ingredientsNode != null) {
            for (JsonNode ingNode : ingredientsNode) {
                // Skip section headers in Mealie: { "title": "For the crust", "isFood": false }
                if (ingNode.has("isFood") && !ingNode.get("isFood").asBoolean()) {
                    boolean hasFood = (ingNode.hasNonNull("food") && !ingNode.path("food").asText().isBlank())
                            || (ingNode.hasNonNull("foodName") && !ingNode.path("foodName").asText().isBlank());
                    if (!hasFood) {
                        continue;
                    }
                }

                String foodName = null;
                String rawText = null;
                String unit = "";
                Double quantity = 1.0;

                if (ingNode.isTextual()) {
                    rawText = ingNode.asText().trim();
                    ParsedIngredient parsed = parseIngredientText(rawText);
                    foodName = parsed.foodName();
                    quantity = parsed.quantity();
                    unit = parsed.unit();
                } else if (ingNode.isObject()) {
                    // 1. Food name
                    if (ingNode.path("food").isObject()) {
                        foodName = ingNode.path("food").path("name").asText(null);
                    } else if (ingNode.path("food").isTextual()) {
                        foodName = ingNode.path("food").asText(null);
                    } else if (ingNode.hasNonNull("foodName")) {
                        foodName = ingNode.path("foodName").asText(null);
                    }

                    // 2. Display / Raw text
                    if (ingNode.hasNonNull("display") && !ingNode.path("display").asText().isBlank()) {
                        rawText = ingNode.path("display").asText().trim();
                    } else if (ingNode.hasNonNull("originalText") && !ingNode.path("originalText").asText().isBlank()) {
                        rawText = ingNode.path("originalText").asText().trim();
                    } else if (ingNode.hasNonNull("rawText") && !ingNode.path("rawText").asText().isBlank()) {
                        rawText = ingNode.path("rawText").asText().trim();
                    }

                    // 3. Unit
                    if (ingNode.path("unit").isObject()) {
                        unit = ingNode.path("unit").path("name").asText("");
                        if (unit.isBlank()) {
                            unit = ingNode.path("unit").path("abbreviation").asText("");
                        }
                    } else if (ingNode.path("unit").isTextual()) {
                        unit = ingNode.path("unit").asText("");
                    }

                    // 4. Quantity
                    if (ingNode.hasNonNull("quantity")) {
                        if (ingNode.path("quantity").isNumber()) {
                            quantity = ingNode.path("quantity").asDouble();
                        } else if (ingNode.path("quantity").isTextual()) {
                            try {
                                quantity = parseQuantityString(ingNode.path("quantity").asText());
                            } catch (Exception ignored) {}
                        }
                    }

                    // Fallback to note if foodName is still empty
                    if ((foodName == null || foodName.isBlank()) && ingNode.hasNonNull("note") && !ingNode.path("note").asText().isBlank()) {
                        foodName = ingNode.path("note").asText().trim();
                    }
                }

                if (foodName == null || foodName.isBlank()) {
                    if (rawText != null && !rawText.isBlank()) {
                        foodName = rawText;
                    } else {
                        continue;
                    }
                }

                if (rawText == null || rawText.isBlank()) {
                    rawText = (quantity != null && quantity > 0 ? quantity + " " : "") +
                            (!unit.isBlank() ? unit + " " : "") +
                            foodName;
                }

                if (quantity == null || quantity <= 0) {
                    quantity = 1.0;
                }

                MatchResolution match = resolveBestMatch(foodName, allItems);

                PendingMealItem item = new PendingMealItem();
                item.setRawIngredientText(rawText);
                item.setFoodName(foodName);
                item.setOriginalQuantity(quantity);
                item.setOriginalUnit(unit != null ? unit : "");
                item.setMatchedItem(match.item());
                item.setMatchConfidence(match.confidence());
                item.setDeductQuantity(quantity);
                item.setDeductUnit(match.item() != null ? match.item().getDefaultUnit() : (unit != null ? unit : ""));
                item.setSelected(match.item() != null);

                meal.addItem(item);
            }
        }

        PendingCookedMeal saved = mealRepository.save(meal);
        log.info("Saved recipe action meal '{}' (ID: {}) with {} ingredients into queue",
                saved.getRecipeName(), saved.getId(), saved.getItems().size());

        if (settingsService != null && settingsService.isMealieAutoDeduct()) {
            log.info("Mealie auto-deduct is enabled; reconciling meal '{}' immediately", saved.getId());
            reconcileMeal(saved.getId(), null);
        }

        return saved;
    }

    private String parseRecipeName(JsonNode root) {
        if (root.hasNonNull("name") && !root.path("name").asText().isBlank()) {
            return root.path("name").asText().trim();
        }
        if (root.hasNonNull("recipeName") && !root.path("recipeName").asText().isBlank()) {
            return root.path("recipeName").asText().trim();
        }
        if (root.hasNonNull("title") && !root.path("title").asText().isBlank()) {
            return root.path("title").asText().trim();
        }
        return "Mealie Recipe";
    }

    private String parseRecipeId(JsonNode root) {
        if (root.hasNonNull("id") && !root.path("id").asText().isBlank()) {
            return root.path("id").asText().trim();
        }
        if (root.hasNonNull("slug") && !root.path("slug").asText().isBlank()) {
            return root.path("slug").asText().trim();
        }
        if (root.hasNonNull("recipeId") && !root.path("recipeId").asText().isBlank()) {
            return root.path("recipeId").asText().trim();
        }
        return null;
    }

    private Double parseServings(JsonNode root) {
        JsonNode node = null;
        if (root.hasNonNull("recipeYield")) node = root.get("recipeYield");
        else if (root.hasNonNull("yieldQuantity")) node = root.get("yieldQuantity");
        else if (root.hasNonNull("servings")) node = root.get("servings");
        else if (root.hasNonNull("yield")) node = root.get("yield");

        if (node == null) return 1.0;
        if (node.isNumber()) {
            double val = node.asDouble();
            return val > 0 ? val : 1.0;
        }
        if (node.isTextual()) {
            String text = node.asText().trim();
            Matcher m = Pattern.compile("(\\d+(\\.\\d+)?)").matcher(text);
            if (m.find()) {
                try {
                    double val = Double.parseDouble(m.group(1));
                    if (val > 0) return val;
                } catch (NumberFormatException ignored) {}
            }
        }
        return 1.0;
    }

    private Double parseQuantityString(String str) {
        if (str == null || str.isBlank()) return null;
        str = str.trim();
        if (str.contains("/")) {
            String[] parts = str.split("/");
            if (parts.length == 2) {
                try {
                    double num = Double.parseDouble(parts[0].trim());
                    double den = Double.parseDouble(parts[1].trim());
                    if (den != 0) return num / den;
                } catch (NumberFormatException ignored) {}
            }
        }
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ParsedIngredient parseIngredientText(String text) {
        if (text == null || text.isBlank()) {
            return new ParsedIngredient("", 1.0, "");
        }
        text = text.trim();
        Matcher m = TEXT_ING_PATTERN.matcher(text);
        if (m.find()) {
            String qtyStr = m.group(1);
            String possibleUnit = m.group(2) != null ? m.group(2) : "";
            String remainder = m.group(3) != null ? m.group(3) : "";

            Double qty = parseQuantityString(qtyStr);
            if (qty == null || qty <= 0) qty = 1.0;

            if (!remainder.isBlank()) {
                return new ParsedIngredient(remainder.trim(), qty, possibleUnit);
            } else if (!possibleUnit.isBlank()) {
                return new ParsedIngredient(possibleUnit.trim(), qty, "");
            }
        }
        return new ParsedIngredient(text, 1.0, "");
    }

    private record ParsedIngredient(String foodName, Double quantity, String unit) {}

    @Transactional
    public PendingCookedMeal queueMealFromWebhook(MealieWebhookPayload payload) {
        log.info("Queueing cooked meal from Mealie into pending review: '{}' (Servings: {})",
                payload.recipeName(), payload.servings());

        PendingCookedMeal meal = new PendingCookedMeal(
                payload.recipeName() != null ? payload.recipeName() : "Cooked Recipe",
                null,
                payload.servings() != null ? payload.servings() : 1.0
        );
        meal.setCookedAt(LocalDateTime.now());
        meal.setStatus("PENDING");

        List<Item> allItems = itemRepository.findAll();

        if (payload.ingredients() != null) {
            for (MealieWebhookPayload.MealieIngredient ingredient : payload.ingredients()) {
                String foodName = ingredient.foodName() != null ? ingredient.foodName().trim() : "";
                if (foodName.isEmpty()) continue;

                double quantity = ingredient.quantity() != null && ingredient.quantity() > 0
                        ? ingredient.quantity()
                        : 1.0;
                String unit = ingredient.unit() != null ? ingredient.unit() : "";

                MatchResolution match = resolveBestMatch(foodName, allItems);

                PendingMealItem item = new PendingMealItem();
                item.setRawIngredientText(foodName);
                item.setFoodName(foodName);
                item.setOriginalQuantity(quantity);
                item.setOriginalUnit(unit);
                item.setMatchedItem(match.item());
                item.setMatchConfidence(match.confidence());
                item.setDeductQuantity(quantity);
                item.setDeductUnit(match.item() != null ? match.item().getDefaultUnit() : unit);
                item.setSelected(match.item() != null);

                meal.addItem(item);
            }
        }

        return mealRepository.save(meal);
    }

    @Transactional(readOnly = true)
    public List<PendingMealDto> getPendingMeals() {
        List<PendingCookedMeal> meals = mealRepository.findByStatusOrderByCookedAtDesc("PENDING");
        return meals.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        return mealRepository.countByStatus("PENDING");
    }

    @Transactional
    public PendingMealDto reconcileMeal(String mealId, ReconcileMealRequestDto request) {
        PendingCookedMeal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new NoSuchElementException("Pending meal not found: " + mealId));

        Map<String, ReconcileMealRequestDto.ReconcileItemDto> requestMap = new HashMap<>();
        if (request != null && request.items() != null) {
            for (ReconcileMealRequestDto.ReconcileItemDto rItem : request.items()) {
                requestMap.put(rItem.pendingItemId(), rItem);
            }
        }

        for (PendingMealItem item : meal.getItems()) {
            ReconcileMealRequestDto.ReconcileItemDto itemReq = requestMap.get(item.getId());

            boolean shouldDeduct = itemReq != null ? itemReq.selected() : item.isSelected();
            String targetItemId = itemReq != null && itemReq.itemId() != null ? itemReq.itemId() : (item.getMatchedItem() != null ? item.getMatchedItem().getId() : null);
            Double deductQty = itemReq != null && itemReq.deductQuantity() != null ? itemReq.deductQuantity() : item.getDeductQuantity();

            if (shouldDeduct && targetItemId != null && deductQty != null && deductQty > 0) {
                Optional<Item> pantryItem = itemRepository.findById(targetItemId);
                if (pantryItem.isPresent()) {
                    Item pItem = pantryItem.get();
                    inventoryService.deductFifo(pItem, deductQty);
                    log.info("Deducted {} {} of '{}' for reconciled meal '{}'",
                            deductQty, pItem.getDefaultUnit(), pItem.getName(), meal.getRecipeName());

                    // Remember mapping if requested
                    if (itemReq != null && itemReq.rememberMapping()) {
                        rememberMapping(item.getFoodName(), pItem);
                    }
                }
            }

            // Update item record state
            if (targetItemId != null) {
                itemRepository.findById(targetItemId).ifPresent(item::setMatchedItem);
            }
            if (deductQty != null) {
                item.setDeductQuantity(deductQty);
            }
            item.setSelected(shouldDeduct);
            mealItemRepository.save(item);
        }

        meal.setStatus("COMMITTED");
        PendingCookedMeal saved = mealRepository.save(meal);
        return mapToDto(saved);
    }

    @Transactional
    public List<PendingMealDto> reconcileAllPending() {
        List<PendingCookedMeal> meals = mealRepository.findByStatusOrderByCookedAtDesc("PENDING");
        List<PendingMealDto> results = new ArrayList<>();
        for (PendingCookedMeal meal : meals) {
            results.add(reconcileMeal(meal.getId(), null));
        }
        return results;
    }

    @Transactional
    public void dismissMeal(String mealId) {
        PendingCookedMeal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new NoSuchElementException("Pending meal not found: " + mealId));
        meal.setStatus("DISMISSED");
        mealRepository.save(meal);
        log.info("Dismissed pending cooked meal: '{}' ({})", meal.getRecipeName(), mealId);
    }

    private MatchResolution resolveBestMatch(String foodName, List<Item> allItems) {
        String cleanFood = foodName.toLowerCase().trim();

        // 1. External mapping (Saved user preference)
        Optional<ExternalMapping> mapping = mappingRepository.findBySystemTypeAndExternalKey("mealie", cleanFood);
        if (mapping.isPresent()) {
            return new MatchResolution(mapping.get().getItem(), "MANUAL");
        }

        // 2. Exact name match
        for (Item item : allItems) {
            if (item.getName().equalsIgnoreCase(foodName.trim())) {
                return new MatchResolution(item, "EXACT");
            }
        }

        // 3. Substring containment
        for (Item item : allItems) {
            String iName = item.getName().toLowerCase();
            if (iName.contains(cleanFood) || cleanFood.contains(iName)) {
                return new MatchResolution(item, "SUBSTRING");
            }
        }

        // 4. Token & prefix match
        for (Item item : allItems) {
            IngredientMatcher.MatchConfidence conf = IngredientMatcher.evaluateMatch(foodName, item.getName());
            if (conf == IngredientMatcher.MatchConfidence.TOKEN) {
                return new MatchResolution(item, "TOKEN");
            }
        }

        // 5. Fuzzy match fallback
        for (Item item : allItems) {
            IngredientMatcher.MatchConfidence conf = IngredientMatcher.evaluateMatch(foodName, item.getName());
            if (conf == IngredientMatcher.MatchConfidence.FUZZY) {
                return new MatchResolution(item, "FUZZY");
            }
        }

        return new MatchResolution(null, "UNMATCHED");
    }

    private void rememberMapping(String foodName, Item item) {
        String key = foodName.toLowerCase().trim();
        Optional<ExternalMapping> existing = mappingRepository.findBySystemTypeAndExternalKey("mealie", key);
        if (existing.isPresent()) {
            ExternalMapping m = existing.get();
            m.setItem(item);
            mappingRepository.save(m);
        } else {
            ExternalMapping m = new ExternalMapping(item, "mealie", key);
            mappingRepository.save(m);
        }
        log.info("Saved Mealie external mapping: '{}' -> '{}'", key, item.getName());
    }

    private PendingMealDto mapToDto(PendingCookedMeal meal) {
        List<PendingMealDto.PendingMealItemDto> itemDtos = new ArrayList<>();
        if (meal.getItems() != null) {
            for (PendingMealItem item : meal.getItems()) {
                Double inStock = null;
                String matchedId = null;
                String matchedName = null;
                String matchedUnit = null;

                if (item.getMatchedItem() != null) {
                    matchedId = item.getMatchedItem().getId();
                    matchedName = item.getMatchedItem().getName();
                    matchedUnit = item.getMatchedItem().getDefaultUnit();
                    inStock = batchRepository.getTotalQuantityForItem(matchedId);
                    if (inStock == null) inStock = 0.0;
                }

                itemDtos.add(new PendingMealDto.PendingMealItemDto(
                        item.getId(),
                        item.getRawIngredientText(),
                        item.getFoodName(),
                        item.getOriginalQuantity(),
                        item.getOriginalUnit(),
                        matchedId,
                        matchedName,
                        matchedUnit,
                        inStock,
                        item.getDeductQuantity(),
                        item.getDeductUnit(),
                        item.isSelected(),
                        item.getMatchConfidence()
                ));
            }
        }

        return new PendingMealDto(
                meal.getId(),
                meal.getRecipeName(),
                meal.getRecipeId(),
                meal.getServings(),
                meal.getStatus(),
                meal.getCookedAt(),
                itemDtos
        );
    }

    private record MatchResolution(Item item, String confidence) {}
}
