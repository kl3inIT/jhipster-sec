package com.vn.core.service.dto.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * DTO representing a single secured entity in the catalog,
 * with its code, display name, supported operations, and JPA attribute names.
 */
public class SecCatalogEntryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String code;

    private String displayName;

    private List<String> operations;

    private List<String> attributes;

    public SecCatalogEntryDTO() {
        // Empty constructor needed for Jackson.
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getOperations() {
        return operations;
    }

    public void setOperations(List<String> operations) {
        this.operations = operations;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecCatalogEntryDTO)) {
            return false;
        }
        SecCatalogEntryDTO other = (SecCatalogEntryDTO) o;
        return Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SecCatalogEntryDTO{" +
            "code='" + code + '\'' +
            ", displayName='" + displayName + '\'' +
            ", operations=" + operations +
            ", attributes=" + attributes +
            "}";
    }
}
