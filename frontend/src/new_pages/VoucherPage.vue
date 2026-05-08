<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { shopApi } from "../services/shopApi";
import { voucherApi } from "../services/voucherApi";
import { setNotice } from "../stores/appState";
import { rememberShop } from "../stores/historyState";

const router = useRouter();

const forms = reactive({
  normal: {
    title: "",
    subTitle: "",
    rules: "",
    payValue: "",
    actualValue: "",
  },
  seckill: {
    title: "",
    subTitle: "",
    rules: "",
    payValue: "",
    actualValue: "",
    stock: "",
    beginTime: "",
    endTime: "",
  },
});

const activeTab = ref("normal");
const selectedShopId = ref("");
const selectedShop = ref(null);
const shopOptions = ref([]);
const shopSearchLoading = ref(false);

async function searchShops(keyword) {
  const name = String(keyword || "").trim();
  if (!name) {
    shopOptions.value = [];
    return;
  }

  shopSearchLoading.value = true;
  const { data, success } = await shopApi.fetchByName(
    { name, current: 1 },
    { silentError: true },
  );
  shopSearchLoading.value = false;

  if (success) {
    shopOptions.value = Array.isArray(data) ? data : [];
  }
}

function selectShop(shopId) {
  selectedShop.value = shopOptions.value.find(
    (shop) => String(shop.id) === String(shopId),
  ) || null;
  if (selectedShop.value) {
    rememberShop(selectedShop.value);
  }
}

function ensureShopSelected() {
  if (selectedShop.value?.id) {
    return true;
  }
  setNotice("error", "请先选择店铺");
  return false;
}

function goToSelectedShop() {
  router.push({
    name: "shop-detail",
    params: { id: selectedShop.value.id },
  });
}

function clearShop() {
  selectedShop.value = null;
}

async function createNormalVoucher() {
  if (!ensureShopSelected()) {
    return;
  }
  const { success } = await voucherApi.create(
    {
      shopId: Number(selectedShop.value.id),
      title: forms.normal.title,
      subTitle: forms.normal.subTitle,
      rules: forms.normal.rules,
      payValue: Number(forms.normal.payValue),
      actualValue: Number(forms.normal.actualValue),
    },
    { successMessage: "普通券创建成功。" },
  );
  if (success) {
    goToSelectedShop();
  }
}

async function createSeckillVoucher() {
  if (!ensureShopSelected()) {
    return;
  }
  const { success } = await voucherApi.createSeckill(
    {
      shopId: Number(selectedShop.value.id),
      title: forms.seckill.title,
      subTitle: forms.seckill.subTitle,
      rules: forms.seckill.rules,
      payValue: Number(forms.seckill.payValue),
      actualValue: Number(forms.seckill.actualValue),
      stock: Number(forms.seckill.stock),
      beginTime: forms.seckill.beginTime,
      endTime: forms.seckill.endTime,
    },
    { successMessage: "秒杀券创建成功。" },
  );
  if (success) {
    goToSelectedShop();
  }
}

function resetNormalForm() {
  forms.normal = {
    title: "",
    subTitle: "",
    rules: "",
    payValue: "",
    actualValue: "",
  };
}

function resetSeckillForm() {
  forms.seckill = {
    title: "",
    subTitle: "",
    rules: "",
    payValue: "",
    actualValue: "",
    stock: "",
    beginTime: "",
    endTime: "",
  };
}
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h3 class="page-panel__title">创建优惠券</h3>
            <p class="page-panel__hint">先搜索并选择已有店铺，再创建普通券或秒杀券。</p>
          </div>
        </div>
      </template>

      <ElForm label-position="top">
        <ElFormItem label="绑定店铺">
          <ElSelect
            v-model="selectedShopId"
            filterable
            remote
            reserve-keyword
            clearable
            :remote-method="searchShops"
            :loading="shopSearchLoading"
            placeholder="输入店铺名称搜索"
            style="width: 100%"
            @change="selectShop"
            @clear="clearShop"
          >
            <ElOption
              v-for="shop in shopOptions"
              :key="shop.id"
              :label="`${shop.name} · ID ${shop.id}`"
              :value="shop.id"
            >
              <div class="voucher-shop-option">
                <strong>{{ shop.name }}</strong>
                <span>ID {{ shop.id }} · {{ shop.area || "未知商圈" }}</span>
              </div>
            </ElOption>
          </ElSelect>
        </ElFormItem>
      </ElForm>

      <ElAlert
        v-if="selectedShop"
        :title="`已绑定店铺：${selectedShop.name}（ID ${selectedShop.id}）`"
        type="success"
        show-icon
        :closable="false"
        class="voucher-shop-alert"
      />
      <ElAlert
        v-else
        title="请选择店铺后再创建优惠券"
        type="info"
        show-icon
        :closable="false"
        class="voucher-shop-alert"
      />

      <ElTabs v-model="activeTab">
        <ElTabPane label="普通券" name="normal">
          <ElForm label-position="top">
            <div class="page-grid-2">
              <ElFormItem label="标题">
                <ElInput v-model="forms.normal.title" />
              </ElFormItem>
              <ElFormItem label="副标题">
                <ElInput v-model="forms.normal.subTitle" />
              </ElFormItem>
              <ElFormItem label="规则">
                <ElInput v-model="forms.normal.rules" />
              </ElFormItem>
              <ElFormItem label="支付金额">
                <ElInput v-model="forms.normal.payValue" />
              </ElFormItem>
              <ElFormItem label="抵扣金额">
                <ElInput v-model="forms.normal.actualValue" />
              </ElFormItem>
            </div>
          </ElForm>

          <div class="filter-actions">
            <ElButton type="primary" @click="createNormalVoucher"
              >创建</ElButton
            >
            <ElButton @click="resetNormalForm">重置</ElButton>
          </div>
        </ElTabPane>

        <ElTabPane label="秒杀券" name="seckill">
          <ElForm label-position="top">
            <div class="page-grid-2">
              <ElFormItem label="标题">
                <ElInput v-model="forms.seckill.title" />
              </ElFormItem>
              <ElFormItem label="副标题">
                <ElInput v-model="forms.seckill.subTitle" />
              </ElFormItem>
              <ElFormItem label="规则">
                <ElInput v-model="forms.seckill.rules" />
              </ElFormItem>
              <ElFormItem label="支付金额">
                <ElInput v-model="forms.seckill.payValue" />
              </ElFormItem>
              <ElFormItem label="抵扣金额">
                <ElInput v-model="forms.seckill.actualValue" />
              </ElFormItem>
              <ElFormItem label="库存">
                <ElInput v-model="forms.seckill.stock" />
              </ElFormItem>
              <ElFormItem label="开始时间">
                <ElInput
                  v-model="forms.seckill.beginTime"
                  type="datetime-local"
                />
              </ElFormItem>
              <ElFormItem label="结束时间">
                <ElInput
                  v-model="forms.seckill.endTime"
                  type="datetime-local"
                />
              </ElFormItem>
            </div>
          </ElForm>

          <div class="filter-actions">
            <ElButton type="primary" @click="createSeckillVoucher"
              >创建</ElButton
            >
            <ElButton @click="resetSeckillForm">重置</ElButton>
          </div>
        </ElTabPane>
      </ElTabs>
    </ElCard>
  </section>
</template>

<style scoped>
.voucher-shop-alert {
  margin-bottom: 16px;
}

.voucher-shop-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.voucher-shop-option span {
  color: var(--app-text-secondary);
  font-size: 12px;
}
</style>
