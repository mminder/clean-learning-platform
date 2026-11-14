package com.cleanlearning.domain.offering.view;

import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.ConsultationPackage;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingType;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.user.ExpertProfile;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ShopConsultationOffering(
    OfferingId id,
    OfferingName name,
    String description,
    Optional<ImageId> imageId,
    SessionDuration sessionDuration,
    CancellationDeadline cancellationDeadline,
    List<ConsultationPackage> packages,
    List<ExpertProfile> eligibleExperts) {

  public ShopConsultationOffering {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(imageId, "imageId");
    Objects.requireNonNull(sessionDuration, "sessionDuration");
    Objects.requireNonNull(cancellationDeadline, "cancellationDeadline");
    packages = List.copyOf(packages);
    if (packages.isEmpty()) {
      throw new IllegalArgumentException(
          "A consultation offering needs at least one package: " + id);
    }
    eligibleExperts = List.copyOf(eligibleExperts);
  }

  public OfferingType type() {
    return OfferingType.CONSULTATION;
  }
}
