package com.vn.core.repository.movie;


import com.vn.core.domain.movie.TopicOrientation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicOrientationRepository extends JpaRepository<TopicOrientation, Long> , JpaSpecificationExecutor<TopicOrientation> {

}
