package com.skafferi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "locations")
public class Location {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String icon = "Archive"; // Refrigerator, Snowflake, Flame, Archive, LayoutGrid, Box, Home, Warehouse

    private String type = "PANTRY"; // FRIDGE, FREEZER, PANTRY, SPICE, OTHER

    private int sortOrder = 0;

    public Location() {
    }

    public Location(String id, String name, String icon, int sortOrder) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.icon = icon;
        this.sortOrder = sortOrder;
    }

    public Location(String id, String name, String description, String icon, String type, int sortOrder) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.type = type;
        this.sortOrder = sortOrder;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
