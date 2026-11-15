package com.cleanlearning.adapters.database.consultation;

import com.cleanlearning.adapters.database.consultation.entity.AppointmentEntity;
import com.cleanlearning.adapters.database.consultation.row.ExpertConsultationCaseRow;
import com.cleanlearning.adapters.database.consultation.row.ExpertConsultationCaseSummaryRow;
import com.cleanlearning.application.consultation.port.LoadExpertConsultationCasePort;
import com.cleanlearning.application.consultation.port.LoadExpertConsultationCasesPort;
import com.cleanlearning.domain.consultation.CaseInList;
import com.cleanlearning.domain.consultation.Note;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.view.ExpertConsultationCase;
import com.cleanlearning.domain.consultation.view.ExpertConsultationCaseSummary;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.user.value.TeamsLink;
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
class ExpertConsultationCaseAdapter
    implements LoadExpertConsultationCasePort, LoadExpertConsultationCasesPort {

  private final ConsultationCaseRepository repository;

  private final NoteRepository noteRepository;

  ExpertConsultationCaseAdapter(
      ConsultationCaseRepository repository, NoteRepository noteRepository) {
    this.repository = repository;
    this.noteRepository = noteRepository;
  }

  @Override
  public Optional<ExpertConsultationCase> load(
      ConsultationCaseId id, UserId expertId, Instant asOf) {
    return repository
        .findExpertCase(id.value(), expertId.value())
        .map(row ->
            toExpertConsultationCase(
                row,
                repository.findAppointments(row.id()),
                noteRepository.findNotes(row.id()).stream()
                    .map(ConsultationCaseMapper::toNote)
                    .toList(),
                asOf));
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

  private static ExpertConsultationCase toExpertConsultationCase(
      ExpertConsultationCaseRow row,
      List<AppointmentEntity> appointments,
      List<Note> notes,
      Instant asOf) {
    return new ExpertConsultationCase(
        new ConsultationCaseId(row.id()),
        row.customerName(),
        row.customerEmail(),
        new OfferingName(row.offeringName()),
        new SessionDuration(row.sessionDurationMinutes()),
        new CancellationDeadline(row.cancellationDeadlineHours()),
        row.purchasedAt(),
        ConsultationCaseMapper.toCaseCredits(row.sessionCount(), appointments),
        Optional.ofNullable(row.cancelledAt()),
        notes,
        Optional.ofNullable(row.teamsLink()).map(TeamsLink::new),
        asOf);
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
