<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { voucherApi } from "../services/voucherApi";
import { buildRedirectPath, isAuthenticated } from "../stores/session";
import { formatPrice, formatVoucherWindow, voucherState } from "../utils/view";

const router = useRouter();

const vouchers = ref([]);
const activeTab = ref("seckill");
const latestOrderId = ref(null);
const orderingVoucherId = ref(null);
const loading = ref(false);

const seckillVouchers = computed(() =>
  vouchers.value.filter((voucher) => voucher.type === 1),
);
const normalVouchers = computed(() =>
  vouchers.value.filter((voucher) => voucher.type !== 1),
);

async function fetchClaimableVouchers(showMessage = false) {
  loading.value = true;
  const { data, success } = await voucherApi.fetchClaimable({
    successMessage: showMessage ? "可抢优惠券已刷新。" : "",
  });
  loading.value = false;

  if (success) {
    vouchers.value = Array.isArray(data) ? data : [];
  }
}

function jumpToLogin() {
  router.push(buildRedirectPath("/claim-vouchers"));
}

async function claimSeckillVoucher(voucher) {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }
  const state = voucherState(voucher);
  if (state.disabled) {
    return;
  }

  orderingVoucherId.value = voucher.id;
  const { data, success } = await voucherApi.seckill(voucher.id, {
    successMessage: "秒杀请求已发送。",
  });
  orderingVoucherId.value = null;

  if (success) {
    latestOrderId.value = data ?? null;
    await fetchClaimableVouchers(false);
  }
}

async function claimNormalVoucher(voucher) {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }

  orderingVoucherId.value = voucher.id;
  const { success } = await voucherApi.claim(voucher.id, {
    successMessage: "优惠券领取成功。",
  });
  orderingVoucherId.value = null;

  if (success) {
    await fetchClaimableVouchers(false);
  }
}

onMounted(() => fetchClaimableVouchers(false));
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h2 class="page-panel__title">抢优惠券</h2>
            <p class="page-panel__hint">
              按普通券和秒杀优惠券分类展示当前可用券。
            </p>
          </div>
          <ElTag :type="isAuthenticated() ? 'primary' : 'info'" effect="plain">
            {{ isAuthenticated() ? "可一键抢购" : "登录后抢购" }}
          </ElTag>
        </div>
      </template>

      <div class="filter-actions">
        <ElButton
          type="primary"
          :loading="loading"
          @click="fetchClaimableVouchers(true)"
        >
          刷新可抢券
        </ElButton>
        <ElTag effect="plain">秒杀券 {{ seckillVouchers.length }} 张</ElTag>
        <ElTag effect="plain">普通券 {{ normalVouchers.length }} 张</ElTag>
        <ElTag v-if="latestOrderId" type="success" effect="plain">
          最近订单 ID：{{ latestOrderId }}
        </ElTag>
      </div>
    </ElCard>

    <ElCard class="page-panel">
      <ElTabs v-model="activeTab">
        <ElTabPane label="秒杀优惠券" name="seckill">
          <ElTable :data="seckillVouchers" border stripe>
            <ElTableColumn prop="id" label="券 ID" width="96" />
            <ElTableColumn prop="shopId" label="店铺 ID" width="100" />
            <ElTableColumn prop="title" label="标题" min-width="160" />
            <ElTableColumn prop="subTitle" label="副标题" min-width="180" />
            <ElTableColumn label="支付 / 抵扣" min-width="180">
              <template #default="{ row }">
                {{ formatPrice(row.payValue) }} /
                {{ formatPrice(row.actualValue) }}
              </template>
            </ElTableColumn>
            <ElTableColumn prop="stock" label="库存" width="96" />
            <ElTableColumn label="有效期" min-width="220">
              <template #default="{ row }">
                {{ formatVoucherWindow(row) }}
              </template>
            </ElTableColumn>
            <ElTableColumn label="操作" width="128" fixed="right">
              <template #default="{ row }">
                <ElButton
                  link
                  type="danger"
                  :disabled="voucherState(row).disabled"
                  :loading="orderingVoucherId === row.id"
                  @click="claimSeckillVoucher(row)"
                >
                  {{
                    voucherState(row).disabled
                      ? voucherState(row).label
                      : "立即抢购"
                  }}
                </ElButton>
              </template>
            </ElTableColumn>
            <template #empty>
              <ElEmpty description="暂无可抢秒杀券。" />
            </template>
          </ElTable>
        </ElTabPane>

        <ElTabPane label="普通优惠券" name="normal">
          <ElTable :data="normalVouchers" border stripe>
            <ElTableColumn prop="id" label="券 ID" width="96" />
            <ElTableColumn prop="shopId" label="店铺 ID" width="100" />
            <ElTableColumn prop="title" label="标题" min-width="160" />
            <ElTableColumn prop="subTitle" label="副标题" min-width="180" />
            <ElTableColumn label="支付 / 抵扣" min-width="180">
              <template #default="{ row }">
                {{ formatPrice(row.payValue) }} /
                {{ formatPrice(row.actualValue) }}
              </template>
            </ElTableColumn>
            <ElTableColumn prop="rules" label="使用规则" min-width="220" />
            <ElTableColumn label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <ElButton
                  link
                  type="primary"
                  :loading="orderingVoucherId === row.id"
                  @click="claimNormalVoucher(row)"
                >
                  立即领卷
                </ElButton>
              </template>
            </ElTableColumn>
            <template #empty>
              <ElEmpty description="暂无普通优惠券。" />
            </template>
          </ElTable>
        </ElTabPane>
      </ElTabs>
    </ElCard>
  </section>
</template>
