package com.skafferi.dto;

public record BarcodeLookupResult(
        boolean found,
        String barcode,
        String name,
        String brand,
        String imageUrl,
        String categorySuggestion,
        String defaultUnit,
        String source, // "LOCAL", "OPEN_FOOD_FACTS", "KROGER", "NOT_FOUND"
        String existingItemId,
        String packageSize,
        Double currentQuantity
) {}
