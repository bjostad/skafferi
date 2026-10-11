package com.skafferi.controller;

import com.skafferi.dto.SettingsDto;
import com.skafferi.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final com.skafferi.service.EmailNotificationService emailNotificationService;

    public SettingsController(SettingsService settingsService, com.skafferi.service.EmailNotificationService emailNotificationService) {
        this.settingsService = settingsService;
        this.emailNotificationService = emailNotificationService;
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
    public ResponseEntity<java.util.Map<String, Object>> testEmail() {
        try {
            emailNotificationService.sendTestEmail();
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Test email sent successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/send-digest")
    public ResponseEntity<java.util.Map<String, Object>> sendDigest(@RequestParam(defaultValue = "daily") String type) {
        try {
            if ("weekly".equalsIgnoreCase(type)) {
                emailNotificationService.sendWeeklyGroceryDigest();
            } else {
                emailNotificationService.sendDailyExpirationDigest();
            }
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Digest dispatched successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/version")
    public ResponseEntity<java.util.Map<String, String>> getVersion() {
        return ResponseEntity.ok(java.util.Map.of("version", settingsService.getAppVersion()));
    }
}
