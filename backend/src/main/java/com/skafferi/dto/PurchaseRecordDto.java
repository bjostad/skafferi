package com.skafferi.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PurchaseRecordDto(
        String id,
        String itemId,
        String itemName,
        LocalDate purchasedDate,
        String store,
        Double quantity,
        String unit,
        Double unitPrice,
        Double totalPrice,
        String notes,
        String source,
        LocalDateTime createdAt
) {}
