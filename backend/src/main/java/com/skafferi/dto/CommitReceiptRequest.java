package com.skafferi.dto;

import java.time.LocalDate;
import java.util.List;

public record CommitReceiptRequest(
        String storeName,
        List<CommitReceiptItem> items
) {
    public record CommitReceiptItem(
            String itemId, // Optional existing item ID
            String name,
            String brand,
            String categoryId,
            String locationId,
            double quantity,
            String unit,
            LocalDate expirationDate,
            Double unitPrice,
            String barcode,
            String imageUrl
    ) {}
}
