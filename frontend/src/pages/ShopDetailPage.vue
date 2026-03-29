<script setup>
import { computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { shopFlowState } from "../stores/shopFlow";
import { shopApi } from "../services/shopApi";
import { voucherApi } from "../services/voucherApi";
import { splitImages, formatPrice, formatVoucherWindow, voucherState } from "../utils/view";
import { isAuthenticated } from "../stores/session";
import { setNotice } from "../stores/appState";

const route = useRoute();
const router = useRouter();

const shopImages = computed(() => splitImages(shopFlowState.selectedShop.value?.images));

async function loadShop() {
  const shopId = route.params.id;
  const [shopResult, voucherResult] = await Promise.all([
    shopApi.fetchDetail(shopId, { silentError: true }),
    voucherApi.fetchList(shopId, { silentError: true }),
  ]);

  if (shopResult.success) {
    shopFlowState.selectedShop.value = shopResult.data || null;
  }

  if (voucherResult.success) {
    shopFlowState.vouchers.value = Array.isArray(voucherResult.data) ? voucherResult.data : [];
  }
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
        <h2>{{ shopFlowState.selectedShop.value?.name || "商铺详情" }}</h2>
        <button class="secondary" @click="router.back()">返回列表</button>
      </div>

      <div class="shop-detail-meta">
        <div>
          <span class="label">评分</span>
          <strong>{{ ((shopFlowState.selectedShop.value?.score || 0) / 10).toFixed(1) }}</strong>
        </div>
        <div>
          <span class="label">评论</span>
          <strong>{{ shopFlowState.selectedShop.value?.comments ?? 0 }}</strong>
        </div>
        <div>
          <span class="label">均价</span>
          <strong>￥{{ formatPrice(shopFlowState.selectedShop.value?.avgPrice) }}/人</strong>
        </div>
        <div>
          <span class="label">营业时间</span>
          <strong>{{ shopFlowState.selectedShop.value?.openHours || "--" }}</strong>
        </div>
      </div>

      <div class="image-strip shop-detail-gallery">
        <img v-for="image in shopImages" :key="image" :src="image" :alt="shopFlowState.selectedShop.value?.name" />
      </div>

      <div class="consumer-rich-copy">
        <p>{{ shopFlowState.selectedShop.value?.area || "商圈待补充" }}</p>
        <p>{{ shopFlowState.selectedShop.value?.address || "地址待补充" }}</p>
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
        <h3>评价区说明</h3>
        <span class="status-pill muted">显式降级</span>
      </div>
      <p class="helper">
        原型里的真实评论流、标签筛选和评价详情依赖后端评论接口；当前仓库的
        `/blog-comments` 控制器仍为空实现，所以本轮只保留店铺详情与优惠券链路。
      </p>
    </article>
  </section>
</template>
