package com.vn.core.service;

import com.vn.core.repository.OrganizationRepository;
import com.vn.core.service.dto.OrganizationDTO;
import com.vn.core.service.dto.OrganizationDetailDTO;
import com.vn.core.service.mapper.OrganizationMapper;
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
    private final OrganizationMapper organizationMapper;

    public BenchmarkOrganizationStandardService(OrganizationRepository organizationRepository, OrganizationMapper organizationMapper) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
    }

    public Page<OrganizationDTO> list(Pageable pageable) {
        LOG.debug("Benchmark request to list organizations with standard flow");
        return organizationRepository.findAll(pageable).map(organizationMapper::toDto);
    }

    public Optional<OrganizationDetailDTO> findOne(Long id) {
        LOG.debug("Benchmark request to get organization with standard flow : {}", id);
        return organizationRepository.findById(id).map(organizationMapper::toDetailDto);
    }
}
