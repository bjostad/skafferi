package com.skafferi.controller;

import com.skafferi.service.BringSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bring")
public class BringController {

    private final BringSyncService bringSyncService;

    public BringController(BringSyncService bringSyncService) {
        this.bringSyncService = bringSyncService;
    }

    @GetMapping("/list")
    public List<Map<String, String>> getShoppingList() {
        return bringSyncService.getBringShoppingList();
    }

    @GetMapping("/lists")
    public List<Map<String, String>> getAllLists() {
        return bringSyncService.loadAllLists();
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addItem(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String spec = payload.get("specification");
        boolean success = bringSyncService.addItem(name, spec);
        if (!success) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failed to add to Bring!. Check your connection or list selection in Settings."));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testAuth(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        boolean success = bringSyncService.authenticate(email, password);
        return ResponseEntity.ok(Map.of("success", success));
    }
}
