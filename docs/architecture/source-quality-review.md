# Source Quality Review

## 이번 정리에서 바로 수정한 내용

### 1. JPA Entity의 JSON 직렬화 책임 제거

기존 `UserAiMemoryEventJpaEntity`가 `ObjectMapper`를 직접 생성해서 `sources_json`, `decision_json`을 직렬화/역직렬화했다.

문제:

- Entity가 DB 매핑 외의 변환 책임을 가짐
- Spring이 관리하는 `ObjectMapper` 설정을 우회함
- JSON 역직렬화 실패를 Entity 내부에서 조용히 삼켜 디버깅이 어려움

수정:

- `UserAiMemoryEventJpaEntity`는 DB 컬럼 매핑만 담당
- JSON 변환은 `UserAiMemoryRepositoryJpaAdaptor`에서 처리
- Spring Bean `ObjectMapper`를 주입받아 사용

관련 파일:

- `src/main/java/realtyos/server/application/rag/infrastructure/jpa/entity/UserAiMemoryEventJpaEntity.java`
- `src/main/java/realtyos/server/application/rag/infrastructure/jpa/UserAiMemoryRepositoryJpaAdaptor.java`

### 2. 모델명 조합 로직 중복 제거

`RagAnswerService`와 `RagAnswerStreamingService`에 동일한 `provider:model` 조합 로직이 중복돼 있었다.

수정:

- `RagModelName`으로 application 내부 공통 유틸 분리

관련 파일:

- `src/main/java/realtyos/server/application/rag/application/RagModelName.java`
- `src/main/java/realtyos/server/application/rag/application/RagAnswerService.java`
- `src/main/java/realtyos/server/application/rag/application/RagAnswerStreamingService.java`

### 3. OAuth 임시 코드 저장소의 DTO 의존 제거

기존 `OAuthCodeRepository`가 interface DTO인 `TokenResponse`를 직접 사용했다.

문제:

- domain 계층이 interfaces 계층에 의존
- API 응답 DTO 변경이 저장소 계약에 영향을 줄 수 있음

수정:

- `OAuthCodeRepository`는 domain 타입 `AuthToken`만 사용
- `LoginController`에서 `TokenResponse.from(AuthToken)`으로 API DTO 변환
- `OAuthCodeRedisAdaptor`도 DTO 의존 제거

관련 파일:

- `src/main/java/realtyos/server/application/auth/domain/OAuthCodeRepository.java`
- `src/main/java/realtyos/server/application/auth/infrastructure/redis/OAuthCodeRedisAdaptor.java`
- `src/main/java/realtyos/server/application/auth/interfaces/LoginController.java`
- `src/main/java/realtyos/server/application/auth/interfaces/dto/TokenResponse.java`

### 4. UserAuthService의 OAuth infrastructure 직접 의존 제거

기존 `UserAuthService`가 `KakaoOAuthClient`, `GoogleOAuthClient` 구현체를 직접 주입받았다.

문제:

- application 계층이 infrastructure 구현체를 직접 의존
- provider가 늘어날 때 application service 변경이 커짐

수정:

- domain port `OAuthUserClient` 추가
- domain profile `OAuthUserProfile` 추가
- Kakao/Google OAuth client는 `OAuthUserClient` 구현체로 변경
- `UserAuthService`는 `List<OAuthUserClient>`만 주입받아 provider별 client를 선택
- 기존 `interfaces.dto.OAuthUserInfo` 제거

관련 파일:

- `src/main/java/realtyos/server/application/auth/domain/OAuthUserClient.java`
- `src/main/java/realtyos/server/application/auth/domain/OAuthUserProfile.java`
- `src/main/java/realtyos/server/application/auth/application/UserAuthService.java`
- `src/main/java/realtyos/server/application/auth/infrastructure/oauth/KakaoOAuthClient.java`
- `src/main/java/realtyos/server/application/auth/infrastructure/oauth/GoogleOAuthClient.java`

### 5. UserAuthService의 API DTO 의존 제거

기존 `UserAuthService`가 `LoginRequest`, `LoginResponse`, `UserRegisterRequest`를 직접 사용했다.

문제:

- application use case와 HTTP API DTO가 결합됨
- controller 응답 구조 변경이 application service에 영향을 줄 수 있음

수정:

- application command/result 타입 추가
  - `AuthLoginCommand`
  - `AuthSignupCommand`
  - `AuthLoginResult`
- controller DTO는 `toCommand()`와 `from()`으로 변환만 담당
- OAuth2 security principal도 `LoginResponse` 대신 `AuthLoginResult`를 보관

관련 파일:

- `src/main/java/realtyos/server/application/auth/application/AuthLoginCommand.java`
- `src/main/java/realtyos/server/application/auth/application/AuthSignupCommand.java`
- `src/main/java/realtyos/server/application/auth/application/AuthLoginResult.java`
- `src/main/java/realtyos/server/application/auth/application/UserAuthService.java`
- `src/main/java/realtyos/server/application/auth/interfaces/LoginController.java`

