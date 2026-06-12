package com.syncplatform.sync_core_service.repository;

import com.syncplatform.sync_core_service.document.SyncRunDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SyncRunDocumentRepository extends ElasticsearchRepository<SyncRunDocument, String> {

    Page<SyncRunDocument> findBySyncConnectionId(String syncConnectionId, Pageable pageable);

    Page<SyncRunDocument> findBySyncConnectionIdAndStatus(
            String syncConnectionId, String status, Pageable pageable);
}