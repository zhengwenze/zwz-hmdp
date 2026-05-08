<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { shopApi } from "../services/shopApi";
import { voucherApi } from "../services/voucherApi";
import { buildRedirectPath, isAuthenticated } from "../stores/session";
import {
  firstImage,
  formatPrice,
  formatVoucherWindow,
  voucherState,
} from "../utils/view";
import { findRecentShop, rememberShop } from "../stores/historyState";

const route = useRoute();
const router = useRouter();

const selectedShop = ref(null);
const vouchers = ref([]);
const isLoading = ref(false);
const loadError = ref("");
const orderingVoucherId = ref(null);

const fallbackShop = computed(() => findRecentShop(route.params.id));
const resolvedShop = computed(() => selectedShop.value || fallbackShop.value);
const coverImage = computed(() => firstImage(resolvedShop.value?.images));
const returnQuery = computed(() => ({
  tab: route.query.tab,
  typeId: route.query.typeId,
  current: route.query.current,
  x: route.query.x,
  y: route.query.y,
  name: route.query.name,
  nameCurrent: route.query.nameCurrent,
}));

async function loadShopDetail() {
  const shopId = route.params.id;
  if (!shopId) {
    return;
  }

  isLoading.value = true;
  loadError.value = "";

  const [shopResult, voucherResult] = await Promise.all([
    shopApi.fetchDetail(shopId, { silentError: true }),
    voucherApi.fetchList(shopId, { silentError: true }),
  ]);

  if (shopResult.success) {
    selectedShop.value = shopResult.data || null;
    rememberShop(selectedShop.value);
  } else {
    selectedShop.value = null;
    loadError.value = "商铺详情暂时拉取失败。";
  }

  if (voucherResult.success) {
    vouchers.value = Array.isArray(voucherResult.data) ? voucherResult.data : [];
  } else {
    vouchers.value = [];
  }

  isLoading.value = false;
}

function backToList() {
  router.push({
    name: "shop-list",
    query: returnQuery.value,
  });
}

function jumpToLogin() {
  router.push(buildRedirectPath(route.fullPath || `/shop/detail/${route.params.id}`));
}

async function claimVoucher(row) {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }

  orderingVoucherId.value = row.id;
  const request = row.type === 1
    ? voucherApi.seckill(row.id, { successMessage: "秒杀请求已发送。" })
    : voucherApi.claim(row.id, { successMessage: "优惠券领取成功。" });
  const { success } = await request;
  orderingVoucherId.value = null;

  if (success) {
    await loadShopDetail();
  }
}

watch(() => route.params.id, loadShopDetail);
onMounted(loadShopDetail);
</script>

