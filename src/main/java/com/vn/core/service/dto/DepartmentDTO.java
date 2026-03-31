package com.vn.core.service.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A DTO for the {@link com.vn.core.domain.Department} entity.
 */
public class DepartmentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String code;

    private String name;

    private String costCenter;

    private Set<EmployeeDTO> employees = new HashSet<>();

    public DepartmentDTO() {
        // Empty constructor needed for Jackson.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public Set<EmployeeDTO> getEmployees() {
        return employees;
    }

    public void setEmployees(Set<EmployeeDTO> employees) {
        this.employees = employees;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DepartmentDTO{" +
            "id=" + id +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", costCenter='" + costCenter + '\'' +
            ", employees=" + employees +
            "}";
    }
}
