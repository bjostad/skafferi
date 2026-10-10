package com.skafferi.controller;

import com.skafferi.domain.Item;
import com.skafferi.dto.ItemDto;
import com.skafferi.dto.PantryItemSummaryDto;
import com.skafferi.dto.PurchaseRecordDto;
import com.skafferi.repository.InventoryBatchRepository;
import com.skafferi.repository.ItemRepository;
import com.skafferi.repository.PurchaseRecordRepository;
import com.skafferi.service.InventoryService;
import com.skafferi.service.PurchaseRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final InventoryService inventoryService;
    private final ItemRepository itemRepository;
    private final PurchaseRecordService purchaseRecordService;
    private final PurchaseRecordRepository purchaseRecordRepository;
    private final InventoryBatchRepository batchRepository;

    public ItemController(InventoryService inventoryService,
                          ItemRepository itemRepository,
                          PurchaseRecordService purchaseRecordService,
                          PurchaseRecordRepository purchaseRecordRepository,
                          InventoryBatchRepository batchRepository) {
        this.inventoryService = inventoryService;
        this.itemRepository = itemRepository;
        this.purchaseRecordService = purchaseRecordService;
        this.purchaseRecordRepository = purchaseRecordRepository;
        this.batchRepository = batchRepository;
    }

    @GetMapping
    public List<PantryItemSummaryDto> getAllItems(
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "NAME") String sortBy) {
        return inventoryService.getAllPantrySummaries(locationId, categoryId, search, status, sortBy);
    }

    @GetMapping("/notifications")
    public com.skafferi.dto.NotificationSummaryDto getNotifications() {
        return inventoryService.getNotificationSummary();
    }

    @GetMapping("/{id}")
    public PantryItemSummaryDto getItemById(@PathVariable String id) {
        return inventoryService.getPantrySummary(id);
    }

    @GetMapping("/expiring")
    public List<PantryItemSummaryDto> getExpiringSoon(@RequestParam(defaultValue = "7") int days) {
        return inventoryService.getExpiringSoonItems(days);
    }

    @PostMapping
    public ResponseEntity<Item> createOrUpdateItem(@RequestBody ItemDto dto) {
        Item saved = inventoryService.createOrUpdateItem(dto);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        purchaseRecordRepository.deleteByItemId(id);
        batchRepository.deleteByItemId(id);
        itemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-freshness")
    public ResponseEntity<PantryItemSummaryDto> resetFreshness(@PathVariable String id) {
        return ResponseEntity.ok(inventoryService.resetFreshness(id));
    }

    @GetMapping("/{id}/purchases")
    public List<PurchaseRecordDto> getPurchasesForItem(@PathVariable String id) {
        return purchaseRecordService.getPurchasesForItem(id);
    }

    @PostMapping("/{id}/purchases")
    public ResponseEntity<PurchaseRecordDto> addPurchaseRecord(
            @PathVariable String id,
            @RequestBody PurchaseRecordDto dto) {
        PurchaseRecordDto toSave = new PurchaseRecordDto(
                dto.id(),
                id,
                dto.itemName(),
                dto.purchasedDate(),
                dto.store(),
                dto.quantity(),
                dto.unit(),
                dto.unitPrice(),
                dto.totalPrice(),
                dto.notes(),
                dto.source() != null ? dto.source() : "MANUAL",
                null
        );
        return ResponseEntity.ok(purchaseRecordService.addPurchase(toSave));
    }

    @DeleteMapping("/{id}/purchases/{purchaseId}")
    public ResponseEntity<Void> deletePurchaseRecord(
            @PathVariable String id,
            @PathVariable String purchaseId) {
        purchaseRecordService.deletePurchase(purchaseId);
        return ResponseEntity.noContent().build();
    }
}
