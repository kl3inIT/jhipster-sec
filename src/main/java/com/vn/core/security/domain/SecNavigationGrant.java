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
import jakarta.validation.constraints.Pattern;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * An app-scoped navigation grant linking an authority to a frontend-owned shell node id.
 */
@Entity
@Table(
    name = "sec_navigation_grant",
    uniqueConstraints = @UniqueConstraint(name = "ux_sec_navigation_grant_authority_app_node", columnNames = { "authority_name", "app_name", "node_id" }),
    indexes = {
        @Index(name = "idx_sec_navigation_grant_app_authority", columnList = "app_name, authority_name"),
        @Index(name = "idx_sec_navigation_grant_app_node", columnList = "app_name, node_id"),
    }
)
public class SecNavigationGrant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "authority_name", nullable = false, length = 50)
    private String authorityName;

    @Column(name = "app_name", nullable = false, length = 100)
    private String appName;

    @Column(name = "node_id", nullable = false, length = 150)
    private String nodeId;

    @Pattern(regexp = "ALLOW|DENY")
    @Column(name = "effect", nullable = false, length = 10)
    private String effect;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecNavigationGrant id(Long id) {
        this.setId(id);
        return this;
    }

    public String getAuthorityName() {
        return authorityName;
    }

    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }

    public SecNavigationGrant authorityName(String authorityName) {
        this.setAuthorityName(authorityName);
        return this;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public SecNavigationGrant appName(String appName) {
        this.setAppName(appName);
        return this;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public SecNavigationGrant nodeId(String nodeId) {
        this.setNodeId(nodeId);
        return this;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public SecNavigationGrant effect(String effect) {
        this.setEffect(effect);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecNavigationGrant)) {
            return false;
        }
        SecNavigationGrant other = (SecNavigationGrant) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SecNavigationGrant{" +
            "id=" + getId() +
            ", authorityName='" + getAuthorityName() + "'" +
            ", appName='" + getAppName() + "'" +
            ", nodeId='" + getNodeId() + "'" +
            ", effect='" + getEffect() + "'" +
            "}";
    }
}
