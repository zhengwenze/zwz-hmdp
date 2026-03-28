<script setup>
import { reactive } from "vue";
import {
  sharedState,
  send,
  isLoading,
  endpointBadgeClass,
  buildVoucherPayload,
} from "../stores/sharedState";

const forms = reactive({
  voucher: {
    shopId: "1",
    normal: {
      shopId: "1",
      title: "",
      subTitle: "",
      rules: "",
      payValue: "",
      actualValue: "",
      type: "0",
    },
    seckill: {
      shopId: "1",
      title: "",
      subTitle: "",
      rules: "",
      payValue: "",
      actualValue: "",
      type: "1",
      stock: "",
      beginTime: "",
      endTime: "",
    },
    seckillId: "1",
  },
});

async function createNormalVoucher() {
  await send(
    "POST /voucher",
    { method: "POST", path: "/voucher", body: buildVoucherPayload(forms.voucher.normal) },
    { successMessage: "普通券创建成功。" },
  );
}

async function createSeckillVoucher() {
  await send(
    "POST /voucher/seckill",
    { method: "POST", path: "/voucher/seckill", body: buildVoucherPayload(forms.voucher.seckill, true) },
    { successMessage: "秒杀券创建成功。" },
  );
}

async function fetchVoucherList(shopId = forms.voucher.shopId) {
  forms.voucher.shopId = String(shopId);
  await send(
    "GET /voucher/list/{shopId}",
    { method: "GET", path: `/voucher/list/${shopId}` },
    {
      successMessage: "优惠券列表已更新。",
      onSuccess: (data) => {
        sharedState.vouchers.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function seckillVoucher(voucherId = forms.voucher.seckillId) {
  forms.voucher.seckillId = String(voucherId);
  await send(
    "POST /voucher-order/seckill/{id}",
    { method: "POST", path: `/voucher-order/seckill/${voucherId}` },
    {
      successMessage: "秒杀下单请求已发送。",
      onSuccess: (data) => {
        sharedState.seckillOrderId.value = data ?? null;
      },
    },
  );
}
</script>

<template>
  <section id="vouchers" class="module-section">
    <div class="section-title">
      <div>
        <p class="eyebrow">Voucher APIs</p>
        <h2>优惠券与秒杀</h2>
      </div>
      <span class="section-hint">普通券、秒杀券、店铺券列表、秒杀下单全接入</span>
    </div>

    <div class="card-grid two">
      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>店铺券列表</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /voucher/list/{shopId}'])">/voucher/list/{shopId}</span>
        </div>
        <div class="form-grid single">
          <label><span>店铺 ID</span><input v-model="forms.voucher.shopId" /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('GET /voucher/list/{shopId}')" @click="fetchVoucherList()">查询店铺券</button>
        </div>
        <div class="voucher-grid">
          <article v-for="voucher in sharedState.vouchers.value" :key="voucher.id" class="voucher-card">
            <div class="blog-card-head">
              <strong>{{ voucher.title }}</strong>
              <span class="ue-stamp">{{ voucher.type === 1 ? "秒杀券" : "普通券" }}</span>
            </div>
            <p>支付 {{ voucher.payValue }} / 抵扣 {{ voucher.actualValue }}</p>
            <p>库存 {{ voucher.stock ?? "--" }} · 生效 {{ voucher.beginTime || "--" }}</p>
            <div class="button-row tight">
              <button class="secondary" @click="forms.voucher.seckillId = String(voucher.id)">设为秒杀 ID</button>
              <button class="secondary" @click="seckillVoucher(voucher.id)">立即秒杀</button>
            </div>
          </article>
        </div>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>新增普通券</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['POST /voucher'])">POST /voucher</span>
        </div>
        <div class="form-grid double">
          <label><span>店铺 ID</span><input v-model="forms.voucher.normal.shopId" /></label>
          <label><span>标题</span><input v-model="forms.voucher.normal.title" /></label>
          <label><span>副标题</span><input v-model="forms.voucher.normal.subTitle" /></label>
          <label><span>规则</span><input v-model="forms.voucher.normal.rules" /></label>
          <label><span>支付金额</span><input v-model="forms.voucher.normal.payValue" /></label>
          <label><span>抵扣金额</span><input v-model="forms.voucher.normal.actualValue" /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('POST /voucher')" @click="createNormalVoucher">创建普通券</button>
        </div>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card full">
        <div class="panel-head">
          <h3>新增秒杀券 + 秒杀下单</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['POST /voucher/seckill'])">POST /voucher/seckill</span>
        </div>
        <div class="form-grid quad">
          <label><span>店铺 ID</span><input v-model="forms.voucher.seckill.shopId" /></label>
          <label><span>标题</span><input v-model="forms.voucher.seckill.title" /></label>
          <label><span>副标题</span><input v-model="forms.voucher.seckill.subTitle" /></label>
          <label><span>规则</span><input v-model="forms.voucher.seckill.rules" /></label>
          <label><span>支付金额</span><input v-model="forms.voucher.seckill.payValue" /></label>
          <label><span>抵扣金额</span><input v-model="forms.voucher.seckill.actualValue" /></label>
          <label><span>库存</span><input v-model="forms.voucher.seckill.stock" /></label>
          <label><span>开始时间</span><input v-model="forms.voucher.seckill.beginTime" type="datetime-local" /></label>
          <label><span>结束时间</span><input v-model="forms.voucher.seckill.endTime" type="datetime-local" /></label>
          <label><span>秒杀券 ID</span><input v-model="forms.voucher.seckillId" /></label>
        </div>
        <div class="button-row wrap">
          <button :disabled="isLoading('POST /voucher/seckill')" @click="createSeckillVoucher">创建秒杀券</button>
          <button :disabled="isLoading('POST /voucher-order/seckill/{id}')" class="accent" @click="seckillVoucher()">发起秒杀下单</button>
        </div>
        <div class="inline-stats">
          <div>
            <span class="label">最近秒杀订单 ID</span>
            <strong>{{ sharedState.seckillOrderId.value ?? "--" }}</strong>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>
