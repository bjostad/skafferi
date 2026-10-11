package com.skafferi.service;

import com.skafferi.domain.Item;
import com.skafferi.domain.PurchaseRecord;
import com.skafferi.repository.PurchaseRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final PurchaseRecordRepository purchaseRecordRepository;

    public AnalyticsService(PurchaseRecordRepository purchaseRecordRepository) {
        this.purchaseRecordRepository = purchaseRecordRepository;
    }

    public Map<String, Object> getOverallSpendSummary() {
        List<PurchaseRecord> records = purchaseRecordRepository.findAll();

        double totalSpend = records.stream()
                .mapToDouble(this::calculateRecordTotal)
                .sum();

        long distinctStores = records.stream()
                .map(PurchaseRecord::getStore)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .count();

        long distinctItems = records.stream()
                .map(r -> r.getItem() != null ? r.getItem().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return Map.of(
                "totalSpend", Math.round(totalSpend * 100.0) / 100.0,
                "totalTransactions", records.size(),
                "distinctStoresCount", distinctStores,
                "distinctItemsCount", distinctItems
        );
    }

    public List<Map<String, Object>> getMonthlySpend() {
        List<PurchaseRecord> records = purchaseRecordRepository.findAll();
        DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        Map<String, List<PurchaseRecord>> byMonth = records.stream()
                .collect(Collectors.groupingBy(r -> {
                    LocalDate d = r.getPurchasedDate() != null ? r.getPurchasedDate() : (r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate() : LocalDate.now());
                    return d.format(ymFormatter);
                }, TreeMap::new, Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<PurchaseRecord>> entry : byMonth.entrySet()) {
            String month = entry.getKey();
            List<PurchaseRecord> monthRecords = entry.getValue();

            double totalSpend = monthRecords.stream().mapToDouble(this::calculateRecordTotal).sum();

            Map<String, Double> byCategory = monthRecords.stream()
                    .collect(Collectors.groupingBy(
                            r -> (r.getItem() != null && r.getItem().getCategory() != null) ? r.getItem().getCategory().getName() : "Uncategorized",
                            Collectors.summingDouble(this::calculateRecordTotal)
                    ));

            Map<String, Double> byStore = monthRecords.stream()
                    .collect(Collectors.groupingBy(
                            r -> (r.getStore() != null && !r.getStore().isBlank()) ? r.getStore().trim() : "Other / Unknown",
                            Collectors.summingDouble(this::calculateRecordTotal)
                    ));

            result.add(Map.of(
                    "month", month,
                    "totalSpend", Math.round(totalSpend * 100.0) / 100.0,
                    "transactionCount", monthRecords.size(),
                    "byCategory", byCategory,
                    "byStore", byStore
            ));
        }

        return result;
    }

    public Map<String, Object> getStoreComparison() {
        List<PurchaseRecord> records = purchaseRecordRepository.findAll();

        Map<String, List<PurchaseRecord>> byStore = records.stream()
                .filter(r -> r.getStore() != null && !r.getStore().isBlank())
                .collect(Collectors.groupingBy(r -> r.getStore().trim()));

        List<Map<String, Object>> storeStats = new ArrayList<>();
        for (Map.Entry<String, List<PurchaseRecord>> entry : byStore.entrySet()) {
            String store = entry.getKey();
            List<PurchaseRecord> list = entry.getValue();

            double spend = list.stream().mapToDouble(this::calculateRecordTotal).sum();
            double avgUnitPrice = list.stream()
                    .filter(r -> r.getUnitPrice() != null && r.getUnitPrice() > 0)
                    .mapToDouble(PurchaseRecord::getUnitPrice)
                    .average()
                    .orElse(0.0);

            storeStats.add(Map.of(
                    "storeName", store,
                    "transactionCount", list.size(),
                    "totalSpend", Math.round(spend * 100.0) / 100.0,
                    "averageUnitPrice", Math.round(avgUnitPrice * 100.0) / 100.0
            ));
        }

        // Compare items purchased across multiple stores
        Map<String, List<PurchaseRecord>> byItem = records.stream()
                .filter(r -> r.getItem() != null && r.getUnitPrice() != null && r.getUnitPrice() > 0 && r.getStore() != null && !r.getStore().isBlank())
                .collect(Collectors.groupingBy(r -> r.getItem().getName().trim().toLowerCase()));

        List<Map<String, Object>> crossStoreItems = new ArrayList<>();
        for (Map.Entry<String, List<PurchaseRecord>> entry : byItem.entrySet()) {
            List<PurchaseRecord> itemRecords = entry.getValue();
            Set<String> stores = itemRecords.stream().map(r -> r.getStore().trim()).collect(Collectors.toSet());
            if (stores.size() >= 2) {
                String itemName = itemRecords.get(0).getItem().getName();
                String unit = itemRecords.get(0).getItem().getDefaultUnit();

                Map<String, Double> avgByStore = itemRecords.stream()
                        .collect(Collectors.groupingBy(
                                r -> r.getStore().trim(),
                                Collectors.averagingDouble(PurchaseRecord::getUnitPrice)
                        ));

                String cheapestStore = avgByStore.entrySet().stream()
                        .min(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);

                double lowestPrice = avgByStore.getOrDefault(cheapestStore, 0.0);

                crossStoreItems.add(Map.of(
                        "itemName", itemName,
                        "unit", unit != null ? unit : "unit",
                        "storePrices", avgByStore,
                        "cheapestStore", cheapestStore != null ? cheapestStore : "",
                        "cheapestPrice", Math.round(lowestPrice * 100.0) / 100.0
                ));
            }
        }

        return Map.of(
                "stores", storeStats,
                "crossStoreComparisons", crossStoreItems
        );
    }

    public Map<String, Object> getItemPriceHistory(String itemId, String query) {
        List<PurchaseRecord> records;
        if (itemId != null && !itemId.isBlank()) {
            records = purchaseRecordRepository.findByItemIdOrderByPurchasedDateDescCreatedAtDesc(itemId);
        } else {
            records = purchaseRecordRepository.findAll();
            if (query != null && !query.isBlank()) {
                String q = query.trim().toLowerCase();
                records = records.stream()
                        .filter(r -> r.getItem() != null && r.getItem().getName() != null && r.getItem().getName().toLowerCase().contains(q))
                        .collect(Collectors.toList());
            }
        }

        records.sort((a, b) -> {
            LocalDate da = a.getPurchasedDate() != null ? a.getPurchasedDate() : LocalDate.MIN;
            LocalDate db = b.getPurchasedDate() != null ? b.getPurchasedDate() : LocalDate.MIN;
            return da.compareTo(db);
        });

        List<Map<String, Object>> points = new ArrayList<>();
        double lowestPrice = Double.MAX_VALUE;
        double highestPrice = 0.0;
        String cheapestStore = "";

        for (PurchaseRecord r : records) {
            Double price = r.getUnitPrice();
            if (price == null && r.getTotalPrice() != null && r.getQuantity() != null && r.getQuantity() > 0) {
                price = r.getTotalPrice() / r.getQuantity();
            }
            if (price == null) continue;

            if (price < lowestPrice) {
                lowestPrice = price;
                cheapestStore = r.getStore() != null ? r.getStore() : "";
            }
            if (price > highestPrice) {
                highestPrice = price;
            }

            LocalDate d = r.getPurchasedDate() != null ? r.getPurchasedDate() : (r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate() : LocalDate.now());

            points.add(Map.of(
                    "date", d.toString(),
                    "unitPrice", Math.round(price * 100.0) / 100.0,
                    "totalPrice", r.getTotalPrice() != null ? Math.round(r.getTotalPrice() * 100.0) / 100.0 : Math.round(price * (r.getQuantity() != null ? r.getQuantity() : 1.0) * 100.0) / 100.0,
                    "store", r.getStore() != null ? r.getStore() : "Unknown",
                    "quantity", r.getQuantity() != null ? r.getQuantity() : 1.0,
                    "unit", r.getUnit() != null ? r.getUnit() : "",
                    "itemName", r.getItem() != null ? r.getItem().getName() : "Item"
            ));
        }

        double avg = points.stream()
                .mapToDouble(p -> ((Number) p.get("unitPrice")).doubleValue())
                .average()
                .orElse(0.0);

        return Map.of(
                "pricePoints", points,
                "lowestPrice", lowestPrice == Double.MAX_VALUE ? 0.0 : Math.round(lowestPrice * 100.0) / 100.0,
                "highestPrice", Math.round(highestPrice * 100.0) / 100.0,
                "averagePrice", Math.round(avg * 100.0) / 100.0,
                "cheapestStore", cheapestStore,
                "recordCount", points.size()
        );
    }

    private double calculateRecordTotal(PurchaseRecord r) {
        if (r.getTotalPrice() != null && r.getTotalPrice() > 0) {
            return r.getTotalPrice();
        }
        if (r.getUnitPrice() != null && r.getQuantity() != null) {
            return r.getUnitPrice() * r.getQuantity();
        }
        if (r.getUnitPrice() != null) {
            return r.getUnitPrice();
        }
        return 0.0;
    }
}
