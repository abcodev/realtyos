package realtyos.server.application.realestate.infrastructure.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import realtyos.server.application.realestate.domain.ApartmentComplex;
import realtyos.server.application.realestate.infrastructure.jpa.entity.ApartmentComplexJpaEntity;

@Mapper(componentModel = "spring")
public interface ApartmentComplexMapper {
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "lastSyncedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "deletedAt", ignore = true)
    ApartmentComplexJpaEntity toEntity(ApartmentComplex domain);

    ApartmentComplex toDomain(ApartmentComplexJpaEntity entity);
}
