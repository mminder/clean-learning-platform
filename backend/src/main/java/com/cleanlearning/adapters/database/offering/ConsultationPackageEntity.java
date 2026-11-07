package com.cleanlearning.adapters.database.offering;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read through the projection in {@link OfferingRepository}.
 */
@Entity
@Table(name = "consultation_package")
class ConsultationPackageEntity {

    @Id
    private UUID id;

    // consultation_offering shares its id with offering, so this is also the offering's id.
    private UUID consultationOfferingId;

    private BigDecimal price;

    protected ConsultationPackageEntity() {
        // for JPA
    }
}
