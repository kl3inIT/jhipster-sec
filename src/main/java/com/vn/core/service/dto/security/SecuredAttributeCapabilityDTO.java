package com.vn.core.service.dto.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * DTO describing current-user access to a single secured attribute.
 */
public class SecuredAttributeCapabilityDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;

    private boolean canView;

    private boolean canEdit;

    public SecuredAttributeCapabilityDTO() {
        // Empty constructor needed for Jackson.
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCanView() {
        return canView;
    }

    public void setCanView(boolean canView) {
        this.canView = canView;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecuredAttributeCapabilityDTO)) {
            return false;
        }
        SecuredAttributeCapabilityDTO other = (SecuredAttributeCapabilityDTO) o;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SecuredAttributeCapabilityDTO{" +
            "name='" + name + '\'' +
            ", canView=" + canView +
            ", canEdit=" + canEdit +
            "}";
    }
}
