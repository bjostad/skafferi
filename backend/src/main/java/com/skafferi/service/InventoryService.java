package com.skafferi.service;

import com.skafferi.domain.*;
import com.skafferi.dto.*;
import com.skafferi.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ItemRepository itemRepository;
    private final InventoryBatchRepository batchRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final BringSyncService bringSyncService;
    private final SettingsService settingsService;

    public InventoryService(ItemRepository itemRepository,
                            InventoryBatchRepository batchRepository,
                            LocationRepository locationRepository,
                            CategoryRepository categoryRepository,
                            BringSyncService bringSyncService,
                            SettingsService settingsService) {
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.locationRepository = locationRepository;
        this.categoryRepository = categoryRepository;
        this.bringSyncService = bringSyncService;
        this.settingsService = settingsService;
    }

    public List<PantryItemSummaryDto> getAllPantrySummaries(String locationId, String categoryId, String searchQuery, String statusFilter, String sortBy) {
        List<Item> items;
        if (searchQuery != null && !searchQuery.isBlank()) {
            items = itemRepository.findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(searchQuery.trim(), searchQuery.trim());
        } else {
            items = itemRepository.findAll();
        }

        List<PantryItemSummaryDto> summaries = items.stream()
                .filter(item -> categoryId == null || categoryId.isBlank() || (item.getCategory() != null && item.getCategory().getId().equalsIgnoreCase(categoryId)))
                .map(this::mapToSummary)
                .filter(summary -> {
                    if (locationId == null || locationId.isBlank()) return true;
                    // Check if item has batches in this location, or default location matches
                    boolean hasBatchInLocation = summary.batches().stream().anyMatch(b -> b.locationId().equalsIgnoreCase(locationId));
                    boolean defaultMatches = summary.defaultLocation() != null && summary.defaultLocation().getId().equalsIgnoreCase(locationId);
                    return hasBatchInLocation || defaultMatches;
                })
                .filter(summary -> {
                    if (statusFilter == null || statusFilter.isBlank() || statusFilter.equalsIgnoreCase("ALL")) {
                        return true;
                    }
                    if (statusFilter.equalsIgnoreCase("EXPIRED")) {
                        return "EXPIRED".equalsIgnoreCase(summary.expiryStatus());
                    }
                    if (statusFilter.equalsIgnoreCase("EXPIRING_SOON")) {
                        return "EXPIRING_SOON".equalsIgnoreCase(summary.expiryStatus());
                    }
                    if (statusFilter.equalsIgnoreCase("FRESH_CHECK")) {
                        return summary.freshCheckNeeded();
                    }
                    if (statusFilter.equalsIgnoreCase("FRESH")) {
                        return "FRESH".equalsIgnoreCase(summary.expiryStatus()) && !summary.freshCheckNeeded();
                    }
                    if (statusFilter.equalsIgnoreCase("LOW_STOCK")) {
                        return summary.isLowStock() && !summary.isOutOfStock();
                    }
                    if (statusFilter.equalsIgnoreCase("OUT_OF_STOCK")) {
                        return summary.isOutOfStock();
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Sorting
        Comparator<PantryItemSummaryDto> comparator;
        if ("EXPIRY".equalsIgnoreCase(sortBy)) {
            // Expired first, then expiring soon, then fresh, then no expiry
            comparator = Comparator.comparing(
                    s -> s.daysUntilEarliestExpiry() != null ? s.daysUntilEarliestExpiry() : Integer.MAX_VALUE
            );
        } else if ("QTY_ASC".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingDouble(PantryItemSummaryDto::totalQuantity);
        } else if ("QTY_DESC".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingDouble(PantryItemSummaryDto::totalQuantity).reversed();
        } else {
            // Default A-Z
            comparator = Comparator.comparing(PantryItemSummaryDto::name, String.CASE_INSENSITIVE_ORDER);
        }

        summaries.sort(comparator);
        return summaries;
    }

    public List<PantryItemSummaryDto> getAllPantrySummaries(String locationId, String categoryId, String searchQuery) {
        return getAllPantrySummaries(locationId, categoryId, searchQuery, null, "NAME");
    }

    public PantryItemSummaryDto getPantrySummary(String itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + itemId));
        return mapToSummary(item);
    }

    public List<PantryItemSummaryDto> getExpiringSoonItems(int daysThreshold) {
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);
        List<InventoryBatch> expiringBatches = batchRepository.findExpiringBefore(thresholdDate);
        Set<String> itemIds = expiringBatches.stream().map(b -> b.getItem().getId()).collect(Collectors.toSet());

        return itemRepository.findAllById(itemIds).stream()
                .map(this::mapToSummary)
                .sorted(Comparator.comparing(s -> s.daysUntilEarliestExpiry() != null ? s.daysUntilEarliestExpiry() : Integer.MAX_VALUE))
                .collect(Collectors.toList());
    }

    @Transactional
    public Item createOrUpdateItem(ItemDto dto) {
        Item item = dto.id() != null ? itemRepository.findById(dto.id()).orElse(new Item()) : new Item();
        item.setName(dto.name());
        item.setBrand(dto.brand());
        item.setDefaultUnit(dto.defaultUnit() != null ? dto.defaultUnit() : "count");
        item.setMinThreshold(dto.minThreshold());
        item.setRestockQuantity(dto.restockQuantity() > 0 ? dto.restockQuantity() : 1.0);
        item.setDefaultPurchaseAmount(dto.defaultPurchaseAmount());
        item.setImageUrl(dto.imageUrl());
        item.setBarcode(dto.barcode());
        item.setAutoAddToBring(dto.autoAddToBring());
        item.setPerishable(dto.perishable());
        item.setPackageSize(dto.packageSize());
        item.setNotes(dto.notes());

        if (dto.categoryId() != null && !dto.categoryId().isBlank()) {
            categoryRepository.findById(dto.categoryId()).ifPresent(item::setCategory);
        }
        if (dto.defaultLocationId() != null && !dto.defaultLocationId().isBlank()) {
            locationRepository.findById(dto.defaultLocationId()).ifPresent(item::setDefaultLocation);
        }

        return itemRepository.save(item);
    }

    @Transactional
    public InventoryBatch addBatch(InventoryBatchDto dto) {
        Item item = itemRepository.findById(dto.itemId())
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + dto.itemId()));

        Location location = locationRepository.findById(dto.locationId())
                .orElse(item.getDefaultLocation() != null ? item.getDefaultLocation() : locationRepository.findAll().get(0));

        InventoryBatch batch = new InventoryBatch();
        batch.setItem(item);
        batch.setLocation(location);
        batch.setQuantity(dto.quantity());
        batch.setUnit(dto.unit() != null ? dto.unit() : item.getDefaultUnit());
        batch.setExpirationDate(dto.expirationDate());
        batch.setOpenedDate(dto.openedDate());
        batch.setPurchasedDate(dto.purchasedDate() != null ? dto.purchasedDate() : LocalDate.now());
        batch.setNote(dto.note());
        batch.setBarcode(dto.barcode() != null ? dto.barcode() : item.getBarcode());

        InventoryBatch saved = batchRepository.save(batch);
        checkAndSyncToBring(item);
        return saved;
    }

    @Transactional
    public double adjustQuantity(String itemId, double delta) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + itemId));

        if (delta > 0) {
            // Increasing quantity: add to the newest batch or create a new one
            List<InventoryBatch> batches = batchRepository.findByItemIdOrderByExpirationDateAsc(itemId);
            if (!batches.isEmpty()) {
                InventoryBatch latest = batches.get(batches.size() - 1);
                latest.setQuantity(latest.getQuantity() + delta);
                batchRepository.save(latest);
            } else {
                Location loc = item.getDefaultLocation() != null ? item.getDefaultLocation() : locationRepository.findAll().get(0);
                InventoryBatch newBatch = new InventoryBatch();
                newBatch.setItem(item);
                newBatch.setLocation(loc);
                newBatch.setQuantity(delta);
                newBatch.setUnit(item.getDefaultUnit());
                batchRepository.save(newBatch);
            }
        } else if (delta < 0) {
            // Decreasing quantity (FIFO)
            deductFifo(item, Math.abs(delta));
        }

        checkAndSyncToBring(item);
        Double total = batchRepository.getTotalQuantityForItem(itemId);
        return total != null ? total : 0.0;
    }

    @Transactional
    public void deductFifo(Item item, double quantityToDeduct) {
        List<InventoryBatch> batches = batchRepository.findByItemIdOrderByExpirationDateAsc(item.getId());
        double remainingToDeduct = quantityToDeduct;

        for (InventoryBatch batch : batches) {
            if (remainingToDeduct <= 0) break;

            if (batch.getQuantity() <= remainingToDeduct) {
                remainingToDeduct -= batch.getQuantity();
                batchRepository.delete(batch);
            } else {
                batch.setQuantity(batch.getQuantity() - remainingToDeduct);
                batchRepository.save(batch);
                remainingToDeduct = 0;
            }
        }
    }

    @Transactional
    public void consumeItem(String itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + itemId));
        List<InventoryBatch> batches = batchRepository.findByItemIdOrderByExpirationDateAsc(item.getId());
        batchRepository.deleteAll(batches);
        checkAndSyncToBring(item);
    }

    @Transactional
    public void moveLocation(String itemId, String batchId, String targetLocationId, Double quantity) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + itemId));
        Location targetLocation = locationRepository.findById(targetLocationId)
                .orElseThrow(() -> new NoSuchElementException("Target location not found: " + targetLocationId));

        if (batchId != null && !batchId.isBlank()) {
            InventoryBatch batch = batchRepository.findById(batchId)
                    .orElseThrow(() -> new NoSuchElementException("Batch not found: " + batchId));

            if (quantity == null || quantity <= 0 || quantity >= batch.getQuantity()) {
                // Move entire batch to new location
                batch.setLocation(targetLocation);
                batchRepository.save(batch);
            } else {
                // Split batch: reduce original and create new batch in target location
                batch.setQuantity(batch.getQuantity() - quantity);
                batchRepository.save(batch);

                InventoryBatch newBatch = new InventoryBatch();
                newBatch.setItem(item);
                newBatch.setLocation(targetLocation);
                newBatch.setQuantity(quantity);
                newBatch.setUnit(batch.getUnit());
                newBatch.setExpirationDate(batch.getExpirationDate());
                newBatch.setOpenedDate(batch.getOpenedDate());
                newBatch.setPurchasedDate(batch.getPurchasedDate());
                newBatch.setNote(batch.getNote());
                newBatch.setBarcode(batch.getBarcode());
                batchRepository.save(newBatch);
            }
        } else {
            // Move entire item: update all current batches and item defaultLocation
            List<InventoryBatch> batches = batchRepository.findByItemIdOrderByExpirationDateAsc(item.getId());
            for (InventoryBatch b : batches) {
                b.setLocation(targetLocation);
                batchRepository.save(b);
            }
            item.setDefaultLocation(targetLocation);
            itemRepository.save(item);
        }
    }

    @Transactional
    public void commitReceipt(CommitReceiptRequest request) {
        for (CommitReceiptRequest.CommitReceiptItem itemDto : request.items()) {
            Item item;
            if (itemDto.itemId() != null && !itemDto.itemId().isBlank()) {
                item = itemRepository.findById(itemDto.itemId()).orElse(null);
            } else if (itemDto.barcode() != null && !itemDto.barcode().isBlank()) {
                item = itemRepository.findByBarcode(itemDto.barcode()).orElse(null);
            } else {
                item = itemRepository.findByNameIgnoreCase(itemDto.name()).orElse(null);
            }

            if (item == null) {
                item = new Item();
                item.setName(itemDto.name());
                item.setBrand(itemDto.brand());
                item.setBarcode(itemDto.barcode());
                item.setImageUrl(itemDto.imageUrl());
                item.setDefaultUnit(itemDto.unit() != null ? itemDto.unit() : "count");
                
                if (itemDto.categoryId() != null) {
                    categoryRepository.findById(itemDto.categoryId()).ifPresent(item::setCategory);
                }
                if (itemDto.locationId() != null) {
                    locationRepository.findById(itemDto.locationId()).ifPresent(item::setDefaultLocation);
                }
                item = itemRepository.save(item);
            }

            Location location = null;
            if (itemDto.locationId() != null) {
                location = locationRepository.findById(itemDto.locationId()).orElse(null);
            }
            if (location == null) {
                location = item.getDefaultLocation() != null ? item.getDefaultLocation() : locationRepository.findAll().get(0);
            }

            InventoryBatch batch = new InventoryBatch();
            batch.setItem(item);
            batch.setLocation(location);
            batch.setQuantity(itemDto.quantity());
            batch.setUnit(itemDto.unit() != null ? itemDto.unit() : item.getDefaultUnit());
            batch.setExpirationDate(itemDto.expirationDate());
            batch.setPurchasedDate(LocalDate.now());
            batch.setBarcode(itemDto.barcode());
            batchRepository.save(batch);
        }
    }

    private void checkAndSyncToBring(Item item) {
        if (!item.isAutoAddToBring()) return;

        Double total = batchRepository.getTotalQuantityForItem(item.getId());
        double currentStock = total != null ? total : 0.0;

        if (currentStock <= item.getMinThreshold()) {
            String note;
            if (item.getDefaultPurchaseAmount() != null && !item.getDefaultPurchaseAmount().isBlank()) {
                note = item.getDefaultPurchaseAmount().trim();
            } else {
                note = item.getRestockQuantity() > 0 ? ((int) item.getRestockQuantity() + " " + item.getDefaultUnit()) : "";
            }
            bringSyncService.addItem(item.getName(), note);
        }
    }

    private PantryItemSummaryDto mapToSummary(Item item) {
        List<InventoryBatch> batches = batchRepository.findByItemIdOrderByExpirationDateAsc(item.getId());
        double totalQuantity = batches.stream().mapToDouble(InventoryBatch::getQuantity).sum();
        boolean isLowStock = totalQuantity <= item.getMinThreshold();
        boolean isOutOfStock = totalQuantity <= 0;

        LocalDate earliestExpiry = null;
        Integer daysUntilExpiry = null;
        String status = "NO_EXPIRY";

        for (InventoryBatch b : batches) {
            if (b.getExpirationDate() != null && b.getQuantity() > 0) {
                if (earliestExpiry == null || b.getExpirationDate().isBefore(earliestExpiry)) {
                    earliestExpiry = b.getExpirationDate();
                }
            }
        }

        if (earliestExpiry != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), earliestExpiry);
            daysUntilExpiry = (int) days;

            if (days < 0) {
                status = "EXPIRED";
            } else if (days <= 5) {
                status = "EXPIRING_SOON";
            } else {
                status = "FRESH";
            }
        }

        boolean freshCheckNeeded = false;
        Long daysInStorage = null;

        if (item.isPerishable() && !isOutOfStock) {
            LocalDate earliestAdded = null;
            for (InventoryBatch b : batches) {
                if (b.getQuantity() > 0) {
                    LocalDate d = b.getPurchasedDate() != null ? b.getPurchasedDate() : (b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate() : LocalDate.now());
                    if (earliestAdded == null || d.isBefore(earliestAdded)) {
                        earliestAdded = d;
                    }
                }
            }
            if (earliestAdded != null) {
                long days = ChronoUnit.DAYS.between(earliestAdded, LocalDate.now());
                daysInStorage = Math.max(0, days);
                int reminderDays = settingsService != null ? settingsService.getFreshFoodReminderDays() : 5;
                boolean reminderEnabled = settingsService != null ? settingsService.isFreshFoodReminderEnabled() : true;
                if (reminderEnabled && days >= reminderDays && (status.equals("NO_EXPIRY") || status.equals("FRESH"))) {
                    freshCheckNeeded = true;
                }
            }
        }

        List<InventoryBatchDto> batchDtos = batches.stream().map(b -> new InventoryBatchDto(
                b.getId(),
                item.getId(),
                item.getName(),
                b.getLocation().getId(),
                b.getLocation().getName(),
                b.getQuantity(),
                b.getUnit(),
                b.getExpirationDate(),
                b.getOpenedDate(),
                b.getPurchasedDate(),
                b.getNote(),
                b.getBarcode()
        )).collect(Collectors.toList());

        return new PantryItemSummaryDto(
                item.getId(),
                item.getName(),
                item.getBrand(),
                item.getCategory(),
                item.getDefaultLocation(),
                item.getDefaultUnit(),
                item.getMinThreshold(),
                item.getRestockQuantity(),
                item.getDefaultPurchaseAmount(),
                item.getImageUrl(),
                item.getBarcode(),
                item.isAutoAddToBring(),
                item.isPerishable(),
                item.getPackageSize(),
                item.getNotes(),
                totalQuantity,
                isLowStock,
                isOutOfStock,
                daysUntilExpiry,
                earliestExpiry,
                status,
                freshCheckNeeded,
                daysInStorage,
                batchDtos
        );
    }

    @Transactional
    public PantryItemSummaryDto resetFreshness(String itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + itemId));
        List<InventoryBatch> batches = batchRepository.findByItemIdOrderByExpirationDateAsc(item.getId());
        for (InventoryBatch b : batches) {
            if (b.getQuantity() > 0) {
                b.setPurchasedDate(LocalDate.now());
                batchRepository.save(b);
            }
        }
        return mapToSummary(item);
    }

    public NotificationSummaryDto getNotificationSummary() {
        boolean expEnabled = settingsService != null ? settingsService.isExpirationReminderEnabled() : true;
        int expThreshold = settingsService != null ? settingsService.getExpirationReminderDays() : 3;
        boolean freshEnabled = settingsService != null ? settingsService.isFreshFoodReminderEnabled() : true;

        List<PantryItemSummaryDto> allItems = getAllPantrySummaries(null, null, null, null, "NAME");

        List<PantryItemSummaryDto> expiredItems = new ArrayList<>();
        List<PantryItemSummaryDto> expiringSoonItems = new ArrayList<>();
        List<PantryItemSummaryDto> freshCheckItems = new ArrayList<>();
        List<PantryItemSummaryDto> lowStockItems = new ArrayList<>();

        for (PantryItemSummaryDto item : allItems) {
            // Consumed / out-of-stock items are excluded from expiration & freshness notifications
            if (item.isOutOfStock()) {
                continue;
            }

            if (expEnabled) {
                if ("EXPIRED".equalsIgnoreCase(item.expiryStatus())) {
                    expiredItems.add(item);
                } else if (item.daysUntilEarliestExpiry() != null && item.daysUntilEarliestExpiry() <= expThreshold) {
                    expiringSoonItems.add(item);
                }
            }

            if (freshEnabled && item.freshCheckNeeded()) {
                freshCheckItems.add(item);
            }

            if (item.isLowStock()) {
                lowStockItems.add(item);
            }
        }

        // Sort expiring soon items by closest expiry first
        expiringSoonItems.sort(Comparator.comparing(
                s -> s.daysUntilEarliestExpiry() != null ? s.daysUntilEarliestExpiry() : Integer.MAX_VALUE
        ));

        int totalAlertCount = expiredItems.size() + expiringSoonItems.size() + freshCheckItems.size();

        return new NotificationSummaryDto(
                totalAlertCount,
                expiredItems.size(),
                expiringSoonItems.size(),
                freshCheckItems.size(),
                lowStockItems.size(),
                expiredItems,
                expiringSoonItems,
                freshCheckItems,
                lowStockItems
        );
    }
}
