package com.skafferi.dto;

import java.util.List;

public record NotificationSummaryDto(
        int totalAlertCount,
        int expiredCount,
        int expiringSoonCount,
        int freshCheckCount,
        int lowStockCount,
        List<PantryItemSummaryDto> expiredItems,
        List<PantryItemSummaryDto> expiringSoonItems,
        List<PantryItemSummaryDto> freshCheckItems,
        List<PantryItemSummaryDto> lowStockItems
) {}
