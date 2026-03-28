package com.vn.core.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Pattern;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * An app-scoped menu permission linking a role to a frontend-owned menu node id.
 */
@Entity
@Table(
    name = "sec_menu_permission",
    uniqueConstraints = @UniqueConstraint(name = "ux_sec_menu_permission_role_app_menu", columnNames = { "role", "app_name", "menu_id" }),
    indexes = {
        @Index(name = "idx_sec_menu_permission_app_role", columnList = "app_name, role"),
        @Index(name = "idx_sec_menu_permission_app_menu", columnList = "app_name, menu_id"),
    }
)
public class SecMenuPermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_name", nullable = false, length = 100)
    private MenuAppName appName;

    @Column(name = "menu_id", nullable = false, length = 150)
    private String menuId;

    @Pattern(regexp = "ALLOW|DENY")
    @Column(name = "effect", nullable = false, length = 10)
    private String effect;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecMenuPermission id(Long id) {
        this.setId(id);
        return this;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public SecMenuPermission role(String role) {
        this.setRole(role);
        return this;
    }

    public MenuAppName getAppName() {
        return appName;
    }

    public void setAppName(MenuAppName appName) {
        this.appName = appName;
    }

    public SecMenuPermission appName(MenuAppName appName) {
        this.setAppName(appName);
        return this;
    }

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }

    public SecMenuPermission menuId(String menuId) {
        this.setMenuId(menuId);
        return this;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public SecMenuPermission effect(String effect) {
        this.setEffect(effect);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecMenuPermission)) {
            return false;
        }
        SecMenuPermission other = (SecMenuPermission) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SecMenuPermission{" +
            "id=" + getId() +
            ", role='" + getRole() + "'" +
            ", appName='" + getAppName() + "'" +
            ", menuId='" + getMenuId() + "'" +
            ", effect='" + getEffect() + "'" +
            "}";
    }
}
