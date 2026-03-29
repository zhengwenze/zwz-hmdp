<script setup>
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { voucherApi } from "../services/voucherApi";
import { buildRedirectPath, isAuthenticated } from "../stores/session";
import { formatPrice, formatVoucherWindow } from "../utils/view";

const router = useRouter();

const forms = reactive({
  shopId: "1",
  seckillId: "1",
  normal: {
    shopId: "1",
    title: "",
    subTitle: "",
    rules: "",
    payValue: "",
    actualValue: "",
    type: 0,
  },
  seckill: {
    shopId: "1",
    title: "",
    subTitle: "",
    rules: "",
    payValue: "",
    actualValue: "",
    type: 1,
    stock: "",
    beginTime: "",
    endTime: "",
  },
});

const vouchers = ref([]);
const latestOrderId = ref(null);
const activeTab = ref("normal");
const listPage = ref(1);
const listPageSize = 10;

const pagedVouchers = computed(() => {
  const start = (listPage.value - 1) * listPageSize;
  return vouchers.value.slice(start, start + listPageSize);
});

function jumpToLogin() {
  router.push(buildRedirectPath("/voucher"));
}

async function fetchVoucherList(shopId = forms.shopId) {
  forms.shopId = String(shopId);
  const { data, success } = await voucherApi.fetchList(shopId, {
    successMessage: "店铺券列表已更新。",
  });
  if (success) {
    vouchers.value = Array.isArray(data) ? data : [];
    listPage.value = 1;
  }
}

async function createNormalVoucher() {
  await voucherApi.create(
    {
      ...forms.normal,
      shopId: Number(forms.normal.shopId),
      payValue: Number(forms.normal.payValue),
      actualValue: Number(forms.normal.actualValue),
    },
    { successMessage: "普通券创建成功。" },
  );
}

async function createSeckillVoucher() {
  await voucherApi.createSeckill(
    {
      ...forms.seckill,
      shopId: Number(forms.seckill.shopId),
      payValue: Number(forms.seckill.payValue),
      actualValue: Number(forms.seckill.actualValue),
      stock: Number(forms.seckill.stock),
    },
    { successMessage: "秒杀券创建成功。" },
  );
}

async function seckillVoucher(voucherId = forms.seckillId) {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }
  forms.seckillId = String(voucherId);
  const { data, success } = await voucherApi.seckill(voucherId, {
    successMessage: "秒杀请求已发送。",
  });
  if (success) {
    latestOrderId.value = data ?? null;
  }
}

function resetNormalForm() {
  forms.normal = {
    shopId: forms.shopId,
    title: "",
    subTitle: "",
    rules: "",
    payValue: "",
    actualValue: "",
    type: 0,
  };
}

function resetSeckillForm() {
  forms.seckill = {
    shopId: forms.shopId,
    title: "",
    subTitle: "",
    rules: "",
    payValue: "",
    actualValue: "",
    type: 1,
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
            <h2 class="page-panel__title">优惠券管理</h2>
            <p class="page-panel__hint">
              列表区使用表格，创建区统一切到标签页表单。
            </p>
          </div>
          <ElTag :type="isAuthenticated() ? 'primary' : 'info'" effect="plain">
            {{ isAuthenticated() ? "可秒杀下单" : "秒杀下单需登录" }}
          </ElTag>
        </div>
      </template>

      <ElForm inline label-position="top">
        <ElFormItem label="店铺 ID">
          <ElInput v-model="forms.shopId" style="width: 180px;" />
        </ElFormItem>
      </ElForm>

      <div class="filter-actions">
        <ElButton type="primary" @click="fetchVoucherList()">查询店铺券</ElButton>
      </div>
    </ElCard>

    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h3 class="page-panel__title">店铺券列表</h3>
            <p class="page-panel__hint">使用统一表格风格展示优惠券。</p>
          </div>
          <ElTag effect="plain">{{ vouchers.length }} 条</ElTag>
        </div>
      </template>

      <ElTable :data="pagedVouchers" border stripe>
        <ElTableColumn prop="id" label="券 ID" width="96" />
        <ElTableColumn prop="title" label="标题" min-width="160" />
        <ElTableColumn prop="subTitle" label="副标题" min-width="180" />
        <ElTableColumn label="类型" width="110">
          <template #default="{ row }">
            <ElTag :type="row.type === 1 ? 'danger' : 'info'" effect="plain">
              {{ row.type === 1 ? "秒杀券" : "普通券" }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="支付 / 抵扣" min-width="180">
          <template #default="{ row }">
            {{ formatPrice(row.payValue) }} / {{ formatPrice(row.actualValue) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="有效期" min-width="220">
          <template #default="{ row }">
            {{ formatVoucherWindow(row) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <ElButton link type="primary" @click="forms.seckillId = String(row.id)">
              设为秒杀券
            </ElButton>
            <ElButton link type="danger" @click="seckillVoucher(row.id)">
              直接秒杀
            </ElButton>
          </template>
        </ElTableColumn>
        <template #empty>
          <ElEmpty description="先查询一个店铺的券列表。" />
        </template>
      </ElTable>

      <div class="page-table-footer">
        <ElPagination
          :current-page="listPage"
          :page-size="listPageSize"
          :total="vouchers.length"
          layout="prev, pager, next"
          @current-change="listPage = $event"
        />
      </div>
    </ElCard>

    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h3 class="page-panel__title">创建与秒杀</h3>
            <p class="page-panel__hint">普通券与秒杀券分开建表单，减少混杂字段。</p>
          </div>
          <ElTag effect="plain">最近订单 ID：{{ latestOrderId ?? "--" }}</ElTag>
        </div>
      </template>

      <ElTabs v-model="activeTab">
        <ElTabPane label="普通券" name="normal">
          <ElForm label-position="top">
            <div class="page-grid-2">
              <ElFormItem label="店铺 ID">
                <ElInput v-model="forms.normal.shopId" />
              </ElFormItem>
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
            <ElButton type="primary" @click="createNormalVoucher">创建普通券</ElButton>
            <ElButton @click="resetNormalForm">重置</ElButton>
          </div>
        </ElTabPane>

        <ElTabPane label="秒杀券" name="seckill">
          <ElForm label-position="top">
            <div class="page-grid-2">
              <ElFormItem label="店铺 ID">
                <ElInput v-model="forms.seckill.shopId" />
              </ElFormItem>
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
                <ElInput v-model="forms.seckill.beginTime" type="datetime-local" />
              </ElFormItem>
              <ElFormItem label="结束时间">
                <ElInput v-model="forms.seckill.endTime" type="datetime-local" />
              </ElFormItem>
              <ElFormItem label="秒杀券 ID">
                <ElInput v-model="forms.seckillId" />
              </ElFormItem>
            </div>
          </ElForm>

          <div class="filter-actions">
            <ElButton type="primary" @click="createSeckillVoucher">创建秒杀券</ElButton>
            <ElButton type="danger" plain @click="seckillVoucher()">发起秒杀</ElButton>
            <ElButton @click="resetSeckillForm">重置</ElButton>
          </div>
        </ElTabPane>
      </ElTabs>
    </ElCard>
  </section>
</template>
