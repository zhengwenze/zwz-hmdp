<script setup>
import { computed, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import {
  sharedState,
  setNotice,
  setLoading,
  isLoading,
  markTouched,
} from "../stores/sharedState";

const router = useRouter();
const route = useRoute();

const navSections = [
  { id: "hero", label: "总览", path: "/" },
  { id: "user", label: "用户", path: "/user" },
  { id: "shop-types", label: "分类", path: "/shop-types" },
  { id: "shops", label: "商铺", path: "/shops" },
  { id: "blogs", label: "博客", path: "/blogs" },
  { id: "follow", label: "关注", path: "/follow" },
  { id: "vouchers", label: "优惠券", path: "/vouchers" },
  { id: "upload", label: "上传", path: "/upload" },
  { id: "logs", label: "请求日志", path: "/logs" },
];

const effectiveApiBase = computed(
  () => sharedState.apiBaseUrl.value.trim() || "同源代理（Vite dev proxy）",
);
const effectiveAssetBase = computed(
  () => sharedState.assetBaseUrl.value.trim() || sharedState.apiBaseUrl.value.trim(),
);
const isLoggedIn = computed(() => Boolean(sharedState.token.value.trim()));
const shortToken = computed(() => {
  if (!sharedState.token.value) {
    return "未登录";
  }
  if (sharedState.token.value.length < 16) {
    return sharedState.token.value;
  }
  return `${sharedState.token.value.slice(0, 8)}...${sharedState.token.value.slice(-6)}`;
});

const endpointCatalog = [
  { key: "POST /user/code", module: "用户", label: "发送验证码" },
  { key: "POST /user/login", module: "用户", label: "验证码登录" },
  { key: "POST /user/logout", module: "用户", label: "登出（后端未完成）" },
  { key: "GET /user/me", module: "用户", label: "当前用户" },
  { key: "GET /user/info/{id}", module: "用户", label: "用户详情" },
  { key: "GET /user/{id}", module: "用户", label: "用户基础信息" },
  { key: "POST /user/sign", module: "用户", label: "签到" },
  { key: "GET /user/sign/count", module: "用户", label: "连续签到天数" },
  { key: "GET /shop/{id}", module: "商铺", label: "商铺详情" },
  { key: "POST /shop", module: "商铺", label: "新增商铺" },
  { key: "PUT /shop", module: "商铺", label: "更新商铺" },
  { key: "GET /shop/of/type", module: "商铺", label: "按分类查商铺" },
  { key: "GET /shop/of/name", module: "商铺", label: "按名称查商铺" },
  { key: "GET /shop-type/list", module: "分类", label: "商铺分类列表" },
  { key: "POST /blog", module: "博客", label: "发布博客" },
  { key: "PUT /blog/like/{id}", module: "博客", label: "点赞 / 取消点赞" },
  { key: "GET /blog/of/me", module: "博客", label: "我的博客" },
  { key: "GET /blog/hot", module: "博客", label: "热门博客" },
  { key: "GET /blog/{id}", module: "博客", label: "博客详情" },
  { key: "GET /blog/likes/{id}", module: "博客", label: "点赞用户列表" },
  { key: "GET /blog/of/user", module: "博客", label: "用户博客列表" },
  { key: "GET /blog/of/follow", module: "博客", label: "关注推送流" },
  { key: "PUT /follow/{id}/{isFollow}", module: "关注", label: "关注 / 取关" },
  { key: "GET /follow/or/not/{id}", module: "关注", label: "是否关注" },
  { key: "GET /follow/common/{id}", module: "关注", label: "共同关注" },
  { key: "POST /voucher", module: "优惠券", label: "新增普通券" },
  { key: "POST /voucher/seckill", module: "优惠券", label: "新增秒杀券" },
  { key: "GET /voucher/list/{shopId}", module: "优惠券", label: "店铺券列表" },
  { key: "POST /voucher-order/seckill/{id}", module: "秒杀", label: "秒杀下单" },
  { key: "POST /upload/blog", module: "上传", label: "上传博客图片" },
  { key: "GET /upload/blog/delete", module: "上传", label: "删除博客图片" },
];

const coverageCount = computed(
  () =>
    endpointCatalog.filter((endpoint) => sharedState.touchedEndpoints[endpoint.key]).length,
);

watch(sharedState.apiBaseUrl, (value) => localStorage.setItem("hmdp-api-base", value));
watch(sharedState.assetBaseUrl, (value) => localStorage.setItem("hmdp-asset-base", value));
watch(sharedState.token, (value) => localStorage.setItem("hmdp-token", value));

function navigateTo(path) {
  router.push(path);
}

function isActiveNav(navPath) {
  if (navPath === "/") {
    return route.path === "/";
  }
  return route.path.startsWith(navPath);
}
</script>

<template>
  <div class="shell">
    <aside class="sidebar ue-shadow ue-washi">
      <div class="sidebar-head">
        <div class="ue-stamp">浮世绘联调台</div>
        <p>全部后端接口都在这个单页里直连验证。</p>
      </div>
      <nav class="sidebar-nav">
        <a
          v-for="item in navSections"
          :key="item.id"
          class="nav-link"
          :class="{ active: isActiveNav(item.path) }"
          @click.prevent="navigateTo(item.path)"
        >
          {{ item.label }}
        </a>
      </nav>
      <div class="sidebar-foot">
        <p class="meta-label">接口覆盖</p>
        <strong>{{ coverageCount }} / {{ endpointCatalog.length }}</strong>
        <p class="meta-label">Blog Comments</p>
        <span class="empty-note">后端仅有空控制器，无可调用方法</span>
      </div>
    </aside>

    <main class="main">
      <router-view />
    </main>

    <aside class="response-panel ue-shadow">
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
      <pre class="json-box dark">{{ sharedState.lastResponse.value ? JSON.stringify(sharedState.lastResponse.value.body, null, 2) : '{ message: "尚无请求" }' }}</pre>
    </aside>
  </div>
</template>
