package com.skafferi.controller;

import com.skafferi.domain.InventoryBatch;
import com.skafferi.dto.InventoryBatchDto;
import com.skafferi.dto.PantryItemSummaryDto;
import com.skafferi.repository.InventoryBatchRepository;
import com.skafferi.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryBatchRepository batchRepository;

    public InventoryController(InventoryService inventoryService, InventoryBatchRepository batchRepository) {
        this.inventoryService = inventoryService;
        this.batchRepository = batchRepository;
    }

    @PostMapping("/adjust")
    public ResponseEntity<PantryItemSummaryDto> adjustQuantity(@RequestBody Map<String, Object> payload) {
        String itemId = (String) payload.get("itemId");
        Number deltaNum = (Number) payload.get("delta");
        double delta = deltaNum != null ? deltaNum.doubleValue() : 0.0;

        inventoryService.adjustQuantity(itemId, delta);
        PantryItemSummaryDto updatedSummary = inventoryService.getPantrySummary(itemId);
        return ResponseEntity.ok(updatedSummary);
    }

    @PostMapping("/batch")
    public ResponseEntity<InventoryBatch> addBatch(@RequestBody InventoryBatchDto dto) {
        InventoryBatch saved = inventoryService.addBatch(dto);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/consume")
    public ResponseEntity<PantryItemSummaryDto> consumeItem(@RequestBody Map<String, Object> payload) {
        String itemId = (String) payload.get("itemId");
        inventoryService.consumeItem(itemId);
        PantryItemSummaryDto updatedSummary = inventoryService.getPantrySummary(itemId);
        return ResponseEntity.ok(updatedSummary);
    }

    @PostMapping("/move-location")
    public ResponseEntity<PantryItemSummaryDto> moveLocation(@RequestBody Map<String, Object> payload) {
        String itemId = (String) payload.get("itemId");
        String batchId = (String) payload.get("batchId");
        String targetLocationId = (String) payload.get("targetLocationId");
        Number qtyNum = (Number) payload.get("quantity");
        Double quantity = qtyNum != null ? qtyNum.doubleValue() : null;

        inventoryService.moveLocation(itemId, batchId, targetLocationId, quantity);
        PantryItemSummaryDto updatedSummary = inventoryService.getPantrySummary(itemId);
        return ResponseEntity.ok(updatedSummary);
    }

    @DeleteMapping("/batch/{batchId}")
    public ResponseEntity<Void> deleteBatch(@PathVariable String batchId) {
        batchRepository.deleteById(batchId);
        return ResponseEntity.noContent().build();
    }
}
