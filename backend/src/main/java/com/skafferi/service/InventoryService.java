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
    private final PurchaseRecordService purchaseRecordService;

    public InventoryService(ItemRepository itemRepository,
                            InventoryBatchRepository batchRepository,
                            LocationRepository locationRepository,
                            CategoryRepository categoryRepository,
                            BringSyncService bringSyncService,
                            SettingsService settingsService,
                            PurchaseRecordService purchaseRecordService) {
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.locationRepository = locationRepository;
        this.categoryRepository = categoryRepository;
        this.bringSyncService = bringSyncService;
        this.settingsService = settingsService;
        this.purchaseRecordService = purchaseRecordService;
    }

    private List<String> cleanFilterList(List<String> raw) {
        if (raw == null) return List.of();
        return raw.stream()
                .filter(Objects::nonNull)
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isBlank() && !"ALL".equalsIgnoreCase(s))
                .collect(Collectors.toList());
    }

    public List<PantryItemSummaryDto> getAllPantrySummaries(
            List<String> locationIds,
            List<String> categoryIds,
            String searchQuery,
            List<String> stockFilters,
            List<String> freshnessFilters,
            String legacyStatus,
            String sortBy) {

        List<String> cleanLocations = cleanFilterList(locationIds);
        List<String> cleanCategories = cleanFilterList(categoryIds);
        List<String> cleanStocks = cleanFilterList(stockFilters);
        List<String> cleanFreshness = cleanFilterList(freshnessFilters);

        List<Item> items;
        if (searchQuery != null && !searchQuery.isBlank()) {
            items = itemRepository.findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(searchQuery.trim(), searchQuery.trim());
        } else {
            items = itemRepository.findAll();
        }

        List<PantryItemSummaryDto> summaries = items.stream()
                .filter(item -> {
                    if (cleanCategories.isEmpty()) return true;
                    return item.getCategory() != null && cleanCategories.stream()
                            .anyMatch(catId -> catId.equalsIgnoreCase(item.getCategory().getId()));
                })
                .map(this::mapToSummary)
                .filter(summary -> {
                    if (cleanLocations.isEmpty()) return true;
                    boolean hasBatchInLocation = summary.batches().stream()
                            .anyMatch(b -> cleanLocations.stream().anyMatch(locId -> locId.equalsIgnoreCase(b.locationId())));
                    boolean defaultMatches = summary.defaultLocation() != null && cleanLocations.stream()
                            .anyMatch(locId -> locId.equalsIgnoreCase(summary.defaultLocation().getId()));
                    return hasBatchInLocation || defaultMatches;
                })
                .filter(summary -> {
                    if (cleanStocks.isEmpty()) return true;
                    return cleanStocks.stream().anyMatch(stock -> {
                        if ("IN_STOCK".equalsIgnoreCase(stock)) {
                            return summary.totalQuantity() > 0;
                        }
                        if ("LOW_STOCK".equalsIgnoreCase(stock)) {
                            return summary.isLowStock() && !summary.isOutOfStock();
                        }
                        if ("OUT_OF_STOCK".equalsIgnoreCase(stock)) {
                            return summary.isOutOfStock();
                        }
                        return false;
                    });
                })
                .filter(summary -> {
                    if (cleanFreshness.isEmpty()) return true;
                    return cleanFreshness.stream().anyMatch(freshness -> {
                        if ("FRESH".equalsIgnoreCase(freshness)) {
                            return "FRESH".equalsIgnoreCase(summary.expiryStatus()) && !summary.freshCheckNeeded();
                        }
                        if ("EXPIRING_SOON".equalsIgnoreCase(freshness)) {
                            return "EXPIRING_SOON".equalsIgnoreCase(summary.expiryStatus());
                        }
                        if ("EXPIRED".equalsIgnoreCase(freshness)) {
                            return "EXPIRED".equalsIgnoreCase(summary.expiryStatus());
                        }
                        if ("FRESH_CHECK".equalsIgnoreCase(freshness)) {
                            return summary.freshCheckNeeded();
                        }
                        return false;
                    });
                })
                .filter(summary -> {
                    if (!cleanStocks.isEmpty() || !cleanFreshness.isEmpty()) {
                        return true;
                    }
                    if (legacyStatus == null || legacyStatus.isBlank() || "ALL".equalsIgnoreCase(legacyStatus)) {
                        return true;
                    }
                    if ("EXPIRED".equalsIgnoreCase(legacyStatus)) {
                        return "EXPIRED".equalsIgnoreCase(summary.expiryStatus());
                    }
                    if ("EXPIRING_SOON".equalsIgnoreCase(legacyStatus)) {
                        return "EXPIRING_SOON".equalsIgnoreCase(summary.expiryStatus());
                    }
                    if ("FRESH_CHECK".equalsIgnoreCase(legacyStatus)) {
                        return summary.freshCheckNeeded();
                    }
                    if ("FRESH".equalsIgnoreCase(legacyStatus)) {
                        return "FRESH".equalsIgnoreCase(summary.expiryStatus()) && !summary.freshCheckNeeded();
                    }
                    if ("LOW_STOCK".equalsIgnoreCase(legacyStatus)) {
                        return summary.isLowStock() && !summary.isOutOfStock();
                    }
                    if ("OUT_OF_STOCK".equalsIgnoreCase(legacyStatus)) {
                        return summary.isOutOfStock();
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Sorting
        Comparator<PantryItemSummaryDto> comparator;
        if ("EXPIRY".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(
                    s -> s.daysUntilEarliestExpiry() != null ? s.daysUntilEarliestExpiry() : Integer.MAX_VALUE
            );
        } else if ("QTY_ASC".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingDouble(PantryItemSummaryDto::totalQuantity);
        } else if ("QTY_DESC".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingDouble(PantryItemSummaryDto::totalQuantity).reversed();
        } else {
            comparator = Comparator.comparing(PantryItemSummaryDto::name, String.CASE_INSENSITIVE_ORDER);
        }

        summaries.sort(comparator);
        return summaries;
    }

    public List<PantryItemSummaryDto> getAllPantrySummaries(String locationId, String categoryId, String searchQuery, String statusFilter, String sortBy) {
        List<String> locs = (locationId != null && !locationId.isBlank()) ? List.of(locationId) : null;
        List<String> cats = (categoryId != null && !categoryId.isBlank()) ? List.of(categoryId) : null;
        return getAllPantrySummaries(locs, cats, searchQuery, null, null, statusFilter, sortBy);
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
        batch.setUnitPrice(dto.unitPrice());
        batch.setStore(dto.store());

        InventoryBatch saved = batchRepository.save(batch);

        if (dto.unitPrice() != null || (dto.store() != null && !dto.store().isBlank())) {
            Double totalPrice = dto.unitPrice() != null ? dto.unitPrice() * dto.quantity() : null;
            purchaseRecordService.recordPurchase(
                    item,
                    batch.getPurchasedDate(),
                    dto.store(),
                    dto.quantity(),
                    batch.getUnit(),
                    dto.unitPrice(),
                    totalPrice,
                    dto.note(),
                    "MANUAL"
            );
        }

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

            LocalDate purchaseDate = itemDto.purchasedDate() != null ? itemDto.purchasedDate()
                    : (request.transactionDate() != null ? request.transactionDate() : LocalDate.now());
            String storeName = itemDto.store() != null && !itemDto.store().isBlank() ? itemDto.store() : request.storeName();

            InventoryBatch batch = new InventoryBatch();
            batch.setItem(item);
            batch.setLocation(location);
            batch.setQuantity(itemDto.quantity());
            batch.setUnit(itemDto.unit() != null ? itemDto.unit() : item.getDefaultUnit());
            batch.setExpirationDate(itemDto.expirationDate());
            batch.setPurchasedDate(purchaseDate);
            batch.setBarcode(itemDto.barcode());
            batch.setUnitPrice(itemDto.unitPrice());
            batch.setStore(storeName);
            batchRepository.save(batch);

            Double totalCost = itemDto.totalPrice() != null ? itemDto.totalPrice()
                    : (itemDto.unitPrice() != null ? itemDto.unitPrice() * itemDto.quantity() : null);
            purchaseRecordService.recordPurchase(
                    item,
                    purchaseDate,
                    storeName,
                    itemDto.quantity(),
                    itemDto.unit() != null ? itemDto.unit() : item.getDefaultUnit(),
                    itemDto.unitPrice(),
                    totalCost,
                    null,
                    "RECEIPT"
            );
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
                b.getBarcode(),
                b.getUnitPrice(),
                b.getStore()
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
