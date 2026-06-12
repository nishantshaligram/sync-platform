package com.syncplatform.sync_core_service.controller;

import com.syncplatform.sync_core_service.document.SyncRunDocument;
import com.syncplatform.sync_core_service.dto.SyncRunSearchResult;
import com.syncplatform.sync_core_service.repository.SyncConnectionRepository;
import com.syncplatform.sync_core_service.repository.SyncRunDocumentRepository;
import com.syncplatform.sync_core_service.repository.SyncRunRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/connections")
@RequiredArgsConstructor
public class SyncHistoryController {

    private final SyncRunDocumentRepository syncRunDocumentRepository;
    private final SyncConnectionRepository syncConnectionRepository;
    private final SyncRunRepository syncRunRepository;

    @GetMapping("/{id}/sync-history/search")
    public ResponseEntity<List<SyncRunSearchResult>> search(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = UUID.fromString(jwt.getSubject());

        // Verify ownership
        syncConnectionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found"));

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("startedAt").descending());

        Page<SyncRunDocument> results;
        if (status != null && !status.isBlank()) {
            results = syncRunDocumentRepository
                    .findBySyncConnectionIdAndStatus(id.toString(), status, pageable);
        } else {
            results = syncRunDocumentRepository
                    .findBySyncConnectionId(id.toString(), pageable);
        }

        List<SyncRunSearchResult> response = results.getContent().stream()
                .map(doc -> SyncRunSearchResult.builder()
                        .runId(doc.getRunId())
                        .triggerType(doc.getTriggerType())
                        .status(doc.getStatus())
                        .errorCategory(doc.getErrorCategory())
                        .startedAt(doc.getStartedAt())
                        .completedAt(doc.getCompletedAt())
                        .durationMs(doc.getDurationMs())
                        .eventsProcessed(doc.getEventsProcessed())
                        .eventsFailed(doc.getEventsFailed())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/sync-history")
    public ResponseEntity<Page<com.syncplatform.sync_core_service.entity.SyncRun>> history(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = UUID.fromString(jwt.getSubject());

        syncConnectionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found"));

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("startedAt").descending());

        return ResponseEntity.ok(syncRunRepository
                .findBySyncConnectionIdOrderByStartedAtDesc(id, pageable));
    }
}