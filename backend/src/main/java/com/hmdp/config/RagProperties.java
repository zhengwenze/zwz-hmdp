package com.hmdp.config;

import com.hmdp.utils.SystemConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag")
public class RagProperties {
    private boolean enabled = true;
    private String docsDir = SystemConstants.RAG_DOCS_DIR;
    private String ollamaBaseUrl = "http://localhost:11434";
    private String chatModel = "qwen2.5:7b";
    private String embeddingModel = "qwen3-embedding:0.6b";
    private int maxSegmentSize = 500;
    private int maxSegmentOverlap = 100;
    private int maxDenseResults = 8;
    private int maxKeywordResults = 8;
    private int maxContextSegments = 6;
    private int maxMemoryTurns = 6;
    private MilvusProperties milvus = new MilvusProperties();

    @Data
    public static class MilvusProperties {
        private String host = "127.0.0.1";
        private int port = 19530;
        private String collectionName = "kb_chunk";
    }
}
