package com.skafferi.controller;

import com.skafferi.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(analyticsService.getOverallSpendSummary());
    }

    @GetMapping("/spend")
    public ResponseEntity<List<Map<String, Object>>> getMonthlySpend() {
        return ResponseEntity.ok(analyticsService.getMonthlySpend());
    }

    @GetMapping("/stores")
    public ResponseEntity<Map<String, Object>> getStoreComparison() {
        return ResponseEntity.ok(analyticsService.getStoreComparison());
    }

    @GetMapping("/prices")
    public ResponseEntity<Map<String, Object>> getPriceHistory(
            @RequestParam(required = false) String itemId,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(analyticsService.getItemPriceHistory(itemId, query));
    }
}
