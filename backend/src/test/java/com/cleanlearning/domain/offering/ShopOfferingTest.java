package com.cleanlearning.domain.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.shared.Money;

class ShopOfferingTest {

    private static final OfferingId ID = new OfferingId(UUID.fromString("00000000-0000-0000-0000-000000001001"));

    @Test
    void consultationOfferingIsOfTypeConsultation() {
        ShopOffering offering = ShopOffering.consultation(ID, "Career Coaching", "Coaching for your career.",
                List.of(Money.chf("450.00")));

        assertThat(offering.id()).isEqualTo(ID);
        assertThat(offering.type()).isEqualTo(OfferingType.CONSULTATION);
        assertThat(offering.name()).isEqualTo("Career Coaching");
        assertThat(offering.description()).isEqualTo("Coaching for your career.");
    }

    @Test
    void consultationPriceIsTheCheapestPackagePrice() {
        ShopOffering offering = ShopOffering.consultation(ID, "Career Coaching", "Coaching for your career.",
                List.of(Money.chf("700.00"), Money.chf("450.00"), Money.chf("1800.00")));

        assertThat(offering.price()).isEqualTo(Money.chf("450.00"));
    }

    @Test
    void consultationOfferingWithOnePackageCostsItsPrice() {
        ShopOffering offering = ShopOffering.consultation(ID, "Career Coaching", "Coaching for your career.",
                List.of(Money.chf("700.00")));

        assertThat(offering.price()).isEqualTo(Money.chf("700.00"));
    }

    @Test
    void consultationOfferingNeedsAPackage() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ShopOffering.consultation(ID, "Career Coaching", "Coaching for your career.",
                        List.of()))
                .withMessageContaining("package");
    }

    @Test
    void rejectsMissingValues() {
        Money price = Money.chf("450.00");
        assertThatNullPointerException()
                .isThrownBy(() -> new ShopOffering(null, OfferingType.CONSULTATION, "Name", "Text", price));
        assertThatNullPointerException()
                .isThrownBy(() -> new ShopOffering(ID, null, "Name", "Text", price));
        assertThatNullPointerException()
                .isThrownBy(() -> new ShopOffering(ID, OfferingType.CONSULTATION, null, "Text", price));
        assertThatNullPointerException()
                .isThrownBy(() -> new ShopOffering(ID, OfferingType.CONSULTATION, "Name", null, price));
        assertThatNullPointerException()
                .isThrownBy(() -> new ShopOffering(ID, OfferingType.CONSULTATION, "Name", "Text", null));
    }

    @Test
    void offeringIdIsRequired() {
        assertThatNullPointerException().isThrownBy(() -> new OfferingId(null));
    }
}