### 6. SggCodeSyncService의 VWorld infrastructure 의존 제거

기존 `SggCodeSyncService`가 `VworldExternalApiClient`와 `VworldApiResponse`를 직접 참조했다.

문제:

- application service가 외부 API DTO 구조를 직접 알고 있음
- VWorld 응답 구조 변경이 application service로 전파됨

수정:

- domain port `SggCodeClient` 추가
- domain page `SggCodePage` 추가
- `VworldExternalApiClient`가 VWorld DTO를 domain page로 변환
- `SggCodeSyncService`는 `SggCodeClient`만 의존

관련 파일:

- `src/main/java/realtyos/server/application/realestate/domain/SggCodeClient.java`
- `src/main/java/realtyos/server/application/realestate/domain/SggCodePage.java`
- `src/main/java/realtyos/server/application/realestate/application/service/SggCodeSyncService.java`
- `src/main/java/realtyos/server/application/realestate/infrastructure/client/VworldExternalApiClient.java`

### 7. 지역 해석 로직 집중화

기존에는 지역 문자열 해석과 SQL 필터링 규칙이 RAG 검색, 실거래 후보 검색, 의사결정 검색에 분산돼 있었다.

문제:

- `마포`, `마포구`, `마포동` 같은 입력을 각 서비스가 다르게 해석할 수 있음
- 전국 지역을 코드에 alias로 하드코딩하는 방식은 유지보수가 불가능함
- RAG 검색과 의사결정 검색에서 같은 지역 버그가 반복될 수 있음

수정:

- domain port `RegionResolver` 추가
- `RegionResolution`, `RegionResolutionType`으로 해석 결과를 구조화
- `JdbcRegionResolver`가 법정동/시군구 코드 테이블을 기준으로 지역명을 해석
- `DealAnalysisService`와 `RagDocumentRepositoryJpaAdaptor`는 resolver 결과에 따라 필터만 조립

관련 파일:

- `src/main/java/realtyos/server/application/realestate/domain/RegionResolver.java`
- `src/main/java/realtyos/server/application/realestate/domain/RegionResolution.java`
- `src/main/java/realtyos/server/application/realestate/domain/RegionResolutionType.java`
- `src/main/java/realtyos/server/application/realestate/infrastructure/jpa/JdbcRegionResolver.java`
- `src/main/java/realtyos/server/application/realestate/application/service/DealAnalysisService.java`
- `src/main/java/realtyos/server/application/rag/infrastructure/jpa/RagDocumentRepositoryJpaAdaptor.java`

### 8. 의사결정 답변 포맷/요약 책임 분리

기존 `RealestateDecisionService`가 후보 검색 orchestration, 점수 적용, 대상별 요약 계산, 답변 문자열 생성을 모두 담당했다.

문제:

- 비교/추천 답변 포맷을 바꾸려면 핵심 의사결정 흐름까지 함께 읽어야 함
- 대상별 통계 계산을 별도로 테스트하거나 재사용하기 어려움
- 서비스가 커질수록 후보 검색 버그와 출력 버그의 변경 범위가 섞임

수정:

- `DecisionResultFormatter`를 추가해 markdown 답변 생성 책임 분리
- `DecisionTargetSummaryBuilder`를 추가해 비교 대상별 평균가/중위가/거래량/최근 변화 계산 분리
- `RealestateDecisionService`는 질문 해석, 후보 조회, 점수화 흐름에 집중

관련 파일:

- `src/main/java/realtyos/server/application/realestate/application/service/DecisionResultFormatter.java`
- `src/main/java/realtyos/server/application/realestate/application/service/DecisionTargetSummaryBuilder.java`
- `src/main/java/realtyos/server/application/realestate/application/service/RealestateDecisionService.java`

### 9. RAG 단일 API 내부 라우팅 분리

기존 `/api/v1/rag/ask` 흐름은 `RagAnswerService`와 `RagAnswerStreamingService`가 직접 `RealestateDecisionService.supports()`를 호출해 의사결정 엔진 사용 여부를 판단했다.

문제:

- RAG application service가 의사결정 서비스의 지원 여부 메서드에 직접 의존
- 시세/비교/추천/단순 검색 의도가 구조화되지 않아 답변 품질 문제를 추적하기 어려움
- non-stream과 stream 흐름의 분기 기준이 서비스 내부에 흩어짐

수정:

- API는 `/api/v1/rag/ask`, `/api/v1/rag/ask/stream` 그대로 유지
- application 내부에 `RagAnswerRouter` 추가
- 질문을 `COMPARISON`, `RECOMMENDATION`, `MARKET_PRICE`, `SEARCH`로 분류
- `SEARCH`만 일반 RAG 검색/LLM 답변 흐름을 타고, 나머지는 의사결정 엔진으로 라우팅
- SSE에는 `route_selected` 이벤트를 추가해 화면/로그에서 내부 분기를 확인 가능하게 함

관련 파일:

