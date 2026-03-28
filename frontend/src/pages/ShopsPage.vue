<script setup>
import { reactive } from "vue";
import {
  sharedState,
  send,
  isLoading,
  endpointBadgeClass,
  toAssetUrl,
  splitImages,
  buildShopPayload,
} from "../stores/sharedState";

const forms = reactive({
  shop: {
    typeId: "1",
    current: "1",
    x: "",
    y: "",
    name: "",
    nameCurrent: "1",
    detailId: "1",
    create: {
      name: "",
      typeId: "1",
      images: "",
      area: "",
      address: "",
      x: "",
      y: "",
      avgPrice: "",
      sold: "",
      comments: "",
      score: "",
      openHours: "10:00-22:00",
    },
    update: {
      id: "",
      name: "",
      typeId: "",
      images: "",
      area: "",
      address: "",
      x: "",
      y: "",
      avgPrice: "",
      sold: "",
      comments: "",
      score: "",
      openHours: "",
    },
  },
});

async function queryShopsByType() {
  await send(
    "GET /shop/of/type",
    {
      method: "GET",
      path: "/shop/of/type",
      query: {
        typeId: forms.shop.typeId,
        current: forms.shop.current,
        x: forms.shop.x,
        y: forms.shop.y,
      },
    },
    {
      successMessage: "分类商铺列表已更新。",
      onSuccess: (data) => {
        sharedState.shopsByType.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function queryShopsByName() {
  await send(
    "GET /shop/of/name",
    {
      method: "GET",
      path: "/shop/of/name",
      query: {
        name: forms.shop.name,
        current: forms.shop.nameCurrent,
      },
    },
    {
      successMessage: "名称搜索结果已更新。",
      onSuccess: (data) => {
        sharedState.shopsByName.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function fetchShopDetail(shopId = forms.shop.detailId) {
  forms.shop.detailId = String(shopId);
  await send(
    "GET /shop/{id}",
    { method: "GET", path: `/shop/${shopId}` },
    {
      successMessage: "商铺详情已更新。",
      onSuccess: (data) => {
        sharedState.selectedShop.value = data || null;
      },
    },
  );
}

async function createShop() {
  await send(
    "POST /shop",
    { method: "POST", path: "/shop", body: buildShopPayload(forms.shop.create) },
    { successMessage: "新增商铺请求已发送。后端不会返回新商铺 ID。" },
  );
}

async function updateShop() {
  await send(
    "PUT /shop",
    { method: "PUT", path: "/shop", body: buildShopPayload(forms.shop.update) },
    {
      successMessage: "商铺更新成功。",
      onSuccess: async () => {
        if (forms.shop.update.id) {
          await fetchShopDetail(forms.shop.update.id);
        }
      },
    },
  );
}
</script>

<template>
  <section id="shops" class="module-section">
    <div class="section-title">
      <div>
        <p class="eyebrow">Shop APIs</p>
        <h2>商铺模块</h2>
      </div>
      <span class="section-hint">查询、创建、更新、选中店铺上下文</span>
    </div>

    <div class="card-grid two">
      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>按分类查询商铺</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /shop/of/type'])">/shop/of/type</span>
        </div>
        <div class="form-grid quad">
          <label><span>分类 ID</span><input v-model="forms.shop.typeId" /></label>
          <label><span>页码 current</span><input v-model="forms.shop.current" /></label>
          <label><span>经度 x</span><input v-model="forms.shop.x" placeholder="可选" /></label>
          <label><span>纬度 y</span><input v-model="forms.shop.y" placeholder="可选" /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('GET /shop/of/type')" @click="queryShopsByType">查询分类商铺</button>
        </div>
        <div class="shop-grid">
          <article v-for="shop in sharedState.shopsByType.value" :key="`type-${shop.id}`" class="shop-card">
            <img v-if="splitImages(shop.images)[0]" :src="toAssetUrl(splitImages(shop.images)[0])" :alt="shop.name" />
            <div>
              <strong>{{ shop.name }}</strong>
              <p>{{ shop.area }} · {{ shop.openHours }}</p>
              <p>评分 {{ shop.score }} · 距离 {{ shop.distance ?? "--" }}</p>
            </div>
            <div class="button-row tight">
              <button class="secondary" @click="fetchShopDetail(shop.id)">详情</button>
            </div>
          </article>
        </div>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>按名称搜索商铺</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /shop/of/name'])">/shop/of/name</span>
        </div>
        <div class="form-grid double">
          <label><span>商铺名称</span><input v-model="forms.shop.name" /></label>
          <label><span>页码 current</span><input v-model="forms.shop.nameCurrent" /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('GET /shop/of/name')" @click="queryShopsByName">搜索商铺</button>
        </div>
        <div class="shop-grid">
          <article v-for="shop in sharedState.shopsByName.value" :key="`name-${shop.id}`" class="shop-card">
            <img v-if="splitImages(shop.images)[0]" :src="toAssetUrl(splitImages(shop.images)[0])" :alt="shop.name" />
            <div>
              <strong>{{ shop.name }}</strong>
              <p>{{ shop.area }} · {{ shop.openHours }}</p>
            </div>
            <div class="button-row tight">
              <button class="secondary" @click="fetchShopDetail(shop.id)">详情</button>
            </div>
          </article>
        </div>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>商铺详情</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /shop/{id}'])">/shop/{id}</span>
        </div>
        <div class="form-grid single">
          <label><span>商铺 ID</span><input v-model="forms.shop.detailId" /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('GET /shop/{id}')" @click="fetchShopDetail()">查询详情</button>
        </div>
        <pre class="json-box">{{ JSON.stringify(sharedState.selectedShop.value || { message: "尚未查询商铺详情" }, null, 2) }}</pre>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>新增商铺</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['POST /shop'])">POST /shop</span>
        </div>
        <div class="form-grid quad">
          <label><span>名称</span><input v-model="forms.shop.create.name" /></label>
          <label><span>分类 ID</span><input v-model="forms.shop.create.typeId" /></label>
          <label><span>商圈</span><input v-model="forms.shop.create.area" /></label>
          <label><span>营业时间</span><input v-model="forms.shop.create.openHours" /></label>
          <label><span>地址</span><input v-model="forms.shop.create.address" /></label>
          <label><span>图片（逗号分隔）</span><input v-model="forms.shop.create.images" /></label>
          <label><span>经度 x</span><input v-model="forms.shop.create.x" /></label>
          <label><span>纬度 y</span><input v-model="forms.shop.create.y" /></label>
          <label><span>均价</span><input v-model="forms.shop.create.avgPrice" /></label>
          <label><span>销量</span><input v-model="forms.shop.create.sold" /></label>
          <label><span>评论数</span><input v-model="forms.shop.create.comments" /></label>
          <label><span>评分</span><input v-model="forms.shop.create.score" /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('POST /shop')" @click="createShop">新增商铺</button>
        </div>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card full">
        <div class="panel-head">
          <h3>更新商铺</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['PUT /shop'])">PUT /shop</span>
        </div>
        <div class="form-grid quad">
          <label><span>ID</span><input v-model="forms.shop.update.id" /></label>
          <label><span>名称</span><input v-model="forms.shop.update.name" /></label>
          <label><span>分类 ID</span><input v-model="forms.shop.update.typeId" /></label>
          <label><span>营业时间</span><input v-model="forms.shop.update.openHours" /></label>
          <label><span>商圈</span><input v-model="forms.shop.update.area" /></label>
          <label><span>地址</span><input v-model="forms.shop.update.address" /></label>
          <label><span>图片</span><input v-model="forms.shop.update.images" /></label>
          <label><span>经度 x</span><input v-model="forms.shop.update.x" /></label>
          <label><span>纬度 y</span><input v-model="forms.shop.update.y" /></label>
          <label><span>均价</span><input v-model="forms.shop.update.avgPrice" /></label>
          <label><span>销量</span><input v-model="forms.shop.update.sold" /></label>
          <label><span>评论数</span><input v-model="forms.shop.update.comments" /></label>
          <label><span>评分</span><input v-model="forms.shop.update.score" /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('PUT /shop')" @click="updateShop">更新商铺</button>
        </div>
      </article>
    </div>
  </section>
</template>
