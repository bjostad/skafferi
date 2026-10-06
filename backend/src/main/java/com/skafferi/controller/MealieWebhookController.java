package com.skafferi.controller;

import com.skafferi.dto.MealieWebhookPayload;
import com.skafferi.service.MealieWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/mealie")
public class MealieWebhookController {

    private final MealieWebhookService mealieWebhookService;

    public MealieWebhookController(MealieWebhookService mealieWebhookService) {
        this.mealieWebhookService = mealieWebhookService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody MealieWebhookPayload payload) {
        mealieWebhookService.processMealCooked(payload);
        return ResponseEntity.ok(Map.of("status", "processed", "recipe", payload.recipeName() != null ? payload.recipeName() : "unknown"));
    }
}
