package com.cleanlearning.domain.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingType;
import com.cleanlearning.domain.shared.value.Money;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShopOfferingTest {

  private static final OfferingId ID =
      new OfferingId(UUID.fromString("00000000-0000-0000-0000-000000001001"));
  private static final OfferingName NAME = new OfferingName("Career Coaching");

  @Test
  void consultationOfferingIsOfTypeConsultation() {
    ShopOffering offering =
        ShopOffering.consultation(
            ID, NAME, "Coaching for your career.", Optional.empty(), List.of(Money.chf("450.00")));

    assertThat(offering.id()).isEqualTo(ID);
    assertThat(offering.type()).isEqualTo(OfferingType.CONSULTATION);
    assertThat(offering.name()).isEqualTo(NAME);
    assertThat(offering.description()).isEqualTo("Coaching for your career.");
  }

  @Test
  void consultationOfferingMayHaveAnImage() {
    ImageId image = new ImageId(UUID.randomUUID());

    assertThat(
            ShopOffering.consultation(
                    ID,
                    NAME,
                    "Coaching for your career.",
                    Optional.of(image),
                    List.of(Money.chf("450.00")))
                .imageId())
        .contains(image);
  }

  @Test
  void consultationPriceIsTheCheapestPackagePrice() {
    ShopOffering offering =
        ShopOffering.consultation(
            ID,
            NAME,
            "Coaching for your career.",
            Optional.empty(),
            List.of(Money.chf("700.00"), Money.chf("450.00"), Money.chf("1800.00")));

    assertThat(offering.price()).isEqualTo(Money.chf("450.00"));
  }

  @Test
  void consultationOfferingWithOnePackageCostsItsPrice() {
    ShopOffering offering =
        ShopOffering.consultation(
            ID, NAME, "Coaching for your career.", Optional.empty(), List.of(Money.chf("700.00")));

    assertThat(offering.price()).isEqualTo(Money.chf("700.00"));
  }

  @Test
  void consultationOfferingNeedsAPackage() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            ShopOffering.consultation(
                ID, NAME, "Coaching for your career.", Optional.empty(), List.of()))
        .withMessageContaining("package");
  }

  @Test
  void rejectsMissingValues() {
    Money price = Money.chf("450.00");
    Optional<ImageId> noImage = Optional.empty();
    OfferingType consultation = OfferingType.CONSULTATION;
    assertThatNullPointerException()
        .isThrownBy(() -> new ShopOffering(null, consultation, NAME, "Text", noImage, price));
    assertThatNullPointerException()
        .isThrownBy(() -> new ShopOffering(ID, null, NAME, "Text", noImage, price));
    assertThatNullPointerException()
        .isThrownBy(() -> new ShopOffering(ID, consultation, null, "Text", noImage, price));
    assertThatNullPointerException()
        .isThrownBy(() -> new ShopOffering(ID, consultation, NAME, null, noImage, price));
    assertThatNullPointerException()
        .isThrownBy(() -> new ShopOffering(ID, consultation, NAME, "Text", noImage, null));
    assertThatNullPointerException()
        .isThrownBy(() -> new ShopOffering(ID, consultation, NAME, "Text", null, price));
  }

  @Test
  void offeringIdIsRequired() {
    assertThatNullPointerException().isThrownBy(() -> new OfferingId(null));
  }
}
