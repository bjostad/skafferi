package com.skafferi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "pending_meal_items")
public class PendingMealItem {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    @JsonIgnore
    private PendingCookedMeal meal;

    private String rawIngredientText;

    private String foodName;

    private Double originalQuantity;

    private String originalUnit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "matched_item_id")
    private Item matchedItem;

    private Double deductQuantity;

    private String deductUnit;

    private boolean selected = true;

    private String matchConfidence = "UNMATCHED"; // "EXACT", "SUBSTRING", "TOKEN", "FUZZY", "MANUAL", "UNMATCHED"

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public PendingMealItem() {
    }

    public PendingMealItem(PendingCookedMeal meal, String rawIngredientText, String foodName,
                           Double originalQuantity, String originalUnit, Item matchedItem,
                           Double deductQuantity, String deductUnit, boolean selected, String matchConfidence) {
        this.meal = meal;
        this.rawIngredientText = rawIngredientText;
        this.foodName = foodName;
        this.originalQuantity = originalQuantity;
        this.originalUnit = originalUnit;
        this.matchedItem = matchedItem;
        this.deductQuantity = deductQuantity;
        this.deductUnit = deductUnit;
        this.selected = selected;
        this.matchConfidence = matchConfidence;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PendingCookedMeal getMeal() {
        return meal;
    }

    public void setMeal(PendingCookedMeal meal) {
        this.meal = meal;
    }

    public String getRawIngredientText() {
        return rawIngredientText;
    }

    public void setRawIngredientText(String rawIngredientText) {
        this.rawIngredientText = rawIngredientText;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public Double getOriginalQuantity() {
        return originalQuantity;
    }

    public void setOriginalQuantity(Double originalQuantity) {
        this.originalQuantity = originalQuantity;
    }

    public String getOriginalUnit() {
        return originalUnit;
    }

    public void setOriginalUnit(String originalUnit) {
        this.originalUnit = originalUnit;
    }

    public Item getMatchedItem() {
        return matchedItem;
    }

    public void setMatchedItem(Item matchedItem) {
        this.matchedItem = matchedItem;
    }

    public Double getDeductQuantity() {
        return deductQuantity;
    }

    public void setDeductQuantity(Double deductQuantity) {
        this.deductQuantity = deductQuantity;
    }

    public String getDeductUnit() {
        return deductUnit;
    }

    public void setDeductUnit(String deductUnit) {
        this.deductUnit = deductUnit;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getMatchConfidence() {
        return matchConfidence;
    }

    public void setMatchConfidence(String matchConfidence) {
        this.matchConfidence = matchConfidence;
    }
}
