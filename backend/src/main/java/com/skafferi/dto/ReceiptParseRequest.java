package com.skafferi.dto;

public record ReceiptParseRequest(
        String rawText,
        String providerId // "auto", "kroger", "generic"
) {}
