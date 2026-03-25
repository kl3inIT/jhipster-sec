package com.vn.core.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Backend-managed menu node metadata for a frontend app.
 */
@Entity
@Table(
    name = "sec_menu_definition",
    uniqueConstraints = @UniqueConstraint(name = "ux_sec_menu_definition_app_menu", columnNames = { "app_name", "menu_id" }),
    indexes = { @Index(name = "idx_sec_menu_definition_app_parent", columnList = "app_name, parent_menu_id") }
)
public class SecMenuDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "menu_id", nullable = false, length = 150)
    private String menuId;

    @Column(name = "app_name", nullable = false, length = 100)
    private String appName;

    @Column(name = "menu_name", nullable = false, length = 200)
    private String menuName;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "parent_menu_id", length = 150)
    private String parentMenuId;

    @Column(name = "route", length = 300)
    private String route;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "ordering", nullable = false)
    private Integer ordering;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecMenuDefinition id(Long id) {
        this.setId(id);
        return this;
    }

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }

    public SecMenuDefinition menuId(String menuId) {
        this.setMenuId(menuId);
        return this;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public SecMenuDefinition appName(String appName) {
        this.setAppName(appName);
        return this;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public SecMenuDefinition menuName(String menuName) {
        this.setMenuName(menuName);
        return this;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public SecMenuDefinition label(String label) {
        this.setLabel(label);
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SecMenuDefinition description(String description) {
        this.setDescription(description);
        return this;
    }

    public String getParentMenuId() {
        return parentMenuId;
    }

    public void setParentMenuId(String parentMenuId) {
        this.parentMenuId = parentMenuId;
    }

    public SecMenuDefinition parentMenuId(String parentMenuId) {
        this.setParentMenuId(parentMenuId);
        return this;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public SecMenuDefinition route(String route) {
        this.setRoute(route);
        return this;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public SecMenuDefinition icon(String icon) {
        this.setIcon(icon);
        return this;
    }

    public Integer getOrdering() {
        return ordering;
    }

    public void setOrdering(Integer ordering) {
        this.ordering = ordering;
    }

    public SecMenuDefinition ordering(Integer ordering) {
        this.setOrdering(ordering);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecMenuDefinition)) {
            return false;
        }
        SecMenuDefinition other = (SecMenuDefinition) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SecMenuDefinition{" +
            "id=" + getId() +
            ", menuId='" + getMenuId() + "'" +
            ", appName='" + getAppName() + "'" +
            ", menuName='" + getMenuName() + "'" +
            ", label='" + getLabel() + "'" +
            ", description='" + getDescription() + "'" +
            ", parentMenuId='" + getParentMenuId() + "'" +
            ", route='" + getRoute() + "'" +
            ", icon='" + getIcon() + "'" +
            ", ordering=" + getOrdering() +
            "}";
    }
}
