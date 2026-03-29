<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import { useRoute, useRouter } from "vue-router";
import { shopFlowState } from "../stores/shopFlow";
import { homeFeedState } from "../stores/homeFeed";
import { shopApi } from "../services/shopApi";
import { isLoading } from "../stores/labState";
import { setNotice } from "../stores/appState";
import ShopPreviewCard from "../components/ShopPreviewCard.vue";

const route = useRoute();
const router = useRouter();
const loadMoreRef = ref(null);
let observer;

const keyword = computed(() => String(route.query.keyword || "").trim());
const typeId = computed(() => String(route.params.typeId || ""));
const typeName = computed(() => {
  const fromQuery = String(route.query.name || "").trim();
  if (fromQuery) {
    return fromQuery;
  }
  return homeFeedState.shopTypes.value.find((item) => String(item.id) === typeId.value)?.name || "全部商铺";
});

const pageTitle = computed(() =>
  keyword.value ? `搜索“${keyword.value}”` : `${typeName.value}商铺`,
);

async function ensureTypes() {
  if (homeFeedState.shopTypes.value.length) {
    return;
  }
  const { data, success } = await shopApi.fetchTypes({ silentError: true });
  if (success) {
    homeFeedState.shopTypes.value = Array.isArray(data) ? data : [];
  }
}

async function requestCurrentPage(reset = false) {
  const endpointKey = keyword.value ? "GET /shop/of/name" : "GET /shop/of/type";
  if (isLoading(endpointKey)) {
    return;
  }

  if (reset) {
    shopFlowState.currentPage.value = 1;
    shopFlowState.hasMore.value = true;
    shopFlowState.shops.value = [];
  }

  if (!shopFlowState.hasMore.value) {
    return;
  }

  let result;
  if (keyword.value) {
    result = await shopApi.fetchByName(
      { name: keyword.value, current: shopFlowState.currentPage.value },
      { silentError: true },
    );
  } else {
    result = await shopApi.fetchByType(
      {
        typeId: typeId.value,
        current: shopFlowState.currentPage.value,
        x: shopFlowState.location.enabled ? shopFlowState.location.x : undefined,
        y: shopFlowState.location.enabled ? shopFlowState.location.y : undefined,
      },
      { silentError: true },
    );
  }

  if (!result.success) {
    return;
  }

  const rows = Array.isArray(result.data) ? result.data : [];
  if (!rows.length) {
    shopFlowState.hasMore.value = false;
    return;
  }

  shopFlowState.shops.value = [...shopFlowState.shops.value, ...rows];
  shopFlowState.currentPage.value += 1;
}

function openType(type) {
  router.push({
    name: "shop-list",
    params: { typeId: String(type.id) },
    query: { name: type.name },
  });
}

function enableGeolocation() {
  if (!navigator.geolocation) {
    setNotice("error", "当前浏览器不支持定位。");
    return;
  }

  navigator.geolocation.getCurrentPosition(
    async ({ coords }) => {
      shopFlowState.location.x = coords.longitude;
      shopFlowState.location.y = coords.latitude;
      shopFlowState.location.enabled = true;
      await requestCurrentPage(true);
      setNotice("success", "已启用当前位置排序。");
    },
    () => {
      setNotice("error", "定位失败，已继续使用普通列表模式。");
    },
  );
}

watch(
  () => [route.params.typeId, route.query.keyword, route.query.name],
  async () => {
    await requestCurrentPage(true);
  },
);

onMounted(async () => {
  await ensureTypes();
  await requestCurrentPage(true);

  if ("IntersectionObserver" in window && loadMoreRef.value) {
    observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        requestCurrentPage();
      }
    }, { rootMargin: "120px" });
    observer.observe(loadMoreRef.value);
  }
});

onBeforeUnmount(() => observer?.disconnect());
</script>

<template>
  <section class="consumer-page">
    <div class="section-title">
      <div>
        <p class="eyebrow">Shop List</p>
        <h2>{{ pageTitle }}</h2>
      </div>
      <span class="section-hint">
        {{ keyword ? "当前使用名称搜索链路。" : "当前使用分类分页链路。" }}
      </span>
    </div>

    <article class="panel ue-washi ue-shadow">
      <div class="panel-head">
        <h3>筛选与切换</h3>
        <span class="status-pill muted">{{ shopFlowState.location.enabled ? "已启用定位" : "普通列表模式" }}</span>
      </div>

      <div class="button-row wrap">
        <button class="secondary" @click="router.back()">返回上一页</button>
        <button
          v-if="!keyword"
          :class="shopFlowState.location.enabled ? 'accent' : ''"
          @click="enableGeolocation"
        >
          使用当前位置排序
        </button>
        <button disabled class="secondary">人气排序（后续开放）</button>
        <button disabled class="secondary">评分排序（后续开放）</button>
      </div>

      <div v-if="!keyword" class="consumer-inline-type-list">
        <button
          v-for="type in homeFeedState.shopTypes.value"
          :key="type.id"
          class="secondary"
          :class="{ accent: String(type.id) === typeId }"
          @click="openType(type)"
        >
          {{ type.name }}
        </button>
      </div>
    </article>

    <div class="shop-feed-grid">
      <ShopPreviewCard
        v-for="shop in shopFlowState.shops.value"
        :key="shop.id"
        :shop="shop"
      />
    </div>

    <div ref="loadMoreRef" class="feed-sentinel">
      <span v-if="isLoading(keyword ? 'GET /shop/of/name' : 'GET /shop/of/type')" class="helper">
        正在续载商铺列表...
      </span>
      <span v-else-if="!shopFlowState.hasMore.value" class="helper">
        没有更多商铺了。
      </span>
    </div>
  </section>
</template>
