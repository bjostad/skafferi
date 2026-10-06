package com.skafferi.controller;

import com.skafferi.domain.Item;
import com.skafferi.dto.ItemDto;
import com.skafferi.dto.PantryItemSummaryDto;
import com.skafferi.repository.ItemRepository;
import com.skafferi.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final InventoryService inventoryService;
    private final ItemRepository itemRepository;

    public ItemController(InventoryService inventoryService, ItemRepository itemRepository) {
        this.inventoryService = inventoryService;
        this.itemRepository = itemRepository;
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
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        itemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-freshness")
    public ResponseEntity<PantryItemSummaryDto> resetFreshness(@PathVariable String id) {
        return ResponseEntity.ok(inventoryService.resetFreshness(id));
    }
}
