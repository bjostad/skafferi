package com.skafferi.service;

import com.skafferi.domain.Item;
import com.skafferi.domain.PurchaseRecord;
import com.skafferi.repository.ItemRepository;
import com.skafferi.repository.PurchaseRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConsumptionModelingService {

    private final PurchaseRecordRepository purchaseRecordRepository;
    private final ItemRepository itemRepository;
    private final com.skafferi.repository.InventoryBatchRepository batchRepository;

    public ConsumptionModelingService(PurchaseRecordRepository purchaseRecordRepository,
                                      ItemRepository itemRepository,
                                      com.skafferi.repository.InventoryBatchRepository batchRepository) {
        this.purchaseRecordRepository = purchaseRecordRepository;
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
    }

    public List<Map<String, Object>> getDepletionPredictions() {
        List<Item> allItems = itemRepository.findAll();
        List<Map<String, Object>> predictions = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Item item : allItems) {
            List<PurchaseRecord> records = purchaseRecordRepository.findByItemIdOrderByPurchasedDateDescCreatedAtDesc(item.getId());
            if (records.size() < 2) {
                continue;
            }

            // Filter records with valid dates
            List<LocalDate> dates = records.stream()
                    .map(r -> r.getPurchasedDate() != null ? r.getPurchasedDate() : (r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate() : null))
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());

            if (dates.size() < 2) continue;

            LocalDate earliest = dates.get(0);
            LocalDate latest = dates.get(dates.size() - 1);
            long totalDays = ChronoUnit.DAYS.between(earliest, latest);
            if (totalDays <= 0) totalDays = 1;

            double totalQtyPurchased = records.stream()
                    .mapToDouble(r -> r.getQuantity() != null && r.getQuantity() > 0 ? r.getQuantity() : 1.0)
                    .sum();

            double dailyRate = totalQtyPurchased / (double) totalDays;
            double avgIntervalDays = (double) totalDays / (double) (dates.size() - 1);

            Double currentQtyVal = batchRepository.getTotalQuantityForItem(item.getId());
            double currentQty = currentQtyVal != null ? currentQtyVal : 0.0;

            Double daysRemaining = null;
            LocalDate estimatedRunOutDate = null;
            boolean runsOutSoon = false;

            if (currentQty <= 0) {
                daysRemaining = 0.0;
                estimatedRunOutDate = today;
                runsOutSoon = true;
            } else if (dailyRate > 0) {
                daysRemaining = currentQty / dailyRate;
                long roundDays = Math.max(0, Math.round(daysRemaining));
                estimatedRunOutDate = today.plusDays(roundDays);
                runsOutSoon = roundDays <= 5;
            }

            predictions.add(Map.of(
                    "itemId", item.getId(),
                    "itemName", item.getName(),
                    "unit", item.getDefaultUnit() != null ? item.getDefaultUnit() : "count",
                    "currentQuantity", Math.round(currentQty * 10.0) / 10.0,
                    "purchaseCount", records.size(),
                    "dailyConsumptionRate", Math.round(dailyRate * 1000.0) / 1000.0,
                    "avgPurchaseCycleDays", Math.round(avgIntervalDays * 10.0) / 10.0,
                    "estimatedDaysRemaining", daysRemaining != null ? Math.round(daysRemaining * 10.0) / 10.0 : null,
                    "estimatedRunOutDate", estimatedRunOutDate != null ? estimatedRunOutDate.toString() : "",
                    "runsOutSoon", runsOutSoon
            ));
        }

        // Sort: items that run out soonest first
        predictions.sort((a, b) -> {
            Double d1 = (Double) a.get("estimatedDaysRemaining");
            Double d2 = (Double) b.get("estimatedDaysRemaining");
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return Double.compare(d1, d2);
        });

        return predictions;
    }

    public List<Map<String, Object>> getSeasonalSuggestions() {
        List<Item> allItems = itemRepository.findAll();
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        List<Map<String, Object>> seasonalMatches = new ArrayList<>();

        for (Item item : allItems) {
            List<PurchaseRecord> records = purchaseRecordRepository.findByItemIdOrderByPurchasedDateDescCreatedAtDesc(item.getId());
            if (records.size() < 3) {
                continue;
            }

            // Group purchases by month
            Map<Integer, Long> countByMonth = records.stream()
                    .map(r -> r.getPurchasedDate() != null ? r.getPurchasedDate() : (r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate() : null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(LocalDate::getMonthValue, Collectors.counting()));

            // Check if current month or adjacent months (+-1) have significant purchase activity
            long currentWindowPurchases = countByMonth.getOrDefault(currentMonth, 0L)
                    + countByMonth.getOrDefault(prevMonth(currentMonth), 0L)
                    + countByMonth.getOrDefault(nextMonth(currentMonth), 0L);

            double seasonalRatio = (double) currentWindowPurchases / (double) records.size();

            // If 50% or more of historical purchases occurred in this 3-month window
            if (seasonalRatio >= 0.50 && currentWindowPurchases >= 2) {
                Double currentQtyVal = batchRepository.getTotalQuantityForItem(item.getId());
                double currentQty = currentQtyVal != null ? currentQtyVal : 0.0;

                String monthName = Month.of(currentMonth).name();
                seasonalMatches.add(Map.of(
                        "itemId", item.getId(),
                        "itemName", item.getName(),
                        "currentQuantity", Math.round(currentQty * 10.0) / 10.0,
                        "unit", item.getDefaultUnit() != null ? item.getDefaultUnit() : "count",
                        "seasonalWindow", formatSeasonWindow(currentMonth),
                        "totalSeasonalPurchases", currentWindowPurchases,
                        "confidencePercent", (int) Math.round(seasonalRatio * 100),
                        "recommendationReason", "Historically purchased frequently during " + formatSeasonWindow(currentMonth)
                ));
            }
        }

        return seasonalMatches;
    }

    private int prevMonth(int m) {
        return m == 1 ? 12 : m - 1;
    }

    private int nextMonth(int m) {
        return m == 12 ? 1 : m + 1;
    }

    private String formatSeasonWindow(int m) {
        int p = prevMonth(m);
        int n = nextMonth(m);
        return Month.of(p).name().substring(0, 3) + " - " + Month.of(n).name().substring(0, 3);
    }
}
