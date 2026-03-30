package com.vn.core.repository;

import com.vn.core.domain.ShoeVariant;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ShoeVariant entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ShoeVariantRepository extends JpaRepository<ShoeVariant, Long> {}
