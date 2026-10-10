package com.skafferi.controller;

import com.skafferi.dto.BarcodeLookupResult;
import com.skafferi.dto.CommitReceiptRequest;
import com.skafferi.dto.ParsedReceiptItemDto;
import com.skafferi.dto.ReceiptParseRequest;
import com.skafferi.dto.ReceiptParseResult;
import com.skafferi.service.BarcodeLookupService;
import com.skafferi.service.InventoryService;
import com.skafferi.service.parser.ReceiptParserRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptParserRegistry parserRegistry;
    private final InventoryService inventoryService;
    private final BarcodeLookupService barcodeLookupService;

    public ReceiptController(ReceiptParserRegistry parserRegistry,
                             InventoryService inventoryService,
                             BarcodeLookupService barcodeLookupService) {
        this.parserRegistry = parserRegistry;
        this.inventoryService = inventoryService;
        this.barcodeLookupService = barcodeLookupService;
    }

    @GetMapping("/providers")
    public List<Map<String, String>> getAvailableProviders() {
        return parserRegistry.getAvailableProviders();
    }

    @PostMapping("/parse")
    public ResponseEntity<ReceiptParseResult> parseReceipt(@RequestBody ReceiptParseRequest request) {
        ReceiptParseResult rawResult = parserRegistry.parse(request.rawText(), request.providerId());

        List<ParsedReceiptItemDto> enrichedItems = new ArrayList<>();
        for (ParsedReceiptItemDto item : rawResult.items()) {
            if (item.barcode() != null && !item.barcode().isBlank()) {
                try {
                    BarcodeLookupResult lookup = barcodeLookupService.lookup(item.barcode());
                    if (lookup != null && lookup.found()) {
                        String matchedId = lookup.existingItemId() != null ? lookup.existingItemId() : item.matchedItemId();
                        String cleanName = lookup.name() != null && !lookup.name().isBlank() ? lookup.name() : item.cleanName();
                        String brand = lookup.brand() != null && !lookup.brand().isBlank() ? lookup.brand() : item.brand();
                        String category = lookup.categorySuggestion() != null && !lookup.categorySuggestion().isBlank() ? lookup.categorySuggestion() : item.suggestedCategoryId();
                        String imageUrl = lookup.imageUrl() != null ? lookup.imageUrl() : item.imageUrl();
                        String unit = lookup.defaultUnit() != null ? lookup.defaultUnit() : item.unit();

                        enrichedItems.add(new ParsedReceiptItemDto(
                                item.rawText(),
                                cleanName,
                                brand,
                                item.quantity(),
                                unit,
                                item.unitPrice(),
                                item.totalPrice(),
                                item.barcode(),
                                category,
                                item.suggestedLocationId(),
                                matchedId,
                                imageUrl,
                                item.estimatedExpirationDate(),
                                item.selected()
                        ));
                        continue;
                    }
                } catch (Exception ignored) {}
            }
            enrichedItems.add(item);
        }

        ReceiptParseResult enrichedResult = new ReceiptParseResult(
                rawResult.storeName(),
                rawResult.sourceParser(),
                rawResult.transactionDate(),
                rawResult.totalAmount(),
                enrichedItems
        );

        return ResponseEntity.ok(enrichedResult);
    }

    @PostMapping("/commit")
    public ResponseEntity<Map<String, Object>> commitReceipt(@RequestBody CommitReceiptRequest request) {
        inventoryService.commitReceipt(request);
        return ResponseEntity.ok(Map.of("status", "success", "count", request.items().size()));
    }
}
