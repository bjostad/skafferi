package com.skafferi.service.parser;

import com.skafferi.dto.ReceiptParseResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReceiptParserRegistryTest {

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
""";

    @Test
    void testRegistryRoutingWithGenericFirst() {
        // Spring might inject GenericTextReceiptParser first in the list
        ReceiptParserRegistry registry = new ReceiptParserRegistry(List.of(
                new GenericTextReceiptParser(),
                new KrogerReceiptParser()
        ));

        ReceiptParseResult resultAuto = registry.parse(SAMPLE_RECEIPT, "auto");
        System.out.println("Result auto sourceParser: " + resultAuto.sourceParser());
        System.out.println("Result auto item count: " + resultAuto.items().size());
        for (var item : resultAuto.items()) {
            System.out.println(" - Item: " + item.cleanName());
        }

        ReceiptParseResult resultGeneric = registry.parse(SAMPLE_RECEIPT, "generic");
        System.out.println("Result generic item count: " + resultGeneric.items().size());
    }
}
