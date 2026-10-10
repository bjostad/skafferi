package com.skafferi.service.parser;

import com.skafferi.dto.ParsedReceiptItemDto;
import com.skafferi.dto.ReceiptParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GenericTextReceiptParserTest {

    private GenericTextReceiptParser parser;

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
""";

    @BeforeEach
    void setUp() {
        parser = new GenericTextReceiptParser();
    }

    @Test
    void testParseSampleReceiptFiltersOutAllMetadata() {
        ReceiptParseResult result = parser.parse(SAMPLE_RECEIPT);
        assertNotNull(result);
        assertEquals(30.33, result.totalAmount());
        assertEquals("Fred Meyer", result.storeName());

        List<ParsedReceiptItemDto> items = result.items();
        // 12 food items - zero metadata lines!
        assertEquals(12, items.size());

        List<String> names = items.stream().map(i -> i.cleanName().toLowerCase()).toList();

        // Verify non-item lines are completely absent
        assertFalse(names.stream().anyMatch(n -> n.contains("logo")));
        assertFalse(names.stream().anyMatch(n -> n.contains("print")));
        assertFalse(names.stream().anyMatch(n -> n.contains("order type")));
        assertFalse(names.stream().anyMatch(n -> n.contains("order date")));
        assertFalse(names.stream().anyMatch(n -> n.contains("order number")));
        assertFalse(names.stream().anyMatch(n -> n.contains("loyalty card")));
        assertFalse(names.stream().anyMatch(n -> n.contains("aurora ave")));
        assertFalse(names.stream().anyMatch(n -> n.contains("rewards")));
        assertFalse(names.stream().anyMatch(n -> n.contains("total savings")));
        assertFalse(names.stream().anyMatch(n -> n.contains("order summary")));
        assertFalse(names.stream().anyMatch(n -> n.contains("original item total")));
        assertFalse(names.stream().anyMatch(n -> n.contains("item coupon")));
        assertFalse(names.stream().anyMatch(n -> n.contains("other fee")));
        assertFalse(names.stream().anyMatch(n -> n.contains("bag fee")));
        assertFalse(names.stream().anyMatch(n -> n.contains("sales tax")));
        assertFalse(names.stream().anyMatch(n -> n.contains("order total")));
        assertFalse(names.stream().anyMatch(n -> n.contains("item details")));
        assertFalse(names.stream().anyMatch(n -> n.contains("14 items")));
        assertFalse(names.stream().anyMatch(n -> n.contains("terminal id")));
        assertFalse(names.stream().anyMatch(n -> n.contains("visa")));
    }

    @Test
    void testLineByLineReceiptRequiresPrice() {
        String receipt = """
            SAFEWAY STORE #1234
            THANK YOU FOR SHOPPING WITH US
            ORGANIC WHOLE MILK    4.29
            SOURDOUGH BREAD       3.99
            SUBTOTAL              8.28
            TAX                   0.00
            TOTAL                 8.28
            CASH TENDER           10.00
            CHANGE                1.72
            """;
        ReceiptParseResult result = parser.parse(receipt);
        assertNotNull(result);
        assertEquals(8.28, result.totalAmount());
        assertEquals(2, result.items().size());
        assertEquals("ORGANIC WHOLE MILK", result.items().get(0).cleanName());
        assertEquals("SOURDOUGH BREAD", result.items().get(1).cleanName());
    }
}
