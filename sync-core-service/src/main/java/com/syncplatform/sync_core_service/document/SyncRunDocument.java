package com.syncplatform.sync_core_service.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.OffsetDateTime;

@Document(indexName = "sync_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncRunDocument {

    @Id
    private String runId;

    @Field(type = FieldType.Keyword)
    private String syncConnectionId;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String triggerType;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String errorCategory;

    @Field(type = FieldType.Date)
    private OffsetDateTime startedAt;

    @Field(type = FieldType.Date)
    private OffsetDateTime completedAt;

    @Field(type = FieldType.Long)
    private long durationMs;

    @Field(type = FieldType.Integer)
    private int eventsProcessed;

    @Field(type = FieldType.Integer)
    private int eventsFailed;

    @Field(type = FieldType.Text)
    private String connectionName;
}