package com.skafferi.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "external_mappings")
public class ExternalMapping {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private String systemType; // "mealie", "bring", "kroger_upc", "kroger_term"

    @Column(nullable = false)
    private String externalKey;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public ExternalMapping() {
    }

    public ExternalMapping(Item item, String systemType, String externalKey) {
        this.item = item;
        this.systemType = systemType;
        this.externalKey = externalKey;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getSystemType() {
        return systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public String getExternalKey() {
        return externalKey;
    }

    public void setExternalKey(String externalKey) {
        this.externalKey = externalKey;
    }
}
