package com.cleanlearning.domain.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.shared.value.Money;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultationPackageTest {

  private static final ConsultationPackageId ID =
      new ConsultationPackageId(UUID.fromString("00000000-0000-0000-0000-000000001011"));
  private static final SessionCount THREE = new SessionCount(3);

  @Test
  void isASessionCountWithAPrice() {
    ConsultationPackage consultationPackage =
        new ConsultationPackage(ID, THREE, Money.chf("450.00"));

    assertThat(consultationPackage.id()).isEqualTo(ID);
    assertThat(consultationPackage.sessionCount()).isEqualTo(THREE);
    assertThat(consultationPackage.price()).isEqualTo(Money.chf("450.00"));
  }

  @Test
  void rejectsMissingValues() {
    Money price = Money.chf("450.00");
    assertThatNullPointerException().isThrownBy(() -> new ConsultationPackage(null, THREE, price));
    assertThatNullPointerException().isThrownBy(() -> new ConsultationPackage(ID, null, price));
    assertThatNullPointerException().isThrownBy(() -> new ConsultationPackage(ID, THREE, null));
    assertThatNullPointerException().isThrownBy(() -> new ConsultationPackageId(null));
  }
}
