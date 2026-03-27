package com.vn.core.domain.movie.enumeration;

public enum Status {
    ACTIVE("Đang hoạt động"),
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Từ chối"),
    COMPLETED("Hoàn thành"),
    CANCELED("Đã hủy");

    private final String displayName;

    Status(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
