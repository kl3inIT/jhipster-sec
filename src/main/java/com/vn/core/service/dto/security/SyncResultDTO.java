package com.vn.core.service.dto.security;

import java.io.Serial;
import java.io.Serializable;

/**
 * Response DTO from the menu definition sync endpoint, reporting how many nodes were inserted vs skipped.
 */
public class SyncResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int seeded;
    private int skipped;

    public SyncResultDTO() {
        // Empty constructor needed for Jackson.
    }

    public SyncResultDTO(int seeded, int skipped) {
        this.seeded = seeded;
        this.skipped = skipped;
    }

    public int getSeeded() {
        return seeded;
    }

    public int getSkipped() {
        return skipped;
    }
}
