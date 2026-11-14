package com.cleanlearning.domain.offering;

import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingStatus;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.BusinessRuleException;
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
 * A consultation offering as the admin manages it (UC-16): its fields, its status, its packages and
 * its eligible experts. The description may be empty. The packages come fewest sessions first, then
 * by id, which is the order the shop shows them in; they may share a session count. The eligible
 * experts are a set of user ids and may be empty; each is an expert when the admin chooses them.
 * The image is optional; it changes apart from the form's save (UC-16 step 3).
 *
 * <p>The status follows UC-16's lifecycle: a draft is published, a published offering is
 * unpublished to a draft again or archived, and only a draft is deleted. An archived offering is
 * final and read-only. A change the status doesn't allow throws a {@link BusinessRuleException}
 * with {@link #WRONG_OFFERING_STATUS}. An offering with purchases is neither unpublished nor
 * deleted ({@link #OFFERING_HAS_PURCHASES}, D-07); the status is checked first.
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

  /**
   * Publishing an offering without a package or without an eligible expert; the message names
   * what's missing.
   */
  public static final String OFFERING_INCOMPLETE = "OFFERING_INCOMPLETE";

  /** A change the offering's status doesn't allow, usually from a stale page. */
  public static final String WRONG_OFFERING_STATUS = "WRONG_OFFERING_STATUS";

  /** Unpublishing or deleting an offering that a customer has bought a package of (D-07). */
  public static final String OFFERING_HAS_PURCHASES = "OFFERING_HAS_PURCHASES";

  // The ids compare as text, which sorts a uuid the way the database does; UUID.compareTo compares
  // signed numbers.
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

  /**
   * A new offering starts as a draft (UC-16 step 4), with a new id and a new id for each package,
   * and without an image: it gets one after its first save.
   *
   * @param experts the users who are experts: every eligible expert id must be one of them
   * @throws IllegalArgumentException if a package names an id (a new offering has no packages to
   *     keep), or an eligible expert id isn't an expert's
   */
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
   * The offering with new values; id and status stay. The packages are matched by id: an intended
   * package with an id keeps that package with its new values, one without an id is a new package
   * with a new id, and a package that none names is dropped. The eligible experts are replaced. The
   * image stays.
   *
   * @param experts the users who are experts: every eligible expert id must be one of them
   * @throws IllegalArgumentException if an intended package names an id that isn't one of this
   *     offering's packages, or names a package twice, or an eligible expert id isn't an expert's
   * @throws BusinessRuleException if the offering is archived
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

  /**
   * The offering with this image instead of the one it had, if any. The caller deletes the old
   * image (D-21: a replaced image gets a new id).
   *
   * @throws BusinessRuleException if the offering is archived
   */
  public ConsultationOffering changeImage(ImageId newImageId) {
    Objects.requireNonNull(newImageId, "newImageId");
    requireNotArchived();
    return withImage(Optional.of(newImageId));
  }

  /**
   * The offering without an image. The caller deletes the image it had, if any.
   *
   * @throws BusinessRuleException if the offering is archived
   */
  public ConsultationOffering removeImage() {
    requireNotArchived();
    return withImage(Optional.empty());
  }

  /**
   * The draft, published: it appears in the shop (a published offering without packages doesn't,
   * UC-02).
   *
   * @throws BusinessRuleException {@link #WRONG_OFFERING_STATUS} if the offering isn't a draft, or
   *     {@link #OFFERING_INCOMPLETE} if it has no package or no eligible expert
   */
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

  /**
   * The published offering, a draft again. Without purchases only (D-07): an offering with
   * purchases is archived instead. The status is checked first.
   *
   * @param hasPurchases whether a customer has bought a package of the offering
   * @throws BusinessRuleException {@link #WRONG_OFFERING_STATUS} if the offering isn't published,
   *     or {@link #OFFERING_HAS_PURCHASES} if it has purchases
   */
  public ConsultationOffering unpublish(boolean hasPurchases) {
    requireStatus(OfferingStatus.PUBLISHED, "unpublished");
    if (hasPurchases) {
      throw new BusinessRuleException(
          OFFERING_HAS_PURCHASES, "The offering has purchases. Archive it instead.");
    }
    return withStatus(OfferingStatus.DRAFT);
  }

  /**
   * The published offering, archived for good: out of the shop and read-only.
   *
   * @throws BusinessRuleException {@link #WRONG_OFFERING_STATUS} if the offering isn't published
   */
  public ConsultationOffering archive() {
    requireStatus(OfferingStatus.PUBLISHED, "archived");
    return withStatus(OfferingStatus.ARCHIVED);
  }

  /**
   * Only a draft may be deleted (UC-16), and only without purchases (D-07). The status is checked
   * first. A draft can't get purchases while unpublishing needs none, so the purchase check is a
   * safety net.
   *
   * @param hasPurchases whether a customer has bought a package of the offering
   * @throws BusinessRuleException {@link #WRONG_OFFERING_STATUS} if the offering isn't a draft, or
   *     {@link #OFFERING_HAS_PURCHASES} if it has purchases
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
        throw new IllegalArgumentException(
            "User " + eligibleExpertId.value() + " is not an expert.");
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
        throw new IllegalArgumentException(
            "Package " + packageId.value() + " is not a package of this offering.");
      }
      if (!named.add(packageId)) {
        throw new IllegalArgumentException("Package " + packageId.value() + " is named twice.");
      }
      matched.add(new ConsultationPackage(packageId, intended.sessionCount(), intended.price()));
    }
    return matched;
  }
}
