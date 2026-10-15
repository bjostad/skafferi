package com.skafferi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.skafferi.domain.PendingCookedMeal;
import com.skafferi.service.PendingMealService;
import com.skafferi.service.RecipeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private static final Logger log = LoggerFactory.getLogger(RecipeController.class);

    private final RecipeService recipeService;
    private final PendingMealService pendingMealService;

    public RecipeController(RecipeService recipeService, PendingMealService pendingMealService) {
        this.recipeService = recipeService;
        this.pendingMealService = pendingMealService;
    }

    @PostMapping(value = {"/action", "/mealie-action"})
    public ResponseEntity<?> handleRecipeAction(@RequestBody JsonNode recipeJson) {
        log.info("Received recipe action POST request from Mealie");
        try {
            PendingCookedMeal meal = pendingMealService.queueMealFromRecipeJson(recipeJson);
            long matchedCount = meal.getItems() != null
                    ? meal.getItems().stream().filter(i -> i.getMatchedItem() != null).count()
                    : 0;

            return ResponseEntity.ok(Map.of(
                    "status", "queued_for_review",
                    "message", "Recipe successfully queued for review in Skafferi",
                    "mealId", meal.getId(),
                    "recipeName", meal.getRecipeName(),
                    "servings", meal.getServings(),
                    "ingredientsCount", meal.getItems() != null ? meal.getItems().size() : 0,
                    "matchedCount", matchedCount
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid recipe action payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to process recipe action: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Failed to process recipe action: " + e.getMessage()));
        }
    }

    @GetMapping("/matches")
    public ResponseEntity<List<Map<String, Object>>> getMatchingRecipes(
            @RequestParam(value = "refresh", required = false, defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(recipeService.getMatchingRecipes(refresh));
    }

    @PostMapping("/ai-zero-waste")
    public ResponseEntity<?> generateZeroWaste(@RequestBody(required = false) Map<String, List<String>> request) {
        try {
            List<String> itemIds = request != null ? request.get("itemIds") : null;
            return ResponseEntity.ok(recipeService.generateZeroWasteMeal(itemIds));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/missing-to-shopping")
    public ResponseEntity<Map<String, Object>> addMissingToShopping(@RequestBody Map<String, List<String>> request) {
        List<String> items = request != null ? request.get("items") : List.of();
        return ResponseEntity.ok(recipeService.addMissingToShoppingList(items));
    }
}
