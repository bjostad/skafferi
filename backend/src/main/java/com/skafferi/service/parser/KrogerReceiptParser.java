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
public class KrogerReceiptParser implements ReceiptParserStrategy {

    // Regex to detect price like $3.49 or 3.49 at end of line or after item name
    private static final Pattern PRICE_PATTERN = Pattern.compile("(?i)(?:\\$|\\b)(\\d+\\.\\d{2})(?:\\s*[FTB])?$");
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(?i)(?:(\\d+(?:\\.\\d+)?)\\s*(?:@|x|ea|pk|lb|oz|ct|count))|(\\d+)\\s+@");
    private static final Pattern UPC_PATTERN = Pattern.compile("\\b(\\d{11,14})\\b");

    @Override
    public String getProviderId() {
        return "kroger";
    }

    @Override
    public String getDisplayName() {
        return "Kroger (Digital / Receipt)";
    }

    @Override
    public boolean canParse(String rawText) {
        if (rawText == null || rawText.isBlank()) return false;
        String lower = rawText.toLowerCase();
        return lower.contains("kroger") || 
               lower.contains("plus card") || 
               lower.contains("kroger feedback") ||
               lower.contains("fuel points") ||
               lower.contains("savings") ||
               lower.contains("mypurchases");
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

            // Check for total line
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

            // Skip common receipt noise lines
            if (lower.startsWith("tax") || lower.startsWith("subtotal") || lower.startsWith("savings") ||
                lower.startsWith("change") || lower.startsWith("cash") || lower.startsWith("visa") ||
                lower.startsWith("mastercard") || lower.startsWith("debit") || lower.startsWith("fuel points") ||
                lower.startsWith("kroger plus") || lower.startsWith("welcome") || lower.startsWith("store:") ||
                lower.startsWith("date:") || lower.startsWith("phone:") || lower.length() < 3) {
                continue;
            }

            // Extract price
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

            // Extract Quantity if specified (e.g. 2 @ 3.49 or 2 PK)
            double quantity = 1.0;
            String unit = "count";
            Matcher qtyMatcher = QUANTITY_PATTERN.matcher(textWithoutPrice);
            if (qtyMatcher.find()) {
                String qtyStr = qtyMatcher.group(1) != null ? qtyMatcher.group(1) : qtyMatcher.group(2);
                try {
                    quantity = Double.parseDouble(qtyStr);
                } catch (Exception ignored) {}
            }

            // Clean item name
            String cleanName = cleanKrogerItemName(textWithoutPrice);
            if (cleanName.length() < 2) continue;

            String categoryId = deduceCategory(cleanName);
            String locationId = deduceLocation(cleanName, categoryId);
            LocalDate expiryEstimate = estimateExpiration(categoryId);

            items.add(new ParsedReceiptItemDto(
                    trimmed,
                    cleanName,
                    "Kroger",
                    quantity,
                    unit,
                    price,
                    price,
                    upc,
                    categoryId,
                    locationId,
                    null,
                    null,
                    expiryEstimate,
                    true
            ));
        }

