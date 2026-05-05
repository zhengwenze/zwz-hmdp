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
      "POST /rag/rebuild",
      { method: "POST", path: "/rag/rebuild" },
      options,
    );
  },
  fetchStatus(options = {}) {
    return apiRequest(
      "GET /rag/status",
      { method: "GET", path: "/rag/status" },
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
