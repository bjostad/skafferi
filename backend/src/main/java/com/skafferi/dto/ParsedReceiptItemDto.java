package com.skafferi.dto;

import java.time.LocalDate;

public record ParsedReceiptItemDto(
        String rawText,
        String cleanName,
        String brand,
        double quantity,
        String unit,
        Double unitPrice,
        Double totalPrice,
        String barcode,
        String suggestedCategoryId,
        String suggestedLocationId,
        String matchedItemId,
        String imageUrl,
        LocalDate estimatedExpirationDate,
        boolean selected
) {}
