package com.vn.core.domain.movie.enumeration.ekip;

public enum ProductionRole {
    DIRECTOR("Đạo diễn"),
    PRODUCER("Nhà sản xuất"),
    ASSISTANT_DIRECTOR("Trợ lý đạo diễn"),
    CINEMATOGRAPHER("Quay phim"),
    ACTOR("Diễn viên"),
    EDITOR("Biên tập");

    private final String displayName;

    ProductionRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
