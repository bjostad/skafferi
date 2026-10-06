package com.skafferi.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReceiptParseResult(
        String storeName,
        String sourceParser,
        LocalDateTime transactionDate,
        Double totalAmount,
        List<ParsedReceiptItemDto> items
) {}
