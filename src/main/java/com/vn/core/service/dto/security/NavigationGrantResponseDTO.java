package com.vn.core.service.dto.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * DTO describing the current user's allowed navigation leaf ids for one frontend app.
 */
public class NavigationGrantResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String appName;

    private List<String> allowedNodeIds;

    public NavigationGrantResponseDTO() {
        // Empty constructor needed for Jackson.
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<String> getAllowedNodeIds() {
        return allowedNodeIds;
    }

    public void setAllowedNodeIds(List<String> allowedNodeIds) {
        this.allowedNodeIds = allowedNodeIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NavigationGrantResponseDTO)) {
            return false;
        }
        NavigationGrantResponseDTO other = (NavigationGrantResponseDTO) o;
        return Objects.equals(appName, other.appName) && Objects.equals(allowedNodeIds, other.allowedNodeIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, allowedNodeIds);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NavigationGrantResponseDTO{" +
            "appName='" + appName + '\'' +
            ", allowedNodeIds=" + allowedNodeIds +
            "}";
    }
}
