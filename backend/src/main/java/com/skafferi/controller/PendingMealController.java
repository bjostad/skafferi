package com.skafferi.controller;

import com.skafferi.dto.PendingMealDto;
import com.skafferi.dto.ReconcileMealRequestDto;
import com.skafferi.service.PendingMealService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meals/pending")
public class PendingMealController {

    private final PendingMealService pendingMealService;

    public PendingMealController(PendingMealService pendingMealService) {
        this.pendingMealService = pendingMealService;
    }

    @GetMapping
    public ResponseEntity<List<PendingMealDto>> getPendingMeals() {
        return ResponseEntity.ok(pendingMealService.getPendingMeals());
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        return ResponseEntity.ok(Map.of("count", pendingMealService.getPendingCount()));
    }

    @PostMapping("/{id}/reconcile")
    public ResponseEntity<PendingMealDto> reconcileMeal(@PathVariable String id,
                                                        @RequestBody(required = false) ReconcileMealRequestDto request) {
        return ResponseEntity.ok(pendingMealService.reconcileMeal(id, request));
    }

    @PostMapping("/reconcile-all")
    public ResponseEntity<List<PendingMealDto>> reconcileAll() {
        return ResponseEntity.ok(pendingMealService.reconcileAllPending());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> dismissMeal(@PathVariable String id) {
        pendingMealService.dismissMeal(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
