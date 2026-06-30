package realtyos.server.application.realestate.infrastructure.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import realtyos.server.application.common.entity.BaseEntity;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Table(name = "real_estate_apartment_complex")
public class ApartmentComplexJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String kaptCode;

    @Column(nullable = false, length = 255)
    private String kaptName;

    @Column(length = 100)
    private String as1;

    @Column(length = 100)
    private String as2;

    @Column(length = 100)
    private String as3;

    @Column(length = 100)
    private String as4;

    @Column(length = 20)
    private String bjdCode;

    @Column(length = 500)
    private String fullAddress;
}
