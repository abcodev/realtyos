package realtyos.server.application.realestate.infrastructure.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import realtyos.server.application.realestate.domain.ApartmentComplexBasisInfo;
import realtyos.server.application.realestate.infrastructure.jpa.entity.ApartmentComplexBasisInfoJpaEntity;

@Mapper(componentModel = "spring")
public interface ApartmentComplexBasisInfoMapper {
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "lastSyncedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "deletedAt", ignore = true)
    ApartmentComplexBasisInfoJpaEntity toEntity(ApartmentComplexBasisInfo domain);

    ApartmentComplexBasisInfo toDomain(ApartmentComplexBasisInfoJpaEntity entity);
}
