package com.vn.core.service.dto.security;

import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * DTO for a security role (maps from the Authority entity).
 * Manual mapping is used in controllers: controllers call
 * {@code new SecRoleDTO(authority.getName(), authority.getDisplayName(), authority.getType().name())}
 * rather than MapStruct (Authority's Persistable pattern complicates code generation).
 */
public class SecRoleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "[A-Z_]+")
    private String name;

    @Size(max = 255)
    private String displayName;

    @NotNull
    @Pattern(regexp = "RESOURCE")
    private String type;

    public SecRoleDTO() {
        // Empty constructor needed for Jackson.
    }

    public SecRoleDTO(String name, String displayName, String type) {
        this.name = name;
        this.displayName = displayName;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecRoleDTO)) {
            return false;
        }
        SecRoleDTO other = (SecRoleDTO) o;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SecRoleDTO{" +
            "name='" + name + '\'' +
            ", displayName='" + displayName + '\'' +
            ", type='" + type + '\'' +
            "}";
    }
}
