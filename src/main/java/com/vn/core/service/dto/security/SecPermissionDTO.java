package com.vn.core.service.dto.security;

import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * DTO for a security permission entry (maps from SecPermission entity).
 */
public class SecPermissionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank
    @Size(max = 50)
    private String authorityName;

    @NotNull
    private String targetType;

    @NotBlank
    @Size(max = 255)
    private String target;

    @NotBlank
    @Size(max = 50)
    private String action;

    @NotNull
    private String effect;

    public SecPermissionDTO() {
        // Empty constructor needed for Jackson.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthorityName() {
        return authorityName;
    }

    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecPermissionDTO)) {
            return false;
        }
        SecPermissionDTO other = (SecPermissionDTO) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SecPermissionDTO{" +
            "id=" + id +
            ", authorityName='" + authorityName + '\'' +
            ", targetType='" + targetType + '\'' +
            ", target='" + target + '\'' +
            ", action='" + action + '\'' +
            ", effect='" + effect + '\'' +
            "}";
    }
}
