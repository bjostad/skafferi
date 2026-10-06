package com.skafferi.service.parser;

import com.skafferi.dto.ReceiptParseResult;

public interface ReceiptParserStrategy {

    /**
     * Unique identifier for this parser (e.g. "kroger", "generic")
     */
    String getProviderId();

    /**
     * Display name for UI selector
     */
    String getDisplayName();

    /**
     * Inspects raw text to determine if this parser can handle it
     */
    boolean canParse(String rawText);

    /**
     * Parses the raw receipt text/html/json into normalized result
     */
    ReceiptParseResult parse(String rawText);
}
