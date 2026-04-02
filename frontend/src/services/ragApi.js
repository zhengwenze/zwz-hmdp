import { apiRequest } from "./httpClient";

export const ragApi = {
  chat(payload, options = {}) {
    return apiRequest(
      "POST /rag/chat",
      { method: "POST", path: "/rag/chat", body: payload },
      options,
    );
  },
  rebuildIndex(options = {}) {
    return apiRequest(
      "POST /rag/index/rebuild",
      { method: "POST", path: "/rag/index/rebuild" },
      options,
    );
  },
  fetchDocuments(options = {}) {
    return apiRequest(
      "GET /rag/documents",
      { method: "GET", path: "/rag/documents" },
      options,
    );
  },
  fetchLatestJob(options = {}) {
    return apiRequest(
      "GET /rag/jobs/latest",
      { method: "GET", path: "/rag/jobs/latest" },
      options,
    );
  },
};
