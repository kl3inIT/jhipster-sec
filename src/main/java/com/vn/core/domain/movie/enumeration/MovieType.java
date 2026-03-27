package com.vn.core.domain.movie.enumeration;

public enum MovieType {
    DIGITAL("Phim kĩ thuật số"),
    MOVIE("Phim nhựa");

    private final String displayName;

    MovieType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
