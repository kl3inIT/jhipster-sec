package com.vn.core.service.mapper.security;

import com.vn.core.security.domain.SecRowPolicy;
import com.vn.core.service.dto.security.SecRowPolicyDTO;
import com.vn.core.service.mapper.EntityMapper;
import org.mapstruct.*;

/**
 * MapStruct mapper for {@link SecRowPolicy} and {@link SecRowPolicyDTO}.
 * All fields are String-to-String or Long-to-Long so no custom conversions are needed.
 */
@Mapper(componentModel = "spring")
public interface SecRowPolicyMapper extends EntityMapper<SecRowPolicyDTO, SecRowPolicy> {}
