package com.hmdp.controller;

import com.hmdp.dto.RagChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/rag")
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "false", matchIfMissing = true)
public class RagStreamDisabledController {

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody(required = false) RagChatRequest request) {
        SseEmitter emitter = new SseEmitter(30_000L);
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("message", "RAG 功能未启用"), MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (IOException e) {
            log.warn("Failed to send disabled RAG stream error", e);
            emitter.completeWithError(e);
        }
        return emitter;
    }
}
