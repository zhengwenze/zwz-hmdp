package com.hmdp.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagConfig {

    @Bean
    public ChatModel ragChatModel(RagProperties properties) {
        return OllamaChatModel.builder()
                .baseUrl(properties.getOllamaBaseUrl())
                .modelName(properties.getChatModel())
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    @Bean
    public EmbeddingModel ragEmbeddingModel(RagProperties properties) {
        return OllamaEmbeddingModel.builder()
                .baseUrl(properties.getOllamaBaseUrl())
                .modelName(properties.getEmbeddingModel())
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    @Bean
    public MilvusEmbeddingStore ragEmbeddingStore(RagProperties properties, EmbeddingModel ragEmbeddingModel) {
        return MilvusEmbeddingStore.builder()
                .host(properties.getMilvus().getHost())
                .port(properties.getMilvus().getPort())
                .collectionName(properties.getMilvus().getCollectionName())
                .dimension(ragEmbeddingModel.dimension())
                .autoFlushOnInsert(true)
                .build();
    }
}
