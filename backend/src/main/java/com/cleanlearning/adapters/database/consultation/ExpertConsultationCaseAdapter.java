package com.cleanlearning.adapters.database.consultation;

import com.cleanlearning.adapters.database.consultation.entity.AppointmentEntity;
import com.cleanlearning.adapters.database.consultation.row.ExpertConsultationCaseSummaryRow;
import com.cleanlearning.application.consultation.port.LoadExpertConsultationCasesPort;
import com.cleanlearning.domain.consultation.CaseInList;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.view.ExpertConsultationCaseSummary;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** The cases as their assigned expert sees them: every query is scoped to the expert in SQL. */
@Component
class ExpertConsultationCaseAdapter implements LoadExpertConsultationCasesPort {

  private final ConsultationCaseRepository repository;

  ExpertConsultationCaseAdapter(ConsultationCaseRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<ExpertConsultationCaseSummary> loadAll(UserId expertId, Instant asOf) {
    Map<UUID, List<AppointmentEntity>> appointmentsByCase =
        repository.findAppointmentsOfExpertCases(expertId.value()).stream()
            .collect(Collectors.groupingBy(AppointmentEntity::getConsultationCaseId));
    return repository.findExpertCases(expertId.value()).stream()
        .map(row ->
            toExpertConsultationCaseSummary(
                row, appointmentsByCase.getOrDefault(row.id(), List.of()), asOf))
        .toList();
  }

  private static ExpertConsultationCaseSummary toExpertConsultationCaseSummary(
      ExpertConsultationCaseSummaryRow row, List<AppointmentEntity> appointments, Instant asOf) {
    return new ExpertConsultationCaseSummary(
        new CaseInList(
            new ConsultationCaseId(row.id()),
            row.purchasedAt(),
            Optional.ofNullable(row.cancelledAt()),
            ConsultationCaseMapper.toCaseCredits(row.sessionCount(), appointments)),
        row.customerName(),
        new OfferingName(row.offeringName()),
        asOf);
  }
}
