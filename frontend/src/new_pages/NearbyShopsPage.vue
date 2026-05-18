<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { shopApi } from "../services/shopApi";
import { rememberShop } from "../stores/historyState";
import { firstImage, formatDistance, formatPrice } from "../utils/view";

const PAGE_SIZE = 5;

const route = useRoute();
const router = useRouter();

const forms = reactive({
  typeId: "",
  current: "1",
  x: "",
  y: "",
});

const loading = ref(false);
const locating = ref(false);
const shopTypes = ref([]);
const nearbyShops = ref([]);

const hasCoordinates = computed(() => forms.x !== "" && forms.y !== "");
const hasPartialCoordinates = computed(() => forms.x !== "" || forms.y !== "");
const pageCount = computed(
  () => Number(forms.current) + (nearbyShops.value.length === PAGE_SIZE ? 1 : 0),
);
const selectedTypeName = computed(() => {
  const selected = shopTypes.value.find(
    (type) => String(type.id) === String(forms.typeId),
  );
  return selected?.name || "当前分类";
});

function asString(value, fallback = "") {
  return typeof value === "string" ? value : fallback;
}

function applyQueryState() {
  forms.typeId = asString(route.query.typeId, forms.typeId);
  forms.current = asString(route.query.current, forms.current);
  forms.x = asString(route.query.x);
  forms.y = asString(route.query.y);
}

function buildQuery(overrides = {}) {
  return {
    typeId: forms.typeId || undefined,
    current: forms.current || "1",
    x: forms.x || undefined,
    y: forms.y || undefined,
    ...overrides,
  };
}

function replaceQuery(overrides = {}) {
  return router.replace({
    name: "nearby-shops",
    query: buildQuery(overrides),
  });
}

function normalizeNumber(value) {
  return Number(String(value).trim());
}

function validateCoordinates() {
  const x = normalizeNumber(forms.x);
  const y = normalizeNumber(forms.y);

  if (!Number.isFinite(x) || !Number.isFinite(y)) {
    ElMessage.warning("请输入有效的经纬度");
    return false;
  }
  if (x < -180 || x > 180 || y < -90 || y > 90) {
    ElMessage.warning("经度范围为 -180 到 180，纬度范围为 -90 到 90");
    return false;
  }
  return true;
}

async function loadShopTypes() {
  const { data, success } = await shopApi.fetchTypes({ silentError: true });
  if (!success) {
    return;
  }
  shopTypes.value = Array.isArray(data) ? data : [];
  if (!forms.typeId && shopTypes.value.length) {
    forms.typeId = String(shopTypes.value[0].id);
  }
}

async function loadNearbyShops(options = {}) {
  if (!forms.typeId) {
    ElMessage.warning("请选择商铺分类");
    return;
  }
  if (hasPartialCoordinates.value && !validateCoordinates()) {
    return;
  }

  if (options.resetPage) {
    forms.current = "1";
  }
  if (options.persist !== false) {
    await replaceQuery();
  }

  loading.value = true;
  try {
    const query = {
      typeId: forms.typeId,
      current: forms.current,
    };
    if (hasCoordinates.value) {
      query.x = normalizeNumber(forms.x);
      query.y = normalizeNumber(forms.y);
    }
    const { data, success } = await shopApi.fetchByType(
      query,
      options.notify ? { successMessage: "查询完成" } : { silentError: true },
    );
    if (success) {
      nearbyShops.value = Array.isArray(data) ? data : [];
    }
  } finally {
    loading.value = false;
  }
}

function useCurrentLocation() {
  if (!navigator.geolocation) {
    ElMessage.warning("当前浏览器不支持定位");
    return;
  }

  locating.value = true;
  navigator.geolocation.getCurrentPosition(
    (position) => {
      forms.x = position.coords.longitude.toFixed(6);
      forms.y = position.coords.latitude.toFixed(6);
      locating.value = false;
      loadNearbyShops({ resetPage: true, notify: true });
    },
    () => {
      locating.value = false;
      ElMessage.warning("无法获取当前位置，请手动输入经纬度");
    },
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 60000,
    },
  );
}

