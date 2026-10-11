package com.skafferi.controller;

import com.skafferi.service.ConsumptionModelingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class PredictionController {

    private final ConsumptionModelingService consumptionModelingService;

    public PredictionController(ConsumptionModelingService consumptionModelingService) {
        this.consumptionModelingService = consumptionModelingService;
    }

    @GetMapping("/predictions")
    public ResponseEntity<List<Map<String, Object>>> getPredictions() {
        return ResponseEntity.ok(consumptionModelingService.getDepletionPredictions());
    }

    @GetMapping("/seasonal-suggestions")
    public ResponseEntity<List<Map<String, Object>>> getSeasonalSuggestions() {
        return ResponseEntity.ok(consumptionModelingService.getSeasonalSuggestions());
    }
}
