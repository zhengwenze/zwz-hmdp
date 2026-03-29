<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from "vue";
import { useRouter } from "vue-router";
import { homeFeedState } from "../stores/homeFeed";
import { sessionState } from "../stores/session";
import { toAssetUrl } from "../stores/appState";
import { shopApi } from "../services/shopApi";
import { blogApi } from "../services/blogApi";
import { setNotice } from "../stores/appState";
import { isLoading } from "../stores/labState";
import BlogPreviewCard from "../components/BlogPreviewCard.vue";

const router = useRouter();
const keyword = ref("");
const loadMoreRef = ref(null);
let observer;

const featuredTypes = computed(() => homeFeedState.shopTypes.value.slice(0, 10));

async function ensureTypes() {
  if (homeFeedState.shopTypes.value.length) {
    return;
  }

  const { data, success } = await shopApi.fetchTypes({ silentError: true });
  if (success) {
    homeFeedState.shopTypes.value = Array.isArray(data) ? data : [];
  }
}

async function loadHotBlogs(reset = false) {
  if (isLoading("GET /blog/hot")) {
    return;
  }

  if (reset) {
    homeFeedState.currentPage.value = 1;
    homeFeedState.hasMore.value = true;
    homeFeedState.hotBlogs.value = [];
  }

  if (!homeFeedState.hasMore.value) {
    return;
  }

  const { data, success } = await blogApi.fetchHot(homeFeedState.currentPage.value, {
    silentError: true,
  });

  if (!success) {
    return;
  }

  const rows = Array.isArray(data) ? data : [];
  if (!rows.length) {
    homeFeedState.hasMore.value = false;
    return;
  }

  homeFeedState.hotBlogs.value = [...homeFeedState.hotBlogs.value, ...rows];
  homeFeedState.currentPage.value += 1;
}

function submitSearch() {
  if (!keyword.value.trim()) {
    setNotice("info", "请输入商户名或地点关键词。");
    return;
  }
  router.push({
    name: "shop-list",
    params: { typeId: "all" },
    query: { keyword: keyword.value.trim() },
  });
}

function openType(type) {
  router.push({
    name: "shop-list",
    params: { typeId: String(type.id) },
    query: { name: type.name },
  });
}

onMounted(async () => {
  await Promise.all([ensureTypes(), loadHotBlogs(true)]);
  homeFeedState.initialized.value = true;

  if ("IntersectionObserver" in window && loadMoreRef.value) {
    observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        loadHotBlogs();
      }
    }, { rootMargin: "120px" });
    observer.observe(loadMoreRef.value);
  }
});

onBeforeUnmount(() => observer?.disconnect());
</script>

<template>
  <section class="consumer-page home-page">
    <section class="hero-card ue-wave-bg ue-washi ue-shadow ukiyo-e-digital-accent-corner">
      <div class="hero-copy">
        <p class="eyebrow">Home</p>
        <h1>浮世绘版黑马点评主站</h1>
        <p class="hero-text">
          交互逻辑切换成原型里的消费链路：首页先看分类与热门笔记，随后进入商铺、秒杀、关注和发笔记。
        </p>
      </div>

      <div class="consumer-search-bar">
        <input
          v-model="keyword"
          placeholder="请输入商户名、地点或商圈"
          @keyup.enter="submitSearch"
        />
        <button class="accent" @click="submitSearch">搜索商铺</button>
      </div>

      <div class="hero-grid">
        <div class="info-card ukiyo-e-digital-card">
          <span class="label">会话</span>
          <strong>{{ sessionState.currentUser.value?.nickName || "游客" }}</strong>
          <small>{{ sessionState.token.value ? "已登录，可操作完整链路" : "公开链路可直接浏览" }}</small>
        </div>
        <div class="info-card ukiyo-e-digital-card">
          <span class="label">分类</span>
          <strong>{{ homeFeedState.shopTypes.value.length }}</strong>
          <small>点击分类进入商铺列表</small>
        </div>
        <div class="info-card ukiyo-e-digital-card">
          <span class="label">热门笔记</span>
          <strong>{{ homeFeedState.hotBlogs.value.length }}</strong>
          <small>下拉自动续载更多内容</small>
        </div>
      </div>
    </section>

    <section class="module-section">
      <div class="section-title">
        <div>
          <p class="eyebrow">Categories</p>
          <h2>商铺分类入口</h2>
        </div>
        <span class="section-hint">保留黑马点评首页先选分类的操作习惯</span>
      </div>

      <div class="consumer-type-grid">
        <button
          v-for="type in featuredTypes"
          :key="type.id"
          class="type-chip consumer-type-chip"
          @click="openType(type)"
        >
          <img :src="toAssetUrl(type.icon)" :alt="type.name" />
          <span>{{ type.name }}</span>
        </button>
      </div>
    </section>

    <section class="module-section">
      <div class="section-title">
        <div>
          <p class="eyebrow">Trending</p>
          <h2>热门笔记流</h2>
        </div>
        <span class="section-hint">点击卡片进入详情，可进一步点赞、关注作者、查看关联商铺</span>
      </div>

      <div class="blog-feed-grid">
        <BlogPreviewCard
          v-for="blog in homeFeedState.hotBlogs.value"
          :key="blog.id"
          :blog="blog"
        />
      </div>

      <div ref="loadMoreRef" class="feed-sentinel">
        <span v-if="isLoading('GET /blog/hot')" class="helper">正在续载更多热门笔记...</span>
        <span v-else-if="!homeFeedState.hasMore.value" class="helper">热门流已经到底了。</span>
      </div>
    </section>
  </section>
</template>
