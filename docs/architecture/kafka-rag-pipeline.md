# Kafka RAG Embedding Pipeline

RealtyOS의 RAG 임베딩 작업은 시간이 오래 걸리고 외부 AI API 장애 영향을 받는다. 이를 동기 API 안에서 모두 처리하면 요청 timeout, 중복 실행, 실패 복구 문제가 생길 수 있다.

이 문서는 Kafka 기반 비동기 임베딩 파이프라인의 설계를 정리한다.

## Flow

```text
POST /api/v1/rag/documents/embeddings/async
        |
        v
RagEmbeddingAsyncService
        |
        +-- rag_embedding_saga 생성
        |
        +-- event_outbox 저장
                |
                v
KafkaOutboxPublisher
        |
        v
topic: realtyos.rag.embedding.requested
        |
        v
RagEmbeddingKafkaConsumer
consumer group: realtyos-rag-workers
        |
        v
RagEmbeddingBuildService
        |
        +-- success -> saga COMPLETED
        |
        +-- failure -> retry topic
                       |
                       +-- max attempts exceeded -> DLQ
```

## Outbox Pattern

임베딩 요청 API는 Kafka에 바로 publish하지 않는다.

먼저 DB transaction 안에서 다음 두 가지를 함께 저장한다.

- `rag_embedding_saga`
- `event_outbox`

이후 `KafkaOutboxPublisher`가 `event_outbox`의 publishable event를 읽어 Kafka로 발행한다. 이렇게 하면 API 처리 중 DB 저장은 성공했는데 Kafka 발행만 실패하는 상황에서도, outbox 재시도로 이벤트를 복구할 수 있다.

## Consumer Group

`RagEmbeddingKafkaConsumer`는 다음 group id로 동작한다.

```text
realtyos-rag-workers
```

임베딩 작업은 consumer group을 통해 여러 worker 인스턴스로 확장할 수 있다. 같은 topic을 여러 서버가 구독하더라도 Kafka partition 단위로 작업이 분배된다.

## Retry

임베딩 처리 중 OpenAI/Ollama API 장애, timeout, 저장 실패가 발생할 수 있다.

실패 시 consumer는 즉시 실패 처리하지 않고 retry topic으로 다시 보낸다.

```text
realtyos.rag.embedding.retry
```

event payload의 `attempt`를 증가시키고, 설정된 최대 횟수까지 재처리한다.

## DLQ

최대 retry 횟수를 초과하면 이벤트는 DLQ topic으로 이동한다.

```text
realtyos.rag.embedding.dlq
```

DLQ 이벤트는 자동 복구 대상이 아니라 운영자가 원인을 확인해야 하는 이벤트다. 현재 구현은 DLQ consume 시 saga를 `FAILED`로 마킹하고 로그를 남긴다.

## Saga

`rag_embedding_saga`는 하나의 임베딩 작업 요청 상태를 추적한다.

상태 예:

- `REQUESTED`
- `PROCESSING`
- `RETRYING`
- `COMPLETED`
- `FAILED`

이를 통해 API 요청 이후에도 작업이 처리 중인지, 완료됐는지, retry 중인지, DLQ로 이동했는지 추적할 수 있다.

상태 조회 API:

```text
GET /api/v1/rag/documents/embeddings/jobs/{sagaId}
GET /api/v1/rag/documents/embeddings/jobs
```

응답에는 상태, provider/model, attempt count, embedded/skipped/failed count, last error, 생성/수정/완료 시각이 포함된다.

## Configuration

기본값은 Kafka 비활성화다. 기존 로컬 실행과 테스트가 Kafka broker 없이도 동작해야 하기 때문이다.

Kafka를 사용할 때는 다음 값을 켠다.

```text
APP_KAFKA_ENABLED=true
APP_KAFKA_OUTBOX_ENABLED=true
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_CONSUMER_GROUP=realtyos-rag-workers
```

로컬 Kafka는 `docker-compose.yml`의 `kafka` 서비스로 실행할 수 있다.
