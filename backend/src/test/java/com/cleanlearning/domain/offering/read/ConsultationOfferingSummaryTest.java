package com.cleanlearning.domain.offering.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultationOfferingSummaryTest {

  private static final OfferingId ID =
      new OfferingId(UUID.fromString("00000000-0000-0000-0000-000000001001"));

  private static final OfferingName NAME = new OfferingName("Career Coaching");

  @Test
  void holdsItsFields() {
    var summary = new ConsultationOfferingSummary(ID, NAME, OfferingStatus.DRAFT, 0, 0);

    assertThat(summary.id()).isEqualTo(ID);
    assertThat(summary.name()).isEqualTo(NAME);
    assertThat(summary.status()).isEqualTo(OfferingStatus.DRAFT);
    assertThat(summary.packageCount()).isZero();
    assertThat(summary.eligibleExpertCount()).isZero();
  }

  @Test
  void rejectsNegativeCounts() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ConsultationOfferingSummary(ID, NAME, OfferingStatus.DRAFT, -1, 0));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ConsultationOfferingSummary(ID, NAME, OfferingStatus.DRAFT, 0, -1));
  }

  @Test
  void rejectsMissingParts() {
    assertThatNullPointerException()
        .isThrownBy(() -> new ConsultationOfferingSummary(ID, NAME, null, 0, 0));
  }
}
