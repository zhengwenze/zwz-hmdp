<script setup>
import { computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { shopFlowState } from "../stores/shopFlow";
import { shopApi } from "../services/shopApi";
import { voucherApi } from "../services/voucherApi";
import { splitImages, formatPrice, formatVoucherWindow, voucherState } from "../utils/view";
import { isAuthenticated } from "../stores/session";
import { setNotice } from "../stores/appState";
import { findRecentShop, rememberShop } from "../stores/historyState";

const route = useRoute();
const router = useRouter();

const fallbackShop = computed(() => findRecentShop(route.params.id));
const resolvedShop = computed(() => shopFlowState.selectedShop.value || fallbackShop.value);
const shopImages = computed(() => splitImages(resolvedShop.value?.images));

async function loadShop() {
  const shopId = route.params.id;
  shopFlowState.detailLoading.value = true;
  shopFlowState.detailError.value = "";
  const [shopResult, voucherResult] = await Promise.all([
    shopApi.fetchDetail(shopId, { silentError: true }),
    voucherApi.fetchList(shopId, { silentError: true }),
  ]);

  if (shopResult.success) {
    shopFlowState.selectedShop.value = shopResult.data || null;
    rememberShop(shopFlowState.selectedShop.value);
  } else {
    shopFlowState.selectedShop.value = null;
    shopFlowState.detailError.value = "店铺详情暂时拉取失败，先用列表里的摘要信息兜底展示。";
  }

  if (voucherResult.success) {
    shopFlowState.vouchers.value = Array.isArray(voucherResult.data) ? voucherResult.data : [];
  }
  shopFlowState.detailLoading.value = false;
}

async function handleVoucher(voucher) {
  const state = voucherState(voucher);
  if (state.disabled) {
    setNotice("info", `${voucher.title}：${state.label}`);
    return;
  }

  if (!isAuthenticated()) {
    router.push({
      path: "/login",
      query: { redirect: route.fullPath },
    });
    return;
  }

  const { data, success } = await voucherApi.seckill(voucher.id, {
    successMessage: "秒杀请求已发送。",
  });
  if (success) {
    setNotice("success", `秒杀成功，订单号：${data}`);
    await loadShop();
  }
}

watch(() => route.params.id, loadShop);
onMounted(loadShop);
</script>

<template>
  <section class="consumer-page">
    <article class="panel ue-washi ue-shadow">
      <div class="panel-head">
        <h2>{{ resolvedShop?.name || "商铺详情" }}</h2>
        <button class="secondary" @click="router.back()">返回列表</button>
      </div>

      <p v-if="shopFlowState.detailError.value" class="helper">{{ shopFlowState.detailError.value }}</p>
      <p v-if="shopFlowState.detailLoading.value" class="helper">正在整理店铺详情...</p>

      <div class="shop-detail-meta">
        <div>
          <span class="label">评分</span>
          <strong>{{ ((resolvedShop?.score || 0) / 10).toFixed(1) }}</strong>
        </div>
        <div>
          <span class="label">评论</span>
          <strong>{{ resolvedShop?.comments ?? 0 }}</strong>
        </div>
        <div>
          <span class="label">均价</span>
          <strong>￥{{ formatPrice(resolvedShop?.avgPrice) }}/人</strong>
        </div>
        <div>
          <span class="label">营业时间</span>
          <strong>{{ resolvedShop?.openHours || "--" }}</strong>
        </div>
      </div>

      <div class="image-strip shop-detail-gallery">
        <img v-for="image in shopImages" :key="image" :src="image" :alt="resolvedShop?.name" />
      </div>

      <div class="consumer-rich-copy">
        <p>{{ resolvedShop?.area || "商圈待补充" }}</p>
        <p>{{ resolvedShop?.address || "地址待补充" }}</p>
      </div>

      <div class="button-row wrap">
        <button class="accent" @click="router.push('/blog/new')">去发探店笔记</button>
        <button class="secondary" @click="router.push('/')">继续逛别的店</button>
      </div>
    </article>

    <article class="panel ue-washi ue-shadow">
      <div class="panel-head">
        <h3>店铺优惠券</h3>
        <span class="status-pill muted">秒杀链路已接通</span>
      </div>
      <div class="voucher-list-grid">
        <article
          v-for="voucher in shopFlowState.vouchers.value"
          :key="voucher.id"
          class="voucher-card consumer-voucher-card"
        >
          <div class="blog-card-head">
            <strong>{{ voucher.title }}</strong>
            <span class="ue-stamp">{{ voucher.type === 1 ? "秒杀券" : "普通券" }}</span>
          </div>
          <p>{{ voucher.subTitle }}</p>
          <p>支付 {{ formatPrice(voucher.payValue) }} / 抵扣 {{ formatPrice(voucher.actualValue) }}</p>
          <p>{{ formatVoucherWindow(voucher) }}</p>
          <div class="button-row tight">
            <button
              :disabled="voucher.type === 1 ? voucherState(voucher).disabled : false"
              :class="voucher.type === 1 && !voucherState(voucher).disabled ? 'accent' : 'secondary'"
              @click="handleVoucher(voucher)"
            >
              {{ voucher.type === 1 ? voucherState(voucher).label : "到店使用" }}
            </button>
          </div>
        </article>
      </div>
    </article>

    <article class="panel ue-washi ue-shadow">
      <div class="panel-head">
        <h3>当前限制</h3>
        <span class="status-pill muted">显式降级</span>
      </div>
      <p class="helper">
        原型里的真实评论流、标签筛选和评价详情依赖后端评论接口；当前仓库的
        `/blog-comments` 控制器仍为空实现，所以本轮只保留店铺详情与优惠券链路。
      </p>
    </article>
  </section>
</template>
