package com.skafferi.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pending_cooked_meals")
public class PendingCookedMeal {

    @Id
    private String id;

    @Column(nullable = false)
    private String recipeName;

    private String recipeId;

    private Double servings = 1.0;

    @Column(nullable = false)
    private String status = "PENDING"; // "PENDING", "COMMITTED", "DISMISSED"

    private LocalDateTime cookedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PendingMealItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (cookedAt == null) {
            cookedAt = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public PendingCookedMeal() {
    }

    public PendingCookedMeal(String recipeName, String recipeId, Double servings) {
        this.recipeName = recipeName;
        this.recipeId = recipeId;
        this.servings = servings != null && servings > 0 ? servings : 1.0;
        this.status = "PENDING";
        this.cookedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public Double getServings() {
        return servings;
    }

    public void setServings(Double servings) {
        this.servings = servings;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCookedAt() {
        return cookedAt;
    }

    public void setCookedAt(LocalDateTime cookedAt) {
        this.cookedAt = cookedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<PendingMealItem> getItems() {
        return items;
    }

    public void setItems(List<PendingMealItem> items) {
        this.items = items;
    }

    public void addItem(PendingMealItem item) {
        item.setMeal(this);
        this.items.add(item);
    }
}
