package com.vn.core.domain.movie;

import com.vn.core.domain.movie.enumeration.Status;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "topic_orientation")
public class TopicOrientation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 20)
    private String code;

    @ManyToOne
    @JoinColumn(name = "movie_profile_id")
    private MovieProfile movieProfile;

    @Column(name = "proposer")
    private String proposer;

    @Column(name = "submit_date")
    private LocalDate submitDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "title")
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getProposer() {
        return proposer;
    }

    public void setProposer(String proposer) {
        this.proposer = proposer;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDate getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(LocalDate submitDate) {
        this.submitDate = submitDate;
    }

    public MovieProfile getMovieProfile() {
        return movieProfile;
    }

    public void setMovieProfile(MovieProfile movieProfile) {
        this.movieProfile = movieProfile;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
