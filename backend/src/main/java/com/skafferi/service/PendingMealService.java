package com.skafferi.service;

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
import java.util.stream.Collectors;

@Service
public class PendingMealService {

    private static final Logger log = LoggerFactory.getLogger(PendingMealService.class);

    private final PendingCookedMealRepository mealRepository;
    private final PendingMealItemRepository mealItemRepository;
    private final ItemRepository itemRepository;
    private final InventoryBatchRepository batchRepository;
    private final ExternalMappingRepository mappingRepository;
    private final InventoryService inventoryService;

    public PendingMealService(PendingCookedMealRepository mealRepository,
                              PendingMealItemRepository mealItemRepository,
                              ItemRepository itemRepository,
                              InventoryBatchRepository batchRepository,
                              ExternalMappingRepository mappingRepository,
                              InventoryService inventoryService) {
        this.mealRepository = mealRepository;
        this.mealItemRepository = mealItemRepository;
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.mappingRepository = mappingRepository;
        this.inventoryService = inventoryService;
    }

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
