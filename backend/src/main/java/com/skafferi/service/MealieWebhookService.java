package com.skafferi.service;

import com.skafferi.domain.ExternalMapping;
import com.skafferi.domain.Item;
import com.skafferi.dto.MealieWebhookPayload;
import com.skafferi.repository.ExternalMappingRepository;
import com.skafferi.repository.ItemRepository;
import com.skafferi.util.IngredientMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MealieWebhookService {

    private static final Logger log = LoggerFactory.getLogger(MealieWebhookService.class);

    private final ItemRepository itemRepository;
    private final ExternalMappingRepository mappingRepository;
    private final InventoryService inventoryService;
    private final SettingsService settingsService;
    private final PendingMealService pendingMealService;

    public MealieWebhookService(ItemRepository itemRepository,
                                ExternalMappingRepository mappingRepository,
                                InventoryService inventoryService,
                                SettingsService settingsService,
                                PendingMealService pendingMealService) {
        this.itemRepository = itemRepository;
        this.mappingRepository = mappingRepository;
        this.inventoryService = inventoryService;
        this.settingsService = settingsService;
        this.pendingMealService = pendingMealService;
    }

    @Transactional
    public void processMealCooked(MealieWebhookPayload payload) {
        log.info("Processing meal cooked webhook from Mealie: '{}' (Servings: {})",
                payload.recipeName(), payload.servings());

        if (payload.ingredients() == null || payload.ingredients().isEmpty()) {
            log.info("No ingredients provided in webhook payload for '{}'", payload.recipeName());
            return;
        }

        // If auto-deduct is disabled (default), queue meal for user review & reconciliation
        if (!settingsService.isMealieAutoDeduct()) {
            pendingMealService.queueMealFromWebhook(payload);
            log.info("Queued meal '{}' for pending user reconciliation review", payload.recipeName());
            return;
        }

        for (MealieWebhookPayload.MealieIngredient ingredient : payload.ingredients()) {
            String foodName = ingredient.foodName() != null && !ingredient.foodName().isBlank()
                    ? ingredient.foodName()
                    : ingredient.rawText();

            if (foodName == null || foodName.isBlank()) continue;

            double quantity = ingredient.quantity() != null && ingredient.quantity() > 0
                    ? ingredient.quantity()
                    : 1.0;

            Optional<Item> matchedItem = findMatchingItem(foodName);
            if (matchedItem.isPresent()) {
                Item item = matchedItem.get();
                log.info("Deducting {} {} of '{}' from pantry for Mealie recipe '{}'",
                        quantity, item.getDefaultUnit(), item.getName(), payload.recipeName());
                inventoryService.deductFifo(item, quantity);
            } else {
                log.debug("No matching pantry item found for Mealie ingredient: '{}'", foodName);
            }
        }
    }

    private Optional<Item> findMatchingItem(String foodName) {
        // 1. Check external mappings
        Optional<ExternalMapping> mapping = mappingRepository.findBySystemTypeAndExternalKey("mealie", foodName.toLowerCase().trim());
        if (mapping.isPresent()) {
            return Optional.of(mapping.get().getItem());
        }

        // 2. Exact match on name
        Optional<Item> exact = itemRepository.findByNameIgnoreCase(foodName.trim());
        if (exact.isPresent()) {
            return exact;
        }

        // 3. Partial match (item name contains foodName or foodName contains item name)
        var all = itemRepository.findAll();
        for (Item item : all) {
            String iName = item.getName().toLowerCase();
            String fName = foodName.toLowerCase();
            if (iName.contains(fName) || fName.contains(iName)) {
                return Optional.of(item);
            }
        }

        // 4. Fallback: Tiered fuzzy matching only after simple match isn't found
        for (Item item : all) {
            if (IngredientMatcher.matches(foodName, item.getName())) {
                log.info("Fuzzy matched Mealie ingredient '{}' to pantry item '{}'", foodName, item.getName());
                return Optional.of(item);
            }
        }

        return Optional.empty();
    }
}
