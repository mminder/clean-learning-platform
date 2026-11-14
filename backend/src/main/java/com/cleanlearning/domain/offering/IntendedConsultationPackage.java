package com.cleanlearning.domain.offering;

import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.shared.value.Money;
import java.util.Objects;
import java.util.Optional;

/**
 * A package as the admin entered it in the offering's form (UC-16 step 3): with the id of the
 * package it keeps, or without an id for a new package. {@link ConsultationOffering} matches the
 * intended packages to its packages by id.
 */
public record IntendedConsultationPackage(
    Optional<ConsultationPackageId> id, SessionCount sessionCount, Money price) {

  public IntendedConsultationPackage {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(sessionCount, "sessionCount");
    Objects.requireNonNull(price, "price");
  }

  /** The package with this id, with its new values. */
  public static IntendedConsultationPackage kept(
      ConsultationPackageId id, SessionCount sessionCount, Money price) {
    return new IntendedConsultationPackage(Optional.of(id), sessionCount, price);
  }

  /** A new package. */
  public static IntendedConsultationPackage added(SessionCount sessionCount, Money price) {
    return new IntendedConsultationPackage(Optional.empty(), sessionCount, price);
  }
}
