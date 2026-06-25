package realtyos.server.application.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID append(String aggregateType, String aggregateId, String eventType, String topic, Object payload) {
        UUID eventId = UUID.randomUUID();
        outboxRepository.save(
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                topic,
                toJson(payload)
        );
        return eventId;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("outbox payload 직렬화에 실패했습니다.", e);
        }
    }
}
