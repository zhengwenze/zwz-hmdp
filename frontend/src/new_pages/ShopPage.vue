<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { shopApi } from "../services/shopApi";
import { voucherApi } from "../services/voucherApi";
import {
  firstImage,
  formatPrice,
  formatVoucherWindow,
  voucherState,
} from "../utils/view";
import { rememberShop } from "../stores/historyState";

const PAGE_SIZE = 10;

const forms = reactive({
  typeId: "1",
  current: "1",
  x: "",
  y: "",
  name: "",
  nameCurrent: "1",
  detailId: "1",
});

const shopTypes = ref([]);
const shopsByType = ref([]);
const shopsByName = ref([]);
const selectedShop = ref(null);
const vouchers = ref([]);
const activeTab = ref("type");

const typePageCount = computed(
  () => Number(forms.current) + (shopsByType.value.length === PAGE_SIZE ? 1 : 0),
);
const namePageCount = computed(
  () =>
    Number(forms.nameCurrent)
    + (shopsByName.value.length === PAGE_SIZE ? 1 : 0),
);

async function loadShopTypes() {
  const { data, success } = await shopApi.fetchTypes({ silentError: true });
  if (success) {
    shopTypes.value = Array.isArray(data) ? data : [];
    if (!forms.typeId && shopTypes.value.length) {
      forms.typeId = String(shopTypes.value[0].id);
    }
  }
}

async function queryShopsByType() {
  const { data, success } = await shopApi.fetchByType(
    {
      typeId: forms.typeId,
      current: forms.current,
      x: forms.x || undefined,
      y: forms.y || undefined,
    },
    { successMessage: "分类商铺列表已更新。" },
  );
  if (success) {
    shopsByType.value = Array.isArray(data) ? data : [];
  }
}

async function queryShopsByName() {
  const { data, success } = await shopApi.fetchByName(
    { name: forms.name, current: forms.nameCurrent },
    { successMessage: "名称搜索结果已更新。" },
  );
  if (success) {
    shopsByName.value = Array.isArray(data) ? data : [];
  }
}

async function fetchShopDetail(shopId = forms.detailId) {
  forms.detailId = String(shopId);
  const [shopResult, voucherResult] = await Promise.all([
    shopApi.fetchDetail(shopId, { silentError: true }),
    voucherApi.fetchList(shopId, { silentError: true }),
  ]);

  if (shopResult.success) {
    selectedShop.value = shopResult.data || null;
    rememberShop(shopResult.data);
  }
  if (voucherResult.success) {
    vouchers.value = Array.isArray(voucherResult.data) ? voucherResult.data : [];
  }
}

function resetTypeFilters() {
  forms.current = "1";
  forms.x = "";
  forms.y = "";
  if (shopTypes.value.length) {
    forms.typeId = String(shopTypes.value[0].id);
  }
  queryShopsByType();
}

function resetNameFilters() {
  forms.name = "";
  forms.nameCurrent = "1";
  shopsByName.value = [];
}

function changeTypePage(page) {
  forms.current = String(page);
  queryShopsByType();
}

function changeNamePage(page) {
  forms.nameCurrent = String(page);
  queryShopsByName();
}

