package com.skafferi.service;

import com.skafferi.domain.Location;
import com.skafferi.repository.InventoryBatchRepository;
import com.skafferi.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final InventoryBatchRepository batchRepository;

    public LocationService(LocationRepository locationRepository, InventoryBatchRepository batchRepository) {
        this.locationRepository = locationRepository;
        this.batchRepository = batchRepository;
    }

    public List<Location> getAllLocations() {
        return locationRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public Location createOrUpdateLocation(Location location) {
        if (location.getId() == null || location.getId().isBlank()) {
            location.setId(UUID.randomUUID().toString());
            if (location.getSortOrder() == 0) {
                location.setSortOrder((int) locationRepository.count() + 1);
            }
        }
        return locationRepository.save(location);
    }

    @Transactional
    public void deleteLocation(String id) {
        if (locationRepository.count() <= 1) {
            throw new IllegalStateException("You must keep at least one storage location.");
        }

        // Reassign any batches currently stored here to the first available location
        List<Location> all = locationRepository.findAllByOrderBySortOrderAsc();
        Location fallback = all.stream()
                .filter(l -> !l.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No fallback location found"));

        var batches = batchRepository.findByLocationId(id);
        for (var batch : batches) {
            batch.setLocation(fallback);
            batchRepository.save(batch);
        }

        locationRepository.deleteById(id);
    }

    @Transactional
    public void reorderLocations(List<String> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            String id = orderedIds.get(i);
            int finalI = i + 1;
            locationRepository.findById(id).ifPresent(loc -> {
                loc.setSortOrder(finalI);
                locationRepository.save(loc);
            });
        }
    }
}
