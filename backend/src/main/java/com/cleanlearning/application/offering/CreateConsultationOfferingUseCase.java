package com.cleanlearning.application.offering;

import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.application.user.port.LoadExpertsPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.IntendedConsultationPackage;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.user.value.UserId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateConsultationOfferingUseCase {

  private final SaveConsultationOfferingPort saveOffering;

  private final LoadExpertsPort loadExperts;

  CreateConsultationOfferingUseCase(
      SaveConsultationOfferingPort saveOffering, LoadExpertsPort loadExperts) {
    this.saveOffering = saveOffering;
    this.loadExperts = loadExperts;
  }

  @Transactional
  public ConsultationOffering invoke(Command command) {
    ConsultationOffering offering =
        ConsultationOffering.create(
            command.name(),
            command.description(),
            command.sessionDuration(),
            command.cancellationDeadline(),
            command.packages(),
            command.eligibleExpertIds(),
            loadExperts.loadExperts());
    saveOffering.create(offering);
    return offering;
  }

  public record Command(
      OfferingName name,
      String description,
      SessionDuration sessionDuration,
      CancellationDeadline cancellationDeadline,
      List<IntendedConsultationPackage> packages,
      Set<UserId> eligibleExpertIds) {

    public Command {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(description, "description");
      Objects.requireNonNull(sessionDuration, "sessionDuration");
      Objects.requireNonNull(cancellationDeadline, "cancellationDeadline");
      packages = List.copyOf(packages);
      eligibleExpertIds = Set.copyOf(eligibleExpertIds);
    }
  }
}
