package com.skafferi.controller;

import com.skafferi.dto.SettingsDto;
import com.skafferi.service.EmailNotificationService;
import com.skafferi.service.RecipeService;
import com.skafferi.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final EmailNotificationService emailNotificationService;
    private final RecipeService recipeService;

    public SettingsController(SettingsService settingsService,
                              EmailNotificationService emailNotificationService,
                              RecipeService recipeService) {
        this.settingsService = settingsService;
        this.emailNotificationService = emailNotificationService;
        this.recipeService = recipeService;
    }

    @GetMapping
    public ResponseEntity<SettingsDto> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PostMapping
    public ResponseEntity<SettingsDto> saveSettings(@RequestBody SettingsDto dto) {
        return ResponseEntity.ok(settingsService.saveSettings(dto));
    }

    @PostMapping("/test-email")
    public ResponseEntity<Map<String, Object>> testEmail() {
        try {
            emailNotificationService.sendTestEmail();
            return ResponseEntity.ok(Map.of("success", true, "message", "Test email sent successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/send-digest")
    public ResponseEntity<Map<String, Object>> sendDigest(@RequestParam(defaultValue = "daily") String type) {
        try {
            if ("weekly".equalsIgnoreCase(type)) {
                emailNotificationService.sendWeeklyGroceryDigest();
            } else {
                emailNotificationService.sendDailyExpirationDigest();
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "Digest dispatched successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/test-llm")
    public ResponseEntity<Map<String, Object>> testLlm(@RequestBody(required = false) Map<String, String> request) {
        String apiKey = request != null ? request.get("apiKey") : null;
        String provider = request != null ? request.get("provider") : null;
        String model = request != null ? request.get("model") : null;
        String baseUrl = request != null ? request.get("baseUrl") : null;
        return ResponseEntity.ok(recipeService.testLlmConnection(apiKey, provider, model, baseUrl));
    }

    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> getVersion() {
        return ResponseEntity.ok(Map.of("version", settingsService.getAppVersion()));
    }
}
