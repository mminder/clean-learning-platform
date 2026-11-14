package com.cleanlearning.adapters.database.image.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "image")
public class ImageEntity {

  @Id private UUID id;

  private String contentType;

  private byte[] data;

  protected ImageEntity() {}

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
