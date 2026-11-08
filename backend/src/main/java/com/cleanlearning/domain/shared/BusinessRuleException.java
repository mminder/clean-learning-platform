package com.cleanlearning.domain.shared;

import java.util.Objects;

/**
 * A business rule says no, such as publishing an offering without a package. The API answers 409 with the message as
 * {@code detail}, written for the user, and the {@code code} (D-34). The code is stable: code that reacts to one
 * conflict, and tests, rely on it; the message may change.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public String code() {
        return code;
    }
}
