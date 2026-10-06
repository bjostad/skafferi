package com.skafferi.controller;

import com.skafferi.domain.Location;
import com.skafferi.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public List<Location> getAllLocations() {
        return locationService.getAllLocations();
    }

    @PostMapping
    public ResponseEntity<Location> createOrUpdateLocation(@RequestBody Location location) {
        Location saved = locationService.createOrUpdateLocation(location);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable String id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reorder")
    public ResponseEntity<Void> reorderLocations(@RequestBody List<String> orderedIds) {
        locationService.reorderLocations(orderedIds);
        return ResponseEntity.ok().build();
    }
}
