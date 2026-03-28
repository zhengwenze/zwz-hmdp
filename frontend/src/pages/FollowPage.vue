<script setup>
import { reactive, ref } from "vue";
import {
  sharedState,
  send,
  isLoading,
  endpointBadgeClass,
} from "../stores/sharedState";
import { asBoolean } from "../api";

const forms = reactive({
  follow: {
    targetUserId: "1",
    checkUserId: "1",
    commonUserId: "1",
  },
});
const followStatus = ref(null);
const commonFollows = ref([]);

async function updateFollowState(value) {
  const boolValue = asBoolean(value);
  await send(
    "PUT /follow/{id}/{isFollow}",
    { method: "PUT", path: `/follow/${forms.follow.targetUserId}/${boolValue}` },
    { successMessage: boolValue ? "已发送关注请求。" : "已发送取关请求。" },
  );
}

async function checkFollowStatus() {
  await send(
    "GET /follow/or/not/{id}",
    { method: "GET", path: `/follow/or/not/${forms.follow.checkUserId}` },
    {
      successMessage: "关注状态已更新。",
      onSuccess: (data) => {
        followStatus.value = data;
      },
    },
  );
}

async function fetchCommonFollows() {
  await send(
    "GET /follow/common/{id}",
    { method: "GET", path: `/follow/common/${forms.follow.commonUserId}` },
    {
      successMessage: "共同关注列表已更新。",
      onSuccess: (data) => {
        commonFollows.value = Array.isArray(data) ? data : [];
      },
    },
  );
}
</script>

<template>
  <section id="follow" class="module-section">
    <div class="section-title">
      <div>
        <p class="eyebrow">Follow APIs</p>
        <h2>关注模块</h2>
      </div>
      <span class="section-hint">选择用户后可直接关注、查状态、查共同关注</span>
    </div>

    <div class="card-grid three">
      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>关注 / 取关</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['PUT /follow/{id}/{isFollow}'])">PUT /follow</span>
        </div>
        <div class="form-grid single">
          <label><span>目标用户 ID</span><input v-model="forms.follow.targetUserId" /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('PUT /follow/{id}/{isFollow}')" @click="updateFollowState(true)">关注</button>
          <button :disabled="isLoading('PUT /follow/{id}/{isFollow}')" class="secondary" @click="updateFollowState(false)">取关</button>
        </div>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>是否已关注</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /follow/or/not/{id}'])">/follow/or/not/{id}</span>
        </div>
        <div class="form-grid single">
          <label><span>目标用户 ID</span><input v-model="forms.follow.checkUserId" /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('GET /follow/or/not/{id}')" @click="checkFollowStatus">查询关注状态</button>
        </div>
        <div class="inline-stats">
          <div>
            <span class="label">结果</span>
            <strong>{{
              followStatus === null
                ? "--"
                : followStatus
                  ? "已关注"
                  : "未关注"
            }}</strong>
          </div>
        </div>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>共同关注</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /follow/common/{id}'])">/follow/common/{id}</span>
        </div>
        <div class="form-grid single">
          <label><span>对方用户 ID</span><input v-model="forms.follow.commonUserId" /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('GET /follow/common/{id}')" @click="fetchCommonFollows">查询共同关注</button>
        </div>
        <pre class="json-box small">{{ JSON.stringify(commonFollows, null, 2) }}</pre>
      </article>
    </div>
  </section>
</template>
