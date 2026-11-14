package com.cleanlearning.adapters.database.consultation;

import com.cleanlearning.adapters.database.consultation.entity.AppointmentEntity;
import com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseRow;
import com.cleanlearning.adapters.database.consultation.row.CustomerConsultationCaseSummaryRow;
import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasePort;
import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasesPort;
import com.cleanlearning.domain.consultation.CaseInList;
import com.cleanlearning.domain.consultation.value.CancellationOutcome;
import com.cleanlearning.domain.consultation.value.CaseStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.consultation.view.CustomerConsultationCase;
import com.cleanlearning.domain.consultation.view.CustomerConsultationCaseSummary;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;
import com.cleanlearning.domain.user.ExpertProfile;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class CustomerConsultationCaseAdapter
    implements LoadCustomerConsultationCasePort, LoadCustomerConsultationCasesPort {

  private final ConsultationCaseRepository repository;

  CustomerConsultationCaseAdapter(ConsultationCaseRepository repository) {
    this.repository = repository;
  }

  /**
   * The query flushes a case created in the same transaction first, so the purchase answers its new
   * case, and so does a cancellation's change of an appointment.
   */
  @Override
  public Optional<CustomerConsultationCase> load(
      ConsultationCaseId id, UserId customerId, Instant asOf) {
    return repository
        .findCustomerCase(id.value(), customerId.value())
        .map(row -> toCustomerConsultationCase(row, repository.findAppointments(row.id()), asOf));
  }

  @Override
  public List<CustomerConsultationCaseSummary> loadAll(UserId customerId, Instant asOf) {
    Map<UUID, List<AppointmentEntity>> appointmentsByCase =
        repository.findAppointmentsOfCustomer(customerId.value()).stream()
            .collect(Collectors.groupingBy(AppointmentEntity::getConsultationCaseId));
    return repository.findCustomerCases(customerId.value()).stream()
        .map(row ->
            toCustomerConsultationCaseSummary(
                row, appointmentsByCase.getOrDefault(row.id(), List.of()), asOf))
        .toList();
  }

  private static CustomerConsultationCase toCustomerConsultationCase(
      CustomerConsultationCaseRow row, List<AppointmentEntity> appointments, Instant asOf) {
    return new CustomerConsultationCase(
        new ConsultationCaseId(row.id()),
        new OfferingName(row.offeringName()),
        Optional.ofNullable(row.offeringImageId()).map(ImageId::new),
        new PurchaseTerms(
            new Money(row.pricePaid()),
            new SessionCount(row.sessionCount()),
            new SessionDuration(row.sessionDurationMinutes()),
            new CancellationDeadline(row.cancellationDeadlineHours())),
        row.purchasedAt(),
        assignedExpert(row),
        ConsultationCaseMapper.toCaseCredits(row.sessionCount(), appointments),
        cancellation(row),
        asOf);
  }

  /**
   * V10's checks say a cancelled case has both {@code cancelled_at} and {@code refund_eligible}.
   */
  private static Optional<CancellationOutcome> cancellation(CustomerConsultationCaseRow row) {
    if (row.status() == CaseStatus.ACTIVE) {
      return Optional.empty();
    }
    return Optional.of(
        new CancellationOutcome(
            Objects.requireNonNull(row.cancelledAt(), "cancelledAt"),
            Objects.requireNonNull(row.refundEligible(), "refundEligible")));
  }

  /** The left join's expert columns are all null before the first booking. */
  private static Optional<ExpertProfile> assignedExpert(CustomerConsultationCaseRow row) {
    UUID expertId = row.expertId();
    if (expertId == null) {
      return Optional.empty();
    }
    return Optional.of(
        new ExpertProfile(
            new UserId(expertId),
            new DisplayName(Objects.requireNonNull(row.expertDisplayName(), "expertDisplayName")),
            Objects.requireNonNull(row.expertBio(), "expertBio")));
  }

  private static CustomerConsultationCaseSummary toCustomerConsultationCaseSummary(
      CustomerConsultationCaseSummaryRow row, List<AppointmentEntity> appointments, Instant asOf) {
    return new CustomerConsultationCaseSummary(
        new CaseInList(
            new ConsultationCaseId(row.id()),
            row.purchasedAt(),
            Optional.ofNullable(row.cancelledAt()),
            ConsultationCaseMapper.toCaseCredits(row.sessionCount(), appointments)),
        new OfferingName(row.offeringName()),
        Optional.ofNullable(row.offeringImageId()).map(ImageId::new),
        Optional.ofNullable(row.expertDisplayName()).map(DisplayName::new),
        asOf);
  }
}
