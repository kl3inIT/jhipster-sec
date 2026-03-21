package com.vn.core.service.dto.security;

import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * DTO for a row-level policy definition (maps from SecRowPolicy entity).
 */
public class SecRowPolicyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank
    @Size(max = 100)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String entityName;

    @NotBlank
    @Size(max = 20)
    private String operation;

    @NotNull
    private String policyType;

    @NotBlank
    @Size(max = 1000)
    private String expression;

    public SecRowPolicyDTO() {
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

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecRowPolicyDTO)) {
            return false;
        }
        SecRowPolicyDTO other = (SecRowPolicyDTO) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SecRowPolicyDTO{" +
            "id=" + id +
            ", code='" + code + '\'' +
            ", entityName='" + entityName + '\'' +
            ", operation='" + operation + '\'' +
            ", policyType='" + policyType + '\'' +
            ", expression='" + expression + '\'' +
            "}";
    }
}
