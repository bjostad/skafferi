package com.skafferi.controller;

import com.skafferi.dto.SettingsDto;
import com.skafferi.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<SettingsDto> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PostMapping
    public ResponseEntity<SettingsDto> saveSettings(@RequestBody SettingsDto dto) {
        return ResponseEntity.ok(settingsService.saveSettings(dto));
    }

    @GetMapping("/version")
    public ResponseEntity<java.util.Map<String, String>> getVersion() {
        return ResponseEntity.ok(java.util.Map.of("version", settingsService.getAppVersion()));
    }
}
