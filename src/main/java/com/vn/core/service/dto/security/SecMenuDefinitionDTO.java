package com.vn.core.service.dto.security;

import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * DTO for a menu definition entry (maps from SecMenuDefinition entity).
 */
public class SecMenuDefinitionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank
    @Size(max = 150)
    private String menuId;

    @NotBlank
    @Size(max = 100)
    private String appName;

    @NotBlank
    @Size(max = 200)
    private String menuName;

    @NotBlank
    @Size(max = 200)
    private String label;

    @Size(max = 500)
    private String description;

    @Size(max = 150)
    private String parentMenuId;

    @Size(max = 300)
    private String route;

    @Size(max = 100)
    private String icon;

    @NotNull
    private Integer ordering;

    public SecMenuDefinitionDTO() {
        // Empty constructor needed for Jackson.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParentMenuId() {
        return parentMenuId;
    }

    public void setParentMenuId(String parentMenuId) {
        this.parentMenuId = parentMenuId;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getOrdering() {
        return ordering;
    }

    public void setOrdering(Integer ordering) {
        this.ordering = ordering;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecMenuDefinitionDTO)) {
            return false;
        }
        SecMenuDefinitionDTO other = (SecMenuDefinitionDTO) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SecMenuDefinitionDTO{" +
            "id=" + id +
            ", menuId='" + menuId + '\'' +
            ", appName='" + appName + '\'' +
            ", menuName='" + menuName + '\'' +
            ", label='" + label + '\'' +
            ", description='" + description + '\'' +
            ", parentMenuId='" + parentMenuId + '\'' +
            ", route='" + route + '\'' +
            ", icon='" + icon + '\'' +
            ", ordering=" + ordering +
            "}";
    }
}
