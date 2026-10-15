package com.skafferi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "cached_recipes")
public class CachedRecipe {

    @Id
    private String id;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(length = 4000)
    private String description;

    private String source;
    private String recipeUrl;
    private String prepTime;
    private String cookTime;
    private String dateUpdated;

    @Lob
    @Column(length = 65535)
    private String ingredientsJson;

    private long lastSyncedAt;

    public CachedRecipe() {
    }

    public CachedRecipe(String id, String slug, String name, String description,
                        String source, String recipeUrl, String prepTime, String cookTime,
                        String dateUpdated, String ingredientsJson, long lastSyncedAt) {
        this.id = id;
        this.slug = slug != null ? slug : id;
        this.name = name != null ? name : "Untitled Recipe";
        this.description = description;
        this.source = source != null ? source : "Mealie";
        this.recipeUrl = recipeUrl;
        this.prepTime = prepTime;
        this.cookTime = cookTime;
        this.dateUpdated = dateUpdated;
        this.ingredientsJson = ingredientsJson;
        this.lastSyncedAt = lastSyncedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRecipeUrl() {
        return recipeUrl;
    }

    public void setRecipeUrl(String recipeUrl) {
        this.recipeUrl = recipeUrl;
    }

    public String getPrepTime() {
        return prepTime;
    }

    public void setPrepTime(String prepTime) {
        this.prepTime = prepTime;
    }

    public String getCookTime() {
        return cookTime;
    }

    public void setCookTime(String cookTime) {
        this.cookTime = cookTime;
    }

    public String getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(String dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public String getIngredientsJson() {
        return ingredientsJson;
    }

    public void setIngredientsJson(String ingredientsJson) {
        this.ingredientsJson = ingredientsJson;
    }

    public long getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(long lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}
