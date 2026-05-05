package com.hmdp.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagConfig {

    @Bean
    public ChatLanguageModel ragChatModel(RagProperties properties) {
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
}
