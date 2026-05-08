<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { shopApi } from "../services/shopApi";
import { firstImage, formatPrice } from "../utils/view";
import { rememberShop } from "../stores/historyState";

const PAGE_SIZE = 10;
const route = useRoute();
const router = useRouter();

const forms = reactive({
  typeId: "1",
  current: "1",
  x: "",
  y: "",
  name: "",
  nameCurrent: "1",
});

const shopTypes = ref([]);
const shopsByType = ref([]);
const shopsByName = ref([]);
const activeTab = ref("type");

const typePageCount = computed(
  () =>
    Number(forms.current) + (shopsByType.value.length === PAGE_SIZE ? 1 : 0),
);
const namePageCount = computed(
  () =>
    Number(forms.nameCurrent) +
    (shopsByName.value.length === PAGE_SIZE ? 1 : 0),
);

function asString(value, fallback = "") {
  return typeof value === "string" ? value : fallback;
}

function applyQueryState() {
  activeTab.value = route.query.tab === "name" ? "name" : "type";
  forms.typeId = asString(route.query.typeId, forms.typeId);
  forms.current = asString(route.query.current, forms.current);
  forms.x = asString(route.query.x);
  forms.y = asString(route.query.y);
  forms.name = asString(route.query.name);
  forms.nameCurrent = asString(route.query.nameCurrent, forms.nameCurrent);
}

function buildListQuery(overrides = {}) {
  return {
    tab: activeTab.value,
    typeId: forms.typeId || undefined,
    current: forms.current || "1",
    x: forms.x || undefined,
    y: forms.y || undefined,
    name: forms.name || undefined,
    nameCurrent: forms.nameCurrent || "1",
    ...overrides,
  };
}

function replaceListQuery(overrides = {}) {
  return router.replace({
    name: "shop-list",
    query: buildListQuery(overrides),
  });
}

async function loadShopTypes() {
  const { data, success } = await shopApi.fetchTypes({ silentError: true });
  if (success) {
    shopTypes.value = Array.isArray(data) ? data : [];
    if (!forms.typeId && shopTypes.value.length) {
      forms.typeId = String(shopTypes.value[0].id);
    }
  }
}

async function queryShopsByType(options = {}) {
  activeTab.value = "type";
  if (options.persist !== false) {
    await replaceListQuery({ tab: "type" });
  }
  const { data, success } = await shopApi.fetchByType(
    {
      typeId: forms.typeId,
      current: forms.current,
      x: forms.x || undefined,
      y: forms.y || undefined,
    },
    options.notify ? { successMessage: "操作成功" } : { silentError: true },
  );
  if (success) {
    shopsByType.value = Array.isArray(data) ? data : [];
  }
}

async function queryShopsByName(options = {}) {
  activeTab.value = "name";
  if (options.persist !== false) {
    await replaceListQuery({ tab: "name" });
  }
  const { data, success } = await shopApi.fetchByName(
    { name: forms.name, current: forms.nameCurrent },
    options.notify ? { successMessage: "操作成功" } : { silentError: true },
  );
  if (success) {
    shopsByName.value = Array.isArray(data) ? data : [];
  }
}

function openShopDetail(shop) {
  rememberShop(shop);
  router.push({
    name: "shop-detail",
    params: { id: shop.id },
    query: buildListQuery(),
  });
}

function resetTypeFilters() {
  activeTab.value = "type";
  forms.current = "1";
  forms.x = "";
  forms.y = "";
  if (shopTypes.value.length) {
    forms.typeId = String(shopTypes.value[0].id);
  }
  queryShopsByType({ notify: true });
}

function resetNameFilters() {
  activeTab.value = "name";
  forms.name = "";
  forms.nameCurrent = "1";
  shopsByName.value = [];
  replaceListQuery({ tab: "name", name: undefined, nameCurrent: "1" });
}

function changeTypePage(page) {
  forms.current = String(page);
  queryShopsByType({ notify: true });
}

function changeNamePage(page) {
  forms.nameCurrent = String(page);
  queryShopsByName({ notify: true });
}

function handleTabChange(tabName) {
  activeTab.value = tabName === "name" ? "name" : "type";
  if (activeTab.value === "type") {
    queryShopsByType();
    return;
  }
  if (forms.name) {
    queryShopsByName();
    return;
  }
  replaceListQuery({ tab: "name" });
}

