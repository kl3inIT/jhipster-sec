package com.vn.core.domain.movie;

import com.vn.core.domain.movie.enumeration.ekip.ProductionRole;
import com.vn.core.security.catalog.SecuredEntity;
import jakarta.persistence.*;

@SecuredEntity(code = "production-ekip", fetchPlanCodes = { "production-ekip-list", "production-ekip-detail" })
@Entity
@Table(name = "production_ekip")
public class ProductionEkip {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Column(name = "ekip_name", length = 50)
    private String ekipName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private ProductionRole role;

    @ManyToOne
    @JoinColumn(name = "movie_profile_id")
    private MovieProfile movieProfile;

    public MovieProfile getMovieProfile() {
        return movieProfile;
    }

    public void setMovieProfile(MovieProfile movieProfile) {
        this.movieProfile = movieProfile;
    }

    public ProductionRole getRole() {
        return role;
    }

    public void setRole(ProductionRole role) {
        this.role = role;
    }

    public String getEkipName() {
        return ekipName;
    }

    public void setEkipName(String ekipName) {
        this.ekipName = ekipName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
