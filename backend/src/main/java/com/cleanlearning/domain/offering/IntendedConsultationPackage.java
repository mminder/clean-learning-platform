package com.cleanlearning.domain.offering;

import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.shared.value.Money;
import java.util.Objects;
import java.util.Optional;

/** {@link ConsultationOffering} matches it to its packages by id. */
public record IntendedConsultationPackage(
    Optional<ConsultationPackageId> id, SessionCount sessionCount, Money price) {

  public IntendedConsultationPackage {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(sessionCount, "sessionCount");
    Objects.requireNonNull(price, "price");
  }

  public static IntendedConsultationPackage kept(
      ConsultationPackageId id, SessionCount sessionCount, Money price) {
    return new IntendedConsultationPackage(Optional.of(id), sessionCount, price);
  }

  public static IntendedConsultationPackage added(SessionCount sessionCount, Money price) {
    return new IntendedConsultationPackage(Optional.empty(), sessionCount, price);
  }
}