onMounted(async () => {
  applyQueryState();
  await loadShopTypes();
  if (activeTab.value === "name") {
    await queryShopsByName({ persist: false });
    return;
  }
  await queryShopsByType({ persist: false });
});
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h2 class="page-panel__title">商铺管理</h2>
          </div>
        </div>
      </template>

      <ElTabs v-model="activeTab" @tab-change="handleTabChange">
        <ElTabPane label="按分类查询" name="type">
          <ElCard shadow="never" class="page-panel">
            <ElForm inline label-position="top">
              <ElFormItem label="分类">
                <ElSelect v-model="forms.typeId" style="width: 180px">
                  <ElOption
                    v-for="type in shopTypes"
                    :key="type.id"
                    :label="type.name"
                    :value="String(type.id)"
                  />
                </ElSelect>
              </ElFormItem>
              <ElFormItem label="页码">
                <ElInput v-model="forms.current" style="width: 120px" />
              </ElFormItem>
              <ElFormItem label="经度 x">
                <ElInput
                  v-model="forms.x"
                  placeholder="可选"
                  style="width: 160px"
                />
              </ElFormItem>
              <ElFormItem label="纬度 y">
                <ElInput
                  v-model="forms.y"
                  placeholder="可选"
                  style="width: 160px"
                />
              </ElFormItem>
            </ElForm>

            <div class="filter-actions">
              <ElButton
                type="primary"
                @click="queryShopsByType({ notify: true })"
                >查询</ElButton
              >
              <ElButton @click="resetTypeFilters">重置</ElButton>
            </div>
          </ElCard>

          <ElCard shadow="never" class="page-panel">
            <template #header>
              <div class="page-panel__header">
                <div>
                  <h3 class="page-panel__title">表格区</h3>
                  <p class="page-panel__hint">
                    统一使用 Element Plus Table 承载列表结果。
                  </p>
                </div>
                <ElTag effect="plain">{{ shopsByType.length }} 条</ElTag>
              </div>
            </template>

            <ElTable :data="shopsByType" border stripe>
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
              <ElTableColumn prop="id" label="ID" width="88" />
              <ElTableColumn prop="name" label="商铺名" min-width="180" />
              <ElTableColumn prop="area" label="商圈" min-width="140" />
              <ElTableColumn label="均价" width="120">
                <template #default="{ row }">
                  ￥{{ formatPrice(row.avgPrice) }}
                </template>
              </ElTableColumn>
              <ElTableColumn label="评分" width="120">
                <template #default="{ row }">
                  {{ ((row.score || 0) / 10).toFixed(1) }}
                </template>
              </ElTableColumn>
              <ElTableColumn
                prop="openHours"
                label="营业时间"
                min-width="180"
              />
              <ElTableColumn label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <ElButton link type="primary" @click="openShopDetail(row)">
                    查看详情
                  </ElButton>
                </template>
              </ElTableColumn>
              <template #empty>
                <ElEmpty description="暂无分类商铺数据" />
              </template>
            </ElTable>

            <div class="page-table-footer">
              <ElPagination
                :current-page="Number(forms.current)"
                :page-size="PAGE_SIZE"
                :page-count="Math.max(typePageCount, 1)"
                layout="prev, pager, next"
                @current-change="changeTypePage"
              />
            </div>
          </ElCard>
        </ElTabPane>

        <ElTabPane label="按名称查询" name="name">
          <ElCard shadow="never" class="page-panel">
            <ElForm inline label-position="top">
              <ElFormItem label="关键词">
                <ElInput
                  v-model="forms.name"
                  placeholder="输入店名"
                  style="width: 240px"
                  @keyup.enter="queryShopsByName({ notify: true })"
                />
              </ElFormItem>
              <ElFormItem label="页码">
                <ElInput v-model="forms.nameCurrent" style="width: 120px" />
              </ElFormItem>
            </ElForm>

            <div class="filter-actions">
              <ElButton
                type="primary"
                @click="queryShopsByName({ notify: true })"
                >查询</ElButton
              >
              <ElButton @click="resetNameFilters">重置</ElButton>
            </div>
          </ElCard>

          <ElCard shadow="never" class="page-panel">
            <template #header>
              <div class="page-panel__header">
                <div>
                  <h3 class="page-panel__title">表格区</h3>
                  <p class="page-panel__hint">关键词搜索结果统一为表格展示。</p>
                </div>
                <ElTag effect="plain">{{ shopsByName.length }} 条</ElTag>
              </div>
            </template>

            <ElTable :data="shopsByName" border stripe>
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
              <ElTableColumn prop="id" label="ID" width="88" />
              <ElTableColumn prop="name" label="商铺名" min-width="180" />
              <ElTableColumn prop="area" label="商圈" min-width="140" />
              <ElTableColumn prop="address" label="地址" min-width="220" />
              <ElTableColumn label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <ElButton link type="primary" @click="openShopDetail(row)">
                    查看详情
                  </ElButton>
                </template>
              </ElTableColumn>
              <template #empty>
                <ElEmpty description="暂无名称搜索结果" />
              </template>
            </ElTable>

            <div class="page-table-footer">
              <ElPagination
                :current-page="Number(forms.nameCurrent)"
                :page-size="PAGE_SIZE"
                :page-count="Math.max(namePageCount, 1)"
                layout="prev, pager, next"
                @current-change="changeNamePage"
              />
            </div>
          </ElCard>
        </ElTabPane>
      </ElTabs>
    </ElCard>
  </section>
</template>
