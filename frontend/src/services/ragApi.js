import { apiRequest } from "./httpClient";
import { appState, NOTICE_MESSAGES, setNotice } from "../stores/appState";
import { sessionState } from "../stores/session";

function trimSlash(value) {
  return value.replace(/\/+$/, "");
}

function buildUrl(path) {
  const baseUrl = appState.apiBaseUrl.value.trim();
  if (!baseUrl) {
    return path;
  }
  return `${trimSlash(baseUrl)}${path}`;
}

function parseSseBlock(block) {
  const event = {
    event: "message",
    data: "",
  };

  block.split(/\r?\n/).forEach((line) => {
    if (!line || line.startsWith(":")) {
      return;
    }
    const separatorIndex = line.indexOf(":");
    const field = separatorIndex === -1 ? line : line.slice(0, separatorIndex);
    let value = separatorIndex === -1 ? "" : line.slice(separatorIndex + 1);
    if (value.startsWith(" ")) {
      value = value.slice(1);
    }
    if (field === "event") {
      event.event = value || "message";
    }
    if (field === "data") {
      event.data = event.data ? `${event.data}\n${value}` : value;
    }
  });

  return event;
}

function parseSseData(rawData) {
  if (!rawData) {
    return null;
  }
  try {
    return JSON.parse(rawData);
  } catch {
    return rawData;
  }
}

function dispatchSseEvent(event, handlers) {
  const data = parseSseData(event.data);
  if (event.event === "meta") {
    handlers.onMeta?.(data);
  } else if (event.event === "delta") {
    handlers.onDelta?.(data);
  } else if (event.event === "done") {
    handlers.onDone?.(data);
  } else if (event.event === "error") {
    handlers.onError?.(data);
  } else {
    handlers.onEvent?.(event.event, data);
  }
}

async function readSseStream(response, handlers = {}) {
  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error("当前浏览器不支持流式响应读取");
  }

  const decoder = new TextDecoder("utf-8");
  let buffer = "";
  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true });
    const blocks = buffer.split(/\r?\n\r?\n/);
    buffer = blocks.pop() || "";
    blocks.forEach((block) => {
      if (block.trim()) {
        dispatchSseEvent(parseSseBlock(block), handlers);
      }
    });
  }

  buffer += decoder.decode();
  if (buffer.trim()) {
    dispatchSseEvent(parseSseBlock(buffer), handlers);
  }
}

export const ragApi = {
  chat(payload, options = {}) {
    return apiRequest(
      "POST /rag/chat",
      { method: "POST", path: "/rag/chat", body: payload },
      options,
    );
  },
  async chatStream(payload, handlers = {}, options = {}) {
    const headers = new Headers({
      Accept: "text/event-stream",
      "Content-Type": "application/json",
    });
    const token = sessionState.token.value.trim();
    if (token) {
      headers.set("authorization", token);
    }

    try {
      const response = await fetch(buildUrl("/rag/chat/stream"), {
        method: "POST",
        headers,
        body: JSON.stringify(payload),
      });
      const contentType = response.headers.get("content-type") || "";
      if (!response.ok || !contentType.includes("text/event-stream")) {
        const text = await response.text();
        throw new Error(text || "RAG 流式问答请求失败");
      }
      await readSseStream(response, handlers);
      return { success: true };
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      if (!options.silentError) {
        setNotice("error", options.errorMessage || message || NOTICE_MESSAGES.operationFailed);
      }
      handlers.onError?.({ message });
      return { success: false, error: message };
    }
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
