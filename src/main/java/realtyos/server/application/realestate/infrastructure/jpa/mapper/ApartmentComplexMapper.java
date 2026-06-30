package realtyos.server.application.realestate.infrastructure.jpa.mapper;

import org.mapstruct.Mapper;
import realtyos.server.application.realestate.domain.ApartmentComplex;
import realtyos.server.application.realestate.infrastructure.jpa.entity.ApartmentComplexJpaEntity;

@Mapper(componentModel = "spring")
public interface ApartmentComplexMapper {
    ApartmentComplexJpaEntity toEntity(ApartmentComplex domain);
    ApartmentComplex toDomain(ApartmentComplexJpaEntity entity);
}
