package com.skafferi.controller;

import com.skafferi.dto.CommitReceiptRequest;
import com.skafferi.dto.ReceiptParseRequest;
import com.skafferi.dto.ReceiptParseResult;
import com.skafferi.service.InventoryService;
import com.skafferi.service.parser.ReceiptParserRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptParserRegistry parserRegistry;
    private final InventoryService inventoryService;

    public ReceiptController(ReceiptParserRegistry parserRegistry, InventoryService inventoryService) {
        this.parserRegistry = parserRegistry;
        this.inventoryService = inventoryService;
    }

    @GetMapping("/providers")
    public List<Map<String, String>> getAvailableProviders() {
        return parserRegistry.getAvailableProviders();
    }

    @PostMapping("/parse")
    public ResponseEntity<ReceiptParseResult> parseReceipt(@RequestBody ReceiptParseRequest request) {
        ReceiptParseResult result = parserRegistry.parse(request.rawText(), request.providerId());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/commit")
    public ResponseEntity<Map<String, Object>> commitReceipt(@RequestBody CommitReceiptRequest request) {
        inventoryService.commitReceipt(request);
        return ResponseEntity.ok(Map.of("status", "success", "count", request.items().size()));
    }
}
