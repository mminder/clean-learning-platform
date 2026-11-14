package com.cleanlearning.domain.offering;

import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingStatus;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.InvalidValueException;
import com.cleanlearning.domain.user.Expert;
import com.cleanlearning.domain.user.value.UserId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Packages come fewest sessions first, then by id, as the shop shows them. Lifecycle: a draft is
 * published; a published offering is unpublished to a draft or archived (final, read-only); only a
 * draft is deleted; an offering with purchases is neither unpublished nor deleted (D-07). The
 * status is checked first.
 */
public record ConsultationOffering(
    OfferingId id,
    OfferingName name,
    String description,
    SessionDuration sessionDuration,
    CancellationDeadline cancellationDeadline,
    OfferingStatus status,
    List<ConsultationPackage> packages,
    Set<UserId> eligibleExpertIds,
    Optional<ImageId> imageId) {

  public static final String OFFERING_INCOMPLETE = "OFFERING_INCOMPLETE";

  public static final String WRONG_OFFERING_STATUS = "WRONG_OFFERING_STATUS";

  public static final String OFFERING_HAS_PURCHASES = "OFFERING_HAS_PURCHASES";

  // The ids compare as text, which sorts a uuid the way the database does; {@code UUID.compareTo}
  // compares signed numbers.
  private static final Comparator<ConsultationPackage> PACKAGE_ORDER =
      Comparator.comparing((ConsultationPackage consultationPackage) ->
              consultationPackage.sessionCount().value())
          .thenComparing(consultationPackage -> consultationPackage.id().value().toString());

  public ConsultationOffering {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(sessionDuration, "sessionDuration");
    Objects.requireNonNull(cancellationDeadline, "cancellationDeadline");
    Objects.requireNonNull(status, "status");
    packages = Objects.requireNonNull(packages, "packages").stream().sorted(PACKAGE_ORDER).toList();
    eligibleExpertIds = Set.copyOf(Objects.requireNonNull(eligibleExpertIds, "eligibleExpertIds"));
    Objects.requireNonNull(imageId, "imageId");
  }

  public static ConsultationOffering create(
      OfferingName name,
      String description,
      SessionDuration sessionDuration,
      CancellationDeadline cancellationDeadline,
      List<IntendedConsultationPackage> packages,
      Set<UserId> eligibleExpertIds,
      List<Expert> experts) {
    return new ConsultationOffering(
        new OfferingId(UUID.randomUUID()),
        name,
        description,
        sessionDuration,
        cancellationDeadline,
        OfferingStatus.DRAFT,
        matchPackages(List.of(), packages),
        requireExperts(eligibleExpertIds, experts),
        Optional.empty());
  }

  /**
   * The packages are matched by id: one with an id keeps that package, one without is new, and a
   * package that none names is dropped.
   */
  public ConsultationOffering edit(
      OfferingName name,
      String description,
      SessionDuration sessionDuration,
      CancellationDeadline cancellationDeadline,
      List<IntendedConsultationPackage> packages,
      Set<UserId> eligibleExpertIds,
      List<Expert> experts) {
    requireNotArchived();
    return new ConsultationOffering(
        id,
        name,
        description,
        sessionDuration,
        cancellationDeadline,
        status,
        matchPackages(this.packages, packages),
        requireExperts(eligibleExpertIds, experts),
        imageId);
  }

  /** The caller deletes the old image (D-21). */
  public ConsultationOffering changeImage(ImageId newImageId) {
    Objects.requireNonNull(newImageId, "newImageId");
    requireNotArchived();
    return withImage(Optional.of(newImageId));
  }

  /** The caller deletes the image it had. */
  public ConsultationOffering removeImage() {
    requireNotArchived();
    return withImage(Optional.empty());
  }

  public ConsultationOffering publish() {
    requireStatus(OfferingStatus.DRAFT, "published");
    List<String> missing = new ArrayList<>();
    if (packages.isEmpty()) {
      missing.add("at least one package");
    }
    if (eligibleExpertIds.isEmpty()) {
      missing.add("at least one eligible expert");
    }
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          OFFERING_INCOMPLETE,
          "The offering needs " + String.join(" and ", missing) + " before it can be published.");
    }
    return withStatus(OfferingStatus.PUBLISHED);
  }

  public ConsultationOffering unpublish(boolean hasPurchases) {
    requireStatus(OfferingStatus.PUBLISHED, "unpublished");
    if (hasPurchases) {
      throw new BusinessRuleException(
          OFFERING_HAS_PURCHASES, "The offering has purchases. Archive it instead.");
    }
    return withStatus(OfferingStatus.DRAFT);
  }

  public ConsultationOffering archive() {
    requireStatus(OfferingStatus.PUBLISHED, "archived");
    return withStatus(OfferingStatus.ARCHIVED);
  }

  /**
   * A draft can't get purchases while unpublishing needs none, so the purchase check is a safety
   * net.
   */
  public void requireDeletable(boolean hasPurchases) {
    requireStatus(OfferingStatus.DRAFT, "deleted");
    if (hasPurchases) {
      throw new BusinessRuleException(
          OFFERING_HAS_PURCHASES, "The offering has purchases and can't be deleted.");
    }
  }

  private void requireStatus(OfferingStatus required, String changed) {
    requireNotArchived();
    if (status != required) {
      throw new BusinessRuleException(
          WRONG_OFFERING_STATUS,
          "The offering is "
              + describe(status)
              + ". Only "
              + describeOne(required)
              + " can be "
              + changed
              + ".");
    }
  }

  private void requireNotArchived() {
    if (status == OfferingStatus.ARCHIVED) {
      throw new BusinessRuleException(
          WRONG_OFFERING_STATUS, "The offering is archived and can't be changed any more.");
    }
  }

  private static String describe(OfferingStatus status) {
    return switch (status) {
      case DRAFT -> "a draft";
      case PUBLISHED -> "published";
      case ARCHIVED -> "archived";
    };
  }

  private static String describeOne(OfferingStatus status) {
    return switch (status) {
      case DRAFT -> "a draft";
      case PUBLISHED -> "a published offering";
      case ARCHIVED -> "an archived offering";
    };
  }

  private ConsultationOffering withStatus(OfferingStatus newStatus) {
    return new ConsultationOffering(
        id,
        name,
        description,
        sessionDuration,
        cancellationDeadline,
        newStatus,
        packages,
        eligibleExpertIds,
        imageId);
  }

  private ConsultationOffering withImage(Optional<ImageId> newImageId) {
    return new ConsultationOffering(
        id,
        name,
        description,
        sessionDuration,
        cancellationDeadline,
        status,
        packages,
        eligibleExpertIds,
        newImageId);
  }

  private static Set<UserId> requireExperts(Set<UserId> eligibleExpertIds, List<Expert> experts) {
    Set<UserId> expertIds = experts.stream().map(Expert::id).collect(Collectors.toSet());
    for (UserId eligibleExpertId : eligibleExpertIds) {
      if (!expertIds.contains(eligibleExpertId)) {
        throw new InvalidValueException("User " + eligibleExpertId.value() + " is not an expert.");
      }
    }
    return eligibleExpertIds;
  }

  private static List<ConsultationPackage> matchPackages(
      List<ConsultationPackage> current, List<IntendedConsultationPackage> intendedPackages) {
    Set<ConsultationPackageId> currentIds =
        current.stream().map(ConsultationPackage::id).collect(Collectors.toSet());
    Set<ConsultationPackageId> named = new HashSet<>();
    List<ConsultationPackage> matched = new ArrayList<>();
    for (IntendedConsultationPackage intended : intendedPackages) {
      if (intended.id().isEmpty()) {
        matched.add(
            new ConsultationPackage(
                new ConsultationPackageId(UUID.randomUUID()),
                intended.sessionCount(),
                intended.price()));
        continue;
      }
      ConsultationPackageId packageId = intended.id().get();
      if (!currentIds.contains(packageId)) {
        throw new InvalidValueException(
            "Package " + packageId.value() + " is not a package of this offering.");
      }
      if (!named.add(packageId)) {
        throw new InvalidValueException("Package " + packageId.value() + " is named twice.");
      }
      matched.add(new ConsultationPackage(packageId, intended.sessionCount(), intended.price()));
    }
    return matched;
  }
}
