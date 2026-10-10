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
    private static final Pattern DIGITAL_QTY_PATTERN = Pattern.compile("(?i)^(\\d+(?:\\.\\d+)?)\\s*x\\s*\\$?(\\d+\\.\\d{2})");
    private static final Pattern DIGITAL_DATE_PATTERN = Pattern.compile("(?i)Order Date:\\s*([A-Za-z]+)\\.?\\s*(\\d{1,2}),?\\s*(\\d{4})");
    private static final Pattern METADATA_LINE_PATTERN = Pattern.compile(
            "(?i)^\\s*(fred\\s+meyer|kroger|ralphs|qfc)?\\s*(logo|print|order\\s+type:?|order\\s+date:?|order\\s+number:?|loyalty\\s+card:?|member:?|rewards|total\\s+savings:?|order\\s+summary|original\\s+item\\s+total|item\\s+coupons?/sales?|other\\s+fees|sales?\\s+tax|order\\s+total|subtotal|balance|payment\\s+details|terminal\\s+id|visa\\s*\\d*|mastercard\\s*\\d*|item\\s+details|\\d+\\s+items?|bag\\s+fee|tax|change|cash|debit|credit).*$"
    );

    @Override
    public String getProviderId() {
        return "kroger";
    }

    @Override
    public String getDisplayName() {
        return "Kroger / Fred Meyer (Digital & Receipt)";
    }

    @Override
    public boolean canParse(String rawText) {
        if (rawText == null || rawText.isBlank()) return false;
        String lower = rawText.toLowerCase();
        return lower.contains("kroger") || 
               lower.contains("fred meyer") ||
               lower.contains("ralphs") ||
               lower.contains("fry's") || lower.contains("frys") ||
               lower.contains("smith's") || lower.contains("smiths") ||
               lower.contains("king soopers") ||
               lower.contains("qfc") ||
               lower.contains("dillons") ||
               lower.contains("city market") ||
               lower.contains("mariano") ||
               lower.contains("plus card") || 
               lower.contains("kroger feedback") ||
               lower.contains("fuel points") ||
               lower.contains("total savings") ||
               lower.contains("savings") ||
               lower.contains("mypurchases") ||
               (lower.contains("order total") && lower.contains("upc:"));
    }

    @Override
    public ReceiptParseResult parse(String rawText) {
        String lower = rawText.toLowerCase();
        if (lower.contains("item details") || lower.contains("upc:")) {
            return parseDigitalReceipt(rawText);
        }
        return parseTraditionalReceipt(rawText);
    }

    private ReceiptParseResult parseDigitalReceipt(String rawText) {
        List<ParsedReceiptItemDto> items = new ArrayList<>();
        Double totalAmount = null;
        LocalDateTime transactionDate = LocalDateTime.now();

        String[] lines = rawText.split("\\r?\\n");

        // 1. Extract Date
        Matcher dateMatcher = DIGITAL_DATE_PATTERN.matcher(rawText);
        if (dateMatcher.find()) {
            try {
                String monthStr = dateMatcher.group(1).substring(0, 3).toLowerCase();
                int day = Integer.parseInt(dateMatcher.group(2));
                int year = Integer.parseInt(dateMatcher.group(3));
                int month = switch (monthStr) {
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
                    default -> transactionDate.getMonthValue();
                };
                transactionDate = LocalDate.of(year, month, day).atTime(12, 0);
            } catch (Exception ignored) {}
        }

        // 2. Extract Order Total (e.g. "Order Total\n$30.33" or "Order Total: $30.33")
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].trim();
            if (l.equalsIgnoreCase("Order Total") && i + 1 < lines.length) {
                Matcher pm = Pattern.compile("\\$?(\\d+\\.\\d{2})").matcher(lines[i + 1].trim());
                if (pm.find()) {
                    try {
                        totalAmount = Double.parseDouble(pm.group(1));
                    } catch (Exception ignored) {}
                }
            } else if (l.toLowerCase().startsWith("order total")) {
                Matcher pm = Pattern.compile("\\$?(\\d+\\.\\d{2})").matcher(l);
                if (pm.find()) {
                    try {
                        totalAmount = Double.parseDouble(pm.group(1));
                    } catch (Exception ignored) {}
                }
            }
        }

        // 3. Find Item Details section
        int startIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.equalsIgnoreCase("Item Details") || trimmed.equalsIgnoreCase("Items") ||
                trimmed.toLowerCase().matches("^item\\s+details.*$") ||
                trimmed.toLowerCase().matches("^items\\s+in\\s+order.*$") ||
                trimmed.toLowerCase().matches("^\\d+\\s+items?$")) {
                startIndex = i + 1;
                // Skip the "X Items" summary line if present
                if (startIndex < lines.length && lines[startIndex].trim().toLowerCase().matches("\\d+\\s+items?")) {
                    startIndex++;
                }
                break;
            }
        }
        if (startIndex == -1) {
            startIndex = 0;
        }

        List<String> currentBlock = new ArrayList<>();
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String lowerLine = line.toLowerCase();
            // Stop delimiters: fees, payments, terminal, website
            if (lowerLine.startsWith("other fees") || lowerLine.startsWith("payment details") ||
                lowerLine.startsWith("terminal id") || lowerLine.startsWith("www.fredmeyer.com") ||
                lowerLine.startsWith("www.kroger.com")) {
                break;
            }

            currentBlock.add(line);

            if (lowerLine.startsWith("upc:")) {
                // End of an item block!
                ParsedReceiptItemDto parsed = parseDigitalItemBlock(currentBlock);
                if (parsed != null) {
                    items.add(parsed);
                }
                currentBlock.clear();
            }
        }

        String storeName = "Kroger";
        String lowerRaw = rawText.toLowerCase();
        if (lowerRaw.contains("fred meyer")) storeName = "Fred Meyer";
        else if (lowerRaw.contains("ralphs")) storeName = "Ralphs";
        else if (lowerRaw.contains("smith's") || lowerRaw.contains("smiths")) storeName = "Smith's";
        else if (lowerRaw.contains("fry's") || lowerRaw.contains("frys")) storeName = "Fry's";
        else if (lowerRaw.contains("king soopers")) storeName = "King Soopers";
        else if (lowerRaw.contains("qfc")) storeName = "QFC";
        else if (lowerRaw.contains("dillons")) storeName = "Dillons";
        else if (lowerRaw.contains("mariano")) storeName = "Mariano's";

        return new ReceiptParseResult(
                storeName,
                "kroger",
                transactionDate,
                totalAmount,
                items
        );
    }

    private ParsedReceiptItemDto parseDigitalItemBlock(List<String> block) {
        if (block.isEmpty()) return null;

        // Find the actual item name line (skip any preceding metadata / fees)
        String nameLine = null;
        for (String line : block) {
            String tl = line.trim();
            if (tl.isEmpty()) continue;
            if (isMetadataOrFeeLine(tl)) continue;
            if (tl.matches("^\\$?\\d+\\.\\d{2}$")) continue;
            if (DIGITAL_QTY_PATTERN.matcher(tl).find()) continue;
            if (tl.toLowerCase().startsWith("item coupon") || tl.startsWith("-")) continue;
            nameLine = tl;
            break;
        }

        if (nameLine == null || nameLine.length() < 2) return null;
        if (nameLine.toLowerCase().contains("bag fee") || nameLine.toLowerCase().contains("other fee")) return null;

        String rawBlockText = String.join("\n", block);

        Double totalPrice = null;
        Double unitPrice = null;
        double quantity = 1.0;
        String barcode = null;

        for (String itemLine : block) {
            String line = itemLine.trim();
            if (line.equals(nameLine)) continue;
            String lowerLine = line.toLowerCase();

            // Total price for line (usually "$X.XX")
            if (totalPrice == null && line.matches("^\\$?\\d+\\.\\d{2}$")) {
                try {
                    totalPrice = Double.parseDouble(line.replace("$", "").trim());
                } catch (Exception ignored) {}
                continue;
            }

            // Quantity / Unit price pattern: "2 x $1.26 each" or "2 x $1.29 $2.59 each (approx.)"
            Matcher qtyMatcher = DIGITAL_QTY_PATTERN.matcher(line);
            if (qtyMatcher.find()) {
                try {
                    quantity = Double.parseDouble(qtyMatcher.group(1));
                    unitPrice = Double.parseDouble(qtyMatcher.group(2));
                } catch (Exception ignored) {}
                continue;
            }

            // Barcode
            if (lowerLine.startsWith("upc:")) {
                Matcher m = Pattern.compile("(\\d{10,14})").matcher(line);
                if (m.find()) {
                    barcode = m.group(1);
                }
            }
        }

        if (unitPrice == null && totalPrice != null) {
            unitPrice = quantity > 0 ? totalPrice / quantity : totalPrice;
        }

        String brand = extractBrand(nameLine);
        String cleanName = cleanDigitalItemName(nameLine);
        String categoryId = deduceCategory(cleanName);
        String locationId = deduceLocation(cleanName, categoryId);
        LocalDate expiryEstimate = estimateExpiration(categoryId);

        return new ParsedReceiptItemDto(
                rawBlockText,
                cleanName,
                brand,
                quantity,
                "count",
                unitPrice,
                totalPrice,
                barcode,
                categoryId,
                locationId,
                null,
                null,
                expiryEstimate,
                true
        );
    }

    private String extractBrand(String name) {
        String trimmed = name.trim();
        if (trimmed.startsWith("Calbee")) return "Calbee";
        if (trimmed.startsWith("Cheetos")) return "Cheetos";
        if (trimmed.startsWith("Doritos")) return "Doritos";
        if (trimmed.startsWith("Fred Meyer")) return "Fred Meyer";
        if (trimmed.startsWith("Kroger")) return "Kroger";
        if (trimmed.startsWith("Harvest Snaps")) return "Harvest Snaps";
        if (trimmed.startsWith("Lay's") || trimmed.startsWith("Lays")) return "Lay's";
        if (trimmed.startsWith("Little Debbie")) return "Little Debbie";
        if (trimmed.startsWith("Nestle")) return "Nestle";
        if (trimmed.startsWith("Simple Truth")) return "Simple Truth";
        if (trimmed.startsWith("Private Selection")) return "Private Selection";
        return "Kroger";
    }

    private String cleanDigitalItemName(String raw) {
        String cleaned = raw.replaceAll("[®™©]", "");
        cleaned = cleaned.replaceAll("\\s{2,}", " ").trim();
        return cleaned;
    }

    private ReceiptParseResult parseTraditionalReceipt(String rawText) {
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

            // Extract price (Traditional OCR receipts must have a price on the item line)
            Double price = null;
            String textWithoutPrice = trimmed;
            Matcher priceMatcher = PRICE_PATTERN.matcher(trimmed);
            if (priceMatcher.find()) {
                try {
                    price = Double.parseDouble(priceMatcher.group(1));
                    textWithoutPrice = trimmed.substring(0, priceMatcher.start()).trim();
                } catch (Exception ignored) {}
            } else {
                continue;
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
        if (lower.contains("frozen") || lower.contains("ice cream") || lower.contains("pizza") || lower.contains("waffle")) {
            return "cat-frozen";
        }
        if (lower.contains("milk") || lower.contains("cheese") || lower.contains("butter") || lower.contains("yogurt") || lower.contains("egg") || lower.contains("cream")) {
            return "cat-dairy";
        }
        if (lower.contains("chip") || lower.contains("crisp") || lower.contains("cracker") || lower.contains("cookie") || lower.contains("pretzel") || lower.contains("nut") || lower.contains("popcorn") || lower.contains("candy") || lower.contains("chocolate") || lower.contains("brownie") || lower.contains("snack")) {
            return "cat-snacks";
        }
        if (lower.contains("chicken") || lower.contains("beef") || lower.contains("pork") || lower.contains("salmon") || lower.contains("fish") || lower.contains("turkey") || lower.contains("bacon") || lower.contains("sausage") || lower.contains("steak") || lower.contains("ground")) {
            return "cat-meat";
        }
        if (lower.contains("bread") || lower.contains("bagel") || lower.contains("tortilla") || lower.contains("buns") || lower.contains("croissant")) {
            return "cat-bakery";
        }
        if (lower.contains("apple") || lower.contains("banana") || lower.contains("berry") || lower.contains("strawberries") || lower.contains("lettuce") || lower.contains("tomato") || lower.contains("onion") || lower.contains("garlic") || lower.contains("potato") || lower.contains("avocado") || lower.contains("spinach") || lower.contains("pepper") || lower.contains("lemon") || lower.contains("lime") || lower.contains("salad")) {
            return "cat-produce";
        }
        if (lower.contains("canned") || lower.contains("soup") || lower.contains("tuna") || lower.contains("beans") || lower.contains("broth") || lower.contains("sauce") || lower.contains("pasta sauce")) {
            return "cat-canned";
        }
        if (lower.contains("rice") || lower.contains("pasta") || lower.contains("flour") || lower.contains("sugar") || lower.contains("oats") || lower.contains("cereal") || lower.contains("quinoa") || lower.contains("noodle")) {
            return "cat-grains";
        }
        if (lower.contains("spice") || lower.contains("cinnamon") || lower.contains("cumin") || lower.contains("paprika") || lower.contains("oregano") || lower.contains("seasoning") || lower.matches(".*\\b(salt|pepper)\\b.*")) {
            return "cat-spices";
        }
        if (lower.contains("water") || lower.contains("juice") || lower.contains("soda") || lower.contains("coffee") || lower.contains("tea") || lower.contains("coke") || lower.contains("pepsi")) {
            return "cat-beverages";
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
}
