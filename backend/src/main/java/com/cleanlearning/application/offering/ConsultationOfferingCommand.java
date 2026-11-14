package com.cleanlearning.application.offering;

import com.cleanlearning.domain.offering.IntendedConsultationPackage;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.user.value.UserId;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ConsultationOfferingCommand(
    OfferingName name,
    String description,
    SessionDuration sessionDuration,
    CancellationDeadline cancellationDeadline,
    List<IntendedConsultationPackage> packages,
    Set<UserId> eligibleExpertIds) {

  public ConsultationOfferingCommand {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(sessionDuration, "sessionDuration");
    Objects.requireNonNull(cancellationDeadline, "cancellationDeadline");
    packages = List.copyOf(packages);
    eligibleExpertIds = Set.copyOf(eligibleExpertIds);
  }
}
