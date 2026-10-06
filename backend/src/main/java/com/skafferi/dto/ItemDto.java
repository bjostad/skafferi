package com.skafferi.dto;

public record ItemDto(
        String id,
        String name,
        String brand,
        String categoryId,
        String categoryName,
        String defaultLocationId,
        String defaultLocationName,
        String defaultUnit,
        double minThreshold,
        double restockQuantity,
        String defaultPurchaseAmount,
        String imageUrl,
        String barcode,
        boolean autoAddToBring,
        boolean perishable,
        String packageSize,
        String notes
) {}
