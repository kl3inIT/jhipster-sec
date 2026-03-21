package com.vn.core.service.mapper.security;

import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.permission.TargetType;
import com.vn.core.service.dto.security.SecPermissionDTO;
import com.vn.core.service.mapper.EntityMapper;
import org.mapstruct.*;

/**
 * MapStruct mapper for {@link SecPermission} and {@link SecPermissionDTO}.
 * <p>
 * The {@code targetType} field is a {@link TargetType} enum on the entity but a {@code String}
 * on the DTO. MapStruct uses the provided default methods to convert between them.
 */
@Mapper(componentModel = "spring")
public interface SecPermissionMapper extends EntityMapper<SecPermissionDTO, SecPermission> {
    @Override
    @Mapping(
        target = "targetType",
        expression = "java(dto.getTargetType() != null ? com.vn.core.security.permission.TargetType.valueOf(dto.getTargetType()) : null)"
    )
    SecPermission toEntity(SecPermissionDTO dto);

    @Override
    @Mapping(target = "targetType", expression = "java(entity.getTargetType() != null ? entity.getTargetType().name() : null)")
    SecPermissionDTO toDto(SecPermission entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(
        target = "targetType",
        expression = "java(dto.getTargetType() != null ? com.vn.core.security.permission.TargetType.valueOf(dto.getTargetType()) : null)"
    )
    void partialUpdate(@MappingTarget SecPermission entity, SecPermissionDTO dto);
}
