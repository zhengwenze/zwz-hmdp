<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { ragApi } from "../services/ragApi";

const SESSION_STORAGE_KEY = "hmdp-rag-session-id";

const activeTab = ref("chat");
const sessionId = ref(readSessionId());
const question = ref("");
const sending = ref(false);
const rebuilding = ref(false);
const status = ref(null);
const latestJob = ref(null);
const documents = ref([]);
const conversations = ref([]);
let pollingTimer = null;

const readyCount = computed(
  () => documents.value.filter((item) => item.status === "READY").length,
);
const failedCount = computed(
  () => documents.value.filter((item) => item.status === "FAILED").length,
);
const running = computed(
  () => status.value?.rebuilding || latestJob.value?.status === "RUNNING",
);

function readSessionId() {
  const existing = localStorage.getItem(SESSION_STORAGE_KEY);
  if (existing) {
    return existing;
  }
  const generated = `rag-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  localStorage.setItem(SESSION_STORAGE_KEY, generated);
  return generated;
}

async function loadStatus() {
  const [statusResult, docsResult, jobResult] = await Promise.all([
    ragApi.fetchStatus({ silentError: true }),
    ragApi.fetchDocuments({ silentError: true }),
    ragApi.fetchLatestJob({ silentError: true }),
  ]);

  if (statusResult.success) {
    status.value = statusResult.data || null;
    latestJob.value = status.value?.latestJob || latestJob.value;
  }
  if (docsResult.success) {
    documents.value = docsResult.data || [];
  }
  if (jobResult.success) {
    latestJob.value = jobResult.data || null;
  }
}

function ensurePolling() {
  clearPolling();
  if (!running.value) {
    return;
  }
  pollingTimer = window.setInterval(loadStatus, 3000);
}

function clearPolling() {
  if (pollingTimer) {
    window.clearInterval(pollingTimer);
    pollingTimer = null;
  }
}

async function sendQuestion() {
  const value = question.value.trim();
  if (!value || sending.value) {
    return;
  }

  sending.value = true;
  const userQuestion = value;
  question.value = "";

  conversations.value.push({
    role: "user",
    content: userQuestion,
    references: [],
    citations: [],
    traceId: "",
  });

  const { data, success } = await ragApi.chat(
    { sessionId: sessionId.value, question: userQuestion },
    { silentError: false },
  );
  if (success && data) {
    conversations.value.push({
      role: "assistant",
      content: data.answer || "暂无回答",
      references: data.references || [],
      citations: data.citations || [],
      traceId: data.traceId || "",
    });
  }
  sending.value = false;
}

async function rebuildIndex() {
  if (rebuilding.value) {
    return;
  }
  rebuilding.value = true;
  const { success } = await ragApi.rebuildIndex({
    successMessage: "已触发知识库重建。",
  });
  rebuilding.value = false;
  if (success) {
    await loadStatus();
    ensurePolling();
  }
}

function prettyTime(value) {
  if (!value) {
    return "--";
  }
  return value.replace("T", " ");
}

onMounted(async () => {
  await loadStatus();
  ensurePolling();
});

onUnmounted(() => {
  clearPolling();
});
</script>

<template>
  <section class="app-page">
    <div class="stats-grid">
      <ElCard shadow="never" class="stat-card">
        <span class="stat-card__label">知识文档</span>
        <p class="stat-card__value">{{ status?.documentCount ?? documents.length }}</p>
        <p class="stat-card__desc">当前纳入索引管理的文档总数。</p>
      </ElCard>
      <ElCard shadow="never" class="stat-card">
        <span class="stat-card__label">可检索文档</span>
        <p class="stat-card__value">{{ status?.readyDocumentCount ?? readyCount }}</p>
        <p class="stat-card__desc">状态为 READY 的文档会参与问答召回。</p>
      </ElCard>
      <ElCard shadow="never" class="stat-card">
        <span class="stat-card__label">失败文档</span>
        <p class="stat-card__value">{{ status?.failedDocumentCount ?? failedCount }}</p>
        <p class="stat-card__desc">处理失败文档需要检查格式或内容是否为空。</p>
      </ElCard>
      <ElCard shadow="never" class="stat-card">
        <span class="stat-card__label">向量切片</span>
        <p class="stat-card__value">{{ status?.chunkCount ?? "--" }}</p>
        <p class="stat-card__desc">当前写入知识库索引的切片数量。</p>
      </ElCard>
    </div>

    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h2 class="page-panel__title">RAG 智能客服</h2>
            <p class="page-panel__hint">
              文档目录 <code>{{ status?.docsDir || "docs/rag" }}</code>，模型
              {{ status?.chatModel || "qwen2.5:7b" }} /
              {{ status?.embeddingModel || "qwen3-embedding:0.6b" }}。
            </p>
          </div>
          <div class="page-actions">
            <ElButton @click="loadStatus">刷新状态</ElButton>
            <ElButton type="primary" :loading="rebuilding" @click="rebuildIndex">
              重建索引
            </ElButton>
          </div>
        </div>
      </template>

      <ElTabs v-model="activeTab">
        <ElTabPane label="客服对话" name="chat">
          <div class="rag-grid">
            <div class="rag-chat">
              <div class="rag-chat__log">
                <ElEmpty
                  v-if="!conversations.length"
                  description="输入问题后，智能客服会仅基于文档知识库回答。"
                />
                <article
                  v-for="(item, index) in conversations"
                  :key="`${item.role}-${index}`"
                  :class="['rag-bubble', `rag-bubble--${item.role}`]"
                >
                  <div class="rag-bubble__meta">
                    <ElTag :type="item.role === 'assistant' ? 'primary' : 'info'" effect="plain">
                      {{ item.role === "assistant" ? "客服" : "用户" }}
                    </ElTag>
                    <span v-if="item.traceId" class="rag-bubble__trace">{{ item.traceId }}</span>
                  </div>
                  <p class="rag-bubble__content">{{ item.content }}</p>
                  <div v-if="item.references?.length" class="rag-references">
                    <div
                      v-for="reference in item.references"
                      :key="reference.chunkId"
                      class="rag-reference"
                    >
                      <div class="rag-reference__meta">
                        <strong>{{ reference.source }}</strong>
                        <span>score {{ Number(reference.score || 0).toFixed(3) }}</span>
                      </div>
                      <p>{{ reference.content }}</p>
                    </div>
                  </div>
                  <div v-else-if="item.citations?.length" class="rag-citations">
                    <ElTag v-for="citation in item.citations" :key="citation.chunkId" effect="plain" type="success">
                      {{ citation.fileName }}{{ citation.section ? ` / ${citation.section}` : "" }}
                    </ElTag>
                  </div>
                </article>
              </div>

              <div class="rag-chat__composer">
                <ElInput
                  v-model="question"
                  type="textarea"
                  :rows="4"
                  resize="none"
                  placeholder="输入一个基于知识库的问题，例如：如何重建知识库索引？"
                  @keyup.enter.ctrl="sendQuestion"
                />
                <div class="page-actions">
                  <ElButton @click="question = ''">清空</ElButton>
                  <ElButton type="primary" :loading="sending" @click="sendQuestion">
                    发送问题
                  </ElButton>
                </div>
              </div>
            </div>

            <ElCard shadow="never" class="rag-side">
              <template #header>
                <div class="page-panel__header">
                  <div>
                    <h3 class="page-panel__title">最近任务</h3>
                    <p class="page-panel__hint">用于观察当前索引构建状态。</p>
                  </div>
                </div>
              </template>

              <ElDescriptions :column="1" border>
                <ElDescriptionsItem label="状态">
                  {{ latestJob?.status || "--" }}
                </ElDescriptionsItem>
                <ElDescriptionsItem label="RAG 启用">
                  {{ status?.enabled ? "是" : "否" }}
                </ElDescriptionsItem>
                <ElDescriptionsItem label="向量维度">
                  {{ status?.embeddingDimension || "--" }}
                </ElDescriptionsItem>
                <ElDescriptionsItem label="开始时间">
                  {{ prettyTime(latestJob?.startedTime) }}
                </ElDescriptionsItem>
                <ElDescriptionsItem label="结束时间">
                  {{ prettyTime(latestJob?.finishedTime) }}
                </ElDescriptionsItem>
                <ElDescriptionsItem label="成功 / 失败">
                  {{ latestJob ? `${latestJob.successFiles} / ${latestJob.failedFiles}` : "--" }}
                </ElDescriptionsItem>
                <ElDescriptionsItem label="错误信息">
                  {{ latestJob?.errorMsg || "无" }}
                </ElDescriptionsItem>
              </ElDescriptions>
            </ElCard>
          </div>
        </ElTabPane>

        <ElTabPane label="知识库状态" name="docs">
          <ElTable :data="documents" stripe>
            <ElTableColumn prop="fileName" label="文件名" min-width="220" />
            <ElTableColumn prop="status" label="状态" width="140" />
            <ElTableColumn prop="chunkCount" label="切片数" width="120" />
            <ElTableColumn prop="updateTime" label="更新时间" min-width="180" />
            <ElTableColumn prop="errorMsg" label="错误信息" min-width="260" />
          </ElTable>
        </ElTabPane>
      </ElTabs>
    </ElCard>
  </section>
</template>

<style scoped>
.rag-grid {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1.7fr) minmax(280px, 0.9fr);
}

.rag-chat,
.rag-side {
  min-height: 560px;
}

.rag-chat {
  display: grid;
  gap: 16px;
  grid-template-rows: minmax(320px, 1fr) auto;
}

.rag-chat__log {
  display: grid;
  gap: 12px;
  align-content: start;
  padding: 8px 0;
  max-height: 560px;
  overflow: auto;
}

.rag-chat__composer {
  display: grid;
  gap: 12px;
}

.rag-bubble {
  display: grid;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--app-border);
  background: var(--app-surface-muted);
}

.rag-bubble--assistant {
  border-color: rgba(37, 99, 235, 0.18);
  background: rgba(37, 99, 235, 0.05);
}

.rag-bubble__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.rag-bubble__trace {
  font-size: 12px;
  color: var(--app-text-muted);
}

.rag-bubble__content {
  margin: 0;
  line-height: 1.8;
  color: var(--app-text-secondary);
  white-space: pre-wrap;
}

.rag-citations {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rag-references {
  display: grid;
  gap: 10px;
}

.rag-reference {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid rgba(22, 163, 74, 0.18);
  border-radius: 8px;
  background: rgba(22, 163, 74, 0.05);
}

.rag-reference__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: var(--app-text-muted);
}

.rag-reference p {
  margin: 0;
  line-height: 1.7;
  color: var(--app-text-secondary);
}

.rag-session {
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
}

@media (max-width: 960px) {
  .rag-grid {
    grid-template-columns: 1fr;
  }

  .rag-side {
    min-height: auto;
  }
}
</style>
