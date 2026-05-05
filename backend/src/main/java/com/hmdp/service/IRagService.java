package com.hmdp.service;

import com.hmdp.dto.RagChatRequest;
import com.hmdp.dto.RagChatResponse;
import com.hmdp.dto.RagDocumentDTO;
import com.hmdp.dto.RagIngestJobDTO;
import com.hmdp.dto.RagStatusDTO;

import java.util.List;

public interface IRagService {
    RagChatResponse chat(RagChatRequest request);

    RagIngestJobDTO rebuildIndex();

    RagStatusDTO status();

    List<RagDocumentDTO> listDocuments();

    RagIngestJobDTO latestJob();
}
