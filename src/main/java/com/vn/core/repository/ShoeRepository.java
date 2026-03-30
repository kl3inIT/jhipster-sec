package com.vn.core.repository;

import com.vn.core.domain.Shoe;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Shoe entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ShoeRepository extends JpaRepository<Shoe, Long> {}
