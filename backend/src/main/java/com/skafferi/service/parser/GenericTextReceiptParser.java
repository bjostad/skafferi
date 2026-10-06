package com.skafferi.service.parser;

import com.skafferi.dto.ParsedReceiptItemDto;
import com.skafferi.dto.ReceiptParseResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GenericTextReceiptParser implements ReceiptParserStrategy {

    private static final Pattern PRICE_PATTERN = Pattern.compile("(?i)(?:\\$|\\b)(\\d+\\.\\d{2})(?:\\s*[FTB])?$");
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(?i)(?:(\\d+(?:\\.\\d+)?)\\s*(?:@|x|ea|pk|lb|oz|ct|count))|(\\d+)\\s+@");

    @Override
    public String getProviderId() {
        return "generic";
    }

    @Override
    public String getDisplayName() {
        return "Generic Grocery Receipt";
    }

    @Override
    public boolean canParse(String rawText) {
        // Fallback parser: matches any text with lines
        return rawText != null && !rawText.isBlank();
    }

    @Override
    public ReceiptParseResult parse(String rawText) {
        List<ParsedReceiptItemDto> items = new ArrayList<>();
        Double totalAmount = null;
        LocalDateTime transactionDate = LocalDateTime.now();

        String[] lines = rawText.split("\\r?\\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            String lower = trimmed.toLowerCase();
            if (lower.startsWith("total") || lower.startsWith("balance") || lower.startsWith("order total")) {
                Matcher pm = Pattern.compile("(\\d+\\.\\d{2})").matcher(trimmed);
                if (pm.find()) {
                    try {
                        totalAmount = Double.parseDouble(pm.group(1));
                    } catch (Exception ignored) {}
                }
                continue;
            }

            if (lower.startsWith("tax") || lower.startsWith("subtotal") || lower.startsWith("savings") ||
                lower.startsWith("change") || lower.startsWith("cash") || lower.startsWith("visa") ||
                lower.startsWith("debit") || lower.startsWith("store:") || lower.startsWith("date:") ||
                trimmed.length() < 3) {
                continue;
            }

            Double price = null;
            String textWithoutPrice = trimmed;
            Matcher priceMatcher = PRICE_PATTERN.matcher(trimmed);
            if (priceMatcher.find()) {
                try {
                    price = Double.parseDouble(priceMatcher.group(1));
                    textWithoutPrice = trimmed.substring(0, priceMatcher.start()).trim();
                } catch (Exception ignored) {}
            }

            double quantity = 1.0;
            String unit = "count";
            Matcher qtyMatcher = QUANTITY_PATTERN.matcher(textWithoutPrice);
            if (qtyMatcher.find()) {
                String qtyStr = qtyMatcher.group(1) != null ? qtyMatcher.group(1) : qtyMatcher.group(2);
                try {
                    quantity = Double.parseDouble(qtyStr);
                } catch (Exception ignored) {}
            }

            String cleanName = textWithoutPrice.replaceAll("^[0-9\\*\\-\\.\\s]+", "").trim();
            if (cleanName.length() < 2) continue;

            items.add(new ParsedReceiptItemDto(
                    trimmed,
                    cleanName,
                    null,
                    quantity,
                    unit,
                    price,
                    price,
                    null,
                    "cat-canned",
                    "loc-pantry",
                    null,
                    null,
                    LocalDate.now().plusMonths(3),
                    true
            ));
        }

        return new ReceiptParseResult(
                "Grocery Store",
                "generic",
                transactionDate,
                totalAmount,
                items
        );
    }
}
