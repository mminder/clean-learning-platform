package com.cleanlearning.domain.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.shared.Money;

class ConsultationPackageTest {

    private static final ConsultationPackageId ID = new ConsultationPackageId(
            UUID.fromString("00000000-0000-0000-0000-000000001011"));

    @Test
    void isASessionCountWithAPrice() {
        ConsultationPackage consultationPackage = new ConsultationPackage(ID, 3, Money.chf("450.00"));

        assertThat(consultationPackage.id()).isEqualTo(ID);
        assertThat(consultationPackage.sessionCount()).isEqualTo(3);
        assertThat(consultationPackage.price()).isEqualTo(Money.chf("450.00"));
    }

    @Test
    void acceptsASingleSession() {
        assertThat(new ConsultationPackage(ID, 1, Money.chf("150.00")).sessionCount()).isEqualTo(1);
    }

    @Test
    void rejectsZeroAndNegativeSessionCounts() {
        Money price = Money.chf("450.00");
        assertThatIllegalArgumentException().isThrownBy(() -> new ConsultationPackage(ID, 0, price))
                .withMessageContaining("0");
        assertThatIllegalArgumentException().isThrownBy(() -> new ConsultationPackage(ID, -3, price))
                .withMessageContaining("-3");
    }

    @Test
    void rejectsMissingValues() {
        assertThatNullPointerException().isThrownBy(() -> new ConsultationPackage(null, 3, Money.chf("450.00")));
        assertThatNullPointerException().isThrownBy(() -> new ConsultationPackage(ID, 3, null));
        assertThatNullPointerException().isThrownBy(() -> new ConsultationPackageId(null));
    }
}
