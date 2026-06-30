package realtyos.server.application.realestate.infrastructure.jpa.mapper;

import org.mapstruct.Mapper;
import realtyos.server.application.realestate.domain.ApartmentComplexBasisInfo;
import realtyos.server.application.realestate.infrastructure.jpa.entity.ApartmentComplexBasisInfoJpaEntity;

@Mapper(componentModel = "spring")
public interface ApartmentComplexBasisInfoMapper {
    ApartmentComplexBasisInfoJpaEntity toEntity(ApartmentComplexBasisInfo domain);
    ApartmentComplexBasisInfo toDomain(ApartmentComplexBasisInfoJpaEntity entity);
}
