<script setup>
import { computed } from "vue";
import { endpointCatalog } from "../config/endpointCatalog";
import { sharedState, endpointBadgeClass } from "../stores/sharedState";

const coverageCount = computed(
  () =>
    endpointCatalog.filter((endpoint) => sharedState.touchedEndpoints[endpoint.key]).length,
);

const coverageRows = computed(() =>
  endpointCatalog.map((endpoint) => ({
    ...endpoint,
    done: Boolean(sharedState.touchedEndpoints[endpoint.key]),
  })),
);
</script>

<template>
  <section id="logs" class="module-section">
    <div class="section-title">
      <div>
        <p class="eyebrow">Coverage & Logs</p>
        <h2>接口覆盖矩阵与请求日志</h2>
      </div>
      <span class="section-hint">覆盖情况按"HTTP 方法 + 路径"统计</span>
    </div>

    <div class="card-grid two">
      <article class="panel dark-panel ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>最后一次响应</h3>
          <span class="ue-stamp">{{ sharedState.lastResponse.value?.status ?? "--" }}</span>
        </div>
        <div class="response-meta">
          <div>
            <span class="label">Endpoint</span>
            <strong>{{ sharedState.lastResponse.value?.endpointKey || "--" }}</strong>
          </div>
          <div>
            <span class="label">URL</span>
            <strong class="mono">{{ sharedState.lastResponse.value?.url || "--" }}</strong>
          </div>
          <div>
            <span class="label">耗时</span>
            <strong>{{ sharedState.lastResponse.value?.durationMs ?? "--" }} ms</strong>
          </div>
        </div>
        <pre class="json-box dark">{{
          sharedState.lastResponse.value
            ? JSON.stringify(sharedState.lastResponse.value.body, null, 2)
            : '{ "message": "尚无请求" }'
        }}</pre>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>接口覆盖矩阵</h3>
          <span class="ue-stamp">{{ coverageCount }} / {{ endpointCatalog.length }}</span>
        </div>
        <div class="coverage-grid">
          <div v-for="row in coverageRows" :key="row.key" class="coverage-item">
            <div>
              <strong>{{ row.key }}</strong>
              <p>{{ row.module }} · {{ row.label }}</p>
            </div>
            <span :class="endpointBadgeClass(row.done)">{{ row.done ? "已验证" : "待触发" }}</span>
          </div>
        </div>
      </article>

      <article class="panel dark-panel ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>最近请求日志</h3>
          <span class="status-pill muted">最多保留 40 条</span>
        </div>
        <div class="log-list">
          <article v-for="entry in sharedState.requestLogs.value" :key="entry.logId" class="log-item">
            <div class="log-top">
              <strong>{{ entry.endpointKey }}</strong>
              <span>{{ entry.displayTime }}</span>
            </div>
            <p>{{ entry.method }} {{ entry.path }}</p>
            <p>状态 {{ entry.status }} · {{ entry.durationMs }}ms</p>
          </article>
        </div>
      </article>
    </div>
  </section>
</template>
