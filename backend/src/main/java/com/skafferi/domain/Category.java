package com.skafferi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String icon;

    private int sortOrder;

    private Integer defaultShelfLifeDays;

    public Category() {
    }

    public Category(String id, String name, String icon, int sortOrder) {
        this(id, name, icon, sortOrder, null);
    }

    public Category(String id, String name, String icon, int sortOrder, Integer defaultShelfLifeDays) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.defaultShelfLifeDays = defaultShelfLifeDays;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getDefaultShelfLifeDays() {
        return defaultShelfLifeDays;
    }

    public void setDefaultShelfLifeDays(Integer defaultShelfLifeDays) {
        this.defaultShelfLifeDays = defaultShelfLifeDays;
    }
}
