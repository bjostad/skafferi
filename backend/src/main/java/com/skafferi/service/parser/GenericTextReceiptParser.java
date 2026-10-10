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
    private static final Pattern UPC_PATTERN = Pattern.compile("\\b(\\d{11,14})\\b");
    private static final Pattern DIGITAL_QTY_PATTERN = Pattern.compile("(?i)^(\\d+(?:\\.\\d+)?)\\s*x\\s*\\$?(\\d+\\.\\d{2})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(?i)(?:Order\\s+Date:|Date:)\\s*([A-Za-z]+)\\.?\\s*(\\d{1,2}),?\\s*(\\d{4})");

    private static final Pattern METADATA_LINE_PATTERN = Pattern.compile(
            "(?i)^\\s*(fred\\s+meyer|kroger|safeway|walmart|target|costco|trader\\s+joe'?s)?\\s*(logo|print|order\\s+type:?|order\\s+date:?|order\\s+number:?|loyalty\\s+card:?|member:?|rewards|total\\s+savings:?|order\\s+summary|original\\s+item\\s+total|item\\s+coupons?/sales?|other\\s+fees|sales?\\s+tax|order\\s+total|subtotal|balance|payment\\s+details|terminal\\s+id|visa\\s*\\d*|mastercard\\s*\\d*|item\\s+details|\\d+\\s+items?|bag\\s+fee|tax|change|cash|debit|credit).*$"
    );

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
        if (rawText == null || rawText.isBlank()) {
            return new ReceiptParseResult("Grocery Store", "generic", LocalDateTime.now(), null, List.of());
        }

        String lower = rawText.toLowerCase();
        if (lower.contains("upc:") || (lower.contains("order date:") && lower.contains("x $"))) {
            return parseBlockReceipt(rawText);
        }
        return parseLineByLineReceipt(rawText);
    }

    private ReceiptParseResult parseBlockReceipt(String rawText) {
        List<ParsedReceiptItemDto> items = new ArrayList<>();
        Double totalAmount = null;
        LocalDateTime transactionDate = LocalDateTime.now();
        String storeName = "Grocery Store";

        String[] lines = rawText.split("\\r?\\n");

        // 1. Detect store name
        for (int i = 0; i < Math.min(lines.length, 10); i++) {
            String l = lines[i].trim().toLowerCase();
            if (l.contains("fred meyer")) { storeName = "Fred Meyer"; break; }
            if (l.contains("kroger")) { storeName = "Kroger"; break; }
            if (l.contains("safeway")) { storeName = "Safeway"; break; }
            if (l.contains("walmart")) { storeName = "Walmart"; break; }
            if (l.contains("target")) { storeName = "Target"; break; }
            if (l.contains("trader joe")) { storeName = "Trader Joe's"; break; }
        }

        // 2. Detect transaction date
        Matcher dateMatcher = DATE_PATTERN.matcher(rawText);
        if (dateMatcher.find()) {
            try {
                String monthStr = dateMatcher.group(1).substring(0, 3).toLowerCase();
                int day = Integer.parseInt(dateMatcher.group(2));
                int year = Integer.parseInt(dateMatcher.group(3));
                int month = parseMonth(monthStr);
                transactionDate = LocalDate.of(year, month, day).atTime(12, 0);
            } catch (Exception ignored) {}
        }

        // 3. Detect total amount
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].trim();
            if (l.equalsIgnoreCase("Order Total") && i + 1 < lines.length) {
                Matcher pm = Pattern.compile("\\$?(\\d+\\.\\d{2})").matcher(lines[i + 1].trim());
                if (pm.find()) {
                    try { totalAmount = Double.parseDouble(pm.group(1)); } catch (Exception ignored) {}
                }
            } else if (l.toLowerCase().startsWith("order total") || l.toLowerCase().startsWith("total")) {
                Matcher pm = Pattern.compile("(\\d+\\.\\d{2})").matcher(l);
                if (pm.find()) {
                    try { totalAmount = Double.parseDouble(pm.group(1)); } catch (Exception ignored) {}
                }
            }
        }

        // 4. Find start of item details
        int startIndex = 0;
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.equalsIgnoreCase("Item Details") || trimmed.equalsIgnoreCase("Items")) {
                startIndex = i + 1;
                if (startIndex < lines.length && lines[startIndex].trim().toLowerCase().matches("\\d+\\s+items?")) {
                    startIndex++;
                }
                break;
            }
        }

        List<String> currentBlock = new ArrayList<>();
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String lowerLine = line.toLowerCase();
            // Stop delimiters at end of receipt items
            if (lowerLine.startsWith("other fees") || lowerLine.startsWith("payment details") ||
                lowerLine.startsWith("terminal id") || lowerLine.startsWith("payment") ||
                lowerLine.startsWith("subtotal") || lowerLine.startsWith("www.")) {
                break;
            }

            currentBlock.add(line);

            if (lowerLine.startsWith("upc:")) {
                ParsedReceiptItemDto parsed = parseBlockItem(currentBlock);
                if (parsed != null) {
                    items.add(parsed);
                }
                currentBlock.clear();
            }
        }

        return new ReceiptParseResult(storeName, "generic", transactionDate, totalAmount, items);
    }

    private ParsedReceiptItemDto parseBlockItem(List<String> block) {
        if (block.isEmpty()) return null;

        // Find the actual item name line (skip any leading noise lines)
        String nameLine = null;
        for (String line : block) {
            String trimmed = line.trim();
            if (isMetadataOrFeeLine(trimmed)) continue;
            if (trimmed.matches("^\\$?\\d+\\.\\d{2}$")) continue;
            if (DIGITAL_QTY_PATTERN.matcher(trimmed).find()) continue;
            if (trimmed.toLowerCase().startsWith("item coupon") || trimmed.startsWith("-")) continue;
            nameLine = trimmed;
            break;
        }

        if (nameLine == null || nameLine.length() < 2) return null;

        Double totalPrice = null;
        Double unitPrice = null;
        double quantity = 1.0;
        String barcode = null;

        for (String line : block) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase();

            // Total price for line (e.g. "$2.52" or "$0.00")
            if (totalPrice == null && trimmed.matches("^\\$?\\d+\\.\\d{2}$")) {
                try {
                    totalPrice = Double.parseDouble(trimmed.replace("$", "").trim());
                } catch (Exception ignored) {}
                continue;
            }

            // Quantity / Unit price: "2 x $1.26 each"
            Matcher qtyMatcher = DIGITAL_QTY_PATTERN.matcher(trimmed);
            if (qtyMatcher.find()) {
                try {
                    quantity = Double.parseDouble(qtyMatcher.group(1));
                    unitPrice = Double.parseDouble(qtyMatcher.group(2));
                } catch (Exception ignored) {}
                continue;
            }

            // UPC
            if (lower.startsWith("upc:")) {
                Matcher m = Pattern.compile("(\\d{10,14})").matcher(trimmed);
                if (m.find()) {
                    barcode = m.group(1);
                }
            }
        }

        if (unitPrice == null && totalPrice != null) {
            unitPrice = quantity > 0 ? totalPrice / quantity : totalPrice;
        }

        String cleanName = nameLine.replaceAll("[®™©]", "").replaceAll("\\s{2,}", " ").trim();

        return new ParsedReceiptItemDto(
                String.join("\n", block),
                cleanName,
                null,
                quantity,
                "count",
                unitPrice,
                totalPrice,
                barcode,
                "cat-canned",
                "loc-pantry",
                null,
                null,
                LocalDate.now().plusMonths(3),
                true
        );
    }

    private ReceiptParseResult parseLineByLineReceipt(String rawText) {
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

            // Skip metadata and non-item lines
            if (isMetadataOrFeeLine(trimmed) || trimmed.length() < 3) {
                continue;
            }

            // Skip discounts/coupons (e.g. "-$1.50" or lines starting with "-")
            if (trimmed.startsWith("-") || trimmed.contains("-$") || lower.contains("coupon") || lower.contains("discount")) {
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

            // Extract UPC if present
            String upc = null;
            Matcher upcMatcher = UPC_PATTERN.matcher(textWithoutPrice);
            if (upcMatcher.find()) {
                upc = upcMatcher.group(1);
                textWithoutPrice = textWithoutPrice.replace(upc, "").trim();
            }

            // MANDATORY: Single-line receipt item MUST have a price or a UPC code!
            if (price == null && upc == null) {
                continue;
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
                    upc,
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

    private boolean isMetadataOrFeeLine(String line) {
        if (line == null) return true;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return true;
        if (METADATA_LINE_PATTERN.matcher(trimmed).matches()) return true;
        String lower = trimmed.toLowerCase();
        return lower.startsWith("tax") || lower.startsWith("subtotal") || lower.startsWith("savings") ||
               lower.startsWith("change") || lower.startsWith("cash") || lower.startsWith("visa") ||
               lower.startsWith("debit") || lower.startsWith("store:") || lower.startsWith("date:") ||
               lower.startsWith("tel:") || lower.startsWith("phone:") || lower.startsWith("bag fee") ||
               lower.startsWith("other fees") || lower.startsWith("original item total") ||
               lower.matches(".*\\b\\d{5}(-\\d{4})?\\s*usa\\b.*");
    }

    private int parseMonth(String monthStr) {
        return switch (monthStr.toLowerCase()) {
            case "jan" -> 1;
            case "feb" -> 2;
            case "mar" -> 3;
            case "apr" -> 4;
            case "may" -> 5;
            case "jun" -> 6;
            case "jul" -> 7;
            case "aug" -> 8;
            case "sep" -> 9;
            case "oct" -> 10;
            case "nov" -> 11;
            case "dec" -> 12;
            default -> 1;
        };
    }
}
