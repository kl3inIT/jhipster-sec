package com.vn.core.domain.movie.enumeration;

public enum Classification {
    MOVIE("Phim truyện"),
    DOCUMENTARY("Tài liệu"),
    SCIENCE("Khoa học"),
    ANIMATION("Hoạt hình");

    private final String displayName;

    Classification(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
