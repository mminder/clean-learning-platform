package com.cleanlearning.domain.shared.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

  @Test
  void isInChf() {
    assertThat(Money.chf("450.00").currency()).isEqualTo("CHF");
  }

  @Test
  void hasTwoDecimals() {
    assertThat(Money.chf("450").amount()).isEqualTo(new BigDecimal("450.00"));
    assertThat(Money.chf("450.5").amount()).isEqualTo(new BigDecimal("450.50"));
    assertThat(Money.chf("450.000").amount()).isEqualTo(new BigDecimal("450.00"));
  }

  @Test
  void rejectsMoreThanTwoDecimals() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Money.chf("450.005"))
        .withMessageContaining("450.005");
  }

  @Test
  void acceptsZero() {
    assertThat(Money.chf("0").amount()).isEqualTo(new BigDecimal("0.00"));
  }

  @Test
  void rejectsNegativeAmounts() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Money.chf("-0.01"))
        .withMessageContaining("-0.01");
  }

  @Test
  void rejectsNull() {
    assertThatNullPointerException().isThrownBy(() -> new Money(null));
  }

  @Test
  void isComparedByValue() {
    assertThat(Money.chf("450.0")).isEqualTo(Money.chf("450.00"));
    assertThat(Money.chf("450.0")).hasSameHashCodeAs(Money.chf("450.00"));
    assertThat(Money.chf("450.00")).isNotEqualTo(Money.chf("700.00"));
  }

  @Test
  void ordersByAmount() {
    assertThat(Money.chf("450.00")).isLessThan(Money.chf("700.00"));
    assertThat(Money.chf("1800.00")).isGreaterThan(Money.chf("700.00"));
    assertThat(Money.chf("450.0")).isEqualByComparingTo(Money.chf("450.00"));
  }
}
