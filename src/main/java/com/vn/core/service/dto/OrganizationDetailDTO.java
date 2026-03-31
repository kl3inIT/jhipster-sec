package com.vn.core.service.dto;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * A DTO for the {@link com.vn.core.domain.Organization} entity (detail view).
 */
public class OrganizationDetailDTO extends OrganizationDTO {

    @Serial
    private static final long serialVersionUID = 1L;

    private BigDecimal budget;

    private Set<DepartmentDTO> departments = new HashSet<>();

    public OrganizationDetailDTO() {
        // Empty constructor needed for Jackson.
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public Set<DepartmentDTO> getDepartments() {
        return departments;
    }

    public void setDepartments(Set<DepartmentDTO> departments) {
        this.departments = departments;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrganizationDetailDTO{" +
            "id=" + getId() +
            ", code='" + getCode() + '\'' +
            ", name='" + getName() + '\'' +
            ", ownerLogin='" + getOwnerLogin() + '\'' +
            ", budget=" + budget +
            ", departments=" + departments +
            "}";
    }
}
