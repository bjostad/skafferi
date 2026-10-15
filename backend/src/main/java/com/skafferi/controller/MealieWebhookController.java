package com.skafferi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.skafferi.domain.PendingCookedMeal;
import com.skafferi.service.PendingMealService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/mealie")
public class MealieWebhookController {

    private final PendingMealService pendingMealService;

    public MealieWebhookController(PendingMealService pendingMealService) {
        this.pendingMealService = pendingMealService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody JsonNode payload) {
        PendingCookedMeal meal = pendingMealService.queueMealFromRecipeJson(payload);
        return ResponseEntity.ok(Map.of(
                "status", "processed",
                "recipe", meal.getRecipeName() != null ? meal.getRecipeName() : "unknown",
                "mealId", meal.getId(),
                "ingredientsCount", meal.getItems() != null ? meal.getItems().size() : 0
        ));
    }
}
