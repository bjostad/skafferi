package com.skafferi.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PendingMealDto(
        String id,
        String recipeName,
        String recipeId,
        Double servings,
        String status,
        LocalDateTime cookedAt,
        List<PendingMealItemDto> items
) {
    public record PendingMealItemDto(
            String id,
            String rawIngredientText,
            String foodName,
            Double originalQuantity,
            String originalUnit,
            String matchedItemId,
            String matchedItemName,
            String matchedItemUnit,
            Double matchedItemInStockQuantity,
            Double deductQuantity,
            String deductUnit,
            boolean selected,
            String matchConfidence
    ) {}
}
