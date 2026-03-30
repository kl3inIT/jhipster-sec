package com.vn.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vn.core.security.catalog.SecuredEntity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Shoe.
 */
@Entity
@SecuredEntity
@Table(name = "shoe")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Shoe implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Column(name = "name")
    private String name;

    @Column(name = "decription")
    private String decription;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "shoe")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "shoe" }, allowSetters = true)
    private Set<ShoeVariant> shoeVariants = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    @JsonIgnoreProperties(value = { "shoes" }, allowSetters = true)
    private Brand brand;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Shoe id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Shoe name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDecription() {
        return this.decription;
    }

    public Shoe decription(String decription) {
        this.setDecription(decription);
        return this;
    }

    public void setDecription(String decription) {
        this.decription = decription;
    }

    public Set<ShoeVariant> getShoeVariants() {
        return this.shoeVariants;
    }

    public void setShoeVariants(Set<ShoeVariant> shoeVariants) {
        if (this.shoeVariants != null) {
            this.shoeVariants.forEach(i -> i.setShoe(null));
        }
        if (shoeVariants != null) {
            shoeVariants.forEach(i -> i.setShoe(this));
        }
        this.shoeVariants = shoeVariants;
    }

    public Shoe shoeVariants(Set<ShoeVariant> shoeVariants) {
        this.setShoeVariants(shoeVariants);
        return this;
    }

    public Shoe addShoeVariant(ShoeVariant shoeVariant) {
        this.shoeVariants.add(shoeVariant);
        shoeVariant.setShoe(this);
        return this;
    }

    public Shoe removeShoeVariant(ShoeVariant shoeVariant) {
        this.shoeVariants.remove(shoeVariant);
        shoeVariant.setShoe(null);
        return this;
    }

    public Brand getBrand() {
        return this.brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    public Shoe brand(Brand brand) {
        this.setBrand(brand);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Shoe)) {
            return false;
        }
        return getId() != null && getId().equals(((Shoe) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Shoe{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", decription='" + getDecription() + "'" +
            "}";
    }
}
