package com.vn.core.service.dto.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * DTO describing current-user entity and attribute capabilities for one secured entity.
 */
public class SecuredEntityCapabilityDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String code;

    private boolean canCreate;

    private boolean canRead;

    private boolean canUpdate;

    private boolean canDelete;

    private List<SecuredAttributeCapabilityDTO> attributes;

    public SecuredEntityCapabilityDTO() {
        // Empty constructor needed for Jackson.
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isCanCreate() {
        return canCreate;
    }

    public void setCanCreate(boolean canCreate) {
        this.canCreate = canCreate;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public void setCanRead(boolean canRead) {
        this.canRead = canRead;
    }

    public boolean isCanUpdate() {
        return canUpdate;
    }

    public void setCanUpdate(boolean canUpdate) {
        this.canUpdate = canUpdate;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }

    public List<SecuredAttributeCapabilityDTO> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<SecuredAttributeCapabilityDTO> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecuredEntityCapabilityDTO)) {
            return false;
        }
        SecuredEntityCapabilityDTO other = (SecuredEntityCapabilityDTO) o;
        return Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SecuredEntityCapabilityDTO{" +
            "code='" + code + '\'' +
            ", canCreate=" + canCreate +
            ", canRead=" + canRead +
            ", canUpdate=" + canUpdate +
            ", canDelete=" + canDelete +
            ", attributes=" + attributes +
            "}";
    }
}
