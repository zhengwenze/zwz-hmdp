package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagStatusDTO {
    private Boolean enabled;
    private String docsDir;
    private Boolean rebuilding;
    private String chatModel;
    private String embeddingModel;
    private Integer embeddingDimension;
    private RagIngestJobDTO latestJob;
    private Long documentCount;
    private Long chunkCount;
    private Long readyDocumentCount;
    private Long failedDocumentCount;
    private List<String> supportedFormats;
}
