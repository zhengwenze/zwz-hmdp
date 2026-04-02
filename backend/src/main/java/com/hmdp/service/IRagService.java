package com.hmdp.service;

import com.hmdp.dto.RagChatRequest;
import com.hmdp.dto.RagChatResponse;
import com.hmdp.dto.RagDocumentDTO;
import com.hmdp.dto.RagIngestJobDTO;

import java.util.List;

public interface IRagService {
    RagChatResponse chat(RagChatRequest request);

    RagIngestJobDTO rebuildIndex();

    List<RagDocumentDTO> listDocuments();

    RagIngestJobDTO latestJob();
}
