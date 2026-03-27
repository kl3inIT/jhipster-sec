package com.vn.core.domain.movie;

import com.vn.core.domain.movie.enumeration.ekip.ProductionRole;
import jakarta.persistence.*;

@Entity
@Table(name = "production_ekip")
public class ProductionEkip {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long ekip_id;

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

    public Long getEkip_id() {
        return ekip_id;
    }

    public void setEkip_id(Long ekip_id) {
        this.ekip_id = ekip_id;
    }
}
