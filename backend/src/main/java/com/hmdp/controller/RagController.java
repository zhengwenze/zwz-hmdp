package com.hmdp.controller;

import com.hmdp.dto.RagChatRequest;
import com.hmdp.dto.RagChatResponse;
import com.hmdp.dto.RagDocumentDTO;
import com.hmdp.dto.RagIngestJobDTO;
import com.hmdp.dto.Result;
import com.hmdp.service.IRagService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/rag")
public class RagController {

    @Resource
    private IRagService ragService;

    @PostMapping("/chat")
    public Result chat(@Valid @RequestBody RagChatRequest request) {
        RagChatResponse response = ragService.chat(request);
        return Result.ok(response);
    }

    @PostMapping("/index/rebuild")
    public Result rebuildIndex() {
        RagIngestJobDTO job = ragService.rebuildIndex();
        return Result.ok(job);
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
