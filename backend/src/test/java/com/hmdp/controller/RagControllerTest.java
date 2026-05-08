package com.hmdp.controller;

import com.hmdp.dto.RagChatRequest;
import com.hmdp.dto.RagChatResponse;
import com.hmdp.dto.RagDocumentDTO;
import com.hmdp.dto.RagIngestJobDTO;
import com.hmdp.dto.RagStatusDTO;
import com.hmdp.service.IRagService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagController.class)
@ContextConfiguration(classes = RagController.class)
@TestPropertySource(properties = "rag.enabled=true")
class RagControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private IRagService ragService;

        @Test
        void chat_shouldBindRequestBodyAndReturnResponse() throws Exception {
                RagChatResponse response = new RagChatResponse("回答", List.of(), List.of(), true, "trace-1");
                when(ragService.chat(any(RagChatRequest.class))).thenReturn(response);

                mockMvc.perform(post("/rag/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"sessionId\":\"s1\",\"question\":\"附近有什么推荐？\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.answer").value("回答"))
                                .andExpect(jsonPath("$.data.grounded").value(true))
                                .andExpect(jsonPath("$.data.traceId").value("trace-1"));

                ArgumentCaptor<RagChatRequest> captor = ArgumentCaptor.forClass(RagChatRequest.class);
                verify(ragService).chat(captor.capture());
                assertEquals("s1", captor.getValue().getSessionId());
                assertEquals("附近有什么推荐？", captor.getValue().getQuestion());
        }

        @Test
        void chat_shouldReturnFailWhenServiceThrowsRuntimeException() throws Exception {
                when(ragService.chat(any(RagChatRequest.class))).thenThrow(new RuntimeException("模型不可用"));

                mockMvc.perform(post("/rag/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"question\":\"问题\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errorMsg").value("模型不可用"));
        }

        @Test
        void status_shouldReturnServiceResult() throws Exception {
                RagStatusDTO status = new RagStatusDTO();
                status.setEnabled(true);
                status.setDocsDir("/docs");
                status.setDocumentCount(2L);
                when(ragService.status()).thenReturn(status);

                mockMvc.perform(get("/rag/status"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.enabled").value(true))
                                .andExpect(jsonPath("$.data.docsDir").value("/docs"))
                                .andExpect(jsonPath("$.data.documentCount").value(2));

                verify(ragService).status();
        }

        @Test
        void rebuild_shouldReturnSubmittedJob() throws Exception {
                RagIngestJobDTO job = new RagIngestJobDTO(1L, "RUNNING", 3, 1, 0,
                                LocalDateTime.now(), null, null);
                when(ragService.rebuildIndex()).thenReturn(job);

                mockMvc.perform(post("/rag/rebuild"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.id").value(1))
                                .andExpect(jsonPath("$.data.status").value("RUNNING"));

                verify(ragService).rebuildIndex();
        }

        @Test
        void rebuildIndex_shouldReturnSubmittedJob() throws Exception {
                RagIngestJobDTO job = new RagIngestJobDTO(2L, "RUNNING", 4, 1, 0,
                                LocalDateTime.now(), null, null);
                when(ragService.rebuildIndex()).thenReturn(job);

                mockMvc.perform(post("/rag/index/rebuild"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.id").value(2))
                                .andExpect(jsonPath("$.data.status").value("RUNNING"));

                verify(ragService).rebuildIndex();
        }

        @Test
        void documents_shouldReturnDocumentList() throws Exception {
                RagDocumentDTO document = new RagDocumentDTO(1L, "faq.md", "/docs/faq.md", "READY",
                                3, null, LocalDateTime.now());
                when(ragService.listDocuments()).thenReturn(List.of(document));

                mockMvc.perform(get("/rag/documents"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data[0].id").value(1))
                                .andExpect(jsonPath("$.data[0].fileName").value("faq.md"))
                                .andExpect(jsonPath("$.data[0].status").value("READY"));

                verify(ragService).listDocuments();
        }

        @Test
        void latestJob_shouldReturnLatestJob() throws Exception {
                RagIngestJobDTO job = new RagIngestJobDTO(3L, "SUCCESS", 4, 4, 0,
                                LocalDateTime.now(), LocalDateTime.now(), null);
                when(ragService.latestJob()).thenReturn(job);

                mockMvc.perform(get("/rag/jobs/latest"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.id").value(3))
                                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

                verify(ragService).latestJob();
        }
}