- `src/main/java/realtyos/server/application/rag/application/RagAnswerRouter.java`
- `src/main/java/realtyos/server/application/rag/application/RagAnswerRoute.java`
- `src/main/java/realtyos/server/application/rag/application/RagAnswerRouteType.java`
- `src/main/java/realtyos/server/application/rag/application/RagAnswerService.java`
- `src/main/java/realtyos/server/application/rag/application/RagAnswerStreamingService.java`
- `src/test/java/realtyos/server/application/rag/application/RagAnswerRouterTest.java`

### 10. 자연어 질문 해석 parser 통합

기존에는 질문 의도와 조건 추출이 `RagAnswerRouter`, `RagQueryRewritePolicy`, `RealestateDecisionService`에 나뉘어 있었다.

문제:

- 같은 질문을 route, 검색 조건, 의사결정 비교 대상 추출에서 다르게 해석할 수 있음
- `살 수 있는`, `후보를 비교`, `단지명 vs 지역명` 같은 표현을 고칠 때 여러 파일을 동시에 수정해야 함
- 자연어 회귀 테스트가 route/condition/decision 흐름별로 흩어짐

수정:

- `RealestateQueryParser` 추가
- `ParsedRealestateQuery`로 intent, reason, condition, comparison target을 구조화
- `RagAnswerRouter`는 parser의 intent만 route로 변환
- `RagQueryRewritePolicy`는 parser의 condition을 검색 조건으로 사용
- `RealestateDecisionService`는 parser의 comparison target을 후보 검색에 사용

관련 파일:

- `src/main/java/realtyos/server/application/rag/domain/RealestateQueryParser.java`
- `src/main/java/realtyos/server/application/rag/domain/ParsedRealestateQuery.java`
- `src/main/java/realtyos/server/application/rag/domain/QueryIntent.java`
- `src/main/java/realtyos/server/application/rag/domain/QueryTarget.java`
- `src/main/java/realtyos/server/application/rag/domain/QueryTargetKind.java`
- `src/test/java/realtyos/server/application/rag/domain/RealestateQueryParserTest.java`

### 11. Kafka 기반 RAG 임베딩 비동기 처리

기존 임베딩 생성은 API 요청 또는 스케줄러에서 동기적으로 실행됐다.

문제:

- 임베딩 생성은 오래 걸리고 OpenAI/Ollama 장애 영향을 받음
- API 요청 중 DB 변경과 외부 메시지 발행을 함께 다루기 어려움
- 실패한 임베딩 작업의 retry/DLQ 추적 구조가 없음

수정:

- `event_outbox` 테이블과 `OutboxEventService` 추가
- `rag_embedding_saga` 테이블로 임베딩 작업 상태 추적
- `/api/v1/rag/documents/embeddings/async` endpoint 추가
- Kafka 임베딩 saga 상태 단건/목록 조회 endpoint 추가
- `KafkaOutboxPublisher`가 outbox event를 Kafka topic으로 발행
- `RagEmbeddingKafkaConsumer`가 consumer group으로 임베딩 작업 처리
- 실패 시 retry topic으로 재발행, 최대 횟수 초과 시 DLQ topic으로 이동

관련 파일:

- `src/main/java/realtyos/server/application/common/outbox/OutboxEventService.java`
- `src/main/java/realtyos/server/application/common/outbox/OutboxRepository.java`
- `src/main/java/realtyos/server/application/rag/application/RagEmbeddingAsyncService.java`
- `src/main/java/realtyos/server/application/rag/application/RagEmbeddingSagaService.java`
- `src/main/java/realtyos/server/application/rag/domain/RagEmbeddingJobStatus.java`
- `src/main/java/realtyos/server/application/rag/interfaces/dto/RagEmbeddingJobStatusResponse.java`
- `src/main/java/realtyos/server/application/rag/infrastructure/kafka/KafkaOutboxPublisher.java`
- `src/main/java/realtyos/server/application/rag/infrastructure/kafka/RagEmbeddingKafkaConsumer.java`
- `src/main/resources/db/migration/V16__create_event_outbox_and_rag_embedding_saga.sql`

## 아직 남아있는 아키텍처 이슈

### 1. 의사결정 서비스에 후보 검색 orchestration이 아직 남아 있음

`RealestateDecisionService`가 현재 맡는 책임:

- 후보 검색 orchestration
- 점수 적용

권장 방향:

- `DecisionCandidateFinder`

처럼 역할을 나누면 테스트와 변경이 쉬워진다.

### 2. SQL fragment 중복

`DealAnalysisService`와 `RagDocumentRepositoryJpaAdaptor`가 `RegionResolver` 결과를 사용하도록 정리됐지만 SQL 조립 방식은 아직 각 repository/service에 남아 있다.

권장 방향:

- `RegionSqlFilterBuilder` 같은 infrastructure 내부 helper 도입
- 또는 resolver 결과를 받아 단순한 `sgg_code IN (...)`, `umd_name IN (...)` 조건만 조립

## 권장 다음 작업 순서

1. `DecisionCandidateFinder` 분리
2. RAG/Decision SQL fragment 중복 제거
3. formatter/summary builder 단위 테스트 추가
