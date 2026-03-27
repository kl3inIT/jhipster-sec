package com.vn.core.domain.movie.enumeration;

public enum Genre {
    ACTION("Hành động"),
    COMEDY("Hài"),
    DRAMA("Tâm lý"),
    HORROR("Kinh dị"),
    ROMANCE("Tình cảm"),
    SCI_FI("Khoa học viễn tưởng"),
    ANIMATION("Hoạt hình");

    private final String displayName;

    Genre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
