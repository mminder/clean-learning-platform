package com.cleanlearning.api.offering.request;

import com.cleanlearning.application.offering.CreateConsultationOfferingUseCase;
import com.cleanlearning.application.offering.EditConsultationOfferingUseCase;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.user.value.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The annotations repeat UC-16's rules so a 400 names every invalid field; the value objects stay
 * the authority. The numbers are {@code Integer}, so a missing one is a 400, not a silent 0.
 */
public record ConsultationOfferingRequest(
    @NotBlank String name,
    @NotNull String description,
    @NotNull @Positive Integer sessionDurationMinutes,
    @NotNull @PositiveOrZero Integer cancellationDeadlineHours,
    @NotNull List<@NotNull @Valid ConsultationPackageRequest> packages,
    @NotNull List<@NotNull UUID> eligibleExpertIds) {

  public CreateConsultationOfferingUseCase.Command toCreateCommand() {
    return new CreateConsultationOfferingUseCase.Command(
        new OfferingName(name),
        description,
        new SessionDuration(sessionDurationMinutes),
        new CancellationDeadline(cancellationDeadlineHours),
        packages.stream().map(ConsultationPackageRequest::toIntendedPackage).toList(),
        eligibleExpertIds.stream().map(UserId::new).collect(Collectors.toSet()));
  }

  public EditConsultationOfferingUseCase.Command toEditCommand() {
    return new EditConsultationOfferingUseCase.Command(
        new OfferingName(name),
        description,
        new SessionDuration(sessionDurationMinutes),
        new CancellationDeadline(cancellationDeadlineHours),
        packages.stream().map(ConsultationPackageRequest::toIntendedPackage).toList(),
        eligibleExpertIds.stream().map(UserId::new).collect(Collectors.toSet()));
  }
}
