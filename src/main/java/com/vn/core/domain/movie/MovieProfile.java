package com.vn.core.domain.movie;

import com.vn.core.domain.movie.converter.GenreConverter;
import com.vn.core.domain.movie.enumeration.Classification;
import com.vn.core.domain.movie.enumeration.Genre;
import com.vn.core.domain.movie.enumeration.MovieType;
import com.vn.core.domain.movie.enumeration.Status;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "movie_profile")
public class MovieProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Column(name = "movie_name", length = 50)
    private String movieName;

    @Column(name = "production_year")
    private Integer productionYear;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    //Định hướng đề tài
    @Column(name = "theme_orientation")
    private String themeOrientation;

    //Tóm tắt nd
    @Column(name = "summary", length = 5000)
    private String summary;

    //Phân loại
    @Enumerated(EnumType.STRING)
    @Column(name = "classification")
    private Classification classification;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    //thể loại
    @Convert(converter = GenreConverter.class)
    @Column(name = "genre")
    private Genre genre;

    @Enumerated(EnumType.STRING)
    @Column(name = "movie_type")
    private MovieType movieType;

    @Column(name = "profile_code", unique = true, length = 30)
    private String profileCode;

    public String getProfileCode() {
        return profileCode;
    }

    public void setProfileCode(String profileCode) {
        this.profileCode = profileCode;
    }

    public MovieType getMovieType() {
        return movieType;
    }

    public void setMovieType(MovieType movieType) {
        this.movieType = movieType;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
        this.classification = classification;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getThemeOrientation() {
        return themeOrientation;
    }

    public void setThemeOrientation(String themeOrientation) {
        this.themeOrientation = themeOrientation;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public Integer getProductionYear() {
        return productionYear;
    }

    public void setProductionYear(Integer productionYear) {
        this.productionYear = productionYear;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