async function clearCoordinates() {
  forms.x = "";
  forms.y = "";
  forms.current = "1";
  await replaceQuery({ x: undefined, y: undefined, current: "1" });
  await loadNearbyShops({ persist: false, notify: true });
}

function handleTypeChange() {
  forms.current = "1";
  loadNearbyShops({ notify: true });
}

function changePage(page) {
  forms.current = String(page);
  loadNearbyShops({ notify: true });
}

function openShopDetail(shop) {
  rememberShop(shop);
  router.push({
    name: "shop-detail",
    params: { id: shop.id },
    query: buildQuery(),
  });
}

onMounted(async () => {
  applyQueryState();
  await loadShopTypes();
  if (forms.typeId) {
    await loadNearbyShops({ persist: false });
  }
});
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h2 class="page-panel__title">附近商铺</h2>
            <p class="page-panel__hint">
              {{ selectedTypeName }} · Redis GEO 距离排序
            </p>
          </div>
          <ElTag effect="plain">{{ nearbyShops.length }} 条</ElTag>
        </div>
      </template>

      <ElCard shadow="never" class="page-panel filter-panel">
        <ElForm inline label-position="top">
          <ElFormItem label="分类">
            <ElSelect
              v-model="forms.typeId"
              class="type-select"
              @change="handleTypeChange"
            >
              <ElOption
                v-for="type in shopTypes"
                :key="type.id"
                :label="type.name"
                :value="String(type.id)"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="经度 x">
            <ElInput
              v-model="forms.x"
              class="coordinate-input"
              placeholder="例如 121.499"
              @keyup.enter="loadNearbyShops({ resetPage: true, notify: true })"
            />
          </ElFormItem>
          <ElFormItem label="纬度 y">
            <ElInput
              v-model="forms.y"
              class="coordinate-input"
              placeholder="例如 31.239"
              @keyup.enter="loadNearbyShops({ resetPage: true, notify: true })"
            />
          </ElFormItem>
        </ElForm>

        <div class="filter-actions">
          <ElButton
            type="primary"
            :loading="loading"
            @click="loadNearbyShops({ resetPage: true, notify: true })"
          >
            查询附近商铺
          </ElButton>
          <ElButton :loading="locating" @click="useCurrentLocation">
            使用当前位置
          </ElButton>
          <ElButton @click="clearCoordinates">清空坐标</ElButton>
        </div>
      </ElCard>

      <ElTable v-loading="loading" :data="nearbyShops" border stripe>
        <ElTableColumn label="封面" width="100">
          <template #default="{ row }">
            <div class="thumb-image">
              <ElImage
                v-if="firstImage(row.images)"
                :src="firstImage(row.images)"
                fit="cover"
              />
              <div v-else class="thumb-image" />
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="name" label="商铺名" min-width="180" />
        <ElTableColumn prop="area" label="商圈" min-width="120" />
        <ElTableColumn label="距离" width="120">
          <template #default="{ row }">
            <ElTag type="success" effect="plain">
              {{ formatDistance(row.distance) }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="评分" width="100">
          <template #default="{ row }">
            {{ ((row.score || 0) / 10).toFixed(1) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="均价" width="110">
          <template #default="{ row }">
            ￥{{ formatPrice(row.avgPrice) }}
          </template>
        </ElTableColumn>
        <ElTableColumn prop="address" label="地址" min-width="220" />
        <ElTableColumn label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <ElButton link type="primary" @click="openShopDetail(row)">
              查看详情
            </ElButton>
          </template>
        </ElTableColumn>
        <template #empty>
          <ElEmpty
            :description="
              hasCoordinates
                ? '暂无附近商铺数据'
                : '暂无该分类商铺数据'
            "
          />
        </template>
      </ElTable>

      <div class="page-table-footer">
        <ElPagination
          :current-page="Number(forms.current)"
          :page-size="PAGE_SIZE"
          :page-count="Math.max(pageCount, 1)"
          layout="prev, pager, next"
          @current-change="changePage"
        />
      </div>
    </ElCard>
  </section>
</template>

<style scoped>
.filter-panel {
  margin-bottom: 18px;
}

.type-select {
  width: 180px;
}

.coordinate-input {
  width: 180px;
}

@media (max-width: 720px) {
  .type-select,
  .coordinate-input {
    width: 100%;
  }
}
</style>
