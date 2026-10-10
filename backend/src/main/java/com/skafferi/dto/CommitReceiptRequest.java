package com.skafferi.dto;

import java.time.LocalDate;
import java.util.List;

public record CommitReceiptRequest(
        String storeName,
        LocalDate transactionDate,
        List<CommitReceiptItem> items
) {
    public CommitReceiptRequest(String storeName, List<CommitReceiptItem> items) {
        this(storeName, null, items);
    }

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
            Double totalPrice,
            LocalDate purchasedDate,
            String store,
            String barcode,
            String imageUrl
    ) {
        public CommitReceiptItem(
                String itemId,
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
        ) {
            this(itemId, name, brand, categoryId, locationId, quantity, unit, expirationDate, unitPrice, null, null, null, barcode, imageUrl);
        }
    }
}
