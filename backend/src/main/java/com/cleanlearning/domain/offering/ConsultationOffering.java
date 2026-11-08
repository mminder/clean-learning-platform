package com.cleanlearning.domain.offering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingStatus;
import com.cleanlearning.domain.offering.value.SessionDuration;

/**
 * A consultation offering as the admin manages it (UC-16): its fields, its status and its packages. The description
 * may be empty. The packages come fewest sessions first, then by id, which is the order the shop shows them in; they
 * may share a session count. Eligible experts and the image come with tasks 05 and 07 of increment 02.
 */
public record ConsultationOffering(OfferingId id, OfferingName name, String description,
        SessionDuration sessionDuration, CancellationDeadline cancellationDeadline, OfferingStatus status,
        List<ConsultationPackage> packages) {

    // The ids compare as text, which sorts a uuid the way the database does; UUID.compareTo compares signed numbers.
    private static final Comparator<ConsultationPackage> PACKAGE_ORDER = Comparator
            .comparing((ConsultationPackage consultationPackage) -> consultationPackage.sessionCount().value())
            .thenComparing(consultationPackage -> consultationPackage.id().value().toString());

    public ConsultationOffering {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(sessionDuration, "sessionDuration");
        Objects.requireNonNull(cancellationDeadline, "cancellationDeadline");
        Objects.requireNonNull(status, "status");
        packages = packages.stream().sorted(PACKAGE_ORDER).toList();
    }

    /**
     * A new offering starts as a draft (UC-16 step 4), with a new id and a new id for each package.
     *
     * @throws IllegalArgumentException if a package names an id: a new offering has no packages to keep
     */
    public static ConsultationOffering create(OfferingName name, String description, SessionDuration sessionDuration,
            CancellationDeadline cancellationDeadline, List<ConsultationPackageEntry> packages) {
        return new ConsultationOffering(new OfferingId(UUID.randomUUID()), name, description, sessionDuration,
                cancellationDeadline, OfferingStatus.DRAFT, matchPackages(List.of(), packages));
    }

    /**
     * The offering with new values; id and status stay. The packages are matched by id: an entry with an id keeps that
     * package with its new values, an entry without one is a new package with a new id, and a package that no entry
     * names is dropped.
     *
     * @throws IllegalArgumentException if an entry names an id that isn't one of this offering's packages, or names a
     *     package twice
     */
    public ConsultationOffering edit(OfferingName name, String description, SessionDuration sessionDuration,
            CancellationDeadline cancellationDeadline, List<ConsultationPackageEntry> packages) {
        return new ConsultationOffering(id, name, description, sessionDuration, cancellationDeadline, status,
                matchPackages(this.packages, packages));
    }

    private static List<ConsultationPackage> matchPackages(List<ConsultationPackage> current,
            List<ConsultationPackageEntry> entries) {
        Set<ConsultationPackageId> currentIds = current.stream().map(ConsultationPackage::id)
                .collect(Collectors.toSet());
        Set<ConsultationPackageId> named = new HashSet<>();
        List<ConsultationPackage> matched = new ArrayList<>();
        for (ConsultationPackageEntry entry : entries) {
            if (entry.id().isEmpty()) {
                matched.add(new ConsultationPackage(new ConsultationPackageId(UUID.randomUUID()),
                        entry.sessionCount(), entry.price()));
                continue;
            }
            ConsultationPackageId packageId = entry.id().get();
            if (!currentIds.contains(packageId)) {
                throw new IllegalArgumentException(
                        "Package " + packageId.value() + " is not a package of this offering.");
            }
            if (!named.add(packageId)) {
                throw new IllegalArgumentException("Package " + packageId.value() + " is named twice.");
            }
            matched.add(new ConsultationPackage(packageId, entry.sessionCount(), entry.price()));
        }
        return matched;
    }
}
