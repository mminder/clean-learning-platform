package com.cleanlearning.application.offering;

import com.cleanlearning.application.offering.port.LockConsultationOfferingPort;
import com.cleanlearning.application.offering.port.SaveConsultationOfferingPort;
import com.cleanlearning.application.user.port.LoadExpertsPort;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.IntendedConsultationPackage;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.value.UserId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditConsultationOfferingUseCase {

  private final LockConsultationOfferingPort lockOffering;

  private final SaveConsultationOfferingPort saveOffering;

  private final LoadExpertsPort loadExperts;

  EditConsultationOfferingUseCase(
      LockConsultationOfferingPort lockOffering,
      SaveConsultationOfferingPort saveOffering,
      LoadExpertsPort loadExperts) {
    this.lockOffering = lockOffering;
    this.saveOffering = saveOffering;
    this.loadExperts = loadExperts;
  }

  @Transactional
  public ConsultationOffering invoke(OfferingId id, Command command) {
    ConsultationOffering offering =
        lockOffering
            .lock(id)
            .orElseThrow(() -> new NotFoundException("Offering " + id.value() + " not found."));
    ConsultationOffering edited =
        offering.edit(
            command.name(),
            command.description(),
            command.sessionDuration(),
            command.cancellationDeadline(),
            command.packages(),
            command.eligibleExpertIds(),
            loadExperts.loadExperts());
    saveOffering.update(edited);
    return edited;
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
