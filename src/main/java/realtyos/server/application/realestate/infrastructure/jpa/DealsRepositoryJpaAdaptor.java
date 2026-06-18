package realtyos.server.application.realestate.infrastructure.jpa;

import realtyos.server.application.realestate.domain.Deals;
import realtyos.server.application.realestate.domain.DealsRepository;
import realtyos.server.application.realestate.infrastructure.jpa.entity.DealsJpaEntity;
import realtyos.server.application.realestate.infrastructure.jpa.mapper.DealsMapper;
import realtyos.server.application.realestate.infrastructure.jpa.repository.DealsJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealsRepositoryJpaAdaptor implements DealsRepository {

    private final DealsJpaRepository jpaRepository;
    private final DealsMapper mapper;

    @Transactional
    @Override
    public List<Deals> saveAll(List<Deals> deals) {

        if (deals.isEmpty()) {
            return List.of();
        }

        Map<String, List<Deals>> groupedDeals = deals.stream()
                .collect(Collectors.groupingBy(d ->
                        d.sggCode() + "-" + d.dealYear() + "-" + d.dealMonth()
                ));

        List<DealsJpaEntity> entitiesToSave = new java.util.ArrayList<>();

        int newCount = 0;
        int updatedCount = 0;

        for (List<Deals> batch : groupedDeals.values()) {

            Deals sample = batch.getFirst();

            List<DealsJpaEntity> existingDeals =
                    jpaRepository.findBySggCodeAndDealYearAndDealMonth(
                            sample.sggCode(),
                            sample.dealYear(),
                            sample.dealMonth()
                    );

            Map<DealKey, DealsJpaEntity> existingMap =
                    existingDeals.stream()
                            .collect(Collectors.toMap(
                                    this::buildKey,
                                    e -> e,
                                    (a, b) -> a
                            ));

            for (Deals deal : batch) {

                DealKey key = buildKey(deal);

                DealsJpaEntity existing = existingMap.get(key);

                if (existing != null) {
                    mapper.updateEntityFromDomain(deal, existing);
                    entitiesToSave.add(existing);
                    updatedCount++;
                } else {
                    entitiesToSave.add(mapper.mapToJpaEntity(deal));
                    newCount++;
                }
            }
        }

        log.info("Upsert summary - New: {}, Updated: {}", newCount, updatedCount);

        return jpaRepository.saveAll(entitiesToSave).stream()
                .map(mapper::mapToDomain)
                .toList();
    }

    @Override
    public List<Deals> findSearchIndexBatch(Integer year, Long afterId, int batchSize) {
        PageRequest pageRequest = PageRequest.of(0, Math.max(1, Math.min(10_000, batchSize)));
        return jpaRepository.findSearchIndexBatch(year, afterId == null ? 0L : afterId, pageRequest).stream()
                .map(mapper::mapToDomain)
                .toList();
    }

    @Override
    public List<Deals> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllById(ids).stream()
                .map(mapper::mapToDomain)
                .toList();
    }

    private record DealKey(
            String sggCode,
            Integer dealYear,
            Integer dealMonth,
            Integer dealDay,
            String aptName,
            String floor,
            String umdName,
            String excluUseArea,
            String jibun,
            String dealAmount) {}

    private DealKey buildKey(Deals d) {
        return new DealKey(
                d.sggCode(),
                d.dealYear(),
                d.dealMonth(),
                d.dealDay(),
                d.aptName(),
                d.floor(),
                d.umdName(),
                d.excluUseArea(),
                d.jibun(),
                d.dealAmount()
        );
    }

    private DealKey buildKey(DealsJpaEntity e) {
        return new DealKey(
                e.getSggCode(),
                e.getDealYear(),
                e.getDealMonth(),
                e.getDealDay(),
                e.getAptName(),
                e.getFloor(),
                e.getUmdName(),
                e.getExcluUseArea(),
                e.getJibun(),
                e.getDealAmount()
        );
    }


}