onMounted(async () => {
  await loadShopTypes();
  await queryShopsByType();
});
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h2 class="page-panel__title">商铺列表模板页</h2>
            <p class="page-panel__hint">
              这张页作为标准列表页示例，统一包含筛选区、表格区和分页区。
            </p>
          </div>
          <div class="page-actions">
            <ElButton @click="loadShopTypes">刷新分类</ElButton>
            <ElButton type="primary" plain @click="fetchShopDetail()">
              查询详情
            </ElButton>
          </div>
        </div>
      </template>

      <ElTabs v-model="activeTab">
        <ElTabPane label="按分类查询" name="type">
          <ElCard shadow="never" class="page-panel">
            <template #header>
              <div class="page-panel__header">
                <div>
                  <h3 class="page-panel__title">筛选区</h3>
                  <p class="page-panel__hint">统一使用 Element Plus 表单组织查询条件。</p>
                </div>
              </div>
            </template>

            <ElForm inline label-position="top">
              <ElFormItem label="分类">
                <ElSelect v-model="forms.typeId" style="width: 180px;">
                  <ElOption
                    v-for="type in shopTypes"
                    :key="type.id"
                    :label="type.name"
                    :value="String(type.id)"
                  />
                </ElSelect>
              </ElFormItem>
              <ElFormItem label="页码">
                <ElInput v-model="forms.current" style="width: 120px;" />
              </ElFormItem>
              <ElFormItem label="经度 x">
                <ElInput v-model="forms.x" placeholder="可选" style="width: 160px;" />
              </ElFormItem>
              <ElFormItem label="纬度 y">
                <ElInput v-model="forms.y" placeholder="可选" style="width: 160px;" />
              </ElFormItem>
            </ElForm>

            <div class="filter-actions">
              <ElButton type="primary" @click="queryShopsByType">查询</ElButton>
              <ElButton @click="resetTypeFilters">重置</ElButton>
            </div>
          </ElCard>

          <ElCard shadow="never" class="page-panel">
            <template #header>
              <div class="page-panel__header">
                <div>
                  <h3 class="page-panel__title">表格区</h3>
                  <p class="page-panel__hint">统一使用 Element Plus Table 承载列表结果。</p>
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
              <ElTableColumn prop="openHours" label="营业时间" min-width="180" />
              <ElTableColumn label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <ElButton link type="primary" @click="fetchShopDetail(row.id)">
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
            <template #header>
              <div class="page-panel__header">
                <div>
                  <h3 class="page-panel__title">筛选区</h3>
                  <p class="page-panel__hint">按关键词查询商铺名称。</p>
                </div>
              </div>
            </template>

            <ElForm inline label-position="top">
              <ElFormItem label="关键词">
                <ElInput
                  v-model="forms.name"
                  placeholder="输入店名"
                  style="width: 240px;"
                  @keyup.enter="queryShopsByName"
                />
              </ElFormItem>
              <ElFormItem label="页码">
                <ElInput v-model="forms.nameCurrent" style="width: 120px;" />
              </ElFormItem>
            </ElForm>

            <div class="filter-actions">
              <ElButton type="primary" @click="queryShopsByName">查询</ElButton>
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
                  <ElButton link type="primary" @click="fetchShopDetail(row.id)">
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

    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h3 class="page-panel__title">店铺详情与优惠券</h3>
            <p class="page-panel__hint">
              详情区与次级列表区统一放在页面底部，避免破坏主列表结构。
            </p>
          </div>
          <div class="inline-actions">
            <ElInput
              v-model="forms.detailId"
              placeholder="商铺 ID"
              style="width: 140px;"
            />
            <ElButton type="primary" @click="fetchShopDetail()">查询详情</ElButton>
          </div>
        </div>
      </template>

      <ElEmpty v-if="!selectedShop" description="先选择或查询一个商铺。" />

      <div v-else class="app-page">
        <ElDescriptions :column="2" border>
          <ElDescriptionsItem label="名称">
            {{ selectedShop.name }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="评分">
            {{ ((selectedShop.score || 0) / 10).toFixed(1) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="均价">
            ￥{{ formatPrice(selectedShop.avgPrice) }}/人
          </ElDescriptionsItem>
          <ElDescriptionsItem label="营业时间">
            {{ selectedShop.openHours || "--" }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="商圈">
            {{ selectedShop.area || "未知商圈" }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="地址">
            {{ selectedShop.address || "暂无地址" }}
          </ElDescriptionsItem>
        </ElDescriptions>

        <div v-if="firstImage(selectedShop.images)" class="detail-cover">
          <ElImage :src="firstImage(selectedShop.images)" fit="cover" />
        </div>

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
          <ElTableColumn label="状态" width="140">
            <template #default="{ row }">
              <ElTag effect="plain">
                {{ row.type === 1 ? voucherState(row).label : "普通券" }}
              </ElTag>
            </template>
          </ElTableColumn>
          <template #empty>
            <ElEmpty description="当前商铺暂无优惠券数据" />
          </template>
        </ElTable>
      </div>
    </ElCard>
  </section>
</template>
