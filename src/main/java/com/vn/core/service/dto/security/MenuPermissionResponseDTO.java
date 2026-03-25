package com.vn.core.service.dto.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * DTO describing the current user's allowed menu ids for one frontend app.
 */
public class MenuPermissionResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String appName;

    private List<String> allowedMenuIds;

    public MenuPermissionResponseDTO() {
        // Empty constructor needed for Jackson.
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<String> getAllowedMenuIds() {
        return allowedMenuIds;
    }

    public void setAllowedMenuIds(List<String> allowedMenuIds) {
        this.allowedMenuIds = allowedMenuIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MenuPermissionResponseDTO)) {
            return false;
        }
        MenuPermissionResponseDTO other = (MenuPermissionResponseDTO) o;
        return Objects.equals(appName, other.appName) && Objects.equals(allowedMenuIds, other.allowedMenuIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, allowedMenuIds);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MenuPermissionResponseDTO{" +
            "appName='" + appName + '\'' +
            ", allowedMenuIds=" + allowedMenuIds +
            "}";
    }
}
