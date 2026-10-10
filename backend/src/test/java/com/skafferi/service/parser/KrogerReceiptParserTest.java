package com.skafferi.service.parser;

import com.skafferi.dto.ParsedReceiptItemDto;
import com.skafferi.dto.ReceiptParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class KrogerReceiptParserTest {

    private KrogerReceiptParser parser;

    private final String SAMPLE_RECEIPT = """
Fred Meyer logo
Print
Order Type: In Store
Order Date: Oct. 2, 2026
Order Number: 701~00013~2026-10-02~503~1521654
Loyalty Card (last 4): #7945
Fred Meyer
18325 Aurora Ave N
Shoreline, WA 98133 USA
Rewards
Total Savings: $16.90
Order Summary
Original Item Total
$47.07
Item Coupons/Sales
-$16.90
Other Fees
+$0.16
Sales Tax
$0.00
Order Total
$30.33
Item Details
14 Items
Calbee® Harvest Snaps® Lightly Salted Green Pea Snack Crisps, 3.3 oz
$2.52
2 x $1.26 each
UPC: 0007114600245
Cheetos® Baked Crunchy Cheese Chips, 7.625 oz
$4.99
1 x $4.99 each
UPC: 0002840018390
Doritos Simply NKD Nacho Cheese Flavored Tortilla Chips, 9.25 oz
$5.49
1 x $5.49 each
UPC: 0002840079475
Fred Meyer™ Vitamin D Whole Milk Half Gallon, 1/2 gallon
$2.59
2 x $1.29 $2.59 each (approx.)
Item Coupon/Sale: -$2.59
UPC: 0001111040147
Fresh Strawberries – 1 LB Clamshell, 1 lb
$3.34
1 x $3.34 $3.99 each
Item Coupon/Sale: -$0.65
UPC: 0003338320027
Harvest Snaps Mango Chile Lime Red Lentil Snack Crisps, 3 oz
$1.26
1 x $1.26 each
UPC: 0007114601087
Kroger® Buttermilk Waffles, 10 ct / 12.3 oz
$1.99
1 x $1.99 $2.49 each
Item Coupon/Sale: -$0.50
UPC: 0001111087832
Lay's Wavy Potato Chips Magic Masala, 7.5 oz
$0.00
1 x $0.00 $3.99 each
Item Coupon/Sale: -$3.99
UPC: 0002840078300
Lay's® Bacon Grilled Cheese Potato Chips, 7.75 oz
$0.00
1 x $0.00 $3.99 each
Item Coupon/Sale: -$3.99
UPC: 0002840076863
Little Debbie® Brownie Pumpkins Multipack, 5 ct / 10.17 oz
$3.00
1 x $3.00 $3.19 each
Item Coupon/Sale: -$0.19
UPC: 0002430004462
Nestle Toll House Peanut Butter Chocolate Chip Ready to Bake Cookie Dough, 14 oz
$4.99
1 x $4.99 each
UPC: 0005000099445
Nestle Toll House Pecan Turtle Delight Cookie Dough Caramel Pecan Chocolate Chips, 16 oz
$0.00
1 x $0.00 $4.99 each
Item Coupon/Sale: -$4.99
UPC: 0005000000928
Other Fees
$0.16
Bag Fee
$0.16
2 x $0.08 each
Payment Details
TERMINAL ID 503
VISA 9324
$30.33
www.fredmeyer.com
1-800-KRO-GERS (1-800-576-4377)
""";

    @BeforeEach
    void setUp() {
        parser = new KrogerReceiptParser();
    }

    @Test
    void testCanParse() {
        assertTrue(parser.canParse(SAMPLE_RECEIPT));
        assertTrue(parser.canParse("Fred Meyer order details UPC: 123456789012"));
        assertTrue(parser.canParse("KROGER PLUS SAVINGS 123"));
    }

    @Test
    void testParseSampleReceipt() {
        ReceiptParseResult result = parser.parse(SAMPLE_RECEIPT);
        assertNotNull(result);
        assertEquals(30.33, result.totalAmount());
        assertEquals(LocalDate.of(2026, 10, 2), result.transactionDate().toLocalDate());

        List<ParsedReceiptItemDto> items = result.items();
        // 12 food items
        assertEquals(12, items.size());

        // Item 1: Calbee Harvest Snaps
        ParsedReceiptItemDto item0 = items.get(0);
        assertTrue(item0.cleanName().contains("Harvest Snaps Lightly Salted Green Pea Snack Crisps"));
        assertFalse(item0.cleanName().contains("®"));
        assertEquals("Calbee", item0.brand());
        assertEquals(2.0, item0.quantity());
        assertEquals(1.26, item0.unitPrice());
        assertEquals(2.52, item0.totalPrice());
        assertEquals("0007114600245", item0.barcode());
        assertEquals("cat-snacks", item0.suggestedCategoryId());

        // Item 3: Milk
        ParsedReceiptItemDto milk = items.get(3);
        assertTrue(milk.cleanName().contains("Vitamin D Whole Milk Half Gallon"));
        assertFalse(milk.cleanName().contains("™"));
        assertEquals("Fred Meyer", milk.brand());
        assertEquals(2.0, milk.quantity());
        assertEquals(1.29, milk.unitPrice());
        assertEquals(2.59, milk.totalPrice());
        assertEquals("0001111040147", milk.barcode());
        assertEquals("cat-dairy", milk.suggestedCategoryId());
        assertEquals("loc-fridge", milk.suggestedLocationId());

        // Item 4: Strawberries
        ParsedReceiptItemDto strawberries = items.get(4);
        assertTrue(strawberries.cleanName().contains("Fresh Strawberries"));
        assertEquals(1.0, strawberries.quantity());
        assertEquals(3.34, strawberries.unitPrice());
        assertEquals(3.34, strawberries.totalPrice());
        assertEquals("0003338320027", strawberries.barcode());
        assertEquals("cat-produce", strawberries.suggestedCategoryId());

        // Item 7: Lay's Magic Masala ($0.00 total)
        ParsedReceiptItemDto lays = items.get(7);
        assertTrue(lays.cleanName().contains("Potato Chips Magic Masala"));
        assertEquals("Lay's", lays.brand());
        assertEquals(1.0, lays.quantity());
        assertEquals(0.00, lays.totalPrice());
        assertEquals("0002840078300", lays.barcode());
        assertEquals("cat-snacks", lays.suggestedCategoryId());

        // Ensure "Bag Fee" is NOT included in items
        for (ParsedReceiptItemDto item : items) {
            assertFalse(item.cleanName().toLowerCase().contains("bag fee"));
            assertFalse(item.cleanName().toLowerCase().contains("other fee"));
        }
    }

    @Test
    void testTraditionalReceipt() {
        String receipt = """
            KROGER STORE #012
            123 MAIN ST
            KROGER PLUS SAVINGS
            KR 2% RED MILK GAL    3.49
            SIMPLE TRUTH ORG EGGS 4.29
            TOTAL 7.78
            BALANCE DUE 7.78
            """;
        ReceiptParseResult result = parser.parse(receipt);
        assertNotNull(result);
        assertEquals(7.78, result.totalAmount());
        assertEquals(2, result.items().size());
        assertEquals("2% Red Milk Gal", result.items().get(0).cleanName());
        assertEquals(3.49, result.items().get(0).totalPrice());
    }
}
