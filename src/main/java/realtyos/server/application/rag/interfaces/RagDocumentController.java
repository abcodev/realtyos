package realtyos.server.application.rag.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import realtyos.server.application.common.response.ApiResponse;
import realtyos.server.application.rag.application.RagDocumentBuildService;
import realtyos.server.application.rag.application.RagEmbeddingAsyncService;
import realtyos.server.application.rag.application.RagEmbeddingBuildService;
import realtyos.server.application.rag.application.RagEmbeddingSagaService;
import realtyos.server.application.rag.application.RagIndexStatsService;
import realtyos.server.application.rag.application.RagSyncService;
import realtyos.server.application.rag.interfaces.dto.RagAsyncJobResponse;
import realtyos.server.application.rag.interfaces.dto.RagDocumentBuildResponse;
import realtyos.server.application.rag.interfaces.dto.RagEmbeddingBuildResponse;
import realtyos.server.application.rag.interfaces.dto.RagEmbeddingJobStatusResponse;
import realtyos.server.application.rag.interfaces.dto.RagIndexStatsResponse;
import realtyos.server.application.rag.interfaces.dto.RagSyncResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rag/documents")
@Tag(name = "RAG Documents", description = "RAG 문서 생성 API")
public class RagDocumentController {

    private final RagDocumentBuildService buildService;
    private final RagEmbeddingBuildService embeddingBuildService;
    private final RagEmbeddingAsyncService embeddingAsyncService;
    private final RagEmbeddingSagaService embeddingSagaService;
    private final RagSyncService syncService;
    private final RagIndexStatsService indexStatsService;

    @PostMapping("/deals")
    @Operation(summary = "실거래가 RAG 문서 생성/갱신", description = "real_estate_deals 데이터를 rag_document 문서로 변환해 upsert합니다. 내용이 바뀌면 기존 embedding을 무효화합니다. limit이 0 이하이면 전체를 처리합니다.")
    public ApiResponse<RagDocumentBuildResponse> buildDealDocuments(
            @RequestParam(defaultValue = "1000") int limit) {
        return ApiResponse.success(RagDocumentBuildResponse.from(buildService.buildDealDocuments(limit)));
    }

    @PostMapping("/embeddings")
    @Operation(summary = "RAG 문서 임베딩 생성", description = "아직 embedding이 없는 rag_document 문서를 선택한 provider/model embedding으로 변환해 rag_embedding에 저장합니다. limit이 0 이하이면 전체를 처리합니다.")
    public ApiResponse<RagEmbeddingBuildResponse> buildDocumentEmbeddings(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model) {
        return ApiResponse.success(RagEmbeddingBuildResponse.from(
                embeddingBuildService.buildDocumentEmbeddings(limit, provider, model)));
    }

    @PostMapping("/embeddings/async")
    @Operation(summary = "Kafka 기반 RAG 문서 임베딩 작업 요청", description = "임베딩 작업을 outbox에 저장하고 Kafka consumer group이 비동기로 처리하도록 요청합니다.")
    public ApiResponse<RagAsyncJobResponse> requestDocumentEmbeddings(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model) {
        return ApiResponse.success(RagAsyncJobResponse.from(
                embeddingAsyncService.requestEmbeddingBuild(limit, provider, model)));
    }

    @GetMapping("/embeddings/jobs/{sagaId}")
    @Operation(summary = "RAG 임베딩 작업 상태 조회", description = "Kafka 기반 비동기 임베딩 작업의 saga 상태를 조회합니다.")
    public ApiResponse<RagEmbeddingJobStatusResponse> getEmbeddingJobStatus(
            @PathVariable UUID sagaId) {
        return ApiResponse.success(RagEmbeddingJobStatusResponse.from(
                embeddingSagaService.getJobStatus(sagaId)));
    }

    @GetMapping("/embeddings/jobs")
    @Operation(summary = "RAG 임베딩 작업 목록 조회", description = "최근 Kafka 기반 비동기 임베딩 작업 상태 목록을 조회합니다.")
    public ApiResponse<List<RagEmbeddingJobStatusResponse>> getEmbeddingJobStatuses(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(embeddingSagaService.findJobStatuses(limit).stream()
                .map(RagEmbeddingJobStatusResponse::from)
                .toList());
    }

    @PostMapping("/sync")
    @Operation(summary = "실거래가 RAG 문서/임베딩 동기화", description = "실거래가 RAG 문서를 upsert하고, 선택한 provider/model 기준으로 누락된 embedding을 생성합니다.")
    public ApiResponse<RagSyncResponse> syncDealDocumentsAndEmbeddings(
            @RequestParam(defaultValue = "1000") int documentLimit,
            @RequestParam(defaultValue = "1000") int embeddingLimit,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model) {
        return ApiResponse.success(RagSyncResponse.from(
                syncService.syncDealDocumentsAndEmbeddings(documentLimit, embeddingLimit, provider, model)));
    }

    @GetMapping("/stats")
    @Operation(summary = "RAG 인덱스 상태 조회", description = "전체 RAG 문서 수, 선택한 embedding provider/model 기준 누락 수, provider/model별 embedding 개수를 조회합니다.")
    public ApiResponse<RagIndexStatsResponse> getIndexStats(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model) {
        return ApiResponse.success(RagIndexStatsResponse.from(indexStatsService.getStats(provider, model)));
    }
}
