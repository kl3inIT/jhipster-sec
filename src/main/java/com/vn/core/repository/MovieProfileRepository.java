package com.vn.core.repository;

import com.vn.core.domain.movie.MovieProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Infrastructure repository so SecureDataManager can resolve JPA + Specification access.
 * Application services must continue using SecureDataManager instead of this repository.
 */
@Repository
public interface MovieProfileRepository extends JpaRepository<MovieProfile, Long>, JpaSpecificationExecutor<MovieProfile> {}
