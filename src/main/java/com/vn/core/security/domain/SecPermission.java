package com.vn.core.security.domain;

import com.vn.core.security.permission.TargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * A permission entry linking an authority (role) to a specific target and action.
 * Uses a String authority_name FK (not @ManyToOne) so it remains decoupled from the
 * Authority entity lifecycle and supports the string-based security context bridge.
 */
@Entity
@Table(name = "sec_permission")
public class SecPermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "authority_name", nullable = false, length = 50)
    private String authorityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private TargetType targetType;

    @Column(name = "target", nullable = false, length = 255)
    private String target;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "effect", nullable = false, length = 10)
    private String effect;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecPermission id(Long id) {
        this.setId(id);
        return this;
    }

    public String getAuthorityName() {
        return authorityName;
    }

    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }

    public SecPermission authorityName(String authorityName) {
        this.setAuthorityName(authorityName);
        return this;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public SecPermission targetType(TargetType targetType) {
        this.setTargetType(targetType);
        return this;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public SecPermission target(String target) {
        this.setTarget(target);
        return this;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public SecPermission action(String action) {
        this.setAction(action);
        return this;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public SecPermission effect(String effect) {
        this.setEffect(effect);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecPermission)) {
            return false;
        }
        SecPermission other = (SecPermission) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SecPermission{" +
            "id=" + getId() +
            ", authorityName='" + getAuthorityName() + "'" +
            ", targetType='" + getTargetType() + "'" +
            ", target='" + getTarget() + "'" +
            ", action='" + getAction() + "'" +
            ", effect='" + getEffect() + "'" +
            "}";
    }
}
