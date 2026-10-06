package com.skafferi.dto;

import java.util.List;
import java.util.Map;

public record MealieWebhookPayload(
        String event, // e.g. "meal_cooked", "mealplan_entry"
        String recipeSlug,
        String recipeName,
        Double servings,
        List<MealieIngredient> ingredients,
        Map<String, Object> extra
) {
    public record MealieIngredient(
            String rawText,
            String foodName,
            Double quantity,
            String unit
    ) {}
}
