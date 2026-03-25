package com.vn.core.service.dto.security;

import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;

/**
 * DTO for each menu node in a sync request payload.
 */
public class SyncNodeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    @Size(max = 150)
    private String parentMenuId;

    @Size(max = 300)
    private String route;

    @Size(max = 100)
    private String icon;

    @NotNull
    private Integer ordering;

    public SyncNodeDTO() {
        // Empty constructor needed for Jackson.
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
}
