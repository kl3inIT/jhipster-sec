package com.vn.core.repository.movie;


import com.vn.core.domain.movie.TopicOrientation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicOrientationRepository extends JpaRepository<TopicOrientation, Long> , JpaSpecificationExecutor<TopicOrientation> {
    @Query("select t.code from TopicOrientation t where t.code like concat(:prefix, '%') order by t.code desc")
    Optional<String> findTopCodeByPrefix(@Param("prefix") String prefix);
}