<template>
  <section class="app-page shop-detail-page">
    <ElCard class="page-panel shop-detail-hero">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h2 class="page-panel__title">
              {{ resolvedShop?.name || "商铺详情" }}
            </h2>
            <p class="page-panel__hint">
              当前页只展示单个商铺详情，列表状态会在返回时恢复。
            </p>
          </div>
          <ElButton @click="backToList">返回商铺列表</ElButton>
        </div>
      </template>

      <ElAlert
        v-if="loadError"
        :title="loadError"
        type="warning"
        show-icon
        :closable="false"
        class="shop-detail-alert"
      />

      <ElSkeleton v-if="isLoading && !resolvedShop" :rows="8" animated />

      <ElEmpty
        v-else-if="!resolvedShop"
        description="未找到当前商铺详情"
      />

      <div v-else class="shop-detail-hero__body">
        <div class="shop-detail-cover">
          <ElImage
            v-if="coverImage"
            :src="coverImage"
            :alt="resolvedShop.name"
            fit="cover"
          />
          <div v-else class="shop-detail-cover__empty">
            暂无封面
          </div>
        </div>

        <div class="shop-detail-summary">
          <div class="shop-detail-summary__title">
            <div>
              <h3>{{ resolvedShop.name }}</h3>
              <p>{{ resolvedShop.area || "未知商圈" }}</p>
            </div>
            <ElTag effect="plain">ID {{ resolvedShop.id }}</ElTag>
          </div>

          <div class="shop-detail-metrics">
            <div>
              <span>评分</span>
              <strong>{{ ((resolvedShop.score || 0) / 10).toFixed(1) }}</strong>
            </div>
            <div>
              <span>均价</span>
              <strong>￥{{ formatPrice(resolvedShop.avgPrice) }}</strong>
            </div>
            <div>
              <span>销量</span>
              <strong>{{ resolvedShop.sold ?? 0 }}</strong>
            </div>
            <div>
              <span>评论</span>
              <strong>{{ resolvedShop.comments ?? 0 }}</strong>
            </div>
          </div>

          <ElDescriptions :column="2" border>
            <ElDescriptionsItem label="营业时间">
              {{ resolvedShop.openHours || "--" }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="商铺类型">
              {{ resolvedShop.typeId ?? "--" }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="地址">
              {{ resolvedShop.address || "暂无地址" }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="坐标">
              {{ resolvedShop.x || "--" }}, {{ resolvedShop.y || "--" }}
            </ElDescriptionsItem>
          </ElDescriptions>
        </div>
      </div>
    </ElCard>

    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h3 class="page-panel__title">店铺优惠券</h3>
            <p class="page-panel__hint">展示当前商铺关联的普通券和秒杀券。</p>
          </div>
          <ElTag effect="plain">{{ vouchers.length }} 张</ElTag>
        </div>
      </template>

      <ElTable :data="vouchers" border stripe>
        <ElTableColumn prop="id" label="券 ID" width="100" />
        <ElTableColumn prop="title" label="标题" min-width="160" />
        <ElTableColumn prop="subTitle" label="副标题" min-width="180" />
        <ElTableColumn label="类型" width="110">
          <template #default="{ row }">
            <ElTag :type="row.type === 1 ? 'danger' : 'info'" effect="plain">
              {{ row.type === 1 ? "秒杀券" : "普通券" }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="价格" min-width="160">
          <template #default="{ row }">
            支付 {{ formatPrice(row.payValue) }} / 抵扣 {{ formatPrice(row.actualValue) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="时窗" min-width="220">
          <template #default="{ row }">
            {{ formatVoucherWindow(row) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="库存" width="120">
          <template #default="{ row }">
            {{ row.type === 1 ? row.stock ?? 0 : "--" }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="140">
          <template #default="{ row }">
            <ElTag effect="plain">
              {{ row.type === 1 ? voucherState(row).label : "普通券" }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <ElButton
              link
              :type="row.type === 1 ? 'danger' : 'primary'"
              :loading="orderingVoucherId === row.id"
              @click="claimVoucher(row)"
            >
              {{ row.type === 1 ? "抢卷" : "领卷" }}
            </ElButton>
          </template>
        </ElTableColumn>
        <template #empty>
          <ElEmpty description="当前商铺暂无优惠券数据" />
        </template>
      </ElTable>
    </ElCard>
  </section>
</template>

<style scoped>
.shop-detail-alert {
  margin-bottom: 16px;
}

.shop-detail-hero__body {
  display: grid;
  grid-template-columns: minmax(260px, 36%) 1fr;
  gap: 24px;
  align-items: stretch;
}

.shop-detail-cover {
  min-height: 320px;
  overflow: hidden;
  border-radius: 8px;
  background: #f4f6f8;
}

.shop-detail-cover .el-image {
  width: 100%;
  height: 100%;
}

.shop-detail-cover__empty {
  display: grid;
  place-items: center;
  height: 100%;
  color: var(--app-text-secondary);
  font-size: 14px;
}

.shop-detail-summary {
  display: grid;
  gap: 18px;
  align-content: start;
}

.shop-detail-summary__title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.shop-detail-summary__title h3 {
  margin: 0;
  color: var(--app-text);
  font-size: 24px;
  line-height: 1.25;
}

.shop-detail-summary__title p {
  margin: 6px 0 0;
  color: var(--app-text-secondary);
  font-size: 14px;
}

.shop-detail-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.shop-detail-metrics > div {
  min-height: 74px;
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: #f8fafc;
}

.shop-detail-metrics span {
  display: block;
  color: var(--app-text-secondary);
  font-size: 12px;
}

.shop-detail-metrics strong {
  display: block;
  margin-top: 8px;
  color: var(--app-text);
  font-size: 20px;
  line-height: 1.2;
}

@media (max-width: 960px) {
  .shop-detail-hero__body {
    grid-template-columns: 1fr;
  }

  .shop-detail-cover {
    min-height: 220px;
  }

  .shop-detail-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
