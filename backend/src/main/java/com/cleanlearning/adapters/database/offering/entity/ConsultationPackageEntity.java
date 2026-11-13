package com.cleanlearning.adapters.database.offering.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "consultation_package")
public class ConsultationPackageEntity {

    @Id
    private UUID id;

    /** Foreign key to {@link ConsultationOfferingEntity}, whose id is also the {@link OfferingEntity}'s id. */
    private UUID consultationOfferingId;

    private int sessionCount;

    private BigDecimal price;

    protected ConsultationPackageEntity() {
        // for JPA
    }

    public ConsultationPackageEntity(UUID id, UUID consultationOfferingId, int sessionCount, BigDecimal price) {
        this.id = id;
        this.consultationOfferingId = consultationOfferingId;
        this.sessionCount = sessionCount;
        this.price = price;
    }

    public UUID getId() {
        return id;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
