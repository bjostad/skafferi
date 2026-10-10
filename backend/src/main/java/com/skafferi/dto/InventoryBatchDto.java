package com.skafferi.dto;

import java.time.LocalDate;

public record InventoryBatchDto(
        String id,
        String itemId,
        String itemName,
        String locationId,
        String locationName,
        double quantity,
        String unit,
        LocalDate expirationDate,
        LocalDate openedDate,
        LocalDate purchasedDate,
        String note,
        String barcode,
        Double unitPrice,
        String store
) {
    public InventoryBatchDto(
            String id,
            String itemId,
            String itemName,
            String locationId,
            String locationName,
            double quantity,
            String unit,
            LocalDate expirationDate,
            LocalDate openedDate,
            LocalDate purchasedDate,
            String note,
            String barcode
    ) {
        this(id, itemId, itemName, locationId, locationName, quantity, unit, expirationDate, openedDate, purchasedDate, note, barcode, null, null);
    }
}
