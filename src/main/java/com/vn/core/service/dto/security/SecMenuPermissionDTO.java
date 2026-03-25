package com.vn.core.service.dto.security;

import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * DTO for a menu permission entry (maps from SecMenuPermission entity).
 */
public class SecMenuPermissionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank
    @Size(max = 50)
    private String role;

    @NotBlank
    @Size(max = 100)
    private String appName;

    @NotBlank
    @Size(max = 150)
    private String menuId;

    @Pattern(regexp = "ALLOW|DENY")
    private String effect;

    public SecMenuPermissionDTO() {
        // Empty constructor needed for Jackson.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
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
        if (!(o instanceof SecMenuPermissionDTO)) {
            return false;
        }
        SecMenuPermissionDTO other = (SecMenuPermissionDTO) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SecMenuPermissionDTO{" +
            "id=" + id +
            ", role='" + role + '\'' +
            ", appName='" + appName + '\'' +
            ", menuId='" + menuId + '\'' +
            ", effect='" + effect + '\'' +
            "}";
    }
}
