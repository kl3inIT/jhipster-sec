package com.vn.core.service;

import com.vn.core.domain.Organization;
import com.vn.core.repository.OrganizationRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Benchmark baseline service that reads organizations through the standard repository flow.
 */
@Service
@Transactional(readOnly = true)
public class BenchmarkOrganizationStandardService {

    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkOrganizationStandardService.class);

    private final OrganizationRepository organizationRepository;

    public BenchmarkOrganizationStandardService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public Page<Organization> list(Pageable pageable) {
        LOG.debug("Benchmark request to list organizations with standard flow");
        return organizationRepository.findAll(pageable);
    }

    public Optional<Organization> findOne(Long id) {
        LOG.debug("Benchmark request to get organization with standard flow : {}", id);
        return organizationRepository.findById(id);
    }
}