        return new ReceiptParseResult(
                "Kroger",
                "kroger",
                transactionDate,
                totalAmount,
                items
        );
    }

    private String cleanKrogerItemName(String raw) {
        // Remove leading codes, asterisks, special characters
        String cleaned = raw.replaceAll("^[0-9\\*\\-\\.\\s]+", "");
        cleaned = cleaned.replaceAll("(?i)\\b(KR|KRG|KROGER|PVT SEL|SIMPLE TRUTH|ORG|ORGANIC)\\b", "").trim();
        cleaned = cleaned.replaceAll("\\s{2,}", " ");
        
        // Capitalize words nicely
        if (cleaned.length() > 0) {
            String[] words = cleaned.toLowerCase().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (!w.isEmpty()) {
                    sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
                }
            }
            return sb.toString().trim();
        }
        return raw;
    }

    private String deduceCategory(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("milk") || lower.contains("cheese") || lower.contains("butter") || lower.contains("yogurt") || lower.contains("egg") || lower.contains("cream")) {
            return "cat-dairy";
        }
        if (lower.contains("apple") || lower.contains("banana") || lower.contains("berry") || lower.contains("lettuce") || lower.contains("tomato") || lower.contains("onion") || lower.contains("garlic") || lower.contains("potato") || lower.contains("avocado") || lower.contains("spinach") || lower.contains("pepper") || lower.contains("lemon") || lower.contains("lime") || lower.contains("salad")) {
            return "cat-produce";
        }
        if (lower.contains("chicken") || lower.contains("beef") || lower.contains("pork") || lower.contains("salmon") || lower.contains("fish") || lower.contains("turkey") || lower.contains("bacon") || lower.contains("sausage") || lower.contains("steak") || lower.contains("ground")) {
            return "cat-meat";
        }
        if (lower.contains("bread") || lower.contains("bagel") || lower.contains("tortilla") || lower.contains("buns") || lower.contains("croissant")) {
            return "cat-bakery";
        }
        if (lower.contains("canned") || lower.contains("soup") || lower.contains("tuna") || lower.contains("beans") || lower.contains("broth") || lower.contains("sauce") || lower.contains("pasta sauce")) {
            return "cat-canned";
        }
        if (lower.contains("rice") || lower.contains("pasta") || lower.contains("flour") || lower.contains("sugar") || lower.contains("oats") || lower.contains("cereal") || lower.contains("quinoa") || lower.contains("noodle")) {
            return "cat-grains";
        }
        if (lower.contains("spice") || lower.contains("pepper") || lower.contains("salt") || lower.contains("cinnamon") || lower.contains("cumin") || lower.contains("paprika") || lower.contains("oregano") || lower.contains("seasoning")) {
            return "cat-spices";
        }
        if (lower.contains("water") || lower.contains("juice") || lower.contains("soda") || lower.contains("coffee") || lower.contains("tea") || lower.contains("coke") || lower.contains("pepsi")) {
            return "cat-beverages";
        }
        if (lower.contains("chip") || lower.contains("cracker") || lower.contains("cookie") || lower.contains("pretzel") || lower.contains("nut") || lower.contains("popcorn") || lower.contains("candy") || lower.contains("chocolate")) {
            return "cat-snacks";
        }
        if (lower.contains("frozen") || lower.contains("ice cream") || lower.contains("pizza") || lower.contains("waffle")) {
            return "cat-frozen";
        }
        if (lower.contains("paper") || lower.contains("soap") || lower.contains("detergent") || lower.contains("cleaner") || lower.contains("foil") || lower.contains("trash") || lower.contains("bag")) {
            return "cat-household";
        }
        return "cat-canned";
    }

    private String deduceLocation(String name, String categoryId) {
        String lower = name.toLowerCase();
        if (lower.contains("frozen") || lower.contains("ice cream") || categoryId.equals("cat-frozen")) {
            return "loc-freezer";
        }
        if (categoryId.equals("cat-dairy") || categoryId.equals("cat-meat") || lower.contains("refrigerat") || lower.contains("fresh")) {
            return "loc-fridge";
        }
        if (categoryId.equals("cat-spices")) {
            return "loc-spices";
        }
        if (categoryId.equals("cat-produce")) {
            if (lower.contains("banana") || lower.contains("apple") || lower.contains("orange") || lower.contains("avocado") || lower.contains("onion") || lower.contains("potato")) {
                return "loc-counter";
            }
            return "loc-fridge";
        }
        return "loc-pantry";
    }

    private LocalDate estimateExpiration(String categoryId) {
        LocalDate today = LocalDate.now();
        return switch (categoryId) {
            case "cat-produce" -> today.plusDays(7);
            case "cat-dairy" -> today.plusDays(14);
            case "cat-meat" -> today.plusDays(5);
            case "cat-bakery" -> today.plusDays(7);
            case "cat-frozen" -> today.plusMonths(6);
            case "cat-canned" -> today.plusYears(2);
            case "cat-grains" -> today.plusMonths(12);
            case "cat-spices" -> today.plusYears(2);
            case "cat-snacks" -> today.plusMonths(3);
            case "cat-beverages" -> today.plusMonths(6);
            default -> today.plusMonths(6);
        };
    }
}
