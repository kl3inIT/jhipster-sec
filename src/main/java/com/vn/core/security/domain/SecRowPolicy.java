package com.vn.core.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * A row-level policy definition that restricts which rows of an entity are visible
 * or modifiable based on an evaluated expression.
 */
@Entity
@Table(name = "sec_row_policy")
public class SecRowPolicy implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "entity_name", nullable = false, length = 255)
    private String entityName;

    @Column(name = "operation", nullable = false, length = 20)
    private String operation;

    @Column(name = "policy_type", nullable = false, length = 20)
    private String policyType;

    @Column(name = "expression", nullable = false, length = 1000)
    private String expression;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecRowPolicy id(Long id) {
        this.setId(id);
        return this;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public SecRowPolicy code(String code) {
        this.setCode(code);
        return this;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public SecRowPolicy entityName(String entityName) {
        this.setEntityName(entityName);
        return this;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public SecRowPolicy operation(String operation) {
        this.setOperation(operation);
        return this;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public SecRowPolicy policyType(String policyType) {
        this.setPolicyType(policyType);
        return this;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public SecRowPolicy expression(String expression) {
        this.setExpression(expression);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecRowPolicy)) {
            return false;
        }
        SecRowPolicy other = (SecRowPolicy) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SecRowPolicy{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", entityName='" + getEntityName() + "'" +
            ", operation='" + getOperation() + "'" +
            ", policyType='" + getPolicyType() + "'" +
            ", expression='" + getExpression() + "'" +
            "}";
    }
}
