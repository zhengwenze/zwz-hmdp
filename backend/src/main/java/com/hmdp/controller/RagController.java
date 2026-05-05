package com.hmdp.controller;

import com.hmdp.dto.RagChatRequest;
import com.hmdp.dto.RagChatResponse;
import com.hmdp.dto.RagDocumentDTO;
import com.hmdp.dto.RagIngestJobDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.RagStatusDTO;
import com.hmdp.service.IRagService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/rag")
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagController {

    @Resource
    private IRagService ragService;

    @PostMapping("/chat")
    public Result chat(@Valid @RequestBody RagChatRequest request) {
        try {
            RagChatResponse response = ragService.chat(request);
            return Result.ok(response);
        } catch (RuntimeException e) {
            log.error("RAG chat failed: question={}", request.getQuestion(), e);
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/status")
    public Result status() {
        try {
            RagStatusDTO status = ragService.status();
            return Result.ok(status);
        } catch (RuntimeException e) {
            log.error("RAG status failed", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/rebuild")
    public Result rebuild() {
        return rebuildIndex();
    }

    @PostMapping("/index/rebuild")
    public Result rebuildIndex() {
        try {
            RagIngestJobDTO job = ragService.rebuildIndex();
            return Result.ok(job);
        } catch (RuntimeException e) {
            log.error("RAG rebuild submit failed", e);
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/documents")
    public Result documents() {
        List<RagDocumentDTO> documents = ragService.listDocuments();
        return Result.ok(documents);
    }

    @GetMapping("/jobs/latest")
    public Result latestJob() {
        return Result.ok(ragService.latestJob());
    }
}
