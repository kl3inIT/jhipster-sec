package com.vn.core.security.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Supported frontend application identifiers for menu-definition and menu-permission security data.
 */
public enum MenuAppName {
    JHIPSTER_SECURITY_PLATFORM("jhipster-security-platform"),
    SALES_CONSOLE("sales-console"),
    MOVIE_APP("movie app");

    private final String value;

    MenuAppName(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<MenuAppName> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            return Optional.empty();
        }

        return Arrays.stream(values())
            .filter(candidate -> candidate.value.equalsIgnoreCase(normalizedValue) || candidate.name().equalsIgnoreCase(normalizedValue))
            .findFirst();
    }

    @Override
    public String toString() {
        return value;
    }
}
