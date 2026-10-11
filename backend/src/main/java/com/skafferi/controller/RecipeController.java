package com.skafferi.controller;

import com.skafferi.service.RecipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/matches")
    public ResponseEntity<List<Map<String, Object>>> getMatchingRecipes() {
        return ResponseEntity.ok(recipeService.getMatchingRecipes());
    }

    @PostMapping("/ai-zero-waste")
    public ResponseEntity<?> generateZeroWaste(@RequestBody(required = false) Map<String, List<String>> request) {
        try {
            List<String> itemIds = request != null ? request.get("itemIds") : null;
            return ResponseEntity.ok(recipeService.generateZeroWasteMeal(itemIds));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/missing-to-shopping")
    public ResponseEntity<Map<String, Object>> addMissingToShopping(@RequestBody Map<String, List<String>> request) {
        List<String> items = request != null ? request.get("items") : List.of();
        return ResponseEntity.ok(recipeService.addMissingToShoppingList(items));
    }
}
