package com.cleanlearning.adapters.database.image.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Written and read by {@code ImageAdapter}. An image is never changed, only inserted and deleted. The database sets
 * {@code created_at} and {@code updated_at}.
 */
@Entity
@Table(name = "image")
public class ImageEntity {

    @Id
    private UUID id;

    private String contentType;

    private byte[] data;

    protected ImageEntity() {
        // for JPA
    }

    public ImageEntity(UUID id, String contentType, byte[] data) {
        this.id = id;
        this.contentType = contentType;
        this.data = data;
    }

    public UUID getId() {
        return id;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }
}
