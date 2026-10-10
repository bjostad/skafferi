package com.skafferi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skafferi.domain.Item;
import com.skafferi.dto.BarcodeLookupResult;
import com.skafferi.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Service
public class BarcodeLookupService {

    private static final Logger log = LoggerFactory.getLogger(BarcodeLookupService.class);
    private final ItemRepository itemRepository;
    private final com.skafferi.repository.InventoryBatchRepository batchRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BarcodeLookupService(ItemRepository itemRepository, com.skafferi.repository.InventoryBatchRepository batchRepository) {
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", "Skafferi-PantryApp/1.0 (Self-Hosted)")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public BarcodeLookupResult lookup(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return new BarcodeLookupResult(false, barcode, null, null, null, null, "count", "NOT_FOUND", null, null, null);
        }

        String cleanedBarcode = barcode.trim();

        // 1. Check local database first
        Optional<Item> localItem = itemRepository.findByBarcode(cleanedBarcode);
        if (localItem.isPresent()) {
            Item item = localItem.get();
            Double currentQty = batchRepository.getTotalQuantityForItem(item.getId());
            return new BarcodeLookupResult(
                    true,
                    cleanedBarcode,
                    item.getName(),
                    item.getBrand(),
                    item.getImageUrl(),
                    item.getCategory() != null ? item.getCategory().getId() : null,
                    item.getDefaultUnit(),
                    "LOCAL",
                    item.getId(),
                    item.getPackageSize(),
                    currentQty != null ? currentQty : 0.0
            );
        }

        // 2. Query Open Food Facts with automatic padding normalization
        try {
            JsonNode product = fetchProductNode(cleanedBarcode);
            if (product == null && cleanedBarcode.startsWith("0")) {
                // Try standard 12-digit UPC or unpadded barcode
                String unpadded = cleanedBarcode.replaceFirst("^0+", "");
                if (unpadded.length() == 11) {
                    product = fetchProductNode("0" + unpadded);
                }
                if (product == null) {
                    product = fetchProductNode(unpadded);
                }
            }

            if (product != null) {
                String productName = product.hasNonNull("product_name") ? product.get("product_name").asText() : "";
                String brand = product.hasNonNull("brands") ? product.get("brands").asText() : "";
                String imageUrl = product.hasNonNull("image_front_url") ? product.get("image_front_url").asText() : null;
                String packageSize = product.hasNonNull("quantity") ? product.get("quantity").asText() : null;

                if (!productName.isBlank()) {
                    String category = deduceCategoryFromOff(product);
                    String existingId = null;
                    Double currentQty = null;
                    Optional<Item> existingByName = itemRepository.findByNameIgnoreCase(productName.trim());
                    if (existingByName.isPresent()) {
                        existingId = existingByName.get().getId();
                        currentQty = batchRepository.getTotalQuantityForItem(existingId);
                    }

                    return new BarcodeLookupResult(
                            true,
                            cleanedBarcode,
                            productName,
                            brand,
                            imageUrl,
                            category,
                            "count",
                            "OPEN_FOOD_FACTS",
                            existingId,
                            packageSize,
                            currentQty
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Open Food Facts lookup failed for barcode {}: {}", cleanedBarcode, e.getMessage());
        }

        return new BarcodeLookupResult(false, cleanedBarcode, null, null, null, null, "count", "NOT_FOUND", null, null, null);
    }

    private String deduceCategoryFromOff(JsonNode product) {
        if (product.has("categories_tags")) {
            String categories = product.get("categories_tags").toString().toLowerCase();
            if (categories.contains("dair") || categories.contains("milk") || categories.contains("cheese") || categories.contains("yogurt")) {
                return "cat-dairy";
            }
            if (categories.contains("fruit") || categories.contains("vegetable") || categories.contains("produce")) {
                return "cat-produce";
            }
            if (categories.contains("meat") || categories.contains("poultry") || categories.contains("fish") || categories.contains("seafood")) {
                return "cat-meat";
            }
            if (categories.contains("bread") || categories.contains("bakery")) {
                return "cat-bakery";
            }
            if (categories.contains("canned") || categories.contains("soup")) {
                return "cat-canned";
            }
            if (categories.contains("cereal") || categories.contains("grain") || categories.contains("pasta") || categories.contains("rice")) {
                return "cat-grains";
            }
            if (categories.contains("beverage") || categories.contains("drink") || categories.contains("juice") || categories.contains("soda")) {
                return "cat-beverages";
            }
            if (categories.contains("snack") || categories.contains("sweet") || categories.contains("biscuit") || categories.contains("cookie")) {
                return "cat-snacks";
            }
            if (categories.contains("frozen")) {
                return "cat-frozen";
            }
        }
        return "cat-canned";
    }

    private JsonNode fetchProductNode(String barcode) {
        if (barcode == null || barcode.isBlank()) return null;
        try {
            String offUrl = "https://world.openfoodfacts.org/api/v2/product/" + barcode + ".json";
            String response = restClient.get()
                    .uri(offUrl)
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                if (root.has("status") && root.get("status").asInt() == 1 && root.has("product")) {
                    return root.get("product");
                }
            }
        } catch (Exception e) {
            log.debug("Open Food Facts query failed for barcode {}: {}", barcode, e.getMessage());
        }
        return null;
    }
}
