package com.skafferi.dto;

import com.skafferi.domain.Category;
import com.skafferi.domain.Location;

import java.time.LocalDate;
import java.util.List;

public record PantryItemSummaryDto(
        String id,
        String name,
        String brand,
        Category category,
        Location defaultLocation,
        String defaultUnit,
        double minThreshold,
        double restockQuantity,
        String defaultPurchaseAmount,
        String imageUrl,
        String barcode,
        boolean autoAddToBring,
        boolean perishable,
        String packageSize,
        String notes,
        double totalQuantity,
        boolean isLowStock,
        boolean isOutOfStock,
        Integer daysUntilEarliestExpiry,
        LocalDate earliestExpiryDate,
        String expiryStatus, // "FRESH", "EXPIRING_SOON", "EXPIRED", "NO_EXPIRY"
        boolean freshCheckNeeded,
        Long daysInStorage,
        List<InventoryBatchDto> batches
) {}
