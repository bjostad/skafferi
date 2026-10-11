package com.skafferi.dto;

import java.util.List;

public record ReconcileMealRequestDto(
        List<ReconcileItemDto> items
) {
    public record ReconcileItemDto(
            String pendingItemId,
            String itemId,
            Double deductQuantity,
            boolean selected,
            boolean rememberMapping
    ) {}
}
